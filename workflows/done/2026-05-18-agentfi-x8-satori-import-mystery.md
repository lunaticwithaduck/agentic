---
title: AgentFi X8 — Investigate AgentCard import-vs-inline satori mystery
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Root-cause why the imported `<AgentCard mode="og">` crashed satori with `Cannot read '...trim()` while identical JSX inlined in the route handler rendered fine.

## Time box
~60 min planned. **Actual: ~12 min — root-cause found on the first directed bisect.**

## Hypotheses tested
- [x] **Hypothesis A** (different invocation patterns): Built `/og/diag-import?mode={jsx,direct,fragment,clone}` to test JSX vs direct-call vs Fragment-wrap vs cloneElement. **All four failed.** Bug is in the React tree AgentCard returns, not in how it's invoked.
- [x] **Hypothesis F** (line-by-line source diff against working inlined version): Found the smoking gun on line 60-61 of `components/AgentCard.tsx`:
  ```ts
  height: isOg ? CARD_HEIGHT_OG : undefined,
  minHeight: isOg ? undefined : 340,
  ```
- [x] Verified fix: changed to conditional spread `...(isOg ? { height: CARD_HEIGHT_OG } : { minHeight: 340 })`. Both `?mode=jsx` and `?mode=direct` now return 200 + PNG.

## Root cause
**`undefined` values in inline style objects crash satori.** TypeScript's `React.CSSProperties` type accepts `string | number | undefined`, so the type checker stays silent. At JS runtime the key stays present in the style object with value `undefined`. Satori iterates style keys and calls `.trim()` on the value, which crashes:
```
TypeError: Cannot read properties of undefined (reading 'trim')
```

**Inlined JSX in route handlers never hit the bug** because nobody writes `<div style={{height: undefined}}>` literally. Programmatic style objects (especially when conditional values are based on a `mode` prop) hit it constantly.

The fix is universal: conditionally spread the keys instead of ternary-with-undefined:
```ts
// ❌
{ height: isOg ? 630 : undefined, minHeight: isOg ? undefined : 340 }
// ✅
{ ...(isOg ? { height: 630 } : { minHeight: 340 }) }
```

## Cleanup applied
- [x] Reverted `app/og/agent/[slug]/route.tsx` to import `<AgentCard>` — deleted ~80 lines of duplicated inline JSX. One component, two render targets, no manual sync.
- [x] Deleted `app/og/diag-import/` (bisect scaffold)
- [x] Updated `.claude/skills/satori.md` with a dedicated "NEVER put undefined values" section + corresponding Failure Modes entry
- [x] Removed the "one component two render targets — with caveats" section's misleading workaround language; the contract works as designed with the undefined-key fix
- [x] Regenerated snapshot baseline (`pnpm exec playwright test snapshots.spec.ts --update-snapshots`) because the rendered OG now uses AgentCard's actual layout (was the slightly-different inlined version)

## Verification
- `pnpm test`: **77/77** vitest
- `pnpm exec playwright test`: **17/17** in 10.5s (15 smoke + 2 snapshot)

## Outcome
Solved on first directed bisect after the invocation-pattern hypotheses fell. The bug was a single-line TS-vs-runtime mismatch hiding in a style object — invisible to type-check, invisible to react render, only triggered by satori's strict key iteration. The satori.md skill now has an explicit failure-mode entry; future projects won't burn the same debugging hours.

The "one component, two render targets" contract is now genuinely real for this project. AgentCard renders the same in `/agent/[slug]/page.tsx` and `/og/agent/[slug]/route.tsx` from a single import.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: satori, `next/og`, React.CSSProperties typing
- Domain knowledge: undefined-in-style-objects is the #1 satori `.trim()` crash; TypeScript's CSSProperties allows it but JS keeps the key
- Verdict: **GENERATE**
- Reason: This is the single most important satori pattern any TS user will hit. Worth a dedicated `.sc` so the synthesis pulls it into the next `satori.md` regeneration if anyone ever re-synthesizes.
- Domain: `satori` (4th entry — skill already synthesized at 3, this just enriches the corpus)

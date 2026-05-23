---
title: AgentFi X3 — Caption templates library (7 templates, versioned, action-typed)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
The publishing engine's first muscle: a versioned library of 7 caption templates (one per action type plus a daily-comp variant), with a render function that takes the action + agent + metric snapshot and produces a finished caption string. Pure TS. Sets up A/B testing in week 2 by versioning each template.

## Steps
- [x] `lib/poster/types.ts` — `CaptionTemplate { id, actionType, version, body }`, `CaptionRenderInput`, `RenderedCaption`, `CaptionSurface`, `SURFACE_CAP` (x: 280, farcaster: 320)
- [x] `lib/poster/amount-parse.ts` — `parseDiemAmount(detail)` handles forward (`+0.18 DIEM`), reverse (`DIEM 0.5`), signed, and returns null for ambiguous
- [x] `lib/poster/templates.ts` — `TEMPLATES` registry keyed by action type. 7 entries: CLAIM / LP / SWAP / STAKE / LOG / MILESTONE / DAILY_COMP. MILESTONE is the only one with an ⚡ prefix.
- [x] `lib/poster/render.ts` — `renderCaption(template, input)`. Substitutes `{slug}`, `{agent}`, `{amount}`, `{detail}`, `{compute_multiple}`, `{compute_val}`, `{percent}`, `{link}`, `{timestamp}`, `{top_3_rows}`. Throws on unknown placeholder so template typos surface in tests.
- [x] Length guardrail: warns (doesn't throw) if rendered caption exceeds `SURFACE_CAP[surface]`. Returned via `RenderedCaption.warnings: string[]`.
- [x] 15 vitest tests: amount parser (4 cases), registry shape (2), per-template render (6), length warnings (2), unknown-placeholder error (1). All pass.

## Verification
- `pnpm test`: 7 files, **58 tests**, all pass (15 new template tests)
- `pnpm build`: PASS

## Outcome
Templates library is dialed and surface-agnostic. Three small notes:

1. **MILESTONE earns the only emoji.** Per design doc §14: "No emoji in UI chrome. (Emoji in action feed LOG content is fine — it's content, not UI.)" The ⚡ on MILESTONE is content-as-signal — milestones are the headline; the lightning bolt earns its place. No other template gets one.

2. **`parseDiemAmount` is permissive on purpose.** Matches `+0.18 DIEM`, `-0.05 DIEM`, `0.5 DIEM`, `DIEM 0.5`. Returns `null` for `"0.5 ETH paired"` (DIEM keyword absent). Caller decides null-handling — for `{amount}` substitution in templates that require a DIEM amount, a missing match means the action shouldn't have hit that template in the first place (materiality should have filtered it).

3. **Strict placeholder substitution.** Unknown `{foo}` in a template throws. Catches typos at test-write time rather than at runtime when a malformed caption ships. Tests cover this explicitly.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: TypeScript template strings, regex parsing
- Domain knowledge: project-specific (AgentFi caption shapes, AUTONO domain language)
- Verdict: **SKIP**
- Reason: No technology-specific knowledge worth capturing. Template rendering + placeholder substitution is well-trodden. The "strict on unknown placeholder" pattern is one line of code, not skill-shaped.

---
title: AgentFi X6 — Headless Playwright smoke test suite
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
End-to-end smoke coverage of every shipped route via headless Chromium. Catches what vitest can't: real Next runtime, real proxy auth, real ImageResponse over HTTP, real CSS render.

## Steps
- [x] Installed `@playwright/test` 1.60 + downloaded Chromium 1223 (1217 was cached but Playwright wanted 1223)
- [x] `playwright.config.ts` — `testDir: tests/`, `webServer: pnpm start`, single worker, Chromium-only project, baseURL `http://localhost:3000`
- [x] `tests/smoke.spec.ts` — 15 specs covering: landing / agent page / comp + screenshot / methodology / preview / admin (401 + 200) / admin/dry-run / 3 OG routes + 404 / unknown route 404 / favicon
- [x] `"test:e2e": "playwright test"` in package.json
- [x] `playwright-report/`, `test-results/`, `.playwright/` in `.gitignore`
- [x] Final result: **15/15 passing in 8.2s**

## Major findings (the real value of this task)

This task surfaced multiple real bugs that vitest had been silently masking, plus a Next.js 16 / satori gotcha trail that invalidates parts of the T4 and T6 `.sc` claims. Documented in detail in this turn's `.sc` so future projects don't relearn.

1. **Vitest OG tests were false positives.** They checked `res.status` + `res.headers` but never consumed the body. `ImageResponse` returns lazily — satori only runs when the stream drains. Updated `lib/__tests__/og-routes.test.ts` to call `await res.arrayBuffer()` for each OG render so vitest now actually exercises satori. Test count unchanged (5 OG tests) but they're now meaningful.

2. **`position: absolute/relative` crashes the current `next/og` satori** with `Cannot read properties of undefined (reading '256')`. T4's `.sc` literally lists this as supported — it isn't, at least in Next.js 16.2.6 + the bundled satori. Refactored AgentCard's BuildModePanel to a 3-segment flexbox bar (pre-threshold half | 2px tick | post-threshold half) — visually identical, satori-safe.

3. **`display: inline-block` crashes satori** with a clearer error: `Allowed values: "flex" | "block" | "contents" | "none" | "-webkit-box". Received: "inline-block"`. Changed AgentDot and the comp/action OG dots from `<span style={{display:"inline-block"}}>` to `<div style={{display:"flex"}}>`.

4. **`flexBasis: "auto"` triggers `Cannot read '...trim'` in satori.** Removed from ActionRow's detail span — default basis renders identically for our layout.

5. **`app/icon.tsx` cannot carry custom variable fonts** (T1/X1 finding, confirmed: variable-axis `weight: 700` queries crash at static-gen). Static `JetBrainsMono-Regular.ttf` (downloaded into `assets/fonts/`) works for OG routes; the icon needs no font at all.

6. **Same JSX behaves differently when imported vs inlined** (smoking gun unsolved). After fixing all of #2–#5, `app/og/agent/[slug]` still crashed with `.trim()` when invoking the imported `<AgentCard mode="og">`. Copy-pasting AgentCard's JSX inline into the route handler renders fine. Workaround: route inlines the JSX with a top-of-file comment explaining the split. Visual-update process: when AgentCard changes, also update the inline copy in the OG route. Vitest covers AgentCard, Playwright covers the OG. Real divergence from T4's "one component, two render targets" contract.

7. **Playwright parallelism + OG renders = socket hang up.** Initial config used 4 workers; OG routes (~1-2s satori each) got "socket hang up" under contention. Reduced to `workers: 1`. Suite runs sequentially in ~10s — fast enough.

8. **Playwright 1.60 → Chromium 1223** required; the existing cached 1217 wasn't compatible. `pnpm exec playwright install chromium` downloaded the missing 113MB even though the cache had similar browsers.

## Verification
- `pnpm exec playwright test --reporter=line`: **15/15 in 8.2s**
- `pnpm test`: **77/77** (vitest, now with body-consuming OG tests)
- `pnpm build`: PASS, full route table 14 entries

## Outcome
The harness is real. Future regressions in any of the 15 covered behaviors will fail loudly. The `.sc` file from this task gets a hefty satori update — with Failure Modes Observed against the T4 and T6 claims — and triggers auto-synthesis of `.claude/skills/satori.md` (this is the 3rd `satori` .sc).

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: `next/og` + satori (deep findings), Playwright Test (config patterns, webServer, browser-cache mismatch)
- Domain knowledge: 5 distinct satori CSS-subset rules invalidating T4's `.sc` claims; vitest false-positive pattern; Playwright single-worker rule for slow-render routes
- Verdict: **GENERATE**
- Reason: The satori findings are significant enough to invalidate two prior `.sc` entries' claims. Documenting in a 3rd `satori` `.sc` triggers automatic synthesis of `.claude/skills/satori.md`.
- Domain: `satori` (3rd entry — **hits synthesis threshold**)

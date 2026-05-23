---
title: AgentFi X21 — Methodology page worked example with live AUTONO numbers
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Make the methodology page concrete: render a worked example using AUTONO's live numbers (staked DIEM, mcap, multiple). Right now the page describes the formula in the abstract; readers should see the math actually applied to a real agent.

## Files
- `app/methodology/page.tsx` — async, pulls AUTONO snapshot, renders a "worked example" panel showing step-by-step computation
- `tests/smoke.spec.ts` — assert the worked example renders with the right structure

## Steps
- [x] Made methodology page async; fetches latest history entry + spot DIEM price in parallel
- [x] Added "worked example · AUTONO" section showing: staked_diem → × 365 → × spot_diem_usd → = compute_val → mcap → multiple
- [x] Live/mock badge in top-right of the panel ("live · from indexer" vs "mock · pre-indexer") so readers know the source
- [x] Playwright case verifies all formula labels + the live/mock badge + a non-zero × multiple value
- [x] Build + test + Playwright green

## Outcome

Completed on 2026-05-18. The page is now async; `loadAutonoNumbers()` reads the latest snapshot from history + spot DIEM price in parallel. If history is populated, the worked example uses real indexer numbers; if cold-start, falls back to mock-data constants (17 staked DIEM, $1.23M mcap) so the page is never empty. A badge in the panel header tells the reader which source they're seeing.

The 6-step ladder reads like a calculator tape: each `× factor` is its own row with the running result, ending in `multiple = mcap ÷ compute_val` highlighted in signal color. Mono font + tabular nums keep the math readable.

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js async server components, parallel `Promise.all` for unrelated data sources
- Domain-specific knowledge: (a) for documentation pages that explain a formula, showing a "worked example" panel beneath the formula is a strong UX pattern — readers prefer concrete numbers to abstract symbols; (b) the live/mock badge pattern lets a page degrade gracefully without confusing the reader about freshness.
- Verdict: SKIP
- Reason: Pure content/UX pattern, no technology-specific gotcha that needs encoding. Worth remembering as a pattern but not skill-worthy.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x21-methodology-worked-example.md`.

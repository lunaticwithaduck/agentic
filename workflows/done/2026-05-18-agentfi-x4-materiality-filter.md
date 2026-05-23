---
title: AgentFi X4 — Materiality filter (the plan's exact rules)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
The gatekeeper that decides which actions are worth posting about. Encodes the plan's literal rules (plan §Day 6) plus the 5/day cap. Pure TS. Verified via tests against the AUTONO mock snapshot.

## Steps
- [x] `lib/poster/materiality.ts` — `evaluate(action, ctx) → { material, reason }`. Reuses `parseDiemAmount` from X3.
- [x] Rules: CLAIM > 0.1 DIEM material; LP/SWAP/STAKE/MILESTONE always material; LOG material only if > 8 chars AND not duplicating recently-posted log content.
- [x] Daily cap of 5 (overridable via `ctx.dayCap`) — runs FIRST so capped material actions are still skipped with reason "daily cap reached (5)".
- [x] 13 vitest tests covering each rule + cap + cap-just-below + cap-configurable + log-dedupe + log-too-short.
- [x] Verify build + tests still pass.

## Verification
- `pnpm test`: 8 files, **71 tests**, all pass (13 new materiality)
- `pnpm build`: PASS

## Outcome
Filter ships as the plan specifies. Three calls worth recording:

1. **Cap check runs first.** Order matters — even an LP (always-material) is skipped once dayPostCount ≥ cap. The reason string makes the distinction visible ("daily cap reached (5)" vs "lp is always material"), so the dry-run output can show *why* the gate fired in a debuggable way.

2. **LOG dedupe uses exact-string match on `detail.trim()`.** This is permissive — a single character difference re-posts. Per plan it's fine for v1; tighter dedupe (semantic similarity, whitespace-collapsed match) is a week-2 nicety if the LOG firehose proves noisy.

3. **No CLAIM amount → not material.** A claim with detail like `"some weird detail"` (no parseable DIEM amount) is treated as non-material. The materiality reason explicitly says "parseable" so debug is easy. Cleaner than letting an unparseable claim through and crashing the template renderer at `{amount}` substitution.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: TypeScript, pure-function rule engine
- Domain knowledge: project-specific materiality thresholds (5/day, 0.1 DIEM, 8 char log min)
- Verdict: **SKIP**
- Reason: Rules engine with hardcoded thresholds is project-specific. No generalizable technology pattern.

---
title: AgentFi T3 — Agent registry, metric strategies, build-mode math
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
The data brain: a typed agent registry + three metric strategies (compute-valuation, treasury-runway, fee-revenue) + a `getMetric()` dispatch + build-mode math. All pure functions consuming mocked inputs — no on-chain calls yet. Lock the DIEM pricing assumption here so the launch can't be embarrassed by it later.

## Steps
- [x] `lib/types.ts` — `Agent`, `MetricStrategy`, `MetricInput` (discriminated union), `MetricResult`, `BuildMode`, `ActionEvent`, `HexAddress`
- [x] `lib/constants.ts` — `DIEM_PRICE_USD`, `BUILD_MODE_THRESHOLD_DIEM_PER_DAY`, `BASE_CHAIN_ID` + locked methodology comment
- [x] `lib/agents.ts` — AUTONO entry with all verified Base addresses + `getAgent` / `listAgents` / `requireAgent`
- [x] `lib/metrics/compute-valuation.ts` — `compute_val = stakedDiem × 365 × diemPriceUsd`, multiple = mcap / compute_val
- [x] `lib/metrics/treasury-runway.ts` — multiple + `runwayDays()` helper
- [x] `lib/metrics/fee-revenue.ts` — annualizes 30d fees, returns P/F multiple
- [x] `lib/metrics/index.ts` — `getMetric(input)` strategy dispatch (typed via discriminated union)
- [x] `lib/build-mode.ts` — `buildMode({ rate, threshold, recentDailyDelta })` with ETA computation, clamping, ACCUMULATE/ACTIVE state
- [x] `lib/format.ts` — `abbrevAddress`, `formatUsd`, `formatMultiple`, `signedDelta` (▲/▼ tone), `formatRate`
- [x] Vitest installed (4.1.6) + `vitest.config.ts` with `@/` alias matching Next.js
- [x] 29 tests across `lib/__tests__/{metrics,build-mode,format}.test.ts` — all passing in 281ms
- [x] `pnpm build` still passes (TypeScript types resolve through full project)

## DIEM peg decision (locked)
`compute_val = stakedDiem × 365 × diemPriceUsd`. `diemPriceUsd` is read from `DIEM_PRICE_USD` constant at $1.00 today. Comment in `constants.ts` calls out that swapping to live Coingecko/Geckoterminal price is a follow-up before launch — this is the #1 methodology nit a CT semi-expert will find.

## Verification
- `pnpm test`: 3 files, 29 tests, all pass
- AUTONO fixture matches design-doc claim: 17 staked DIEM × $1 × 365 = $6,205 compute_val; $1,234,795 mcap → **199× multiple** (rounded)
- Build: PASS

## Outcome
Data brain in place. Highlights:

1. **DIEM peg locked in code** with explanatory comment in `constants.ts`. A future "wire live DIEM price" task only needs to replace the constant with a fetch — methodology is already correct.

2. **Discriminated-union dispatch for `getMetric`** lets each strategy declare its own input shape; the compiler ensures callers can't pass treasury-runway inputs to compute-valuation. Cleaner than the per-strategy adapter pattern I'd considered.

3. **Build-mode ETA design choice**: ETA computed from a caller-supplied `recentDailyDelta` rather than from the indexer state directly. Keeps `buildMode()` pure; the orchestrator (T5/T6) is responsible for computing the 7d trailing delta from snapshots. Default-null when delta is absent — never returns a misleading "soon" estimate.

4. **`formatUsd` magnitude tiers**: under 10K renders with commas (`$6,205`), 10K–1M is K (`$890.0K`), then M and B. Choice driven by readability at small dollar amounts where dropping precision misleads.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: vitest 4, TypeScript discriminated unions
- Domain knowledge: vitest config needs its own `@/` alias mapping (separate from `tsconfig.paths`); `environment: 'node'` is required for pure-TS lib tests (jsdom would slow startup pointlessly); `lib/**/*.test.ts` glob keeps tests colocated without polluting `app/` route discovery
- Verdict: **GENERATE**
- Reason: vitest setup for a Next.js project has a few non-obvious config choices that bite people first time (the alias duplication especially). Worth a `vitest` domain.
- Domain: `vitest` (new)

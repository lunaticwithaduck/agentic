---
title: AgentFi X12 — Live DIEM price fetch (replace $1.00 hardcode)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Steps
- [x] `lib/chain/diem-price.ts` — `getDiemPriceUsd()` async, GeckoTerminal `/networks/base/tokens/<DIEM>`, 5-min in-memory cache, falls back to `DIEM_PRICE_USD = 1.0` constant on failure / null / non-positive value
- [x] `lib/chain/autono.ts` — replaced static `DIEM_PRICE_USD` import with `await getDiemPriceUsd()` inside the Promise.all parallel-fetch block; now part of the same hot path
- [x] 5 vitest tests: success / cache hit / 500 fallback / null fallback / zero fallback

## Verification
- `pnpm test`: **117/117** (+5)
- `pnpm exec playwright test`: 17/17
- `pnpm build`: clean

## Outcome
Compute multiple now uses spot DIEM price as the methodology page documents. When DIEM trades above $1.00 the multiple compresses correctly (was the #1 CT-semi-expert nit). Cache 5-min so the hot path doesn't hit GeckoTerminal on every render.

## .sc
SKIP — extends X9's defi-data patterns. Already captured there.

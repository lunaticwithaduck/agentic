---
title: Bend t12 — live-interest-adjusted HF (the actual unlock, not oracle bundling)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## What was originally hypothesized
Competing bots use Pyth/Redstone oracle-update bundling to catch whales (the $30k+ events). Build matching capability.

## What we actually found
Investigated the May 6 whale tx (`0x3f3a5d50...`) directly:
- Caller is an EOA, not a contract — no flash loan, no swap, no oracle bundling
- Calldata is standard Morpho `liquidate(MarketParams, borrower, max_collateral, 0, "")` — selector `0xd8eabcb8`
- No oracle update tx in block 20524480
- Gas used: 177,899
- Receipt logs include `AccrueInterest` from Morpho's internal `_accrueInterest()` call

**The whale liquidator just held $200k of HONEY and called Morpho.liquidate at the right moment.** The position became liquidatable purely from accrued interest at the liquidation block — Morpho's internal interest accrual pushed HF below 1.

## The real bug — our monitor used stale `totalBorrowAssets`
Our monitor computed HF using `totalBorrowAssets` straight from positions.json — the value Morpho LAST persisted. But Morpho's actual liquidation check uses LIVE totalBorrowAssets = stored × (1 + accrued_interest_since_lastUpdate).

For markets with long elapsed time + high utilization rate (like iBERA in early May at ~100% utilization), accrued interest pushes HF down meaningfully. Our monitor was reporting healthy positions that were actually liquidatable.

## The fix (shipped)
Added to `monitor.js`:
1. `readBorrowRate(rpc, irm, marketParams, marketState)` — calls AdaptiveCurveIRM's `borrowRateView` to get current rate/sec
2. `wTaylorCompounded(rate, elapsed)` — same 3-term Taylor approximation Morpho uses internally
3. `liveTotalBorrowAssets(market, blockTimestamp)` — projects stored value forward to current block
4. `computeHF(position, market, oraclePrice, blockTimestamp)` — uses live TBA instead of stored

Per-sweep: read each market's borrow rate alongside oracle price (in parallel). Cache `borrowRatePerSec` on the market record. Compute HF with the live projection.

## Current rate snapshot (2026-05-19 ~06:00 UTC)
| Market | APR | lastUpdate age | Projection impact |
|---|---|---|---|
| WBTC/HONEY | 0.06% | 29h | negligible (~0.0002%) |
| sUSDe/HONEY | 9.94% | 6.3d | meaningful (~0.17% in 6 days) |
| wgBERA/HONEY | 2.41% | 6.3d | meaningful (~0.04%) |
| WETH/HONEY | 1.50% | 51m | negligible |
| WBERA/HONEY | 0.20% | 14h | negligible |
| iBERA/HONEY | 2.06% | 6.3d | meaningful (~0.04%) |

Current borrowers are too healthy for these small projections to matter. But the May 6 iBERA whale was at HF=1.0004 stored with much higher historical rates — the projection would have shifted that below 1.0 in the same block Morpho accrued.

## Reframing the historical capture rate
The earlier "1 of 8 needs oracle bundling" finding was wrong. **All 8 historical liquidations are catchable** by polling — provided the monitor uses live-interest-adjusted HF.

Realistic monthly capture (revised UP):
- Per cluster: maybe $30-40k addressable if we win the race against `0x0568ccb3` and `0x22c3462e`
- Win-rate assumption: 30-50% (one of three bots, no latency advantage)
- Expected: $10-20k/cluster
- Cluster frequency: ~1-2/month
- Steady-state: $10-40k/month

Significantly better than the previous "$5-15k" estimate.

## Outcome

Completed 2026-05-19. Investigated the May 6 whale tx to confirm/reject the oracle-bundling theory. Found it was wrong — the whale liquidator didn't bundle anything. They just called Morpho.liquidate as an EOA at the moment Morpho's internal accrue would push the position below HF=1. Our monitor was missing these because we computed HF off stored totalBorrowAssets instead of projecting forward with the IRM rate. Shipped the fix: added `borrowRateView` reads + Taylor compounding + live-TBA projection to monitor.js. Monitor restarted, running clean. This unlocks the entire whale class that we'd thought was out-of-reach — realistic monthly income revised upward from $5-15k to $10-40k.

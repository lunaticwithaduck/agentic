---
title: Discover real per-asset LIQUIDATION_BONUS on HyperLend + HypurrFi
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Executor assumed LIF=5% (Aave default). HyperLend backtest showed actual profit ratios up to 14% on UETH. Real LIF is per-asset, encoded in `Pool.getConfiguration(asset)` reserve config bitmap.

## Steps
- [x] Decode Aave V3 ReserveConfiguration: LIQUIDATION_BONUS at bits 32-47
- [x] Query `Pool.getConfiguration(asset)` for every reserve on both chains
- [x] Build per-reserve LIF tables in config.js as `LIF_BPS_BY_ASSET`
- [x] Update both executors to lookup real LIF; `LIF_DEFAULT_BPS=10500` fallback for unknown assets
- [x] Guard: if LIF=0 (debt-only asset), abort presign

## Discovered LIFs (2026-05-20)

### HyperLend
| Asset | LIF (bps) | Bonus % |
|---|---:|---:|
| WHYPE | 11000 | +10% |
| **wstHYPE** | **11500** | **+15%** |
| kHYPE | 11000 | +10% |
| **UBTC** | **12000** | **+20%** |
| **UETH** | **11500** | **+15%** |
| USDe | 0 | debt-only |
| USDT0 | 10800 | +8% |
| USDC | 10800 | +8% |

### HypurrFi
- WHYPE, UBTC, UETH, USOL: 11200 (+12%)
- wstHYPE, kHYPE, stables, PTs, beHYPE: 10800 (+8%)
- USDXL, USDe, feUSD: 0 (debt-only)

## Outcome
Completed 2026-05-20. Real LIFs across both chains are **8–20%**, not the 5% I hardcoded. The 5% assumption was safe (under-estimating → dust to OWNER) but suboptimal — left unrealized profit on the table for high-LIF assets. Now executor computes correct `expectedSeized` and chooses tight swap `amountIn`, leaving less dust. Bigger swap → larger `amountOutMinimum` headroom → `$20 minProfitWei` is now more comfortably above the actual margin.

**Behavior change at scale:**
- Wins on UBTC collateral now seize ~14% more value than estimated
- The `swapAmountIn = 97% × expectedSeized` safety margin still applies on top
- Dust transferred to OWNER decreases by ~5-15% per fire (real profit unchanged; just less leftover transfer gas)

## Completion
Run `/complete workflows/tasks/2026-05-20-per-asset-lif-discovery.md`.

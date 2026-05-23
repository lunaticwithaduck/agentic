---
title: Bend health-factor monitor — score every position, emit alerts
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Continuously compute health factor (HF) for every open Bend position. When HF drops near 1.0, alert the executor to arm. When HF < 1.0, emit "FIRE" signal.

Health factor formula (Morpho-style):
```
HF = (collateral_amount × collateral_price × lltv) / (borrow_assets)
```
HF ≥ 1 → safe. HF < 1 → liquidatable.

Depends on: `bend-t1-indexer-positions` (position registry).

## Oracle reads per market
Each market has a per-market oracle returning `price()` as 36-decimal scaled (Morpho convention: `price * 10^(36 + loanDecimals - collateralDecimals)`).

- WBTC/HONEY oracle: `0x7473Be21793d12ccEc17CE17fD95ba1cB114C9EB`
- sUSDe/HONEY oracle: `0xA5B159c09De984CF5AC51500C9305F1347AcE444`
- wgBERA/HONEY oracle: `0x83348Bef4994AbFC11Af0790cA83fa5f582B77ae`
- WETH/HONEY oracle: `0xD0009f347f288d9e1133bAdB15b43C186bc4eea8`
- WBERA/HONEY oracle: `0x93AD06c576C523E6FA6B70b594340EFcdBa3228C`
- iBERA/HONEY oracle: `0x7614a738aCCC2Ed7234638250e769F72BcBC3880`

## Steps
- [x] Read all 7 oracles via `oracle.price()` selector (Morpho standard `function price() view returns(uint256)`)
- [x] Implement borrow-asset conversion: `borrowAssets = (borrowShares × totalBorrowAssets + totalBorrowShares - 1) / totalBorrowShares`
- [x] Implement Morpho HF: `HF = (collateral × oraclePrice × lltv) / (1e36 × 1e18 × borrowAssets)`
- [x] Loop: every block recompute HF for all positions
- [x] Telegram alerts at WARN (HF<1.1), ARM (HF<1.02), FIRE (HF<1.0)
- [x] Min-debt filter ($20) + per-position 5-min dedup
- [x] Persistent state: `healthfactors.json` + `armed/<key>.json`
- [x] Quiet logging (full table every 30 blocks only)

## Outcome

Completed 2026-05-18. The Bend health-factor monitor is operational at `/home/jojo/automation/bend/monitor.js`. It reads `positions.json` from the indexer, reads all 7 market oracles in parallel via the standard Morpho `price()` selector, computes HF per Morpho's formula (`collateral × oraclePrice × lltv / (1e36 × 1e18 × borrowAssets)`), and emits Telegram alerts at three thresholds with 5-minute dedup and a $20 min-debt filter. When a position fires, it writes `data/armed/<marketId>-<borrower>.json` for the executor (t4) to consume. Live state shows the WBTC market borrower `0xcb88ac0f` at HF 1.186 ($14k debt → ~$2k profit if liquidated) as the most realistic near-term target. ~250ms per sweep including 7 oracle reads.

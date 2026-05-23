---
title: Bend race executor — port MIBERA execute.js pattern, paper-trade first
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
The hot-path component that actually wins races. Receives "armed" signals from the monitor (t2), pre-signs liquidation txs, broadcasts them via WSS-driven block triggers + dual-RPC fallback.

## Steps
- [x] Port MIBERA execute.js patterns: pre-sign, dual-broadcast, WSS trigger, wall-clock backup
- [x] `data/armed/*.json` IPC via fs.watch; initial scan on startup
- [x] WSS block subscription re-checks armed positions; fire if HF crosses 1.0
- [x] Wall-clock expiry: drop armed positions after 2min if HF recovers above 1.05
- [x] Telegram alerts at every step (pre-signed, FIRING, broadcast OK, confirmed, profit/loss, paper)
- [x] BEND_DRY env + --dry flag for paper-trade
- [x] Dual-RPC broadcast via Promise.any (publicnode + official)
- [x] Liquidated event decoding to extract real on-chain profit
- [x] Kodiak swap-path builder (swap-path.js with findBestPool + quoteSwap + buildSwapData)
- [x] Per-market Kodiak pool persisted in positions.json via indexer
- [x] 0.5% slippage protection via amountOutMinimum

## Kodiak pools discovered
- WBTC/HONEY  — 0.30% pool `0x545Bea6Ea7F8fD8dCC5C9A6802a8ebF3DbFc1C6E`
- sUSDe/HONEY — 0.05% pool `0xcFfe3649a78A84A1C4aD9417aA041C2c52379AcE`
- wgBERA/HONEY — 0.05% pool `0x4E4C8ffD73A4AA6ad262672e1FB3602412e82EF2`
- WETH/HONEY  — 0.30% pool `0x9EB897D400f245E151daFD4c81176397D7798C9c`
- WBERA/HONEY — 0.01% pool `0xA99a2a23cDC6DAee0ec0EF4Ffe4133618849C890`
- iBERA/HONEY — 0.30% pool `0x3F4748132e7c4662D61eb14c611306e5208D5d8E`

## ✅ Oracle ↔ Kodiak pricing verified consistent
Initial confusion: I'd computed Bend's WBTC oracle as $99k vs Kodiak's $76k = 23% gap. Wrong. HONEY is pegged $0.9992. Re-deriving with correct 10^46 scale gave 76,622 HONEY/WBTC, matching Kodiak's 76,071 within 1%. Berachain's bridged WBTC simply trades at a discount to mainnet BTC. Executor's expected profit math is correct.

## Files produced
- `/home/jojo/automation/bend/executor.js` — race executor
- `/home/jojo/automation/bend/swap-path.js` — Kodiak v3 discovery + calldata builder

## Outcome

Completed 2026-05-18. The race executor ports MIBERA's proven WSS+pre-sign+dual-RPC pattern to a multi-position liquidation flow. File-watch on `data/armed/*.json` triggers per-position pre-signing; WSS block subscription re-checks HF and fires when one crosses 1.0; wall-clock expiry drops stale armed positions. Kodiak swap path is fully wired: indexer auto-discovers best fee tier per market on startup, executor encodes real `exactInputSingle` calldata with 0.5% slippage protection. 6 of 7 markets have Kodiak pools (only the unlisted 7th market lacks one). The oracle ↔ DEX price gap I worried about initially turned out to be my own scale-math error — the prices match within 1%, and the executor's expected profit projections are correct.

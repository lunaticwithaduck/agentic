---
title: P0 — fix Felix monitor false-FIRE: live-read position state instead of trusting stale positions.json
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Felix **monitor** was computing HF from `positions.json` collateral/borrowShares, which the indexer updates lazily. When a borrower topped up collateral on-chain, the monitor saw stale data and logged false `🔥 FIRE` events. The **executor** re-reads live state from `morpho.position()` and correctly skipped (`HF healed`). Fix: make the monitor live-read too, so monitor and executor agree.

## Root cause confirmed
For position `0xae5028f1aca...` in market `0xd7d38220...` at 17:00:55 UTC:
- positions.json collateral: `11461491693026684733` (11.46 HYPE)
- on-chain collateral:      `15079661763371939176` (15.08 HYPE) — user added 3.62 HYPE
- Monitor HF using stale coll: **0.9966** → false FIRE
- Executor HF using live coll: **1.3113** → correctly skipped
- The 31.6% collateral delta exactly matches the HF delta (0.9966 vs 1.3113)

The indexer's `SupplyCollateral` event scan was too lazy. Other quantitative position data (borrowShares from Borrow/Repay/Liquidate, collateral from SupplyCollateral/WithdrawCollateral) had the same risk.

## Steps taken
- [x] Reproduced and documented the divergence with on-chain math
- [x] Added `readPositionState(rpc, marketId, borrower)` helper using `position(bytes32,address)` selector + manual abi-encoding (felix/monitor.js:108-121)
- [x] In `loadAndSweep`: replaced the nested `for (mid)/for (addr)` loop with a candidates array + `Promise.all` of live reads
- [x] On read failure: SKIP the position for this tick (do NOT fall back to stale positions.json — that was the original bug)
- [x] Added `liveFailCount` diagnostic that logs `⚠️ live-position reads failed for X/Y — RPC pool stressed` when >25% fail
- [x] Restarted felix-monitor.service; verified no more 0xae5028 false-arms in logs
- [x] Confirmed healthfactors.json now shows correct HF 1.3325 + correct coll 15.0797 HYPE for 0xae5028

## Verification
After fix deployed at 17:12:42 UTC:
- 0xae5028 HF in healthfactors.json: **1.3325** ✓ (matches executor's 1.3113 within oracle drift)
- 0xae5028 coll: **15.0797 HYPE** ✓ (matches on-chain)
- Zero false 🎯-arm log lines for 0xae5028 since restart
- Real whale 0x24df4b7af6 still continuously armed at HF 1.0165 ✓
- Dust 0x023f609c9d still tracked at HF 0.959 ✓

## Outcome
Completed 2026-05-21. The Felix lane is now correct end-to-end: monitor and executor compute the same HF from the same live on-chain source. When `0x24df4b7af6` ($286k whale) crosses below 1.0, the fire will broadcast — no more `HF healed (1.31)` ghost-cancellations.

Initial full sweep is now ~70s (was instant) because it does 216 parallel live-reads against the 7-RPC public pool. Steady-state per-block ticks (~10 positions across 4 watched markets) are fast (<1s).

## Out-of-scope follow-up
Indexer freshness — `positions.json` collateral/borrowShares are still stale and only refresh on indexer event scans. With this fix, that staleness no longer causes false fires; it's purely a performance optimization for sweep cadence. Can be tracked separately.

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-executor-stale-hf-bug.md`.

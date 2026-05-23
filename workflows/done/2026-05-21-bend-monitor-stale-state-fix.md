---
title: Apply Felix's live-read fix to Bend monitor (Morpho fork — same stale-state bug)
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Bend monitor inherited the same stale-state bug as Felix (Bend was the source file Felix was copied from). The monitor computed HF using `p.collateral`/`p.borrowShares` from positions.json instead of live-reading from `morpho.position()`. Apply the same patch we just landed on Felix.

## Steps taken
- [x] Added `readPositionState(rpc, marketId, borrower)` helper using `position(bytes32,address)` selector + manual ABI encoding, calling `cfg.BEND` (the Berachain Morpho fork singleton at 0x2414…17D0)
- [x] Flattened the nested `for (mid)/for (addr)` loop in `loadAndSweep` into a candidates array + `Promise.all` of live reads
- [x] On read failure: SKIP for this tick (do NOT fall back to stale positions.json)
- [x] Added `liveFailCount` diagnostic that warns when >25% of reads fail
- [x] Smoke test (`--once --dry`): all 23 positions evaluated cleanly, no errors
- [x] Restarted bend-monitor.service (PID 33519)
- [x] Spot-check `0xaeb9f06c`: monitor snapshot matches on-chain (1.6785 HYPE, HF 1.1599) ✓

## Verification
After fix deployed at 17:22:42 UTC:
- All 23 Bend positions evaluated via live `morpho.position()` reads
- Monitor's reported collateral for spot-checked borrower exactly matches on-chain
- WSS connection re-established cleanly
- Bend has been quiet (no positions HF<1.09), so no false-FIREs to compare against — but the fix is mechanically identical to Felix's, which was confirmed to eliminate false-FIREs there

## Outcome
Completed 2026-05-21. Bend's lane now has the same live-read guarantee as Felix. The two Morpho-style lanes (Bend on Berachain, Felix on HyperEVM) are now structurally protected against indexer-lag false-FIREs. The three lens-based lanes (Sonic, HyperLend, HypurrFi) were always safe by construction.

## Completion
Run `/complete workflows/tasks/2026-05-21-bend-monitor-stale-state-fix.md`.

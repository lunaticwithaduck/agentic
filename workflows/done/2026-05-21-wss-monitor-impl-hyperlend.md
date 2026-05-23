---
title: Implement WSS monitor for HyperLend
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Build `monitor-wss.js` for HyperLend that detects HF<1.0 events within sub-second of block production via Alchemy WSS, replacing 2s HTTP polling as the primary detection path.

## Steps
- [x] Wrote `/home/jojo/automation/hyperlend/monitor-wss.js` with subscribe-newHeads, watch-list management, reconnect logic, heartbeat
- [x] Tested WSS connectivity — 19 blocks received in 20s direct test
- [x] Debugged "no events" issue — was just an artifact of buffered output; messages were arriving fine
- [x] Tuned WATCH_THRESHOLD_HF = 1.20 (was 1.10, gave 0 users; now 3 users)
- [x] Added per-block heartbeat log every 60 blocks for ops visibility
- [x] Created `hyperlend-monitor-wss.service` (additive, runs alongside HTTP monitor)
- [x] Enabled + started — active, subscribed to newHeads via Alchemy

## Architecture
**Watch list**: HF<1.20 borrowers, rebuilt every 60s via HTTP sweep. Currently 3 users.

**Block loop**: subscribe to Alchemy WSS `newHeads` → on each block, Multicall3-batch `getUserAccountData` for watch list → if HF<1.0, write armed file + Telegram `🔥 WSS arm`.

**Dedup**: 30s per-user dedup so flapping HF doesn't spam-arm.

**Fallbacks**:
- HTTP monitor (existing, 2s polling) still runs in parallel — both write to the same `data/armed/` directory; whoever sees it first wins
- Heartbeat detects silent WSS drop (no block in 10s) → force reconnect + Telegram alert
- Reconnect uses exponential backoff (1s → 30s cap)

## Outcome
Completed 2026-05-21. WSS monitor active as `hyperlend-monitor-wss.service`. Fleet now has 13 services across 4 chains. Sequence on next at-risk position:
1. Block N produced → WSS pushes header ~50-200ms later
2. Monitor multicalls watch list (3 users) → ~300-500ms
3. HF<1.0 detected → write armed file
4. Executor's `fs.watch` fires → presign + estimateGas + broadcast → ~1-1.5s
5. **Total: ~1.5-2.5s end-to-end** (vs prior ~2-4s with HTTP polling)

This should put us in the same tier as `0xdd86…0Fa` (the bot that beat us last night). Still won't beat MEV-bundle bots, but will compete with block-driven HTTP-polled competitors.

**Parallel validation**: HTTP monitor still running. We'll see which arms first on the next real fire. After 1 day of data, can decide to disable HTTP monitor or keep as backup.

## Completion
Run `/complete workflows/tasks/2026-05-21-wss-monitor-impl-hyperlend.md`.

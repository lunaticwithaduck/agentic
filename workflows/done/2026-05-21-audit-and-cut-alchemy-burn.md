---
title: Audit + cut continuous Alchemy CU burn (we're at $6 with zero fires)
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Find leaks. User saw $6 of Alchemy burn with no fires. My $3-5/mo estimate was wrong by 10×.

## Findings
1. **ethers v6 JsonRpcProvider default pollingInterval = 4000ms** — every long-lived provider polls `eth_blockNumber` every 4s. 3 executors × 15 calls/min × 24h × ~10 CU = ~1.7M CU/day = ~$0.76/day = **~$23/mo silent drain**.
2. **HL/HP indexers used `cfg.HTTP_RPC` with null HTTP_RPC2/3** — all rotation hit Alchemy. ~443 indexer starts/day across the fleet.
3. **WSS reconnects** observed at 11/17/5 events per chain in 24h. Resubscribe overhead + Alchemy bills per delivered subscription event (per their pricing). Could be the biggest line item if WSS billing is significant.

## Steps shipped
- [x] Set `provider.pollingInterval = 60_000` (1 min) in all 3 executors (HL/HP/Felix). Executors are fs.watch-driven so they don't need 4s block polling.
- [x] Patched HL/HP indexers to use `(cfg.LOGS_RPCS || []).concat([cfg.HTTP_RPC])` — public pool first, Alchemy fallback. Indexers will pick up on next 5-min cycle.
- [x] Restarted executors with new polling cadence.
- [x] Saved findings as `feedback_alchemy_hidden_burns.md` memory.

## Outcome
Completed 2026-05-21. Expected reductions:
- Executor polling: ~$23/mo → ~$1.5/mo (15× reduction)
- HL/HP indexers: ~$0.50/mo → ~$0/mo
- **WSS message delivery: unchanged** — still need to investigate whether this is the actual biggest cost

If burn stays high tomorrow despite these fixes, the WSS subscriptions are the culprit. Options if so:
- Drop WSS, fall back to 1s HTTP polling on public RPC pool (free)
- Keep WSS but accept the cost as the price of sub-second detection
- Negotiate paid Alchemy tier for cheaper per-message rate

## Completion
Run `/complete workflows/tasks/2026-05-21-audit-and-cut-alchemy-burn.md`.

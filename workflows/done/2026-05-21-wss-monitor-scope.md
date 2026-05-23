---
title: Scope the WSS-driven monitor upgrade for HyperEVM bots
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Define the design, effort, and risk of upgrading HyperLend + HypurrFi monitors from 2s HTTP polling to WSS-driven sub-second detection.

## Steps
- [x] Measured current detection latency (lost by 1-2s to competitor `0xdd86…`)
- [x] Designed watch-list architecture (HF in [0.90, 1.10], rebuilt every 60s by HTTP sweep)
- [x] Confirmed Alchemy free tier supports `eth_subscribe("newHeads")` and Multicall on `latest`
- [x] Estimated effort: 5-6 hr total; ~3 hr core + 1 hr fallback + 1 hr mirror to HypurrFi + tests
- [x] Defined rollout plan: HL first, parallel with HTTP for 1 day, then HP
- [x] Decision: **GO** — build for HyperLend now

## Outcome
Completed 2026-05-21. User chose "Build it now — 5-6 hr, HyperLend first." Spawning implementation task `2026-05-21-wss-monitor-impl-hyperlend.md`. Key risks to mitigate in implementation:
1. Silent WSS drops — need heartbeat / reconnect with backoff
2. Stale state right after a new block — keep HTTP fallback ready
3. Alchemy free-tier compute units — monitor usage during the parallel run

## Completion
Run `/complete workflows/tasks/2026-05-21-wss-monitor-scope.md`.

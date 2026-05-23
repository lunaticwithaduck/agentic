---
title: Route WSS-monitor multicalls to public RPC (off Alchemy)
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Cut Alchemy PAYG costs ~$55/mo by routing the per-block multicall in both WSS monitors through the public/stakely/purroofgroup rotation. WSS subscription stays on Alchemy.

## Steps
- [x] Added `LOGS_RPCS` to `/home/jojo/automation/hyperlend/config.js` (HypurrFi already had it)
- [x] Replaced `rpcCall()` in both `monitor-wss.js` files with a rotating helper that tries LOGS_RPCS in round-robin then falls back to Alchemy
- [x] Watch-list refresh + per-block multicall both use the new helper
- [x] Once-tested both monitors — HL watch list 4 users, HP 1 user — works
- [x] Restarted services — both subscribed to Alchemy WSS, multicalls now go elsewhere

## Architecture (now)
| Call | Where | CU cost on Alchemy |
|---|---|---:|
| WSS subscribe newHeads | Alchemy (push) | ~0 |
| Per-block Multicall3 getUserAccountData | LOGS_RPCS rotation | 0 |
| Watch-list refresh (60s) | LOGS_RPCS rotation | 0 |
| Executor fresh-HF + oracle prices | Alchemy (executor uses cfg.HTTP_RPC) | ~50 CU per fire |
| Executor estimateGas + broadcast | Alchemy | ~80 CU per fire |
| Fallback when all public RPCs fail | Alchemy | rare |

## Outcome
Completed 2026-05-21. Projected monthly Alchemy CU drops from ~200M ($90 PAYG) to <5M (essentially $0–5). Public RPC rotation handles the bursty per-block calls (we already validated they handle latest-tag eth_call fine; the only previously-observed rate-limit cooldown was on parallel getLogs bursts, which doesn't apply here at 1 multicall/sec/chain).

Risk: if all three public RPCs simultaneously throttle (unlikely), monitor falls back to Alchemy — function continues, just spends a few CUs. No detection gap.

## Completion
Run `/complete workflows/tasks/2026-05-21-route-multicall-off-alchemy.md`.

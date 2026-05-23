---
title: Alchemy fire-only — eliminate ALL idle Alchemy usage
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
User reports $10 burn with zero fires. Move ALL idle Alchemy usage to public RPC pool. Alchemy only when actually broadcasting a fire.

## Root causes found
1. **Alchemy WSS subscriptions** — billed per delivered message; 3 monitors × ~1 msg/sec was the heaviest line
2. **ethers JsonRpcProvider** polled `eth_blockNumber` every 4s default — across 3 executors, ~$23/mo silent
3. **rpcCall rotation included Alchemy as 1-of-N fallback** — even at 1/8 rotation, monitor traffic still touched Alchemy every 8 reads

## Steps shipped
- [x] Set `WSS_RPC = null` in hyperlend/hypurrfi/felix configs — HL/HP monitor-wss takes the HTTP-fallback branch; Felix monitor rewritten to setInterval HTTP poll
- [x] Executor `provider` URL changed from `cfg.HTTP_RPC` (Alchemy) → `cfg.LOGS_RPCS[0]` (public RPC). Polling is on public pool.
- [x] **Removed Alchemy from monitor rpcCall fallback rotation entirely.** Public RPCs only. If all 7 fail, the call errors (better than silent Alchemy billing).
- [x] All HyperEVM services restarted
- [x] **Verified: 0 active connections to any Alchemy IP after restart + 30s settle**

## Alchemy usage going forward
| Path | Hits Alchemy? |
|---|---|
| Monitor reads (HL/HP/Felix) | ❌ public pool only |
| Watch-list refreshes | ❌ public pool only |
| Indexer scans | ❌ LOGS_RPCS first, Alchemy last fallback |
| Executor provider (estimateGas, call, waitForTransaction) | ❌ public RPC |
| **Executor broadcast (eth_sendRawTransaction)** | **✅ Alchemy direct fetch** |
| Balance watcher | ❌ public pool with Alchemy fallback |

## Tradeoff
Detection latency: WSS ~100ms → HTTP polling 500-1000ms. Lose ~500ms on race-critical fires. Cost goes from ~$100+/mo to near-zero idle + small per-fire.

User chose cost. Reversible if a real winning fire opportunity demands sub-second.

## Completion
Run `/complete workflows/tasks/2026-05-21-alchemy-fire-only.md`.

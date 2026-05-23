---
title: Fix Felix rate-limit errors + find free WSS for HyperEVM
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
1. Stop Felix's `Cannot convert undefined to BigInt` rate-limit errors
2. Find a free WSS endpoint for HyperEVM (Alchemy WSS was the heaviest billed line)

## Free WSS hunt — no luck
Probed 9 candidate WSS URLs from the 7 known HyperEVM public RPCs:

| Endpoint | Connect | Block delivery |
|---|---|---|
| `wss://hyperliquid.drpc.org` | ✅ subscribes | ❌ 0 blocks in 20s |
| `wss://1rpc.io/hyperliquid` | ✅ subscribes | ❌ 0 blocks in 20s, closes |
| `wss://rpc.hyperliquid.xyz/evm` | ❌ HTTP 405 | — |
| `wss://hyperliquid.api.onfinality.io/...` | ❌ HTTP 404 | — |
| `wss://hyperliquid.rpc.blxrbdn.com/` | ❌ HTTP 405 | — |
| `wss://hyperliquid-json-rpc.stakely.io` | ❌ HTTP 429 | — |
| `wss://999.rpc.thirdweb.com/` | ❌ HTTP 302 | — |
| `wss://hyperliquid.api.pocket.network/` | ❌ HTTP 400 | — |

**Verdict: no working free WSS for HyperEVM.** drpc/1rpc accept subscriptions but deliver no notifications (likely abandoned WSS support or paywalled silently). The only WSS that delivers is Alchemy, which bills per-message — that's why we killed it.

Sticking with HTTP polling on the public pool.

## Felix rate-limit fix
Felix has 39 markets × ~3 reads per market per block = ~117 RPC calls per block. At 1s cadence this saturated the 7-RPC pool. Two changes:
- [x] **Drop poll cadence from 1s → 3s** in `felix/monitor.js` block loop. Cuts load 3×.
- [x] **Suppress noisy transient errors** (`rate limited`, `Too Many`, `undefined to a BigInt`, `all public RPCs`). Count them silently; print a summary every 30 blocks if any occurred. Real errors still log.

## Outcome
Completed 2026-05-21. Felix monitor restarted with 3s cadence + error filter. Confirmed:
- Zero fresh errors logged in the visible window post-restart
- Service stable, 0 active Alchemy connections held
- HF sweep still produces 217 positions correctly

Detection latency on Felix: ~3-4s (was 1-2s on WSS). Acceptable tradeoff for the cost saving.

If we ever need sub-second Felix detection, the only path is paid Alchemy WSS or a custom paid provider — there's no free WSS option for HyperEVM as of May 2026.

## Completion
Run `/complete workflows/tasks/2026-05-21-fix-felix-rate-limits-and-find-free-wss.md`.

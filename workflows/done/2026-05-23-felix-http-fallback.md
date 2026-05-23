---
title: Add HTTP-poll fallback to Felix monitor (purroofgroup WSS stale incident)
created: 2026-05-23
completed: 2026-05-23
status: done
---

## Goal
Fix Felix's stale-WSS exposure after morning stall incident.

## Steps
- [x] Investigate stalls — purroofgroup stale by ~1700 blocks, Alchemy/stakely/blxrbdn live
- [x] Probe user-suggested WSS endpoints — dRPC rejects newHeads with "Unsupported subscription" code 23; OnFinality 429 rate-limited
- [x] Per user constraint: Alchemy reserved for fires only; need free public source for monitor
- [x] Port HyperLend's HTTP-poll fallback into Felix monitor-wss.js
- [x] WSS_URL default null (engages fallback); heartbeat message dynamic per source
- [x] Restart Felix, verify head-tracking restored

## Outcome

Completed 2026-05-23 ~09:08 UTC. Felix now uses HTTP-poll fallback via cfg.LOGS_RPCS
rotation (rpc.hyperliquid.xyz/evm + stakely + blxrbdn + others). Confirmed at block
35860030 immediately after restart — current chain head, fully recovered from 1700-block lag.

### Code changes in `/home/jojo/automation/felix/monitor-wss.js`
1. **Line 44** — `WSS_URL` default changed from `'wss://rpc.purroofgroup.com'` to `null`.
   Set `HYPEREVM_RPC_WSS` env var to re-enable WSS.
2. **Line ~194** — added `startHTTPFallback()` modeled on HyperLend's pattern.
   1s polling via `rpcCall('eth_blockNumber')` which rotates through `LOGS_RPCS`.
3. **`connectWSS()`** now checks `if (!WSS_URL) return startHTTPFallback();`
4. **Heartbeat** message now says "Felix WSS stalled" or "Felix block-poll stalled"
   depending on actual source — matches HyperLend/HypurrFi alert wording.

### Why this fix is more robust than picking a new WSS endpoint
- No free HyperEVM WSS provider currently supports `eth_subscribe newHeads` AND has
  reliable head-tracking (purroofgroup goes stale; dRPC rejects sub; OnFinality 429s)
- Public HTTP pool has 6 endpoints with rotation on failure — single-provider
  outage doesn't take Felix down
- HyperLend/HypurrFi already proved this pattern works (recovered within 60s today
  while Felix would have been stuck indefinitely)

### Lessons
- WSS without HTTP fallback = single point of failure
- When WSS provider exists, design for failure: dual-source or auto-fallback
- "WSS stalled" alerts where the source is stuck consuming stale blocks (not
  disconnected) are silent — heartbeat detects the gap but reconnect to the same
  stale source doesn't help. The fix is source-switching, not reconnect.

## Skill candidate evaluation
- Technologies/frameworks touched: HyperEVM public RPC pool (purroofgroup, stakely, blxrbdn, onfinality, thirdweb), Felix monitor-wss.js, WSS newHeads subscription, dRPC limitations, OnFinality rate limits
- Domain-specific knowledge involved: HyperEVM free WSS landscape (most are HTTP-only or don't support newHeads); the "stale WSS source" failure mode where reconnect doesn't help because the source itself is broken; HTTP-poll fallback as the design pattern that survives ANY WSS source going stale
- Verdict: **GENERATE**
- Reason: Adds the WSS-source-stale failure mode (distinct from connection-drop) and the HyperEVM-specific WSS provider landscape audit (2026-05-23 snapshot). Reusable on any chain where free WSS options are sparse.

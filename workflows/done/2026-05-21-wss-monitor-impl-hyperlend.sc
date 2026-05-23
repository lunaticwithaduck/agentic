---
domain: hyperevm
source_task: 2026-05-21-wss-monitor-impl-hyperlend.md
date: 2026-05-21
keywords: ["wss", "websocket", "newHeads", "alchemy", "subscription", "race-bot", "reconnect", "heartbeat"]
---

## Extracted Knowledge

### Alchemy WSS `newHeads` on HyperEVM (free tier)
- `eth_subscribe("newHeads")` works on free tier — confirmed 19 blocks in 20s with ~1s spacing
- Subscription response shape: `{jsonrpc:"2.0",method:"eth_subscription",params:{subscription:"0x…",result:{number:"0x…",hash:"…",…}}}`
- Use the `ws` package (Node), not ethers' WebSocketProvider — ethers v6 adds overhead and harder to reconnect cleanly
- Single account can hold multiple WSS connections; we tested 2 simultaneously without issue

### Watch-list pattern
For block-driven monitor on chains with many borrowers, don't multicall ALL borrowers on every block:
- Maintain a watch list of users with HF below a threshold (e.g. HF<1.20)
- Refresh the watch list every 60s via HTTP full sweep
- On each WSS block, only multicall the watch list (typically 5-50 users vs 50-500 total)
- Tradeoff: a user moving from HF=2.0 to HF=0.99 in one block (oracle dump) is missed until next watch-list refresh. Acceptable for normal HF drift; not for black-swan moves.

### Reconnect strategy
- WSS connections drop silently sometimes (NAT, idle timeout, server-side flap)
- Pattern: on `close` event, schedule reconnect with `setTimeout(connectWSS, delay)`, double delay up to 30s cap
- Heartbeat detection: separately track `lastBlockAt`. If `Date.now() - lastBlockAt > 10s`, force-close the WS (will trigger reconnect)
- Telegram alert on heartbeat trip; dedup 5-min to avoid alert storms

### Per-block evaluation must not block the next block
```js
ws.on('message', raw => {
  if (m.method === 'eth_subscription') {
    evalBlock(blockNum).catch(e => log('err:', e.message));  // fire-and-forget
  }
});
```
At ~1s block times, evalBlock takes 300-500ms (multicall + decode). If we awaited each before the next message, we'd queue and fall behind. Don't await.

### Armed file directory is the integration point
- HTTP monitor and WSS monitor BOTH write to `data/armed/<key>.json`
- Executor's `fs.watch` fires on either source
- Write is idempotent (overwrite) — duplicate arms don't break anything
- Whichever monitor sees the HF drop first wins; we run them both for resilience

### Watch threshold tuning
- HF<1.10 caught 0 users (no one was close)
- HF<1.20 caught 3 users (more sensible)
- Too tight → miss the wider drift band before a real fire
- Too wide → unnecessary RPC load. At ~1s blocks with 50 users in watch, that's ~3000 calls/hour — within Alchemy free tier
- Tune per-chain based on observed HF distribution

## Proposed Skill Content
Add to `hyperevm` skill under a new "WSS-driven monitor architecture" section:
- Alchemy free tier supports `newHeads` subscription on HyperEVM
- Use `ws` package for low overhead; ethers WebSocketProvider has more reconnect complexity
- Watch-list pattern: filter to HF<1.20 users, rebuild every 60s
- Fire-and-forget evalBlock so block N+1 processing doesn't queue behind block N
- Heartbeat + force-reconnect when no block in 10s
- Run alongside HTTP monitor for resilience; both write to same `data/armed/` dir

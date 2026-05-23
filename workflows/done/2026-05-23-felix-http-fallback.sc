---
domain: defi-liquidations
source_task: 2026-05-23-felix-http-fallback.md
date: 2026-05-23
keywords: [stale-wss, http-fallback, hyperevm-wss-providers, drpc-no-newheads, onfinality-429, purroofgroup-stale, source-switching, monitor-resilience]
---

## Extracted Knowledge

### "Stale WSS source" is distinct from "WSS disconnect" — reconnect doesn't help
A WSS endpoint can be CONNECTED (open socket, sending messages) but serving STALE blocks (lagging chain head by hundreds-thousands of blocks). Heartbeat code that triggers `ws.close()` + reconnect-on-disconnect treats this as a transient drop and reconnects to the same broken source — Felix experienced 1700-block lag while its WSS was "healthy" from the socket's POV.

**Detection signature:** monitor processes blocks at sub-chain-head numbers while other monitors on the same chain are at head. Compare your monitor's `lastBlockNumber` log line against a reference RPC's `eth_blockNumber` — gap > ~50 blocks = stale source.

**Fix pattern:** when heartbeat detects gap, **switch sources** (not reconnect). Easiest implementation: HTTP-poll fallback on a different provider pool that immediately replaces the stale stream.

### HyperEVM free-WSS landscape (2026-05-23 snapshot)
Free public WSS providers for HyperEVM evaluated:

| Provider | Status |
|---|---|
| `wss://rpc.purroofgroup.com` | Connects + supports newHeads, but **goes stale** (saw 1700-block lag) |
| `wss://hyperliquid.drpc.org` | Connects, but rejects newHeads with `"Unsupported subscription: newHeads"` (code 23) |
| `wss://hyperliquid.api.onfinality.io/evm/public-ws` | 429 rate-limited (per public docs and confirmed in probe) |
| `wss://hyperliquid-json-rpc.stakely.io` | HTTP-only, no WSS |
| `wss://hyperliquid.rpc.blxrbdn.com/` | HTTP-only, 405 on WSS upgrade |
| Alchemy paid WSS | Works, but per-message billing on Alchemy reserves it for fires only in our setup |

Conclusion: **no free HyperEVM WSS provider is currently reliable** for monitor use. The robust pattern is HTTP polling via a rotated public RPC pool. WSS is opt-in via env var.

### HTTP-poll fallback pattern (model: HyperLend → ported to Felix)
```js
const WSS_URL = process.env.HYPEREVM_RPC_WSS || null;

async function startHTTPFallback() {
  log('⚠️ no WSS_URL — falling back to 1s HTTP poll on public RPC pool');
  let seen = 0;
  while (true) {
    try {
      const n = parseInt(await rpcCall('eth_blockNumber', []), 16);
      if (n > seen) {
        seen = n;
        onNewBlock(n).catch(e => log('onNewBlock err:', e.message));
      }
    } catch (e) { log('http-poll err:', e.message.slice(0, 80)); }
    await new Promise(r => setTimeout(r, 1000));
  }
}

function connectWSS() {
  if (!WSS_URL) return startHTTPFallback();
  // ... existing WSS code
}
```

`rpcCall` already rotates through `cfg.LOGS_RPCS` on failure, so any single public RPC going stale automatically rotates to the next. 1s polling is fine — HyperEVM blocks are ~1-2s.

### Dynamic stall message — say what kind of source actually failed
After porting fallback, the heartbeat alert should reflect WHICH source stalled:
```js
const src = WSS_URL ? 'WSS' : 'block-poll';
tg(`⚠️ Felix ${src} stalled ${(gap/1000).toFixed(0)}s (last block ${lastBlockNumber}) — public RPC pool issue`);
```
Avoids confusion when reading the alert at 11am ("Felix WSS stalled" — but we're not even on WSS).

### Reserve paid RPC for fires, never for monitor block-poll
Pattern from this fleet: Alchemy HyperEVM paid HTTP plan is reserved for executor broadcasts (eth_sendRawTransaction). Monitor block-polling uses the free public LOGS_RPCS pool with rotation. Why:
- Monitor polls every 1s = 86k req/day per chain = blows through Alchemy CU budget
- Executor fires are sparse (hours apart) — paid RPC justified for reliability under that workload
- Public RPC pool with rotation handles monitor-scale traffic for free
- Single-provider failure on monitor side just rotates; no Alchemy spend either way

When designing a new chain lane, **explicitly split** RPC roles in config:
- `LOGS_RPCS` — public pool, used for monitor block-poll + log scans + position reads
- `HTTP_RPC` — paid (Alchemy/QuickNode/dRPC), used for executor fires only

## Failure Modes Observed

### Stale-WSS lag while "connected" caused 25-min monitor blindness on $292k arm
This morning: purroofgroup WSS stayed connected and delivered messages, but its block stream was 1500-1700 blocks behind chain head. Felix's heartbeat detected the gap after 61s, triggered `ws.close() + reconnect`, but the reconnect went to the same broken source. Felix continued at stale blocks for ~25 min while a $292k position was armed. The fix wasn't reconnecting — it was switching sources entirely.

Lesson: **heartbeat-triggered reconnect is necessary but not sufficient.** Add a "switch source" path that engages on persistent staleness, not just disconnect.

### Hardcoded WSS default that "usually works" defeats the failure model
Felix's code had `const WSS_URL = process.env.HYPEREVM_RPC_WSS || 'wss://rpc.purroofgroup.com';` — the hardcoded default meant the env-var-unset case still tried purroofgroup. Better default: `null`. Force the operator to explicitly opt into WSS by setting the env var, and use HTTP fallback as the safe default for new deployments.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extending the monitor architecture section):

### Stale-WSS source is a distinct failure mode from WSS disconnect
A WSS endpoint can stay connected while serving stale blocks. Heartbeat-triggered `ws.close()+reconnect` doesn't fix it — the broken source accepts the reconnect and continues serving stale data. Always pair WSS with a source-switching path (HTTP poll on a different provider pool).

### HTTP-poll fallback is the resilient default for monitors
For any chain where free WSS providers are unreliable or rate-limited, default to HTTP polling via a rotated public RPC pool. Engage WSS only when an env var explicitly opts in. The monitor loses ~1s of latency vs WSS but gains immunity to single-provider failure.

### Always split paid vs free RPC roles in config
`LOGS_RPCS` (free public pool, rotated) for monitor + log scans. `HTTP_RPC` (paid) for executor fires only. Saves CU budget AND survives free-pool outages without escalating to paid.

### Dynamic stall message reflects actual source
After implementing fallback, ensure the heartbeat alert reports `WSS` or `block-poll` depending on which is actually active. Static "WSS stalled" wording lies after fallback engages and confuses the on-call.

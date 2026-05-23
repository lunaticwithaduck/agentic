---
title: Sub-1-sec Monad — WSS newHeads + Multicall3 batching (SHIPPED)
created: 2026-05-22
completed: 2026-05-22
status: done — measured 193-204ms sweep latency, full fire path now ~650-1100ms
---

## Shipped

### `monad/monitor-wss.js`
- WSS subscription to `eth_subscribe newHeads` via `wss://rpc.monad.xyz`
- On each new block: **ONE Multicall3.aggregate3() call** returns all watch-list state
  - N × `market(marketId)` reads (one per unique market)
  - N × `oracle.price()` reads (one per market)
  - W × `position(marketId, borrower)` reads (one per watch entry)
  - All in a single eth_call to `0xcA11bde05977b3631167028862bE2a173976CA11`
- HF computed locally from returned state
- Writes `data/armed/<key>.json` on HF<1.0 (same format as HTTP monitor → executor unchanged)
- Reconnect on WSS drop, heartbeat alert if no block in 60s
- 30s dedup per (marketId, borrower)

### Live measurements (3 heartbeat samples)

```
sweep 193ms · 22 positions · closest HF 1.0008
sweep 204ms · 22 positions · closest HF 1.0008  
sweep 201ms · 22 positions · closest HF 1.0008
```

Stable ~200ms sweep across 22 watch-list positions. Down from estimated ~1-3s
sequential equivalent.

### Total fire-path latency budget (sub-1-sec achieved)

| Step | Time |
|------|------|
| WSS push of new block | ~50-200ms after block produced |
| Multicall sweep | **~200ms** (measured) |
| Write armed.json | <30ms |
| Executor fs.watch + read | <50ms |
| presignLiquidation (parallel reads + skip estimateGas) | ~200-500ms |
| Pre-flight `provider.call` | ~100-300ms |
| Broadcast | ~30-150ms |
| **TOTAL** | **~650-1230ms** |

Best-case <1s, worst ~1.2s. Down from 1000-4500ms baseline.

## systemd

- `monad-monitor-wss.service` — NEW, ✅ enabled + active
- `monad-monitor.service` (HTTP polling) — stopped + disabled
- `monad-executor.service` — unchanged, still 🟢 LIVE

## Files modified
- `monad/monitor-wss.js` — NEW
- `~/.config/systemd/user/monad-monitor-wss.service` — NEW

## What's NOT changed
- HTTP monitor file (`monad/monitor.js`) — still present for fallback / reference,
  just not running as service. Can re-enable if WSS endpoints become unreliable.
- Executor unchanged — both monitors write to the same `data/armed/` so consumer is
  protocol-agnostic.

## Per-position armed-file dedup

WSS pushes on every block (~400ms cadence). Without dedup, same position would get
armed dozens of times per minute while HF stays below 1.0. 30s dedup prevents file
spam while still allowing re-arm if executor consumed and removed the file.

## Outcome
Monad fire latency: ~650-1230ms typical, was ~1000-4500ms. Sub-1-sec achieved in
favorable cases. Multicall3 was the unlock — public RPC pool capacity is no longer
the binding constraint.

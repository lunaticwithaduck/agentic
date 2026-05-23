---
title: Tydro latency upgrade — Monad-level optimizations applied
created: 2026-05-22
completed: 2026-05-22
status: done — Tydro now at sub-second fire latency
---

## Shipped

### 1. WSS + Multicall3 monitor (`tydro/monitor-wss.js`)
- Subscribes to `wss://rpc-qnd.inkonchain.com` for push-based block detection
- On each new block: ONE `Multicall3.aggregate3()` returns ALL watch-list users'
  `getUserAccountData` (HF + debt + collateral). 91 users batched in one RPC call.
- Same `fire` flag mechanism as Monad: writes armed file at HF<1.05 with `fire: <bool>`
- 30s dedup, bypassed on fire transition (HF<1.0)

### 2. Pool cache (`tydro/swap-path.js`)
- `findBestPoolWithCache` wraps `findBestPool` with 1h TTL
- First call discovers, subsequent calls 0ms

### 3. Executor optimizations (`tydro/executor.js`)
- **Skip estimateGas** — hardcoded 1.9M ceiling (Ink small-block cap is 2M)
- **Parallel reads** — `Promise.all` on nonce + feeData
- **Skip pre-flight** `provider.call` — saves 100-300ms
- **Pre-sign cache** — Map<filename, {presigned, builtAt}> with 60s TTL
- **Pre-warm broadcast** — eth_chainId at startup to amortize TLS handshake
- **HF arm zone** — accept HF in [1.0, 1.05) for arm-path presign (was bailing on ≥1.0)
- **fs.watch** listens for both 'rename' and 'change' events

### 4. systemd
- `tydro-monitor-wss.service` — NEW, ✅ enabled + active
- `tydro-monitor.service` — stopped + disabled
- `tydro-executor.service` — restarted, ✅ active

## Live measurements

```
Monitor sweep:   153ms (91 users via Multicall3)
Cold presign:    2-4s  (Slipstream pool discovery — off hot path)
Cached presign:  0ms   (cache hits visible: ⚡ cached presign (built Xms ago))
```

### Latency budget projection

| Step | Time |
|------|------|
| WSS push | ~50-200ms |
| Multicall sweep | ~150ms |
| Write armed.json | <30ms |
| fs.watch + read | <50ms |
| Presign (cache hit) | 0ms |
| Pre-flight | 0ms (skipped) |
| Broadcast | 30-150ms |
| **TOTAL** | **~260-580ms** |

Same profile as Monad. The Tydro $20M whales (HF 1.04) will now race at sub-second
latency when they cross.

## Why these whales matter
- `0x2ce8733ed6…` — **$12.36M** at HF 1.0414
- `0xc9cdcd2538…` — **$7.71M** at HF 1.0426
- `0xc99f25ac24…` — $1.22M at HF 1.0221
- `0x489c82a820…` — $290k at HF 1.0490
- `0x933a7c11cc…` — $227k at HF 1.0218

If wstETH/kBTC drops ~5%, these all cross HF<1.0. With LIF ~5-7% on Tydro, the
$12M whale alone could be ~$600k of profit if we win the race.

## Files modified
- `tydro/config.js` — WSS_RPC default to `wss://rpc-qnd.inkonchain.com`
- `tydro/monitor-wss.js` — NEW (180 lines, mirrors HyperLend pattern)
- `tydro/swap-path.js` — added `findBestPoolWithCache`
- `tydro/executor.js` — presign cache, skip pre-flight, skip estimateGas, parallel reads, pre-warm, HF arm zone, fs.watch update
- `~/.config/systemd/user/tydro-monitor-wss.service` — NEW

## Effort
~3 hours (predicted 4-5h). Saved time by lifting templates from Monad/HyperLend directly.

## Outcome
Tydro lane latency: same ~260-580ms profile as Monad. The fleet's two
race-optimized lanes (Tydro + Monad) cover the largest exposed positions across
all chains. Felix/Bend/HyperLend/HypurrFi/Sonic remain on older HTTP-polling
architecture for now — can be upgraded if they ever lose meaningful races (which
we have no current evidence of).

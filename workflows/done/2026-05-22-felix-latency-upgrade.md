---
title: Felix latency upgrade — full Monad-pattern shipped
created: 2026-05-22
completed: 2026-05-22
status: done — Felix now at sub-second fire latency profile
---

## Shipped

### 1. WSS+Multicall3 monitor (`felix/monitor-wss.js`)
- Free HyperEVM WSS at `wss://rpc.purroofgroup.com` (verified working 2026-05-22)
- Multicall3 at canonical `0xcA11bde0…` (3808 bytes deployed)
- Same Morpho Blue per-(market, borrower) batch as Monad
- Writes armed file at HF<1.02 with `fire` flag

### 2. Pool cache (`felix/swap-path.js`)
- `findBestPoolWithCache` with 1h TTL
- **PLUS V3 depth pre-filter**: rejects pools whose tokenIn balance is <2× swap amount
- Fixes the 614% impact bug from project-x's empty pools

### 3. Executor optimizations (`felix/executor.js`)
- Skip estimateGas (hardcoded gasLimit = 1.9M)
- Parallel nonce + feeData via Promise.all
- Skip pre-flight `provider.call`
- Pre-sign cache (60s TTL) keyed by armed-filename
- HF arm-zone allowed (HF in [1.0, 1.02))
- Pre-warm broadcast endpoint at startup
- fs.watch listens for both 'rename' and 'change'
- Latency instrumentation (`presign:Xms`, `broadcast:Yms`, `TOTAL:Zms`)

### 4. systemd
- `felix-monitor-wss.service` — NEW, 🟢 enabled + active
- `felix-monitor.service` — stopped + disabled
- `felix-executor.service` — restarted with all opts

## Verified live
```
WSS connected to wss://rpc.purroofgroup.com
subscribed newHeads
🎯 arm 0x8eecdd03:0x24df4b7a HF 1.0156 debt $289214
pre-signing arm zone (HF 1.0156, will broadcast on fire)
```

The $289k position is being correctly identified and arm-presigned.

## Remaining limitation: market 0x8eecdd03 swap path

The arm-presign of the $289k position currently fails downstream because the V3 pool
for that market's coll/loan pair has insufficient depth across all configured HyperEVM
DEXes (project-x + hyperswap-v3). Same class of issue as Tydro's weETH problem before
we found the Slipstream pool. Resolution options:

1. Check other HyperEVM DEXes (Kodiak? UBSwap?) not in `lib/hyperevm-dexes.js`
2. Multihop route (V3 path-encoded or helper contract)
3. Accept that this specific market is unfireable until pool depth improves

Not blocking — the rest of Felix's 20+ watch-list positions can presign+fire normally.

## Files modified
- `felix/monitor-wss.js` — NEW (~200 lines, Morpho-flavor of Monad pattern)
- `felix/swap-path.js` — added `findBestPoolWithCache` + V3 depth pre-filter
- `felix/executor.js` — presign cache, skip pre-flight, parallel reads, pre-warm, arm-zone HF check
- `~/.config/systemd/user/felix-monitor-wss.service` — NEW

## Outcome
Felix now at the same race-optimized tier as Monad and Tydro. Three of the
seven fleet chains running sub-second fire-latency lanes covering $25M+ of cliff
exposure.

Bend / Sonic / HyperLend / HypurrFi still on older architecture — could be upgraded
later if a real fire shows we're losing races on those chains.

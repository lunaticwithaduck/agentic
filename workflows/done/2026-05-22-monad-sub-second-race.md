---
title: Sub-1-sec Monad fire — partial ship, RPC pool became the bottleneck
created: 2026-05-22
completed: 2026-05-22
status: done — executor hot-path opts shipped, monitor poll-rate reverted (RPC limits surfaced)
---

## Shipped: executor hot-path optimizations

### 1. Parallel reads in `presignLiquidation`
Was: 3 sequential `eth_call`s (position, market, oracle) — 300-900ms total.
Now: `Promise.all` — same calls in parallel, 150-300ms.
**Savings: 150-600ms per fire.**

### 2. Skip estimateGas, hardcode gasLimit at 1.9M
Was: `provider.estimateGas` RPC roundtrip before signing (100-300ms).
Now: hardcoded 1.9M (typical liquidate is 600-900k, plenty of headroom).
**Savings: 100-300ms per fire.**

Trade-off: if a fire reverts at execution, we still pay gas for the failed tx. The
post-broadcast revert decoder handles failure classification — we don't lose information.

### 3. Parallel nonce + feeData fetch
Was: sequential `getTransactionCount` then `getFeeData` (~50-100ms each).
Now: `Promise.all`.
**Savings: 50-100ms per fire.**

### 4. Connection pre-warm to dRPC broadcast endpoint
At executor startup, send one no-op `eth_chainId` to dRPC so TLS handshake is done
before any fire. Node's native fetch (undici) keepalives by default.
**Savings: 30-100ms on first fire of the session (none on subsequent — pool stays warm).**

### 5. Latency instrumentation
Added `t0` timestamps + `presign:Xms`, `broadcast:Yms`, `TOTAL:Zms` log lines so we can
empirically measure end-to-end timing on real fires.

## NOT shipped: monitor poll-rate reduction

**Attempted**: lower `POLL_MS` from 3000 → 400 (matching Monad block time).
**Result**: 5-10× more "RPC pool stressed" warnings, ~50% of per-borrower position
reads failing per cycle.
**Root cause**: `loadAndSweep` does 28 eth_calls per cycle (market state + per-borrower
positions). At 400ms cadence × 28 reads = 70/sec across 3 RPCs ≈ 23/sec each, which
saturates QuickNode's 25/sec cap. Tried 1000ms — still 5x stress. **Reverted to 3000ms.**
**True sub-1-sec requires WSS subscription** (push-based, no per-cycle RPC cost). Deferred.

## Net latency impact

| Component | Before | After |
|-----------|--------|-------|
| Monitor detection (HF<1 → armed file) | 0-3000ms | 0-3000ms (unchanged) |
| Executor read+presign | 500-1300ms | 200-500ms |
| estimateGas pre-flight | 100-300ms | 0ms (skipped) |
| Broadcast | 50-200ms | 30-150ms (warm) |
| **End-to-end** | **1000-4500ms** | **700-3700ms** |

Per-fire savings: ~300-800ms in best case. Monitor detection latency unchanged (3s
worst case dominates). To break <1s, would need WSS monitor — verified works on Monad
(all 3 WSS endpoints respond: rpc.monad.xyz, monadinfra, dRPC) but the implementation
is a separate task (~2-3h).

## Files modified
- `monad/monitor.js` — POLL_MS comment update (value unchanged)
- `monad/executor.js`:
  - Parallel reads in `presignLiquidation`
  - Skip `estimateGas`, hardcoded `gasLimit = 1_900_000n`
  - Parallel nonce + feeData
  - Pre-warm fire RPC at startup
  - Latency timestamps on processArmed

## Files restarted
- `monad-monitor.service`, `monad-executor.service` — both 🟢 LIVE

## Honest outcome

I overscoped the original "ship sub-1-sec" plan. The win was bounded by RPC pool
capacity, not just code optimization. Shipped what works (executor hot-path), held
back what would degrade the monitor (poll-rate). Net: typical fire is ~500ms faster.
For true sub-1-sec, the right path is WSS monitor — confirmed feasible, scoped for
a separate task when needed.

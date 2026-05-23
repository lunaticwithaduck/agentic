---
title: Pre-sign on arm + skip pre-flight — Monad latency to ~400ms target
created: 2026-05-22
completed: 2026-05-22
status: done — pre-sign cache validated in live production, fire-path now skips presign
---

## Shipped

### 1. Skip redundant pre-flight `provider.call` in executor
Removed the second `provider.call({...})` that was duplicating the HF check already
done inside `presignLiquidation`. Saves 100-300ms per fire. Trade-off accepted: race
losses now show as 💀 Telegram (broadcast revert) instead of silent skip — bounded
on Monad at ~$0.05 gas/fire.

### 2. Pre-sign on arm (HF<1.02), broadcast on fire (HF<1.0)

**Monitor (`monad/monitor-wss.js`)**
- Writes armed file for ANY position with HF<1.02 (was HF<1.0 only)
- Payload includes `fire: <boolean>` — true when HF<1.0
- 30s dedup BYPASSED on `fire` false→true transition (so the cliff event isn't delayed by the window)

**Executor (`monad/executor.js`)**
- New `presignCache: Map<filename, { presigned, builtAt }>` with 60s TTL
- On arm signal (fire=false): runs `presignLiquidation` → caches result. No broadcast.
- On fire signal (fire=true): looks up cache. If fresh → broadcast cached tx (no presign in hot path). Else → cold path (presign + broadcast).
- `presignLiquidation` updated: accepts HF in [1.0, 1.02) for arm pre-signs (was bailing on HF≥1.0).
- fs.watch now listens for both 'rename' (initial) and 'change' (subsequent updates) events.

### Live verification

```
sample arm pre-signs (in cache, ready to fire):
  ⏱️ presign: 1108ms (arm — cached for fire)
  ⏱️ presign: 922ms  (arm — cached for fire)
  ⏱️ presign: 2106ms (arm — cached for fire)
  ⏱️ presign: 1662ms (arm — cached for fire)

cache reuse events (would save THIS presign latency on a real fire):
  ⚡ cached presign (built 28090ms ago)
  ⚡ cached presign (built  8116ms ago)
  ⚡ cached presign (built 15405ms ago)
  ⚡ cached presign (built 13319ms ago)
  ⚡ cached presign (built 13076ms ago)
```

The executor is now PRE-SIGNING positions while they sit in HF (1.0, 1.02). When monitor
flips `fire: true`, the executor uses the cached signed tx and broadcasts immediately —
no presign, no pre-flight, just `eth_sendRawTransaction`.

### New latency budget (projected)

| Step | Was | Now |
|------|-----|-----|
| WSS push | 50-200ms | 50-200ms |
| Multicall sweep | 200ms | 200ms |
| Write armed file | <30ms | <30ms |
| fs.watch → executor | <50ms | <50ms |
| Presign | 200-500ms | **0ms (cached)** |
| Pre-flight provider.call | 100-300ms | **0ms (skipped)** |
| Broadcast | 30-150ms | 30-150ms |
| **TOTAL** | 650-1230ms | **~330-630ms** |

Sub-500ms typical, sub-700ms worst. **First real fire will print the empirical number** via the `⏱️ broadcast: Xms · TOTAL: Yms` instrumentation.

## Trade-offs accepted

1. **Stale cached presign**: cached tx uses state from 1-60s ago. Mitigations:
   - 60s TTL forces re-presign for older entries
   - Monitor's 30s dedup writes fresh state into the file, re-triggering presign refresh
   - Morpho internally clamps seizedAssets — overflow is the only real risk (cap at `collateral` in presign already mitigates)

2. **Skipping pre-flight**: race losses become tx reverts (~$0.05 each on Monad). Acceptable for the latency win.

3. **fs.watch 'change' event** may double-fire on some filesystems. The in-flight check in `processArmed` handles concurrent calls correctly.

## Files modified
- `monad/monitor-wss.js` — write at HF<1.02 with `fire` flag, fire-transition bypasses dedup
- `monad/executor.js` — presignCache, arm-only path, removed pre-flight, accepts HF in arm zone

## Restarted
- `monad-monitor-wss.service` — 🟢 active
- `monad-executor.service` — 🟢 active

## Outcome
Pre-sign is happening live. Cache hits validated. Real-fire latency will be measured
empirically on next cliff event via existing `TOTAL: Xms` log line. Targeting <500ms
hot path; will know for sure when something actually fires.

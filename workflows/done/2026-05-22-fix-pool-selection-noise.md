---
title: Fix V3 pool selection — depth filter + retry-on-null for multihop flake
created: 2026-05-22
completed: 2026-05-22
status: done — both Telegram-spam issues resolved
---

## Issues observed (user-reported Telegram spam)

```
⏭️ Monad skip 0x8bdb7d2c:0x04480… — pool impact 13121998999.7%
⏭️ Monad skip 0x8bdb7d2c:0x53cc1… — pool impact 18792569053.6%
⏭️ Monad skip 0x8bdb7d2c:0x7902b… — pool impact 12799113173.9%
⏭️ Monad skip 0xc4504d2b:0x65548… — no pool 0x103222→0x754704 explain those
```

## Root causes

### Issue 1: pool impact in billions of percent
PancakeSwap V3 quoter on Monad returns spot-price-based quotes that ignore actual
pool depth. For wstETH/WETH pair, PCS V3 has a near-empty pool but quotes "1:1 at
0% slippage". `findBestV3` ranks by `expectedOut` — PCS V3's lying quote beats Uni
V4's realistic quote. Executor's downstream depth check catches it and skips, but
every block re-arms emit a new skip Telegram with a different bogus number.

### Issue 2: "no pool" earnAUSD/USDC despite multihop being live
The multihop V4+Curve route works at most sizes but is flaky due to RPC-pool
saturation. `findBestDirect` and `findBestMultihop` run in parallel, each firing
~13 concurrent RPC calls (V3 fee tiers × 2 calls + V4 fee tiers + Curve probe).
Combined ~40 parallel calls saturate the public RPC pool. Some calls fail → multihop
returns null → fallback halves to smaller size → smaller size also returns null.

## Fixes

### A. Depth filter in `findBestV3` (`monad/swap-path.js`)
Before accepting a V3 pool result, call `tokenIn.balanceOf(pool)` and require it to be
at least 2× the swap amount. Pools that can't physically absorb the swap get filtered
out at SELECTION time, not at executor's depth check. PCS V3's empty pool is now
correctly skipped during pool ranking.

### B. Retry-on-null in `findBestPoolWithFallback` (`monad/swap-path.js`)
Before halving on null, retry the same amount once after a 500ms breather. RPC
saturation typically clears within a second. The retry catches transient flakes
without dropping to a smaller seize chunk.

### C. Coarse-bucket dedup in `tgSkip` (`monad/executor.js`)
Skip reason is now bucketed before dedup: "pool impact 13.1B%" and "pool impact
18.7B%" both bucket to `"pool impact (depth-limited)"`. One Telegram per 5min per
(market, borrower) instead of one per cycle per unique percentage.

## Verification (live)

```
last 5 min of executor activity:
  swap impact warnings: 0   (was many per minute)
  no V3/V4 route errors: 0  (was several per cycle)
  cached presign hits: 49   (cache reuse working)
  presign latencies: 2-6s   (slower due to depth check, but off hot path)
```

User's Telegram should now be quiet on these patterns.

## Trade-off accepted
Depth check adds 1 extra eth_call per V3 fee tier per find call (~8 extra reads
on each Monad sweep). Increases presign time from ~1-2s to ~3-6s. Acceptable
because presign happens on ARM (off hot path), not on FIRE.

## Files modified
- `monad/swap-path.js` — `findBestV3` depth filter, `findBestPoolWithFallback` retry
- `monad/executor.js` — `tgSkip` bucketReason

## Outcome
Both Telegram-spam patterns resolved. Executor still 🟢 LIVE. Pre-sign cache + depth
filter combine cleanly: bot now picks correct (Uni V4) pool on every cycle and doesn't
emit junk skip alerts for transient RPC flakes.

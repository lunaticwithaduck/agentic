---
title: weETH → WETH multihop investigation — NO CODE NEEDED (false alarm)
created: 2026-05-22
completed: 2026-05-22
status: done — no work shipped, pool exists, earlier "no pool" was RPC flakes
---

## Goal (re-stated)
Build multihop weETH→WETH route on Tydro to fix executor's "no pool" Telegram skip
alerts.

## Investigation — pool DOES exist

Direct factory.getPool probe on Slipstream Ink:
```
tickSpacing=1    → 0xf9349c5af43d2abc2758e90cfb341722116fac38   ($2M TVL)
tickSpacing=50   → null
tickSpacing=100  → null
tickSpacing=200  → null
tickSpacing=2000 → null
```

CoinGecko confirms: `weETH / WETH 0.01%` pool with $2.0M TVL on
`velodrome-finance-slipstream-ink`. Direct route exists.

`findBestPool(weETH, WETH)` returns the pool correctly with expectedOut = 1.09 WETH
per weETH (correct — accounts for weETH's staking yield).

## Root cause of the false "no pool" alerts
The earlier "weETH→WETH (no pool)" alerts were transient RPC failures from BEFORE the
pool cache was added. The pool cache (shipped earlier today) caches successful
discoveries with 1h TTL, eliminating the RPC-saturation race.

## Verified working post-cache

Latest executor activity (since restart at 19:38):
```
chose pair: weETH→WETH (pool 0xF9349C5aF43D2abC2758e90Cfb341722116fac38 fee 1)
chose coll: weETH (581.5843 = $1,315,528.21)
expected seized: 297.2985 weETH ($672,481.39)
```

That's the $1.2M whale `0xc99f25ac24…` being correctly presigned with the right pool.

## Cache stats
27 cold presigns, 38 cache hits → 58% reuse rate within minutes of restart.

## Lesson captured
**Investigate before building.** If we'd jumped straight to deploying a MultiHopSwapper
on Ink + wiring V3 path-encoded routes, we'd have spent 2-3 hours on infrastructure that
isn't needed. The fix was already shipped — the pool cache. We just had to verify the
flake had cleared.

Same pattern as the earlier "5 missed liquidations on Felix" investigation — turned
out to be $0.21 of dust, not a real miss.

## Outcome
Zero code changes. weETH/WETH routing already works correctly with the existing
Slipstream tickSpacing=1 pool and the pool cache. Telegram alerts for this pair
should stop once cache is fully warm.

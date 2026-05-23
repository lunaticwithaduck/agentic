---
title: Port Bend's anvil-fork smoketest to Monad lane
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Outcome
Smoketest infrastructure ported and run against `wstETH/WETH` whale. The test correctly **revealed a critical gap**: Uniswap V3 has wstETH/WETH pools at fees 100/500/3000 on Monad but they all have **zero liquidity**. Our v1 Uni-V3-only swap-path cannot atomically fire **any** of the currently at-risk markets:

| Market | At-risk debt | Uni V3 route |
|--------|-------------|--------------|
| wstETH/WETH | $16.5M cluster | pools exist, zero liquidity ❌ |
| wsrUSD/USD1 | $2.5M | no Uni V3 pool ❌ |
| WBTC/USDC | smaller | no Uni V3 pool ❌ |

## What works
- ✅ Anvil fork of Monad starts cleanly (`~/.foundry/bin/anvil --fork-url https://monad.drpc.org --port 8546`)
- ✅ `monad/smoketest.js` loads positions, reads market state, queries oracle on the fork
- ✅ `findBestPool` correctly returns null when no liquidity exists (no false positives)
- ✅ Discovery routed through mainnet RPC instead of fork (avoids cascading rate limits)

## What needs to happen before flipping LIVE
**Curve integration is mandatory** — not optional. Without Curve in the swap-path, the at-risk inventory is uncapturable.

Specifically:
1. Wire Curve on Monad into the V3-style swap-path catalog (`lib/monad-dexes.js` extension)
2. Verify Curve has wstETH/WETH pools (Curve specializes in LST stable-stable pairs)
3. Adapt `swap-path.js` to handle Curve's different ABI (StableSwap, not Uni V3)
4. Re-run smoketest with Curve enabled → expect quoter to return non-zero

## Files
- NEW: `/home/jojo/automation/monad/smoketest.js` — anvil-fork smoke test for the lane

## Outcome / next step
Monad lane stays in DRY mode. **Do NOT flip live yet** — the smoketest proved we can't capture any at-risk position without Curve. File a follow-up task to integrate Curve.

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-local-smoketest.md`.

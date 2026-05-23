---
title: Add 2 missing high-liquidity DEXes (PancakeSwap V3 + Curve) to Monad swap-path
created: 2026-05-22
completed: 2026-05-22
status: done — phased ship, unlocks new route + adds stable-swap optimization
---

## Result: Monad swap-path expanded from 2 to 4 DEXes

Added:
- **PancakeSwap V3** (`pancakeswap-v3-monad`) — canonical Uni V3 fork, drop-in via existing
  `v3-classic` handler. Factory `0x0bfbcf9f...`, SwapRouter `0x1b81D678...`, QuoterV2
  `0xB048Bbc1...`. Fee tiers `[100, 500, 2500, 10000]` (note 2500 not 3000).
- **Curve** (`curve-monad`) — new `curve-stable` handler with `get_dy()` quote and
  `exchange(i, j, dx, min_dy)` swap calldata builder. Pool registry currently has just
  the AUSD/USDC/USDT0 stable triplet (`0x94264410...`, $2.97M TVL).

## Live measured impact

Per-market route discovery at $1k probe size:

| Market | Old best | New best | Notes |
|--------|----------|----------|-------|
| `0xe35c5abc64` | (no route) | **PancakeSwap V3 fee=500** | newly unlocked |
| `0x8bdb7d2c50` | Uniswap V4 | Uniswap V4 (unchanged) | V4 still best for wstETH/WETH |
| `0x7aeb107bee` | Uniswap V4 | Uniswap V4 (unchanged) | V4 still best |
| `0x51efed9df4` | Uniswap V3 | Uniswap V3 (unchanged) | V3 still best |

Stable-swap probes (would benefit liquidations that need to convert seized collateral
to AUSD or USDT0 to repay the loan):
- AUSD → USDC ($10k): **Curve, 9998.35 USDC out** (~0.017% slippage — perfect stable)
- USDC → AUSD ($10k): **Curve, 10000.25 AUSD out** (positive slippage)
- Pre-Curve, USDC → AUSD picked Uniswap V3 fee=500 with $0.13 output — catastrophic.
  This route is now fixed for any future fire that touches AUSD.

## Deferred (Phase 2 not pursued)

- **Balancer V3** ($14.7M TVL): all liquidity is in `wn*` Aave-wrapper tokens
  (`wnUSDT0/wnAUSD/wnUSDC`) which wouldn't appear directly as Morpho collateral. The
  non-wn pools have minimal TVL.
- **TraderJoe V2.2** ($3M AUSD/USDC): overlaps with Curve coverage; LB ABI is complex
  (Liquidity Book pair binSteps + version arrays). Not worth the integration cost when
  Curve already handles this pair.

Revisit if a future Morpho market introduces a token whose only viable route requires
these DEXes.

## Files modified
- `lib/monad-dexes.js` — added PCS V3 + Curve catalog entries
- `monad/swap-path.js` — added `findBestCurve` handler + `curve-stable` branch in
  `findBestPool` dispatcher + `buildSwapData`
- `monad-executor.service` restarted to load new code

## What was NOT pursued (and why)

Investigated whether `earnAUSD`/`YZM`/`aHYPER` (collateral in 3 of the 4 "dead markets")
could be redeemed atomically. They're yield-aggregator vaults — `convertToAssets()`
returns rates (~1.03×) but `previewRedeem`/`maxRedeem` revert. These are
withdrawal-queue vaults (basis-trade strategies unwind on cooldown). **Same shape as
Liquity stability-pool liquidations: structurally non-atomic. No amount of swap routing
fixes this.** $1.3M of seemingly at-cliff exposure is unfireable for our atomic-arb
model.

## Outcome

Completed 2026-05-22. Monad executor live with expanded routing. 1 previously
unroutable market now has a path; any future market touching the AUSD/USDC/USDT0
stable triplet gets correct stable pricing instead of catastrophic Uniswap V3
mispricing. No regressions in markets already covered.

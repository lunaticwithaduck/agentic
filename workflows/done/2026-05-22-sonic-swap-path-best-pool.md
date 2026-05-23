---
title: Fix Sonic swap-path to compare quotes across all Shadow tickSpacings, not pick the first
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
`sonic-silo/swap-path.js` was routing trades through dust Shadow pools because `findBestPool()` probed with a hardcoded `1e15` amount that's meaningless for 6-decimal tokens (1 billion USDC.e). Real pools reverted at that probe size, leaving the "best" pool effectively random — usually the first pool found, regardless of liquidity.

## Root cause
```js
// BEFORE
const probeAmount = 10n ** 15n;
const quotes = await Promise.all(pools.map(async ({ tickSpacing, pool }) => {
  const data = quoterIfc.encodeFunctionData('quoteExactInputSingle', [{
    tokenIn, tokenOut, amountIn: probeAmount, tickSpacing, sqrtPriceLimitX96: 0,
  }]);
  ...
}));
```

Probing a 6-decimal token (USDC.e) with `1e15` = 1,000,000,000 USDC.e = 1 billion tokens. Every real Shadow pool reverts at that size → all quotes are 0 → sort is meaningless → first pool wins. For the 159 USDC.e → wS trade overnight, this routed through the dust ts=100 pool ($18 USDC.e depth) instead of the deep ts=50 pool ($184k depth).

## Fix
- Accept `amountIn` parameter in `findBestPool(rpc, tokenIn, tokenOut, amountIn)`
- Use the actual trade size as the probe — pool depth + tick proximity now correctly determine the winner
- Filter out pools that revert (insufficient liquidity)
- Return `expectedOut` along with `pool`/`tickSpacing` so caller doesn't need a second quote RPC
- Updated `executor.js` to pass `collateralToLiquidate` as the probe amount

## Verification (same armed position, before vs after)
```
BEFORE: pool=0xeAA89d63 (ts=100, dust)  quote=211 wS     expectedProfit=-2,896 wS
AFTER:  pool=0x324963c2 (ts=50, deep)   quote=3,217 wS   expectedProfit=+178 wS
```

15× more output, profit sign flipped from loss to gain.

## Outcome
Completed 2026-05-22. Sonic swap-path now picks the right Shadow pool based on real trade-size quotes. The overnight `0x24c0c26735f2` fire is still blocked by a *separate* limitation (`sTokenRequired=true` — silo lacks underlying liquidity, our v1 executor doesn't take sToken receipts), but that's now visible as the real issue instead of being masked by the bogus -2,896 wS profit calculation.

## Out-of-scope follow-up
Add sToken-take path to Sonic executor (v2 capability). Would unblock the `0x24c0c26735f2` class of fires where the silo is underlying-illiquid but the borrower is genuinely insolvent.

## Completion
Run `/complete workflows/tasks/2026-05-22-sonic-swap-path-best-pool.md`.

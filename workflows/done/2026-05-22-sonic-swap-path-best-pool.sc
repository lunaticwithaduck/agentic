---
domain: defi-liquidations
source_task: 2026-05-22-sonic-swap-path-best-pool.md
date: 2026-05-22
keywords: ["swap-path", "v3-router", "uniswap-v3", "ramses", "shadow", "pool-selection", "tickspacing", "fee-tier", "quote", "slippage"]
---

## Extracted Knowledge

### V3 router pool-selection: always pass the actual trade size to the quoter
For Uniswap V3 / Ramses V3 / any fork (Shadow on Sonic, Aerodrome Slipstream on Base, etc.), the same fee tier / tickSpacing can have multiple pools with vastly different liquidity depths. The canonical pool-selection pattern is:

1. Enumerate all fee tiers / tickSpacings via `factory.getPool(tokenA, tokenB, ts)`
2. For each existing pool, call `quoter.quoteExactInputSingle(...)` with **the real amountIn**
3. Pick the pool with the highest `amountOut`

**The probe amount IS the quote amount.** Do NOT probe with a hardcoded size and then quote separately:
- A hardcoded probe (e.g. `1e15`) is meaningless across decimals. For a 6-dec token like USDC.e, `1e15` = 1 billion tokens — every real pool reverts. For 18-dec it's 0.001 token — too small to surface slippage differences.
- The pool that gives the best quote at one trade size may not be the best at another. Same pair, $100 trade → pool A wins; $10k trade → pool B wins (price impact on shallow pool dominates).
- Quoter reverts when the pool can't fill the request. Filter those out (`amountOut === 0n`); don't sort them to position 1 by accident.

### Why pool depth ≠ pool selection
A V3 pool's reserve balance is a TVL signal, not a price signal — concentrated liquidity means the active liquidity at the current tick can be tiny even when total reserves are huge. The real test is whether the quoter returns a non-zero answer for your actual size. A pool with $184k reserves can still be useless if all liquidity is parked at a tick far from current price.

### Anti-pattern: "find pool, then quote separately"
```js
// WRONG — pool selected without knowing trade size
const best = await findBestPool(rpc, tokenIn, tokenOut);
const out = await quoteSwap(rpc, tokenIn, tokenOut, best.tickSpacing, amountIn);
```
This calls `findBestPool` blind, then quotes only the chosen pool. If `findBestPool` chose wrong, you get a bad quote with no second chance.

### Correct pattern
```js
// Pass real size to selection; return quote bundled with pool
const best = await findBestPool(rpc, tokenIn, tokenOut, amountIn);
const expectedOut = best.expectedOut;  // already in result, no extra RPC
```

### Failure signature in production logs
Before the fix, the executor logged:
```
shadow pool: 0xeAA89d63 tickSpacing: 100 quote: 211.27 wS
expectedProfit: -2,896 wS  (skipped — negative profit gate)
```

After:
```
shadow pool: 0x324963c2 tickSpacing: 50  quote: 3,217.63 wS
expectedProfit: +178 wS  (would have fired if not for unrelated sTokenRequired)
```

Same trade, same pair, same time — 15× more output, sign-flipped profit.

### Ramses V3 fork notes (Shadow on Sonic)
- Uses `tickSpacing` instead of `fee` tier (different ABI from Uni V3 V2 quoter)
- Common values: 1, 10, 50, 100, 200, 1000, 2000
- Quoter signature: `quoteExactInputSingle((tokenIn, tokenOut, amountIn, tickSpacing, sqrtPriceLimitX96)) returns (amountOut, sqrtPriceX96After, initializedTicksCrossed, gasEstimate)`
- Router signature: `exactInputSingle((tokenIn, tokenOut, tickSpacing, recipient, deadline, amountIn, amountOutMinimum, sqrtPriceLimitX96))` — `tickSpacing` slot replaces V3's `fee` slot

## Proposed Skill Content

Extend `defi-liquidations` with a "DEX router selection" section:

- **Always pass the real trade size to the pool-selection function**, never probe with hardcoded amounts.
- For V3-style DEXes (Uni V3, Ramses, Shadow, Aerodrome Slipstream, etc.), enumerate all fee tiers / tickSpacings → quote each at the real `amountIn` → pick max `amountOut`.
- Filter out pools that revert (`amountOut === 0n`) before sorting — otherwise they sort to position 1 arbitrarily.
- Bundle the quote with the pool selection result so the caller doesn't need a second RPC.
- Pool depth (reserve balance) is a TVL signal, not a price-impact signal — concentrated liquidity means active liquidity at the current tick can be tiny even when reserves look big. Trust the quoter, not the reserves.
- Diagnostic signature of this bug in production logs: a "quote: X" where X is suspiciously low relative to the input size, paired with a negative expectedProfit and a skip. Often "the pool is small" gets blamed when the real bug is "we picked the wrong pool".

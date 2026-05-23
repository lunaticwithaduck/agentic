---
domain: hyperevm
source_task: 2026-05-20-pool-depth-validation.md
date: 2026-05-20
keywords: ["uniswap-v3", "pool-depth", "slippage", "swap-impact", "hyperswap", "liquidator"]
---

## Extracted Knowledge

### Oracle price ≠ pool execution price
Oracle returns spot but Uniswap V3 pools have concentrated liquidity. Large swaps move through ticks, getting progressively worse execution. A swap that consumes >5% of the pool's `tokenIn` reserves is likely to slip 2-5%+ vs spot — and that's after the pool fee (typically 0.3%).

For a liquidator computing `amountOutMinimum` from oracle prices: small swaps (< 1% of pool) tend to be within slippage tolerance, but large swaps need either a tighter safety margin or to be skipped entirely. **Skip is cheaper than revert.**

### Cheap depth proxy: ERC20.balanceOf(pool)
You can estimate pool depth without reading slot0+liquidity+tickBitmap:
```js
const reserve = BigInt(await tokenIn.balanceOf(poolAddress));
const impactPct = Number((swapAmountIn * 10000n) / reserve) / 100;
// > 5%: skip; > 1%: caution; < 1%: fine
```
This is the total reserve in the pool, not the depth at current tick. But for a healthy pool, total reserve is within 2-3× of depth-at-tick. Good enough for a binary skip/proceed decision; reserve smaller than swap means definitely-revert.

### Same-asset liquidation (coll == debt)
A user can supply AND borrow the same asset (e.g., for leverage trade unwinds, or accidental config). When their HF drops, Aave V3 `liquidationCall` handles same-asset cases natively:
- Flash loan X of asset
- liquidationCall(asset, asset, user, X) — Aave repays X of debt, seizes ~X×LIF of supply
- Contract now holds X×LIF of asset; repay X+premium; profit = X×(LIF-1) - premium

**No swap is needed.** Executors that always set `swapTarget=ROUTER` will try to swap A→A which has no pool → SwapFailed revert.

Defensive check:
```js
const sameAsset = collAsset.underlying.toLowerCase() === debtAsset.underlying.toLowerCase();
if (sameAsset) {
  swapTarget = ethers.ZeroAddress;
  swapData = '0x';
}
```

### Why oracle prices don't catch this
AaveOracle gives "fair value" — a smoothed price often from Chainlink or a fork. The DEX pool may diverge by:
- ~0.1% on tightly-arb'd majors
- 1-5% on illiquid or LST-correlated pairs
- 10%+ during arb storms or after large dumps

Our `amountOutMinimum = funding + premium + $20` uses oracle math; the swap delivers DEX-pool math. A 2% gap on a $24k liquidation = $480 short → revert NoProfit.

### Practical thresholds for HyperEVM (HyperSwap V3)
After running the analyzer 2026-05-20:
- Major pairs (WHYPE/USDC fee 3000): pool reserves $1-5M, our typical swap $1-10k = <1% impact
- LST-correlated pairs (kHYPE/WHYPE fee 100): pool reserves $5-50k. Large positions ($100k+) impossible.
- Stable-stable (USDC/USDT0 fee 100): generally fine, deep liquidity
- 100-bps pools tend to be thinner than 3000-bps — fee tier is a rough depth proxy

## Proposed Skill Content
Add to `hyperevm` skill under "Liquidator hardening checklist":
- Always depth-check before broadcast. ERC20.balanceOf(pool) is the cheap proxy.
- Same-asset liquidation: zero out swapTarget; Aave handles repay internally.
- Skip at presign rather than pre-flight revert — saves gas, Telegram noise, AND keeps you off competitor radar.

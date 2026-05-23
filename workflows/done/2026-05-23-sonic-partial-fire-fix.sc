---
domain: defi-liquidations
source_task: 2026-05-23-sonic-partial-fire-fix.md
date: 2026-05-23
keywords: [silo-v2, partial-fire, stoken-required, collateralsilo-getliquidity, maxdebttocover-bug, proportional-scaling, 95-percent-margin]
---

## Extracted Knowledge

### Silo V2 partial-liquidation pattern: scale down to fit collateralSilo.getLiquidity()
For Silo V2 markets where `SiloLens.maxLiquidation` returns `sTokenRequired=true`, the
correct atomic-fire path is NOT to skip — it's to partial-fire within the collateral
silo's current liquidity.

```js
let [collMax, debtMax, sTokenReq, full] = lens.maxLiquidation(silo, hook, borrower);
if (sTokenReq) {
  const collLiq = await collateralSilo.getLiquidity();
  if (collLiq === 0n) return null;  // truly stuck
  const cappedColl = collLiq * 95n / 100n;  // 5% margin for interest accrual + rounding
  if (cappedColl < collMax) {
    const cappedDebt = (cappedColl * debtMax) / collMax;
    collateralToLiquidate = cappedColl;
    debtToRepay = cappedDebt;
    sTokenRequired = false;  // protocol now returns underlying, not shares
  }
}
```

After scaling, the liquidationCall returns underlying tokens (USDC.e for our market)
that can be swapped on DEX as normal. Profit per fire is bounded by `collLiq × LIF`
not by the borrower's total position.

### CRITICAL: maxDebtToCover must equal the partial debtToRepay, NOT MaxUint256
The v1 executor passed `maxDebtToCover: ethers.MaxUint256` because the protocol
internally clamps to the max liquidatable amount. **This breaks partial fires** —
the protocol clamps back UP to full and re-triggers sTokenRequired internally,
producing either revert or unwanted sToken receipt.

For partial fires, pass `maxDebtToCover: debtToRepay` (the scaled-down value)
explicitly. The protocol then liquidates EXACTLY that amount, no more.

This is the single most non-obvious correctness piece in the whole fix.

### 95% margin on partial seize
Always cap the partial at 95% of `collateralSilo.getLiquidity()`, not 100%. Reasons:
- Interest accrual between read and execution can shift the borrower's debt accounting
- Rounding in the proportional `cappedDebt = (cappedColl * debtMax) / collMax` math
- Other liquidators may chip at the silo's liquidity in the same block

5% absorbs all three without meaningful profit hit (95% of $76k × 6.5% LIF = $4.7k vs 100% = $4.94k).

### Why blanket-skip was leaving real money on the table
Yesterday's 0xbf5b0bc2 ($4M wS debt at the cliff for 13 min): blanket-skip captured $0.
Partial-fire-within-liquidity captures: 95% of $51k-76k silo1 liquidity × 6.5% LIF
= $3-5k per fire. Borrower's auto-rebalancer pattern produces multiple cliff approaches
per week → real recurring revenue from a previously written-off lane.

### Silo V2 partial liquidation hook ABI
For reference, the partial liq hook signature is:
```solidity
function liquidationCall(
  address _collateralAsset,
  address _debtAsset,
  address _user,
  uint256 _maxDebtToCover,
  bool _receiveSToken
) external returns (uint256 withdrawCollateral, uint256 repayDebtAssets);
```

`_maxDebtToCover` is a cap, not a target — protocol takes min(this, actual_liquidatable).
For partial fires, this cap matters: set it to the scaled debtToRepay value.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extends Silo V2 sToken section):

### Silo V2 partial-fire-within-liquidity pattern (replaces blanket sTokenRequired skip)
When maxLiquidation returns sTokenRequired=true, the FULL fire exceeds collateral
silo's getLiquidity. Scale down to 95% of getLiquidity proportionally → sTokenRequired
flips to false → atomic fire succeeds.

```js
if (sTokenRequired) {
  const liq = await collateralSilo.getLiquidity();
  if (liq === 0n) return skip;
  const capped = liq * 95n / 100n;
  if (capped < collMax) {
    collMax = capped;
    debtMax = (capped * debtMax) / collMax_prev;
    sTokenRequired = false;
  }
}
```

### Partial fires require explicit maxDebtToCover (not MaxUint256)
Pass the SCALED debtToRepay as maxDebtToCover so the protocol caps the liquidation
at the partial amount. Passing MaxUint256 re-triggers sTokenRequired internally and
breaks the partial fire.

### 95% liquidity margin is the right default for partial fires
Absorbs interest accrual, proportional rounding, and same-block competition without
meaningful profit hit. 100% will randomly revert; <90% leaves obvious profit on table.

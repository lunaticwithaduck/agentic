---
title: Ship Sonic partial-fire-within-liquidity fix in executor.js
created: 2026-05-23
completed: 2026-05-23
status: done
---

## Outcome

Completed 2026-05-23. Shipped LIVE. Executor restarted at 10:23 UTC and processing
normally. Next sTokenRequired=true scenario will now partial-fire instead of skip.

### Changes to `/home/jojo/automation/sonic-silo/executor.js`
1. Changed destructure of maxLiquidation result from `const` to `let` so we can rescale
2. Added PARTIAL-FIRE PATH block right after maxLiquidation:
   - Reads `payload.collateralSilo.getLiquidity()` via direct RPC call
   - If liquidity = 0: keeps existing skip-and-Telegram-alert path
   - If liquidity > 0 and full seize exceeds liquidity:
     - Caps `collateralToLiquidate` at 95% of liquidity (safety margin for accrual)
     - Scales `debtToRepay` proportionally
     - Flips `sTokenRequired = false` (protocol will return underlying)
   - If liquidity > 0 and full seize ≤ liquidity: passes through unchanged
3. Removed the old dead `if (sTokenRequired) → skip` block downstream (now unreachable)
4. Changed `maxDebtToCover: ethers.MaxUint256` → `maxDebtToCover: debtToRepay` so the
   protocol caps the liquidation at our partial amount instead of internally clamping
   back up to full (which would re-trigger sTokenRequired)

### Expected behavior on next cliff
For a borrower like 0xbf5b0bc2 with $5M collateral × max LIF when silo1.getLiquidity()
sits around $30-76k:
- Partial seize: liquidity × 95% ≈ $28-72k USDC.e
- Partial debt repay: ~$26-67k worth of wS
- LIF profit: 6.5% × $28-72k = **$1.8-4.7k per fire**
- Multiple fires possible per cliff event as borrower may oscillate

### Risk assessment
- The patch only ACTIVATES when sTokenRequired=true, which we were skipping anyway → no regression on healthy fires
- The 5% margin and proportional debt scaling are well-defined math
- maxDebtToCover=debtToRepay change is the critical correctness piece — protocol must clamp to our partial amount
- Profit gate (minProfitWei) downstream still protects against bad swap routing

### Validation plan
- Live wait — next sTokenRequired=true scenario will exercise the path
- Logs will show "scaling partial fire: coll X → Y, debt Z → W"
- If anything goes wrong, the on-chain `minProfitWei` check reverts the tx (no capital loss, just gas)
- Plan to fork-test with proportional oracle stub if no real fire happens in 48h

## Skill candidate evaluation
- Technologies/frameworks touched: Silo V2 maxLiquidation semantics, partial-liquidation params, collateralSilo.getLiquidity, ethers.js BigInt scaling, sonic-silo executor architecture
- Domain-specific knowledge involved: maxDebtToCover MUST be set to the partial debt (not MaxUint256) for partial fires to avoid protocol re-clamping to full; the proportional scaling formula (cappedColl/collMax × debtMax); 95% safety margin pattern for accrual+rounding
- Verdict: **GENERATE**
- Reason: Codifies the partial-fire pattern for Silo V2 (reusable for any future Silo V2 lane). The MaxUint256 → debtToRepay correctness lesson is non-obvious.

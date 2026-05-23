---
title: Bend smoke test — full happy-path flash loan + liquidate + swap + profit
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Validate BendLiquidator end-to-end on anvil fork: force a real liquidation via oracle manipulation, verify HONEY profit lands in owner wallet.

## Steps
- [x] anvil_setCode oracle override (constant-returning bytecode pattern)
- [x] anvil fork at current Berachain block
- [x] WBTC oracle crashed to 65% of real price → HF 0.774 (liquidatable)
- [x] BendLiquidator.liquidate() called via on-anvil signer
- [x] tx success verified, Liquidated event decoded
- [x] Owner HONEY balance verified
- [x] Critical production bug discovered + fixed

## Critical bug caught: seizedAssets overflow
**Pre-fix:** executor.js passed `seizedAssets = type(uint256).max` to Morpho. Morpho computes `seizedAssets × oraclePrice / ORACLE_SCALE` BEFORE clamping to actual liquidatable amount. With max × non-zero price, this overflows. EVERY real liquidation would have reverted with `Panic OVERFLOW(17)`.

**Post-fix:** pass `BigInt(payload.collateral)` (the borrower's actual collateral amount) as the seizedAssets argument. Morpho clamps internally to the actual liquidatable cap.

Applied to both `smoketest.js` and `executor.js` (production). Restarted `bend-executor.service` to pick up the fix.

## Smoke test result
```
Status:    ✅ SUCCESS
Gas used:  322,316
Seized:    0.2591 WBTC
Repaid:    12,412.50 HONEY
Profit:    7,287.37 HONEY ≈ $7,287
Owner HONEY balance: 7,287 HONEY (was 0)
```

(Profit is from a 35% oracle crash scenario; real HF=1.0 liquidations will yield less per-dollar-of-collateral but still positive within the contract's minProfitWei threshold.)

## Outcome

Completed 2026-05-18. Full flash-loan → liquidate → Kodiak-swap → repay → profit-to-owner flow verified end-to-end on anvil fork at the current Berachain block. Used `anvil_setCode` with constant-returning bytecode (`PUSH32 <price> PUSH1 0 MSTORE PUSH1 32 PUSH1 0 RETURN`) to crash the WBTC oracle's price to 65% of real, pushing HF to 0.774 and making the existing $14k debt position liquidatable. Liquidator earned 7,287 HONEY profit in one tx — full Liquidated event decoded and owner balance matched.

Most importantly, this caught a critical bug: passing `seizedAssets = type(uint256).max` to Morpho causes an arithmetic overflow panic because Morpho multiplies seizedAssets × oraclePrice before clamping. Every real liquidation would have reverted in production. Fixed in both smoketest and executor, executor service restarted.

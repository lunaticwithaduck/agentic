---
title: Investigate flash-loan amplification of Somnia SLP rewards
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Determine whether a Tokos flash loan can amplify SLP rewards on the $1k QuickSwap
WSOMI/USDC.e LP during Epoch 13 (final week).

## Steps
- [x] Read SLP docs at `liquidity.somnia.network` + Somnia blog for accounting mechanic
- [x] Determine if rewards are TWAB / snapshot / locked / continuous
- [x] Check if Tokos accepts QuickSwap LP token as collateral (alternative looping play)
- [x] Verdict + reasoning

## Outcome

Completed 2026-05-22. **Verdict: NO — flash loan cannot amplify SLP rewards.**

### Accounting mechanic: time-weighted continuous accrual (TWAB)
Quoted directly from Somnia's own materials:
- "Time-weighted formula that factors in liquidity amount, **duration**, pool weighting, multipliers"
- "Points accumulate **every hour** based on your current position"
- "**Time-Weighted Balances** — prevent short-term inflows from gaming rewards" (explicit anti-flash-loan language in the program design)
- "Loyalty multipliers reset on withdrawal" — flash-loan in/out destroys the multiplier instead of growing it
- "Whale caps" explicitly reduce returns for oversized positions

This mechanism is **specifically designed** to defeat flash-loan amplification.

### Math: max possible amplification
Even if you flash-borrowed $10M into the LP:
- Hourly tick = ~0.6% of one weekly epoch's points
- Flash loan must repay in the same tx → net balance unchanged at end of block
- Hourly indexer sampling lands BETWEEN blocks → never captures the inflated state
- Result: effectively **zero amplification**

### Off-chain accounting
There is **no on-chain points contract** to manipulate. Points are computed by an off-chain indexer that samples `LP.balanceOf(user)` hourly and writes scores to the SLP backend. Nothing to attack in a single block.

### Tokos LP-token collateral
**Not supported.** Tokos docs list only token-form reserves (aUSDC, aWETH, USDC, ETH, WSOMI) — no QuickSwap LP tokens. Aave V3 forks rarely list AMM V2 LP tokens as collateral due to oracle manipulation concerns. The looping alternative play (deposit LP → borrow → buy more LP → loop) is also unavailable.

### "Could we be wrong" disclaimer
A **MAYBE** edge case would exist only if:
- The SLP indexer reads at a publicly-predictable block boundary (no evidence it does), AND
- A single hourly tick has meaningful weight vs $1k base (it doesn't)

To verify definitively without trusting the blog wording: open browser devtools on
`liquidity.somnia.network`, watch the XHR populating "your points", transfer LP
in/out, and check whether points update in a single block (per-block accounting) or
only after an hourly tick (TWAB). The evidence so far is overwhelmingly TWAB.

### Final action: stick with the original plan
- Hold position through Epoch 13 end (~2026-05-29) — capture remaining ~$29 in time-weighted SOMI rewards
- Withdraw + swap rewards to USDC.e at season end
- Re-evaluate when Season 2 announcement (if any) lands

No flash-loan amplification, no LP-collateral looping, no alternative play on Somnia for this position. The mechanism is correctly designed to reward only locked-time capital.

## Skill candidate evaluation
- Technologies/frameworks touched: SLP TWAB mechanic, off-chain points indexers, anti-flash-loan program design, Tokos asset listing constraints
- Domain-specific knowledge involved: TWAB defeats single-block manipulation by design; "loyalty multiplier reset on withdrawal" as a discrete anti-gaming feature; the diagnostic test (devtools XHR + LP in/out transfer) to verify accounting cadence
- Verdict: **GENERATE**
- Reason: First TWAB / anti-flash-loan-program-design entry in the domain. Reusable test pattern for verifying any rewards program's accounting cadence on any chain.

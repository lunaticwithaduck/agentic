---
title: Add sToken handling to Sonic liquidator — unblock the $20k insolvent borrower
created: 2026-05-22
completed: 2026-05-22
status: punted
---

## Goal
Unblock the Sonic borrower `0x24c0c26735f2` ($20k wS debt, ratio 1.0006) that's been armed since 08:16 UTC but skipped because `sTokenRequired=true`.

## Investigation findings
1. **Contract already supports sToken receive** — `SiloLiquidator.sol` line 77 has `receiveSToken` as a per-call parameter; line 94-98 sweeps any leftover sToken to OWNER. No contract change is needed to *accept* sTokens.

2. **The real blocker is on-chain state, not code**: collateral silo at `0x322e1d5384aa4ED66AeCa770B95686271de61dc3` has:
   - Utilization **100.01%** (debt 2,083,675 vs collateral 2,083,388 USDC.e)
   - `getLiquidity()` returns **0**
   - Physical USDC.e balance 6,018 but locked by protocol invariants
   - Result: cannot redeem sToken → underlying in the same tx (would revert)

3. **No DEX pool for sUSDC.e** to swap to — only unrelated `bUSDC.e-20` pools with $0-$306 TVL. Not enough depth for the ~$160 seize size.

4. **Atomic profit is impossible** given current chain state. The flashloan path requires output-debt-token to repay; if we take sToken and can't convert, the flashloan repay fails and the tx reverts.

## Why not implement the multi-tx hold-and-redeem path?
- Would need to replace flashloan with our own wallet capital (no money in = no flashloan to repay)
- Capital lockup: ~$1k wS or ~$150 USDC.e on Sonic (we don't have that bridged in)
- Multi-tx tracking code for the eventual redeem-when-liquidity-returns
- Expected profit per fire: ~$8 (178 wS × $0.048)
- This class of fire recurs maybe a few times per month
- ROI of engineering + capital lockup vs $8/fire is negative

## Outcome
**Punted.** The Sonic v1 executor will continue to skip `sTokenRequired=true` fires with the current log line. Bigger fires (>$1k profit) in this state could justify revisiting with the multi-tx redeem-later architecture, but the current ~$8 opportunity is too small.

## Light follow-up worth doing
Extend the Sonic executor to send a one-line Telegram alert when it skips with `sTokenRequired=true` AND the expected profit ≥ a threshold (e.g., $100). Right now the skip is silent, so we won't know if a bigger version of this opportunity passes through. Could be filed as a separate small task.

## Completion
Run `/complete workflows/tasks/2026-05-22-sonic-stoken-handling.md`.

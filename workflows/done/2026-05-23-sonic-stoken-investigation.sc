---
domain: defi-liquidations
source_task: 2026-05-23-sonic-stoken-investigation.md
date: 2026-05-23
keywords: [silo-v2, stoken, exhaustive-share-token-audit, physical-vs-protocol-liquidity, beets-wrapped, no-atomic-path, sWS, sUSDC]
---

## Extracted Knowledge

### Silo V2 has SIX share tokens per market — audit ALL six before declaring "no DEX path"
For each Silo V2 market (e.g. `S/USDC.e_borrowable_S`), there are 6 share-token addresses to audit:
- silo0.collateralShareToken (often == silo0 address itself)
- silo0.debtShareToken
- silo0.protectedShareToken
- silo1.collateralShareToken (often == silo1 address itself)
- silo1.debtShareToken
- silo1.protectedShareToken

When sTokenRequired=true, the liquidator MIGHT receive any of these depending on which side has the liquidity shortage. A first-pass check of just collateralShareToken can MISS a viable path if the protocol routes the receipt through debt or protected tokens.

Pattern: query each of the 6 via CoinGecko `/onchain/networks/{chain}/tokens/{address}/pools` and gather (pool count, max reserve, 24h volume) for each. The decision is per-market based on the WORKED-EXAMPLE max trade size, not abstract reasoning.

### Physical underlying balance ≠ withdrawable liquidity
A silo can hold significant physical underlying tokens but still return `getLiquidity() = 0` because protocol invariants enforce:
- Utilization caps (don't let withdrawals push utilization > 100%)
- Debt floors (keep enough to service interest accrual)
- Bad-debt buffers

Real example (2026-05-23): silo1 (USDC.e) held $87,924 of physical USDC but `getLiquidity() = 0`. An atomic `redeem()` reverts despite the balance.

**Don't conflate `token.balanceOf(silo) > 0` with "redeem will succeed".** Always check `getLiquidity()` separately. It's the only canonical answer.

### Beets-wrapped Silo shares create dust DEX pools that look like a path but aren't
Beets Finance wraps Silo V2 share tokens into "bXxx-NN" tokens (e.g., bUSDC.e-20). These bToken wrappers DO have DEX pool listings — but the pools are typically:
- Pair against other Beets-wrapped tokens (bXxx-NN / wstkscUSD, bXxx-NN / scUSD)
- Dust reserves (saw $264, $0.01, $0.0005)
- Near-zero 24h volume

The existence of these pools makes a token-pools query return non-zero results, which can fool a quick audit. **Always check `reserve_in_usd` and `volume_usd.h24` per pool** — count alone doesn't matter. A market with 3 pools all under $300 reserve = effectively zero DEX presence.

### CoinGecko onchain endpoint covers all Sonic DEXes comprehensively
Verified across Shadow, Equalizer, Beethoven, Curve-on-Sonic — the `/onchain/networks/sonic/tokens/{addr}/pools` endpoint returns the full DEX-pool inventory. No need to manually probe each DEX. Same likely holds for other chains with mature CoinGecko coverage.

### When a market is confirmed structurally unfireable, add to UNFIREABLE_MARKETS proactively
For Sonic Silo V2 markets where ALL six share-token audits return "no DEX path" AND `getLiquidity()` has been observed to be persistently 0:
- Add the silo address (or marketId) to `UNFIREABLE_MARKETS` in `fleet-cliff-report.js`
- Suppress the per-chain executor's `🎯 arm` log noise for these markets (or accept the noise as low-cost data)
- Telegram alert when `sTokenRequired=true` AND expected profit > threshold remains valuable as a "rare opportunity to manually intervene" signal

This keeps the cliff-scope dashboard honest. Better to label a market as "won't fire" than to keep getting alerts for positions we can never capture.

## Failure Modes Observed

### "There should be a way" intuition collided with comprehensive evidence
User instinct on 2026-05-23: "there should be a way, use gecko api to check dexes" — but a full 6-share-token audit with CoinGecko onchain data confirmed NO atomic path exists. Sometimes the structural block is real and exhaustive verification is the only way to honestly close the question. Don't keep looking for a path that's been searched and ruled out — invest the effort in non-atomic alternatives or skip.

Mitigation: when investigating any "is there a DEX path" question, EXHAUSTIVELY enumerate share tokens / derivative wrappers / DEX inventories from a single comprehensive source (CoinGecko onchain) BEFORE declaring either GO or SKIP. The cost of exhaustive verification is 1 minute of API calls; the cost of revisiting "are we sure" loops is hours.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extending the Silo V2 sToken section):

### Audit ALL 6 share tokens per Silo V2 market before declaring no DEX path
The 6 addresses to check: silo0.{collateral,debt,protected}ShareToken + silo1.{collateral,debt,protected}ShareToken. Run CoinGecko `/onchain/networks/{chain}/tokens/{addr}/pools` against each, count pools AND check max reserve_in_usd. A token with 3 pools all under $300 reserve = effectively no DEX presence.

### Physical balance != withdrawable liquidity for Silo V2
Always check `silo.getLiquidity()` to know if `redeem()` succeeds atomically. Protocol invariants can lock physical underlying even when `token.balanceOf(silo) > 0`.

### Beets-wrapped Silo shares are derivatives that look like paths but usually aren't
Pools named `bXxx-NN / wstkscUSD` or `bXxx-NN / scUSD` are Beets wrappers. Check reserve_in_usd before assuming the wrapped path is viable. Most are dust.

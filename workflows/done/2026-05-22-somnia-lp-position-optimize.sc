---
domain: defi-liquidations
source_task: 2026-05-22-somnia-lp-position-optimize.md
date: 2026-05-22
keywords: [lp-incentive-layer, somnia-liquidity-points, slp-multipliers, epoch-end-capture, scout-missed-rewards, lp-optimization, somnia, quickswap]
---

## Extracted Knowledge

### Scouting a chain's DEX/lending economics is incomplete without checking the incentive layer
A scout that reports "QuickSwap WSOMI/USDC.e earns 1.6% APR from fees" is technically correct but operationally useless if the chain runs a points/rewards program that adds 150%+ APY in token emissions. **Always include a step for "what incentive programs are running" before final verdict.**

Specific sources to check on every new chain:
1. Protocol's own foundation/blog for "season N", "points", "liquidity mining", "rewards program"
2. The protocol's "liquidity" or "incentives" subdomain (e.g. `liquidity.somnia.network`)
3. DEX's own farms page (QuickSwap, Velodrome, Aerodrome typically have one)
4. DefiLlama yields page filtered by chain (often picks up incentive layers automatically)

A baseline-fee-only APR is a floor, not the real return. The gap between "fees only" and "fees + emissions" on a new chain is typically 10-100×.

### Somnia Liquidity Points (SLP) program structure (2025-10 → 2026-05)
- Season-based with epochs (Season 1: 13 epochs, 90 days, 1M SOMI allocation)
- Per-venue multipliers:
  - DEX LP: 0.1×
  - Lending supply: 0.2×
  - LST/staking: 0.3×
- $2M TVL cap per pool
- Dashboard: `liquidity.somnia.network`
- Effective APY for top-tier pools during Season 1: ~150%+ in SOMI rewards (price-exposed)

When Season N ends and Season N+1 isn't announced, the same pool drops back to fee-only APR. Time the exit.

### "Epoch-end capture, then exit" pattern for finite-window incentive programs
For programs with publicly-known end dates:
1. Hold position through the final epoch to capture remaining emissions
2. Withdraw on or shortly after the announced end
3. Swap claimed reward tokens to stables immediately (unless you're long the token thesis) — emissions tokens are price-exposed and typically dump post-program
4. Wait for announcement of next program before redeploying

Don't exit early (forfeits remaining emissions). Don't stay past end without confirmation of continuation (drops to baseline yield + carries unhedged token exposure).

### Concentrated-LP band sizing from observed volatility
Rule of thumb based on 30-day volatility:
- Range ≤10%: tight ±5% band (rebalances ~1/week)
- Range 10-30%: moderate ±15% band (rebalances ~1/month)
- Range 30-50%: wide ±25% band (rebalances ~quarterly)
- Range >50%: stay full-range; concentration not worth the management

Also consider trend direction — for a token in confirmed downtrend, skew the band downward (e.g., -20% / +10% around current spot) to keep it in-range as the price drifts.

### Recognize when a DEX doesn't actually have V3
QuickSwap on Somnia (as of 2026-05) is V2-only (0.05% fee, full-range only) despite QuickSwap's mainnet (Polygon) V3 presence. Don't assume V3 exists wherever the DEX brand exists. Concentration playbooks only apply when the V3 tier is actually live on that specific chain.

Verify by:
1. Checking the DEX's UI on the target chain for a "V3" / "concentrated" / "ranges" option
2. Looking at the factory ABI — `getPool(tokenA, tokenB, fee)` (V3) vs `getPair(tokenA, tokenB)` (V2)
3. Watching the project's deployment announcements for "V3 launching on chain X"

### Hedge LP token exposure during incentive-capture window
For the final-epoch-capture pattern: if the reward token (here SOMI) is in confirmed downtrend, hedging the LP's directional exposure during the remaining capture window protects the realized rewards.

- LP holds ~50% in the volatile token (WSOMI) and ~50% in the stable (USDC.e)
- Short the volatile token's value on a CEX perp (if listed) for ~50% of LP value
- During the holding period: LP earns fees + emissions; short captures any drawdown in WSOMI
- Exit both legs simultaneously when withdrawing the LP

Only worth the overhead at LP sizes >$5-10k. At $1k, the spreads/funding eat the benefit. Note for future scaling.

## Failure Modes Observed

### Scout produced incomplete answer because it didn't check the incentive layer
First Somnia arb scout (2026-05-22) returned "1.6% APR — not worth it" for the WSOMI/USDC.e LP. Correct as far as fee math, but missed the SLP program that's paying ~152% APY in SOMI rewards through Epoch 13. Lesson: **the "incentive program check" should be a mandatory step in every chain-economics scout, NOT optional**. A scout report without it is operationally misleading.

Pattern to add to future scouts:
1. Verified facts (TVL, depth, fees, etc.)
2. Atomic economics (the math the user asked about)
3. **Incentive layer** (rewards programs, points, airdrops — read 2-3 sources)
4. Verdict that incorporates 1+2+3

Skipping step 3 produces "technically correct but commercially wrong" verdicts.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (under a new "Incentive layer" section or extending the chain-screening section):

### Always check the incentive layer before reporting a chain's economics
For any DEX yield or chain-scouting question, before computing fees-only APR, search:
- `<chain>.network/blog` for "points" / "season" / "liquidity mining"
- `liquidity.<chain>.network` or similar subdomain
- DefiLlama Yields filtered by chain
- The DEX's own farms/incentives page

The gap between baseline and incentive-layer APR is typically 10-100× on newer chains. A scout report without this step is misleading.

### Epoch-end-capture pattern for finite-window rewards
Hold through the final epoch, exit at end, swap rewards to stables immediately. Don't redeploy until next season is confirmed. Especially important when the reward token is in a confirmed downtrend.

### Hedge LP volatile-token exposure during incentive windows (>$5k size)
Short the volatile token on a CEX perp for ~50% of LP value during the capture window. Protects realized rewards against further token depreciation. Skip below $5k LP size — spreads/funding eat the benefit.

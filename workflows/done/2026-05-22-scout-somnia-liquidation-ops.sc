---
domain: defi-liquidations
source_task: 2026-05-22-scout-somnia-liquidation-ops.md
date: 2026-05-22
keywords: [somnia, chain-screening, dex-depth-wipes-bonus, tripwire-thresholds, voltiq-keeperless, aave-v3-fork-inherits-flashloan, small-chain-defer]
---

## Extracted Knowledge

### Thin DEX depth is the silent kill for a small-chain liquidation lane
A chain can have a permissionless Aave V3 fork AND a flash-loan source AND msg.sender
bonus AND look perfect on the 4-of-5 checklist. If the deepest DEX on the chain has
<$5M TVL, **any liquidation >$5-10k will eat 5-20% slippage and wipe the bonus**.

This is the structural blocker that disqualifies most new EVM chains in their first
6-12 months. The bonus is typically 5-15%; slippage on a thin pool can exceed that
on a single swap. Net outcome: profitable on paper, lose money in practice.

Concrete example: **Somnia (2026-05-22)** had Tokos (Aave V3 fork, ~$1M TVL) + QuickSwap
($693k TVL). Liquidating a $50k position would push ~7% of pool liquidity through
QuickSwap — slippage alone consumes the 5-7% Aave V3 liquidation bonus.

### Tripwire thresholds for "revisit small chain" decisions
Don't just SKIP / WAIT — write down the conditions that would flip the verdict. For
chain re-evaluation:

```
Revisit when ALL of:
1. DefiLlama chain TVL > $25M (= depth for $50k+ fires without saturating)
2. ≥1 DEX on the chain > $5M TVL (= absorbs liquidation collateral with <2% slippage)
3. Target lending protocol's LiquidationCall event count > 4 in trailing 30d
   (= real liquidation activity, not a theoretical opportunity)
```

Without these tripwires, "we'll revisit in 6 months" becomes "we never revisit".

### Aave V3 forks inherit `flashLoanSimple` for free
When a lending protocol is described as "built on Aave v3 infrastructure" or "Aave v3
fork", you can probably assume `Pool.flashLoanSimple(receiver, asset, amount, params,
referralCode)` works without verification — it's part of the canonical Pool ABI and
no fork has reason to remove it. Saves a research step.

Still verify before deploying capital — the rare fork that strips flash loans
(e.g. for risk reasons) does exist.

### Voltiq-class "keeperless reactive engines" disqualify a venue
A new protocol category to watch for: "reactive liquidation engines" that explicitly
eliminate the external liquidator role. The contract triggers liquidations internally
on a reactive cycle (similar to a Chainlink Automation but on-chain). There is no
`msg.sender` payout lane — it's analogous to Liquity Stability Pool socializing the
bonus, but more aggressive (zero external participant).

When scouting, search a candidate protocol's docs for "keeperless", "reactive",
"automated liquidation cycle" — these are red flags. SKIP, don't WAIT.

### CoinGecko `/onchain/networks/{slug}` may not exist for new chains
For Somnia, CoinGecko on-chain endpoints aren't fully populated yet. Fall back to:
1. DefiLlama (`defillama.com/chain/<name>` + protocol pages)
2. Messari quarterly reports (`messari.io/report/state-of-<chain>-q<N>-<year>`)
3. Protocol's own docs + blog
4. Blockscout explorer for unverified contract addresses

Lesson: don't gate your scout on a single data source. Cross-reference 2-3.

### Chain age + TVL are leading indicators of liquidation lane viability
Rough empirical thresholds from our fleet (Berachain, HyperEVM, Monad, Sonic, Ink):
- Chain age < 6 months → almost always too thin for race
- Chain TVL < $25M → top DEX usually <$5M → slippage wipes bonus
- Lending protocol TVL < $5M → liquidation frequency too low to amortize indexer + RPC cost

Somnia checks all three "too small" boxes. The math doesn't work, regardless of
how nicely the protocol stack composes on paper.

## Failure Modes Observed

### "Looks perfect on paper, fails in practice" — the slippage-wipe pattern
On the first read of Somnia's protocol stack, Tokos (Aave V3 fork + flash loans +
permissionless) looked like a clear GO. Only when adding up "what does it take to
swap $50k of seized collateral through a $693k-TVL DEX" did the structural blocker
become obvious. Apply the 5th gate (DEX depth × likely fire size) BEFORE the gates
1-4 (permissionless, bonus, atomic, flash loan) when scouting small chains — it's
the fastest disqualifier and saves time on protocol-mechanics reading.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extending "Targets filter checklist"
and "Chain expansion decision framework" sections):

### DEX-depth gate is the first gate for small chains
For any new chain scout, compute the slippage-to-bonus ratio BEFORE reading lending
protocol mechanics:
- Find top DEX TVL on the chain (DefiLlama)
- Estimate likely fire size: median $5-10k for small chains, $50-100k for established
- If `(fire_size / top_dex_tvl) × 2.0 ≥ bonus_pct`, the lane can't be profitable
- Above ~3% of pool depth per swap → catastrophic slippage on thin V3 pools

This single check disqualifies most new chains in <5 minutes. Apply before any
contract-level mechanics research.

### Write down tripwires when verdict is WAIT
Don't say "revisit in 6 months". Write the specific numeric thresholds that flip
the verdict (chain TVL, top DEX TVL, monthly liquidation count). Then set the actual
calendar/monitoring tripwire — e.g. monthly cron that scrapes DefiLlama for the
chain + alerts when thresholds cross.

### New disqualifying category: "keeperless reactive" engines
Protocols like Voltiq explicitly eliminate the liquidator role. Trigger keywords in
docs: "keeperless", "reactive cycle", "automated internal liquidation". These are
structural SKIPs, not WAITs. Add to the existing list (Blur Blend, Dolomite,
Liquity V2 forks).

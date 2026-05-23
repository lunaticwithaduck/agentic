---
domain: defi-liquidations
source_task: 2026-05-22-somnia-slp-flashloan-amplification.md
date: 2026-05-22
keywords: [twab, time-weighted-balance, flash-loan-amplification, off-chain-points-indexer, lp-rewards-program-design, anti-gaming, somnia-slp, loyalty-multiplier]
---

## Extracted Knowledge

### TWAB defeats flash-loan amplification by design
Time-weighted average balance (TWAB) accounting is the explicit anti-flash-loan mechanism for any LP rewards program. The math:
- Reward weight = `Σ (balance_at_tick × duration_at_tick)`
- Flash loan must repay in the same block → net balance unchanged at block boundary
- Indexer samples between blocks → never captures the inflated state
- Even if a flash inflated to $10M for one block, contribution to a weekly epoch's score is bounded by `1 block / (7 × 24 × 3600 / blocktime)` ≈ effectively zero

Programs that announce "time-weighted" or "TWAB" in their docs are signaling: **don't try this**. The mechanism is correct.

### Diagnostic test: is a rewards program TWAB or snapshot?
Before assuming you can amplify a points program with a flash loan, run this test:
1. Open browser devtools on the program's dashboard, watch the XHR request that populates "your points / score"
2. Transfer the relevant token (LP, staked token, etc.) in or out by a meaningful amount
3. Refresh — does your score change immediately in the same block?
   - **Yes, per-block:** snapshot or per-block accounting. Flash-loan amplification might work
   - **No, only after some delay (1m, 1h, 24h):** TWAB or hourly indexer. Flash-loan amplification useless

This works for any off-chain-accounted points program. On-chain points contracts can be inspected directly via `eth_call`.

### Recognize anti-gaming design markers
A rewards program is structurally flash-loan-resistant if its docs / dashboard show ANY of:
- "Time-weighted" / "TWAB" / "duration" / "continuous accrual"
- "Loyalty multiplier" / "duration multiplier" that resets on withdrawal
- "Whale caps" / "diminishing returns above $X"
- "Hourly snapshots" / "off-chain indexed"
- "Streak counter" / "consecutive epochs"

Multiple of these in the same program = no atomic attack vector. Move on.

### Off-chain points indexers have no on-chain attack surface
If a points program's accounting is computed off-chain (indexer samples on-chain balances at intervals and writes scores to a database), there is **no on-chain contract to manipulate**. The only attack surface is:
- The indexer's RPC source (DoS, but pointless for amplification)
- The sampling cadence (predictable for snapshot-style only)
- Race condition between indexer sample and your block submission (impossible to win deterministically)

This is the dominant design pattern for modern points programs (Hyperliquid, Blast, Somnia SLP, etc.). Don't waste time on amplification attempts against off-chain-indexed programs.

### Loyalty multiplier resets are a hidden anti-flash-loan tax
Programs that scale multipliers with **uninterrupted holding duration** make flash loans actively destructive: a 1-block flash inflow that comes back out can RESET your loyalty multiplier, *reducing* your effective rewards rate. The mechanism is non-obvious from a casual read of the docs — always look for "loyalty resets on withdrawal" or "consecutive epochs" language.

### Tokos (and Aave V3 forks broadly) don't list LP tokens as collateral
For Aave V3-style lending forks, the listed reserves are almost always single tokens (USDC, ETH, WSOMI, etc.), never AMM V2 LP tokens. The reason is oracle manipulation: an LP token's "price" must be derived from its constituent reserves and a manipulation-resistant oracle, which most fork deployments don't bother to wire up.

Practical implication: the "deposit LP → borrow → buy more LP → loop" strategy isn't available on the typical Aave V3 fork. If looping LP positions is the goal, look for specialized leveraged-LP protocols (Gearbox-style credit accounts, Sommelier vaults, Index Coop products) rather than generic lending markets.

## Failure Modes Observed

### Initial instinct ("can we flash-loan it?") would have been wrong without docs check
Without checking the SLP accounting docs, the instinct "we have a flash loan source AND an LP rewards pool, there must be an angle" would have wasted hours. The doc check took 10 minutes and definitively closed the question. **Always read the rewards program's mechanic docs before designing a strategy** — the program designers explicitly signed up to defeat your approach.

Pattern: when evaluating any "can I amplify rewards with X" idea, first 10 minutes go to "how does the rewards program account for balance?" If the answer is TWAB / continuous / off-chain-indexed → close the investigation immediately.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extending the new "Incentive layer" / arb sections):

### TWAB rewards programs are structurally flash-loan-resistant
Before designing any "flash-loan to amplify rewards" strategy, check the program's accounting mechanic. If docs say "time-weighted", "continuous accrual", "hourly snapshots", "loyalty multiplier", "duration-weighted", or any equivalent — flash loans cannot amplify. Close the investigation.

### Diagnostic: in/out token transfer + devtools XHR
For off-chain-indexed points programs, the fastest way to verify TWAB vs snapshot is to watch the dashboard XHR while transferring the relevant token in/out. If score changes in the same block → snapshot. If only after an interval → TWAB.

### Aave V3 forks don't list LP tokens as collateral
The "loop your LP into a lending market" play is almost never available on a generic Aave V3 fork (Tokos, HyperLend, HypurrFi, Bend, etc.). Look for specialized leveraged-LP protocols instead, or treat the LP as fee-yield-only.

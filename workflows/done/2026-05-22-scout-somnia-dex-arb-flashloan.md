---
title: Scout Somnia for atomic DEX-DEX arb with flash loans (≥$50/fire net)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Determine whether Somnia network has atomic DEX-DEX arbitrage opportunities profitable
at ≥$50 net per fire, fundable via on-chain flash loan.

## Steps
- [x] Verify flash-loan source on Somnia (Tokos `flashLoanSimple` ABI + fee)
- [x] Inventory price gaps across QuickSwap / Somnia Exchange / Somnex for WSOMI/USDC.e/WETH
- [x] Estimate gas per atomic arb tx on Somnia mainnet
- [x] Compute max profitable trade size given pool depths + observed gap
- [x] Check competition signal
- [x] Verdict: GO / WAIT / SKIP

## Outcome

Completed 2026-05-22. Verdict: **SKIP** (re-evaluate as WAIT in Q4 2026).

### Verified facts

**Flash loan source**
- Tokos Pool (Aave V3 fork): `0xEC6758e6324c167DB39B6908036240460a2b0168`
- PoolAddressesProvider: `0x1C13Fea2A9a3Ae9962f12B6afAC1AFcd8205f752`
- `flashLoanSimple()` exposed — standard Aave V3 ABI
- Fee: 0.05% (FLASHLOAN_PREMIUM_TOTAL default, unchanged)
- Tokos TVL only $241k — supplies are thin but support $5k-scale flash sizes

**DEX price divergence (WSOMI/USDC.e)**
| DEX | Pool reserve | 24h vol | WSOMI in USDC.e |
|---|---|---|---|
| QuickSwap (0.05%) | $265.7k | $23.2k | $0.1629 |
| Somnia Exchange V2 | $57.2k | $7.7k | $0.1622 |
| Somnex | $5.3k | $241 | ~$0.1625 |

- Max observed gap: ~0.4% (QS vs SomEx) — needed 2.4%+ to clear costs
- WSOMI/WETH only on QuickSwap ($130 reserve, dead)
- USDC.e/WETH: no pool anywhere → no triangular path

**Gas**
- Somnia base: 6.16e-9 USD/gas
- 500k-gas atomic arb ≈ $0.003 (negligible, not a constraint)

**Competition**
- Couldn't query Blockscout FlashLoan events via WebFetch (API not exposed cleanly)
- Chain-wide 24h DEX volume only $31k → no economic surface for serious MEV competition

**Profit math (best case today)**
- $500 max viable trade size before SomEx slippage swamps the gap
- $500 × 0.4% gap = $2 gross
- Minus 0.05% flash + 0.05+0.30% DEX fees ≈ $2 fees
- Net per fire: ~$0 (break-even at best)

### Structural blockers
1. Only one liquid overlapping pair (WSOMI/USDC.e) on more than one DEX
2. Second-DEX pool depth caps fire size at ~$500-1k
3. Observed gap is 0.4% — too tight for ANY trade size to clear $50 net
4. Total chain DEX volume $31k/day = doesn't move enough for repeatable opportunities

### Tripwires to revisit (any one flips us to "consider building")
- Somnex or Somnia Exchange V2 grows past **$200k TVL on a shared pair** (today: $5k / $57k)
- A **WETH-paired pool appears on a second DEX** (enables triangular routes)
- New bridge/listing event creates a **temporary listing-lag gap** (event-driven, not steady-state — would still be one-off, not bot-worthy)
- Tokos `FlashLoan` event count > 10/day in trailing 30 days (proves chain has reached arb-bot density)

### Recommendation
- Park Tokos Pool + GeckoTerminal pool addresses in monitoring sheet
- Optionally set a lightweight passive monitor: fetch GeckoTerminal `/onchain/networks/somnia/pools` weekly, alert if any two pools on same pair show >1.5% gap AND both have >$50k reserve
- Re-scout in Q4 2026 (~5 months out)
- **Do NOT build infra for Somnia today.** Same conclusion as the prior liquidation scout: chain is too thin to support our economics.

## Skill candidate evaluation
- Technologies/frameworks touched: Somnia chain, Tokos (Aave V3 fork) flashLoanSimple, GeckoTerminal pool queries, DEX-DEX spot arb economics, slippage math on thin V2/V3 pools
- Domain-specific knowledge involved: arb-specific cost stack (flash fee + 2×DEX fee + 2-leg slippage + gas) and the "second-DEX pool depth is the binding constraint, not the first" pattern; the GeckoTerminal base/quote-flip pitfall; chain-wide DEX volume as an arb-viability indicator
- Verdict: **GENERATE**
- Reason: Reusable arb-economics formulas + small-chain disqualifying patterns not present in the existing skill (which is mostly liquidation-focused). New for the domain: GeckoTerminal price normalization, second-pool-depth binding constraint, chain-wide-volume indicator.

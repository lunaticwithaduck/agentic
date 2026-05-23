---
domain: defi-liquidations
source_task: 2026-05-22-scout-somnia-dex-arb-flashloan.md
date: 2026-05-22
keywords: [dex-dex-arb, flash-loan-arb, atomic-arb, somnia, geckoterminal-base-quote, second-pool-depth-binding, chain-wide-volume-gate, tokos]
---

## Extracted Knowledge

### Atomic DEX-DEX arb cost stack (full formula)
```
gross_profit = trade_size × (gap_pct - 2 × dex_fee_pct)
                                  - flash_fee_pct
                                  - slippage_A_pct - slippage_B_pct
net_profit   = gross_profit × trade_size - gas_usd
```
Where:
- `flash_fee_pct` = 0.05% on Aave V3 / Aave V3 forks (default unchanged); 0% on Morpho/Balancer
- `dex_fee_pct` = 0.30% V2, 0.05/0.30/1.0% V3 (per-tier)
- `slippage_X_pct` ≈ `trade_size / pool_reserve` on V2 (linear), modified by tick liquidity on V3

For ≥$50 net, **gap × trade_size must clear ~$60-100** depending on stack. On a thin chain, the binding constraint is rarely the gap — it's the smaller pool's depth.

### The "second pool depth is the binding constraint" rule
For DEX-DEX arb, you can't trade more than the **smaller** of the two pools can absorb at acceptable slippage. The bigger DEX is cosmetic — if pool B is $50k, you can only push $500-1k through it before slippage eats the gap. Always size to `min(reserve_A, reserve_B) × 0.01` for ~1% slippage budget per leg.

Practical implication: a $500k pool paired with a $50k pool gives the same max trade size as two $50k pools. **Stop reporting top-DEX TVL — report the second-deepest pool depth on the specific pair you're arbing.**

### GeckoTerminal base/quote-flip pitfall
GeckoTerminal pool data flips base/quote depending on token order in the pool. For a `USDC.e/WSOMI` pool, the "price" reported is USDC.e in WSOMI (not WSOMI in USDC.e). For `WSOMI/USDC.e` pools, it's the inverse.

**Always normalize before comparing prices across pools** — re-derive from `reserves.token0 / reserves.token1` if needed. Real example: a "$0.998 vs $0.162" gap that's actually a base/quote flip showing identical $0.163 prices.

### Chain-wide DEX volume is a sanity check for arb viability
If a chain's TOTAL 24h DEX volume is <$100k, no atomic arb business can sustain there. Below ~$1M/day total, arb is event-driven (new listings, oracle hiccups) not steady-state. Use DefiLlama or CoinGecko to check chain-wide DEX volume before sizing any arb opportunity.

Somnia 2026-05-22: chain-wide DEX volume = $31k/24h. Even with $50-100 in winnable arb per fire, you'd need very lucky timing. **Volume floor for arb = $1M/day chain-wide**.

### Tokos (Somnia Aave V3 fork) flash-loan endpoint
- Pool: `0xEC6758e6324c167DB39B6908036240460a2b0168`
- PoolAddressesProvider: `0x1C13Fea2A9a3Ae9962f12B6afAC1AFcd8205f752`
- `flashLoanSimple(receiver, asset, amount, params, referralCode)` — standard Aave V3 ABI, unchanged
- Fee: 0.05% (FLASHLOAN_PREMIUM_TOTAL default)
- Available liquidity caps the borrowable size — Tokos active loans $337k means flash borrows above ~$200k will fail

### Somnia gas is essentially free
Base price ~6.16e-9 USD/gas. A 500k-gas atomic tx costs $0.003. **Gas is not a constraint on Somnia** — but DEX depth and gap size still are. Cheap gas amplifies the chain's appeal for high-frequency strategies, but only AFTER the arb gap size + pool depth combination clears costs at SOME trade size.

### Flash-loan-funded arb is structurally different from inventory arb
- **Inventory arb**: hold tokens, swap when profitable. Profit per fire is unbounded by flash fee but bounded by inventory.
- **Flash-loan arb**: borrow per-fire, swap A→B→A, repay + premium. No inventory required; size limited only by flash-loan-source liquidity.
- **For a multi-chain bot**, flash-loan arb is strictly better: no capital lockup, no chain-specific inventory rebalancing, no withdrawal limits. Tax/accounting also simpler (every fire is its own self-contained P&L).

### Arb tripwires for revisiting small chains
Set numeric tripwires for arb specifically (different from liquidation tripwires):
1. **Second-deepest pool on a tradeable pair >$200k** (so $5k-10k fire size becomes viable)
2. **A WETH or WBTC pool appears on ≥2 DEXes** (enables non-stablecoin triangular routes)
3. **Chain-wide 24h DEX volume >$1M** (general arb-viability gate)
4. **Flash-loan-source liquidity >$500k** (allows $50k+ borrows for occasional big fires)

If all four cross simultaneously, the chain probably has a real arb lane. Until then, the math doesn't work no matter how clever the routing.

## Failure Modes Observed

### Volume floor is the most-missed gate
Before this scout, the obvious answer to "is Somnia worth arbing" felt like "depends on the gaps". Actually, the chain-wide $31k/day volume IS the answer — no chain that small has the *cadence* of price movements that creates repeated arb opportunities. Even if a gap appears, it appears once a week, not 10× per day. Bot infra needs cadence, not just episode-level profitability.

Lesson: check chain-wide DEX 24h volume FIRST as a kill-switch before any other arb analysis. Below $1M/day → SKIP regardless of everything else.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extending the new "DEX-depth gate" section
with arb-specific guidance):

### DEX-DEX arb cost stack (separate from liquidation math)
For atomic arb funded by flash loan, the cost stack is different from liquidation:
- Flash fee × trade_size (0.05% Aave / 0% Morpho/Balancer)
- 2 × DEX fee × trade_size (typically 0.05-0.30% per leg)
- Slippage on BOTH pools (binding constraint = smaller pool)
- Gas (chain-dependent; <$0.10 on cheap L2/altchains, >$5 on Ethereum L1)

Skip arb research if chain-wide 24h DEX volume <$1M. Cadence is the silent killer.

### "Second pool depth" rule for any DEX-DEX trade
Max profitable trade size = `min(pool_A, pool_B) × ~1%` — the bigger pool's depth doesn't matter. Always report the **smaller pool's reserve** when assessing arb viability between two DEXes on the same pair.

### GeckoTerminal price normalization
Pool data flips base/quote by token order. Always re-derive prices from `reserves.token0 / reserves.token1` before cross-DEX comparison. Don't trust the displayed quote price across pools.

### Tokos Pool reference (Somnia)
Address `0xEC6758e6324c167DB39B6908036240460a2b0168` exposes Aave V3 `flashLoanSimple` at 0.05% fee. Catalogued for future re-evaluation when Somnia DEX depth grows.

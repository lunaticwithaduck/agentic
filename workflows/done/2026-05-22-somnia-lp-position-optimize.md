---
title: Optimize existing $1k QuickSwap WSOMI/USDC.e LP — concentrate + check SOMI incentives
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
User has $1k full-range V2-style LP in QuickSwap WSOMI/USDC.e on Somnia, set & forgotten.
Determine optimal action: stay, concentrate, withdraw, or stack incentives.

## Steps
- [x] Research SOMI incentive programs
- [x] Recommend concentrated-range bands given WSOMI volatility
- [x] Compare stay+optimize vs withdraw+redeploy
- [x] Output: one-page decision

## Outcome

Completed 2026-05-22. **Verdict: hold for 7 days to capture final SLP epoch, then withdraw.**

### Critical finding the prior scout missed
The pool is enrolled in the **Somnia Liquidity Points (SLP) program** (`liquidity.somnia.network`). The user's $1k is actually earning **~152% APY in SOMI rewards** + ~1.6% in trading fees, not just 1.6%. The prior scout completely missed this incentive layer.

### SLP program status
- Season 1 currently on **Epoch 13 of 13** — final week before program ends
- Total allocation: 1,000,000 SOMI (~$163k) over 90 days
- DEX pool multiplier: 0.1× (QuickSwap, Somnex, Somnia Exchange comparable)
- Lending multiplier: 0.2× (Tokos)
- LST multiplier: 0.3×
- $2M TVL cap per pool (uncertain if currently filled)
- **No Season 2 publicly announced** as of 2026-05-22

### WSOMI volatility (CoinGecko)
- Current: $0.1632
- 7-day range: $0.156–$0.174 (±5.5%)
- 20-day range: $0.157–$0.201 (±12% / 28% peak-to-trough)
- Trend: confirmed downtrend (ATH $1.84 Sep 2025 → $0.148 ATL Mar 2026)

### Recommendation: capture final epoch, then exit

**Week of 2026-05-22 → 2026-05-29:**
- Hold position. At ~152% APY × 7 days × $1000 = ~$29 in SOMI rewards realized
- Optional hedge: short ~$500 SOMI on Binance/MEXC perp to neutralize SOMI price risk during the final week (only worth the effort if they're not running this everywhere)

**End of Season 1 (~2026-05-29 to ~2026-06-05):**
- Withdraw LP from QuickSwap
- Claim SOMI rewards (`liquidity.somnia.network`)
- Swap claimed SOMI → USDC.e on QuickSwap (lock in the gain, avoid further SOMI depreciation)
- Net realized: ~$1030 worth (the $1k principal + ~$30 in claimed SOMI)

**Post-Season 1:**
- Monitor `blog.somnia.network` and `liquidity.somnia.network` for Season 2
- If Season 2 launches with comparable multipliers (>50% APY equivalent): redeploy
- If not / unfavorable terms: keep capital out — the pool's intrinsic fee APR is only 1.6%, not worth the WSOMI exposure

### Concentrated-range note
QuickSwap on Somnia is V2 (0.05% fee, full-range only) — no V3 concentrated tier exists today. If V3 launches, recommended band would be ±15% (~$0.139-$0.188) given the 28% monthly range. Until then, no concentration play is available.

### Alternative if exiting now
- Tokos USDC.e supply APR (estimated): 3-8% base + 0.2× SLP multiplier ≈ 30-60% effective
- Would forfeit the final epoch (~$29) and lower expected return for the next 7 days
- **Don't switch now** — capture the SLP epoch first, evaluate Tokos vs LP after Season 1 ends

## Skill candidate evaluation
- Technologies/frameworks touched: Somnia Liquidity Points (SLP) program, QuickSwap on Somnia (V2 only, no V3 tier), Tokos lending APR estimation, concentrated-LP band sizing from volatility data
- Domain-specific knowledge involved: SLP multiplier structure (DEX 0.1× / Lending 0.2× / LST 0.3×), the "scout missed the incentives layer" failure pattern, epoch-end-capture-then-exit timing pattern
- Verdict: **GENERATE**
- Reason: Adds the incentive-program-layer knowledge that was completely absent from the prior two Somnia .sc files. The "scout output looks correct but missed the rewards program" failure mode is reusable across any new-chain scout.

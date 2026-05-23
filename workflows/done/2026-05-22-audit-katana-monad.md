---
title: Quick audit — is Katana or Monad worth a liquidator lane?
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Verdicts
- 🟢 **Monad: STRONG YES** — port Felix → Monad lane next
- 🟡 **Katana: SOFT NO** — revisit if vault bridge or pool incident drives borrows

## Monad findings (chainId 143)
- Morpho TVL: $154M; also Euler V2 $88M, Curvance $62M, Neverland $44M (multi-protocol ecosystem)
- **$21.6M at-risk** across 11 positions at HF≤1.10 (vs Felix's $1.1M)
- Whale cluster: 4× wstETH/WETH positions @ HF 1.002 totaling **$16.5M** — single LST peg event triggers all
- Recent 60d realized seize: **$342k** across 20 liquidations (one $320k single fire on syzUSD/USDC in April)
- DEX ecosystem includes Uniswap V3, V4, Pancakeswap V3, **Curve** (critical for wstETH/WETH), LFJ V2 — multi-DEX best-quote pattern works
- Code reuse: ~90% from Felix; multi-DEX catalog from project-x integration pattern
- Estimated effort: 1-2 days port + 4 hours multi-DEX wiring
- Expected revenue: $500-$5k/mo based on historical flow, with potential for $50k+ in a Lido perturbation event

## Katana findings (chainId 747474)
- Morpho TVL $133M but at-risk inventory only $1,020 — yUSD/vbUSDC dust positions HF 0.05–0.09
- 60d seize total: $26k across 20 events (mostly small)
- Position density too low to justify a dedicated lane right now
- Mostly "vault bridge" (vb prefix) tokens — niche pairs, but borrowers seem well-managed

## Decision framework recap (from prior research)
1. ✓ Morpho deployed
2. **Monad: 21.6M ≥ $50k threshold ✓ | Katana: $1k ✗**
3. **Monad: $342k seized real flow ✓ | Katana: $26k mostly small ✗**
4. Block time fits us (both < 1s, but Monad currently has less established MEV competition than Base)
5. Code reuse high for both

## Outcome
Completed 2026-05-22. Monad is the next lane to build. Katana parked for later — could revisit quarterly if at-risk inventory grows.

## Completion
Run `/complete workflows/tasks/2026-05-22-audit-katana-monad.md`.

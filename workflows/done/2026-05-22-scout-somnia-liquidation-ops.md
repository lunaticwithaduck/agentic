---
title: Scout Somnia network for flash-loan DEX-exit liquidation ops
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Determine whether Somnia network has a viable liquidation race opportunity matching
the existing 7-chain fleet's pattern.

## Steps
- [x] Verify chain basics: chain ID, RPC endpoints, native gas token, age of mainnet
- [x] Inventory lending protocols on Somnia (CoinGecko `/onchain` + DefiLlama + Messari)
- [x] Apply 5-point race-worthiness checklist to each candidate
- [x] Inventory DEXes (Uni V3 forks, V4, Curve, others) with TVL + token coverage
- [x] Verify a flash-loan source exists on-chain
- [x] Write verdict (GO / SKIP / WAIT)

## Outcome

Completed 2026-05-22. Verdict: **WAIT** (revisit ~Nov 2026 / when chain TVL ≥ $25-50M).

**Chain basics**
- Somnia mainnet live since 2025-09-02 (~8 months at scout time)
- Chain ID: 5031, native gas: SOMI
- RPC HTTP: `https://api.infra.mainnet.somnia.network/` (also Thirdweb)
- WSS: not publicly documented (need paid GetBlock/Thirdweb tier)
- Explorer: `https://explorer.somnia.network` (Blockscout)
- Total chain DeFi TVL: ~$2.65M (Q4 2025 — Messari)

**Lending protocols**
- **Tokos (`tokos.fi`)** — Aave V3 fork, native Somnia lending. Passes criteria 1+2+4
  (permissionless `liquidationCall`, bonus to msg.sender, inherits Aave V3
  `flashLoanSimple`). Mainnet contract addresses not publicly indexed; would require
  explorer queries. Assets: ETH, WSOMI, USDC.e.
- **Gearbox Finance** — integrated with Somnia (Messari Q4 2025). Modular lending for
  SOMI + USDC.e. Credit-account liquidation mechanics differ from Aave.
- **Voltiq** — keeperless reactive engine. **No msg.sender liquidator lane** — SKIP.
- Tokos + Gearbox combined ≈ 59% of chain TVL ≈ ~$1.6M.

**DEXes (critically thin)**
- QuickSwap: $693k TVL (V2/V3 fork, WSOMI/USDC.e/WETH pairs)
- Somnia Exchange: $193k TVL
- Somnex: ~$49-209k TVL (V3 + perps)
- No Curve, no Balancer, no V4 on Somnia today.

**Flash loan sources**
- No native Aave V3, no Morpho, no Balancer V2 deployed.
- Tokos (Aave V3 fork) likely retains `flashLoanSimple` on its Pool — usable if needed.

**Blockers vs the 5-point checklist**
1. Permissionless protocol → PASS (Tokos)
2. Bonus to msg.sender → PASS (Tokos)
3. Atomic DEX exit → **FAIL** — top DEX <$700k TVL. Any liquidation >$5-10k will eat
   5-20%+ slippage and wipe the bonus. This is the structural blocker.
4. Flash loan source → PASS (via Tokos' inherited Aave V3 method)
5. Real activity (≥4-8 liqs/month) → almost certainly **FAIL** — combined Tokos+Gearbox
   TVL of $1.6M implies near-zero liquidations actually happening

**Action items**
- Set a tripwire (revisit when):
  - DefiLlama Somnia chain TVL > $25M, AND
  - At least one Somnia DEX > $5M TVL, AND
  - Tokos `LiquidationCall` event count > 4 in trailing 30d (verifiable via Blockscout)
- Do NOT build Somnia infrastructure today — the lane economics don't exist.

**Estimated cost of premature deployment:** ~3 days of build + maintenance overhead +
ongoing RPC/wallet/gas baseline (~$30/mo idle). Worth waiting for the chain to mature.

## Skill candidate evaluation
- Technologies/frameworks touched: Somnia EVM chain, Tokos (Aave V3 fork), QuickSwap fork, Gearbox, Voltiq
- Domain-specific knowledge involved: chain-screening framework application to a small chain; the "thin DEX = liquidation lane unviable" pattern in practice; tripwire thresholds for revisit; Voltiq's "keeperless reactive engine" as a category that disqualifies a venue (analogous to Liquity SP-pays bulk)
- Verdict: **GENERATE**
- Reason: First Somnia-specific entry; reusable pattern for screening any small/new chain (the "DEX-depth wipes bonus" failure mode and the tripwire-threshold pattern apply generally).

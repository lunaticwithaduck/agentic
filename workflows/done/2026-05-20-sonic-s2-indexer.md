---
title: Sonic Silo V2 — s2 indexer (discover silos + track borrowers via SiloLens)
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Steps
- [x] Project at /home/jojo/automation/sonic-silo/ with symlinked ethers
- [x] config.js with 64 silo addresses (from silo-contracts-v2 deployment JSON)
- [x] Silo V2 ABI (SiloConfig.getSilos/getConfig + SiloLens isSolvent/getUserLTV/maxLiquidation)
- [x] indexer.js — resolves market metadata, discovers borrowers via debtShareToken Transfer events, reconciles via SiloLens
- [x] Indexed all 64 markets in ~6 minutes

## Validation results
- **78 active borrowers across 11 active silos** (3.4× Bend's 23)
- **3 positions ALREADY INSOLVENT** in LBTC/scBTC market (LTV 120-130% vs LT 75%):
  - 0xddf6f217... debt 0.0372 scBTC (~$3.7k) LTV 130%
  - 0xeba10935... debt 0.0080 scBTC (~$800) LTV 124%
  - 0x42981436... debt 0.0051 scBTC (~$510) LTV 120%
- Top economic positions: $200k+ wS debts in S/USDC.e_borrowable_S
- Sonic activity is dramatically higher than Berachain — this is a real game

## Outcome

Completed 2026-05-20. Ported the indexer pattern from Bend to Sonic Silo V2. Architecture port leveraged the SiloLens helper (`0x925d5466...`) so we don't replicate Morpho-style HF math or oracle decoders — just call `isSolvent`, `getUserLTV`, `getLt`. Indexed all 64 silos in ~6 minutes via debtShareToken Transfer-from-zero scanning. Found 78 active borrowers and — critically — 3 positions that are ALREADY INSOLVENT (LTV > LT) in the LBTC/scBTC market, sitting unliquidated. These provide a real smoke-test target without needing oracle manipulation. Next: s3-s6 fast to attempt a real liquidation on anvil fork against these positions.

---
title: Sonic Silo V2 — research mechanics before porting Bend infrastructure
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Steps
- [x] Dispatched research agent for full architecture scope
- [x] Documented liquidation ABI, flash loan sources, DEX exits, oracle stack, top markets
- [x] Decision gate: GO — proceed to s2 (indexer)

## Key findings

### Architecture (KEY DIFFERENCE from Morpho)
- NOT a singleton — each market = 2 Silo contracts (Silo0+Silo1) + immutable SiloConfig
- 61 markets deployed on Sonic
- Discoverable via factory + `silo-core/deploy/silo/_siloDeployments.json`
- **`SiloLens` helper at `0x925d5466d4d5b01995e20e1245924ada6415126a`** exposes `isSolvent(silo, user)`, `getUserLTV(silo, user)`, `maxLiquidation(borrower)` — this MASSIVELY simplifies our port (no need to recreate Morpho HF math + per-oracle decoders)

### Liquidation ABI (verbatim)
```solidity
function liquidationCall(
    address _collateralAsset,
    address _debtAsset,
    address _user,
    uint256 _maxDebtToCover,    // pass type(uint256).max for partial-to-target
    bool    _receiveSToken      // true = always succeeds even without underlying liquidity
) external returns (uint256 withdrawCollateral, uint256 repayDebtAssets);
```
Permissionless. Hook contract is `PartialLiquidation`, NOT the Silo itself. Liquidation fee per-market (2.5-5%). Partial liquidation to target LTV (not 50%).

### Flash loan source — Beets Vault (Balancer V2 fork)
- Address: `0xBA12222222228d8Ba445958a75a0704d566BF2C8` (same as mainnet Balancer)
- Fee: **0%**
- Broad token coverage on Sonic (USDC.e, scUSD, stS, wS, etc.)
- Standard `flashLoan(receiver, tokens[], amounts[], userData)` ABI

### DEX exit
- **Shadow Exchange** (Uniswap V3 fork, uses `tickSpacing` not `fee`): SwapRouter `0x5543c6176feb9b4b179078205d7c29eea2e2d695`
- **ODOS** aggregator: `0xaC041Df48dF9791B0654f1Dbbf2CC8450C5f2e9D`
- **1inch**: `0x111111125421cA6dc452d289314280a0f8842A65`
- Plan: ODOS for best price; fallback Shadow `exactInputSingle` for known-deep pools

### Oracle stack
- Per-silo, configured in SiloConfig
- Mix of Chainlink push, Pyth pull, Redstone push, eOracle push
- **Skip implementing — use SiloLens.isSolvent() directly**

### Top markets (by community attention)
- `Silo_stS_S` — flagship looping market
- `Silo_wstkscUSD_USDC.e` — top stablecoin yield-loop
- `Silo_S_USDC.e` (+ borrowable variant)
- `Silo_WETH_USDC.e`
- 18+ Pendle PT silos — oracle-sensitive, frequent liquidations on time decay

### RPC quality (no surprises)
- publicnode WSS + HTTP free
- 1-second blocks, ~720ms finality
- Gas ~55 gwei × ~$0.30/S = ~$0.03 per liquidation tx
- Archive available via Ankr or dRPC

### Bot competition
- Silo publishes their own [LiquidationHelper](https://github.com/silo-finance/liquidation) and runs a public liquidation UI
- Assumed 2-4 serious bots, fragmented field
- Edge available via WSS + wall-clock pattern (same as won Bend loan-132)

## Verdict: GO

| Metric | Value |
|---|---|
| Build effort | 5-8 days (vs ~3 weeks greenfield — saved by Bend infra reuse) |
| Expected monthly capture | $7-20k (steady state, moderate competition) |
| TVL | $5.94M aggregate / 61 markets |
| 30d protocol fees | $171k → meaningful borrow activity |

## Outcome

Completed 2026-05-20. Silo V2 architecture is meaningfully different from Morpho Blue (per-market contracts, not singleton), but **SiloLens helper** at `0x925d5466...` exposes `isSolvent` + `getUserLTV` + `maxLiquidation` directly, eliminating the need to port HF math or oracle decoders. Liquidation function `PartialLiquidation.liquidationCall(collateral, debt, user, maxDebtToCover, receiveSToken)` is permissionless. Flash loan source: Beets Vault (Balancer V2 fork) at the canonical `0xBA12...` address, 0% fee. DEX exit via ODOS aggregator. RPC stack mirrors Bend (publicnode WSS, archive via Ankr). Go-decision confirmed: 5-8 day port for $7-20k/month expected capture. Next: s2 (indexer using SiloLens for solvency reads).

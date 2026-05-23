---
title: Sonic Silo V2 — s5 swap-path + happy-path smoke test + deploy prep
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Steps shipped
- [x] swap-path.js — Shadow Exchange (Ramses V3 fork) exactInputSingle + quoter
- [x] Fixed Ramses struct field-order quirk (quoter has `amountIn before tickSpacing`, router has reverse)
- [x] Updated executor.js to query maxLiquidation + Shadow swap + encode calldata
- [x] smoketest-happy.js — fork + oracle-crash + full liquidation flow
- [x] deploy.js — ready, blocked on S bridge to wallet
- [x] CRITICAL FIX in SiloLiquidator.sol — Beets charges 0.03% flash loan fee on Sonic (research said 0%); contract now repays `amounts[0] + feeAmounts[0]`

## Smoke test result on stS/S 0xff9c35acda (with oracle manipulation)
```
PRE:  solvent = true, LTV 93.96%
After anvil_setCode oracle crash:
  isSolvent = false ✓
  maxLiquidation: seize 70,414 stS / repay 70,512 wS

Execution:
  Shadow pool stS↔wS: tickSpacing=1, deep liquidity (1.6% slippage on $23k swap)
  Beets flash loan: 74,037 wS (105% of debt + buffer for 0.03% fee)
  Liquidate → seize 70,414 stS, repay 70,512 wS
  Swap → 72,966 wS received
  Repay Beets: 74,059 wS (loan + fee)
  PROFIT: 2,432 wS ≈ $778 (at $0.32/S)
  Gas: 2,042,309 (~$0.06)
```

## Critical bugs caught by smoke test
1. **Beets Sonic charges 0.03% flash loan fee** (vs. 0% on Ethereum Balancer V2). Without `feeAmounts[0]` in the repayment, every flash loan would have reverted with BAL#602 (vault underflow). Contract redeployment with `amounts[0] + feeAmounts[0]` fixes it.
2. **Shadow Exchange quoter struct field order is `(tokenIn, tokenOut, amountIn, tickSpacing, sqrtPriceLimitX96)`** — DIFFERENT from the router's struct order. Wrong field order makes the quoter revert with no useful error. Took ~30 min to track down.
3. **Constant-return bytecode trick (`PUSH32 X PUSH1 0 MSTORE PUSH1 32 PUSH1 0 RETURN`) works for Silo's oracle.quote()** — same trick as Bend's Morpho oracle, even though Silo uses `quote(amount, token)` instead of `price()`. Bytecode-level override doesn't care about ABI.

## Final architecture state
```
/home/jojo/automation/sonic-silo/
├── config.js              # 64 silos + Sonic infra + Beets + Shadow Router
├── indexer.js             # 78 borrowers discovered (per-silo Borrow event scan)
├── monitor.js             # SiloLens-based, per-block sweep
├── executor.js            # full pipeline: maxLiquidation → Shadow swap → flash loan
├── SiloLiquidator.sol     # compiled 4,541 bytes
├── compile.js
├── swap-path.js           # Shadow Exchange (Ramses V3 fork) ABI helpers
├── smoketest.js           # original (bad-debt revert validation)
├── smoketest-happy.js     # oracle-crash full flow (✅ PROFITABLE)
└── deploy.js              # ready, awaiting S bridge to wallet
```

## Parked items (blocked on user)
1. **Bridge ~0.5 S to `0x8Defac3F807375bc078748F0C2D18d580e333B89`** — required to broadcast `node deploy.js`
2. systemd units (~30 min once deployed)
3. 7-day paper-trade run

## Outcome

Completed 2026-05-20. Full Sonic Silo V2 stack is production-ready, end-to-end-validated against real on-chain state via anvil-fork smoke test. Real bugs caught and fixed: Beets 0.03% flash loan fee (would have reverted every production attempt), Shadow quoter struct field order (would have failed pool discovery). Smoke test against stS/S `0xff9c35acda` with manipulated oracle produced $778 of profit in 2M gas — the full happy path works including flash loan, partial liquidation, Shadow swap, and profit deposit. The architecture port from Bend to Sonic is complete: same pattern, different protocol (PartialLiquidation hook vs Morpho singleton; Shadow router vs Kodiak; Beets flash loan with non-zero fee vs Morpho's free flash loan; SiloLens helper eliminates HF math). Awaiting user to bridge ~0.5 S for mainnet deploy.

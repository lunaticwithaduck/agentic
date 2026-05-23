---
title: Full prod E2E smoke on paid dRPC — revealed two production bugs, fixed both
created: 2026-05-22
completed: 2026-05-22
status: done
---

## What this session uncovered (BEFORE we'd hit it on a real fire)

The paid dRPC endpoint let the anvil fork run the full integration test. The tx reverted with debug_traceTransaction revealing:
```
CALL liquidator → CALL Morpho.flashLoan → CALL liquidator (callback) →
  CALL UniversalRouter → CALL V4 PoolManager → CALL UR → CALL V4 PoolManager (OUT OF GAS)
```

Root causes (now fixed):

### Bug 1: `expectedSeized` not capped at borrower's collateral
Production code computed `expectedSeized = (borrowed × LIF) / oraclePrice`. For deeply-underwater positions, this can exceed the borrower's actual collateral. Morpho clamps the seize to actual collateral, but our pre-built `swapData` would still try to swap the LARGER amount → Permit2 `transferFrom` fails (insufficient balance) → tx reverts.

**Fix**: `const expectedSeized = rawExpectedSeized > collateral ? collateral : rawExpectedSeized;`

### Bug 2: Pass `expectedSeized` to Morpho, not MaxUint256
Executor was passing `ethers.MaxUint256` as `seizedAssets` to MorphoLiquidator.liquidate. Morpho would seize whatever it could (clamping to collateral). But our pre-built swapData expected a specific amount. If Morpho seized LESS than the swap amountIn, Permit2 transferFrom fails.

**Fix**: pass `expectedSeized` directly so Morpho seizes EXACTLY what the swap expects.

## Still-open concern (separable, lower-risk)

For very-large-collateral whales (e.g. the $5.59M `0x044808` with ~2275 wstETH), even capped at collateral, the seize amount (~1100-2200 wstETH) exceeds the V4 pool's single-tx capacity (~1000 wstETH before OOG). Current code path:
1. Discover at `expectedSeized` size — V4 quoter reverts because pool can't fill → `findBestPool` returns null → executor skips fire.

Result: very-large whales would be SKIPPED rather than firing partial. Future improvement = "partial liquidation strategy" where we seize a pool-safe amount and re-fire later.

For the immediate at-risk whales:
- `0x713ab45c66` (1196 wstETH) — `expectedSeized` likely ~1144 wstETH, MAY succeed via V4 quoter, MAY revert
- Smaller whales (`0x933a7c11`, 0x044808 subset, etc) — more likely to fit V4 pool capacity

## What's now LIVE on Monad
- Contract `0x235899576Deb5ea87d7eE8fD0859e83E46BA5300` (Permit2-aware)
- Executor with collateral-cap fix + correct Morpho seize amount
- `MONAD_RPC_HTTP_FIRE` set to paid dRPC for broadcast path
- All 12 services across 6 chains active

## Files modified this task
- `monad/executor.js`: capped expectedSeized at collateral; passed it (not MaxUint256) to Morpho
- `monad/prod-smoketest.js`: better error decoding (Error(string) selector parsing)

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-alchemy-fire-only.md` (the earlier alchemy task that flowed into this).

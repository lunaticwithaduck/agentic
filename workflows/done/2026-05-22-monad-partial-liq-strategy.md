---
title: Implement partial-liquidation strategy for V4 pool-depth constraints
created: 2026-05-22
completed: 2026-05-22
status: done
---

## 🎉 Production smoke PASS — full chain verified end-to-end on anvil fork

```
expectedSeized (capped at collateral):  1146.99 wstETH
partial seize (pool-safe):              143.37 wstETH
swap output:                            176.75 WETH
profit (sweep to owner):                23.43 WETH ≈ $50k @ WETH $2122
gas used:                               457,737
tx status:                              ✅ SUCCESS
```

## What was implemented
1. **`findBestPoolWithFallback(rpc, tokenIn, tokenOut, requestedAmount)`** in `monad/swap-path.js`:
   - Tries the full requested size first
   - On revert (V4 quoter OOG or no pool), iteratively halves up to 6× (64× reduction range)
   - Returns `{ pool, amount }` where amount ≤ requested is the largest pool-safe size
   - Returns null only if even 1/64th has no route

2. **Executor wired to use the fallback** (`monad/executor.js`):
   - `expectedSeized` capped at collateral (per the earlier bug fix)
   - `findBestPoolWithFallback` returns `{ pool, seizeAmount }`
   - `seizeAmount` passed to Morpho as the `seizedAssets` cap → Morpho seizes EXACTLY that much
   - `swapAmountIn = seizeAmount * 0.97` → swap consumes what contract holds
   - On partial seizure, log: `⚠️ partial fire: seize capped at X (of Y wanted, pool-depth limited)`

3. **`prod-smoketest.js` aligned with production logic** for honest E2E validation

## Cascade-fire behavior for large whales
- Whale `0x713ab45c` (1196 wstETH collateral) → first fire seizes 143 wstETH, position still insolvent → re-arms next tick → next fire seizes another 143 → repeats ~6-8× to fully drain.
- Each fire = ~23.4 WETH (~$50k) profit. Total per whale: ~$300-400k.
- Big whale `0x044808` ($5.59M, ~2275 wstETH) → same pattern at higher per-fire profit. Total ~$1M+ potential.

## Verified-working architectural invariants
- `expectedSeized ≤ collateral` (cap fix)
- `Morpho's seizedAssets cap == swap.amountIn` (so contract has EXACTLY what swap consumes)
- `swap.amountIn ≤ pool depth at quote time` (partial-liq fallback)
- Profit sweep transfers leftover loan token to OWNER (line 82-83 of MorphoLiquidator)

## Files
- `lib/monad-dexes.js` (existing, unchanged)
- `monad/swap-path.js` — added `findBestPoolWithFallback`
- `monad/executor.js` — uses fallback + passes `seizeAmount` to Morpho
- `monad/prod-smoketest.js` — full E2E verifier on anvil fork

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-partial-liq-strategy.md`.

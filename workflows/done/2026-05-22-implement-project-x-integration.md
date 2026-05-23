---
title: Implement project-x routing across HyperEVM lanes (Felix, HyperLend, HypurrFi)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Add `project-x` (HyperEVM's dominant V3 DEX) to the swap-path of all 3 HyperEVM lanes. Survey on 2026-05-22 showed project-x has 5-50× more liquidity than HyperSwap on every active HyperEVM token. Same Uni V3 ABI, same `swapTarget+swapData` pattern, no liquidator redeploy.

## Implementation

### Project-X contracts (newly catalogued)
Found by enumerating contracts created by the factory deployer `0x09529ed21d0f908fe472dadd8354cc3b24599bf6` via Etherscan V2 multichain (chainid 999), then verifying each candidate's `factory()` pointer matches the live Project-X factory `0xff7b3e8c00e57ea31477c32a5b52a58eea47b072`.

| Role | Address |
|------|---------|
| Factory | `0xff7b3e8c00e57ea31477c32a5b52a58eea47b072` |
| SwapRouter | `0xfd3fcc166b4691d425d6dc19487a2dff31417a75` |
| QuoterV2 | `0x239f11a7a3e08f2b8110d4ca9f6b95d4c8865258` |
| NFPM | `0xead19ae861c29bbb2101e834922b2feee69b9091` |
| WHYPE (WETH9) | `0x5555555555555555555555555555555555555555` |

Fee tiers `100/500/3000/10000` with canonical V3 tickSpacings `1/10/60/200` — vanilla Uniswap V3 fork, no Ramses-style weirdness.

### Architectural change
- New shared file: `/home/jojo/automation/lib/hyperevm-dexes.js` — exports `HYPEREVM_DEXES = [project-x, hyperswap-v3]` with factory/router/quoter addresses
- Rewrote `felix/swap-path.js`, `hyperlend/swap-path.js`, `hypurrfi/swap-path.js` to iterate all DEXes × all fee tiers, quote each, pick max `expectedOut`
- `findBestPool` returns `{ dex, router, fee, pool, expectedOut }` — the router belongs to the chosen pool's DEX
- Quote method: Project-X has QuoterV2 (precise on-chain quote), HyperSwap doesn't expose a quoter so we fall back to slot0 spot-price approximation
- Executors (`felix/executor.js`, `hyperlend/executor.js`, `hypurrfi/executor.js`) now pass `pool.router` as `swapTarget` instead of the hardcoded `swapPath.ROUTER`
- Felix executor also passes `expectedSeized` as `amountIn` so quotes are at the real trade size (HyperLend/HypurrFi do pair-selection probing without amountIn — they have a separate depth check after)

### No contract redeploys needed
Confirmed pre-implementation: `MorphoLiquidator.sol` and `HyperLendLiquidator.sol` both accept `swapTarget` as a per-call parameter with `approve+call` pattern. Router switching is fully off-chain.

## Verification

```
=== smoke test: Felix whale's market (0x8eecdd03, holds 0x24df4b7af6 $286k debt) ===
★ best: project-x
  fee tier: 100
  pool: 0x7cC3439CAbEb62A63732a63a0915999CEB125D5C
  router: 0xfd3fcc166b4691d425d6dc19487a2dff31417a75
  expectedOut: 323624899 (for 300000000 in)
```

```
=== Felix --discover sample ===
0xd7d3822065  best: project-x (fee 500)  ← market with $231k + $362k positions
0x707dddc200  best: project-x (fee 500)
0xf9f0473b23  best: hyperswap-v3 (fee 3000)  ← hyperswap still wins where it's deeper
0xace279b5c6  best: hyperswap-v3 (fee 100)
```

All 3 executors restarted cleanly at 10:48:06 UTC. Idle and armed for fires.

## Outcome
Completed 2026-05-22. The 3 HyperEVM liquidator lanes now route through whichever V3 DEX offers the best quote per fire. Project-X gets picked for the dominant markets (USDC/WHYPE, WHYPE/UBTC, the Felix stable-stable whale market); HyperSwap still wins on some pairs where it's genuinely deeper. No on-chain changes, no contract redeploys.

When the Felix whale `0x24df4b7af6` crosses HF<1.0, the swap will go through project-x's deep $11M+ pools instead of HyperSwap (which had no presence in the same pair).

## Out-of-scope follow-up
- Add `curve-hyperevm` and `balancer-v3-hyperevm` to the DEX catalog — they were 2nd-best for several stable-stable pairs in the survey
- Add HyperLend/HypurrFi `findBestPool` calls to pass `amountIn` once depth-check is integrated (currently they probe without it, but the pair-selection depth check catches bad pools downstream)

## Completion
Run `/complete workflows/tasks/2026-05-22-implement-project-x-integration.md`.

---
title: Scope the project-x DEX integration for HyperEVM lanes (Felix, HyperLend, HypurrFi)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Scope adding `project-x` as a routing option for HyperEVM liquidations. This document is the plan only — no implementation yet.

## Research findings

### Project-X is a vanilla Uniswap V3 fork
On-chain reads against top pool `0x6c9a33e3b592c0d65b3ba59355d5be0d38259285` (USDC/WHYPE 0.05%) confirm:

| Property | Value |
|----------|-------|
| Architecture | Standard Uniswap V3 (not Ramses-style tickSpacing-as-fee) |
| Pool interface | `IUniswapV3Pool` — `fee()`, `slot0()`, `liquidity()`, `tickSpacing()` all match |
| Factory | `0xFf7B3e8C00e57ea31477c32A5B52a58Eea47b072` |
| Fee tiers | `100/500/3000/10000` with tickSpacings `1/10/60/200` (canonical V3) |
| Factory owner | `0x153242182AcDF6B93eC0D2911734633A6C8442B8` |
| Network slug (CG) | `project-x` |

**This means our existing HyperSwap V3 swap-path code can be reused almost verbatim** — same ABI, same calldata format, same Quoter pattern.

### Our liquidator contracts are router-agnostic ← critical
`MorphoLiquidator.sol` (Felix) and `HyperLendLiquidator.sol` (HyperLend, HypurrFi) both accept `swapTarget` + `swapData` as **per-call parameters**:

```solidity
// MorphoLiquidator.sol:75
address swapTarget,    // freeform, not whitelisted
...
IERC20(mp.collateralToken).approve(swapTarget, collBal);
(bool ok, bytes memory ret) = swapTarget.call(swapData);
```

**No contract redeploy needed.** We just pass a different `swapTarget` + `swapData` from the executor.

### Still need (TODO during implementation)
- **Project-X Router02 address** (for building `exactInputSingle` calldata) — not in CG metadata, needs lookup via project-x docs / verified contracts on hyperevmscan
- **Project-X Quoter address** (for off-chain quote estimates) — same source
- Both should be on hyperevmscan.io with "Verified Contract" badges. ~30 min lookup.

## Strategy

**Multi-DEX best-quote pool selection** — keep HyperSwap, add Project-X, query both per fire, pick max amountOut.

This mirrors the structure of the Sonic swap-path fix shipped earlier today (compare across fee tiers within Shadow). Now we extend it across DEXes within a chain.

```
findBestPool(rpc, tokenIn, tokenOut, amountIn) {
  const candidates = await Promise.all([
    findHyperSwapPools(rpc, tokenIn, tokenOut, amountIn),
    findProjectXPools(rpc, tokenIn, tokenOut, amountIn),
  ]);
  // Each returns { dex, router, pool, fee, quote }
  return candidates.flat().sort((a,b) => b.quote - a.quote)[0];
}
```

The returned router becomes the `swapTarget` passed to the liquidator. The Approve+Call pattern in the liquidator stays unchanged.

### Why not full multi-DEX (all 11+ HyperEVM V3 forks)?
- Project-X covers most active liquidity (per the survey)
- Stable-stable pairs (Felix whale's market) may benefit from `curve-hyperevm` — note for a follow-up
- Diminishing returns: querying 11 DEXes per arm = 11× more RPC calls. Start with project-x; expand only if survey data justifies.

## Implementation plan

### Files to modify
1. `/home/jojo/automation/felix/swap-path.js` — add project-x pool discovery + quote, multi-DEX selection
2. `/home/jojo/automation/hyperlend/swap-path.js` — same
3. `/home/jojo/automation/hypurrfi/swap-path.js` — same
4. (Optional) `/home/jojo/automation/lib/v3-pool-finder.js` — shared helper across the 3 lanes to avoid code duplication

### Estimated effort
- Router/Quoter address lookup: **30 min**
- Code changes across 3 swap-paths (or 1 lib + 3 thin call sites): **1.5–2 hours**
- Smoke test against current armed position (Felix `0x24df4b7af6` via `executor.js --dry`): **30 min**
- Validation against survey data (recompute "best DEX per token" with the new code, match the report): **30 min**
- **Total: 3-4 hours**

### Risks
- **Low**: Same ABI as HyperSwap V3, router-agnostic liquidator contracts, no on-chain changes
- **Quoter behavior on revert**: Project-X V3 quoter may revert (vs return 0) when pool can't fill; must wrap in try/catch like the Shadow fix
- **Approval gas**: each call does `IERC20.approve(swapTarget, collBal)` — switching swapTarget per fire is already supported (allowance is set per-tx). No extra cost.
- **Aggregator MEV**: Project-X with $133M/24h USDC/WHYPE volume is a busy pool — sandwich risk is real. Our `amountOutMinimum = expectedQuote * 0.95` floor is the protection. If we see sandwiches, tighten to 0.97-0.98 for stable pairs.

### Validation criteria
- For the Felix whale `0x24df4b7af6` collateral→USDT0 swap path: new code should pick the deeper pool (project-x USDT0/* or HyperSwap, whichever quotes more for the actual seize size)
- For HyperLend whale `0x23edade4c7` WHYPE→USDC: new code should pick project-x ($11.5M pool) over HyperSwap (empty)
- Dry-run on at least one historical armed file should show expectedProfit ≥ previous run

## Decision
**Recommend: implement.** The survey showed structurally missed liquidity on every active HyperEVM token. The cost to ship is bounded (3-4 hours, no on-chain changes, low risk), and the upside is every future HyperEVM fire goes through the best available route. For the $286k Felix whale alone, this could be the difference between a profitable liquidation and a failed/marginal one.

Order of work (recommended): build the shared `lib/v3-pool-finder.js` that knows about both HyperSwap V3 and Project-X factories, then have the 3 chain swap-paths call into it. Cleaner than 3 parallel edits.

## Outcome
Completed 2026-05-22. Scope is recommend-to-implement; not started yet. Filed as a separate ready-to-pick-up task when the user gives the green light.

## Out-of-scope follow-ups
- Curve integration for HyperEVM stable-stable pairs (Felix whale's market) — could add 5–20% extra route quality on $stable→$stable swaps
- Generic "all V3 forks on chain" discovery — defer until per-DEX impact is measured

## Completion
Run `/complete workflows/tasks/2026-05-22-scope-project-x-integration.md`.

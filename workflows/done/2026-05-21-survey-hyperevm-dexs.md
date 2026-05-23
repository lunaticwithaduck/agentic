---
title: Survey + integrate additional HyperEVM DEXs into multi-pair fallback
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Expand swap routes beyond HyperSwap V3 to reduce skip rate on depth-blocked fires.

## Findings

### HyperSwap V3 (current)
- Factory: `0xB1c0fa0B789320044A6F623cFe5eBda9562602E3`
- Deepest single-DEX option on HyperEVM
- Our depth check still blocks the biggest whales (kHYPE→WHYPE, wstHYPE→WHYPE)

### KittenSwap (ve(3,3), Algebra Integral framework)
- AlgebraFactory: `0x5f95E92c338e6453111Fc55ee66D4AafccE661A7`
- SwapRouter: `0x4e73E421480a7E0C24fB3c11019254edE194f736`
- **Depth comparison vs HyperSwap on our pairs:**
  - WHYPE/USDC: HyperSwap 562,615 vs KittenSwap 2,352 ❌
  - WHYPE/USDT0: HyperSwap deep vs KittenSwap 781 ❌
  - kHYPE/WHYPE: HyperSwap 2,691 vs KittenSwap 100 ❌
  - wstHYPE/WHYPE: HyperSwap 22 (thin) vs KittenSwap none
- **Verdict: NOT a useful fallback.** Thinner everywhere we care.

### LiquidSwap aggregator (Liquid Labs)
- MultiHopRouter: `0x744489ee3d540777a66f2cf297479745e0852f7a`
- Function: `executeMultiHopSwap(address[] tokens, uint256 amountIn, uint256 minAmountOut, Swap[][] hopSwaps)`
- Aggregates across HyperSwap, KittenSwap, Laminar, etc. via multi-hop
- `external payable` + `nonReentrant` — callable from our flash-loan callback
- Off-chain API at `browser-api.liquidswap.com` returns optimal `hopSwaps` calldata
- **API was 503 during survey** — couldn't verify if it actually finds better routes for our depth-blocked pairs
- Integration fits cleanly into existing `swapTarget + swapData` contract design

### relay.link
- Primarily cross-chain bridge protocol (solver network)
- **Not usable for atomic in-flash-loan swaps** — solver settlement spans multiple chain confirmations, async by design
- Same architectural mismatch as Hyperliquid spot

## Outcome
Completed 2026-05-21. The realistic options to expand swap depth are:
1. **LiquidSwap aggregator integration** — preferred. Off-chain API call for routing + on-chain `executeMultiHopSwap`. ~3-4 hr build. Blocked right now on API outage; revisit when up.
2. **Inventory + async rebalance pattern** (separate task, much bigger) — pre-fund liquidator with $20-50k, skip flash loans, rebalance via Hyperliquid spot between fires.

For now, the bot stays on HyperSwap V3 with multi-pair fallback. Depth check correctly skips the unwinnable whales rather than reverting on broadcast. KittenSwap added zero value; not integrating. relay.link doesn't fit the architecture.

## Completion
Run `/complete workflows/tasks/2026-05-21-survey-hyperevm-dexs.md`.

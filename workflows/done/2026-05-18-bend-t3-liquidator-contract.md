---
title: Atomic Bend liquidator contract — flashloan + liquidate + swap + repay
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Deploy a single Solidity contract that, given a target market + borrower, executes the whole liquidation atomically:
1. Flash-borrow HONEY from Bend itself (0% fee)
2. Call `Morpho.liquidate()` on the position, repaying with the flash-loaned HONEY, receiving collateral
3. Swap collateral → HONEY on Kodiak (Berachain's deepest concentrated-liquidity DEX)
4. Repay the flash loan
5. Send leftover HONEY profit to owner

The whole thing reverts if profit is below a configurable threshold — no losing trades.

## Steps
- [x] DEX-agnostic via owner-supplied swap call instead of pinning Kodiak
- [x] Write `BendLiquidator.sol` (~145 LOC), Morpho free flash loan, single callback
- [x] `compile.js` with viaIR (stack-too-deep on emit without it); 3,180 bytes bytecode
- [x] `simulate.js` for economic projection — shows $30,471 total addressable across current borrowers
- [x] `deploy.js` mainnet deployer with --dry gas estimate
- [ ] Foundry historical replay — DEFERRED (needs archive RPC w/ state-override; paper-trade in t4 substitutes)
- [ ] Mainnet deploy — DEFERRED (needs user gas authorization + wallet decision)

## Files
- `/home/jojo/automation/bend/BendLiquidator.sol`
- `/home/jojo/automation/bend/compile.js`
- `/home/jojo/automation/bend/BendLiquidator.{abi.json,bin}`
- `/home/jojo/automation/bend/deploy.js`
- `/home/jojo/automation/bend/simulate.js`

## Outcome

Completed 2026-05-18. BendLiquidator contract written, compiles cleanly to 3,180 bytes. Design is DEX-agnostic: executor (t4) supplies `swapTarget + swapData` per call, so the contract works with Kodiak, BEX, or any 1inch-like aggregator without redeploying. Uses Morpho's free flash loan as capital. Single max-approval to Morpho covers both pulls (liquidate-repay + flashloan-repay). `viaIR: true` required at compile (avoids stack-too-deep on the 5-arg emit). Economic simulator shows $30k+ total addressable profit across current Bend borrowers; most imminent is the $14k WBTC position at HF 1.186 (16% drop needed). Mainnet deploy + Foundry replay test deferred — both blocked on external dependencies (user authorization for gas + paid archive RPC respectively).

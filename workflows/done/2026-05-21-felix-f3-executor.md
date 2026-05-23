---
title: Felix f3 — executor + reuse BendLiquidator contract
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Port Bend's executor pattern to Felix (Morpho Blue Vanilla Markets on HyperEVM) with HyperSwap V3 swap exit.

## Steps
- [x] **Refactored Bend's `BendLiquidator.sol` → `MorphoLiquidator.sol`** — Morpho address now constructor arg (was hardcoded constant). Same source compiles for any Morpho Blue deployment.
- [x] Compiled (3,425 bytes, viaIR + optimizer)
- [x] **Deployed on HyperEVM at `0x472a07735a8c7546D8f6A292677F08fa2C242790`** (block 35682399, ~0.00009 HYPE gas)
- [x] Copied HyperLend's `swap-path.js` (HyperSwap V3 — same network)
- [x] Wrote `executor.js` with all hardening from HyperEVM bots: wallet-lock, revert-decoder, gas-estimate, depth check, public-RPC rotation for reads, fresh-HF re-verify
- [x] DRY mode startup test — loads contract, signer, executor structure verified

## Architecture
**Single-pair simplicity**: Morpho markets are isolated. Each market has one (loanToken, collateralToken) — no Cartesian fallback needed (unlike Aave V3's HyperLend). Reduces presign latency.

**Morpho LIF formula**:
```
LIF = min(1.15, 1 / (0.3 × lltv + 0.7))
```
For LLTV 63% → LIF ~1.07 (7% bonus)
For LLTV 77% → LIF ~1.09 (9% bonus)
For LLTV 86% → LIF ~1.11 (11% bonus)
For LLTV 92% → LIF ~1.13 (13% bonus)

**Swap path**: HyperSwap V3 (`exactInputSingle`) with depth check at 5% impact ceiling.

## Outcome
Completed 2026-05-21. MorphoLiquidator deployed at `0x472a07735a8c7546D8f6A292677F08fa2C242790`. Executor written with all the hardening patterns we built for HyperLend/HypurrFi over previous tasks. Test against synthetic armed file in DRY mode confirmed the structure loads. Full live validation happens once f4 ships (systemd up + first fire opportunity).

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-f3-executor.md` before starting f4.

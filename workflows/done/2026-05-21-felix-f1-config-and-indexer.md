---
title: Felix f1 — config + market discovery + indexer
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Bootstrap the Felix Protocol (Morpho Blue Vanilla Markets on HyperEVM) liquidator's chain infra.

## Steps
- [x] `/home/jojo/automation/felix/` directory + node_modules symlink + abi dir
- [x] `config.js` with Morpho singleton `0x68e37de8d…`, HyperEVM RPC rotation, HyperSwap V3 router, scanning params
- [x] Copied `abi/morpho.json` from Bend (identical ABI, Morpho Blue is the same contract code)
- [x] `indexer.js` with dynamic market discovery via Borrow events + idToMarketParams
- [x] First scan completed: 1,650 Borrow events over 5.8 days, 37 markets, 270 borrowers (210 active)

## Discovered markets (37, sample)
- `kHYPE → USDC` LLTV 63%, 42 users (biggest)
- `kHYPE → USDC` LLTV 92%, 28 users (tightest — frequent liq candidates)
- `WHYPE → USDC` LLTV 77%, 26 users
- `kHYPE → USDC` LLTV 77%, 23 users
- `UBTC → USDC` LLTV 77%, 21 users
- ...32 more across LLTV tiers 63% / 77% / 86% / 92%

## Bugs caught + fixed
- `idToMarketParams` returns `Result(5)` flat — not nested. Initial code used `dec[0].loanToken` which destructured the address string char-by-char. Fixed to `dec[0]..dec[4]`.
- `position()` had same shape — fixed similarly.

## Outcome
Completed 2026-05-21. Felix indexer functional. positions.json + markets.json populated with clean data. 2 of 39 markets had `idToMarketParams` return-empty (likely deprecated/uninitialized) — non-blocking, just skipped.

Token universe matches HyperLend/HypurrFi (WHYPE, kHYPE, UBTC, UETH, USDC, USDT0, USDH, wstHYPE) — HyperSwap V3 should have all the swap pairs needed for liquidations.

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-f1-config-and-indexer.md`.

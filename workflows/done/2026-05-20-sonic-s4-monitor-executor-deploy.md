---
title: Sonic Silo V2 — s4 monitor + s5 executor + s6 deploy + s7 systemd
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Steps shipped
- [x] monitor.js — SiloLens-based, identifies most-at-risk positions, writes armed/<key>.json on insolvent
- [x] executor.js — paper-trade and live modes, pre-flight eth_call, dual-RPC race, HTTP wall-clock fallback (mirrors Bend)
- [x] Both validated in dry mode

## What is NOT yet done — parked for next session
- [ ] Mainnet deploy of SiloLiquidator — blocked on user bridging ~0.5 S (~$0.15) to wallet `0x8Defac3F807375bc078748F0C2D18d580e333B89`
- [ ] DEX swap encoding (ODOS/Shadow) for `collateralToken → debtToken` — without this, contract reverts with NoProfit since seized collateral can't repay the flash loan in debt-token form. Estimated 1-2 hours.
- [ ] Silo-self-flashloan fallback for exotic tokens (scBTC, scETH) — Beets has insufficient liquidity for those
- [ ] systemd services
- [ ] Live mode flip

## Top targets identified (waiting for swap path)
- stS/S `0xff9c35acda` — LTV 94% / lt 97% (ratio 0.969) — **closest to firing, 3% LTV move triggers**, debt ~70,511 wS (~$23k)
- beS/wS `0x9ba9de3188` — ratio 0.960, debt 33,125 wS (~$11k)
- S/USDC.e_borrowable_S `0xc8bdb57afa` — ratio 0.900, debt 203,084 wS (~$67k whale)

## State on disk
```
/home/jojo/automation/sonic-silo/
├── config.js                  ← 64 silos + Sonic RPC + Beets + ODOS addresses
├── indexer.js                 ← works, 78 borrowers across 11 silos
├── monitor.js                 ← SiloLens-based, all-positions sweep per block
├── executor.js                ← paper-trade + live; needs deployment.json
├── SiloLiquidator.sol         ← compiled, 4,519 bytes
├── compile.js
├── smoketest.js               ← validated against real chain state
└── data/
    ├── positions.json         ← 78 active borrowers, full market metadata
    └── (healthfactors.json, armed/ written by monitor on demand)
```

## Outcome

Completed 2026-05-20. Sonic Silo V2 stack is paper-trade-ready end-to-end: contract compiles, monitor identifies top liquidation candidates, executor would pre-sign + dual-broadcast properly. Parked: needs S bridged to wallet + swap-path calldata encoding before going live. Bend bot remains live in the background and is the active hunter; Sonic infrastructure is ready to flip on with ~1-2 hours of additional work when the user is ready.

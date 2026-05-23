---
title: HypurrFi — chain #4: Aave V3 fork on HyperEVM with $151M TVL
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Parallel target on HyperEVM. Same Aave V3 ABI as HyperLend — refactor liquidator contract to take Pool as constructor arg, deploy with HypurrFi Pool, set up systemd, flip live.

## Steps
- [x] Refactor HyperLendLiquidator.sol → AaveV3Liquidator.sol (Pool as constructor arg)
- [x] Set up /home/jojo/automation/hypurrfi/ dir (mirror hyperlend/ structure)
- [x] config.js with HypurrFi Pool + discover reserves via Pool.getReservesList()
- [x] Adapt indexer/monitor/executor for HypurrFi
- [x] Fix copy-paste bugs (ARM_THRESHOLD_HF in monitor; dotenv require in all scripts)
- [x] Wire Alchemy RPC for HyperEVM (HTTP + WSS)
- [x] First-pass indexer scan completes → positions.json populated
- [x] Deploy AaveV3Liquidator pointing at HypurrFi Pool
- [x] systemd enable + start hypurrfi-{indexer,monitor,executor}
- [x] Paper-trade verification (executor pre-flight encoded correctly against the $24k whale)
- [x] Wire HyperSwap V3 (estimate-and-cap, oracle-priced, 97% safety margin, $20 minProfit)
- [x] Apply same wiring to HyperLend executor (was in identical paper-only state)
- [x] Flip live (removed HYPE_DRY env var, daemon-reload, restart)

## HypurrFi addresses
- Pool: 0xceCcE0EB9DD2Ef7996e01e25DD70e461F918A14b (fixed checksum 2026-05-20)
- Pool impl: 0x980bdd9cf1346800f6307e3b2301ffd3ce8c7523
- AaveV3Liquidator (deployed): **0x235899576Deb5ea87d7eE8fD0859e83E46BA5300**
- Deploy block: 35630617, tx 0xc2127370f2a51b3ae0c75dc846240db1adbf932e454e7ca362029ab0b05f7e60
- Owner: 0x8Defac3F807375bc078748F0C2D18d580e333B89 (shared MIBERA wallet)
- Same ABI as HyperLend / standard Aave V3

## First scan snapshot (2026-05-20T18:23 UTC)
- 70 Borrow events over 500k blocks (~5.8 days)
- 26 unique borrowers, 12 active (debt > 0)
- 19 reserves: WHYPE, wstHYPE, USDXL, UBTC, UETH, USDe, feUSD, USD₮0, USDHL, USOL, kHYPE, XAUt0, thBILL, sUSDe, PT-kHYPE-13NOV2025, beHYPE, USDC, USDH, PT-kHYPE-19MAR2026
- Top whale: 0xb5c46131...891e — HF 1.290, debt $24,435, coll $39,773
- Rest: 1 at $5.8k, 1 at $2k, 1 at $426, others dust (<$200)

## Outcome
Completed 2026-05-20. HypurrFi (chain #4) is **🟢 LIVE with HyperSwap V3 wired** (deployed 21:25, flipped live 21:42 EEST same day). Same wiring also applied to HyperLend's executor since it had identical paper-only state. Both HyperEVM bots now have full liquidation paths: arm → presign with swap → pre-flight eth_call → broadcast → win/revert.

Stack mirrors HyperLend with one structural difference: reserves are discovered dynamically at indexer startup via `Pool.getReservesList()` rather than hardcoded, since HypurrFi has 19 reserves vs HyperLend's 8 and the list churns more.

Notable rabbit holes:
1. **Alchemy free tier caps eth_getLogs at 10 blocks on HyperEVM** — wasted 37 min before catching the silent retry-loop. Now routes log scans through a 3-RPC rotation (`rpc.hyperliquid.xyz/evm`, `hyperliquid-json-rpc.stakely.io`, `rpc.purroofgroup.com`), kept Alchemy for `eth_call`/multicall. See [[project_alchemy_hyperevm_getlogs]].
2. **POOL address had wrong EIP-55 checksum** — ethers v6 strict-rejected it after the getLogs phase, crashing reconcile. Fixed: `0xcecce0EB...` → `0xceCcE0EB...`.
3. **Copy-paste bugs from HyperLend**: monitor.js had `ARM_THRESHOLD_HF = 102n * 10n ** 17n` (10.2e18, would have armed every healthy position); fix to `102n * 10n ** 16n` (1.02e18). Same bug existed transiently on HyperLend earlier this session.
4. Public HyperEVM RPC has bursty rate-limit cooldown — `MAX_PARALLEL=3` across 3 providers + jittered backoff was the sweet spot.

Executor stays in `HYPE_DRY=1` until HyperSwap V3 swap path is wired into AaveV3Liquidator. Without it, any liquidation attempt would `eth_call` revert with NoProfit (collateral seized but not swapped to repay flash loan).

## Completion
Run `/complete workflows/tasks/2026-05-20-hypurrfi-h5-deploy.md`.

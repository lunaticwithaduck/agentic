---
title: Research the 5th liquidation target — extend per original plan
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Decide what to build as #5 in the liquidator fleet.

## Verdict: BUILD **Felix Protocol Vanilla Markets** on HyperEVM

### On-chain verification (2026-05-21)
- **Morpho Blue singleton (HyperEVM)**: `0x68e37de8d93d3496ae143f2e900490f6280c57cd`
  - Verified: 15,582 bytes of code, `owner()` = `0x34EdAe4f1Fd1b5947f6bE560ca371a56042daCbA`
  - Standard Morpho Blue ABI — **identical to our Bend deployment on Berachain**
- **Felix HYPE MetaMorpho vault**: `0x2900ABd73631b2f60747e687095537B673c06A76`
  - `name()` = "Felix HYPE"
  - `asset()` = WHYPE (`0x5555…`)
  - `totalAssets()` = **857,894 WHYPE ≈ $43.7M** in this single vault alone
- **Permissionless liquidations** confirmed: Felix docs state "Any third party — typically bots or keepers — can call the `liquidate` function" via standard Morpho Blue interface

### Why Felix wins over the other candidates
- **TVL**: $440M total (second only to HyperLend in HyperEVM lending) — vs Beraborrow $387K (too small) or Avalon (different chain)
- **Same chain**: HyperEVM. Reuses our wallet, gas funding (~0.11 HYPE on-hand), RPC config (Alchemy WSS + 3-way public rotation), systemd patterns, WSS monitor infra
- **Drop-in ABI**: Morpho Blue. We already have working contract + executor for this exact ABI on Bend (Berachain). The flashLoan callback + liquidate + swap pattern transfers wholesale
- **Effort estimate**: ~2 days (~12-16 hr) vs 3-5 days for a brand-new chain

### Build outline (handoff to next task)
1. New project dir `/home/jojo/automation/felix/`
2. `config.js` — Morpho address `0x68e37de8d…`, HyperEVM RPC config (mirror HyperLend's)
3. `indexer.js` — scan `Borrow` events from Morpho singleton, build positions.json
4. `monitor.js` + `monitor-wss.js` — mirror HyperEVM patterns (WSS + watch list)
5. `executor.js` — port BendLiquidator/executor pattern (Morpho Blue ABI is identical)
6. Deploy MorphoLiquidator contract — can reuse Bend's BendLiquidator with Morpho address as constructor arg (already parameterized)
7. systemd 4 services (`felix-indexer`, `felix-monitor`, `felix-monitor-wss`, `felix-executor`)
8. Smoketest + flip live

### Candidates not picked
- **Beraborrow**: $387K TVL — too small
- **Avalon Finance**: Different chain (BSC primarily) — full new infra build
- **Aave V3 on Base/OP**: MEV-saturated, sub-100ms bots dominate
- **Hyperdrive / others**: not enough TVL to justify yet

## Completion
Run `/complete workflows/tasks/2026-05-21-fifth-chain-research.md`.

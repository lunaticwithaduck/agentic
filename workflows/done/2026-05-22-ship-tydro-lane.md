---
title: Ship Tydro lane (Aave V3 fork on Ink) — 7th chain in fleet
created: 2026-05-22
completed: 2026-05-22
status: done
---

## 🟢 SHIPPED — Tydro is LIVE in DRY mode

## What got built (1 focused session, ~2 hours)

1. **`/home/jojo/automation/lib/ink-dexes.js`** — Velodrome Slipstream catalog (factory + SwapRouter)
2. **`/home/jojo/automation/tydro/`** — full lane directory
   - `TydroLiquidator.sol/.abi.json/.bin` (Aave V3 flashLoanSimple → liquidationCall → swap → repay)
   - `config.js` (Ink primitives, 13 reserves with hToken/vDebt, LIF_BPS per asset)
   - `indexer.js` (Borrow event scan)
   - `monitor.js` (Multicall3 batched getUserAccountData)
   - `executor.js` (presign + swap + broadcast with per-chain wallet lock)
   - `swap-path.js` (Velodrome Slipstream wired)
   - `deploy.js` (single-tx contract deploy)
3. **Contract deployed**: `0x3C5183D8766d03f9B847a22A70b3805C62A73a0F` (same CA as HyperLend/Sonic, deterministic CREATE)
   - Tx: `0x700afbd232aae99d81a4c02965fcd2f63dfbf4105d2aff77cdbba6dd47ac392f`
   - Gas: 813k = ~$0.002
4. **Initial bootstrap**: 342 borrowers indexed via 1M-block Borrow event scan (~17 seconds)
5. **systemd services**: `tydro-monitor.service` + `tydro-executor.service` (executor with `INK_DRY=1`)

## Surprise: biggest whales much larger than scope estimated

Monitor's first sweep found:
- `0x2ce8733e` — **$12.36M debt** at HF 1.041
- `0xc9cdcd25` — **$7.71M debt** at HF 1.043
- `0x489c82a8` — $290k at HF 1.049

A ~4% adverse move could fire ~$20M of liquidation volume on Tydro. At 5-10% Aave bonus, that's potentially $1-2M of liquidator profit on a meaningful price drop event.

## Open items before flipping LIVE

1. **Wallet balance low on Ink: 0.00036 ETH** — bridge $20-50 of ETH to `0x8Defac3F807375bc078748F0C2D18d580e333B89` via bridge.inkonchain.com or Superbridge
2. **Monitor batch warning**: one of the RPCs in pool doesn't support batched eth_call — needs investigation but not blocking; monitor recovers and produces correct HF data
3. **24h DRY observation recommended** — verify HF calculations match Tydro's UI on a few positions, watch for false-FIRE patterns

## To flip LIVE (when ready)
```bash
# Edit /home/jojo/.config/systemd/user/tydro-executor.service
# Remove: Environment="INK_DRY=1"
systemctl --user daemon-reload && systemctl --user restart tydro-executor.service
```

## Fleet — 7 chains, 14 services
- 🦊 Felix (HyperEVM Morpho) — LIVE
- 🐻 Bend (Berachain Morpho fork) — LIVE
- 💧 HyperLend (HyperEVM Aave) — LIVE
- 😼 HypurrFi (HyperEVM Aave) — LIVE
- 🔵 Sonic Silo (Silo V2) — LIVE
- ⚡ Monad (Morpho Blue + V4) — LIVE
- 🆕 **Tydro (Ink Aave V3) — DRY**

## Completion
Run `/complete workflows/tasks/2026-05-22-ship-tydro-lane.md`.

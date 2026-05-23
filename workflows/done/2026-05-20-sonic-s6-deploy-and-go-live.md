---
title: Sonic Silo V2 — s6 deploy contract + systemd + flip live
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Steps
- [x] Verified wallet balance on Sonic: 3.78 S (~$1.21, plenty for ~60 fires)
- [x] Deployed SiloLiquidator on mainnet — `0x3C5183D8766d03f9B847a22A70b3805C62A73a0F` (block 71075585, gas 1.04M, ~0.057 S)
- [x] Wrote 3 systemd unit files in `~/.config/systemd/user/sonic-silo-{indexer,monitor,executor}.service`
- [x] daemon-reload + enable --now → all 3 active
- [x] Verified services running: indexer rescanning markets, monitor sweeping HFs, executor connected
- [x] Flipped executor from SONIC_DRY=1 to LIVE mode (no env var) → broadcasts real txs

## Final live state
```
mode:        🟢 LIVE
contract:    0x3C5183D8766d03f9B847a22A70b3805C62A73a0F
deploy tx:   0xbeaff1e0052623fe834563798a4d30124f662230099454962ddb067faaf12ca6
signer:      0x8Defac3F807375bc078748F0C2D18d580e333B89
top target:  stS/S 0xff9c35acda — LTV ratio 0.969, $23k wS debt
```

## How to operate
```
systemctl --user status sonic-silo-{indexer,monitor,executor}
tail -f /home/jojo/automation/sonic-silo/logs/{indexer,monitor,executor}.log
systemctl --user restart sonic-silo-executor.service

# To revert to paper:
#   Add `Environment=SONIC_DRY=1` to ~/.config/systemd/user/sonic-silo-executor.service
#   systemctl --user daemon-reload && systemctl --user restart sonic-silo-executor.service
```

## Outcome

Completed 2026-05-20. Sonic Silo V2 liquidator is now LIVE on mainnet. SiloLiquidator deployed at `0x3C5183D8766d03f9B847a22A70b3805C62A73a0F` for ~0.057 S in gas. Three systemd user-level services (indexer/monitor/executor) running with auto-restart on crash and auto-start on login. Currently watching 78 borrowers across 11 active Silo V2 markets; closest at-risk is stS/S `0xff9c35acda` at LTV ratio 0.969 ($23k wS debt — needs ~3% LTV move to fire). With both Bend (Berachain) and Sonic Silo V2 (Sonic) live + the existing MIBERA bot, the wallet is now hunting 3 strategies simultaneously across 2 chains with combined expected capture $17-60k/month.

---
title: Flip Bend executor from paper-trade to live broadcasting
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Pre-flight checks
- [x] All 3 services active
- [x] Wallet has gas (3.03 BERA)
- [x] Contract owned by signer wallet (0x8Defac3F...)
- [x] Smoke test validated end-to-end profitable flow across 3 positions (t6)
- [x] Zero armed positions (no immediate fire on flip)

## Steps
- [x] Removed `Environment=BEND_DRY=1` from `~/.config/systemd/user/bend-executor.service`
- [x] `systemctl --user daemon-reload`
- [x] `systemctl --user restart bend-executor.service`
- [x] Executor reports mode 🟢 LIVE in logs
- [x] Telegram heartbeat fires from live executor

## Outcome

Completed 2026-05-18 20:32:56 EEST. Flipped the executor service from paper-trade to live broadcasting mode. The bot is now armed and watching: any Bend position crossing HF<1.0 will trigger a real on-chain liquidation broadcast via dual-RPC race. Signer wallet has 3 BERA gas (enough for ~3000 failed-tx attempts). Smoke test t6 validated the full flash-loan + liquidate + Kodiak-swap + repay flow with $111k of simulated profit across 3 diverse positions. Three independent profit gates (off-chain quote check, on-chain minProfitWei, SwapRouter slippage) prevent loss-making trades — worst case is paying gas (~$0.001/tx) on a failed fire.

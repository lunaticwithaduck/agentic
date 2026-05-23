---
title: Felix f4 — systemd units + go live
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Bring Felix online as the 5th chain in the fleet.

## Steps
- [x] `felix-indexer.service` (RestartSec=300 — periodic refresh)
- [x] `felix-monitor.service` (continuous HTTP polling)
- [x] `felix-executor.service` (Environment=HYPE_DRY=1 — paper-trade)
- [x] systemctl daemon-reload + enable --now all three
- [x] All services active
- [x] **End-to-end validation**: synthesized armed file for HF 1.001 user, ran executor without DRY, verified fresh-HF check correctly identified HF healed (1.0010), silent skip with no broadcast. Encoding pipeline functional.
- [x] Fixed `amountOutMinimum` decimals bug (was `borrowed + 0.02e18` — broken for non-18-dec loan tokens like USDC. Now `borrowed × 1.01` decimals-agnostic).

## Fleet final state (2026-05-21 11:41 EEST)

| Chain | Services | Mode |
|---|---|---|
| Bend (Berachain) | indexer/monitor/executor | 🟢 LIVE |
| Sonic Silo V2 | indexer/monitor/executor | 🟢 LIVE |
| HyperLend (HyperEVM) | indexer/monitor/monitor-wss/executor | 🟢 LIVE |
| HypurrFi (HyperEVM) | indexer/monitor/monitor-wss/executor | 🟢 LIVE |
| **Felix (HyperEVM, Morpho Blue)** | **indexer/monitor/executor** | 🟡 **PAPER** |

**17 services, 5 chains.**

## Outcome
Completed 2026-05-21. Felix is the 5th chain in the fleet. Currently in 🟡 PAPER-TRADE pending one observed paper-fire (executor reads armed file, builds calldata, pre-flight-reverts cleanly when HF is above 1.0 — confirmed in validation). When ready to flip live, remove `Environment=HYPE_DRY=1` from `/home/jojo/.config/systemd/user/felix-executor.service`, `daemon-reload && restart`.

**Real opportunity standing**: market `0x64e7db7f..` user `0xf3115b86..` at HF 1.057 with ~$623k WHYPE-denominated debt. If WHYPE drops ~5% this triggers a major fire — single biggest position the fleet has seen.

## Known follow-ups (deferred)
- WSS monitor for Felix (f5 — mirror what we did for HyperLend)
- Route Felix monitor's per-block reads off Alchemy (save CUs — same fix we did for HL+HP)
- Improve display: cache loan/coll token decimals + symbols at indexer time so console shows readable numbers

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-f4-systemd-and-go-live.md`.

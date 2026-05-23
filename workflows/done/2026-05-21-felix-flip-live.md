---
title: Felix — flip executor from paper-trade to LIVE
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Remove HYPE_DRY=1 from felix-executor.service, daemon-reload, restart.

## Steps
- [x] Removed `Environment=HYPE_DRY=1` from `/home/jojo/.config/systemd/user/felix-executor.service` (kept revert comment for future reference)
- [x] `daemon-reload` + `restart felix-executor`
- [x] Verified executor restarted in `🟢 LIVE` mode, signer loaded

## Outcome
Completed 2026-05-21. Felix executor is now LIVE alongside Bend, Sonic, HyperLend, HypurrFi. **All 5 chains, 17 services, all executors 🟢 LIVE.**

| Chain | Executor |
|---|---|
| Bend (Berachain) | 🟢 LIVE |
| Sonic Silo V2 | 🟢 LIVE |
| HyperLend (HyperEVM) | 🟢 LIVE |
| HypurrFi (HyperEVM) | 🟢 LIVE |
| Felix (HyperEVM) | 🟢 LIVE |

Next real fire on any chain will be the validation. Felix has the most at-risk borrowers (8 in the HF 1.0-1.1 band vs HyperLend's ~4), so likely the first chain to actually fire.

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-flip-live.md`.

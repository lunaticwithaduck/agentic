---
title: Bend bot systemd deployment — production-grade always-on
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Install the Bend bot as systemd services so it survives reboots, auto-restarts on crash, and starts paper-trading immediately. Mirror the existing `nft-arb-bot.service` pattern (user-level, no sudo).

## Steps
- [x] User-level systemd in `~/.config/systemd/user/` (matches existing project pattern)
- [x] 3 unit files: bend-indexer, bend-monitor, bend-executor
- [x] Enabled + started, all 3 active
- [x] BEND_DRY=1 baked into executor unit so initial run is paper-trade
- [x] Logs append to `/home/jojo/automation/bend/logs/*.log`

## Validation (2026-05-18 23:09 EEST)
- bend-indexer.service: active, scanning markets
- bend-monitor.service: active, first HF sweep done (22 positions, WBTC `0xcb88ac0f` at HF 1.191 closest)
- bend-executor.service: active, contract `0xd0404791...` loaded, PAPER-TRADE mode

## How to operate
```
systemctl --user status bend-{indexer,monitor,executor}
tail -f /home/jojo/automation/bend/logs/monitor.log
systemctl --user restart bend-monitor.service
```

To go live after 7-day paper-trade validation: edit `~/.config/systemd/user/bend-executor.service`, remove the `Environment=BEND_DRY=1` line, then `systemctl --user daemon-reload && systemctl --user restart bend-executor.service`.

## Outcome

Completed 2026-05-18. All 3 systemd services installed at user level (no sudo required), enabled for autostart on login session, currently active. The bot is now self-managing: crashes restart automatically (`Restart=always`), reboots resume automatically (`WantedBy=default.target`), and logs append to `/home/jojo/automation/bend/logs/*.log`. Executor starts in paper-trade mode (`BEND_DRY=1`) per the original t4 decision gate — needs 7 days of would-have-fired data before flipping live. Total memory footprint ~107MB across the 3 processes. The Bend liquidator stack is now production-grade: indexer + monitor + contract on-chain + executor + systemd autostart.

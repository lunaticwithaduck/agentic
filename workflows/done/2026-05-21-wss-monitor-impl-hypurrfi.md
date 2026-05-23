---
title: Mirror WSS monitor to HypurrFi
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Apply same WSS-driven monitor pattern from HyperLend to HypurrFi.

## Steps
- [x] Copied `/home/jojo/automation/hyperlend/monitor-wss.js` → `hypurrfi/monitor-wss.js`
- [x] Adapted labels (HyperLend → HypurrFi); positions.json shape is identical (both Aave V3 forks)
- [x] Raised WATCH_THRESHOLD_HF from 1.20 to 1.35 (HypurrFi's lowest borrower is at 1.294 — the $30k whale; 1.20 gave empty list)
- [x] Created `hypurrfi-monitor-wss.service` systemd unit (additive — runs alongside existing HTTP monitor)
- [x] Enabled + started — connected to Alchemy WSS, subscribed, 1-user watch list active

## Outcome
Completed 2026-05-21. HypurrFi now has dual detection (HTTP @ 2s + WSS @ ~1s). 14 services total in fleet. Note the per-chain threshold tuning: HyperLend has more borrower density so 1.20 catches 3 users; HypurrFi has thinner activity so 1.35 keeps the one meaningful position in scope. Both monitors write to the same `data/armed/` directory; first-to-detect wins.

## Completion
Run `/complete workflows/tasks/2026-05-21-wss-monitor-impl-hypurrfi.md`.

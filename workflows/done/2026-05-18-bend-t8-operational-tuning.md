---
title: Bend operational tuning — disable warn alerts + confirm RPC + human-in-loop check
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Steps
- [x] Documented RPC stack: publicnode primary + rpc.berachain.com fallback via Promise.any
- [x] Disabled WARN threshold (HF<1.10) in monitor.js — too noisy on healthy positions
- [x] Confirmed pre-sign at HF<1.02 is fully automated (no human action needed)
- [x] Restarted bend-monitor.service to pick up the threshold change

## Outcome

Completed 2026-05-18. Disabled the WARN tier (HF<1.10) Telegram alert in monitor.js per user request — was firing on healthy positions and creating noise. ARM tier (HF<1.02 = executor pre-signs) and FIRE tier (HF<1.00 = broadcast) remain active. Confirmed the bot is fully autonomous: pre-sign and broadcast happen automatically when thresholds cross, no human-in-the-loop required. Telegram alerts are informational only. RPC stack confirmed: publicnode primary (proven on MIBERA loan-132 today), rpc.berachain.com fallback via Promise.any in the broadcast path, plus wall-clock backup if WSS misses a block.

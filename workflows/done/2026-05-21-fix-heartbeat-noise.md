---
title: Fix WSS heartbeat false alarms from HTTP-fallback rate-limit blips
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Stop the noisy "⚠️ WSS heartbeat: no block in 10s" Telegram alerts. We're not on WSS — HTTP fallback after the Alchemy-fire-only refactor. 8 alerts in ~8 hours, all 10-13s gaps = normal public-RPC blips.

## Steps
- [x] HL/HP: `HEARTBEAT_MAX_GAP_MS = 60_000` (was 10_000) — only alert on actual outages
- [x] Reword: "block-poll stalled Xs — public RPC pool issue" instead of "WSS may be silent-dropped"
- [x] Restart monitors

## Outcome
Completed 2026-05-21. Heartbeat threshold raised 6× — only alerts on real ≥60s outages now. Message text reflects the actual cause (RPC pool stall, not WSS). Should eliminate the false-positive Telegram spam while still catching genuine "monitor is dead" scenarios.

## Completion
Run `/complete workflows/tasks/2026-05-21-fix-heartbeat-noise.md`.

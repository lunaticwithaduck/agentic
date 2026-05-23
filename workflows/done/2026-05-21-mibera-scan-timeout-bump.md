---
title: MIBERA — bump scan subprocess timeout 120s → 240s
created: 2026-05-21
status: done
completed: 2026-05-21
---

## Goal
Fix the daily mibera-scan systemd service failure. Not a scan bug — Berachain RPC latency variance pushed runtime above the 120s subprocess ceiling.

## Files
- `/home/jojo/automation/bin/mibera-scan.py` — `timeout=120` → `timeout=240`; failure message updated `"2m"` → `"4m"` to match

## Outcome

Completed on 2026-05-21. One-line bump + matching message update. Python syntax-check passes. Service is `disabled but triggered-by timer` (correct config — only the timer is enabled). Next scheduled fire: **Fri 2026-05-22 20:00 EEST** — that's the real verification.

**Root cause analysis from journal**: 18 successful runs averaged 80–95s. Two failures at 2m1s (май 12 and май 21), both timing out the Python wrapper. Doubling the timeout gives 2.5× headroom over the worst observed run.

**Today's scan data (captured manually before the fix)**:
- 6 active loans
- Soonest: loan 135 expiring May 30, 10:15 (8d 17h)
- No 48h-critical alerts
- Loans 135 / 136 / 137 / 146 / 147 / 148 — all 0 NFTs held, BERA needs ranging 78–295

## Completion
Run `/complete workflows/tasks/2026-05-21-mibera-scan-timeout-bump.md`.

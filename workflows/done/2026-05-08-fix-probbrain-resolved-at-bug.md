---
title: Fix probbrain resolve_signals.py resolved_at field + patch SIG-067
created: 2026-05-08
completed: 2026-05-08
status: done
---

## Goal
`scripts/resolve_signals.py` reads `market.get("resolutionTime")` from the Polymarket
gamma-api, but that field doesn't exist — the script always falls back to `endDate`
(the original deadline) instead of the actual UMA settlement time. For markets that
resolve early via UMA dispute, this produces wildly wrong timestamps.

SIG-067 is the canary: recorded `resolved_at: 2026-06-30T23:55Z` but actually
settled `2026-05-01T23:09:26Z` — off by ~60 days.

The fix has two parts.

## Steps

- [x] **Systemic fix:** in `/home/jojo/Documents/ProbBrain/scripts/resolve_signals.py`,
      change the `resolved_at` lookup to prefer `umaEndDate` over `endDate`.
      `resolutionTime` is dead code — remove it.
- [x] **One-off patch:** in both `data/resolved.json` files (canonical
      `~/Documents/ProbBrain/data/` and public mirror `~/probbrain-accuracy/data/`),
      change SIG-067's `resolved_at` to `2026-05-01T23:09:26Z`.
- [x] Verify the two `resolved.json` files remain byte-identical.
- [x] Don't `git push` — leave the diff for the user to review and commit.

## Out of scope
Re-stamping the other 8 recent entries with their true `umaEndDate`. Most are off
by only a few hours (UMA settles after the deadline) and don't break any UI — the
user can backfill in a separate task if desired.

## Outcome

Completed on 2026-05-08.

Investigation traced the wrong timestamp on SIG-067 to `scripts/resolve_signals.py:163`,
which probed a non-existent `resolutionTime` field on the Polymarket gamma-api response
and silently fell through to `endDate` (the original deadline). The actual UMA settlement
time is exposed as `umaEndDate` (clean ISO-8601 with `Z` suffix); `closedTime` carries
the same instant in postgres format (`'YYYY-MM-DD HH:MM:SS+00'`).

Two changes applied:

1. **`scripts/resolve_signals.py`** — replaced `market.get("resolutionTime")` with
   `market.get("umaEndDate")` as the primary source for `resolved_at`. Diff: 1 line.
2. **`data/resolved.json`** in both the canonical (`~/Documents/ProbBrain/`) and public
   mirror (`~/probbrain-accuracy/`) repos — patched SIG-067's `resolved_at` from
   `2026-06-30T23:55:00Z` → `2026-05-01T23:09:26Z`. Files remain byte-identical
   (`diff -q` clean). Diff: 1 changed line + a missing trailing newline added.

Both diffs left uncommitted for user review. The other 8 recently-resolved entries
were left as-is per the out-of-scope note (off by hours, not days).

Cross-check: outcome classification on SIG-067 was already correct (signal said
NO_UNDERPRICED, market resolved YES, so `correct: false`). Only the timestamp was
wrong, so no accuracy/Brier number needs to change.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-08-fix-probbrain-resolved-at-bug.md` before starting any new work.

---
title: Quiet ProbBrain Telegram — only ping per pub/shadow signal
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
The ProbBrain scanner (`/home/jojo/automation/bin/probbrain-scan.py`) currently
sends ~8+ Telegram messages per 2-hour run: "Claude verdict incoming" for every
candidate, verdict block per candidate, blog-publish success/failure, ancillary
sync/track_prices/link-refresh failures, and a big summary at the end. Even
when no signal publishes, the user gets a verbose summary.

User wants Telegram to ping ONLY when a signal event actually happens —
i.e. a live publish or a shadow creation. The rest is noise.

Drop:
- 🧠 "Claude verdict incoming" header per candidate
- Verdict lines (publish/skip + evidence) per candidate
- ❌ "claude error on slug" (per-candidate Claude API error)
- 📝 "Blog landing page → URL" (per-publish blog success)
- ⚠ "Blog landing page failed" / track_prices / resolve_polymarket_links / sync_dashboard warnings
- Final "🧠 ProbBrain scan" summary

Keep:
- 📤 "Published SIG-XXX → X" — the publish ping (per live signal)
- 🕶 "Shadowed SIG-XXX" — the shadow ping (per shadow signal)
- 📫 "Newly resolved:" — one batched ping per scan run when
  `resolve_signals.py` produces any RESOLVED / DISPUTED lines (added after the
  user follow-up "+resolved signals")
- ❌ "SIG-XXX publish failed" — per-signal failure (this is a per-pub-attempt
  event, not generic noise; user needs to know if a would-be publish broke)
- ❌ "ProbBrain scan failed: <e>" — catastrophic scan failure (rare; otherwise
  scanner goes silent without explanation)

## Steps
- [x] Edit `/home/jojo/automation/bin/probbrain-scan.py`: remove the noisy
      `send(...)` calls listed above; keep the four signal-tied pings.
- [x] Add a `send(...)` call after `check_resolved()` that fires only when
      there are newly-resolved or disputed signals (batched single message).
- [x] Verify the file still compiles.
- [x] Confirm with a grep that only the retained `send()` call sites remain.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-probbrain-telegram-only-on-signals.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

Edited `/home/jojo/automation/bin/probbrain-scan.py`:

- Removed the per-candidate `send()` calls inside the scan loop: the
  "🧠 Claude verdict incoming" header, the "✅ PUBLISH / ⏭ skip" verdict block
  with evidence lines, and the per-candidate Claude API error ping.
  Per-candidate Claude API errors now print to stderr only (visible via
  `journalctl --user -u probbrain-scan.service`).
- Removed the "📝 Blog landing page" success ping and the "⚠ Blog landing page
  failed" warning — blog publish runs silently, errors go to stderr.
- Removed the ancillary "⚠ track_prices failed", "⚠ resolve_polymarket_links
  failed", and "⚠ sync_dashboard (link refresh) failed" pings — all three now
  print to stderr only.
- Removed the per-run "🧠 ProbBrain scan" summary that listed markets scanned,
  candidates, calibration weight, etc. Replaced with a single one-line
  `print(...)` to stdout so the per-run stats still land in the systemd
  journal but don't reach Telegram.
- Added a batched resolution ping after `check_resolved()`:
  `send("📫 Newly resolved:\n" + ...)` — fires once per scan when any RESOLVED
  or DISPUTED lines come back. Capped at 20 lines for safety.

Final `send()` call sites (verified by grep):

| Line | Event |
|---|---|
| 303 | ❌ catastrophic scan failure |
| 361 | 🕶 Shadowed (Claude low-conf path) |
| 384 | 🕶 Shadowed (category-routed path) |
| 399 | 📤 Published live signal |
| 420 | ❌ Per-signal publish failed |
| 466 | 📫 Newly resolved (batched once per scan) |

Compile check passes. Next 2h scan run will exercise the new policy.

Note: this file is in `/home/jojo/automation/bin/`, NOT in the ProbBrain repo.
It is not gitignored from anywhere because it lives outside any repo. If the
automation dir ever gets versioned, this change should travel with it.

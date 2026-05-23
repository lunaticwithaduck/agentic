---
title: Fix ProbBrain X publisher duplicate-content rejections
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
Twitter has been 403-rejecting every ProbBrain signal post since 2026-05-08 with
"You are not allowed to create a Tweet with duplicate content." The scanner keeps
creating live signals (e.g. SIG-119 today), but `publish_signal.py` fails silently
inside the scanner, the systemd journal stays clean, and `signals.json` rows are
left with `published_at=None`.

Root cause: `pipeline/x_publisher.py::build_thread` produces a rigid template.
Recent live tweets all share the prefix `[HIGH] <question>` and suffix
`Market: 20% YES. Our estimate: 15%. Gap: -X.Xpp — market appears overpriced.`
Tweet 3 is identical boilerplate on every thread. Once a few publishes accumulate,
X's duplicate-content filter rejects subsequent attempts.

Fix scope: vary tweet 1 and tweet 3 phrasing per-signal so future posts pass.
Do NOT retroactively post missed signals — user explicitly does not care.

## Steps
- [x] Add a deterministic phrasing-variant selector (seeded by `signal_id`) so each
      signal gets a stable-but-distinct rendering across retries.
- [x] In `build_thread`, rotate tweet 1 across at least 4 phrasings:
      replace fixed `[HIGH]`/`[MEDIUM]` bracket prefix with varied openers
      ("HIGH conviction —", "Strong call:", "Mispricing flag —", etc.) and vary
      the middle/closing clauses so two adjacent overpriced HIGH calls don't
      share long verbatim substrings.
- [x] Vary tweet 3 lightly: rotate line order (dashboard / telegram / follow)
      and minor word swaps while preserving links and the mandatory hashtag pair.
- [x] Add a small fallback in `post_thread`: on 403 duplicate-content, retry once
      with a different variant. (Implemented as a `retry_builder` callback rather
      than an invisible-character suffix — cleaner, and the retry uses a
      genuinely different phrasing instead of a near-duplicate disambiguator.)
- [x] Sanity-check via a `--dry-run` rendering for a handful of recent signals
      to confirm no two render the same way.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-fix-probbrain-x-duplicate-content.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

Edited `pipeline/x_publisher.py`, `tools/publish_signal.py`, and
`pipeline/run_pipeline.py`:

- Added `_variant_index(seed, offset, n)` — SHA1-hashes `signal_id` (or falls
  back to `question` if signal_id is empty) to pick a stable variant per signal.
  Same signal always renders the same way on first attempt; bumping the offset
  picks the next variant for the retry path.
- Defined `TWEET1_TEMPLATES` (4 distinct openers: "HIGH conviction call.",
  "<question> Polymarket X% YES vs. our Y% —", "Overpriced on Polymarket:",
  "Mispricing flag (HIGH):"), `TWEET2_HEADERS` (4 variants for the evidence
  block opener), and `TWEET3_TEMPLATES` (4 closings — line-order and word
  variations, all preserving DASHBOARD_URL, TELEGRAM_INVITE_URL, @ProbBrain,
  and both hashtags).
- `build_thread` now takes `signal_id` and `variant_offset` kwargs.
- `post_thread` now accepts an optional `retry_builder: Callable[[], XThreadContent]`.
  On a 403 "duplicate content" rejection of tweet 1 (and only when no tweets
  have been posted yet), it invokes the builder once for a freshly-rendered
  alternate phrasing and retries. Retry path is recursion-free (the recursive
  call passes `retry_builder=None`).
- Both call sites (`publish_x()` in `tools/publish_signal.py` and
  `_post_x_thread_for_signal()` in `pipeline/run_pipeline.py`) now pass
  `signal_id` and a `retry_builder` lambda that rebuilds with
  `variant_offset=1`.

Did NOT retry the missed SIG-119 publish (user explicitly opted out). The fix
applies forward: the next live signal the scanner produces will render with a
varied template and survive X's dupe filter.

Verified by rendering threads for SIG-108/110/113/114/115/119 with both
variant_offset=0 and =1; all retry phrasings differ from their first-attempt
phrasing, and `python -m py_compile` passes on all three edited files.

**Observability gap noted (not fixed in this task):** `probbrain-scan.py`
sends Telegram messages on publish failure but `tools/publish_signal.py`'s
errors don't reach the systemd journal — they're swallowed inside a
`subprocess.run(..., capture_output=True)` and only the *tail* gets sent to
Telegram. The next outage will surface in Telegram but still not in
`journalctl --user -u probbrain-scan.service`. Worth a follow-up task.

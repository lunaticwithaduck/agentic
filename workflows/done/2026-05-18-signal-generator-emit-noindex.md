---
title: Update signal-page generator to emit noindex,nofollow for NOT_PUBLISHED/SHADOW
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
The probbrain signal-page generator regenerates `signals/sig-NNN.html` files on every "live accuracy update" bot run. Hand-stamped robots noindex tags get wiped on each regen. Also, the seo_desc template always appends ` Status: {status}.` — which re-leaks Status into PENDING/WIN/LOSS snippets after every regen. Find the generator, fix both.

## Outcome
- Found generator at `/home/jojo/Documents/ProbBrain/tools/generate_signal_pages.py` (separate repo: `git@github.com:vpjonny/probbrain.git`, which mirrors output to `probbrain-accuracy` via the "live accuracy update" bot).
- Edited `_render_page(s)` to:
  - Compute `is_unindexable = is_shadow or status in ("NOT_PUBLISHED", "SHADOW")` (the `shadow` boolean field + the `status` string both feed this — a resolved shadow signal with `status=WIN` still counts as unindexable).
  - Emit `<meta name="robots" content="noindex,nofollow">` right after `<meta charset="UTF-8">` when `is_unindexable`.
  - Only append ` Status: {status}.` to `seo_desc` when `is_unindexable` (defense-in-depth on draft/shadow pages, clean snippet on indexable PENDING/WIN/LOSS pages).
- Ran the generator locally — verified 45 of 122 pages received the noindex tag (42 NOT_PUBLISHED/SHADOW status + 3 resolved-shadow signals where `is_shadow=True` but status flipped to WIN). Indexable pages have clean snippets, no `Status:` suffix.
- Committed as `fea346c8 feat(signals): emit noindex for shadow/unpublished, strip Status leak from indexable snippets` and pushed to `vpjonny/probbrain` origin/main.

## Pipeline note
The generator output lives in `Documents/ProbBrain/dashboard/signals/` and is mirrored to `vpjonny/probbrain-accuracy:signals/` by the live-accuracy update bot. I did **not** commit the locally-regenerated `dashboard/signals/*.html` files — the bot will produce the same output on its next scheduled run, and probbrain-accuracy already has my one-shot stamps in commit `720eda3a` covering the gap.

## Followups
- After the next "live accuracy update" bot run, verify the regenerated `probbrain-accuracy/signals/sig-100.html` (or any SHADOW signal) still has the `<meta name="robots" content="noindex,nofollow">` line. If missing, the bot is sourcing from a stale generator copy and we'll need to chase the mirror pipeline.
- Sitemap filtering for unpublished/shadow signals — already in the generator (per `feat(sitemap): exclude NOT_PUBLISHED and SHADOW signals` in probbrain-accuracy). Confirmed.

## Completion
Done.

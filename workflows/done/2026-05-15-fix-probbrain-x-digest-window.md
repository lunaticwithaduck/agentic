---
title: Fix probbrain-accuracy AI news X digest window — use discovered_at
created: 2026-05-15
completed: 2026-05-15
status: done
---

## Goal
The AI news fetcher (`/home/jojo/probbrain-accuracy/news-fetcher/news-fetch.js`) posts
to Telegram, Bluesky, and X. Telegram and Bluesky are at ~82 posts/24h; X is stuck at
~3 posts/24h because its candidate filter at line 591 uses `published_at` instead of
`discovered_at`.

HN items carry their original post time (often days old when ProbBrain discovers them)
and arXiv stamps everything at 04:00 UTC daily — both fall out of the 2-hour
`published_at` window immediately. Only GitHub trending items qualify because their
`published_at` equals `discovered_at`.

The comment at `news-fetch.js:117-119` already documents that `discovered_at` is the
canonical "when ProbBrain first saw it" timestamp that Telegram/Bluesky timing aligns
with — X is the outlier.

## Steps
- [x] Edit `/home/jojo/probbrain-accuracy/news-fetcher/news-fetch.js` line 591 to use
      `it.discovered_at` instead of `it.published_at`
- [x] Lint check: `node --check news-fetch.js`

## Outcome

Completed on 2026-05-15. Changed the X candidate filter in
`news-fetcher/news-fetch.js` (around line 591) from
`it.published_at` → `it.discovered_at` and added a comment explaining the why. Syntax
check passed (`node --check` OK). Sanity check against current state shows 1 eligible
candidate by `discovered_at` (vs 0 before); X waits for the digest minimum of 3 items,
so it'll fire on the next normal news cycle once HN/arXiv discoveries accumulate —
this is now aligned with how Telegram/Bluesky already behave.

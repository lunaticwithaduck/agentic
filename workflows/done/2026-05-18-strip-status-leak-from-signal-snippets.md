---
title: Strip "Status: X." leak from signal meta descriptions and verify /dashboard absent from sitemap
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Two final pre-GSC-submission cleanups:

1. Every signal HTML page embeds `Status: PENDING/WIN/LOSS/NOT_PUBLISHED/SHADOW.` at the end of its `<meta name="description">`, `<meta property="og:description">`, and JSON-LD `"description"` field. This appears as the snippet in Google search results and hurts both CTR and Google's quality signal. Strip the leak from PENDING/WIN/LOSS signals (keep on NOT_PUBLISHED/SHADOW as a soft "skip" signal since those shouldn't be indexed).

2. Verify `/dashboard` is absent from the new filtered `sitemap.xml`.

## Outcome
- Stripped ` Status: (PENDING|WIN|LOSS)\.` from 80 signal HTML files via in-place `sed`. Operation affects all three occurrences per file (meta description, og:description, JSON-LD description) because the regex is anchored on the literal pattern.
- 41 NOT_PUBLISHED/SHADOW pages confirmed untouched — their `Status:` markers remain as a soft hint to Google (and SHADOW pages additionally carry `noindex,nofollow` from prior task).
- `/dashboard` confirmed absent from `sitemap.xml` (grep returns 0). GSC's old `REDIRECT_ERROR` entry should drop off after the next crawl since Google no longer sees it referenced.
- Idempotent — re-running the sed command on already-stripped files is a no-op.

## Caveats
- Page bodies still display the status (intentional — the data point belongs there, just not in the search snippet).
- Signal page generator (not located in this session) will overwrite the strip on next regeneration. Pair with a generator-side fix when next touching that code.

## Completion
Done.

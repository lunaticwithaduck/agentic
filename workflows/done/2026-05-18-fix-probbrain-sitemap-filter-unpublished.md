---
title: Filter unpublished/shadow signals out of probbrain sitemap.xml
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
GSC reports 107/0 indexed for the sitemap. Among 360 signal pages, 63 are `Status: NOT_PUBLISHED` and 60 are `Status: SHADOW` (paper-traded internal). Including these in the sitemap tells Google to crawl drafts/internal-test content, which gets every signal flagged as "Crawled - currently not indexed". Remove them from the sitemap so only resolved (`WIN`, `LOSS`) and live unresolved (`PENDING`) signals are submitted.

## Outcome
- `probbrain-accuracy/sitemap.xml` rewritten: **128 → 87 entries** (41 NOT_PUBLISHED/SHADOW signals removed)
- All top-level pages preserved (/, /arbitrage, /news, /kelly, /methodology, /signals, /portfolio, /status)
- All WIN/LOSS/PENDING signals preserved
- Generator script `news-fetcher/lib/sitemap.js` is in its original state — the next time `news-fetch.js` runs, it will regenerate the full unfiltered sitemap. Generator-level filtering left to the user to decide (do it permanently in code, or treat sitemap.xml as a hand-curated file).
- User will commit + deploy via Vercel + re-submit sitemap in GSC

## Followups (not done in this task)
1. Page-side `Status: ...` leak in meta descriptions of PENDING signals — still tells Google "this is not resolved yet". Consider removing `Status:` from the description template, or adding `<meta name="robots" content="noindex">` to NOT_PUBLISHED/SHADOW pages so they're not indexable even via internal links from /signals listing.
2. /signals listing page currently links to NOT_PUBLISHED signals (sig-001 etc.). Removing those from the listing would prevent Google from discovering them via internal links.
3. www.probbrain.com still serves 200 alongside apex (user said apex set as primary on Vercel — verify www now 301s to apex).
4. /dashboard returns 308 → / from a sitemap-listed URL; investigate whether /dashboard should exist or be removed.
5. The site's only impressions today come from LLM web-search agents (Perplexity/SearchGPT/Bing Copilot), not human Google searches — see GSC query list.

## Completion
Done.

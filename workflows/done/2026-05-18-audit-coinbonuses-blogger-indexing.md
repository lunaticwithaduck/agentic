---
title: Audit coinbonuses.blogspot.com indexing and identify fixes
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Investigate indexing state of coinbonuses.blogspot.com via GSC URL Inspection API. Find why submitted=41 indexed=0 in the atom feed report. Output a prioritized fix list — then fix what can be fixed automatically.

## Outcome — root cause
Each post body had a **second** `<link rel="canonical">` (alongside Blogger's auto-injected one in the head) pointing to a long-slug URL that doesn't exist as a real post. Two publishers (`/home/jojo/bcgame-blogger/blogger_bot.py` and `/home/jojo/Documents/ProbBrain/tools/publish_blogger_signal.py`) computed a predicted URL from the post title, baked it into the body's canonical/og:url/JSON-LD `@id`/`mainEntityOfPage`, then published — but Blogger truncates slugs at ~38 characters on publish, so the URL Blogger actually serves is shorter and different from what's baked in the body. Google saw the conflict and refused to index 38 of 44 posts.

Initial wrong diagnosis: I matched the body canonical with a double-quote regex that missed Blogger's head canonical (single-quote, reversed attr order), so I thought the canonical was wrong everywhere. Once both canonicals were visible side-by-side, the real story was clear: head canonical is right (Blogger generates it from the served URL), body canonical was wrong (publisher script baked in a predicted URL).

## Outcome — fixes applied
1. **`/home/jojo/bcgame-blogger/blogger_bot.py`** — `publish_post()` now accepts a `predicted_url` argument. After `posts().insert()` returns, if Blogger's returned `url` differs from `predicted_url`, the function does a string replace on the HTML and calls `posts().update()` to patch canonical/og:url/JSON-LD to match the real URL.
2. **`/home/jojo/Documents/ProbBrain/tools/publish_blogger_signal.py`** — same fix applied after the existing `posts().insert()` call.
3. **`/home/jojo/bcgame-blogger/fix_canonical_drift.py`** — new one-shot cleanup script. Lists every post on the blog, extracts self-referencing URLs from body canonical / og:url / JSON-LD `@id` / `mainEntityOfPage`, replaces any that don't match the post's actual URL, calls `posts().update()`. Idempotent.
4. Ran the cleanup: **fixed=38, already-clean=6, failed=0**.
5. Verified live: all 44 posts now report canonical = served URL. `0 mismatches / 44 total`.

## What the user still needs to do in GSC
- **Sitemaps → re-submit** `https://coinbonuses.blogspot.com/sitemap.xml` (was stuck `isPending: True, lastDownloaded: null` for 11 days).
- **URL Inspection → Request Indexing** on 3–5 priority posts (BC.Game review, Polymarket review, Kelly criterion, Upshot Cards review) to push fresh recrawl. Without the request, Google's recrawl cadence is slow on `.blogspot.com` subdomains.
- Wait 1–2 weeks; revisit GSC to see indexed-count climb from 0.

## Followups (deliberately not done)
- The publishers still inject SEO meta tags (canonical/og:url/JSON-LD) into post body content. A cleaner architecture moves those to the Blogger theme template using `data:view.url` and `data:view.title`. But that requires Blogger dashboard theme editing and only matters once the in-body injection is wrong; right now it's correct, so this is a future tidy-up not a bug.
- Indexing API pings are deliberately no-op in `ping_google_indexing()` (per code comment, Google deprecated for general content in 2023, only fast-indexes JobPosting/BroadcastEvent now).

## Completion
Done.

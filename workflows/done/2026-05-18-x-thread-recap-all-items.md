---
title: Switch X 2h recap from capped single tweet to thread of all items
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Current X poster crams the 2h news recap into a single 280-char tweet via
`pickDigestItems(items, max=3)` + `formatDigest`. With realistic headline
lengths, only 2 items fit before the limit hits, so the tweet looks like:

```
🆕 AI news past 2h:
• Source A: Headline 1
• Source B: Headline 2
→ probbrain.com/news
```

User wants ALL last-2h items in the recap. Single tweet is physically
impossible past ~3 items (280-char limit). Plan: **post as a reply-chain
thread**, one item per tweet, so every news item gets its own line.

**Explicit risk acknowledged**: Free-tier X API is ~17 posts/24h per
user. A thread of 5 items every 2h = 60 posts/day, well over the cap.
User chose thread anyway — accepting the increased likelihood of
hitting the per-app 403 cap. If 403s recur, the "burn 2h slot on
failure" fix from `2026-05-18-diagnose-x-api-403` still protects
us from a retry storm.

## Steps
- [x] Inspected news-item shape: `id`, `source_id`, `source_name`,
      `category_emoji`, `title`, `headline`, `url`, `published_at`,
      `discovered_at` — all available per item.
- [x] Added `pickAllRecent(items, cutoffMs, posted)` in `lib/x.js` —
      picks every unposted item discovered in last `cutoffMs`, sorted
      newest first, source-diversified (each source's newest first,
      then fill with remaining). No hard upper cap; caller caps to
      `THREAD_MAX`.
- [x] Added `formatThreadTweets(items)` in `lib/x.js` — returns array
      of tweet strings, each ≤280 chars. Per-tweet: `[i/N] emoji source`
      + headline (truncated to fit) + URL. First tweet prepended with
      `🆕 AI news past 2h\n\n`; last tweet appended with
      `\n\n→ probbrain.com/news`.
- [x] Added `postThread(client, items)` in `lib/x.js` — caps at
      `THREAD_MAX = 25`, calls `client.v2.tweetThread(texts)` (native
      helper), single-tweet fallback if `items.length == 1`. Returns
      `{ ids, texts }`.
- [x] Updated `news-fetch.js` `postToX`:
      - Replaced import `pickDigestItems, postDigest` →
        `pickAllRecent, postThread`.
      - Inlined candidate selection became `pickAllRecent(items,
        X_DIGEST_WINDOW_MS, posted)`.
      - On success: marks every item in the picked list as posted
        (capped at thread-length actually emitted), logs
        `x: posted thread with N items (first tweet_id=...)`.
- [x] Kept `X_DIGEST_MIN_ITEMS = 3` gate (don't post a "thread" with
      fewer items than that — saves cap for genuine bursts) and the
      already-fixed failure-path `last_digest_at` burn.
- [x] Dry-ran `formatThreadTweets` against real `news.json`: 25 well-
      formed tweets, longest 229 chars, header on first, footer on
      last. Format reads cleanly per tweet.
- [x] Stopped scanner timers before commit/push (muscle memory from
      the three prior races today). Committed `8b5700c2`. Pushed
      `cf3c23d0..8b5700c2 main -> main`. Restarted timers.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-18-x-thread-recap-all-items.md` before starting any new work.

## Outcome

Completed on 2026-05-18.

Thread mode shipped. Next X attempt won't fire until the in-app 2h
gate clears at **18:57 EEST** (`last_digest_at: 2026-05-18T13:57:19Z` UTC
+ 2h). First scheduled news-fetcher tick after that is **19:05 EEST** —
that's when the first real thread either posts (visible at @ProbBrain)
or hits 403 with the burn-the-slot fix preventing a retry storm.

Dry-run preview of the new format (sample tweets from real news.json):

```
[7/25] 🔥 Hacker News
WriteUp: 16 Bytes of x86 that turn Matrix rain into sound
https://hellmood.111mb.de//wake_up_16b_writeup.html

[18/25] 💻 GitHub Trending
GitHub project agent-skills provides a validated skill registry…
https://github.com/tech-leads-club/agent-skills

[25/25] 📄 arXiv cs.CL
DiscoExplorer: An Open Interface for the Study of Multilingual…
https://arxiv.org/abs/2605.15304

→ probbrain.com/news
```

**Trade-off the user accepted**: each thread is now N posts instead of
1. On X Free-tier (~17 posts/24h per user), a 12-attempt/day cadence at
average 5 items/thread = ~60 posts/day → will trip the per-app cap.
The "burn 2h slot on failure" fix from earlier this session means we
don't retry-storm when that happens — we just fail one thread and wait
2h for the next attempt. If the cap-trip rate is high, next move is
either Premium ($8/mo, 25k-char single post) or shortening the per-
thread item count.

**Architectural decisions worth noting**:
- Kept `THREAD_MAX = 25` (X's documented thread length max from the
  client lib). If a 2h window has >25 items, the oldest 25-N are
  dropped — they'll show up on the website but not the thread. With
  source-diversification first, the kept items are at least 1 per
  active source.
- Picker prefers `discovered_at` over `published_at` (consistent with
  the 2026-05-15 X-digest-window fix and TG/Bluesky behavior — HN/
  arXiv items with old original publish times still qualify if freshly
  discovered).
- `formatThreadTweets` truncates the headline (not the URL) when a
  tweet overflows. URLs are kept whole because (a) X t.co shortens
  them on-receive to a fixed 23 chars anyway and (b) a truncated URL
  is broken; a truncated headline still conveys the gist.
- Old `formatDigest` / `pickDigestItems` / `postDigest` left in place
  but no longer wired into `postToX`. Easy to revert if thread mode
  trips problems in production. Will remove on the next pass if the
  thread mode stays.

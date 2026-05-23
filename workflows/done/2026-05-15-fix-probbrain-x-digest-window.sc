---
domain: news-aggregation
source_task: 2026-05-15-fix-probbrain-x-digest-window.md
date: 2026-05-15
keywords: [news, aggregator, feed, rss, hn, arxiv, digest, discovered_at, published_at]
---

## Extracted Knowledge

When aggregating items from multiple feed sources (RSS, HN, arXiv, GitHub trending,
sitemaps, HTML indexes) into a unified pool that is then re-broadcast to outbound
channels (Telegram, Bluesky, X), there are two distinct timestamps and they are NOT
interchangeable:

- **`published_at`** = the source's original publication time. Comes from the
  upstream feed (HN's `time`, arXiv's submission date, RSS `<pubDate>`). Can be days
  or weeks old by the time the aggregator pulls the item. arXiv stamps everything at
  `04:00 UTC` once per day. HN trending items resurface long-tail posts. GitHub
  trending uses "now" so `published_at ≈ discovered_at` for that source only.
- **`discovered_at`** = when the aggregator first saw it during its own fetch loop.
  This is the meaningful "newness" signal for any downstream cadence/throttle logic
  because it's what determines what's actually new from the aggregator's perspective.

**The bug pattern:** outbound channels with a digest window (e.g. "post a digest of
items from the past 2h every 2h") that filter candidates by `published_at` will
silently starve themselves. HN/arXiv items get discovered hours or days after their
published time and never qualify; only the GitHub-style "publish = now" sources count.
On a healthy multi-source feed this means the channel posts ~3/24h when it should
post ~80/24h, matching the other channels that use a simple "haven't posted this id
yet" dedup.

**The fix:** time-window filters for outbound posting should key off `discovered_at`.
A simple-dedup channel (Telegram/Bluesky in this case) implicitly uses discovery
ordering because new items always appear after the last `posted` write — but
explicit time-window filters need to be wired to `discovered_at` deliberately.

Related: when seeding a new outbound channel from an existing item pool, record
the seeded ids with `null` post-times so they show up in the dedupe set but don't
contribute to latency stats — this matches the pattern in
`postToTelegram`/`postToBluesky`/`postToX`'s `isFirstRun` branches.

## Proposed Skill Content

A `news-aggregation` skill would cover:

1. Timestamp semantics: source-provided `published_at` vs aggregator-recorded
   `discovered_at`. Which one to use for: deduping, time-window filtering, latency
   metrics, "ago" UI badges, RSS feed pubDate output.

2. Per-source `published_at` quirks worth remembering:
   - HN: original-post time, can be days old
   - arXiv: bucketed at `04:00 UTC` daily
   - GitHub trending: ≈ discovery time
   - RSS: as published by the source; often reliable but varies

3. Digest throttle pattern: minimum interval between digests + minimum item count
   + sliding window — and that the window MUST be on `discovered_at`, not
   `published_at`, to avoid starvation from late-discovered sources.

4. Silent-seed pattern for first-run of a new outbound channel: write all current
   item ids with `null` post-times so old items don't spam, while still tracking
   them in the dedupe map.

5. Cross-channel symmetry: when one channel uses simple dedup (no time window) and
   another uses a digest window, verify they target the same effective set —
   otherwise the windowed channel will silently lag.

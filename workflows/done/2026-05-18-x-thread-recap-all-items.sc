---
domain: twitter-api
source_task: 2026-05-18-x-thread-recap-all-items.md
date: 2026-05-18
keywords: ["tweetThread", "twitter-api-v2", "reply chain", "280 char limit", "thread mode", "rate cap interaction", "URL t.co weight"]
---

## Extracted Knowledge

### `client.v2.tweetThread(texts)` posts a reply chain in one call

twitter-api-v2 ships a native thread helper that chains tweets as
reply-to-previous:

```js
const results = await client.v2.tweetThread([
  '[1/3] header + first item',
  '[2/3] second item',
  '[3/3] third item + footer',
]);
// results: TweetV2PostTweetResult[] — one per tweet, in order
// each result has .data.id; the chain is already in place server-side
```

Important constraints:
- Each tweet in the array must independently respect the 280-char
  weighted limit (no auto-splitting).
- Documented thread length max is 25 tweets per call. Beyond that
  you need to chain multiple `tweetThread` calls with explicit
  `in_reply_to_tweet_id`.
- Each tweet in the thread counts as a separate write toward the
  X-side per-user / per-app rate cap. A 10-tweet thread = 10 writes.

The 1-item case can be a special-case skip to a plain `client.v2.tweet`
to avoid the (small) thread-call overhead.

### URLs count as 23 weighted chars regardless of actual length

X's "weighted character count" treats every URL as exactly 23 chars
because t.co will wrap it server-side. But the SDK measures the
literal string length client-side when you compute room. Two
implications:

1. A long URL string (100 chars) eats 100 of your budget in code,
   even though X only counts 23. Conservative — tweets fit.
2. Don't try to "save chars" by shortening URLs yourself — t.co
   does it for you, and a self-shortened URL just makes you eat the
   23 chars for the t.co wrap PLUS your own URL string. Always pass
   the canonical URL.

When building a per-tweet budget, use actual string length and
accept that you're under-utilizing the actual 280 limit by ~20-50
chars per URL. The simplicity is worth it.

### Per-tweet budget pattern: fix boilerplate, truncate headline only

For a thread item with fixed structure (index marker, emoji, source,
headline, URL, optional header/footer), compute the fixed overhead
first and let the variable-length field absorb the rest:

```js
const headerPart = isFirst ? `${THREAD_HEADER}\n\n` : '';
const footerPart = isLast ? `\n\n${THREAD_FOOTER}` : '';
const idxMarker = `[${i}/${n}] `;
const sourceLine = emoji ? `${emoji} ${src}` : src;
const urlPart = url ? `\n${url}` : '';

const fixed = headerPart.length + idxMarker.length + sourceLine.length
            + 1 + urlPart.length + footerPart.length;
const room = Math.max(20, TWEET_LIMIT - fixed);
const headTrim = head.length > room ? head.slice(0, room - 1) + '…' : head;
```

**Truncate the headline, never the URL** — a truncated headline still
conveys the gist; a truncated URL is broken. The `Math.max(20, ...)`
floor prevents pathological cases where the boilerplate exceeds 280
(e.g., extremely long source name).

### Source-diversification picker

For digests that should represent N sources fairly, sort by recency
then pick one item per source first, fill remaining slots with newest
leftovers:

```js
const eligible = items.filter(...).sort(newestFirst);
const seenSources = new Set();
const head = [];  // one per source
const tail = [];  // leftover items from already-seen sources
for (const it of eligible) {
  if (seenSources.has(it.source_id)) tail.push(it);
  else { seenSources.add(it.source_id); head.push(it); }
}
return [...head, ...tail];
```

This gives the thread "breadth before depth" — every source represented
once before any source gets a second item. Capping the final array at
`THREAD_MAX` then preserves source diversity over thread length.

### Thread mode multiplies post count → must be cap-aware

Switching a digest from "1 tweet per trigger" to "N tweets per trigger
as a thread" multiplies the per-day post count by N. On X Free tier
(~17 posts/24h per user), this can self-DoS within hours.

Two mitigations required when shipping thread mode:

1. **Keep the time-window gate** (e.g., 2h between thread attempts) so
   threads can't fire on every scheduler tick. Without this, a 15-min
   scheduler with 5-item threads = 480 tweets/day.
2. **Burn the gate on failure too** — otherwise a 403 leaves the gate
   open and the next tick re-attempts the same thread. See sibling
   `.sc` on the `last_digest_at` failure-path bug.

Without both, thread mode trips X's anti-spam within hours and the
account silently 403s for the remainder of the rolling window.

## Proposed Skill Content

Folds into the existing `twitter-api` skill (once synthesized):

1. **`client.v2.tweetThread(texts)`** is the canonical thread API.
   Each element is one tweet, ≤280 chars; max 25 per call. Returns
   array of TweetV2PostTweetResult in order.
2. **URLs eat 23 weighted chars** but the SDK measures literal length.
   Use literal length client-side; accept the budget under-utilization
   in exchange for guaranteed-fit tweets.
3. **Truncate the variable-length field (headline), never the URL**.
   A broken URL is worse than a clipped title.
4. **Source-diversification picker pattern** for any "represent N
   sources" digest. Cap final array after diversification.
5. **Thread mode interacts dangerously with rate caps**. Multiplying
   post count per trigger demands a paired time-window gate AND
   failure-path slot burn. Ship both at once or expect to be 403'd.
6. **1-item special case**: skip `tweetThread` for a 1-element array
   and call `client.v2.tweet` directly. Saves an API hop and avoids
   the (small) thread-API overhead.

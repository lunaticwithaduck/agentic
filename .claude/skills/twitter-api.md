---
name: twitter-api
description: Build and debug X (Twitter) API integrations using twitter-api-v2 — auth, write-cap diagnostics, thread mode, 280-char budgeting, and rate-cap-safe patterns
activation:
  keywords: ["x api", "twitter api", "twitter-api-v2", "tweet", "tweetThread", "x rate limit", "403 forbidden tweet", "OAuth 1.0a", "X consumer key", "x access token"]
  file_patterns: ["**/x.js", "**/twitter*", "**/*tweet*"]
---

# X (Twitter) API

## Purpose
Operate the X API v2 via the `twitter-api-v2` npm package for personal-
tier integrations (single account, news/digest bots). Covers OAuth 1.0a
user-context auth, diagnosing the recurring 403 patterns, packing
tweets within the 280-char budget, posting threads, and the rate-cap
arithmetic that makes thread mode dangerous without paired safeguards.

## Auth setup

X v2 write endpoints accept OAuth 1.0a user-context credentials:

```js
import { TwitterApi } from 'twitter-api-v2';

const client = new TwitterApi({
  appKey:       process.env.X_CONSUMER_KEY,
  appSecret:    process.env.X_CONSUMER_SECRET,
  accessToken:  process.env.X_ACCESS_TOKEN,
  accessSecret: process.env.X_ACCESS_TOKEN_SECRET,
});
```

Four env vars, all required. Not bearer token, not OAuth 2.0 PKCE —
those are for read-only or app-only contexts. For posting tweets from
your account, OAuth 1.0a is what `tweetThread`, `v2.tweet`, and most
write endpoints expect.

## Diagnosing 403s

### Disambiguate auth vs write-cap with `GET /2/users/me`

When `v2.tweet()` returns 403, the first diagnostic is the cheapest
read call:

```js
const me = await client.v2.me();
// OK: tokens valid; 403 is write-specific (rate cap or scope)
// FAIL same 403: tokens or account-level problem
// FAIL 401: token expired or revoked
```

This isolates auth-validity from write-permission/rate issues in <1s.

### `403 "You are not permitted to perform this action" + "type":"about:blank"` = rate cap, not permission revocation

```
status: 403
title: Forbidden
detail: You are not permitted to perform this action.
type: about:blank
```

The `type: about:blank` is the diagnostic key. X uses specific `type`
URLs for known categories (`…/problems/usage-capped`,
`…/problems/oauth1-permissions`, `…/problems/duplicate-rules`). **A
blank type means none of those matched** — it's X's catch-all for
"the request is fine but you can't do this right now," which in
practice almost always means a per-app/per-user rate cap was tripped
at X's anti-spam layer.

Intermittent successes interleaved with 403s rule out permission
revocation — if permissions were truly revoked, the error would be
permanent.

### X caps on ATTEMPTS, not successes

X Free-tier documented limits:
- 1500 reads/month
- 500 writes/month
- 17 tweets/24h per user

A `status.json` showing `posts_24h: 6` looks safely under the cap, but
X's anti-spam counts **attempts** (including failed POSTs), not just
successful tweets. A buggy retry loop posting 96 attempts/day will
trip the cap even though only 6 succeed.

Always reconcile "successful posts/day" against "POST attempts/day"
when diagnosing 403s. If the journal shows repeated `X ERR: 403`
lines at fixed intervals, the bug is local: the retry loop is the
cap-tripper, not the underlying API plan.

## Tweet budgeting and packing

### URLs count as 23 weighted chars regardless of actual length

X's "weighted character count" treats every URL as exactly 23 chars
because t.co will wrap it server-side. But the SDK measures the
literal string length client-side when you compute room. Two
implications:

1. A long URL string (100 chars) eats 100 of your client-side budget,
   even though X only counts 23. Conservative — tweets always fit.
2. Don't self-shorten URLs — t.co does it on receipt, and a
   self-shortened URL just makes you eat the 23 chars for the t.co
   wrap PLUS your shortener's chars. Always pass canonical URLs.

When building a budget, use literal string length and accept that
you're under-utilizing the actual 280 limit by ~20-50 chars per URL.

### Per-tweet budget pattern: fix boilerplate, truncate the variable field

For a tweet with fixed structure (prefix, source, headline, URL),
compute the fixed overhead and let the variable-length field absorb
the remainder:

```js
const headerPart = isFirst ? `${HEADER}\n\n` : '';
const footerPart = isLast ? `\n\n${FOOTER}` : '';
const idxMarker = `[${i}/${n}] `;
const sourceLine = emoji ? `${emoji} ${src}` : src;
const urlPart = url ? `\n${url}` : '';

const fixed = headerPart.length + idxMarker.length + sourceLine.length
            + 1 + urlPart.length + footerPart.length;
const room = Math.max(20, 280 - fixed);
const headTrim = head.length > room ? head.slice(0, room - 1) + '…' : head;
```

**Truncate the headline (or other variable text), never the URL** — a
truncated headline still conveys the gist; a truncated URL is broken.
The `Math.max(20, ...)` floor prevents pathological cases where the
boilerplate exceeds 280 (e.g., extremely long source name).

## Thread mode

### `client.v2.tweetThread(texts)` posts a reply chain in one call

```js
const results = await client.v2.tweetThread([
  '[1/3] header + first item',
  '[2/3] second item',
  '[3/3] third item + footer',
]);
// results: TweetV2PostTweetResult[] — one per tweet, in order
```

Constraints:
- Each element must independently respect the 280-char weighted limit.
  No auto-splitting.
- Documented max is 25 tweets per `tweetThread` call. For longer
  threads, chain multiple calls with explicit `in_reply_to_tweet_id`.
- Each tweet in the thread counts as a separate write toward all rate
  caps. A 10-tweet thread = 10 writes.

Special-case the 1-item path to plain `client.v2.tweet` to skip thread
overhead.

### Thread mode multiplies post count → must be cap-aware

Switching a digest from "1 tweet per trigger" to "N tweets per trigger
as a thread" multiplies the per-day post count by N. On Free tier
(~17 posts/24h per user), this can self-DoS within hours.

**Two mitigations required when shipping thread mode**:

1. **Keep a time-window gate** (e.g., 2h between thread attempts) so
   threads can't fire on every scheduler tick. Without this, a 15-min
   scheduler with 5-item threads = 480 tweets/day.
2. **Burn the gate on failure too** — see "Per-window gates must
   update on failure" below.

Ship both at once or expect to be 403'd within hours.

## The "burn the slot on failure" pattern

For any per-time-window gate that wraps an API call:

```js
// BUG: gate only enforced after success
if (lastDigestAt && now - lastDigestAt < MIN_INTERVAL_MS) return;  // skip
try {
  await postDigest(...);
  await writeJsonAtomic(state, { last_digest_at: now });  // ← only on success
} catch (e) {
  log(`ERR: ${e.message}`);
  // ← last_digest_at NEVER updated on failure
}
```

After the first failure, `lastDigestAt` stays frozen at the last
*successful* post. Every subsequent scheduler tick sees the gate
clear → re-attempts → fails → gate still clear → re-attempts. Result:
attempts at scheduler cadence (e.g., every 15min) instead of the
intended gate cadence (e.g., every 2h).

Fix is one line — write the gate timestamp in the catch block too:

```js
} catch (e) {
  log(`ERR: ${e.message}`);
  const failedAt = toUtcIso(new Date());
  await writeJsonAtomic(state, { ...state, last_digest_at: failedAt });
}
```

Cost: a transient network error costs the next gate slot. Acceptable
trade-off for a throttle-by-design path. The principle: **any
"don't do X more than once per Y" gate must update on every attempt,
not every success** — otherwise the gate becomes a no-op the moment
anything errors.

## Source-diversification picker

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

"Breadth before depth" — every source represented once before any
source gets a second item. Capping the final array preserves source
diversity over thread length.

## Failure Modes

- **Hammer-on-failure**: time-window gate only enforced after success
  → scheduler-cadence attempts instead of gate-cadence. Symptom: 96
  attempts/day with intended 12. Triggers X anti-spam 403s.
- **Treating intermittent 403 as permanent permission loss**:
  successes interleaved with 403s rule out permission revocation;
  it's a rate cap. Don't waste time at developer.x.com.
- **Treating intermittent 403 as a "feature" of X**: it's a self-
  inflicted retry loop in your code 90% of the time. Check attempt
  cadence first.
- **Self-shortening URLs to save chars**: t.co does it for you on
  receipt; pre-shortening eats chars for both your shortener and
  t.co's wrap. Pass canonical URLs.
- **Truncating the URL to fit**: broken link. Truncate the headline
  instead.
- **Shipping thread mode without paired time-window gate**: 5-item
  threads × 15-min scheduler = 480 posts/day, will trip cap within
  hours.
- **Posting 1-item "thread"**: works but adds a tiny API overhead.
  Special-case to `client.v2.tweet`.

## Output Format

When debugging an X API issue, provide:

1. Auth probe result (`v2.me()` output or error).
2. Recent attempt pattern (count of `X ERR: 403` per hour vs
   successful `tweet_id=...` logs).
3. Local gate config (`MIN_INTERVAL_MS`, `THREAD_MAX`, scheduler
   cadence) and verification that attempts-per-day = scheduler-ticks-
   per-day ÷ (gate-window-minutes ÷ scheduler-tick-minutes), not
   `scheduler-ticks-per-day`.
4. Specific fix: gate-on-failure write, thread cap, header/footer
   boilerplate math, etc.
5. Recovery validation plan: which scheduler tick should validate
   the fix and how to read the journal.

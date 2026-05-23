---
domain: twitter-api
source_task: 2026-05-18-diagnose-x-api-403.md
date: 2026-05-18
keywords: ["x api", "twitter-api-v2", "403 forbidden", "rate cap", "burn the slot", "last_digest_at", "intermittent 403"]
---

## Extracted Knowledge

### `403 "You are not permitted to perform this action" + "type":"about:blank"` is rate-cap, not permission revocation

When `client.v2.tweet()` (twitter-api-v2 npm) returns:

```
status: 403
title: Forbidden
detail: You are not permitted to perform this action.
type: about:blank
```

…the `type: about:blank` is the diagnostic key. X uses specific
`type` URLs for known error categories — `…/problems/usage-capped`,
`…/problems/oauth1-permissions`, `…/problems/duplicate-rules`, etc.
**A blank type means none of those matched** — it's X's catch-all
"the request is structurally fine but you can't do this right now,"
which in practice almost always means a per-app or per-endpoint
rate cap was tripped at X's anti-spam layer.

Contrast: if app permissions were truly revoked (Read-Only on
developer.x.com), the error would be permanent. **Intermittent
successes interleaved with 403s rule out permission revocation.**

### Disambiguate auth vs write-cap with `GET /2/users/me`

When 403s are recurring on POST endpoints, the first diagnostic is
the cheapest read call:

```js
import { TwitterApi } from 'twitter-api-v2';
const client = new TwitterApi({ appKey, appSecret, accessToken, accessSecret });
const me = await client.v2.me();  // GET /2/users/me
```

- **GET succeeds** → tokens are 100% valid; the 403 is write-specific.
  Investigate rate caps, attempt cadence, or per-endpoint permissions.
- **GET fails with same 403** → tokens or account level. Different
  problem class.
- **GET fails with 401** → token expired or revoked. Re-auth needed.

This isolation step takes <1s and removes hours of speculation.

### X rate-caps on ATTEMPTS, not successes

X Free-tier documented limits (as of 2026):
- 1500 reads/month
- 500 writes/month
- 17 tweets/24h per user

`status.json` showing `posts_24h: 6` looks safely under the cap.
But X's anti-spam layer counts **attempts** (including failed POSTs),
not just successful tweets. A buggy retry loop posting 96 attempts/day
will trip the cap even though only 6 succeed.

Always reconcile "successful posts/day" against "POST attempts/day"
when diagnosing X 403s. If attempt count is high (visible in the
journal as repeated `X ERR: 403` lines at fixed intervals), the bug
is local: the retry loop is the cap-tripper, not the underlying API
plan.

### "Burn the slot on failure too" — per-window gates must update on failure

The bug pattern that caused this incident:

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
*successful* post's timestamp. Every subsequent tick sees
"now - lastDigestAt > MIN_INTERVAL_MS" → gate clears → re-attempts.
Result: ~96 attempts/day instead of the intended 12 on a 15-min
scheduler with a 2h gate.

Fix is one line — write the gate timestamp in the catch block too:

```js
} catch (e) {
  log(`ERR: ${e.message}`);
  const failedAt = toUtcIso(new Date());
  await writeJsonAtomic(state, { ...state, last_digest_at: failedAt });
}
```

Cost: a transient network error costs the next 2h slot. Acceptable
trade-off for a throttle-by-design path. The principle: **any
"don't do X more than once per Y" gate must update on every attempt,
not every success** — otherwise the gate becomes a no-op the moment
anything errors.

### "Attempt count looks suspicious" beats "upstream provider is at fault"

In hypothesis ranking, when an API returns a cap-style error but your
local attempt rate looks anomalously high, **check the attempt
cadence first** before assuming the upstream provider is the
constraint. The user caught this incident by asking "it was supposed
to post every 2h. how come we tryna post every 15 mins?" — the gate
bug was visible from the attempt cadence alone. Diagnostic order
should be:

1. How often is the request being attempted (journal: count
   `ERR` lines per hour)?
2. Compare to the local-config "intended" cadence
   (`MIN_INTERVAL_MS`, `X_MIN_INTERVAL_MS`, etc.)
3. If mismatch → local bug. If match → upstream cap.

## Proposed Skill Content

A `twitter-api` skill should activate on prompts mentioning X API,
Twitter API, twitter-api-v2, posting tweets, X rate limits, 403
Forbidden on tweet POST. It should teach:

1. **`type:"about:blank"` 403 on POST /2/tweets = rate cap**, not
   permanent permission loss. Intermittent successes confirm it.
2. **`GET /2/users/me` first** to isolate auth from write-cap issues.
3. **X counts attempts, not just successful tweets**. A retry storm
   trips the cap even with low success counts.
4. **The "burn the slot on failure" pattern** for any per-time-window
   gate: update the gate timestamp in the failure path too, or the
   gate becomes a no-op after any error.
5. **Diagnostic order**: compare attempt cadence to intended cadence
   before assuming the upstream provider is the constraint.
6. **twitter-api-v2 quirks**: OAuth 1.0a user-context uses appKey/
   appSecret + accessToken/accessSecret (NOT bearer token, NOT
   OAuth 2.0); `client.v2.tweet(text)` is the POST endpoint;
   `e.data` has the JSON error body, `e.headers` has rate-limit
   headers when present.

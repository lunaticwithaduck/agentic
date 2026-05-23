---
domain: twitter-api
source_task: 2026-05-11-fix-probbrain-x-duplicate-content.md
date: 2026-05-11
keywords: ["twitter", "x-api", "tweepy", "duplicate content", "403", "anti-spam", "thread publishing"]
---

## Extracted Knowledge

### X's duplicate-content filter trips on near-identical text, not exact duplicates

When posting to Twitter/X via the v2 API (`tweepy.Client.create_tweet`), the
account-level anti-spam filter returns:

```
403 Forbidden: You are not allowed to create a Tweet with duplicate content.
```

This fires when the new tweet shares long substrings with the account's recent
tweets — it is NOT a hash equality check. Two tweets with the same template
prefix/suffix and different middle-text can both trigger it if the shared
boilerplate is the dominant content.

**Concrete failure pattern observed:** every signal-publishing tweet used a
fixed template:
```
[HIGH] <question> Market: 20% YES. Our estimate: 15%. Gap: -X.Xpp — market appears overpriced.
```
After 4–5 consecutive publishes with `[HIGH]…20% YES…15%…overpriced.` as the
shared backbone, every subsequent post got 403'd. The questions differed but
the surrounding scaffolding was identical.

### Invisible-character disambiguators don't help

Appending zero-width spaces, trailing periods, or unicode markers is unreliable —
X's filter normalizes whitespace and considers the visible content. The fix
needs to be variation in the *readable* text.

### The reliable mitigation: deterministic per-item phrasing rotation

Maintain a small pool of surface-text templates (4 is enough for most signal
volumes). Pick the template per-item using a stable hash of an item-specific
identifier:

```python
def _variant_index(seed: str, offset: int, n: int) -> int:
    h = int(hashlib.sha1((seed or "fallback").encode("utf-8")).hexdigest()[:8], 16)
    return (h + max(0, offset)) % n
```

Properties this gives you:
- Same item always renders the same way on the first attempt → idempotent
  retries don't suddenly post different content if the publish step is replayed.
- `offset=1` selects a *different* template for the retry path after a 403.
- Item-keyed (not time-keyed) means two items posted seconds apart still get
  uncorrelated variants.

Vary the structural parts that are reused: openers ("HIGH conviction call.",
"Mispricing flag (HIGH):"), middle phrasing, and the boilerplate closing tweet
(rotate the order of "dashboard / telegram / follow" lines, swap "We track
every call" / "Public scoreboard" / etc). Preserve all required content
(URLs, hashtags, mention of the brand) across every variant.

### tweepy thread chaining + partial-failure handling

Threads require posting sequentially, passing the previous id as
`in_reply_to_tweet_id`:

```python
r1 = client.create_tweet(text=tweet1, media_ids=[...])
r2 = client.create_tweet(text=tweet2, in_reply_to_tweet_id=r1.data["id"])
r3 = client.create_tweet(text=tweet3, in_reply_to_tweet_id=r2.data["id"])
```

Gotcha: if tweet 2 or 3 fails after tweet 1 succeeds, you have a published
orphan you can't roll back. When implementing a retry-on-dupe-403, only retry
if no tweets have been posted yet (`not ids`) — retrying after a partial
thread produces visible duplicates on the timeline.

### Detecting the duplicate-content case

```python
def _is_duplicate_content_error(exc: BaseException) -> bool:
    return "duplicate content" in str(exc).lower()
```

Other 403s from X (rate limits, suspended permissions, etc.) need different
handling — don't catch all 403s with the same retry path. The string match is
the cheapest reliable discriminator since tweepy doesn't surface a structured
error code for this specific case.

### OAuth 1.0a credentials structure (tweepy)

For posting on a user's behalf you need all four:
- `X_CONSUMER_KEY`, `X_CONSUMER_SECRET` (app-level)
- `X_ACCESS_TOKEN`, `X_ACCESS_TOKEN_SECRET` (user-level)

`tweepy.Client` takes all four directly. For media uploads you currently still
need v1.1 via `tweepy.API(tweepy.OAuth1UserHandler(...))` — v2 doesn't yet
cover media. Two clients, same credentials.

### Failure mode: silent publish failure inside a subprocess wrapper

If your publishing tool is invoked via `subprocess.run(..., capture_output=True)`
from a scheduled scanner, the 403 error stays inside the subprocess's stderr
and never reaches the systemd journal. The scanner sees `returncode != 0` and
sends Telegram, but `journalctl --user -u <service>` looks clean. When
diagnosing "automation stopped posting", check the data file (`published_at`
fields) and Telegram channel, not the journal.

## Proposed Skill Content

A `twitter-api` skill should activate on prompts mentioning Twitter/X posting,
tweepy, thread publishing, duplicate-content errors, or X API integration.

It should teach:

1. **X's 403 "duplicate content" is template-driven, not hash-driven.** Recognize
   the signature: rigid prefix/suffix templates + repeated boilerplate close.
2. **Mitigation pattern:** deterministic variant rotation seeded by a stable
   per-item id. Provide the `_variant_index` helper and a small template-pool
   example.
3. **Never use invisible characters** as a disambiguator. They don't fool X's
   filter and they look weird if a human ever inspects the tweet.
4. **Retry-on-dupe-403 only when nothing has been posted yet.** Partial threads
   are unrecoverable.
5. **tweepy split:** v2 `tweepy.Client` for tweet creation, v1.1 `tweepy.API`
   for media uploads. Same OAuth 1.0a credentials.
6. **Observability gotcha:** subprocess-wrapped publishers swallow stderr into
   `capture_output=True`. For long-running automated posters, surface the
   error class (especially dupe-403) to the parent process / alerting channel.

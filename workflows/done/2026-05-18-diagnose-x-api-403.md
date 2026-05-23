---
title: Diagnose intermittent X (Twitter) API 403 in news-fetcher
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
news-fetcher's POST to X (`client.v2.tweet`) is returning **403 Forbidden**
on most ticks for days, but with intermittent successes — not a hard
permanent break. Today's pattern: success at 14:25 EEST (`tweet_id=2056335408007450779`),
then every 15-min tick since has logged:

```
X ERR: Request failed with code 403
details: {"detail":"You are not permitted to perform this action.",
          "type":"about:blank","title":"Forbidden","status":403}
```

Same intermittent pattern visible going back to 2026-05-14: bursts of
403s during certain hours, isolated successes at others. `status.json`
shows `x.posts_24h: 6` (well under any documented cap).

## Hypotheses (ranked)
1. **X Free-tier write cap** (500 tweets / 30-day rolling window). The
   intermittent success pattern matches a cap that lets a tweet through
   when older tweets fall off the window. `posts_24h=6` doesn't refute
   this — the cap is monthly, not daily.
2. **App permissions toggled to read-only at developer.x.com** — would
   cause permanent 403, not intermittent. **Doesn't fit the pattern**
   unless permissions are flapping (unlikely).
3. **Per-endpoint POST /2/tweets rate limit** (Free tier: 17/24h
   per-app). 6 posts/24h doesn't exceed this, but counts may include
   *attempted* writes, not just successful ones — every 15-min tick
   adds an attempt. ~96 attempts/day far exceeds 17.
4. **Tweet content violates a rule** — would normally give a different
   error code (`duplicate_content`, etc.), so unlikely.

The fact that successes happen at all means **auth and write scope are
fine** — the limit is at the rate/cap layer.

## Steps
- [x] Ran a read-only diagnostic script (`x_diag.mjs`, deleted after)
      that loaded the same creds via `loadEnv()` and called `GET /2/users/me`.
      Result: **OK** — `{"id":"2036482538533953537","name":"ProbBrain","username":"ProbBrain"}`.
      Tokens fully authenticate; the 403 is endpoint-specific (writes).
- [x] User caught the actual bug by asking "it was supposed to post
      every 2h. how come we tryna post every 15 mins?" — turned the
      investigation in the right direction.
- [x] Traced `postToX` in `news-fetcher/news-fetch.js`: the 2h gate
      (`X_MIN_INTERVAL_MS`) only updates `last_digest_at` on **success**.
      Every tick after the first failure re-attempts indefinitely
      because the gate stays frozen at the last successful post's
      timestamp. ~96 attempts/day vs the intended 12 — almost certainly
      what trips X's per-app 403 cap (even though daily-success count
      is only 6/24h).
- [x] Applied one-line fix: in the `catch (e)` block of `postToX`,
      also write `last_digest_at = now` so a failed attempt still
      "burns" the 2h slot. Cost: a transient network error costs one
      2h slot — acceptable for a digest already throttled to once-per-2h.
- [x] Stopped scanner timers (race: scanner modified working tree
      mid-commit, again — same operational issue captured in the prior
      `git-operations` .sc). Stopping timers is now the muscle memory
      step.
- [x] Committed `fix(news-fetch): burn the 2h slot on X failure too`
      (`7b4eb262`); pushed `169daaee..7b4eb262 main -> main`.
- [x] Restarted timers; next news-fetcher tick at 17:10 EEST will
      exercise the fix organically.
- [x] Did NOT need to run `--post-probe` (which would have posted a
      real diagnostic tweet) — the read-only diagnostic + code trace
      was enough to identify the bug.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-18-diagnose-x-api-403.md` before starting any new work.

## Outcome

Completed on 2026-05-18.

**Real bug, not an X-side problem.** The `GET /2/users/me` probe
confirmed tokens are fully valid (user `ProbBrain`, id
`2036482538533953537`); auth and write scope are fine. The 403 cascade
was a self-inflicted DoS: a logic gap in `postToX` meant the 2h cadence
gate only worked on success — every 15-min tick after a failure
re-attempted the same digest, producing ~96 X-API POST attempts/day
instead of the intended 12. X's per-app anti-spam tripped that, and
returned 403 on most attempts regardless of daily success count being
low (6/24h, way under any documented limit).

One-line fix in `news-fetch.js:614-617`: on failure, also write
`last_digest_at = now` so the 2h gate is enforced whether the post
succeeded or not. Commit `7b4eb262`, pushed.

**The user found the bug, not me.** I had ranked "X Free-tier monthly
cap" as the most likely cause; the user's "but it's supposed to be
every 2h" cut through that and pointed straight at the cadence. Worth
remembering: when an API returns a cap-style error but the local
attempt rate looks suspicious, check the attempt cadence *before*
assuming the upstream provider is at fault.

**Recurring lesson** (third time today): scanner systemd timers
modify the working tree mid-git-operation. The `git-operations` .sc
written earlier this session captured this pattern, but I still hit
it again before stopping the timers. The muscle memory now is "before
any commit/rebase/push in probbrain-accuracy, stop both timers; do
the work; restart." Worth a process check the next time — maybe a
wrapper script that does stop → operation → start automatically.

**Validation deferred to organic tick**: next news-fetcher run at
17:10 EEST. Expected behavior: if there's no successful post since
~15:10 EEST (=2h ago), it'll attempt one POST and either succeed
(unblocked) or fail-then-burn-the-slot (no longer hammering). Either
way, journal log should now show at most one X attempt per ~2h
instead of one per 15-min.

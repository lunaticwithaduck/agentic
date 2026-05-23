---
title: Re-auth bcgame-blogger and stop the 7-day Testing-mode token churn
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
`bcgame-blogger` (and `ProbBrain`'s `publish_blogger_signal.py`, which shares the
same config) is failing OAuth refresh again:

```
google.auth.exceptions.RefreshError: invalid_grant: Token has been expired or revoked.
```

Confirmed at 2026-05-18 by refreshing the token from
`/home/jojo/bcgame-blogger/config.json` → `google.refresh_token`. `mtime` on
that file is 2026-05-01, so despite the helper-fixing work on 2026-05-11
(`done/2026-05-11-blogger-oauth-reauth-helper.md`, `…-fix-redirect-uri.md`,
`…-port-8765.md`), the actual re-auth flow apparently never ran to completion —
the token in the file is still the dead 2026-05-01 one.

Root cause for the **recurring** death (today + every ~7d): the OAuth client
`gdo.apps.googleusercontent.com` is still in Google Cloud Console "Testing"
publishing-status, where refresh tokens for external accounts expire 7 days
after issuance, unconditionally. Moving it to "In production" stops the churn.

## Steps
- [x] User registered `http://localhost:8080/` in Cloud Console (port 8080 ended
      up being the working port for this round, not the 8765 the canonical
      helper was pinned to).
- [x] Ran a one-shot re-auth helper at `/tmp/blogger_reauth_8080.py` that
      generated the consent URL, wrote it to `/tmp/blogger_consent_url.txt`
      (because the user's terminal couldn't auto-launch a browser nor cleanly
      select wrapped text), listened on `127.0.0.1:8080` for the callback, and
      atomically wrote the new `refresh_token` to
      `/home/jojo/bcgame-blogger/config.json`.
- [x] Verified `config.json` got a new `refresh_token` (mtime now
      `2026-05-18 16:03:51`, tail `PDIvr5eQ`), `creds.refresh()` succeeds, and
      `service.blogs().listByUser(userId='self')` lists the target blog
      `5262487602084435431` (Crypto Bonus Hub).
- [x] Smoke-tested end-to-end by triggering `systemctl --user start
      blogger-daily.service` — published the live post
      `Is Polymarket Legal 2026? Complete Guide` at 2026-05-18T06:09:33-07:00
      (`coinbonuses.blogspot.com/2026/05/is-polymarket-legal-2026-complete-guide.html`).
- [x] **Permanent fix**: user confirmed they moved the OAuth client from
      "Testing" to "In production" in Cloud Console — future refresh tokens
      no longer expire after 7 days.
- [x] Deleted `/home/jojo/blogger_auth.py` (stale port-8080 helper that did
      direct (non-atomic) write and skipped the blog sanity check). The
      canonical helper at `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py`
      remains in place but is now pinned to port **8765** while Cloud Console
      has **8080** registered — divergence noted but not resolved this round
      (user declined to realign; low priority since "In production" status
      should keep tokens alive for months).
- [x] Confirmed ProbBrain's `tools/publish_blogger_signal.py` reads from the
      same `/home/jojo/bcgame-blogger/config.json` (`BLOGGER_CONFIG = Path(...)`
      at line 41, `cfg["google"]["refresh_token"]` at line 78) — the new
      refresh token automatically fixes the ProbBrain signal publishing path
      too, no separate re-auth needed.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-18-fix-blogger-oauth-expired-again.md` before starting any new work.

## Outcome

Completed on 2026-05-18.

OAuth re-auth restored, both blogger pipelines unblocked, and the 7-day
Testing-mode token-expiry churn is now structurally fixed.

**What actually unblocked the flow** (and was the new wrinkle relative to the
2026-05-11 round): Python's `webbrowser` module couldn't auto-open a browser
from the user's terminal session, and the terminal also couldn't cleanly
select the long printed consent URL. Both attempts at the canonical helper
stalled here — that's why `config.json` had been untouched since 2026-05-01
despite three prior task rounds. Workaround: split `InstalledAppFlow` into
its constituent parts and dump the `flow.authorization_url()` to a file
(`/tmp/blogger_consent_url.txt`) the user could open in any editor and
copy from cleanly, then run a custom `http.server.HTTPServer` on the
loopback port to catch the callback. This pattern is the right escape hatch
for any OAuth flow when the consent URL can't reach a browser via stdout.

Live verification: a fresh post (`Is Polymarket Legal 2026? Complete Guide`)
appeared on `coinbonuses.blogspot.com` 2 minutes after triggering
`blogger-daily.service`, confirming the full
config → auth → Blogger API → publish path is healthy. Same config is shared
with ProbBrain's signal publisher, so that pipeline is auto-fixed too.

**Loose ends not addressed** (deliberately, user declined):
- Canonical helper at `Documents/ProbBrain/tools/reauth_blogger.py` is still
  pinned to port 8765 while Cloud Console now has 8080 registered. Not a
  problem today (auth is "In production", tokens persist), but the next time
  re-auth is needed someone will hit `redirect_uri_mismatch` again unless
  one side gets realigned.
- The stale `python3 -m http.server 8765 --bind 127.0.0.1` (pid 2111681,
  5-day-old, serves `probbrain-accuracy/`) still holds port 8765. Same
  process pattern recurred from the 2026-05-11 round — something is
  re-launching this preview server periodically.

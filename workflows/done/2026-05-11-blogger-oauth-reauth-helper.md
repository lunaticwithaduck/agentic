---
title: Add a one-shot Blogger OAuth re-auth helper script
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
`tools/publish_blogger_signal.py` is failing with
`google.auth.exceptions.RefreshError: invalid_grant: Token has been expired or revoked.`
on every run since the refresh_token at
`/home/jojo/bcgame-blogger/config.json` → `google.refresh_token` got revoked by
Google. As a result, no live ProbBrain signal since the revocation has its
Blogger landing page published.

There is currently no helper script to obtain a new refresh_token — the file
was apparently populated manually at some point in the past. The user has hit
this "auth error on blogger again" multiple times, which strongly hints the
OAuth client is still in "Testing" mode in Google Cloud Console (Testing-mode
refresh tokens expire after 7 days for non-Google-internal accounts).

Fix scope:
- Add a one-shot helper `tools/reauth_blogger.py` that runs the Google
  InstalledAppFlow against the existing client_id/client_secret and writes the
  resulting refresh_token back into `bcgame-blogger/config.json` atomically,
  preserving all other fields.
- Do NOT auto-run it — it requires the user to click through the consent
  screen in a browser. Just leave it ready, tell the user the one command to
  run, and flag the Testing-mode root-cause hypothesis so they can fix it
  permanently in Google Cloud Console.

## Steps
- [x] Write `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py` using
      `google_auth_oauthlib.flow.InstalledAppFlow.from_client_config(...)`
      with scope `https://www.googleapis.com/auth/blogger`. Use a local-server
      flow so the browser callback is automatic.
- [x] Helper must read `client_id` / `client_secret` from
      `/home/jojo/bcgame-blogger/config.json`, never hard-code them.
- [x] On success, write the new refresh_token back to that config via a
      `.tmp` + `replace()` atomic swap so a partial write can't corrupt the
      file. Keep all other top-level keys intact.
- [x] Verify the new credentials by calling `service.blogs().listByUser(userId='self')`
      once and printing the available blog IDs (so the user can confirm the
      `blog_id` they expect is reachable).
- [x] Compile-check.
- [x] Tell the user the one command to run and flag the Testing-mode
      hypothesis as the likely root cause of repeated revocations.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-blogger-oauth-reauth-helper.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

Confirmed the Blogger publish failure firsthand by invoking
`tools/publish_blogger_signal.py --signal-id SIG-115`:

```
google.auth.exceptions.RefreshError: invalid_grant: Token has been expired or revoked.
```

The refresh_token at `/home/jojo/bcgame-blogger/config.json` → `google.refresh_token`
is dead. Nothing else in the OAuth config is wrong — `client_id`, `client_secret`,
and the venv's `google_auth_oauthlib` import all check out.

Wrote `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py` (new file, not
modifying any existing publishing code). What it does:

1. Loads `client_id` / `client_secret` from the existing config (never hardcoded).
2. Builds a synthetic `installed`-app client_config dict and hands it to
   `InstalledAppFlow.from_client_config` with scope
   `https://www.googleapis.com/auth/blogger`.
3. Runs `flow.run_local_server(port=0, access_type="offline", prompt="consent")`.
   The `prompt="consent"` is critical — without it, Google reuses the existing
   grant and returns no refresh_token at all on the JSON response, so re-auth
   silently produces no usable credential.
4. Atomic write-back: dumps the updated config to `config.json.tmp`, then
   `tmp.replace(CONFIG_PATH)` so a crash mid-write can't corrupt the file.
   All other top-level keys in config.json are preserved.
5. Sanity-check: calls `service.blogs().listByUser(userId="self")` and prints
   `id`, `name`, and `url` for every blog the new credentials can reach. Lets
   the user verify the right blog is in the list before re-running the
   publisher.

The helper isn't called from anywhere automatically — running it requires a
human at a browser to grant consent. Surface area is one new file; no
existing code paths touched. `python -m py_compile` passes; all required
imports (`google_auth_oauthlib.flow`, `google.oauth2.credentials`,
`googleapiclient.discovery`) resolve in the ProbBrain venv.

**Likely root cause of the recurring revocations** (flagged to the user, not
fixed by this task): the OAuth client at
`gdo.apps.googleusercontent.com` is probably still in **Testing**
publishing-status in Google Cloud Console. Testing-mode refresh tokens for
external accounts expire after 7 days unconditionally. The fix is to move
the OAuth client to "In production" in the Google Cloud Console OAuth consent
screen settings. After that, refresh tokens stay valid until explicit
revocation / 6 months of inactivity.

**To resume Blogger publishing**, the user needs to run (once):

```
/home/jojo/Documents/ProbBrain/venv/bin/python \
    /home/jojo/Documents/ProbBrain/tools/reauth_blogger.py
```

This opens a browser, asks for Google consent on the same account that owns
the target blog, writes the new refresh_token, and prints the reachable
blog list. After that, the scanner's next 2h tick (next at 20:17 EEST) will
silently re-attempt blog publishing on any new live signal.

---
title: Fix redirect_uri_mismatch in reauth_blogger.py
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
The user ran `tools/reauth_blogger.py` and Google's consent screen rejected
with `Error 400: redirect_uri_mismatch`. This means the OAuth client at
`gdo.apps.googleusercontent.com` is a **Web application** type — for which
Google strictly enforces the registered redirect URI list. The script uses
`run_local_server(port=0)`, which generates `http://localhost:<random_port>/`
each run — that will never match a registered URI.

Two reasonable fixes:

A) Pin the script to a fixed loopback port (e.g. 8765) and tell the user to
   add `http://localhost:8765/` to the OAuth client's "Authorized redirect
   URIs" in the Google Cloud Console (one-time action). Keeps the existing
   client_id / client_secret intact — important because
   `/home/jojo/bcgame-blogger/blogger_bot.py` also reads from the same config.

B) Create a new "Desktop app" OAuth client in Cloud Console. Desktop-app
   clients support loopback redirect on any port without registration. Would
   require swapping client_id/client_secret in config.json AND updating
   blogger_bot.py to pick them up — broader blast radius.

Going with (A) — minimal surface area, no impact on the other Google API
consumer in the user's setup.

## Steps
- [x] Pin `run_local_server(...)` in `tools/reauth_blogger.py` to a fixed
      port (8765). Print the exact redirect URI the user needs to register
      at the start so it's obvious what to paste into Cloud Console.
- [x] On failure, the existing error path should already surface
      `redirect_uri_mismatch` clearly — no extra catching needed, but make
      the pre-flight message unmistakable so the user knows the prerequisite.
- [x] Compile-check.
- [x] Tell the user the exact Cloud Console steps to add the redirect URI.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-reauth-blogger-fix-redirect-uri.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

Updated `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py`:

- Added `OAUTH_LOOPBACK_PORT = 8766` and `EXPECTED_REDIRECT_URI = f"http://localhost:{OAUTH_LOOPBACK_PORT}/"`
  at module top. (Picked 8766 instead of the originally-planned 8765 because
  the user already has a long-running `python3 -m http.server 8765` static
  file server bound to that port — a 9-day-old pid `714064` unrelated to
  this work. Don't kill it; just used the next free port.)
- Changed `flow.run_local_server(port=0, ...)` to
  `flow.run_local_server(port=OAUTH_LOOPBACK_PORT, ...)` so the redirect URI
  sent to Google is stable and matchable.
- Added a pre-flight `print(...)` at the top of `main()` that prints the
  exact redirect URI and explicit Cloud Console steps to register it. This
  makes the failure mode self-documenting if the user runs the script before
  registering the URI.
- Plumbed `EXPECTED_REDIRECT_URI` through the synthetic `client_config` dict
  so the `installed.redirect_uris` list matches what `run_local_server` will
  actually use.

Verified port 8766 is free with `ss -tln`. `python -m py_compile` passes.

**Why the original `port=0` choice failed:** Google's OAuth has two relevant
client types in Cloud Console. "Desktop app" clients have built-in loopback
support — any `http://localhost:<port>/` works without registration.
"Web application" clients enforce an **exact** redirect URI match against the
registered list, including the port and trailing slash. The user's existing
OAuth client is the Web-application type (confirmed by the
`redirect_uri_mismatch` error — Desktop-app clients give a different error
code on similar mistakes). With a Web-application client, OS-picked random
ports can never be pre-registered, so the script must pin a port and the
user must list it.

**To resume the re-auth flow, the user needs to** (one-time setup):

1. Open Google Cloud Console → APIs & Services → Credentials.
2. Find the OAuth 2.0 Client ID ending in `gdo.apps.googleusercontent.com`.
3. Under "Authorized redirect URIs", click "Add URI" and paste:
   `http://localhost:8766/`
   (note the trailing slash — Google's match is byte-exact)
4. Save. Wait ~30s for propagation.
5. Rerun `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py`.

The "Testing mode" hypothesis from the earlier task is still relevant for
the recurring revocations — registering the redirect URI fixes today's
error, but if the OAuth client stays in Testing mode the new refresh token
will expire again in ~7 days.

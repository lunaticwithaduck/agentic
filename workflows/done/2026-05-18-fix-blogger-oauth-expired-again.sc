---
domain: google-oauth
source_task: 2026-05-18-fix-blogger-oauth-expired-again.md
date: 2026-05-18
keywords: ["google oauth", "refresh token", "invalid_grant", "InstalledAppFlow", "headless", "no browser", "authorization_url", "consent url"]
---

## Extracted Knowledge

### Escape hatch: when Python's `webbrowser` can't launch and the terminal can't cleanly select text

`InstalledAppFlow.run_local_server(open_browser=True)` calls Python's
`webbrowser` module, which calls `xdg-open` (or platform equivalent). Two
common failures, both silent enough to mistake for a successful start:

1. `xdg-open` can't find a default browser in the current `$DISPLAY` /
   session (common when the script was launched from a TTY, an SSH session
   without X forwarding, or a sandboxed agent shell).
2. The browser opens but the user can't select the auth URL `run_local_server`
   prints to stdout — terminal wraps it across lines, or the terminal
   emulator doesn't support text selection at all (e.g. an in-IDE pty).

When this happens, **don't keep retrying `run_local_server`**. Split the flow
into its constituent parts and route the URL through the filesystem:

```python
from google_auth_oauthlib.flow import Flow
from http.server import BaseHTTPRequestHandler, HTTPServer
import urllib.parse, json
from pathlib import Path

flow = Flow.from_client_config(client_config, SCOPES, redirect_uri=REDIRECT_URI)
auth_url, state = flow.authorization_url(
    access_type="offline",
    prompt="consent",
    include_granted_scopes="false",
)
Path("/tmp/consent_url.txt").write_text(auth_url + "\n")
# user opens the file in any editor (vim, less, code), selects, pastes into browser

received = {}
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a, **kw): pass  # silence
    def do_GET(self):
        params = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        if "code" in params:
            received["code"] = params["code"][0]
            self.send_response(200); self.end_headers()
            self.wfile.write(b"OK, close this tab.\n")

httpd = HTTPServer(("127.0.0.1", PORT), Handler)
while "code" not in received:
    httpd.handle_request()

flow.fetch_token(code=received["code"])
creds = flow.credentials  # has refresh_token if prompt="consent" was set
```

Note the client_config wrapper key here is `"web"` (not `"installed"`) when
you're driving the flow manually with `Flow.from_client_config(...)` against
a Web-application OAuth client — `Flow` doesn't auto-detect.

### Debugging recurring OAuth failures: check config `mtime` before re-running the helper

If "the auth helper has been fixed in prior task rounds but the credentials
keep failing," the most common cause is that **the helper never actually ran
to completion** — the consent flow stalled (no browser, redirect_uri_mismatch
in Cloud Console, port collision, user closed the browser tab too early) so
the atomic write-back at the end never executed.

First diagnostic, before any code changes:

```bash
stat -c '%y %n' /path/to/config.json
```

If the mtime predates the last round of helper work, the prior "fix" never
landed a new refresh_token — the actual issue is whatever stalled the consent
flow, not the helper code. Don't keep refining the helper; figure out where
the user is stuck in the browser step.

### Shared config: one re-auth can fix multiple consumers

When several projects share the same OAuth client + refresh_token file (a
common pattern when one Google account owns Blogger/Gmail/Drive resources
accessed by multiple bots), a single successful re-auth fixes all of them
silently — no need to run the helper N times. Verify by grepping for the
config path across the repo set:

```bash
grep -rl 'bcgame-blogger/config.json\|google.refresh_token' \
     /path/to/project1 /path/to/project2
```

Document the shared-config dependency in the Outcome — future you will not
remember which side "owns" the credentials when one of them breaks again.

### Production-mode OAuth client tokens persist; Testing-mode ones don't

Confirmed empirically this round: an OAuth client in "Testing" publishing
status (Cloud Console → APIs & Services → OAuth consent screen) issues
refresh tokens that die after exactly 7 days regardless of usage. The user
moved the client to "In production" between the 2026-05-11 round and now,
and the new refresh_token issued 2026-05-18 should persist for months
instead of dying next week. The 2026-05-11 `.sc` flagged this as the likely
root cause; this round confirms the diagnosis and the fix.

### The canonical helper drifts from Cloud Console state — pick one port and align

This project ended up with two re-auth helpers using different ports (8080
vs 8765), and the Cloud Console's Authorized redirect URI list now contains
the port that the *non-canonical* helper used (8080), not the canonical one
(8765). Practical impact: future re-auth will fail with
`redirect_uri_mismatch` if anyone runs the canonical helper without first
re-adding 8765 to Cloud Console (or realigning the helper to 8080).

When you fix an OAuth helper, also update Cloud Console *or* update the
helper to match what's already in Cloud Console — divergence between the
two is invisible until the next time auth dies, at which point you have to
re-debug it from scratch. Don't leave two helpers in the tree using
different ports.

## Failure Modes Observed

The two existing `.sc` files in this domain assume `run_local_server` will
just work — the 2026-05-11 `redirect-uri.sc` file explicitly recommends
`flow.run_local_server(port=OAUTH_LOOPBACK_PORT, ..., open_browser=True)`.
That recommendation is correct when a browser is reachable, but it gives
no fallback for the headless-ish case (no `$DISPLAY`, sandboxed shell,
unselectable terminal output). The cycle of helper rewrites on 2026-05-11
through 2026-05-12 ground to a halt at exactly this point — the helper was
correct, but the user couldn't get from the printed URL to a browser.

The escape-hatch pattern above (write URL to file + custom HTTPServer) is
what the skill should teach as the default when "the helper isn't broken
but the consent step isn't completing." It's also strictly more flexible:
you can run the manual-Flow version in any environment, including ones
where Python can't launch a browser at all.

## Proposed Skill Content

In addition to what the two prior `.sc` files propose, the skill should add:

1. **Mtime-first diagnosis**: when an OAuth helper "fixed it" in a past task
   round but the same `invalid_grant` keeps recurring, check the mtime of
   the credentials file before touching code. If the mtime predates the
   prior fix, the helper never wrote — the bug is in the human-in-the-loop
   step (browser, Cloud Console, port), not the helper.
2. **Headless re-auth pattern**: when `webbrowser`/`xdg-open` can't launch
   or the user can't select the printed URL, switch from
   `InstalledAppFlow.run_local_server(...)` to manual
   `Flow.authorization_url()` + custom `http.server.HTTPServer` for the
   callback. Dump the consent URL to a file (`/tmp/consent_url.txt`) so
   the user can open it in any editor and copy cleanly.
3. **`Flow` vs `InstalledAppFlow` client_config wrapper**: when you call
   `Flow.from_client_config(...)` directly (not `InstalledAppFlow`), use
   `"web"` as the wrapper key, not `"installed"`. The wrapper key must
   match the application type of the underlying OAuth client.
4. **Shared-config consumers**: explicitly grep for and document every
   project that reads from the same OAuth config file before declaring a
   re-auth "done" — one fix often closes multiple unrelated tickets, and
   skipping the verification leaves consumers silently still-broken until
   their next scheduled run.
5. **Helper-vs-Cloud-Console drift**: treat the port in `Authorized
   redirect URIs` and the port in the helper script as a single setting
   distributed across two places. Whenever either changes, update both in
   the same session.

---
name: google-oauth
description: Debug and operate Google OAuth 2.0 refresh-token flows for Blogger/Gmail/Drive/Calendar APIs — diagnose invalid_grant, redirect_uri_mismatch, and headless re-auth
activation:
  keywords: ["google oauth", "refresh token", "invalid_grant", "redirect_uri_mismatch", "InstalledAppFlow", "google-auth-oauthlib", "blogger api", "gmail api", "google drive api", "oauth consent screen"]
  file_patterns: ["**/oauth*", "**/reauth*", "**/blogger*", "**/google_auth*", "**/credentials*.json"]
---

# Google OAuth 2.0

## Purpose
Debug Google OAuth refresh-token failures and run interactive re-auth flows
for personal-tier integrations with Google APIs (Blogger, Gmail, Drive,
Calendar). Covers the recurring `invalid_grant` and `redirect_uri_mismatch`
gotchas, the headless re-auth escape hatch, and the operational hygiene
that keeps single-account bots from breaking weekly.

## Instructions

### 1. Diagnose `invalid_grant: Token has been expired or revoked`

This is Google's catch-all error string. It fires for all of:

1. User explicitly revoked the app at `myaccount.google.com/permissions`
2. User changed their Google account password
3. Refresh token sat unused for 6 months
4. **OAuth client is in "Testing" publishing-status in Cloud Console → token
   auto-expires after 7 days for any non-Google-internal account**
5. User accumulated 50+ refresh tokens for the same client_id/account
   (oldest gets evicted)
6. Google Workspace admin removed the grant

If the same client+account hits this repeatedly (every week or two with no
user action), case (4) is the most likely root cause. The durable fix:
Cloud Console → APIs & Services → OAuth consent screen → "Publish app".
Production-mode refresh tokens persist until explicit revocation or 6
months idle.

### 2. Before refining the helper, check the config file's mtime

When prior task rounds have "fixed" the re-auth helper but `invalid_grant`
keeps recurring, the most likely cause is that **the helper never actually
ran to completion** — the consent flow stalled (no browser, redirect_uri
mismatch, port collision, user closed the tab early), so the atomic
write-back at the end never executed.

First diagnostic, before touching any code:

```bash
stat -c '%y %n' /path/to/credentials_or_config.json
```

If the mtime predates the prior helper-fix work, the bug is in the
human-in-the-loop step (browser, Cloud Console, port), not the helper
code. Stop refining the helper and figure out where the user is stuck.

### 3. Diagnose `Error 400: redirect_uri_mismatch` by checking client type

This error fires when the redirect URI your code sends does not exactly
match the OAuth client's "Authorized redirect URIs" list. Google does not
tell you what URI it received — deduce it from your flow type and port.

| Client type | Loopback behavior |
|---|---|
| Desktop app | Any `http://localhost:<port>/` accepted without pre-registration |
| Web application | Byte-exact match against registered URIs only |

Check the type explicitly: Cloud Console → APIs & Services → Credentials →
click the OAuth client → "Application type" at the top. Web application is
the default Cloud Console creates and is what most existing clients are.

**For Web-application clients**, pin a fixed port and register that exact URI
(trailing slash mandatory, byte-exact, no normalization):

```python
OAUTH_LOOPBACK_PORT = 8766
EXPECTED_REDIRECT_URI = f"http://localhost:{OAUTH_LOOPBACK_PORT}/"

client_config = {
    "installed": {
        "client_id": client_id,
        "client_secret": client_secret,
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "redirect_uris": [EXPECTED_REDIRECT_URI],
    }
}
flow = InstalledAppFlow.from_client_config(client_config, SCOPES)
creds = flow.run_local_server(port=OAUTH_LOOPBACK_PORT, ...)
```

In Cloud Console: Credentials → OAuth client → Authorized redirect URIs →
Add URI → paste `http://localhost:8766/` (include trailing slash) → Save.
Wait ~30s for propagation.

**For Desktop-app clients**, use `port=0` and a bare `http://localhost`
redirect URI. The loopback flow handles the rest.

**Pre-flight the chosen port** with `ss -tln | grep ":<port> "`. Other
long-running local servers (static-file servers, dev daemons) frequently
grab common ports. Prefer 8766 / 8090 / 9876 over 8080 / 3000 / 5000 /
8000 / 8888 to reduce collision risk.

### 4. Force a real refresh_token on re-auth

Google only issues a `refresh_token` on the *first* consent grant for a
given (client_id, account, scope-set). Subsequent grants silently return
only an `access_token` — the downstream code then dead-ends after the
1-hour expiry with no recoverable error. Always pass both:

```python
creds = flow.run_local_server(
    port=OAUTH_LOOPBACK_PORT,
    access_type="offline",   # ask for offline access (required for refresh_token)
    prompt="consent",        # force consent screen to re-render
    open_browser=True,
)
assert creds.refresh_token, "Google did not return refresh_token — revoke at myaccount.google.com/permissions and rerun"
```

### 5. Headless re-auth: `webbrowser` can't launch / terminal can't select

When `InstalledAppFlow.run_local_server` stalls because (a) Python's
`webbrowser` module can't find a browser in the current session, or
(b) the auth URL it prints to stdout wraps across lines and the terminal
doesn't support clean text selection — switch to the manual-`Flow`
pattern and route the URL through the filesystem:

```python
from google_auth_oauthlib.flow import Flow
from http.server import BaseHTTPRequestHandler, HTTPServer
import urllib.parse
from pathlib import Path

flow = Flow.from_client_config(client_config, SCOPES, redirect_uri=REDIRECT_URI)
auth_url, _state = flow.authorization_url(
    access_type="offline",
    prompt="consent",
    include_granted_scopes="false",
)
Path("/tmp/consent_url.txt").write_text(auth_url + "\n")
# user opens the file in any editor (vim, less, code), selects, pastes into browser

received = {}
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a, **kw): pass  # silence default request log
    def do_GET(self):
        params = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        if "code" in params:
            received["code"] = params["code"][0]
            self.send_response(200); self.end_headers()
            self.wfile.write(b"OK, close this tab.\n")
        elif "error" in params:
            received["error"] = params["error"][0]
            self.send_response(400); self.end_headers()

httpd = HTTPServer(("127.0.0.1", PORT), Handler)
while not received:
    httpd.handle_request()

if "error" in received:
    raise SystemExit(f"OAuth error: {received['error']}")
flow.fetch_token(code=received["code"])
creds = flow.credentials
```

Note the client_config wrapper key when using `Flow.from_client_config`
directly is `"web"` (not `"installed"`) for Web-application clients —
`Flow` does not auto-detect like `InstalledAppFlow` does.

### 6. `client_config` wrapper key must match the application type

Both `InstalledAppFlow.from_client_config(...)` and `Flow.from_client_config(...)`
expect the same nested-dict shape as Google's downloaded credentials JSON:

```python
client_config = {
    "installed": {  # for Desktop / installed apps
        "client_id": ...,
        "client_secret": ...,
        ...
    }
}
# OR
client_config = {
    "web": {  # for Web application clients
        ...
    }
}
```

The wrapper key must match the actual OAuth client's "Application type" in
Cloud Console. Pass a flat `{client_id, client_secret, ...}` and the call
raises `ValueError: Client secrets must be for a web or installed app.`.

### 7. Atomic config write-back

Refresh-token rotation writes the new value into a config file that the
rest of the system reads on every run. A crash mid-write corrupts that
file and breaks all consumers. Always:

```python
tmp = CONFIG_PATH.with_suffix(CONFIG_PATH.suffix + ".tmp")
tmp.write_text(json.dumps(cfg, indent=2))
tmp.replace(CONFIG_PATH)  # atomic on POSIX
```

`Path.replace()` is atomic on POSIX file systems — readers see either the
old config or the new config, never a half-written one.

### 8. Sanity-check the new credentials before declaring success

After a successful re-auth, immediately call a cheap API method to confirm
the new token actually works and the account has access to what you
expect — earlier failure signal than waiting for the next scheduled run:

```python
service = build("blogger", "v3", credentials=fresh)
blogs = service.blogs().listByUser(userId="self").execute()
for b in blogs.get("items", []):
    print(b["id"], b["name"], b["url"])
```

For Blogger, `listByUser('self')` lists all reachable blogs. For Gmail,
`service.users().getProfile(userId='me').execute()`. For Drive,
`service.about().get(fields='user').execute()`.

### 9. Document shared-config consumers

When several projects share the same OAuth client + refresh_token file
(a common pattern when one Google account owns resources accessed by
multiple bots), one successful re-auth silently fixes all of them — no
need to re-run helpers N times. Always verify the consumer list and
document it:

```bash
grep -rl 'path/to/shared/config.json\|google.refresh_token' \
     /path/to/project1 /path/to/project2
```

Document the shared-config dependency in the task Outcome — future you
will not remember which project "owns" the credentials when one of them
breaks again.

### 10. Helper-vs-Cloud-Console drift is invisible until next re-auth

The port in `Authorized redirect URIs` and the port in the helper script
are a single setting distributed across two places. If they diverge, the
next re-auth will fail with `redirect_uri_mismatch` and someone has to
re-debug from scratch. When you fix one side, update the other in the
same session. Don't leave two helpers in the tree using different ports.

**Implementation pattern** that makes drift cheap to fix:

```python
OAUTH_LOOPBACK_PORT = 8080  # Confirmed registered as of YYYY-MM-DD (re-auth round)
EXPECTED_REDIRECT_URI = f"http://localhost:{OAUTH_LOOPBACK_PORT}/"

client_config = {"installed": {..., "redirect_uris": [EXPECTED_REDIRECT_URI]}}
creds = flow.run_local_server(port=OAUTH_LOOPBACK_PORT, ...)
```

Single named constant, downstream f-string interpolation everywhere.
Changing the pinned port is then a one-line edit. After the edit,
**always** verify with `grep -n <old_port> <helper>` — if the grep
returns matches, you missed a hardcoded copy (commonly in the comment
block above the constant, or in docstrings/error messages that string-
interpolate the port number).

**Decision rule** when realigning between two candidate ports: pick the
one with empirical evidence of registration (a successful re-auth this
round), not the one that "should be" registered from a prior task's
notes. Then **date-stamp the comment** ("Confirmed registered as of
YYYY-MM-DD (re-auth round)") so the next maintainer can distinguish
observed truth from prior intention without re-opening Cloud Console.

## Failure Modes

- **`run_local_server(port=0)` on a Web-application client** — produces a
  different ephemeral port every run; no registered URI list can match.
  Use Desktop-app clients with bare `http://localhost`, or pin a fixed
  port and register that exact URI.
- **Bare `http://localhost` registered for a Web-app client** — only works
  for Desktop-app loopback; on Web-app clients Google does byte-exact
  match including port. Wildcarding does not happen.
- **Missing `prompt="consent"`** on re-auth — `creds.refresh_token` comes
  back `None`, the access token works for an hour, then the next refresh
  silently fails. Always `assert creds.refresh_token` after the flow.
- **Token mtime predates the "fix"** — multiple rounds of helper rewrites
  did not actually re-issue the token; the consent step never completed.
  Debug the browser/Cloud-Console step, not the helper.
- **OAuth client stuck in "Testing"** — refresh tokens die after 7 days
  for external accounts unconditionally. The recurring weekly auth
  failures are this, not a code bug. Move to "In production".

## Output Format

When fixing a Google OAuth issue, provide:

1. Diagnosis: which of the failure-mode categories above matched (with
   evidence — config mtime, error string, Cloud Console screenshot or
   description).
2. The exact re-auth command(s) the user needs to run, including
   environment (`./venv/bin/python …`).
3. Cloud Console steps if registration is required, written with byte-exact
   URIs (trailing slash, port).
4. Verification: a cheap sanity-check API call output (blog list, user
   email, file count) confirming the new credentials actually work.
5. Permanent-fix recommendation if the failure is structural (e.g. Testing
   mode → Publish app).
6. List of all consumers that share the same config so the user knows the
   blast radius of the re-auth.

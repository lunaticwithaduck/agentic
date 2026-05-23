---
domain: google-oauth
source_task: 2026-05-11-blogger-oauth-reauth-helper.md
date: 2026-05-11
keywords: ["google oauth", "refresh token", "invalid_grant", "InstalledAppFlow", "blogger api", "testing mode", "prompt=consent"]
---

## Extracted Knowledge

### `invalid_grant: Token has been expired or revoked` is Google's catch-all

When `google.oauth2.credentials.Credentials.refresh(Request())` raises:

```
google.auth.exceptions.RefreshError: ('invalid_grant: Token has been expired or revoked.',
    {'error': 'invalid_grant', 'error_description': 'Token has been expired or revoked.'})
```

…the error message gives you no diagnostic. The same string fires for ALL of:

1. User explicitly revoked the app at https://myaccount.google.com/permissions
2. User changed their Google account password
3. Refresh token sat unused for 6 months
4. **OAuth client is in "Testing" publishing-status in Google Cloud Console
   → token auto-expires after 7 days for any non-Google-internal account**
5. User accumulated 50+ refresh tokens for the same client_id/account
   (oldest gets evicted)
6. Google Workspace admin removed the grant

If the same client+account hits this *repeatedly* (every week or two with
no user action), case (4) is by far the most likely root cause. The
fix is in the OAuth consent screen settings in Google Cloud Console:
move the client from "Testing" to "In production". Production-mode
refresh tokens persist until explicit revocation or 6 months idle.

### `prompt="consent"` is required to actually get a refresh_token

Google's default OAuth flow only issues a `refresh_token` on the *first*
consent grant. If the user has previously granted access (even to a different
scope), subsequent calls return only an `access_token` and silently omit the
refresh_token. The downstream code then receives credentials that work for an
hour and then dead-end with no recoverable error.

To force a fresh refresh_token, both of these are required:

```python
creds = flow.run_local_server(
    port=0,
    access_type="offline",  # ask for offline access (gets refresh_token at all)
    prompt="consent",       # force the consent screen to re-render
    open_browser=True,
)
```

After the flow returns, always check `creds.refresh_token is not None` and
fail loudly if it's empty — that means Google silently reused the existing
grant and the new refresh_token never came back. The remediation is for the
user to manually revoke at myaccount.google.com/permissions and rerun.

### `InstalledAppFlow.from_client_config` expects a wrapped dict

The function does NOT accept a flat `{client_id, client_secret, ...}` object.
It wants the same shape Google's downloaded JSON client uses:

```python
client_config = {
    "installed": {                       # <-- this wrapper is mandatory
        "client_id": client_id,
        "client_secret": client_secret,
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "redirect_uris": ["http://localhost"],
    }
}
flow = InstalledAppFlow.from_client_config(client_config, SCOPES)
```

For web-server flows, the wrapper key is `"web"` instead of `"installed"`.

### `port=0` for `run_local_server` avoids collisions

`run_local_server(port=0, ...)` lets the OS pick a free ephemeral port and
plumbs the chosen port into the redirect URI sent to Google automatically.
Hard-coding a port (e.g. 8080) will fail intermittently if anything else on
the machine is bound to that port. The user's existing redirect URI list in
the Cloud Console must include `http://localhost` (no port) — Google treats
that as a wildcard for any localhost port.

### Atomic config writeback pattern

Refresh-token rotation writes the new value back to a config file the rest
of the system reads on every run. A crash mid-write corrupts that file and
breaks all subsequent publishes. Always:

```python
tmp = CONFIG_PATH.with_suffix(CONFIG_PATH.suffix + ".tmp")
tmp.write_text(json.dumps(cfg, indent=2))
tmp.replace(CONFIG_PATH)  # atomic on POSIX
```

`Path.replace()` is atomic on POSIX file systems — readers see either the
old config or the new config, never a half-written one.

### Sanity-check the new credentials before declaring success

After a successful re-auth, call a cheap API method to confirm the new token
actually works and the account has access to what you expect:

```python
service = build("blogger", "v3", credentials=fresh)
blogs = service.blogs().listByUser(userId="self").execute()
```

For Blogger, `listByUser('self')` returns every blog the account can reach.
The user can eyeball the response and confirm the `blog_id` they care about
is in the list — a much earlier failure signal than waiting for the next
scheduled publish.

## Proposed Skill Content

A `google-oauth` skill should activate on prompts mentioning Google OAuth,
refresh tokens, invalid_grant, InstalledAppFlow, Blogger/Gmail/Drive/Calendar
API auth, Google Cloud Console OAuth consent.

It should teach:

1. **Diagnose `invalid_grant` by ruling out the recurring causes** in order:
   recent revocation → 7-day Testing-mode expiry (most likely if "this keeps
   happening") → password change → 6-month inactivity → 50-token cap.
2. **Move OAuth client from Testing to In production** as the durable fix
   for recurring revocations on external accounts. Path: Cloud Console →
   APIs & Services → OAuth consent screen → "Publish app".
3. **Always pass `prompt="consent"` + `access_type="offline"`** on re-auth
   flows, and assert `creds.refresh_token is not None` afterwards.
4. **Wrap client_config under `"installed"` (or `"web"`)** when feeding it to
   `InstalledAppFlow.from_client_config`.
5. **`run_local_server(port=0)` + `http://localhost` redirect** is the
   collision-free local-callback pattern.
6. **Atomic writeback** of the rotated refresh_token via `.tmp` + `replace()`
   to avoid corrupting the shared config file.
7. **Sanity-check the new credentials** with a cheap list-style API call
   before considering re-auth done.

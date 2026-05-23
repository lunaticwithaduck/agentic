---
domain: google-oauth
source_task: 2026-05-11-reauth-blogger-fix-redirect-uri.md
date: 2026-05-11
keywords: ["redirect_uri_mismatch", "web application client", "desktop app client", "InstalledAppFlow", "run_local_server", "loopback port"]
---

## Extracted Knowledge

### `Error 400: redirect_uri_mismatch` — Web-app clients require exact URI match

When the consent screen rejects with:

```
Error 400: redirect_uri_mismatch
This app's request is invalid
```

…the redirect URI your code sent does not appear in the OAuth client's
"Authorized redirect URIs" list. Google won't tell you what URI it actually
received — you have to deduce it from your code's flow type and port choice.

The kicker: this only happens on **Web application** type OAuth clients.
"Desktop app" clients support automatic loopback redirection on any port
(`http://localhost:<any>/` works without pre-registration). Google's Cloud
Console has defaulted to creating Web-application clients for years, so most
existing clients in the wild fall into this strict-matching bucket.

### How to tell which type your existing client is

You can't reliably tell from the client_id alone. Two practical heuristics:

- If `port=0` (random ephemeral port) ever worked: probably Desktop-app.
- If you got `redirect_uri_mismatch` with a random port: definitely Web-app.

Check explicitly: Cloud Console → APIs & Services → Credentials → click the
OAuth client. The page shows "Application type: Web application" or
"Desktop application" at the top.

### The fix for Web-app clients: pin a port and register that exact URI

In your script:

```python
OAUTH_LOOPBACK_PORT = 8766
EXPECTED_REDIRECT_URI = f"http://localhost:{OAUTH_LOOPBACK_PORT}/"

client_config = {
    "installed": {
        ...,
        "redirect_uris": [EXPECTED_REDIRECT_URI],  # match what we'll actually use
    }
}
flow = InstalledAppFlow.from_client_config(client_config, SCOPES)
creds = flow.run_local_server(port=OAUTH_LOOPBACK_PORT, ...)
```

In Cloud Console: Credentials → OAuth client → "Authorized redirect URIs" →
Add URI → paste `http://localhost:8766/` (trailing slash required, byte-exact
match) → Save. Wait ~30s for propagation, then rerun the script.

### Byte-exact match: the trailing slash matters

Google's matcher does not normalize. `http://localhost:8766` and
`http://localhost:8766/` are considered different. `run_local_server` ends
its redirect URI with `/`, so register that exact form.

### Pre-flight the port pin

Before pinning a port, check it's actually free on the user's machine —
`ss -tln | grep ":<port> "`. Other long-running local servers (static-file
servers, dev daemons) frequently grab common ports. If the port is taken,
your script will fail with `OSError: [Errno 98] Address already in use`
before Google ever sees the request. Pick a less-common port (8766, 8090,
9876) over conventional ones (8080, 3000, 5000, 8000, 8888) to reduce
collision risk.

### Why `port=0` is tempting but wrong on Web-app clients

OS-picks-ephemeral-port (`run_local_server(port=0)`) is collision-proof and
the canonical pattern for desktop tooling. But it produces a different
redirect URI every run (`http://localhost:54321/`, then `:51234/`, then
`:39456/`…), and no Web-application client's redirect list can enumerate
every possible port. Pinning a port trades one-time Cloud Console registration
for stability — for any Web-application client, that trade is mandatory.

## Failure Modes Observed

The previous `.sc` in this domain (`2026-05-11-blogger-oauth-reauth-helper.sc`)
contained this misleading guidance:

> "The user's existing redirect URI list in the Cloud Console must include
> `http://localhost` (no port) — Google treats that as a wildcard for any
> localhost port."

That description is correct **only for Desktop-app OAuth clients** and was
written without checking which client type the user actually had. Applied to
a Web-application client — which is what the user has, and which is the
default Cloud Console creates — `port=0` + bare `http://localhost` produces
`Error 400: redirect_uri_mismatch` on every consent attempt.

The correct distinction (now captured in this `.sc`):

| Client type | Loopback behavior |
|---|---|
| Desktop app | Any `http://localhost:<port>/` accepted without pre-registration |
| Web application | Byte-exact match against registered URIs only |

When future synthesis builds a `google-oauth` skill from these `.sc` files,
the Web-app strict-matching path is the safer default to teach — most
clients in the wild are Web-application type, and the failure mode is loud
(immediate consent-screen rejection) rather than silent. The Desktop-app
wildcarding can be mentioned as a special case for clients explicitly of
that type.

## Proposed Skill Content

A `google-oauth` skill should add the following section on top of what the
prior `.sc` proposed:

1. **Diagnose `redirect_uri_mismatch` by checking client type first.**
   Cloud Console → Credentials → click the OAuth client → read "Application
   type" at the top. Treat Web application as the default assumption.
2. **For Web-application clients, pin a fixed port and register the exact
   URI** (including trailing slash, including port number). Do not use
   `port=0`. Do not register a bare `http://localhost`.
3. **For Desktop-app clients, use `port=0` and a bare `http://localhost`
   redirect URI.** The loopback flow handles the rest.
4. **Pre-flight the chosen port with `ss -tln`** to avoid `Address already
   in use` errors from unrelated local servers.
5. **Remember the trailing slash on registered redirect URIs** — Google's
   matcher is byte-exact and does not normalize.

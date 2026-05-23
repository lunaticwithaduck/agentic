---
domain: google-oauth
source_task: 2026-05-18-realign-reauth-helper-to-8080.md
date: 2026-05-18
keywords: ["OAUTH_LOOPBACK_PORT", "redirect_uri drift", "helper Cloud Console divergence", "reauth_blogger", "port pinning"]
---

## Extracted Knowledge

### "Track to what's empirically registered, not what's theoretically registered"

When realigning an OAuth helper's pinned loopback port, the decision
between two candidate ports should favor the one you have *evidence* is in
the Cloud Console "Authorized redirect URIs" list — not the one you
"remember adding" in a previous task round. The cost of being wrong is a
`redirect_uri_mismatch` at the next re-auth (often months later, by which
point nobody remembers why).

Concrete heuristic: if a re-auth round just completed successfully using
port X, register that observation in the helper's comment block ("Confirmed
registered as of YYYY-MM-DD") and align the helper to port X. Leaving the
helper on a different port Y just because Y was the "official" pick in a
prior round is how drift compounds across years.

### One constant, downstream f-string interpolation

Keep the pinned port as a single named constant and have everything
downstream derive from it via f-string:

```python
OAUTH_LOOPBACK_PORT = 8080
EXPECTED_REDIRECT_URI = f"http://localhost:{OAUTH_LOOPBACK_PORT}/"

client_config = {"installed": {..., "redirect_uris": [EXPECTED_REDIRECT_URI]}}
creds = flow.run_local_server(port=OAUTH_LOOPBACK_PORT, ...)
```

Changing the port is then a one-line edit. Verify post-edit with
`grep -n <old_port> <file>` — if it returns matches, you missed a hardcoded
copy somewhere (commonly in the comment block above the constant, or in
docstrings / error messages that string-interpolate the port number).

### Date-stamp the registration evidence in the comment

Comments that say "MUST list http://localhost:8080/" without a date age
poorly — readers can't tell whether the comment was the source of truth or
a stale aspiration. Append "Confirmed registered as of YYYY-MM-DD (re-auth
round)" so the next maintainer knows the comment reflects observed reality,
not a wish.

## Proposed Skill Content

The existing `google-oauth` skill already has Section 10 ("Helper-vs-Cloud-
Console drift is invisible until next re-auth"). This task adds a concrete
implementation pattern that should be folded into Section 10 on the next
synthesis pass:

1. Use a single `OAUTH_LOOPBACK_PORT` constant; derive `EXPECTED_REDIRECT_URI`
   from it via f-string; pass both into `client_config["...".["redirect_uris"]`
   and `run_local_server(port=...)`. Changing the port becomes a one-line edit.
2. After changing the port, `grep -n <old_port> <helper>` to catch stale
   copies hiding in comments or error messages.
3. Date-stamp the comment with "Confirmed registered as of YYYY-MM-DD" so
   readers can distinguish observed truth from prior intention.
4. When choosing between two candidate ports, pick the one with empirical
   evidence of registration (a successful re-auth this round), not the one
   that "should be" registered from history.

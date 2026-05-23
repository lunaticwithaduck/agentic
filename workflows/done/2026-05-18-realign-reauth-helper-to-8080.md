---
title: Realign canonical reauth_blogger.py from port 8765 to 8080
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Eliminate the helper-vs-Cloud-Console port drift identified at the end of
`done/2026-05-18-fix-blogger-oauth-expired-again.md`. The canonical re-auth
helper at `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py` is hard-coded
to `OAUTH_LOOPBACK_PORT = 8765`, but Cloud Console's "Authorized redirect URIs"
has `http://localhost:8080/` registered (confirmed working today). We can't
verify whether `http://localhost:8765/` is still registered without opening
the console, so leaving the helper on 8765 is a future footgun: next re-auth
(~6 months from now under "In production" status) may hit
`redirect_uri_mismatch` and force another debug spiral.

Realigning the helper to 8080 means it targets the port we *know* is
registered, and removes a divergence between two places that must stay in
sync but rarely do.

## Steps
- [x] Edit `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py`:
      change `OAUTH_LOOPBACK_PORT = 8765` to `OAUTH_LOOPBACK_PORT = 8080`.
- [x] Update the surrounding comment block that references "8765" so it
      doesn't lie. `EXPECTED_REDIRECT_URI` interpolates the constant so it
      tracks automatically — no separate change needed there.
- [x] `python -m py_compile` the helper to confirm syntax is clean.
- [x] `grep -n 8765 /home/jojo/Documents/ProbBrain/tools/reauth_blogger.py`
      to confirm no stale references remain.
- [x] Do NOT touch `/home/jojo/probbrain-accuracy/`-serving
      `python3 -m http.server 8765` (pid 2111681). User did not authorize
      killing it this round; flagging only.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-18-realign-reauth-helper-to-8080.md` before starting any new work.

## Outcome

Completed on 2026-05-18.

One-line constant change plus a comment refresh:

- `OAUTH_LOOPBACK_PORT` flipped from `8765` → `8080` at
  `Documents/ProbBrain/tools/reauth_blogger.py:39`. `EXPECTED_REDIRECT_URI`
  is an f-string interpolation of that constant, so it automatically tracks
  to `http://localhost:8080/` with no separate edit.
- The 5-line comment above the constant rewrote `8765` → `8080` and added
  "Confirmed registered as of 2026-05-18 (re-auth round)" so future-me
  doesn't re-doubt which port is the right one without re-debugging.
- `python -m py_compile` passes. `grep -n 8765` over the helper returns
  zero matches — no stale references hiding.

The helper now matches the only redirect URI we have empirical proof is
registered in Cloud Console (today's re-auth completed against
`http://localhost:8080/`). Next re-auth — likely ~6 months out under "In
production" token persistence — will work without anyone re-opening Cloud
Console to investigate.

The stale `python3 -m http.server 8765 --bind 127.0.0.1` (pid 2111681,
serving `~/probbrain-accuracy/`) is still running on port 8765. User did
not authorize killing it this round, so it was left alone. Note: now that
the helper targets 8080, that port collision is no longer a re-auth issue
either way.

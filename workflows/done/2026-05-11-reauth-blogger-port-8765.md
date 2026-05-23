---
title: Kill stale 8765 http.server and pin reauth helper back to 8765
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
User authorized killing PID 714064 (a 9-day-old `python3 -m http.server 8765`
serving `/home/jojo/probbrain-accuracy/` — a forgotten local dev preview of
the public dashboard, no automation depends on it) and switching the
re-auth helper's OAuth callback back to port 8765. Fewer digits to memorize
when pasting the redirect URI into the Google Cloud Console.

## Steps
- [x] `kill 714064` and verify port 8765 is free.
- [x] Edit `tools/reauth_blogger.py`: change `OAUTH_LOOPBACK_PORT` from
      8766 → 8765 (and update the comment that explains the port choice).
- [x] Compile-check.
- [x] Tell the user the updated redirect URI to paste into Cloud Console.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-reauth-blogger-port-8765.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

- `kill 714064` cleanly terminated the stale `python3 -m http.server 8765`
  process. Confirmed via `ss -tln`: port 8765 is now free.
- Edited `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py` —
  `OAUTH_LOOPBACK_PORT` is now `8765` and the comment explaining the choice
  was updated to match. `EXPECTED_REDIRECT_URI` interpolates the constant so
  it tracks automatically.
- `python -m py_compile` passes; grep confirms no stale 8766 references.

Updated Cloud Console step the user needs to take:

```
http://localhost:8765/
```

(trailing slash, exact match — Google's matcher is byte-exact). Paste that
into APIs & Services → Credentials → the OAuth client → "Authorized
redirect URIs" → Save, wait ~30s, then rerun:

```
/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py
```

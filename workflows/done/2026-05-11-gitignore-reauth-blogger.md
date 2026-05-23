---
title: Gitignore tools/reauth_blogger.py in the ProbBrain repo
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
`tools/reauth_blogger.py` is currently untracked in the ProbBrain repo and the
user has decided it shouldn't be checked in (one-off local helper for minting
new Blogger OAuth refresh tokens; only useful on the machine that runs the
publisher, no need to share via git).

Add the path to `.gitignore` so it stops appearing in `git status` and can
never accidentally land in a future `git add .`.

## Steps
- [x] Append `tools/reauth_blogger.py` to `/home/jojo/Documents/ProbBrain/.gitignore`
      under a clearly-labelled section.
- [x] `git status` confirms the file is no longer listed under Untracked files.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-gitignore-reauth-blogger.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

Appended to `/home/jojo/Documents/ProbBrain/.gitignore` (right above the
existing OS-junk section):

```
# Local-only operator helpers (machine-specific, not for sharing)
tools/reauth_blogger.py
```

`git check-ignore -v` confirms the rule fires:

```
.gitignore:33:tools/reauth_blogger.py    tools/reauth_blogger.py
```

The file no longer appears under "Untracked files" in `git status`. Committed
the `.gitignore` change as `296a5e8` (commit message: `chore: gitignore
tools/reauth_blogger.py`) so the rule persists across checkouts. ProbBrain
`main` is now 2 commits ahead of `origin/main` — the earlier
`44cd6ac fix(x): rotate tweet templates + retry on dupe-content 403` and
this one — both will go up on the next scanner auto-push or manual push.

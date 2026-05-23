---
title: Make reauth_blogger.py directly executable from the shell
created: 2026-05-11
completed: 2026-05-11
status: done
---

## Goal
The user ran `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py` directly
in bash (without prefixing `venv/bin/python`) and got `Permission denied`.

Two reasons:
1. The file is `rw-r--r--`, no execute bit.
2. Even with `+x`, the current shebang `#!/usr/bin/env python3` would pick the
   system Python which lacks `google_auth_oauthlib`.

Fix scope: point the shebang at the ProbBrain venv and `chmod +x`, so the
script becomes a one-liner the user can paste directly.

## Steps
- [x] Change the shebang in `tools/reauth_blogger.py` from
      `#!/usr/bin/env python3` to `#!/home/jojo/Documents/ProbBrain/venv/bin/python`.
- [x] `chmod +x` the file.
- [x] Verify the file is executable and the new shebang resolves.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-11-reauth-blogger-executable.md` before starting any new work.

## Outcome

Completed on 2026-05-11.

Updated the shebang in `/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py`
to point directly at the project venv interpreter
(`#!/home/jojo/Documents/ProbBrain/venv/bin/python`) and added the execute
bit (`chmod +x`). File mode is now `rwxr-xr-x`.

The user can now invoke the helper with just the path:

```
/home/jojo/Documents/ProbBrain/tools/reauth_blogger.py
```

No `venv/bin/python` prefix needed.

(Generic Unix wisdom note: pointing a venv tool's shebang at the venv's
absolute interpreter path is the standard way to make it "just work" when
invoked by path. `#!/usr/bin/env python3` is for scripts that should run
against whatever python is on the user's PATH — wrong default for a tool
that needs venv-specific deps.)

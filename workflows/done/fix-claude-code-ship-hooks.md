---
title: Fix claude-code ship hooks — copy .cjs files in build script
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
The claude-code build script copies `*.js` and `*.sh` files for hooks, but the hooks are
`.cjs` files. The ship ends up with only `.sh` shims while `settings.json` references `.cjs`.
Other ships (cursor, copilot) copy `.cjs` files explicitly by name. Align claude-code to match.

## Steps
- [x] Fix build-claude-code.sh: replace *.js/*.sh glob with explicit .cjs file copies
- [x] Update root CLAUDE.md: change hook filename references from .sh to .cjs
- [x] Fix the ship directly: copy .cjs files, remove .sh shims from ship/claude-code/.claude/hooks/

## Outcome

Completed on 2026-03-15. The claude-code ship's build script was using a `*.js *.sh` glob
that captured the `.sh` shims but missed the actual `.cjs` hook implementations. Fixed by
replacing the glob with explicit copies of `skill-detector.cjs`, `block-secrets.cjs`,
`post-write.cjs`, and `post-stop.cjs` — matching the pattern used in the cursor and copilot
build scripts. Also removed the stale `.sh` shims from the ship directory and updated CLAUDE.md
to reference `.cjs` filenames correctly.

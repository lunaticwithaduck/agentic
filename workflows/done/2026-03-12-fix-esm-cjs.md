---
title: Fix hooks — rename .js to .cjs for ESM project compatibility
status: done
created: 2026-03-12
completed: 2026-03-12
---

## Problem
Hooks use require() (CommonJS). If a client's project has "type": "module" in
package.json, Node.js treats .js as ESM and throws "require is not defined".

## Fix
Rename all shipped hook files from .js to .cjs. Node.js always treats .cjs as
CommonJS regardless of package.json "type" field. Update shims, build scripts,
hooks.json generator, and bench suites accordingly.

Files to rename:
- .claude/hooks/*.js → *.cjs (4 files)
- buildScripts/src/cursor-hooks/*.js → *.cjs (2 files)

## Outcome

Completed on 2026-03-12. Renamed all 6 hook implementations from `.js` to `.cjs`
using `git mv` (preserving history). Updated all 4 `.sh` shims to reference `.cjs`.
Updated `build-cursor.sh` to copy `.cjs` files, `generate-hooks-json.js` to emit
`.cjs` paths in hooks.json, and bench suites 03 and 07 to look for `.cjs` files.
Build verified clean (25 cursor files, hooks.json references `.cjs` throughout).
All three suites pass: 01 (24/24), 03 (89/89), 07 (12/12).

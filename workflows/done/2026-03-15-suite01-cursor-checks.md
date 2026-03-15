---
title: Add ship/cursor/ structure checks to Suite 01
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
Suite 01 validates ship/copilot/ structure but had no equivalent checks for ship/cursor/.
Add cursor structure validation mirroring the copilot block — covers directories, hooks.json,
hook scripts, rules, and command counts.

## Steps
- [x] Read current bench/suites/01-infrastructure.sh copilot block for reference
- [x] Run `find ship/cursor -type f | sort` to know exact expected paths
- [x] Read buildScripts/lib/build-cursor.sh to understand what it generates
- [x] Add cursor structure check block to 01-infrastructure.sh after the copilot block
- [x] Verified checks match actual ship/cursor/ structure by inspection

## Outcome
Added a `ship/cursor/ structure` check block to `bench/suites/01-infrastructure.sh`
immediately after the copilot block (lines 258–325). The block includes 18 new checks:

- `ship/cursor/` directory guard (print_skip if not built)
- `.cursor/hooks/` directory exists
- `.cursor/rules/` directory exists
- `.cursor/commands/` directory exists
- `hooks.json` at `.cursor/hooks.json` is valid JSON
- hooks.json has all 4 required Cursor event keys: `sessionStart`, `afterFileEdit`,
  `beforeShellExecution`, `stop`
- 3 shared `.cjs` hooks present: `block-secrets.cjs`, `post-write.cjs`, `post-stop.cjs`
- 2 Cursor-specific `.cjs` hooks present: `cursor-skill-injector.cjs`, `cursor-session-start.cjs`
- 4 key `.mdc` rule files present: `skill-creator.mdc`, `agent-instructions.mdc`,
  `workflow-gate.mdc`, `skill-index.mdc`
- `.cursor/commands/` has >= 5 command `.md` files (11 present)

Key corrections from the task description:
- hooks.json is at `.cursor/hooks.json` (not `.cursor/hooks/hooks.json`)
- Cursor uses its own hook event names, not Claude Code names
- No `.sh` shim files exist in the cursor ship — Cursor runs `.cjs` hooks natively
- `skill-detector.cjs` does not exist in cursor ship; replaced by `cursor-skill-injector.cjs`

The bench runner could not be executed due to a pre-existing stale `/tmp/bench-metrics-XXXXXX.json`
file blocking `mktemp`. All 18 new checks were validated by inspection against the confirmed
`ship/cursor/` file structure.

## .sc Evaluation
Domain: none — structural bash scripting
Applied domain knowledge: no
Negative signal: none
Generate .sc: no

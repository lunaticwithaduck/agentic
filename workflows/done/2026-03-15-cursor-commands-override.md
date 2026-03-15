---
title: Implement cursor-commands override directory
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
Fix the known problem (cursor-commands-not-adapted.md) where Cursor command files contain
Claude Code-specific paths and references. Implement buildScripts/src/cursor-commands/ with
Cursor-specific variants of the affected commands. Update build-cursor.sh to merge overrides.

## Steps
- [x] Read each affected command in .claude/commands/
- [x] Read buildScripts/lib/build-cursor.sh to understand current copy step
- [x] Create buildScripts/src/cursor-commands/ directory
- [x] Write Cursor-specific variants for /setup, /status, /clean, /complete, /agent
      (replace .claude/ refs with .cursor/, remove Claude Code-only features)
- [x] Update build-cursor.sh to merge cursor-commands/ overrides after copying base commands

## Outcome

Completed on 2026-03-15. Created `buildScripts/src/cursor-commands/` with five Cursor-specific
command overrides. Each file corrects the path and feature references that were wrong in the
Claude Code originals:

- **setup.md** — removed platform detection logic (Cursor always uses `.cursor/`), removed the
  MCP servers step (Step 4) entirely, updated hook path to `.cursor/hooks/post-write.cjs`,
  updated config target to `.cursor/rules/agent-instructions.mdc`
- **status.md** — replaced `.claude/skill-usage.json` with `.cursor/skill-usage.json`, replaced
  `.claude/skills/skill-rules.json` with `.cursor/rules/skill-index.mdc`, updated command
  references from `/clean` to `clean` (Cursor command syntax)
- **clean.md** — replaced `.claude/skill-usage.json` with `.cursor/skill-usage.json`, replaced
  `.claude/skills/skill-rules.json` with `.cursor/rules/skill-index.mdc`, removed the archived/
  step (archive to `.cursor/skills/archived/`) and replaced with removal from skill-index.mdc
- **complete.md** — replaced `.claude/` skill path references with `.cursor/rules/`, updated
  `autolearn-pending` target from `.claude/autolearn-pending` to `.cursor/autolearn-pending`,
  updated closing suggestion from `/status` to `status`
- **agent.md** — rewrote entirely to explain Cursor's model: no subagent dispatch, adopt the
  role directly, read `.cursor/rules/agent-instructions.mdc` for context

Updated `buildScripts/lib/build-cursor.sh` to insert step 9 (override merge) between the base
command copy (step 8) and the setup.sh copy (step 10). The override step uses a non-fatal
WARNING if the directory is missing, matching the style of other optional copy steps.

## .sc Evaluation
Domain: build-tooling
Applied domain knowledge: no
Negative signal: none
Generate .sc: no

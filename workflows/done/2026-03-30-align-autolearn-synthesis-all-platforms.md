---
title: Align autolearn synthesis instructions across all three platforms
created: 2026-03-30
completed: 2026-05-08
status: done
---

## Goal
Make autolearn synthesis consistent across claude-code, copilot, and cursor.
Both mechanisms should coexist: inline synthesis during `/complete` (primary) +
`autolearn-pending` hook fallback (safety net). Currently each platform has a
different subset of the full picture.

## Current State

| Aspect | claude-code | copilot | cursor |
|--------|-------------|---------|--------|
| `/complete` inline synthesis | NO (deferred only) | YES | NO (deferred only) |
| `autolearn-pending` fallback | YES (multi-platform detect) | YES (instructions only) | YES |
| `.sc` format with domain frontmatter | YES (in copilot src, not claude-code) | YES | NO (bare format) |
| Skill file path | `.claude/skills/[domain].md` | `.github/skills/[domain]/SKILL.md` | `.cursor/rules/[domain].mdc` |
| Confirmation output mentions synthesis | NO | YES | NO |

## Steps

### Source files to update (6 files):
- [ ] `.claude/commands/complete.md` — add inline synthesis (like copilot), keep autolearn-pending as fallback, platform-aware paths
- [ ] `buildScripts/src/copilot-commands/complete.md` — already has inline; add autolearn-pending fallback mention
- [ ] `buildScripts/src/copilot-rules/copilot-instructions.md` — already has hook description; add note about inline being primary
- [ ] `buildScripts/src/copilot-rules/workflow-gate.instructions.md` — add .sc format with domain frontmatter + synthesis threshold
- [ ] `buildScripts/src/cursor-commands/complete.md` — add inline synthesis + autolearn-pending fallback, cursor-specific paths
- [ ] `buildScripts/src/cursor-rules/workflow-gate.mdc` — add .sc format with domain frontmatter + synthesis threshold

### Alignment target:
All platforms should say the same thing, platform-adapted:
1. Write `.sc` with YAML frontmatter (domain, source_task, date, keywords)
2. Check threshold (≥3 same-domain `.sc` files, no existing skill)
3. If threshold met: synthesize inline NOW (primary path)
4. Also write `autolearn-pending` file as fallback (in case synthesis fails or was skipped)
5. Confirmation step mentions whether a skill was synthesized

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-03-30-align-autolearn-synthesis-all-platforms.md` before starting any new work.

## Outcome

Closed on 2026-05-08 as **not applicable**. The user confirmed this project is used with claude-code only — copilot and cursor are not in active use, so cross-platform alignment of autolearn synthesis is unnecessary.

No work was performed; acceptance criteria are intentionally left unchecked. If the user later starts using copilot or cursor, this task can be reopened — the analysis of current state and the alignment target above remain valid as a starting point.

A project memory was saved at `~/.claude/projects/-home-jojo-agentic/memory/project_platform_scope.md` so future sessions don't propose cross-platform work by default.

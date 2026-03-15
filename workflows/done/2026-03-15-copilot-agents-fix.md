---
title: Fix copilot agent files — path leaks and Claude Code-only tools
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
Fix copilot agent files that reference Claude Code-specific tools (TaskCreate, TaskUpdate,
TaskList) and wrong paths (.claude/agents/, .claude/skills/). Create
buildScripts/src/copilot-agents/ overrides where needed.

## Steps
- [x] Read all .claude/agents/*.md source files
- [x] Read buildScripts/lib/build-copilot.sh — how agents are converted (.agent.md output)
- [x] Read buildScripts/lib/convert-to-agent.js — conversion logic
- [x] Read workflows/problems/copilot-pretesting-findings.md for exact issue list
- [x] Create buildScripts/src/copilot-agents/ with fixed variants for affected agents
      (remove TaskCreate/TaskUpdate/TaskList refs, replace .claude/ with .github/,
       remove references to non-existent skills or replace with available ones)
- [x] Update build-copilot.sh to merge copilot-agents/ overrides (new step 12)

## Outcome

Created `buildScripts/src/copilot-agents/` with six fixed agent override files:

| File | Issues Fixed |
|------|-------------|
| `project-manager.md` | Removed all TaskCreate/TaskUpdate/TaskList references; replaced with instructions to manage work via `workflows/tasks/` files using Read/Write/Edit tools. Added inline task file template. |
| `worker.md` | Removed `.claude/skills/implementation.md` reference (wrong path + nonexistent skill); removed `code-review` skill reference; removed `TaskUpdate` tool reference. |
| `architect.md` | Removed 6 nonexistent skill references (system-design, api-design, adr, scenario-compare, dependency-graph, impact-analysis); removed TaskCreate tool reference; retained `mermaid-diagram` (exists in skill-rules.json); replaced `find-related`/`impact-analysis` steps with plain codebase exploration instructions. |
| `refactorer.md` | Removed 4 nonexistent skill references (code-smell-detector, find-related, refactor, explain-code); removed TaskUpdate tool reference; replaced skill-based steps with equivalent plain-language instructions. |
| `researcher-documenter.md` | Removed 8 nonexistent skill references (readme-generator, api-docs, onboarding-guide, technical-writing, changelog, code-walkthrough, summarize, explain-code, find-related, weekly-summary); retained `mermaid-diagram`; removed verbose skill list from Process section. |
| `auditor.md` | Removed `code-review` skill reference; removed TaskUpdate tool reference; replaced "using the `code-review` skill" with plain review instructions. |

Updated `buildScripts/lib/build-copilot.sh`:
- Added step 12: "Apply Copilot agent overrides from buildScripts/src/copilot-agents/"
- Step runs `convert-to-agent.js` on each override file, overwriting the base-converted versions in `ship/copilot/.github/agents/`
- Pattern mirrors the existing step 10 for `copilot-commands` overrides
- Renumbered downstream steps: gitkeep step moved from 13 to 14

The build pipeline now produces correct `.github/agents/*.agent.md` files: base conversion from `.claude/agents/` runs first, then the Copilot-specific overrides replace the files that had Claude Code tool references or nonexistent skill references.

## .sc Evaluation

```
.sc eval: 2026-03-15-copilot-agents-fix
- New pattern introduced: copilot-agents/ override directory mirrors copilot-commands/ pattern
- Candidate skill: none — the fix is structural (directory + build step), not a knowledge domain
- Recurring pattern: Copilot overrides follow a consistent "source override + convert-to-X.js" model
- No new skill warranted; pattern is already documented in build-copilot.sh comments
```

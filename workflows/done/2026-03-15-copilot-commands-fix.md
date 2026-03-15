---
title: Implement copilot-commands override directory
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
Fix copilot command/prompt files that contain Claude Code-specific paths and references.
Mirror the cursor-commands override pattern: create buildScripts/src/copilot-commands/
with copilot-specific variants, update build-copilot.sh to merge them.

## Steps
- [x] Read workflows/problems/copilot-pretesting-findings.md for full detail
- [x] Read buildScripts/src/cursor-commands/*.md for the pattern to follow
- [x] Read buildScripts/lib/build-copilot.sh to understand current copy step
- [x] Read the affected source commands in .claude/commands/
- [x] Create buildScripts/src/copilot-commands/ with corrected variants
      (replace .claude/ refs with .github/, remove Claude Code-only features)
- [x] Update build-copilot.sh to merge copilot-commands/ overrides after copying base commands

## Outcome

Completed on 2026-03-15. Created `buildScripts/src/copilot-commands/` with four override files
(setup.md, clean.md, status.md, agent.md) that replace Claude Code-specific paths and references
with their Copilot equivalents. Updated `buildScripts/lib/build-copilot.sh` with a new step 10
that runs after the base commands are converted (step 9), converting each override file through
`convert-to-prompt.js` and writing the result to `ship/copilot/.github/prompts/`, overwriting
the base versions. Steps 10-12 were renumbered to 11-13 to accommodate the new step.

### Changes made

**buildScripts/src/copilot-commands/setup.md**
- Removed platform detection (Step 0) and all Claude Code / Cursor branches
- Removed MCP Servers step (Claude Code only)
- Changed hook path from `.claude/hooks/post-write.sh` → `.github/hooks/post-write.cjs`
- Changed apply target from `CLAUDE.md` + `.claude/settings.json` → `copilot-instructions.md`
- Added verification of `.github/hooks/` and `workflows/` directories

**buildScripts/src/copilot-commands/clean.md**
- Changed `.claude/skill-usage.json` → `.github/skill-usage.json`
- Changed `.claude/skills/skill-rules.json` → `.github/skills/skill-rules.json`
- Removed archive concept (no `.claude/skills/archived/` equivalent): replaced with "delete skill
  directory from `.github/skills/` and remove entry from `.github/skills/skill-rules.json`"

**buildScripts/src/copilot-commands/status.md**
- Changed `.claude/skill-usage.json` → `.github/skill-usage.json`
- Changed `.claude/skills/skill-rules.json` → `.github/skills/skill-rules.json`
- Changed skill count source label to match `.github/skills/skill-rules.json`

**buildScripts/src/copilot-commands/agent.md**
- Changed all `.claude/agents/` paths → `.github/agents/` with correct `.agent.md` extensions
- Removed "Spawn a Task subagent" step (Claude Code-specific API)
- Added inline role adoption language matching the cursor-commands/agent.md pattern
- Updated intro text: "In GitHub Copilot, agents are not dispatched as separate subprocesses"

**buildScripts/lib/build-copilot.sh**
- Added step 10: iterate over `buildScripts/src/copilot-commands/*.md`, run each through
  `convert-to-prompt.js`, write to `ship/copilot/.github/prompts/` (overwrites base versions)
- Renumbered old steps 10-12 to 11-13

---

## .sc Evaluation

**Skill candidate evaluation:**
- Technologies/frameworks touched in this task: bash shell scripting, GitHub Copilot prompt file
  format (`.prompt.md` with YAML frontmatter), agentic build pipeline conventions
- Domain-specific knowledge involved: the `convert-to-prompt.js` conversion pattern (first line
  becomes `description:` in frontmatter, rest is body); copilot-commands override pattern mirrors
  cursor-commands override pattern exactly; `.github/agents/` uses `.agent.md` suffix not `.md`;
  step ordering matters — overrides must run after base conversion to guarantee overwrite
- Verdict: SKIP
- Reason: The task was structural plumbing (adding an override directory and a build step) with
  no non-obvious technology-specific patterns beyond what is already captured in the build pipeline
  itself — the patterns are self-documenting in the scripts and override files.

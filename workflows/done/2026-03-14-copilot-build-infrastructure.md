---
title: Copilot port — build infrastructure + content
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Implement ship/copilot/ build target. Creates all source files and the build script.
Final rebuild is done separately after all sub-tasks complete.

## Decisions made

- Skill injection: deterministic UserPromptSubmit hook (not model-selected)
- Instructions: separate copilot-instructions.md (not relying on CLAUDE.md native reading)
- Skills: skill-creator only at install, autolearn grows the library

## Files created

### buildScripts/lib/build-copilot.sh
Main build script for ship/copilot/ target. Follows exact pattern of build-cursor.sh:
creates dir structure, copies shared hooks, copies skill-detector-copilot.cjs as skill-detector.cjs,
generates hooks.json, converts skill-creator to directory format, generates filtered skill-rules.json,
copies copilot-instructions.md, workflow-gate.instructions.md, converts all commands and agents,
copies setup.sh, creates .gitkeep files, verifies output.

### buildScripts/lib/convert-to-prompt.js
Converts .claude/commands/*.md → .github/prompts/*.prompt.md. Adds YAML frontmatter
(description from first line, mode: agent). Body unchanged.

### buildScripts/lib/convert-to-agent.js
Converts .claude/agents/*.md → .github/agents/*.agent.md. Extracts description from first
paragraph after ## Role heading. Adds YAML frontmatter (description, tools: []). Full file
content preserved as body.

### buildScripts/src/copilot-hooks/skill-detector-copilot.cjs
Port of .claude/hooks/skill-detector.cjs for Copilot paths. ROOT_DIR resolves from
.github/hooks/ up two levels. Skill content path uses directory-based format:
skillDir/<name>/SKILL.md. All logic identical to claude-code version.

### buildScripts/src/copilot-hooks/generate-hooks-json-copilot.js
Generates hooks.json with Copilot event names: UserPromptSubmit, PreToolUse, PostToolUse, Stop.
Commands reference .github/hooks/*.cjs paths.

### buildScripts/src/copilot-rules/copilot-instructions.md
Full project instructions for Copilot. References .github/ paths throughout.
Includes: workflow pipeline, 3-layer skill system description, hooks table, agents table,
slash commands table, task pipeline REQUIRED section with mechanical completion language,
conventions, key directories, file structure, getting started.

### buildScripts/src/copilot-rules/workflow-gate.instructions.md
workflow-gate.mdc content reformatted as .instructions.md with applyTo: "**/*" frontmatter.

### buildScripts/src/copilot-setup.sh
Setup script for Copilot projects. Creates workflow dirs, verifies .github/ infrastructure,
checks for GitHub CLI + Copilot. Mirrors cursor-setup.sh pattern.

## Files modified

### buildScripts/build.sh
- Added COPILOT_STATUS variable
- Added copilot build block (source build-copilot.sh)
- Added copilot to mkdir -p line
- Added copilot summary block
- Updated failure condition to include COPILOT_STATUS
- Added version stamp for ship/copilot/.version

### .claude/hooks/block-secrets.cjs
Added comment documenting .github/ platform detection. isCursor detection unchanged;
copilot uses same plain-text output format as claude-code (not Cursor JSON).

### .claude/hooks/post-write.cjs
Added isCopilot detection: `path.basename(path.dirname(__dirname)) === '.github'`.
autolearn-pending path now resolves to .github/autolearn-pending for Copilot installs.

## Files created (ship/)

### ship/PLATFORM-PARITY.md
New comparison table covering Claude Code, Cursor, and GitHub Copilot across:
always-on instructions, hooks lifecycle, skill injection method, slash commands,
agents, workflow pipeline, autolearn pipeline, skill decay tracking, skills at install,
skill file format, secrets blocking, workflow gate enforcement.

## Outcome

All acceptance criteria met:
- buildScripts/lib/build-copilot.sh exists and is executable
- buildScripts/lib/convert-to-prompt.js converts commands correctly
- buildScripts/lib/convert-to-agent.js converts agents correctly
- buildScripts/src/copilot-hooks/skill-detector-copilot.cjs exists with correct paths
- buildScripts/src/copilot-rules/copilot-instructions.md exists
- buildScripts/src/copilot-rules/workflow-gate.instructions.md exists
- buildScripts/src/copilot-setup.sh exists
- buildScripts/build.sh updated to include copilot target
- block-secrets.cjs and post-write.cjs updated with .github/ detection
- ship/PLATFORM-PARITY.md created

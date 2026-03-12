---
title: Cursor ship — command files reference Claude Code paths and features
status: open
opened: 2026-03-12
---

## Context

The build step copies `.claude/commands/*.md` to `.cursor/commands/` as-is.
These command files were written for Claude Code and contain paths and
concepts that don't map to Cursor.

## Specific Issues

**Path leaks — references to `.claude/` in command bodies:**
- `/setup` → instructs updating `.claude/hooks/post-write.sh` and `.claude/settings.json`
- `/status` → reads `.claude/skill-usage.json` and `.claude/skills/skill-rules.json`
- `/clean` → reads `.claude/skill-usage.json`, `.claude/skills/skill-rules.json`, archives to `.claude/skills/archived/`
- `/complete` → references `.claude/skills/` for skill candidating context
- `/agent` → hardcodes all 8 agent paths under `.claude/agents/`

**Feature gaps — commands referencing Claude Code-only features:**
- `/agent` → dispatches subagents via the Agent tool. Cursor has no subagent system.
  The command is essentially inert in Cursor.
- `/setup` → mentions MCP server setup (`.claude/scripts/mcp-setup.sh`), which doesn't
  ship in the Cursor distribution.

## What Doesn't Break (just confusing)

Most commands still work at a high level — the AI reads the command intent and acts
accordingly even if path references are wrong. A user running `/complete` in Cursor will
still get a task completion workflow; the wrong `.claude/` path in the instruction text
doesn't prevent the core behavior.

The real risk is `/setup` producing wrong configuration advice.

## Why Not Fixing Now

- Commands are instructional text, not code — wrong paths cause confusion, not crashes
- The subagent gap (`/agent`) is a platform limitation, not a fixable command text issue
- A proper fix requires either: (a) a command content transform step in build-cursor.sh,
  or (b) maintaining separate `.claude/commands/` and `.cursor/commands/` source trees
- Option (b) creates a maintenance burden; option (a) requires a reliable text transform
  for Markdown prose (fragile)
- Impact is low for the first Cursor users — they'll see the right behaviors even if
  the incidental path references are wrong

## When to Fix

When Cursor adoption grows enough that command accuracy matters. At that point, the
cleanest fix is likely option (b): a `buildScripts/src/cursor-commands/` override directory
that the build step merges over the shared commands, selectively replacing commands that
need Cursor-specific variants.

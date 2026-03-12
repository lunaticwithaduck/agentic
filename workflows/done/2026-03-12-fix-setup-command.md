---
title: Make /setup platform-aware (CLAUDE.md vs agent-instructions.md)
status: done
created: 2026-03-12
completed: 2026-03-12
---

## Problem
/setup hardcodes Claude Code paths and concepts throughout:
- Step 5 writes to CLAUDE.md (doesn't exist in Cursor installs)
- Step 5 updates .claude/hooks/post-write.sh (wrong platform)
- Step 5 updates .claude/settings.json (no Cursor equivalent)
- Step 4 offers MCP setup via `claude mcp add` (Claude Code CLI, not available in Cursor)

## Fix
Detect platform at the start of /setup by checking which file exists:
- CLAUDE.md present → Claude Code mode
- .cursor/rules/agent-instructions.md present → Cursor mode

Apply changes to the correct files per platform. Remove/adapt MCP step for Cursor.

## Outcome

Completed on 2026-03-12. Added Step 0 (platform detection) to `/setup` — checks for `CLAUDE.md` vs `.cursor/rules/agent-instructions.md` to determine Claude Code vs Cursor mode. Step 3 now references the correct hook file per platform (`post-write.sh` vs `post-write.cjs`). Step 4 (MCP servers) is explicitly Claude Code only with a skip note for Cursor. Step 5 branches fully: Claude Code updates `CLAUDE.md` + `.claude/hooks/post-write.sh` + `.claude/settings.json`; Cursor updates `.cursor/rules/agent-instructions.md` + `.cursor/hooks/post-write.cjs` (no settings.json equivalent). Build verified clean (25 cursor files, 33 claude-code files).

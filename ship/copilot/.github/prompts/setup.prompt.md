---
description: Interactive setup to personalize the agentic infrastructure for a new project.
mode: agent
---

## Instructions

### Step 0: Detect Platform

Before asking any questions, detect which platform this install is running on:

- If `CLAUDE.md` exists in the project root → **Claude Code mode**
- If `.cursor/rules/agent-instructions.mdc` exists → **Cursor mode**

Store this for use in Steps 3, 4, and 5. If both exist, prefer Claude Code mode and note it.
If neither exists, tell the user agentic doesn't appear to be installed and stop.

### Step 1: Project Basics
Ask the user:
- What is the project name?
- Brief one-line description?
- What language/framework? (e.g., TypeScript + Next.js, Python + FastAPI, Rust + Axum)

### Step 2: Commands
Ask the user:
- What is the build command? (e.g., `npm run build`, `cargo build`, `make`)
- What is the test command? (e.g., `npm test`, `pytest`, `cargo test`)
- What is the lint command? (e.g., `npm run lint`, `ruff check .`, `cargo clippy`)
- What are the key source directories? (e.g., `src/`, `app/`, `lib/`)

### Step 3: Hooks
Based on the language/framework, suggest appropriate post-write hooks:
- TypeScript/JavaScript: ESLint, Prettier
- Python: Ruff, Black, mypy
- Rust: rustfmt, clippy
- Go: gofmt, go vet

Ask: "Would you like me to configure these hooks?"

**Claude Code**: The hook to update is `.claude/hooks/post-write.sh`
**Cursor**: The hook to update is `.cursor/hooks/post-write.cjs`

### Step 4: MCP Servers (Claude Code only)
*Skip this step entirely for Cursor — `claude mcp add` is a Claude Code CLI command with no Cursor equivalent.*

**Claude Code only**: Ask: "Would you like to set up any MCP servers?"
- context7 (library documentation lookup)
- filesystem (enhanced file access)
- Custom (let user specify)

If yes, provide the `claude mcp add` commands they should run.

### Step 5: Apply Changes
After gathering all information, apply changes to the platform-appropriate files:

**Claude Code**:
1. Update `CLAUDE.md` — fill in Language/Framework, Build/Test/Lint commands, Key Directories
2. Update `.claude/hooks/post-write.sh` with language-specific linting (if user said yes in Step 3)
3. Update `.claude/settings.json` permissions if needed
4. Confirm all changes made

**Cursor**:
1. Update `.cursor/rules/agent-instructions.mdc` — fill in Language/Framework, Build/Test/Lint commands, Key Directories
2. Update `.cursor/hooks/post-write.cjs` with language-specific linting (if user said yes in Step 3)
3. Confirm all changes made (no settings.json equivalent in Cursor)

### Step 6: Verify
- Run the test command to verify it works
- Run the lint command to verify it works
- Confirm setup is complete
- Suggest: "You're all set. Use `/idea` to start capturing work items."

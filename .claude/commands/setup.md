Interactive setup to personalize the agentic infrastructure for a new project.

## Instructions

Walk the user through configuring agentic for their specific project. Ask questions one section at a time, then apply changes.

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

### Step 4: MCP Servers
Ask: "Would you like to set up any MCP servers?"
- context7 (library documentation lookup)
- filesystem (enhanced file access)
- Custom (let user specify)

If yes, provide the `claude mcp add` commands they should run.

### Step 5: Apply Changes
After gathering all information:
1. Update `CLAUDE.md` with project-specific sections:
   - Fill in Language/Framework
   - Fill in Build/Test/Lint commands
   - Fill in Key Directories
2. Update `.claude/hooks/post-write.sh` with language-specific linting
3. Update `.claude/settings.json` permissions if needed
4. Confirm all changes made

### Step 6: Verify
- Run the test command to verify it works
- Run the lint command to verify it works
- Confirm setup is complete
- Suggest: "You're all set. Use `/idea` to start capturing work items."

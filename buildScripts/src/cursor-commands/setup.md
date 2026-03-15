Interactive setup to personalize the agentic infrastructure for a new project.

## Instructions

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

The hook to update is `.cursor/hooks/post-write.cjs`

### Step 4: Apply Changes
After gathering all information, apply changes to the platform-appropriate files:

1. Update `.cursor/rules/agent-instructions.mdc` — fill in Language/Framework, Build/Test/Lint commands, Key Directories
2. Update `.cursor/hooks/post-write.cjs` with language-specific linting (if user said yes in Step 3)
3. Confirm all changes made

### Step 5: Verify
- Run the test command to verify it works
- Run the lint command to verify it works
- Confirm setup is complete
- Suggest: "You're all set. Use `idea` to start capturing work items."

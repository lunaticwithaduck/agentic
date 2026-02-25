# CLAUDE.md - Agentic Project Infrastructure

This project uses **agentic**, an AI-first infrastructure for Claude Code.
It provides workflows, skills, hooks, subagents, and slash commands out of the box.

## Workflow Pipeline

Ideas flow through a three-stage pipeline:

1. **Ideas** (`workflows/ideas/`) - Raw ideas, feature requests, brainstorms
2. **Tasks** (`workflows/tasks/`) - Refined, actionable work items with clear acceptance criteria
3. **Done** (`workflows/done/`) - Completed work with outcome notes

Use `/idea`, `/promote`, `/complete`, and `/status` slash commands to manage the pipeline.

## Available Skills

Skills live in `.claude/skills/` and provide domain expertise. Skills are **auto-detected** via
the `UserPromptSubmit` hook -- every prompt is evaluated against the full skill library and
relevant skills are activated automatically.

### Skill Library by Category

| Category | Skills |
|----------|--------|
| **Code Quality** | `code-review`, `refactor`, `explain-code`, `debug`, `performance-optimization`, `code-smell-detector` |
| **Git & GitHub** | `git-commit`, `review-pr`, `create-pr`, `changelog`, `git-workflow` |
| **Architecture** | `adr`, `impact-analysis`, `dependency-graph`, `scenario-compare`, `system-design`, `api-design` |
| **Testing** | `test-writer`, `test-coverage`, `e2e-testing`, `mock-generator` |
| **Documentation** | `api-docs`, `readme-generator`, `technical-writing`, `code-comments`, `jsdoc-generator` |
| **Content** | `pdf-extract`, `summarize`, `de-ai-ify` |
| **DevOps** | `dockerfile`, `ci-cd`, `deployment`, `monitoring`, `cost-analysis`, `infrastructure` |
| **Data** | `database-schema`, `migration`, `data-modeling`, `sql-optimization`, `seed-generator` |
| **Security** | `security-audit`, `vulnerability-scan`, `secrets-management`, `dependency-check` |
| **Frontend** | `component-design`, `accessibility-audit`, `responsive-design`, `css-review`, `storybook` |
| **Backend** | `error-handling`, `logging-strategy`, `caching-strategy`, `middleware-design`, `rate-limiting` |
| **Meta** | `skill-creator`, `onboarding-guide`, `technical-proposal`, `code-walkthrough` |
| **Workflow** | `workflow-manager` |
| **Utilities** | `regex-helper`, `env-setup`, `error-message-writer`, `cli-builder`, `mermaid-diagram` |
| **Design** | `figma` |

### Skill Auto-Detection

The hook at `.claude/hooks/skill-detector.sh` runs on every user prompt and injects a skill
evaluation protocol. Pattern-based trigger rules are defined in `.claude/skills/skill-rules.json`,
mapping each skill to keywords, file patterns, and tool triggers. This achieves ~93% F1 score
(100% suite pass rate) using the "forced evaluation" approach -- Claude evaluates all skills against
the prompt and activates the 1-3 most relevant ones before proceeding.

## Hooks

Hooks in `.claude/settings.json` enforce guardrails automatically:

- **UserPromptSubmit**: Runs skill auto-detection on every prompt (`skill-detector.sh`)
- **PreToolUse (Bash)**: Blocks commands that would expose secrets or credentials
- **PostToolUse (Write/Edit)**: Runs validation after file modifications
- **Stop**: Post-response hook placeholder for skill usage validation (`post-stop.sh`)

## Multi-Agent System

Eight specialized agents in `.claude/agents/`:

| Agent | Role |
|-------|------|
| **project-manager** | Orchestrates work, breaks down tasks, coordinates all other agents |
| **architect** | Makes design decisions, evaluates trade-offs, defines system structure |
| **worker** | Implements scoped tasks assigned by the project manager |
| **refactorer** | Restructures existing code to improve quality without changing behavior |
| **researcher-documenter** | Gathers knowledge, researches topics, writes documentation |
| **devops** | Manages builds, pipelines, containers, deployments, and infrastructure |
| **security** | Finds vulnerabilities, reviews for exploits, hardens the codebase |
| **auditor** | Quality gatekeeper that reviews all agent output before acceptance |

**Usage**: Use `/agent <type> <task>` to dispatch a subagent, or let the PM orchestrate
multi-agent workflows for larger work items.

## Slash Commands

| Command | Description |
|---------|-------------|
| `/idea [title]` | Create a new idea in the pipeline |
| `/promote [file]` | Move an idea to an actionable task |
| `/complete [file]` | Mark a task as done |
| `/status` | Overview of all workflow stages |
| `/setup` | Personalize agentic for a new project |
| `/review` | Trigger a code review of current changes |
| `/security` | Run a security scan of the codebase |
| `/diagram` | Generate architecture diagrams |
| `/agent <type> <task>` | Dispatch a named subagent |
| `/clean [mode]` | Clean up stale items in the project |

## Conventions

1. **Use subagents for parallel work** - When multiple independent changes are needed,
   spawn worker subagents to handle them concurrently
2. **Use hooks for enforcement** - Automate guardrails rather than relying on memory;
   add linting, formatting, and security checks as hooks
3. **Use skills for expertise** - Reference skill files when performing specialized tasks
   like code review or testing
4. **Plan before coding** - Always read existing code and create a plan before implementing
5. **Test everything** - Write tests alongside features, not after
6. **Document decisions** - Record architectural decisions in `docs/ARCHITECTURE.md`

## Project-Specific Configuration

### Language / Framework
Bash + Python 3 (no external dependencies). Configuration via JSON and Markdown with YAML frontmatter.

### Build Commands
No build step — agentic is a collection of config files and shell scripts.

### Test Commands
```bash
bash bench/run.sh                  # run all benchmark suites
bash bench/run.sh --suite=02       # run a single suite
bash .claude/scripts/validate.sh   # validate infrastructure integrity
```

### Lint Commands
```bash
shellcheck .claude/hooks/*.sh      # lint hook scripts (if shellcheck installed)
python3 -m json.tool .claude/skills/skill-rules.json  # validate JSON
```

### Key Directories
- `.claude/skills/` — 65 skill definitions + skill-rules.json
- `.claude/hooks/` — 4 lifecycle hook scripts
- `.claude/agents/` — 8 agent role definitions
- `.claude/commands/` — 10 slash command definitions
- `bench/` — benchmark suite (suites, fixtures, results)
- `workflows/` — idea → task → done pipeline

## File Structure

```
.claude/
  settings.json    # Hooks, permissions, and configuration
  skills/          # Skill definitions (expertise areas)
  hooks/           # Hook scripts (guardrails)
  commands/        # Slash command definitions
  agents/          # Subagent role definitions
  scripts/         # Utility scripts (validate.sh, mcp-setup.sh)
workflows/
  ideas/           # Stage 1: Raw ideas
  tasks/           # Stage 2: Actionable tasks
  done/            # Stage 3: Completed work
docs/
  ARCHITECTURE.md  # System architecture documentation
```

## Getting Started

1. Clone this repo into your project (or use it as a template)
2. Run `bash setup.sh` to initialize
3. Run `/setup` in Claude Code to personalize
4. Customize `CLAUDE.md` with your project specifics
5. Start using `/idea` to capture work items

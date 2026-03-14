# CLAUDE.md - Agentic Project Infrastructure

This project uses **agentic**, an AI-first infrastructure for Claude Code.
It provides workflows, skills, hooks, subagents, and slash commands out of the box.

## Workflow Pipeline

Ideas flow through a pipeline:

1. **Ideas** (`workflows/ideas/`) - Raw ideas, feature requests, brainstorms
2. **Tasks** (`workflows/tasks/`) - Refined, actionable work items with clear acceptance criteria
3. **Done** (`workflows/done/`) - Completed work with outcome notes and `.sc` skill candidates
4. **Problems** (`workflows/problems/`) - Open design problems and known issues (not actionable yet)

Use `/idea`, `/promote`, `/complete`, and `/status` slash commands to manage the pipeline.

## Available Skills

Skills live in `.claude/skills/` and provide domain expertise. Skills are **auto-detected** via
the `UserPromptSubmit` hook -- every prompt is evaluated against the full skill library and
relevant skills are activated automatically.

### Skill Design Philosophy

Skills inject **domain-specific knowledge** that Claude doesn't have reliably on its own.
They are not methodology templates — Claude already knows how to debug, refactor, review code,
and write tests. Skills are for specialized domains where expert knowledge matters:
specific vulnerability classes, SQL query patterns, WCAG criteria, IaC tool syntax, etc.

Generic methodology skills (debug, refactor, code-review, test-writer, etc.) have been
intentionally excluded — they add template overhead without adding knowledge.

### Skill Library by Category

| Category | Skills |
|----------|--------|
| **Security** | `security-audit`, `vulnerability-scan`, `secrets-management`, `dependency-check` |
| **Data** | `database-schema`, `migration`, `data-modeling`, `sql-optimization` |
| **Backend** | `caching-strategy`, `rate-limiting`, `logging-strategy` |
| **DevOps** | `dockerfile`, `ci-cd`, `deployment`, `monitoring`, `cost-analysis`, `infrastructure` |
| **Frontend** | `accessibility-audit`, `responsive-design`, `css-review`, `storybook` |
| **Content** | `pdf-extract`, `de-ai-ify` |
| **Utilities** | `regex-helper`, `mermaid-diagram`, `skill-creator` |
| **Design** | `figma` |

### Skill Auto-Detection

The hook at `.claude/hooks/skill-detector.sh` runs on every user prompt. It reads the prompt
text, matches it deterministically against keywords in `.claude/skills/skill-rules.json`, and
injects matched skill content directly as context. If nothing matches, the hook outputs nothing —
zero overhead. No AI evaluation step, no token cost on non-domain prompts.

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
| `/bench` | Show latest benchmark results |

## Task Pipeline — REQUIRED

**This is not optional.** Before starting any multi-step work, you MUST:

1. Create a task file in `workflows/tasks/` — one file per logical unit of work
2. Implement the task
3. Run `/complete` on it before moving to the next task

**What does NOT count as task tracking:**
- TodoWrite or any in-memory task list
- In-chat markdown checklists (`- [ ] step 1`)
- Mental notes or planned-out responses
- Any mechanism that doesn't write a file to `workflows/tasks/`

The only valid task record is a `.md` file in `workflows/tasks/`. Everything else evaporates between sessions and defeats the purpose.

**Why it matters:** The pipeline is how skill candidates (`.sc` files) are generated, how decisions are recorded, and how the project's memory persists across sessions. Skipping it doesn't save time — it breaks the system.

The human manages their work in external tools (JIRA, Linear, etc.). `workflows/` exists for you.

### Task File Template

Every task file must follow this structure:

```markdown
---
title: Short description
created: YYYY-MM-DD
status: in-progress
---

## Goal
What needs to be done and why.

## Steps
- [ ] Step 1
- [ ] Step 2

## Completion
When all steps above are done:
Run `/complete workflows/tasks/THIS-FILENAME.md` before starting any new work.
```

The `## Completion` section is mandatory — it must be the last section in every task file.

## Conventions

1. **Use subagents for parallel work** - When multiple independent changes are needed,
   spawn worker subagents to handle them concurrently
2. **Use hooks for enforcement** - Automate guardrails rather than relying on memory;
   add linting, formatting, and security checks as hooks
3. **Use skills for domain expertise** - Skills inject knowledge for specialized domains
   (security, SQL, infra, a11y); for general coding tasks Claude needs no skill activation
4. **Plan before coding** - Always read existing code and create a plan before implementing
5. **Test everything** - Write tests alongside features, not after
6. **Document decisions** - Record open problems in `workflows/problems/`, completed decisions in `workflows/done/`

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
- `.claude/skills/` — 26 domain-specific skill definitions + skill-rules.json
- `.claude/hooks/` — 4 lifecycle hook scripts
- `.claude/agents/` — 8 agent role definitions
- `.claude/commands/` — 11 slash command definitions
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
  done/            # Stage 3: Completed work (+ .sc skill candidates)
  problems/        # Open design problems and known issues
```

## Getting Started

1. Clone this repo into your project (or use it as a template)
2. Run `bash setup.sh` to initialize
3. Run `/setup` in Claude Code to personalize
4. Customize `CLAUDE.md` with your project specifics
5. Start using `/idea` to capture work items

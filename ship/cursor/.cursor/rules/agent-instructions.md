---
description: Agent instructions — always active
alwaysApply: true
---

# Agent Instructions — Agentic Project Infrastructure

This project uses **agentic**, an AI-first infrastructure for Cursor.
It provides workflows, skills, hooks, and slash commands out of the box.

## Workflow Pipeline

Ideas flow through a pipeline:

1. **Ideas** (`workflows/ideas/`) - Raw ideas, feature requests, brainstorms
2. **Tasks** (`workflows/tasks/`) - Refined, actionable work items with clear acceptance criteria
3. **Done** (`workflows/done/`) - Completed work with outcome notes and `.sc` skill candidates
4. **Problems** (`workflows/problems/`) - Open design problems and known issues (not actionable yet)

Use `.cursor/commands/` slash commands to manage the pipeline: `idea`, `promote`, `complete`, `status`.

## Skill System

Skills inject **domain-specific knowledge** for specialized domains. Three layers work together:

**Layer 1 — Skill Index** (`skill-index.md`, always active): Lists all available skills and their trigger keywords. When you see a relevant topic in the conversation, you know which skill to request.

**Layer 2 — agentRequested rules** (per skill, full content): Type `@skill-name` to attach a skill, or request it when keywords from the index appear. More reliable because Layer 1 primes your awareness.

**Layer 3 — Automatic injection after file edits** (`cursor-skill-injector.js`): After any file edit, the hook matches the file path against skill `filePatterns` and injects the relevant skill content automatically. Deterministic, zero effort.

### Skill Design Philosophy

Skills inject **domain-specific knowledge** you don't have reliably on your own.
They are NOT methodology templates. Good candidates:
- Specific tool syntax (Dockerfile best practices, Terraform patterns, Mermaid syntax)
- Specific standards with enumerable criteria (WCAG levels, OWASP Top 10)
- Specific domain checklists (SQL indexing patterns, cache invalidation strategies)

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

## Slash Commands

Commands live in `.cursor/commands/`. Use them to manage the workflow pipeline:

| Command | Description |
|---------|-------------|
| `idea [title]` | Create a new idea in the pipeline |
| `promote [file]` | Move an idea to an actionable task |
| `complete [file]` | Mark a task as done |
| `status` | Overview of all workflow stages |
| `setup` | Personalize agentic for a new project |
| `review` | Trigger a code review of current changes |
| `security` | Run a security scan of the codebase |
| `diagram` | Generate architecture diagrams |
| `clean [mode]` | Clean up stale items in the project |
| `bench` | Show latest benchmark results |

## Conventions

1. **Use skills for domain expertise** - Skills inject knowledge for specialized domains (security, SQL, infra, a11y); for general coding tasks no skill activation is needed
2. **Use hooks for enforcement** - Guardrails run automatically; secrets blocking, file validation, and skill injection fire without prompting
3. **Plan before coding** - Always read existing code and create a plan before implementing
4. **Test everything** - Write tests alongside features, not after
5. **Document decisions** - Record open problems in `workflows/problems/`, completed decisions in `workflows/done/`
6. **Self-manage the task pipeline** - The workflow pipeline is agent-facing infrastructure, not human-facing. Before implementing multi-step work, break it into tasks and create them in `workflows/tasks/` (one per logical unit of work). Implement one task at a time, then run `complete` on it before moving to the next. Do not wait to be asked — the human works from external tools (JIRA, Linear, etc.); `workflows/` exists for you.

## Project-Specific Configuration

### Language / Framework
Bash + Python 3 (no external dependencies). Configuration via JSON and Markdown with YAML frontmatter.

### Build Commands
No build step — agentic is a collection of config files and shell scripts.

### Test Commands
```bash
bash bench/run.sh                  # run all benchmark suites
bash bench/run.sh --suite=02       # run a single suite
bash .cursor/scripts/validate.sh   # validate infrastructure integrity
```

### Lint Commands
```bash
python3 -m json.tool .cursor/rules/skill-index.md  # validate JSON (if applicable)
```

### Key Directories
- `.cursor/rules/` — skill rule definitions + skill-index.md
- `.cursor/hooks/` — lifecycle hook scripts
- `.cursor/commands/` — slash command definitions
- `bench/` — benchmark suite (suites, fixtures, results)
- `workflows/` — idea → task → done pipeline

## File Structure

```
.cursor/
  hooks.json       # Hook event configuration
  rules/           # Skill definitions (always-on index + agentRequested skills)
  hooks/           # Hook scripts (guardrails + skill injection)
  commands/        # Slash command definitions
workflows/
  ideas/           # Stage 1: Raw ideas
  tasks/           # Stage 2: Actionable tasks
  done/            # Stage 3: Completed work (+ .sc skill candidates)
  problems/        # Open design problems and known issues
```

## Getting Started

1. Clone this repo into your project (or use it as a template)
2. Run `bash setup.sh` to initialize
3. Use `setup` command to personalize agentic for your project
4. Customize this file with your project specifics
5. Start using `idea` to capture work items

# CLAUDE.md — Project Instructions

This project uses **agentic** infrastructure for AI-assisted development.
Run `/setup` to personalize this file for your project.

## Workflow Pipeline

Ideas flow through a pipeline:

1. **Ideas** (`workflows/ideas/`) — Raw ideas, feature requests, brainstorms
2. **Tasks** (`workflows/tasks/`) — Refined, actionable work items with clear acceptance criteria
3. **Done** (`workflows/done/`) — Completed work with outcome notes and `.sc` skill candidates
4. **Problems** (`workflows/problems/`) — Open design problems and known issues (not actionable yet)

Use `/idea`, `/promote`, `/complete`, and `/status` to manage the pipeline.

## Skills

Skills live in `.claude/skills/` and provide domain expertise. They are **auto-detected** via
the `UserPromptSubmit` hook — every prompt is matched against `.claude/skills/skill-rules.json`
and relevant skills are injected as context. No match = zero overhead.

Skills inject **domain-specific knowledge**, not methodology. Claude already knows how to
debug, refactor, and write tests. Skills are for specialized domains where expert knowledge
matters. New skills are auto-generated from completed tasks via the `.sc` pipeline.

## Hooks

Hooks in `.claude/settings.json` enforce guardrails automatically:

- **UserPromptSubmit**: Skill auto-detection + workflow gate (`skill-detector.cjs`)
- **PreToolUse (Bash)**: Blocks commands that would expose secrets (`block-secrets.cjs`)
- **PostToolUse (Write/Edit)**: Validation after file modifications (`post-write.cjs`)

## Multi-Agent System

Eight specialized agents in `.claude/agents/`:

| Agent | Role |
|-------|------|
| **project-manager** | Orchestrates work, breaks down tasks, coordinates agents |
| **architect** | Design decisions, trade-offs, system structure |
| **worker** | Implements scoped tasks |
| **refactorer** | Restructures code without changing behavior |
| **researcher-documenter** | Gathers knowledge, writes documentation |
| **devops** | Builds, pipelines, containers, deployments |
| **security** | Finds vulnerabilities, hardens the codebase |
| **auditor** | Quality gatekeeper — reviews all agent output |

Use `/agent <type> <task>` to dispatch a subagent.

## Slash Commands

| Command | Description |
|---------|-------------|
| `/idea [title]` | Create a new idea |
| `/promote [file]` | Move an idea to a task |
| `/complete [file]` | Mark a task as done |
| `/status` | Pipeline overview |
| `/setup` | Personalize for your project |
| `/review` | Code review of current changes |
| `/security` | Security scan |
| `/diagram` | Architecture diagrams |
| `/agent <type> <task>` | Dispatch a subagent |
| `/clean [mode]` | Clean up stale items |

## Task Pipeline — REQUIRED

Before starting any multi-step work, you MUST:

1. Create a task file in `workflows/tasks/` — one file per logical unit of work
2. Implement the task
3. When done, run `/complete` to move it to `workflows/done/` with outcome notes and `.sc` evaluation

The only valid task record is a `.md` file in `workflows/tasks/`. In-memory checklists, TodoWrite,
and chat-based lists do not count — they evaporate between sessions.

### Task File Template

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

## Project-Specific Configuration

> Run `/setup` to fill in this section, or edit manually.

### Language / Framework
<!-- e.g. TypeScript + React, Python + FastAPI, Go, etc. -->

### Build Commands
<!-- e.g. npm run build -->

### Test Commands
<!-- e.g. npm test, pytest -->

### Lint Commands
<!-- e.g. npm run lint, ruff check . -->

### Key Directories
<!-- e.g. src/ — application code, tests/ — test suite -->

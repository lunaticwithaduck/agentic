# agentic

A reusable, AI-first project infrastructure for Claude Code. Clone this into any project to instantly have a full Claude Code setup: skills, hooks, subagents, slash commands, workflow tracking, and MCP tooling.

## Quick Start

```bash
./setup.sh
```

This creates the workflow directories, ensures hooks are executable, and offers to configure MCP servers.

To validate the full infrastructure after setup:

```bash
bash .claude/scripts/validate.sh
```

## What's Included

| Component | Location | Purpose |
|-----------|----------|---------|
| Skills | `.claude/skills/` | Reusable expertise (code review, implementation, testing, workflow management) |
| Hooks | `.claude/hooks/` | Automated enforcement (secret blocking, post-write validation) |
| Agents | `.claude/agents/` | 8 specialized subagents (see below) |
| Commands | `.claude/commands/` | 10 slash commands (see below) |
| Workflows | `workflows/` | Idea-to-done pipeline (`ideas/` -> `tasks/` -> `done/`) |

## Agents

| Agent | Role |
|-------|------|
| **project-manager** | Orchestrates work, coordinates all other agents |
| **architect** | Design decisions, trade-offs, system structure |
| **worker** | Implements scoped tasks from the PM |
| **refactorer** | Improves code quality without changing behavior |
| **researcher-documenter** | Researches topics, writes documentation |
| **devops** | Builds, pipelines, containers, infrastructure |
| **security** | Finds vulnerabilities, hardens the codebase |
| **auditor** | Reviews all agent output before acceptance |

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

## Workflow Pipeline

1. Capture ideas with `/idea "my feature concept"`
2. Promote to tasks with `/promote idea-filename`
3. Mark complete with `/complete task-filename`
4. Check progress with `/status`

## Customization

Run `/setup` inside Claude Code for an interactive walkthrough, or edit `CLAUDE.md` directly. Look for `TODO:` comments marking customization points.

## Prior Art

Built on patterns from the Claude Code community. See [awesome-claude-code](https://github.com/hesreallyhim/awesome-claude-code) for more resources.

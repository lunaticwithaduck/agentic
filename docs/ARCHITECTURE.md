# Architecture

## Overview

**agentic** is a reusable, AI-first project infrastructure for Claude Code. It provides a
ready-to-clone template with skills, hooks, agents, slash commands, a workflow pipeline,
and a self-measuring benchmark suite. It is not a runtime application — it is a collection
of configuration, prompt engineering, and shell scripts that enhance Claude Code's behavior
when present in a project directory.

## Components

| Component | Description | Location |
|-----------|-------------|----------|
| Skills | 65 markdown expertise files with YAML frontmatter, each defining a domain-specific framework Claude can activate | `.claude/skills/*.md` |
| Skill Rules | JSON mapping of keywords, file patterns, and tool triggers for each skill | `.claude/skills/skill-rules.json` |
| Hooks | Shell scripts that run on Claude Code lifecycle events (prompt submit, tool use, stop) | `.claude/hooks/*.sh` |
| Agents | 8 specialized subagent role definitions (PM, architect, worker, auditor, etc.) | `.claude/agents/*.md` |
| Commands | 10 slash command definitions for workflow management and tooling | `.claude/commands/*.md` |
| Workflow Pipeline | Three-stage idea-to-done pipeline tracked via markdown files | `workflows/{ideas,tasks,done}/` |
| Bench Suite | Self-measuring benchmark with 5 test suites, fixtures, and metrics tracking | `bench/` |
| Setup Scripts | Interactive setup, validation, MCP configuration, and Playwright scaffolding | `.claude/scripts/*.sh` |

## Data Flow

```
User Prompt
    │
    ▼
┌──────────────────────┐
│  UserPromptSubmit     │  skill-detector.sh injects skill evaluation protocol
│  Hook                 │  into Claude's context before every response
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Claude Code          │  Evaluates all 65 skills against the prompt,
│  (with skill context) │  activates 1-3 most relevant, then responds
└──────────┬───────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
┌─────────┐  ┌──────────┐
│ Bash    │  │ Write/   │
│ Command │  │ Edit     │
└────┬────┘  └────┬─────┘
     │            │
     ▼            ▼
┌─────────┐  ┌──────────┐
│PreToolUse│  │PostToolUse│  block-secrets.sh validates commands;
│Hook      │  │Hook       │  post-write.sh checks for hardcoded secrets
└──────────┘  └───────────┘
```

### Workflow Pipeline Flow

```
/idea "feature X"          /promote idea-file          /complete task-file
       │                          │                           │
       ▼                          ▼                           ▼
workflows/ideas/           workflows/tasks/            workflows/done/
  2026-02-25-feature-x.md   2026-02-25-feature-x.md    2026-02-25-feature-x.md
  status: idea               status: task                status: done
                              + acceptance criteria       + completed date
                              + priority                  + outcome notes
```

## Tech Stack

- **Language**: Bash (hooks, bench runner, setup scripts), Python 3 (bench analysis, metrics, E2E comparison)
- **Framework**: Claude Code hooks and commands system
- **Configuration**: JSON (settings, skill rules, fixtures), Markdown with YAML frontmatter (skills, agents, commands)
- **Infrastructure**: Local filesystem — no server, no database, no build step

## Key Design Decisions

| Decision | Rationale | Date |
|----------|-----------|------|
| Pure bash hooks (no Node/Python dependencies) | Hooks run on every prompt — must be fast with zero startup cost | 2026-02 |
| Keyword-based skill detection over semantic matching | Deterministic, testable, no API calls needed; achieves 93% F1 | 2026-02 |
| Separate `skill-rules.json` from skill `.md` files | Rules need machine parsing; skills need human-readable content | 2026-02 |
| Numbered bench suites auto-discovered by glob | Easy to add new suites without modifying the runner | 2026-02 |
| E2E comparison via direct API calls (no SDK) | Zero dependencies — `urllib` only; the project should be cloneable anywhere | 2026-02 |
| Metrics as JSON + human-readable Markdown snapshots | JSON for machine comparison; Markdown for git-diffable history | 2026-02 |
| Changelog with inline experiment notes | Documents what was tried, not just what changed — builds institutional knowledge | 2026-02 |

## Directory Structure

```
.
├── .claude/
│   ├── settings.json       # Hook configuration, tool permissions
│   ├── skills/             # 65 skill .md files + skill-rules.json
│   ├── hooks/              # 4 lifecycle hook scripts
│   ├── agents/             # 8 agent role definitions
│   ├── commands/           # 10 slash command definitions
│   └── scripts/            # Setup and validation utilities
├── bench/
│   ├── run.sh              # Benchmark runner (auto-discovers suites)
│   ├── suites/             # 5 numbered test suites
│   ├── fixtures/           # Labeled test data (prompts, commands)
│   ├── e2e/                # E2E quality comparison framework
│   ├── lib/                # Shared helpers (colors, metrics)
│   └── results/            # Metrics snapshots and changelog
├── docs/
│   ├── ARCHITECTURE.md     # This file
│   └── EVALUATION.md       # Project evaluation and improvement ideas
├── workflows/
│   ├── ideas/              # Stage 1: Raw ideas
│   ├── tasks/              # Stage 2: Actionable work items
│   └── done/               # Stage 3: Completed work
├── CLAUDE.md               # Claude Code project context (read on every session)
├── README.md               # Project overview and quick start
└── setup.sh                # Initial setup script
```

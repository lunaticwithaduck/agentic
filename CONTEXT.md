# Project Context — agentic

> Stable reference. Read SESSION.md first for current session state.

---

## What This Project Is

**agentic** — reusable AI-first infrastructure for Claude Code, Cursor, and GitHub Copilot.
Clone into any project: skills, hooks, agents, commands, workflow pipeline, and a benchmark suite.
Ships as pre-built distributions via `buildScripts/build.sh` → `ship/claude-code/`, `ship/cursor/`, `ship/copilot/`.

**Versioning:** `VERSION` file at repo root, stamped into `ship/*/.version` on every build. `CHANGELOG.md` tracks releases.

---

## The Core Loop

Skills ship as `skill-creator` only at install. Domain skills grow from the user's own work:

1. Claude self-completes work → writes `.sc` (skill candidate) to `workflows/done/`
2. `post-write.cjs` counts `.sc` files by domain → sets `autolearn-pending` at N=3
3. `skill-detector.cjs` injects synthesis instructions on next prompt
4. Claude synthesizes skill, updates `skill-rules.json`, runs Suite 02 regression
5. Library grows with skills specific to this project's domain

**Design principle:** Skills inject facts/patterns/standards Claude doesn't have reliably
(OWASP classes, SQL patterns, WCAG criteria, Dockerfile practices).
They do NOT encode process Claude already knows.

**Skill format:** YAML frontmatter + `## Purpose` + `## Instructions` + `## Failure Modes`
(`## Failure Modes` populated only from observed failures, never inferred).

---

## Multi-Platform Ship

```
ship/
  claude-code/   — 4 hooks, skill-creator + skill-rules.json, 8 agents,
                   11 commands, CLAUDE.md, setup.sh
  cursor/        — .cursor/hooks/ (5 hooks), .cursor/rules/ (.mdc files),
                   11 commands, hooks.json, setup.sh
  copilot/       — .github/hooks/ (4 hooks), .github/skills/skill-creator/,
                   skill-rules.json, copilot-instructions.md,
                   workflow-gate.instructions.md, 11 prompts, 8 agents, setup.sh
```

Rebuild: `bash buildScripts/build.sh`

### Hook Architecture

All hook logic in `.cjs` files (CommonJS — works regardless of `"type": "module"`). `.sh` files are thin shims.

```
.claude/hooks/
  skill-detector.cjs  — UserPromptSubmit: keyword match → skill injection + synthesis trigger
  post-write.cjs      — PostToolUse: JSON validation + .sc domain counting → autolearn-pending
  block-secrets.cjs   — PreToolUse: secrets guard (platform-aware output format)
  post-stop.cjs       — Stop: writes SESSION.md with mechanical session state
  *.sh                — shims: exec node "$(dirname ...)/hook.cjs"
```

`post-write.cjs` and `block-secrets.cjs` are platform-aware: detect `.cursor`, `.github`,
or `.claude` in `__dirname` and behave accordingly.

### Cursor 3-Layer Skill Injection

Cursor's `beforeSubmitPrompt` cannot inject context (confirmed API gap). Workaround:

| Layer | Mechanism | Reliability |
|-------|-----------|-------------|
| 1 | `skill-index.mdc` always in context — lists available skills + keywords | Deterministic |
| 2 | `agentRequested` rules per skill — model requests when relevant | Probabilistic |
| 3 | `cursor-skill-injector.cjs` afterFileEdit — glob matches → injects | Deterministic |

### Platform-Specific Build Overrides

```
buildScripts/src/cursor-commands/   — cursor-specific command variants (.claude/ → .cursor/)
buildScripts/src/cursor-hooks/      — cursor-skill-injector.cjs, cursor-session-start.cjs
buildScripts/src/cursor-rules/      — agent-instructions.mdc, workflow-gate.mdc
buildScripts/src/copilot-commands/  — copilot-specific command variants (.claude/ → .github/)
buildScripts/src/copilot-agents/    — copilot agent variants (no Claude Code-only tools)
buildScripts/src/copilot-hooks/     — Copilot UserPromptSubmit hook source
buildScripts/src/copilot-rules/     — copilot-instructions.md, workflow-gate.instructions.md
```

---

## Benchmark Suite (7 suites)

| Suite | What it tests | Threshold |
|-------|--------------|-----------|
| 01 — Infrastructure | Dirs, hooks, skills, copilot + cursor ship structure | All checks pass |
| 02 — Skill Detection | Precision/recall of keyword auto-detection | F1 ≥ 70%, P ≥ 70% |
| 03 — Hook Security | Block/allow corpus for secrets hook | 0 false positives |
| 04 — Task Quality | E2E: with-infra vs vanilla (LLM judge) | Win rate ≥ 60%, avg delta > 0 |
| 05 — Keyword Overlap | Skill keyword duplication | < 10% dup rate |
| 06 — Token Cost | Output token overhead | Measured, not gated |
| 07 — Skill Candidating | Autolearn pipeline integrity (deterministic) | 12/12 |

Run: `bash bench/run.sh` | Single: `bash bench/run.sh --suite=04`
Suites 04 + 06 require external terminal: `env -u CLAUDECODE bash bench/run.sh --suite=04`

---

## Key File Paths

```
VERSION                              — current version
CHANGELOG.md                         — release history
SESSION.md                           — volatile session state (Stop hook + Claude)
buildScripts/build.sh                — builds all three ship targets
buildScripts/lib/build-claude-code.sh / build-cursor.sh / build-copilot.sh
buildScripts/lib/convert-skill.js    — skill format → Cursor .mdc rule
buildScripts/lib/convert-to-prompt.js — commands/*.md → .github/prompts/*.prompt.md
buildScripts/lib/convert-to-agent.js  — agents/*.md → .github/agents/*.agent.md
.claude/skills/                      — 28 skill .md files (internal; skill-creator ships)
.claude/skills/skill-rules.json      — keyword triggers for all skills
.claude/hooks/*.cjs                  — hook implementations (CommonJS, platform-aware)
.claude/hooks/*.sh                   — shims
.claude/commands/complete.md         — step 8: negative signal question + .sc generation
.claude/commands/                    — 11 slash command definitions
.claude/agents/                      — 8 agent role definitions
bench/e2e/tasks.json                 — 10 E2E task definitions (eq01–eq10)
bench/fixtures/skill-prompts.json    — detection fixtures (auto-appended on synthesis)
bench/suites/07-skill-candidating.sh — 12 deterministic autolearn pipeline tests
workflows/tasks/                     — active tasks (empty between sessions is normal)
workflows/done/                      — completed work + .sc skill candidates
workflows/problems/                  — open design problems
workflows/ideas/                     — backlog ideas
```

---

## Workflow Convention

1. Create task file in `workflows/tasks/` before multi-step implementation work
2. Implement one task at a time
3. `/complete [file]` → moves to `workflows/done/` with outcome + optional `.sc`

**Agents self-complete** — create tasks before starting, complete when done.

**What needs a task file:** Multi-step implementation work only.
Single-file writes (problem files, SESSION.md updates, CONTEXT.md edits) do NOT need task tracking.

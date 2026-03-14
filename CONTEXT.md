# Session Context

> Load this file at the start of a new session for full continuity.
> Last updated: 2026-03-14 (session 3)

---

## What This Project Is

**agentic** — reusable AI-first infrastructure for Claude Code, Cursor, and GitHub Copilot.
Clone into any project for instant AI setup: 28 domain-specific skills, 4 hooks, 8 agents, 11
commands, workflow pipeline, and a 7-suite benchmark. Ships as pre-built distributions for
three platforms via `buildScripts/build.sh` → `ship/claude-code/`, `ship/cursor/`, `ship/copilot/`.

**Version: 0.1.1** — adds GitHub Copilot as a third ship target. `VERSION` file at repo root,
stamped into `ship/*/.version` on every build. `CHANGELOG.md` at repo root tracks releases.

---

## Current State of the Skill Library

**28 skills total** — 27 domain-specific + `skill-creator` (meta). The 28th skill
(`e2e-evaluation`) was the first auto-generated skill, synthesized by the autolearn
pipeline on 2026-03-07 from 3 `.sc` files capturing LLM-judge benchmark insights.

Mass-deleted 39 generic methodology skills (debug, refactor, code-review, test-writer, etc.)
after confirming they LIMIT Claude rather than expand capability. Claude already knows
methodology; skills should inject domain knowledge it lacks reliably.

**Design principle**: Skills inject facts, patterns, standards Claude doesn't have reliably
(OWASP classes, SQL indexing patterns, WCAG criteria, Dockerfile best practices).
They do NOT encode process Claude already knows.

**Skill format includes `## Failure Modes` section** — populated only from observed
failures, never inferred. Empty if no real failures documented yet.

Categories: Security(4), Data(4), Backend(3), DevOps(6), Frontend(4), Content(2),
Utilities(3), Design(1), E2E Evaluation(1).

---

## Multi-Platform Ship (2026-03-14)

agentic ships to three platforms. Pre-built distributions in `ship/`:

```
ship/
  claude-code/   — full infra: 4 hooks, skill-creator + skill-rules.json, 8 agents,
                   11 commands, CLAUDE.md, setup.sh
  cursor/        — .cursor/hooks/ (5 hooks), .cursor/rules/ (agent-instructions.mdc,
                   skill-index.mdc, skill-creator.mdc), 11 commands, hooks.json, setup.sh
  copilot/       — .github/hooks/ (4 hooks), .github/skills/skill-creator/, skill-rules.json,
                   copilot-instructions.md, workflow-gate.instructions.md, 11 prompts,
                   8 agents, setup.sh  [33 files]
```

Rebuild: `bash buildScripts/build.sh`

### Hook Architecture

All hook logic lives in `.cjs` files (CommonJS explicitly — works in any project
regardless of `"type": "module"` in package.json). `.sh` files are thin shims.

```
.claude/hooks/
  skill-detector.cjs    — UserPromptSubmit: keyword matching → skill injection + synthesis
  post-write.cjs        — PostToolUse: JSON validation + .sc domain counting
  block-secrets.cjs     — PreToolUse: secrets guard (platform-aware: JSON output on Cursor)
  post-stop.cjs         — Stop hook placeholder
  *.sh                  — shims: exec node "$(dirname ...)/hook.cjs"
```

`post-write.cjs` and `block-secrets.cjs` are platform-aware: they detect `.cursor`, `.github`,
or `.claude` in `__dirname` and behave accordingly (different output format, different paths).
`post-write.cjs` writes `autolearn-pending` to whichever of `.cursor/`, `.github/`, or `.claude/`
matches the install — all three platforms are handled.

### Cursor 3-Layer Skill Injection

Cursor's `beforeSubmitPrompt` cannot inject context (confirmed API gap). Workaround:

| Layer | Mechanism | Reliability |
|-------|-----------|-------------|
| 1 | `skill-index.mdc` always in context — lists all available skills + keywords | Deterministic |
| 2 | `agentRequested` rules per skill — model requests when relevant | Probabilistic |
| 3 | `cursor-skill-injector.cjs` afterFileEdit — matches file against rule `globs:`, injects | Deterministic |

`skill-index.mdc` lists **only shipped rules** (just skill-creator at install, grows via
autolearn). The old bug (listing all 28 skills when only skill-creator shipped) is fixed.

### Cursor-Specific Hooks

```
buildScripts/src/cursor-hooks/
  cursor-skill-injector.cjs  — afterFileEdit: scans .cursor/rules/*.mdc globs, injects matches
  cursor-session-start.cjs   — sessionStart: checks .cursor/autolearn-pending, injects synthesis
```

### Platform Parity Doc

`ship/PLATFORM-PARITY.md` — honest comparison table of Claude Code vs Cursor features.

---

## Autolearn Pipeline (Skill Candidating + Decay)

### Skill Candidating (.sc files) — LIVE VALIDATED

1. `/complete` generates `.sc` alongside done file when domain knowledge was applied
2. `post-write.cjs` detects `.sc` writes, counts by domain, flags at N=3 by writing
   `{.claude|.cursor|.github}/autolearn-pending` (platform-aware — all three platforms)
3. `skill-detector.cjs` (Claude Code / Copilot) or `cursor-session-start.cjs` / `cursor-skill-injector.cjs`
   (Cursor) injects synthesis instructions on next prompt/session/file-edit
4. Claude writes skill, updates skill-rules.json, writes fixtures, runs Suite 02,
   clears flag, appends entry to skill-index.mdc

**Live validation (2026-03-07):** First full cycle: 3 `e2e-evaluation` .sc files →
synthesis → skill written → 5 fixture prompts → Suite 02 precision held at 98.1%.

### Skill Decay — IMPLEMENTED

Usage tracking via `.claude/skill-usage.json` (gitignored).
`/clean` reports stale (90+ days) and never-fired skills. `/status` shows Skill Health.

### Negative Signal Gap — PARTIALLY ADDRESSED

Built: Failure Modes section in skill format, `/complete` negative question,
Suite 02 regression after synthesis. Rejected: keyword correction detection,
Stop hook logging. Deferred: `.sc-negative` pipeline.

---

## Skill Detection (Claude Code)

`skill-detector.cjs` (UserPromptSubmit): keyword match against `skill-rules.json` →
injects matched skill `.md` content as context. Deterministic, zero overhead when no match.

Verified by Suite 02 (precision/recall/F1). Latest: 98.1% precision.

---

## Benchmark Overview (7 suites)

| Suite | What it tests | Last result |
|-------|--------------|-------------|
| 01-infrastructure | File structure, hooks, skill coverage | 24/24 ✓ (2026-03-14) |
| 02-skill-detection | Precision/recall of skill auto-detection | 98.1% precision |
| 03-hook-security | Block/allow corpus for secrets hook | 89/89 ✓ |
| 04-task-quality | E2E quality: with-infra vs vanilla | 70% win rate, avg +0.334 (10 tasks) |
| 05-keyword-overlap | Skill keyword duplication | 100% |
| 06-token-cost | Output token overhead | avg 1.19x ratio |
| 07-skill-candidating | Autolearn pipeline integrity (deterministic) | 12/12 ✓ |

Run all: `bash bench/run.sh`
Run single: `bash bench/run.sh --suite=07`
Must run from a real terminal (not inside Claude Code) for suites 04 and 06.

**Suite 07 note:** sandbox in the test copies both `.sh` shims and `.cjs` implementations —
needed after the .js → .cjs rename. Both must be present for the sandbox to work.

---

## Suite 04 Latest Results (2026-03-07)

| Task | Skills | Winner | Δ score | Notes |
|------|--------|--------|---------|-------|
| eq01 Write unit tests | _(none)_ | INFRA | +0.17 | |
| eq02 Security review | security-audit | INFRA | **+0.50** | Fixed 2026-03-07 |
| eq03 Debug race condition | _(none)_ | vanilla | −0.13 | |
| eq04 Refactor messy code | _(none)_ | vanilla | −0.50 | Accepted, rubric issue |
| eq05 Caching strategy | caching-strategy | INFRA | +0.38 | |
| eq06 SQL optimization | sql-optimization | INFRA | **+1.63** | Domain skill huge win |
| eq07 Dockerfile hardening | dockerfile | INFRA | **+0.50** | Re-run fixed variance |
| eq08 Accessibility audit | accessibility-audit | INFRA | +0.30 | |
| eq09 Rate limiting | rate-limiting | — | — | Silent failure, open |
| eq10 CI/CD pipeline | ci-cd | INFRA | +0.88 | |

---

## /complete Command Convention

Done files keep their original task filename — no sequential NN- prefix (would cause
merge conflicts on teams). Date-based names are already unique.

Skill candidating: `/complete` asks the negative question ("did an existing skill give
wrong guidance?") and generates `.sc` file if domain knowledge was applied. `.sc` goes
to `workflows/done/[original-filename].sc`.

---

## Open Issues / Next Steps

**Open task:** `workflows/tasks/2026-03-14-copilot-port.md` — parent backlog item, pending
real-world VS Code hook validation and Suite 01 copilot structure checks.

**Known open problems (`workflows/problems/`):**
- `cursor-commands-not-adapted.md` — commands reference `.claude/` paths and subagent
  features; low severity but misleading. Fix: `buildScripts/src/cursor-commands/` overrides
- `cursor-port-limitations.md` — `beforeSubmitPrompt` cannot inject context (Cursor API gap)
- `cursor-workflow-gate-synthesis-conflict.md` — FIXED in session 3 (synthesisPending guard)
- `negative-signal-gap.md` — partially addressed
- `autolearn-quality-verification.md`, `autolearn-saturation.md`, `skill-decay-knowledge-staleness.md`,
  `skill-scope-boundary.md`, `bench-coupling-drift.md`

**Ideas (`workflows/ideas/`):**
- `build-and-bench-versioning.md` — internal build manifest (git sha + bench run linkage)
- `agentic-versioning.md` — user-facing semver; now at v0.1.1
- `cursor-session-start-stack-detection.md` — project scanning as a skill injection layer
- `2026-02-25-ci-workflow-for-bench.md`, `2026-03-01-federated-skill-commons.md`,
  `2026-02-28-self-improving-skill-library.md`

**Workflow enforcement (session 3 — 2026-03-14):**
Meta-principle validated in client testing: "Rules that describe exact file operations and
visible written output succeed; rules that describe intent fail."
Applied across all platforms:
- `workflow-gate.mdc` / `workflow-gate.instructions.md` — numbered mechanical file ops, hard constraint language
- `complete.md` step 8 — mandatory written .sc evaluation block, default GENERATE, synthesis trigger
- `cursor-session-start.cjs` — synthesisPending guard prevents conflicting directives (C1 fix)
- `cursor-skill-injector.cjs` — isWorkflowFile guard (workflows/ prefix) prevents false STOP on /complete (C2 fix)
- `agent-instructions.mdc` — mechanical completion steps replacing "run the complete command"

**Unresolved (Copilot):**
- UserPromptSubmit output format: plain text vs `{"systemMessage":"..."}` JSON — needs VS Code testing
- Suite 01 has no copilot structure checks yet

**eq09 silent failure** — subprocess timeout, empty result directory, not yet investigated.

---

## Key File Paths

```
VERSION                              — current version (0.1.1)
CHANGELOG.md                         — release history
ship/claude-code/                    — Claude Code distribution
ship/cursor/                         — Cursor distribution
ship/copilot/                        — GitHub Copilot distribution (new)
README.md                            — platform parity table (Claude Code vs Cursor vs Copilot)
buildScripts/build.sh                — builds all three ship targets (reads VERSION)
buildScripts/lib/build-claude-code.sh
buildScripts/lib/build-cursor.sh
buildScripts/lib/build-copilot.sh    — new
buildScripts/lib/convert-skill.js    — converts agentic skill format → Cursor rule format
buildScripts/lib/convert-to-prompt.js — converts .claude/commands/*.md → .github/prompts/
buildScripts/lib/convert-to-agent.js  — converts .claude/agents/*.md → .github/agents/
buildScripts/lib/generate-skill-index.js  — builds skill-index.md from shipped rules only
buildScripts/lib/generate-hooks-json.js   — builds .cursor/hooks.json
buildScripts/src/cursor-hooks/       — Cursor-specific hook sources (.cjs)
buildScripts/src/cursor-rules/       — agent-instructions.mdc, workflow-gate.mdc sources
buildScripts/src/cursor-setup.sh     — Cursor-specific setup.sh source
buildScripts/src/copilot-hooks/      — Copilot UserPromptSubmit hook source
buildScripts/src/copilot-rules/      — copilot-instructions.md, workflow-gate.instructions.md
buildScripts/src/copilot-setup.sh    — Copilot-specific setup.sh source
.claude/skills/                      — 28 skill .md files
.claude/skills/skill-rules.json      — keyword triggers for all skills
.claude/skill-usage.json             — runtime usage tracking (gitignored)
.claude/autolearn-pending            — synthesis flag (gitignored)
.claude/hooks/*.cjs                  — hook implementations (CommonJS, platform-aware)
.claude/hooks/*.sh                   — shims delegating to .cjs
.claude/commands/complete.md         — includes negative signal question in step 8
.claude/commands/                    — 11 slash command definitions
.claude/agents/                      — 8 agent role definitions
bench/fixtures/skill-prompts.json    — detection fixtures (auto-appended on synthesis)
bench/e2e/tasks.json                 — 10 E2E task definitions (eq01–eq10)
bench/suites/07-skill-candidating.sh — 12 deterministic autolearn pipeline tests
workflows/ideas/                     — ideas pending
workflows/tasks/                     — empty (all tasks complete)
workflows/done/                      — completed tasks (date-prefixed filenames)
workflows/problems/                  — open design problems
```

---

## Workflow Convention

Always use the pipeline for non-trivial tasks:
1. Create task file in `workflows/tasks/` before starting
2. Implement
3. `/complete [file]` → moves to `workflows/done/` with outcome notes + optional `.sc`

**Agents self-complete** — create tasks before starting, complete them when done,
do not wait for human to invoke either step.

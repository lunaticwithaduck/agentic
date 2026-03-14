# agentic

AI-first infrastructure for Claude Code, Cursor, and GitHub Copilot. Clone into any project
to get workflows, skills, hooks, and benchmarking out of the box — without allocating a human
resource to AI maintenance.

The core idea: teams should not have to manually configure AI behavior. agentic makes the
skill library self-improving, self-measuring, and self-correcting over time.

---

## The Three Pillars

### 1. Automatic Skill Detection

Claude doesn't reliably know specialized domain knowledge — Terraform patterns, SQL indexing
strategies, WCAG criteria, security vulnerability classes. Skills encode that knowledge and
inject it exactly when needed.

**Claude Code**: The `UserPromptSubmit` hook runs on every prompt. It matches the prompt text
deterministically against keyword rules in `skill-rules.json` and injects the relevant skill
content directly as context — zero overhead if nothing matches, no AI evaluation step.

```
User prompt → keyword match → skill injected → Claude responds with domain expertise
```

**GitHub Copilot (VS Code agent mode)**: Same deterministic hook as Claude Code — a
`UserPromptSubmit` hook (`skill-detector.cjs`) runs on every prompt, keyword-matches against
`skill-rules.json`, and injects matched skills as context. Skill content lives in
`.github/skills/<name>/SKILL.md` (directory-based format).

**Cursor**: A three-layer approach compensates for Cursor's lack of per-prompt context injection:

| Layer | Mechanism | Reliability |
|-------|-----------|-------------|
| 1 | `skill-index.md` always in context — model knows which skills exist and when to use them | Deterministic |
| 2 | `agentRequested` rules per skill — model requests the full skill content when relevant | Probabilistic |
| 3 | `cursor-skill-injector.js` hook — after any file edit, matches the file path against skill `globs`, injects content automatically | Deterministic |

28 skills ship out of the box across security, data, backend, DevOps, frontend, and content
domains. Domain skills are auto-generated from completed work via autolearn — you start with
just `skill-creator` and grow the library as you work.

### 2. Autolearning — the Self-Improving Skill Library

The skill library is not static. As work gets done, domain knowledge accumulates and the
library grows to reflect what this team actually works on.

**Skill Candidating (`.sc` files)**

When a task is completed, Claude optionally emits a `.sc` (skill candidate) file in
`workflows/done/` alongside the regular completion record. A `.sc` file captures the
domain-specific knowledge applied in that task — not methodology, but concrete patterns
and standards worth encoding.

```
workflows/done/
  2026-03-01-optimize-slow-queries.md      ← task completion
  2026-03-01-optimize-slow-queries.sc      ← skill candidate: postgres indexing patterns
```

Skill candidates accumulate. When the same domain appears in N `.sc` files, the autolearn
system synthesizes them into an actual skill and adds it to the library automatically. One
occurrence might be a one-off. Three occurrences is a real pattern.

**Skill Decay**

Skills written today may be wrong 6 months from now. Frameworks evolve, APIs deprecate,
security patches land. The autolearn loop tracks freshness — when a `.sc` file for an
existing domain conflicts with the current skill, it proposes an update rather than a
new skill. Stale skills surface in `/status` output.

> **Status:** Autolearning is live and validated end-to-end. The full cycle — `.sc` candidating
> → domain threshold → synthesis injection → skill written → Suite 02 regression check — has
> been confirmed in production. The first auto-generated skill (`e2e-evaluation`) was synthesized
> from three `.sc` files. Skill decay tracking is also active via `/status` and `/clean`.

### 3. Benchmarking AI Infrastructure

The only way to know if the skill library is actually helping is to measure it. agentic
ships with a benchmark suite that compares Claude's output quality with and without the
infrastructure active.

Seven suites:

| Suite | What it measures |
|-------|-----------------|
| 01 | Infrastructure integrity (hooks, skills, commands all present and valid) |
| 02 | Skill detection accuracy (precision, recall, F1 against labelled prompts) |
| 03 | Hook security (secrets blocked, exit codes correct) |
| 04 | E2E task quality (A/B: with-infra vs. without, Claude-as-judge scoring) |
| 05 | Keyword overlap (no two skills claim the same trigger) |
| 06 | Token cost (output token delta: does the infra make responses more efficient?) |
| 07 | Skill candidating (autolearn pipeline integrity — `.sc` detection, synthesis injection, fixture format) |

Run all suites:

```bash
bash bench/run.sh
```

Run a single suite:

```bash
bash bench/run.sh --suite=04
```

> Suites 04 and 06 spawn real Claude subprocesses. Run these from a terminal, not inside
> Claude Code.

---

## The Workflows Directory

`workflows/` is a three-stage pipeline for managing work:

```
workflows/
  ideas/     ← raw ideas, not yet actionable
  tasks/     ← promoted ideas with acceptance criteria, ready to implement
  done/      ← completed work with outcome notes (and .sc skill candidates)
  problems/  ← known issues and open design questions
```

Slash commands manage the pipeline:

| Command | What it does |
|---------|--------------|
| `/idea [title]` | Capture a new idea |
| `/promote [file]` | Promote an idea to an actionable task |
| `/complete [file]` | Mark a task done, emit a `.sc` if applicable |
| `/status` | See the full pipeline at a glance |

This directory is also where the autolearn system reads its signal. Every `workflows/done/`
file is potential training data for the skill library.

---

## Setup

Pre-built distributions live in `ship/`:

```
ship/
  claude-code/   ← ready to drop into any project using Claude Code
  cursor/        ← ready to drop into any project using Cursor
  copilot/       ← ready to drop into any project using GitHub Copilot (VS Code)
```

**Claude Code:**

```bash
cp -r ship/claude-code/.claude your-project/
cp -r ship/claude-code/workflows your-project/
cp ship/claude-code/CLAUDE.md your-project/
cp ship/claude-code/setup.sh your-project/
cd your-project && bash setup.sh
```

**Cursor:**

```bash
cp -r ship/cursor/.cursor your-project/
cp -r ship/cursor/workflows your-project/
cp ship/cursor/setup.sh your-project/
cd your-project && bash setup.sh
```

**GitHub Copilot:**

```bash
cp -r ship/copilot/.github your-project/
cp -r ship/copilot/workflows your-project/
cp ship/copilot/setup.sh your-project/
cd your-project && bash setup.sh
```

Or clone the repo and use it as a template, then copy the appropriate `ship/` contents into
your project root.

After setup, open your AI assistant and run `/setup` to personalize the infrastructure for
your project's stack.

To rebuild distributions from source:

```bash
bash buildScripts/build.sh
```

---

## Current Status

All benchmark suites pass. The autolearning loop is live — skills grow automatically
from completed work without manual intervention. See `workflows/problems/` for known open
design questions.

Benchmark results (latest full run, 2026-03-07, 10 E2E tasks):
- Suite 01 (infrastructure): 24/24 ✓
- Suite 07 (autolearn pipeline): 12/12 ✓
- Skill detection: 98.1% precision (Suite 02)
- E2E quality: 8/10 tasks with-infra wins, average delta +0.334 (scale 0–5)

Platform support:

| Feature | Claude Code | Cursor | GitHub Copilot |
|---------|-------------|--------|----------------|
| Skill detection | ✅ Deterministic (UserPromptSubmit keyword match) | ⚠️ 3-layer (index + agentRequested + glob afterFileEdit) | ✅ Deterministic (UserPromptSubmit keyword match) |
| Skill injection | ✅ Full content injected as context | ✅ Full content injected after file edit | ✅ Full content injected as context |
| Workflow enforcement | ✅ Hook + CLAUDE.md | ✅ Hook + workflow-gate.mdc (alwaysApply) | ✅ Hook + workflow-gate.instructions.md |
| Autolearn pipeline | ✅ Full cycle | ✅ Full cycle (sessionStart + afterFileEdit) | ✅ Full cycle (UserPromptSubmit) |
| Commands | ✅ 11 slash commands | ✅ 11 slash commands | ✅ 11 prompt files |
| Agents | ✅ 8 subagent definitions | ❌ No native agent support | ✅ 8 agent files |
| Secrets blocking | ✅ PreToolUse hook | ✅ beforeShellExecution hook | ✅ PreToolUse hook |
| Hooks verified | ✅ | ✅ | ⚠️ Pending VS Code integration test |

---

## Contributing

agentic is designed to improve itself. If you build something useful, the `.sc` candidating
system is meant to eventually surface it as a skill for everyone. For now, PRs welcome.

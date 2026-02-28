# agentic

AI-first infrastructure for Claude Code. Clone into any project to get workflows, skills,
hooks, and benchmarking out of the box — without allocating a human resource to AI maintenance.

The core idea: teams should not have to manually configure AI behavior. agentic makes the
skill library self-improving, self-measuring, and self-correcting over time.

---

## The Three Pillars

### 1. Automatic Skill Detection

Claude doesn't reliably know specialized domain knowledge — Terraform patterns, SQL indexing
strategies, WCAG criteria, security vulnerability classes. Skills encode that knowledge and
inject it exactly when needed.

The `UserPromptSubmit` hook runs on every prompt. It reads the prompt text, matches it
deterministically against keyword rules in `skill-rules.json`, and injects the relevant
skill content directly as context — zero overhead if nothing matches, no AI evaluation step.

```
User prompt → keyword match → skill injected → Claude responds with domain expertise
```

26 skills ship out of the box across security, data, backend, DevOps, frontend, and content
domains. See `.claude/skills/` and `.claude/skills/skill-rules.json`.

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

> **Note:** Autolearning is in active development. The architecture is designed, the `.sc`
> format is being finalized, and the synthesis hook is upcoming. See `workflows/ideas/` for
> the current state of the work.

### 3. Benchmarking AI Infrastructure

The only way to know if the skill library is actually helping is to measure it. agentic
ships with a benchmark suite that compares Claude's output quality with and without the
infrastructure active.

Six suites:

| Suite | What it measures |
|-------|-----------------|
| 01 | Infrastructure integrity (hooks, skills, commands all present and valid) |
| 02 | Skill detection accuracy (precision, recall, F1 against labelled prompts) |
| 03 | Hook security (secrets blocked, exit codes correct) |
| 04 | E2E task quality (A/B: with-infra vs. without, Claude-as-judge scoring) |
| 05 | Keyword overlap (no two skills claim the same trigger) |
| 06 | Token cost (output token delta: does the infra make responses more efficient?) |

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

```bash
git clone https://github.com/your-org/agentic
cd your-project
cp -r agentic/.claude .
cp -r agentic/workflows .
cp agentic/CLAUDE.md .
bash agentic/setup.sh
```

Or use agentic as a template repository and clone directly into your project root.

`setup.sh` will:
- Create `workflows/` directories
- Verify `.claude/` structure and set hook permissions
- Check that Claude Code is installed
- Walk you through optional MCP server setup

After setup, open Claude Code and run `/setup` to personalize the infrastructure for your
project's stack.

---

## Current Status

The core infrastructure (skill detection, hooks, benchmark suites 01-06) is stable and
tested. The autolearning loop (`.sc` candidating, synthesis, decay) is in active design —
see `workflows/ideas/` for the work in progress and `workflows/problems/` for known open
questions.

Benchmark results (as of 2026-03-01, 10 E2E tasks):
- 7/10 tasks: with-infra wins
- Average quality delta: +0.334 (scale 0-5)
- Skill detection F1: passing

---

## Contributing

agentic is designed to improve itself. If you build something useful, the `.sc` candidating
system is meant to eventually surface it as a skill for everyone. For now, PRs welcome.

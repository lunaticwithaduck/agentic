# Session Context

> Load this file at the start of a new session for full continuity.
> Last updated: 2026-03-07

---

## What This Project Is

**agentic** — reusable AI-first infrastructure for Claude Code. Clone into any project
for instant Claude setup: 27 domain-specific skills, 4 hooks, 8 agents, 11 commands,
workflow pipeline, and a 7-suite benchmark.

---

## Current State of the Skill Library

**27 skills total** — 26 domain-specific + `skill-creator` (meta).

Mass-deleted 39 generic methodology skills (debug, refactor, code-review, test-writer, etc.)
after confirming they LIMIT Claude rather than expand capability. Claude already knows
methodology; skills should inject domain knowledge it lacks reliably.

**Design principle**: Skills inject facts, patterns, standards Claude doesn't have reliably
(OWASP classes, SQL indexing patterns, WCAG criteria, Dockerfile best practices).
They do NOT encode process Claude already knows.

**Skill format now includes `## Failure Modes` section** — populated only from observed
failures, never inferred. Empty if no real failures documented yet.

Categories: Security(4), Data(4), Backend(3), DevOps(6), Frontend(4), Content(2),
Utilities(3), Design(1).

---

## Autolearn Pipeline (Skill Candidating + Decay)

### Skill Candidating (.sc files) — IMPLEMENTED + EXTENDED

When `/complete` runs, Claude optionally generates a `.sc` (skill candidate) file alongside
the done file in `workflows/done/`. The `.sc` captures domain-specific knowledge applied
during the task — concrete patterns, facts, or standards worth encoding as a reusable skill.

**Architecture:**
1. `/complete` generates `.sc` alongside done file when domain knowledge was applied
   - Also asks: "Did an existing skill give wrong/incomplete guidance?" → captures failure
     modes in `.sc` or Outcome section (negative signal capture, 2026-03-07)
2. `post-write.sh` (PostToolUse) detects `.sc` writes, counts by domain, flags at N=3
   by writing `.claude/autolearn-pending`
3. `skill-detector.sh` (UserPromptSubmit) checks flag on next prompt, injects synthesis
   instructions so Claude generates the skill within normal conversation flow
4. Claude writes skill to `.claude/skills/`, updates `skill-rules.json`,
   writes fixture prompts to `bench/fixtures/skill-prompts.json`,
   runs Suite 02 precision regression check, flag is cleared

**Synthesis instruction steps (in skill-detector.sh):**
1. Read .sc files
2. Synthesize into skill file (include `## Failure Modes` if failures documented)
3. Write `.claude/skills/{domain}.md`
4. Add entry to `skill-rules.json`
5. Write 3–5 fixture prompts to `bench/fixtures/skill-prompts.json`
6. Delete `.claude/autolearn-pending`
7. Tell user skill was generated + fixture count
8. Run `bash bench/run.sh --suite=02`, compare precision to previous run,
   warn if precision dropped >5% (keyword pollution detection)

**Key files:**
- `.claude/commands/complete.md` — step 8 handles .sc generation + negative signal question
- `.claude/hooks/post-write.sh` — .sc detection and domain counting
- `.claude/hooks/skill-detector.sh` — synthesis injection + skill usage tracking

**Status:** Pipeline implemented, unit-tested (suite 07, 12/12), live validation pending.

### Skill Decay — IMPLEMENTED

Usage tracking via `.claude/skill-usage.json` (sidecar, gitignored):
- `skill-detector.sh` writes `{skill_name: {last_used, used_count}}` on every skill fire
- `/clean` reports stale skills (90+ days without firing) and never-fired skills
- `/status` shows Skill Health section (active/stale/never-fired counts)
- `/clean apply` offers to archive stale skills to `.claude/skills/archived/`

### Negative Signal Gap — PARTIALLY ADDRESSED (2026-03-07)

Five approaches evaluated via 4 independent subagents (architect, researcher, implementer,
auditor). Three built, two rejected, one deferred.

**Built:**
- Failure modes section in skill format (static, format-only)
- `/complete` now asks the negative question (human-annotated at highest-fidelity moment)
- Suite 02 precision regression after synthesis (automated, deterministic)

**Rejected:** Keyword correction detection (30–50% FP rate), Stop hook logging (no conversation access)

**Deferred:** `.sc-negative` full pipeline — revisit when first auto-generated skill exists
and `/complete` has produced a corpus of failure observations.

Full analysis + decision log: `workflows/problems/negative-signal-gap.md`

### Open Autolearn Problems (in `workflows/problems/`)

- **autolearn-quality-verification** — no cheap quality proxy for generated skills
- **autolearn-saturation** — loop runs indefinitely with diminishing returns
- **autolearn-cold-start** — no signal on fresh projects (mitigable)
- **negative-signal-gap** — partially addressed (see above)
- **skill-scope-boundary** — project-specific vs. general knowledge conflation
- **skill-decay-knowledge-staleness** — deeper issues beyond usage tracking

---

## Skill Detection — Deterministic Keyword Matching

`skill-detector.sh` (UserPromptSubmit hook) reads the prompt, matches against keywords
in `skill-rules.json`, and injects matched skill `.md` content directly as context.
No AI evaluation step — deterministic, zero overhead when no skills match.

Verified by suite 02 (precision/recall/F1 against fixture in `bench/fixtures/skill-prompts.json`).

---

## Benchmark Overview (7 suites)

| Suite | What it tests | Last result |
|-------|--------------|-------------|
| 01-infrastructure | File structure, hooks, skill coverage | 24/24 |
| 02-skill-detection | Precision/recall of skill auto-detection | high F1 |
| 03-hook-security | Block/allow corpus for secrets hook | 100% |
| 04-task-quality | E2E quality: with-infra vs vanilla | 70% win rate, avg +0.334 (10 tasks) |
| 05-keyword-overlap | Skill keyword duplication | 100% |
| 06-token-cost | Output token overhead | avg 1.19x ratio |
| 07-skill-candidating | Autolearn pipeline integrity (deterministic) | 12/12 |

Run all: `bash bench/run.sh`
Run single: `bash bench/run.sh --suite=07`
Must run from a real terminal (not inside Claude Code) for suites 04 and 06.

---

## Suite 04 Latest Results (2026-02-28, 10 tasks)

**7/10 infra wins · avg delta +0.334 · avg with-infra 4.329/5.0**

| Task | Skills | Winner | Δ score | Notes |
|------|--------|--------|---------|-------|
| eq01 Write unit tests | _(none)_ | INFRA | +0.17 | |
| eq02 Security review | security-audit, vuln-scan | INFRA | +0.25 | |
| eq03 Debug race condition | _(none)_ | vanilla | −0.13 | |
| eq04 Refactor messy code | _(none)_ | vanilla | −0.50 | Behavior-preservation failure |
| eq05 Caching strategy | caching-strategy | INFRA | +0.38 | |
| eq06 SQL optimization | sql-optimization | INFRA | **+1.63** | Domain skill huge win |
| eq07 Dockerfile hardening | dockerfile | vanilla | −0.25 | Missed HEALTHCHECK |
| eq08 Accessibility audit | accessibility-audit | INFRA | +0.30 | |
| eq09 Rate limiting | rate-limiting | INFRA | +0.63 | Fewer tokens too |
| eq10 CI/CD pipeline | ci-cd | INFRA | +0.88 | |

---

## Agent Self-Completion Convention (2026-03-07)

CLAUDE.md now includes convention 7: agents self-complete tasks without waiting for
human instruction. The workflow pipeline is agent-facing infrastructure — humans use
external tools (JIRA, Linear, etc.). When a task in `workflows/tasks/` is fully
implemented, run `/complete` autonomously.

---

## Open Issues / Next Steps

- **eq04 behavior-preservation failure** — structural, repeatable. Investigate root cause.
- **eq07 Dockerfile loss** — infra missed HEALTHCHECK. Check dockerfile skill content.
- **Suite 06 input tokens** — meaningless in subprocess mode. Needs API mode for real numbers.
- **Autolearn live validation** — moved to done/ (deferred, not blocked).
- **`.sc-negative` pipeline** — deferred until first auto-generated skill exists.

---

## Pipeline State

**Ideas (3):**
- CI workflow for bench (high, 2026-02-25)
- Federated Skill Commons (high, 2026-03-01)
- Self-improving skill library (medium, 2026-02-28)

**Tasks (0):** Clear.

**Done (9):**
- Suite 07 skill candidating pipeline (2026-03-07)
- Autolearn live validation (deferred/done, 2026-03-07)
- Bench infra isolated tempdir, Suite 04 expansion, Suite 06 token cost (2026-02-28)
- Deterministic skill prefilter, Skill candidating, Skill decay (2026-03-01)
- bench/CLAUDE.md guardrail (2026-02-26)

---

## Key File Paths

```
.claude/skills/               — 27 skill .md files (now with ## Failure Modes section)
.claude/skills/skill-rules.json — keyword triggers for all skills
.claude/skill-usage.json      — runtime usage tracking (gitignored)
.claude/autolearn-pending     — synthesis flag (gitignored)
.claude/hooks/skill-detector.sh — core: detection + usage tracking + synthesis injection
.claude/hooks/post-write.sh   — JSON validation + .sc domain counting
.claude/hooks/post-stop.sh    — Stop hook placeholder (no conversation access)
.claude/hooks/block-secrets.sh — PreToolUse secrets guard
.claude/commands/complete.md  — includes negative signal question in step 8
.claude/commands/              — 11 slash command definitions
.claude/agents/                — 8 agent role definitions
bench/fixtures/skill-prompts.json — detection fixtures (auto-appended on synthesis)
bench/e2e/tasks.json          — 10 E2E task definitions (eq01–eq10)
bench/e2e/compare.py          — Suite 04 engine
bench/e2e/token_compare.py    — Suite 06 engine
bench/suites/07-skill-candidating.sh — 12 deterministic autolearn pipeline tests
workflows/ideas/              — 3 ideas pending
workflows/tasks/              — 0 tasks pending
workflows/done/               — 9 completed tasks
workflows/problems/           — 6 open design problems
```

---

## Workflow Convention

Always use the pipeline for non-trivial tasks:
1. `/idea [title]` → creates in `workflows/ideas/`
2. `/promote [file]` → moves to `workflows/tasks/` with acceptance criteria
3. Implement
4. `/complete [file]` → moves to `workflows/done/` with outcome notes + optional `.sc`

**Agents self-complete** — do not wait for human to invoke `/complete`.

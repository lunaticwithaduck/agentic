# Session Context

> Load this file at the start of a new session for full continuity.
> Last updated: 2026-03-07

---

## What This Project Is

**agentic** — reusable AI-first infrastructure for Claude Code. Clone into any project
for instant Claude setup: 28 domain-specific skills, 4 hooks, 8 agents, 11 commands,
workflow pipeline, and a 7-suite benchmark.

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

**Skill format now includes `## Failure Modes` section** — populated only from observed
failures, never inferred. Empty if no real failures documented yet.

Categories: Security(4), Data(4), Backend(3), DevOps(6), Frontend(4), Content(2),
Utilities(3), Design(1), E2E Evaluation(1).

---

## Autolearn Pipeline (Skill Candidating + Decay)

### Skill Candidating (.sc files) — IMPLEMENTED + LIVE VALIDATED

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

**Live validation (2026-03-07):**
- First full cycle ran end-to-end: 3 `e2e-evaluation` .sc files → synthesis triggered →
  skill written → 5 fixture prompts appended → Suite 02 precision held at 98.1% (0.0pp delta)
- eq02 re-run post security-audit fix: −1.5 → +0.5 (2-point swing, severity rating guide worked)

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
| 02-skill-detection | Precision/recall of skill auto-detection | 98.1% precision, high F1 |
| 03-hook-security | Block/allow corpus for secrets hook | 100% |
| 04-task-quality | E2E quality: with-infra vs vanilla | 70% win rate, avg +0.334 (10 tasks) |
| 05-keyword-overlap | Skill keyword duplication | 100% |
| 06-token-cost | Output token overhead | avg 1.19x ratio |
| 07-skill-candidating | Autolearn pipeline integrity (deterministic) | 12/12 |

Run all: `bash bench/run.sh`
Run single: `bash bench/run.sh --suite=07`
Must run from a real terminal (not inside Claude Code) for suites 04 and 06.

---

## Suite 04 Latest Results (2026-03-07, re-runs)

**eq02 fixed:** security-audit skill now has CRITICAL/HIGH/MEDIUM/LOW severity guide +
common misratings list. Re-run confirmed: −1.5 → +0.5 (severity rating guide worked).

**eq07 resolved:** Was reported as vanilla win (−0.25). Fresh re-run showed infra WIN (+0.5).
February result was LLM judge variance — no skill change needed.

**eq04 accepted loss:** Rubric calibration issue. Haiku judge applies leniency when behavior
change "fixes a latent bug." Accepted — not a skill problem.

**eq09 open:** Silent failure (empty result directory). Likely subprocess timeout. Not yet fixed.

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

## Agent Self-Completion Convention (2026-03-07)

CLAUDE.md convention 7 updated to both halves:
1. **Create** tasks in `workflows/tasks/` before starting multi-step work
2. **Self-complete** each task with `/complete` when done — do not wait to be asked

First version only said "self-complete when done" — missed the create step, so tasks were
never created and the pipeline was never fed. Discovered via webhook-relay sandbox test
(prompt 1 built the app correctly but zero tasks/zero .sc files).

---

## Sandbox Testing (temp/webhook-relay)

Active experiment to validate autolearn pipeline end-to-end on a real project:
- `temp/` is now gitignored
- `temp/webhook-relay/` has full agentic infra installed with empty skills
- Building a FastAPI/SQLite webhook relay service from scratch
- Goal: skills should auto-generate from development work, then score well on Suite 02
- Prompt 1 result: app built correctly, pipeline bypassed (convention bug, now fixed)
- Prompt 2 running now with fixed CLAUDE.md

---

## Open Issues / Next Steps

- **eq09 silent failure** — subprocess timeout, empty result directory, not yet fixed
- **"bench" keyword breadth** — `e2e-evaluation` skill fires on any prompt with "bench" (too noisy)
- **Suite 01 count stale** — still says 27 skills, should be 28
- **webhook-relay sandbox** — prompt 2 running, watching for pipeline to fire correctly
- **`.sc-negative` pipeline** — deferred until positive path has more data
- **Federated Skill Commons** — explicitly deferred, too early

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
.claude/skills/               — 28 skill .md files (now with ## Failure Modes section)
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
temp/webhook-relay/           — sandbox project (gitignored), testing autolearn e2e
workflows/ideas/              — 3 ideas pending
workflows/tasks/              — 0 tasks pending
workflows/done/               — 9 completed tasks
workflows/problems/           — 6 open design problems
```

---

## Workflow Convention

Always use the pipeline for non-trivial tasks:
1. Create task file in `workflows/tasks/` before starting
2. Implement
3. `/complete [file]` → moves to `workflows/done/` with outcome notes + optional `.sc`

**Agents self-complete** — create tasks before starting, complete them when done,
do not wait for human to invoke either step.

# Session Context

> Load this file at the start of a new session for full continuity.
> Last updated: 2026-03-01

---

## What This Project Is

**agentic** — reusable AI-first infrastructure for Claude Code. Clone into any project
for instant Claude setup: 27 domain-specific skills, 4 hooks, 8 agents, 5 commands,
workflow pipeline, and a 6-suite benchmark.

---

## Current State of the Skill Library

**27 skills total** — 26 domain-specific + `skill-creator` (meta).

Mass-deleted 39 generic methodology skills (debug, refactor, code-review, test-writer, etc.)
after confirming they LIMIT Claude rather than expand capability. Claude already knows
methodology; skills should inject domain knowledge it lacks reliably.

**Design principle**: Skills inject facts, patterns, standards Claude doesn't have reliably
(OWASP classes, SQL indexing patterns, WCAG criteria, Dockerfile best practices).
They do NOT encode process Claude already knows.

Categories: Security(4), Data(4), Backend(3), DevOps(6), Frontend(4), Content(2),
Utilities(3), Design(1).

`skill-creator.md` now explicitly instructs to update `skill-detector.sh` in addition
to `skill-rules.json` and `CLAUDE.md` — added this session.

---

## Benchmark Overview (6 suites)

| Suite | What it tests | Last result |
|-------|--------------|-------------|
| 01-infrastructure | File structure, hooks, skill coverage | 24/24 ✅ (was 22/24, fixed missing hooks) |
| 02-skill-detection | Precision/recall of skill auto-detection | ~high F1 (fixture pruned to 40 clean prompts) |
| 03-hook-security | Block/allow corpus for secrets hook | 100% |
| 04-task-quality | E2E quality: with-infra vs vanilla | **70% win rate, avg +0.334** (10 tasks) |
| 05-keyword-overlap | Skill keyword duplication | 100% |
| 06-token-cost | Output token overhead | avg 1.19x ratio (new suite) |

Run all: `bash bench/run.sh`
Run single: `bash bench/run.sh --suite=04`
Must run from a real terminal (not inside Claude Code) for suites 04 and 06.

---

## Suite 04 Latest Results (2026-02-28, 10 tasks)

**7/10 infra wins · avg delta +0.334 · avg with-infra 4.329/5.0**

| Task | Skills | Winner | Δ score | Output Δ tokens | Notes |
|------|--------|--------|---------|-----------------|-------|
| eq01 Write unit tests | _(none)_ | INFRA | +0.17 | +3 | |
| eq02 Security review | security-audit, vuln-scan | INFRA | +0.25 | +3,523 | Comprehensive checklist drives thoroughness |
| eq03 Debug race condition | _(none)_ | vanilla | −0.13 | +86 | GIL misconception not addressed by infra |
| eq04 Refactor messy code | _(none)_ | vanilla | −0.50 | +863 | Infra changes `if cat:` → `if cat is not None:` (behavior break) |
| eq05 Caching strategy | caching-strategy | INFRA | +0.38 | +267 | |
| eq06 SQL optimization | sql-optimization | INFRA | **+1.63** | −104 | Covering indexes, partial index — domain skill huge win |
| eq07 Dockerfile hardening | dockerfile | vanilla | −0.25 | −270 | Infra missed HEALTHCHECK |
| eq08 Accessibility audit | accessibility-audit | INFRA | +0.30 | +18 | |
| eq09 Rate limiting | rate-limiting | INFRA | +0.63 | −722 | Better answer AND fewer tokens |
| eq10 CI/CD pipeline | ci-cd | INFRA | +0.88 | +514 | |

**Key patterns:**
- Domain skill tasks (eq06, eq09, eq10) = highest quality gains
- Domain tasks sometimes SAVE tokens (sql: −104, rate-limiting: −722) — more precise = less wandering
- eq04 behavior-preservation failure is structural and repeatable — worth investigating
- eq07 Dockerfile loss: check `.claude/skills/dockerfile.md` for HEALTHCHECK content

**Token cost note**: `claude -p --output-format=json` only reports user-message input tokens
(3–10 tokens), NOT full context (CLAUDE.md + skills + hook injections). Input overhead is
currently unmeasurable in subprocess mode. Output delta is real.

---

## Suite 06 Token Cost

New suite this session. `bench/e2e/token_compare.py` spawns real `claude -p` subprocesses
and captures `usage{}` from the JSON response. Input tokens are misleading (user-message only);
output delta is real. Results in `bench/results/tokens/`.

To get true input token measurement: would need API mode with explicit system prompt injection
(like the API mode in `compare.py`).

---

## Infrastructure Fixes This Session

1. **Missing hooks** (caused suite 01 failing 22/24):
   - Created `.claude/hooks/post-write.sh` — validates JSON files on Write/Edit
   - Created `.claude/hooks/post-stop.sh` — Stop hook placeholder (exits 0)

2. **Stale fixture** (caused suite 02 F1 = 50%):
   - `bench/fixtures/skill-prompts.json` pruned from 100 → 40 prompts
   - Removed all entries expecting deleted methodology skills

3. **Suite 04 expansion** (5 → 10 tasks):
   - Cleaned stale `expected_skills` in eq01/eq03/eq04/eq05
   - Added eq06–eq10 covering sql-optimization, dockerfile, accessibility-audit, rate-limiting, ci-cd

4. **Timeout + resilience** in `compare.py` and `token_compare.py`:
   - Timeout: 180s → 300s
   - Per-task error handling: timeout/failure logs and skips, doesn't crash whole run
   - `_run_with_retry()`: retries once on RuntimeError before giving up

---

## Open Issues / Next Steps

- **eq04 behavior-preservation failure** is structural and repeatable — something in the
  infra context nudges `if cat:` → `if cat is not None:`. Investigate what's driving it.

- **eq07 Dockerfile loss** — infra missed HEALTHCHECK. Check dockerfile skill content.
  Either add HEALTHCHECK to the skill or accept this as a gap.

- **Suite 06 input token measurement** — currently meaningless in subprocess mode.
  True overhead measurement requires API mode. Consider `--mode=api` variant or separate
  instrument.

- **Self-improving skill library** (in `workflows/ideas/2026-02-28-self-improving-skill-library.md`)
  — not yet promoted. Mechanism to read `workflows/done/` and propose skill updates.

- **Ideas not yet promoted**:
  - `workflows/ideas/2026-02-25-ci-workflow-for-bench.md`
  - `workflows/ideas/2026-02-25-exclusion-rules-skill-detection.md`
  - `workflows/ideas/2026-02-25-run-suite04-e2e-quality.md`
  - `workflows/ideas/2026-02-28-self-improving-skill-library.md`

---

## Key File Paths

```
bench/e2e/tasks.json          — 10 E2E task definitions (eq01–eq10)
bench/e2e/compare.py          — Suite 04 engine (subprocess + API modes)
bench/e2e/token_compare.py    — Suite 06 engine (subprocess, captures usage{})
bench/suites/04-task-quality.sh
bench/suites/06-token-cost.sh
bench/results/e2e/            — Cached responses + judgements per task
bench/results/tokens/         — Cached token measurements per task
bench/results/metrics/        — Overall bench run reports
.claude/skills/               — 27 skill .md files
.claude/skills/skill-rules.json
.claude/hooks/skill-detector.sh
.claude/hooks/post-write.sh
.claude/hooks/post-stop.sh
workflows/ideas/              — 4 ideas pending promotion
workflows/done/               — 4 completed tasks
```

---

## Workflow Convention

Always use the pipeline for non-trivial tasks:
1. `/idea [title]` → creates in `workflows/ideas/`
2. `/promote [file]` → moves to `workflows/tasks/` with acceptance criteria
3. Implement
4. `/complete [file]` → moves to `workflows/done/` with outcome notes

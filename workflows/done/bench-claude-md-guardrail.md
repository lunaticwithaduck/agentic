# Solution: bench/CLAUDE.md Coupling Guardrail

**Date completed:** 2026-02-26
**Solves:** `workflows/problems/bench-coupling-drift.md`

---

## What Was Built

`bench/CLAUDE.md` — a subdirectory CLAUDE.md that Claude Code auto-loads whenever you work on any file inside `bench/`.

## Why This Approach

Claude Code automatically surfaces CLAUDE.md files from the working directory and its parents when answering questions or making edits. A `bench/CLAUDE.md` is the lightest possible guardrail:

- Zero runtime cost (no new scripts or CI steps)
- Zero maintenance unless the coupling actually changes
- Automatically scoped — only active when editing bench files
- Forces the coupling knowledge to be explicit and findable

The alternative (a central `DEPS.json` registry, per-file dependency sections, or a new suite 06-coupling.sh) adds more infrastructure to maintain without meaningfully improving the signal.

## Contents of bench/CLAUDE.md

1. **File inventory** — one-line description of every bench file's role
2. **Coupling map** — "if you change X, check Y" for each tight coupling point:
   - metrics.sh JSON schema → report.py
   - compare.py `--json` output → 04-task-quality.sh
   - compare.py cache key names → cleanup.sh globs
   - Suite file names → report.py `SUITE_LABELS`
   - Skill library → tasks.json `expected_skills`
   - block-secrets.sh contract → 03-hook-security.sh
   - Agent/command counts → 01-infrastructure.sh thresholds
3. **Operation checklists** — step-by-step for common changes: adding/removing a suite, adding/removing a skill, changing JSON schemas
4. **Hardcoded thresholds reference** — table of every numeric threshold with file and line number
5. **Key gotchas** — non-obvious behaviors: suite 04 skips inside Claude Code, suites are `source`d not exec'd, `-sub` cache suffix, ANSI alignment, changelog insertion point

## Couplings Documented (Summary)

| Source | Dependents |
|--------|-----------|
| `metrics.sh` JSON schema | `report.py`, changelog rendering |
| `compare.py` output fields | `04-task-quality.sh` (8 fields parsed) |
| `compare.py` cache key names | `cleanup.sh` (4 glob patterns) |
| Suite file names | `report.py` SUITE_LABELS dict |
| `.claude/skills/*.md` | `tasks.json` expected_skills, suite 02 fixtures |
| `block-secrets.sh` exit/stdout | `03-hook-security.sh` pass/fail logic |
| Agent/command counts | `01-infrastructure.sh` floor thresholds |

## What This Does Not Solve

- It doesn't prevent drift mechanically — it surfaces coupling knowledge so the developer (or Claude) can't miss it.
- If `bench/CLAUDE.md` itself gets out of date, it becomes noise. Keep it accurate when you change the bench.
- Suite 06-coupling.sh (mechanical validation) was explicitly rejected in favor of this lighter approach. Reconsider if coupling drift remains a problem after a month of use.

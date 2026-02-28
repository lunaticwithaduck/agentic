---
title: Suite 04 task expansion — domain skill coverage
created: 2026-02-28
promoted: 2026-02-28
completed: 2026-02-28
status: done
author: user
tags: [bench, suite-04, e2e, skills]
priority: high
complexity: medium
---

# Suite 04 task expansion — domain skill coverage

## Description

Expand bench/e2e/tasks.json from 5 to ~10 tasks, covering more domain-specific skills
(sql-optimization, dockerfile, accessibility-audit, rate-limiting, ci-cd).
Also clean up stale `expected_skills` references to deleted methodology skills
(test-writer, debug, refactor, system-design, code-smell-detector) in the existing 5 tasks.

## Notes

- Current 5 tasks are too few for statistical significance (delta is within noise)
- eq01/eq03/eq04/eq05 have stale expected_skills pointing to deleted skills
- New tasks must cover skills from the 27-skill library (domain-specific only)

## Acceptance Criteria

- [x] Stale expected_skills cleaned from eq01, eq03, eq04, eq05
- [x] 5 new tasks added (eq06–eq10) covering: sql-optimization, dockerfile, accessibility-audit, rate-limiting, ci-cd
- [x] Each new task has a clear prompt, expected_skills, and weighted rubric
- [x] All tasks validated with dry-run (python3 bench/e2e/compare.py --dry-run)

## Outcome

Completed on 2026-02-28. tasks.json expanded from 5 to 10 tasks. Stale expected_skills
removed from eq01/eq03/eq04 (methodology skills deleted last session) and eq05 (system-design
removed, caching-strategy kept). Added eq06 (SQL optimization), eq07 (Dockerfile hardening),
eq08 (accessibility audit), eq09 (rate limiting), eq10 (CI/CD pipeline) — each with a
prompt, expected_skills referencing current skills, and a weighted multi-dimension rubric.
Validated with compare.py --dry-run: all 10 tasks load and run correctly.

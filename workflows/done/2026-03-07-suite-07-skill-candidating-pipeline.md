---
title: Suite 07 — Skill Candidating Pipeline
created: 2026-03-07
promoted: 2026-03-07
completed: 2026-03-07
status: done
author: user
tags: [benchmark, autolearn, skill-candidating, pipeline]
priority: high
complexity: medium
---

# Suite 07 — Skill Candidating Pipeline

## Description

A deterministic benchmark suite that tests the integrity of the autolearn pipeline end-to-end.
Suite 04 tests output quality; suite 07 tests whether the pipeline that builds the skill library
works correctly. These are independent concerns and warrant separate suites.

The suite uses fixtures (mock `.sc` files, mock tasks) so it runs without LLM calls — fast,
deterministic, runnable in CI.

## What to test

1. `post-write.sh` counts `.sc` files per domain correctly
2. Flag is NOT set before N=3 for a given domain
3. Flag IS set at exactly N=3
4. `skill-detector.sh` injects synthesis instructions when flag is present
5. `skill-detector.sh` does NOT inject synthesis when flag is absent
6. Synthesized skill lands in `.claude/skills/` with valid frontmatter
7. New skill entry appears correctly in `skill-rules.json`
8. New skill fires on a matching prompt via `skill-detector.sh`
9. Flag is cleared after synthesis

## Notes

- Deterministic — no LLM judge needed for steps 1–5, 7–9
- Steps 6/8 use fixture-based validation (check file format, check keyword firing)
- Consistent with suite 01 (infrastructure integrity) and suite 02 (detection accuracy) lineage
- Suite 04 and suite 07 answer orthogonal questions — keep them separate

## Acceptance Criteria

- [x] Suite runs with `bash bench/run.sh --suite=07` from a standard terminal
- [x] `post-write.sh` domain counting tested: 1 .sc = no flag, 2 .sc = no flag, 3 .sc = flag set
- [x] Negative case: 3 .sc files across 3 different domains do NOT set the flag
- [x] `skill-detector.sh` injects synthesis block when `autolearn-pending` is present
- [x] `skill-detector.sh` outputs nothing synthesis-related when flag is absent
- [x] Fixture-based: suite provides its own mock .sc files and cleans up after itself
- [x] Synthesized skill format validated: required frontmatter fields present and non-empty
- [x] skill-rules.json entry validated: keywords array non-empty, filePatterns present
- [x] New skill fires on a keyword-matching prompt via skill-detector.sh after synthesis
- [x] Passes cleanly on a fresh clone with no prior state

## Outcome

Completed on 2026-03-07. Implemented `bench/suites/07-skill-candidating.sh` with 12 deterministic tests covering the full autolearn pipeline: `post-write.sh` domain counting (tests 1–4), `skill-detector.sh` synthesis injection and flag lifecycle (tests 5–7), synthesized skill format validation (test 8), `skill-rules.json` entry validation (test 9), keyword firing on the newly registered skill (test 10), synthesis instruction referencing `bench/fixtures/skill-prompts.json` (test 11), and fixture entry format/append correctness (test 12). Suite runs in ~650ms with no LLM calls. Also updated `skill-detector.sh` to include fixture auto-generation instructions in the synthesis block, and updated `bench/CLAUDE.md` coupling map accordingly.

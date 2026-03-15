---
title: Misc cleanup — skill-rules filePatterns, SUITE_LABELS, CONTEXT.md
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
Three small independent fixes:
1. Fix skill-rules.json filePatterns .claude/ → .github/ transform in copilot build
2. Add suites 06 and 07 to SUITE_LABELS in bench/lib/report.py
3. Update CONTEXT.md to reflect session 4 work

## Steps
- [x] Read buildScripts/lib/build-copilot.sh — find where skill-rules.json is copied
- [x] Add a python3 transform step after the copy that replaces
      .claude/skills/ → .github/skills/ in the copied skill-rules.json
      (affects filePatterns entries, not skill keywords)
- [x] Read bench/lib/report.py — find SUITE_LABELS dict
- [x] Add entries for 06-token-cost and 07-skill-candidating
- [x] Also check suite_notes() function — add cases for 06 and 07 if they emit extra metrics
- [x] Read bench/suites/06-token-cost.sh and 07-skill-candidating.sh to check their SUITE_JSON
- [x] Update CONTEXT.md to reflect session 4 (2026-03-15)

## Outcome

### Item 1: skill-rules.json filePatterns transform
Added a `python3 -c` one-liner immediately after step 6 in `buildScripts/lib/build-copilot.sh`.
It reads the already-written `${SHIP_DIR}/.github/skills/skill-rules.json`, iterates all rules,
and rewrites each `filePatterns` entry replacing `.claude/skills/` with `.github/skills/`.
The source file at `.claude/skills/skill-rules.json` is untouched.

### Item 2: SUITE_LABELS + suite_notes
`bench/lib/report.py` changes:
- Added `"06-token-cost": "06 · Token Cost      "` to `SUITE_LABELS` (07 was already present)
- Added `suite_notes("06-token-cost")` case — reads `avg_cost_ratio`, `avg_input_overhead`,
  `avg_output_delta` from SUITE_JSON. Color-codes ratio: green <5x, yellow <10x, red ≥10x.
  Handles skipped state (suite 06 cannot run inside Claude Code).
- Suite 07 emits `SUITE_JSON="{}"` — no extra metrics, no `suite_notes` case needed.

### Item 3: CONTEXT.md
Updated CONTEXT.md for session 4 (2026-03-15):
- Version bumped to 0.1.2 (VERSION file was already at 0.1.2)
- New problem files noted: `skill-quality-benchmark-service.md`, `copilot-pretesting-findings.md`
- eq09 root cause documented and fixed: rate-limiting.md Output Format rewritten code-first;
  compare.py hardened with skipped_tasks[] tracking
- cursor-commands override added to Key File Paths and Multi-Platform Ship section
- Suite 01 cursor structure checks noted (18 added, session 4)
- Copilot BLOCKER issues section added with in-flight fix status
- Open Issues section updated: completed tasks removed, in-flight tasks listed
- Benchmark table updated: Suite 01 entry updated to reflect cursor structure checks
- Document kept under 280 lines

## .sc Evaluation

**Domain knowledge applied?** No specialized domain knowledge was required for these changes.
All three items were mechanical: a string substitution in a build script, adding dict entries
and a display function in Python, and updating a context document. No skill was activated
and no new domain insight was captured.

**Skill candidate:** SKIP — no domain knowledge applied, no .sc generated.

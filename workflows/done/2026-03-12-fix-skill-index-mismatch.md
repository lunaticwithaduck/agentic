---
title: Fix skill-index.md — only list skills that actually ship
status: done
created: 2026-03-12
completed: 2026-03-12
---

## Problem
build-cursor.sh generates skill-index.md from the full skill-rules.json (28 skills)
but only skill-creator.md ships as a rule. Model reads index, tries @security-audit,
gets nothing.

## Fix
Generate skill-index.md from only the rules present in the output .cursor/rules/
directory (skill-creator only at install time). Autolearn synthesis already appends
to skill-index.md as new skills are created.

## Outcome

Completed on 2026-03-12. Updated `build-cursor.sh` step 6 to dynamically filter
`skill-rules.json` to only include skills that have a corresponding `.md` file in
the output `rules/` directory before passing it to `generate-skill-index.js`. At
install time the index lists only `skill-creator`. As autolearn synthesizes domain
skills they get written to `.cursor/rules/` and appended to `skill-index.md` by the
synthesis instructions — the index stays in sync with reality.

---
title: Fix done/ file naming — drop sequential NN- prefix
status: done
created: 2026-03-12
completed: 2026-03-12
---

## Problem
/complete scans done/ for max NN- prefix and increments it. Two teammates working
in parallel both see the same max and create the same number — git conflict.

## Fix
Update /complete command: keep the original task filename when moving to done/,
no sequential prefix. Date-based names are already unique.

## Outcome

Completed on 2026-03-12. Removed the NN- sequential prefix logic from the /complete
command. Done files now keep their original task filename, which is already date-based
and unique. Also updated the .sc file reference from `[NN-stem].sc` to
`[original-filename].sc` for consistency.

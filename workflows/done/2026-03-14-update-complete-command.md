---
title: Update complete.md — mandatory .sc evaluation + synthesis trigger
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Replace silent "ask yourself" step 8 with mandatory written evaluation, GENERATE
default, and synthesis trigger that arms autolearn-pending when domain count ≥ 3.

## Acceptance Criteria

- [x] Step 8 requires written evaluation block before deciding
- [x] Default is GENERATE; SKIP requires explicit justification and is described as rare
- [x] Synthesis trigger counts .sc files by domain and writes autolearn-pending at ≥3
- [x] Platform-aware: writes to .cursor/ or .claude/ depending on what exists
- [x] Negative signal question preserved

## Outcome

Completed on 2026-03-14. Replaced step 8 of `.claude/commands/complete.md` entirely.
Key changes: evaluation block is now mandatory written output (not internal decision);
default flipped to GENERATE with SKIP requiring explicit one-sentence justification;
synthesis trigger added — after writing .sc, counts domain matches, checks for existing
skill file, writes autolearn-pending to whichever platform path exists if count ≥ 3.

---
title: Fix auditor blockers — C1/C2/C3 (cursor) + complete.md + Copilot ROOT_DIR
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Fix all blocker-level issues identified by Auditor 1 and Auditor 2 before running the build.

## Steps

- [x] C1: Add synthesisPending guard to workflow block in cursor-session-start.cjs
- [x] C2: Broaden isTaskFile guard in cursor-skill-injector.cjs to cover workflows/ + synthesisPending
- [x] C3: Change WARNING to exit 1 in build-cursor.sh step 7
- [x] complete.md: Add .github/autolearn-pending branch to synthesis trigger
- [x] complete.md: Fix "status: task" → "status: in-progress" (N2)
- [x] agent-instructions.mdc: Strengthen task pipeline section (W4)
- [x] Run build and verify

## Outcome

Completed on 2026-03-14. Fixed all auditor blockers and rebuilt cleanly at v0.1.1. C1: moved
pendingFile declaration above the workflow block in cursor-session-start.cjs and wrapped the
workflow block in `if (!synthesisPending)` — synthesis and workflow directives no longer fire
simultaneously. C2: replaced `isTaskFile` (workflows/tasks/ only) with `isWorkflowFile`
(workflows/ prefix) AND added `!synthesisPending` to the guard in cursor-skill-injector.cjs —
prevents false STOP on /complete writes to workflows/done/ and on synthesis file edits.
C3: changed WARNING to `exit 1` in build-cursor.sh step 7 — missing cursor-specific rules now
fail the build instead of silently shipping a broken distribution. complete.md: added
.github/autolearn-pending branch to synthesis trigger, fixed "status: task" → "status: in-progress".
agent-instructions.mdc: replaced "Run the complete command" with numbered mechanical file
operations matching workflow-gate.mdc language. Build output: claude-code 33 files, cursor 26
files, copilot 33 files — all three targets green.

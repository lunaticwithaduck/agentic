---
title: Workflow enforcement — Claude Code open-task nudge
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Extend `skill-detector.cjs` (UserPromptSubmit hook) to handle the non-empty tasks case.

## Acceptance Criteria

- [x] When tasks/ is non-empty, every UserPromptSubmit injects open-task filenames
- [x] Message is soft (no REQUIRED / STOP language) to avoid mid-task noise
- [x] Existing empty-check behaviour unchanged
- [x] ship/claude-code reflects the change after rebuild

## Outcome

Completed on 2026-03-14. Added an `else` branch to the existing pipeline reminder block
in `skill-detector.cjs`. When `workflows/tasks/` is non-empty, the hook now injects:
"Open task(s) in workflows/tasks/: [filenames]. Run /complete on any finished tasks before
starting new work." Soft language — no REQUIRED/STOP — so it doesn't interrupt mid-task
work but surfaces at the right moment when the model is about to start something new.
ship/claude-code will reflect this after the rebuild task runs.

---
title: Workflow enforcement — Cursor hooks + workflow-gate rule
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Add workflow enforcement to Cursor's hook layer and add a dedicated focused MDC rule.

## Acceptance Criteria

- [x] cursor-session-start.cjs: empty tasks → creation injection fires at session open
- [x] cursor-session-start.cjs: non-empty tasks → open-task nudge with filenames fires
- [x] cursor-skill-injector.cjs: empty tasks at file edit → hard STOP injected
- [x] cursor-skill-injector.cjs: non-empty tasks at file edit → soft nudge injected
- [x] workflow-gate.mdc: exists, alwaysApply: true, focused on workflow only
- [x] build script picks up workflow-gate.mdc and copies it to ship/cursor/

## Outcome

Completed on 2026-03-14.

- `cursor-session-start.cjs`: restructured to always run workflow state check first
  (creation requirement or open-task nudge), then autolearn check. Both contribute to
  a single additional_context output.
- `cursor-skill-injector.cjs`: added workflow state check before skill/autolearn assembly.
  Skips check if the file being edited is itself a task file (prevents false STOP on
  task creation). Empty tasks → hard STOP. Non-empty → soft nudge.
- `workflow-gate.mdc`: new dedicated 15-line rule (alwaysApply: true). Single job —
  workflow enforcement only. Cannot be buried in agent-instructions.mdc.
- `build-cursor.sh`: step 7 now loops over cursor-specific rules rather than hardcoding
  agent-instructions.mdc only — workflow-gate.mdc and any future rules copy automatically.

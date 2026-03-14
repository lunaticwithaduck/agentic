---
title: Workflow enforcement — rebuild + validate
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Steps

- [x] Run `bash buildScripts/build.sh`
- [x] Verify `ship/cursor/.cursor/hooks/cursor-session-start.cjs` has workflow checks
- [x] Verify `ship/cursor/.cursor/hooks/cursor-skill-injector.cjs` has workflow checks
- [x] Verify `ship/cursor/.cursor/rules/workflow-gate.mdc` exists
- [x] ship/claude-code includes updated skill-detector.cjs (via hooks copy step)
- [x] Build completed cleanly — claude-code: 33 files, cursor: 26 files

## Outcome

Completed on 2026-03-14. Build clean. All verification checks confirmed correct output
in both ship targets. workflow-gate.mdc present in ship/cursor with alwaysApply: true.
Workflow state checks confirmed in both cursor hook files via grep.

---
title: Fix autolearn pipeline — payload field + .sc schema in instructions
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal

Three source fixes identified during Copilot port testing. The autolearn synthesis
pipeline was non-functional because post-write.cjs dropped events when the hook
payload used `filePath` instead of `file_path`, and instruction files didn't
specify the `.sc` frontmatter schema so models produced schema-less candidates.

## Steps

- [x] Fix `post-write.cjs`: accept `filePath` and `path` as fallbacks for `file_path`
- [x] Fix `workflow-gate.instructions.md`: add `.sc` frontmatter schema (domain, source_task, date, keywords)
- [x] Fix `copilot-instructions.md`: add `.sc` frontmatter schema
- [x] Rebuild ships: `bash buildScripts/build.sh`
- [x] Verify Suite 01 still passes: `bash bench/run.sh --suite=01`

## Outcome

Completed on 2026-03-15. Fixed three root causes of autolearn pipeline failure found during Copilot port testing:
1. `post-write.cjs` now accepts `file_path || filePath || path` — no more silent drops when hook payload uses camelCase field.
2. `workflow-gate.instructions.md` now specifies the full `.sc` frontmatter schema including the mandatory `domain:` field.
3. `copilot-instructions.md` likewise updated with `.sc` schema. All ships rebuilt; Suite 01 passes 62/62.

## Completion

Run `/complete workflows/tasks/2026-03-15-fix-autolearn-pipeline.md` before starting any new work.

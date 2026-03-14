---
title: Workflow enforcement — task file template
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Add a standard task file template to both CLAUDE.md and agent-instructions.mdc so that
every task file the model creates includes an explicit completion section. This is the
primary fix for Problem B (task not completed) — the completion instruction lives inside
the work artifact itself, not in ambient context.

## Changes

- `CLAUDE.md` — add task file template under the Task Pipeline section
- `buildScripts/src/cursor-rules/agent-instructions.mdc` — same template

## Acceptance Criteria

- [x] Template present in CLAUDE.md
- [x] Template present in agent-instructions.mdc
- [x] Completion section is last item — unavoidable when model reads task back

## Outcome

Completed on 2026-03-14. Added a `### Task File Template` section to both CLAUDE.md and
agent-instructions.mdc immediately after the Task Pipeline enforcement block. Template
includes mandatory `## Completion` section as the final item, with an explicit `/complete`
command call. The instruction is now in the work artifact itself — the model reads it back
while working and sees the next step directly.

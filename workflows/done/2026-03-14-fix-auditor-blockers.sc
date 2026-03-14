---
domain: agentic-hooks
source_task: 2026-03-14-fix-auditor-blockers.md
date: 2026-03-14
keywords: cursor hooks synthesis conflict workflow guard isTaskFile false STOP
---

## Extracted Knowledge

### Synthesis conflict guard — ordering matters
In Cursor session hooks, `synthesisPending` (whether `.cursor/autolearn-pending` exists) must
be evaluated BEFORE the workflow state check. If both blocks run, the model receives two
conflicting hard-stop directives in the same `additional_context` — one telling it to synthesise
a skill, another telling it to create a task file. The model cannot satisfy both simultaneously
and loops. Fix: compute `synthesisPending = fs.existsSync(pendingFile)` at the top of the hook
and wrap the entire workflow block in `if (!synthesisPending)`.

### isWorkflowFile guard — too-narrow path matching causes false STOP
The original guard `filePath.includes('workflows/tasks/')` only allows the task-creation write
to bypass the workflow check. But `/complete` writes to `workflows/done/` AFTER deleting the
task file — at that point `workflows/tasks/` is empty, so the hook emits STOP. Correct pattern:
guard on `filePath.includes('workflows/')` (any workflow path) OR on `synthesisPending` (which
covers synthesis output writes to `.cursor/rules/`). Two conditions needed:

```js
const isWorkflowFile = filePath.replace(/\\/g, '/').includes('workflows/');
if (!isWorkflowFile && !synthesisPending) { /* workflow check */ }
```

### build-cursor.sh — WARNING is not a build failure
Using `echo WARNING` for a missing required rule allows a broken distribution to ship silently.
Any file that is listed in the build script's copy loop is required; missing it should fail the
build immediately. Pattern: use `exit 1` (not WARNING) for any missing source file in a build step.

### complete.md platform-aware autolearn-pending — three branches, not two
The synthesis trigger in `/complete` must write `autolearn-pending` to the correct platform
directory. Three possible locations (write to whichever exists):
- `.cursor/autolearn-pending` — Cursor install
- `.claude/autolearn-pending` — Claude Code install
- `.github/autolearn-pending` — Copilot install
Omitting the `.github/` branch means the synthesis trigger never arms on Copilot-only projects.

## Proposed Skill Content

A skill covering Cursor/Copilot hook guard patterns would include:
- The synthesis-first check pattern (compute synthesisPending before any other branch)
- The isWorkflowFile vs isTaskFile distinction and why the narrower guard fails
- The three-platform autolearn-pending path pattern
- The build script WARNING-vs-exit-1 rule for required files

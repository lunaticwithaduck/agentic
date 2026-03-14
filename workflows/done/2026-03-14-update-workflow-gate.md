---
title: Update workflow-gate.mdc — mechanical completion + .sc evaluation
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Acceptance Criteria

- [x] Completion steps are numbered file operations (Write done file, Delete task file, Write .sc evaluation)
- [x] Hard constraint: "saying all done without physically moving the file is a violation"
- [x] .sc evaluation block requires written output (not silent decision)
- [x] workflow-gate.mdc does NOT include synthesis trigger (that lives in complete.md only)

## Outcome

Completed on 2026-03-14. Replaced the vague "run /complete" completion section with
four numbered mandatory steps: Write done file, Delete task file, Write .sc evaluation
block (visible output, not internal), then and only then tell the user. Hard constraint
language added. Synthesis trigger intentionally absent — lives in complete.md only since
workflow-gate.mdc is always-applied and doesn't have access to the count/check logic.

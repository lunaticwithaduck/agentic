---
title: Cursor workflow hooks conflict with autolearn synthesis
identified: 2026-03-14
severity: medium
---

## Problem

`cursor-session-start.cjs` and `cursor-skill-injector.cjs` inject the workflow
enforcement reminder (creation requirement or open-task nudge) unconditionally —
even when `autolearn-pending` exists.

`skill-detector.cjs` (Claude Code) correctly wraps its workflow block in
`if (!synthesisPending)`, giving synthesis full priority. The Cursor hooks do not.

## Failure scenario

1. Autolearn threshold reached — `.cursor/autolearn-pending` written
2. `workflows/tasks/` happens to be empty
3. Session opens → model receives both:
   - `[REQUIRED — create a task file first]`
   - `[AUTOLEARN — SYNTHESIS REQUIRED. Do this now, before anything else.]`
4. Model follows synthesis steps, writes `.cursor/rules/${domain}.mdc`
5. `afterFileEdit` fires → `[STOP — No task file exists]` injected again
6. Conflicting directives, potential loop

## Fix

Wrap the workflow state block in `if (!synthesisPending)` in both Cursor hooks,
mirroring the Claude Code pattern exactly:

```js
// cursor-session-start.cjs and cursor-skill-injector.cjs
if (!synthesisPending) {
  // ... workflow state check (empty → creation, non-empty → nudge)
}
```

`synthesisPending` is already checked in both files for the autolearn block —
just hoist the check to gate the workflow block as well.

## Also fix (W1 from same audit)

`build-cursor.sh` step 7 uses `WARNING` + continues on missing rule files.
Should be `exit 1` to match step 3 (hooks) behaviour and prevent silent
incomplete ship artifacts.

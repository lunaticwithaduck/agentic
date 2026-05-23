---
title: Use absolute paths for agentic hook commands
created: 2026-05-12
completed: 2026-05-12
status: done
---

## Goal
`/home/jojo/agentic/.claude/settings.json` registers Claude Code hooks with
relative `node .claude/hooks/<file>.cjs` commands. When a session does any
Bash work in another project directory (like `~/Documents/ProbBrain/`),
Claude Code resolves the relative path against the wrong cwd and the hook
runner fails with:

```
Error: Cannot find module '/home/jojo/Documents/ProbBrain/.claude/hooks/post-stop.cjs'
```

The failure is non-blocking but pollutes the UI on every turn and silently
skips the session-state bookkeeping that the hooks do.

Fix: replace every `node .claude/hooks/<file>.cjs` in agentic's settings
with `node /home/jojo/agentic/.claude/hooks/<file>.cjs` so the command works
regardless of the hook runner's cwd.

## Steps
- [x] Inspect `/home/jojo/agentic/.claude/settings.json` for every
      `node .claude/hooks/...` command across all hook event types.
- [x] Rewrite each to the absolute path
      `node /home/jojo/agentic/.claude/hooks/<file>.cjs`.
- [x] Verify the resulting JSON parses cleanly (`python3 -m json.tool`).
- [x] Confirm by listing the new commands that all paths point at real
      files under `/home/jojo/agentic/.claude/hooks/`.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-12-fix-agentic-hook-paths.md` before starting any new work.

## Outcome

Completed on 2026-05-12.

`Edit` with `replace_all=true` swapped every `"command": "node .claude/hooks/`
prefix to `"command": "node /home/jojo/agentic/.claude/hooks/`. Four hook
commands updated in one shot:

```
node /home/jojo/agentic/.claude/hooks/skill-detector.cjs   (UserPromptSubmit)
node /home/jojo/agentic/.claude/hooks/block-secrets.cjs    (PreToolUse / Bash)
node /home/jojo/agentic/.claude/hooks/post-write.cjs       (PostToolUse / Write|Edit)
node /home/jojo/agentic/.claude/hooks/post-stop.cjs        (Stop)
```

`python3 -m json.tool` confirms the file still parses; `ls` confirms each of
the four `.cjs` files actually exists at the new absolute path. Next time the
Stop hook fires after a cross-project session, it'll resolve cleanly instead
of throwing `MODULE_NOT_FOUND`.

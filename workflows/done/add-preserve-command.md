---
title: Add /preserve slash command for session notes
created: 2026-03-15
completed: 2026-03-15
status: done
---

## Goal
Add a `/preserve` command that instructs Claude to write meaningful session notes into
SESSION.md's ## Session Notes section before ending a session. The Stop hook writes the
mechanical state; this command handles the human-readable summary.

## Steps
- [x] Write .claude/commands/preserve.md
- [x] Add preserve.md to ship/claude-code/ (build handles this, but ship needs direct update too)

## Outcome

Completed on 2026-03-15. Created `.claude/commands/preserve.md` and copied it to
`ship/claude-code/.claude/commands/preserve.md`. The command instructs Claude to reflect
on the session conversation, write 3–6 terse bullet-point notes into SESSION.md's
`## Session Notes` section, and confirm what was written. Complements the Stop hook
which handles mechanical state (open tasks, recent completions) automatically.

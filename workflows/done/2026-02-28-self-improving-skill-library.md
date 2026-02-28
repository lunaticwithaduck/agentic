---
title: Self-improving skill library via workflows/done analysis
created: 2026-02-28
promoted: 2026-02-28
completed: 2026-02-28
status: done
author: user
tags: [skills, meta-learning, self-improvement]
priority: medium
complexity: low
---

# Self-improving skill library via workflows/done analysis

## Description

A periodic skill (or hook) that reads `workflows/done/` to extract patterns from completed
work — what decisions were made, what mistakes were corrected, what approaches succeeded.
It uses this to propose updates to existing skills or add new project-specific skill files.

The core insight: domain-specific skills are most valuable but can't be written a priori
for every project. This mechanism lets the skill library grow organically from actual
project experience rather than being invented generically upfront.

## Notes

- Emerged from discussion about why generic methodology skills (debug, refactor) are net
  negative while domain-specific skills (security-audit, caching-strategy) are net positive
- The done/ directory already contains outcome notes — this repurposes them as training signal
- Implemented as a `/learn` slash command — zero overhead when not invoked
- Should distinguish project-specific learnings (stay local) from general ones (could be
  contributed upstream to the agentic repo)
- Related: the current skill auto-detection forces 1-3 skill activations per prompt —
  if the skill library improves over time, this becomes more valuable; if skills stay
  generic, forced activation is net negative

## Acceptance Criteria

- [x] A command reads all files in `workflows/done/` and extracts key decisions and corrections
- [x] It identifies patterns that could be encoded as skill guardrails (things that went wrong)
- [x] It proposes additions or edits to `.claude/skills/` with a human approval step
- [x] Project-specific learnings are scoped locally and don't pollute the upstream library
- [x] The mechanism itself is lightweight enough not to add meaningful latency to normal workflows

## Outcome

Completed on 2026-02-28. Created `/learn` slash command (`.claude/commands/learn.md`) that
analyzes `workflows/done/` to extract patterns from completed work — decisions made,
corrections applied, domain knowledge gained — and proposes skill library improvements.
The command classifies findings as project-specific vs general, and requires explicit human
approval before making any changes. Implemented as a command (not a hook) for zero overhead
during normal workflows. Updated `CLAUDE.md` with the new command in the slash commands table.

---
title: Self-improving skill library via workflows/done analysis
created: 2026-02-28
status: idea
author: user
tags: [skills, meta-learning, self-improvement]
priority: medium
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
- Could run as a Stop hook (post-response) or as a periodic slash command `/learn`
- Should distinguish project-specific learnings (stay local) from general ones (could be
  contributed upstream to the agentic repo)
- Related: the current skill auto-detection forces 1-3 skill activations per prompt —
  if the skill library improves over time, this becomes more valuable; if skills stay
  generic, forced activation is net negative

## Possible Acceptance Criteria

- [ ] A command or hook reads all files in `workflows/done/` and extracts key decisions and corrections
- [ ] It identifies patterns that could be encoded as skill guardrails (things that went wrong)
- [ ] It proposes additions or edits to `.claude/skills/` with a human approval step
- [ ] Project-specific learnings are scoped locally and don't pollute the upstream library
- [ ] The mechanism itself is lightweight enough not to add meaningful latency to normal workflows

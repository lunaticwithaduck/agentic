---
title: Skill Candidating via .sc files in workflows/done/
created: 2026-03-01
status: idea
author: user
tags: [skills, autolearn, self-improvement, architecture]
priority: high
---

# Skill Candidating via .sc files in workflows/done/

## Description

When a task is completed, Claude optionally generates a `.sc` (skill candidate) file alongside
the regular done file in `workflows/done/`. The `.sc` file captures domain-specific knowledge
applied during the task — not methodology, but concrete patterns, facts, or standards that
would be worth encoding as a reusable skill.

Skill candidates accumulate over time. When the same domain appears in N `.sc` files (N=3 is
the working hypothesis), the autolearn system synthesizes them into an actual skill file and
adds it to skill-rules.json automatically.

## Why This Matters

It ties the learning signal to actual work rather than arbitrary file counts or time intervals.
Recurrence is the quality gate: if a domain appears once, it might be a one-off. If it appears
three times, it's a real pattern worth encoding.

## Key Design Questions

- What goes in a `.sc` file? (domain, extracted pattern, proposed keywords, source task, date)
- How is "same domain" detected? (exact string vs. semantic similarity)
- What runs the synthesis step when N is reached?
- How does Claude decide whether to write a `.sc` at all? (skill-design-principle filter)
- What happens when `.sc` files at different granularity levels are synthesized?

## Possible Acceptance Criteria

- [ ] `/complete` instructs Claude to generate a `.sc` alongside the done file when domain
      knowledge was applied
- [ ] `.sc` file format is defined: YAML frontmatter (domain, date, source_task, keywords)
      + markdown body with extracted pattern
- [ ] PostToolUse (Write) hook detects new `.sc` files, counts by domain, flags when N=3
- [ ] Flagged domains trigger skill synthesis via Stop hook
- [ ] Synthesis reads all `.sc` files for that domain, generates skill .md + skill-rules.json entry
- [ ] Generated skill appears in `.claude/skills/` automatically

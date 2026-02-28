---
title: Skill Candidating via .sc files in workflows/done/
created: 2026-03-01
promoted: 2026-03-01
completed: 2026-03-01
status: done
author: user
tags: [skills, autolearn, self-improvement, architecture]
priority: high
complexity: medium
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

## .sc File Format

```yaml
---
domain: <canonical-domain-name>
source_task: <filename of the done/ task>
date: <YYYY-MM-DD>
keywords: ["keyword1", "keyword2"]
---

# Skill Candidate: <Domain Name>

## Extracted Knowledge

[Specific patterns, facts, standards, or anti-patterns learned — NOT methodology]

## Proposed Skill Content

[What a skill file for this domain would contain — the actual expert guidance]
```

## Architecture

1. `/complete` generates a `.sc` alongside the done file when domain knowledge was applied
2. `post-write.sh` (PostToolUse) detects `.sc` writes, counts by domain, writes flag
   `.claude/autolearn-pending` when a domain reaches N=3
3. `skill-detector.sh` (UserPromptSubmit) checks for flag on next prompt, injects synthesis
   instruction so Claude generates the skill within normal conversation flow
4. Claude writes the skill file to `.claude/skills/`, updates `skill-rules.json`, clears flag

## Acceptance Criteria

- [x] `/complete` instructs Claude to generate a `.sc` alongside the done file when domain
      knowledge was applied
- [x] `.sc` file format is defined: YAML frontmatter (domain, date, source_task, keywords)
      + markdown body with extracted pattern
- [x] PostToolUse (Write) hook detects new `.sc` files, counts by domain, flags when N=3
- [x] Flagged domains trigger skill synthesis via next UserPromptSubmit hook injection
- [x] Synthesis instruction includes all `.sc` file contents and step-by-step instructions
- [x] skill-detector.sh tracks skill usage (last_used, used_count) in skill-usage.json

## Outcome

Completed on 2026-03-01. Implemented the full skill candidating pipeline: updated /complete
command to generate .sc files, post-write.sh to detect and count .sc files by domain (flagging
at N=3), and skill-detector.sh to check for the autolearn flag and inject synthesis instructions
with all .sc file contents. Also added skill-usage.json tracking for decay signals. End-to-end
tested with mock .sc files — pipeline works from .sc write through domain counting through
synthesis injection. Live validation of actual synthesis (Claude following the injected
instructions to write a skill file) deferred to a follow-up task.

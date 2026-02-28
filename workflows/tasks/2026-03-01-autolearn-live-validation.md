---
title: Live validation of autolearn synthesis pipeline
created: 2026-03-01
promoted: 2026-03-01
status: task
author: user
tags: [skills, autolearn, validation]
priority: high
complexity: low
---

# Live validation of autolearn synthesis pipeline

## Description

The skill candidating infrastructure is implemented and unit-tested. This task validates
the end-to-end loop in a live session:

1. Complete 3+ tasks in the same domain via `/complete`
2. Verify `.sc` files are generated correctly
3. Verify the synthesis flag fires when N=3 is reached
4. Verify Claude follows the injected synthesis instructions to write a real skill
5. Verify the generated skill appears in `.claude/skills/` and `skill-rules.json`
6. Verify the generated skill fires on a matching prompt via skill-detector.sh

## How to Test

- Use real tasks or create 3 test tasks in `workflows/tasks/` that involve the same
  domain (e.g., "graphql-patterns", "redis-caching", or any domain not already in the
  skill library)
- Complete each with `/complete` — ensure Claude generates a `.sc` file each time
- On the 3rd completion, the flag should be set
- On the next prompt, Claude should automatically synthesize and write the skill

## Acceptance Criteria

- [ ] 3 `.sc` files generated for the same domain via `/complete`
- [ ] `post-write.sh` correctly sets `.claude/autolearn-pending` after the 3rd
- [ ] `skill-detector.sh` injects synthesis instructions on the next prompt
- [ ] Claude generates a valid skill .md file in `.claude/skills/`
- [ ] Claude adds a correct entry to `skill-rules.json`
- [ ] The new skill fires on a matching prompt (verified via hook test)

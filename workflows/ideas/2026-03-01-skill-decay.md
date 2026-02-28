---
title: Skill Decay — freshness tracking and pruning for the skill library
created: 2026-03-01
status: idea
author: user
tags: [skills, maintenance, decay, freshness]
priority: medium
---

# Skill Decay — freshness tracking and pruning for the skill library

## Description

Skills written today may be wrong or irrelevant 3-6 months from now. Frameworks evolve,
security patches land, APIs deprecate, best practices shift. Currently the skill library only
grows — nothing detects staleness or prunes outdated content.

A stale skill is actively harmful: it injects outdated guidance with the authority of expert
knowledge. The system should track freshness and flag (or remove) skills that haven't been
validated recently.

## Signals That a Skill May Be Stale

- Time elapsed since last write (passive — may just mean the domain wasn't touched)
- A new `.sc` file for the same domain with conflicting patterns (active conflict signal)
- Zero fires over a long period (domain may no longer be relevant to the project)
- User correction after skill fires (negative signal — connects to negative-signal-gap problem)

## Key Design Questions

- What's the right decay window? (3 months? 6 months? domain-dependent?)
- Who validates a flagged skill? Claude alone, or prompt to user?
- When `.sc` files conflict with existing skills, should the skill update or the `.sc` be rejected?
- Should fire rate be tracked? (requires state — adds complexity)
- How does decay interact with autolearn? (autolearn could refresh a skill, resetting its clock)

## Possible Acceptance Criteria

- [ ] Skills have `created` and `last_validated` in frontmatter
- [ ] A periodic check (or `/clean` command) flags skills older than N months with no recent
      `.sc` corroboration
- [ ] Autolearn synthesis updates `last_validated` when it rewrites a skill
- [ ] Conflicting `.sc` files trigger a skill update proposal rather than a new skill
- [ ] Flagged stale skills are surfaced in `/status` output

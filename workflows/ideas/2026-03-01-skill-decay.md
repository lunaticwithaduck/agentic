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

## Usage Tracking

Skills should carry `last_used` and `used_count` metadata, updated each time the skill fires.
These are the most reliable decay signals:

- `last_used` — active signal: skill hasn't fired in 90 days → domain may no longer be relevant
- `used_count` — depth signal: `count: 0` means the skill never helped; `count: 500` means
  it's load-bearing and should be updated with care, not overwritten

Implementation note: writing back to skill `.md` frontmatter from a hook is fragile.
Prefer a sidecar file — `.claude/skill-usage.json` — with `{ skill_name: { last_used, count } }`.
The decay check reads the sidecar; the skill files stay clean.

## Key Design Questions

- What's the right decay window? (3 months? 6 months? domain-dependent?)
- When `.sc` files conflict with existing skills, should the skill update or the `.sc` be rejected?
- How does decay interact with autolearn? (autolearn synthesis resets `last_validated`, not `last_used`)
- At what `used_count` threshold does a skill become "stable" and require stronger evidence to update?

## Possible Acceptance Criteria

- [ ] Skills have `created` and `last_validated` in frontmatter
- [ ] A periodic check (or `/clean` command) flags skills older than N months with no recent
      `.sc` corroboration
- [ ] Autolearn synthesis updates `last_validated` when it rewrites a skill
- [ ] Conflicting `.sc` files trigger a skill update proposal rather than a new skill
- [ ] Flagged stale skills are surfaced in `/status` output

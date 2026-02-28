---
title: Skill Decay — freshness tracking and pruning for the skill library
created: 2026-03-01
promoted: 2026-03-01
completed: 2026-03-01
status: done
author: user
tags: [skills, maintenance, decay, freshness]
priority: medium
complexity: low
---

# Skill Decay — freshness tracking and pruning for the skill library

## Description

Skills written today may be wrong or irrelevant 3-6 months from now. Frameworks evolve,
security patches land, APIs deprecate, best practices shift. Currently the skill library only
grows — nothing detects staleness or prunes outdated content.

A stale skill is actively harmful: it injects outdated guidance with the authority of expert
knowledge. The system should track freshness and flag (or remove) skills that haven't been
validated recently.

Usage tracking (`skill-usage.json` with `last_used` and `used_count`) is already implemented
in `skill-detector.sh`. This task adds the detection and surfacing layer on top of that data.

## Signals That a Skill May Be Stale

- Time elapsed since last fire (passive — may just mean the domain wasn't touched)
- Zero fires ever (skill was created but never triggered)
- A new `.sc` file for the same domain with conflicting patterns (active conflict signal)

## Acceptance Criteria

- [x] `/clean` reports stale skills (no fires in 90+ days) and never-fired skills
- [x] `/status` shows a stale skill count as a health indicator
- [x] Decay detection reads from `.claude/skill-usage.json` — no new hooks needed
- [x] Skills that have never appeared in `skill-usage.json` are flagged as "never fired"
- [x] `/clean apply` offers to archive stale skills (move to `.claude/skills/archived/`)

## Outcome

Completed on 2026-03-01. Added skill decay detection to `/clean` (scans skill-usage.json vs
skill-rules.json, flags stale 90+ day and never-fired skills, offers archival in apply mode)
and `/status` (Skill Health section showing active/stale/never-fired counts). No new hooks —
builds entirely on the usage tracking already implemented in skill-detector.sh during skill
candidating work.

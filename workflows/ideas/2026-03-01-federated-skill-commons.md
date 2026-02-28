---
title: Federated Skill Commons — collective intelligence across teams and companies
created: 2026-03-01
status: idea
author: user
tags: [skills, federation, network-effect, product, vision]
priority: high
---

# Federated Skill Commons

## The Insight

Every team using agentic learns independently. Company A discovers the optimal Redis connection
pooling pattern via their autolearn loop. Company B reinvents it from scratch three months
later. The knowledge stays locked in `.claude/skills/local/`. No one benefits from anyone
else's discoveries.

But most domain knowledge isn't proprietary. SQL indexing patterns, Docker security hardening,
rate limiting strategies, WCAG compliance rules — these are general. A team that learns them
shouldn't keep them to themselves.

## The Vision

`.sc` files for general (non-proprietary) knowledge flow upstream. The agentic maintainers
(or an automated system) curate them into the base skill library. Every new project starts
smarter than the last team's project did. The more teams use agentic, the better the base
library gets for everyone.

This is a compounding network effect, not just a per-team productivity tool.

## How It Works (rough sketch)

1. `.sc` files have a `scope` field: `local` (project-specific) or `general` (shareable)
2. Claude applies the scope filter at `.sc` generation time: is this pattern specific to
   our stack/conventions, or would any team benefit from it?
3. `general` `.sc` files can be submitted upstream via a simple PR or API call
4. Curation layer (automated + occasional human) validates and merges into base skill library
5. All agentic users pull the improved library on next install/update

## Why This Is Bigger Than It Sounds

- Every team contributes knowledge at the cost of one small file
- Every team benefits from the accumulated knowledge of all teams
- The base skill library becomes a living corpus of collective intelligence
- Teams working on the same stack (e.g., Django + Postgres + Celery) share a skill commons
  specific to their domain without sharing code or business logic

## Risks and Open Questions

- **Privacy**: even "general" patterns may leak tech stack, architecture decisions
- **Quality control**: crowdsourced skills could introduce wrong or biased guidance
- **Scope detection accuracy**: Claude deciding `local` vs `general` is a judgment call
- **Who runs the curation layer**: a maintainer bottleneck at scale vs. automated risk

## Note

This idea emerged from the autolearn brainstorm. Not a near-term implementation target —
but worth capturing now because it shapes design decisions made earlier (the `scope` field
in `.sc` files, the `local/` directory separation).

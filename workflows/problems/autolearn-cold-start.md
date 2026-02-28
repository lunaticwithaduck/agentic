# Problem: Autolearn Cold Start

**Date identified:** 2026-03-01
**Status:** Open — mitigable, not blocking

---

## The Problem

The autolearn system learns from `.sc` files that accumulate as work gets done. A new project
using agentic starts with zero `.sc` files. For the first N completed tasks, the skill library
is entirely generic — no project-specific knowledge, no domain recurrence signal.

The system gets better over time but starts weak. Teams adopting agentic expecting an
intelligent assistant get a generic one for the first several weeks.

## Why It Matters

First impressions set expectations. If the skill library doesn't reflect the team's stack
for the first month, teams may conclude the system isn't useful and stop using it — before
it has enough signal to become useful.

## Mitigation Approaches

- **Seed from setup**: `/setup` already asks about the team's tech stack. It could generate
  2-3 initial `.sc` files from that input, jump-starting the recurrence counter for the
  project's actual domains
- **Import from sibling projects**: if the team has another project already using agentic
  on the same stack, copy its `.sc` files as a starting corpus
- **Accept the cold start**: document it as expected behavior; the library reaches useful
  density after ~30 completions
- **Lower initial threshold**: use N=1 for the first skill in a domain (lower bar to get
  any project-specific skill at all), then raise to N=3 for updates

## Note

User believes this is mitigable — not a blocking concern, but worth tracking.

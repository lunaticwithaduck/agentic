# Problem: Skill Decay — Knowledge Staleness

**Date identified:** 2026-03-01
**Status:** Partially solved — usage decay implemented, knowledge freshness open

---

## The Problem

Skills are written once and never updated unless someone manually edits them. Domain knowledge
ages: tools change, best practices evolve, security vulnerabilities get patched, APIs deprecate.
A skill written in 2024 about Terraform patterns may be actively wrong by 2026. Nothing detects
this drift.

## Why It Matters

A stale skill is worse than no skill for the same reason a bad skill is: it injects wrong
information with the authority of "expert guidance." The user trusts the skill; Claude follows
it; the output is confidently outdated.

This is especially bad for security skills (CVEs change), infrastructure skills (tool versions
change), and any skill tied to a specific API or library.

## Dimensions of the Problem

1. **No freshness metadata** — skills have no `last_validated` date or source reference
2. **No decay signal** — nothing tells us when a skill's domain has changed
3. ~~**No pruning mechanism** — the library only grows, never shrinks or updates~~ ✓ Solved — `/clean` archives stale skills based on fire rate
4. ~~**Cross-session blindness** — Claude Code has no memory of when a skill was last useful~~ ✓ Solved — `skill-usage.json` tracks last_used and used_count per skill

## Approaches Worth Exploring

- Add `last_validated` and `source` frontmatter to skill files; flag skills older than N months
- After autolearn runs, check if existing skills in the same domain conflict with new patterns;
  if so, propose an update rather than a new skill
- Version skills: keep a changelog of what changed and why, so decay is visible

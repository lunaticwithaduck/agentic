# Problem: Skill Decay

**Date identified:** 2026-03-01
**Status:** Open

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
3. **No pruning mechanism** — the library only grows, never shrinks or updates
4. **Cross-session blindness** — Claude Code has no memory of when a skill was last useful

## Approaches Worth Exploring

- Add `last_validated` and `source` frontmatter to skill files; flag skills older than N months
- After autolearn runs, check if existing skills in the same domain conflict with new patterns;
  if so, propose an update rather than a new skill
- Track skill "fire rate" over time: if a skill stops firing (domain no longer comes up),
  flag for review
- Version skills: keep a changelog of what changed and why, so decay is visible

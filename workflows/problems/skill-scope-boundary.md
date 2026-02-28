# Problem: Skill Scope Boundary

**Date identified:** 2026-03-01
**Status:** Open

---

## The Problem

Project-specific knowledge and general domain knowledge currently live in the same skill
library with no distinction. A skill about "this team's Postgres schema conventions" is
fundamentally different from a skill about "SQL indexing patterns" — but both would appear
in `.claude/skills/` and fire the same way.

This has consequences in both directions:
- Project-specific skills get inherited by teams that clone agentic (wrong)
- General skills learned on one project don't flow upstream to benefit others (waste)

## Why It Matters

For agentic to be a reusable template, the skill library must be general. For teams using
it to get value, skills must be specific. These goals conflict if there's no boundary.

At scale (a company running agentic across 50 repos), project-specific skills leaking into
general ones — or general discoveries not being shared — becomes a significant problem.

## Dimensions of the Problem

1. **No scope metadata** — skills don't declare whether they're project-specific or general
2. **Single library** — no structural separation between local and shared knowledge
3. **Clone contamination** — cloning agentic to a new project brings all skills, including
   ones tuned to the original project's stack
4. **Upstream contribution gap** — no path for a team to say "this skill is general enough
   to be in the base repo"

## Approaches Worth Exploring

- Add `scope: project | general` frontmatter to skill files
- Separate directories: `.claude/skills/` (general, tracked in agentic repo) vs.
  `.claude/skills/local/` (project-specific, gitignored from agentic but tracked locally)
- Autolearn emits project-specific `.sc` files by default; promotion to general requires
  explicit action (PR to agentic upstream)
- `.gitignore` pattern: `local-*.md` in `.claude/skills/` for project-specific skills that
  shouldn't be committed to the agentic template

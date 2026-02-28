Analyze completed work to discover patterns and propose skill improvements.

## Instructions

1. Read all `.md` files in `workflows/done/`
2. For each completed task, extract:
   - **What was built** — the outcome and deliverables
   - **Key decisions** — approaches chosen and trade-offs made
   - **Corrections** — mistakes caught, rework done, or assumptions that were wrong
   - **Domain knowledge** — any specialized patterns, standards, or checklists that emerged

3. Cross-reference findings against the current skill library in `.claude/skills/`:
   - Do any existing skills need updates based on lessons learned?
   - Are there new domain-specific patterns that no existing skill covers?
   - Were any skills activated but unhelpful (indicating the skill needs refinement)?

4. Classify each finding as:
   - **Project-specific** — relevant only to this project (propose a local skill in `.claude/skills/`)
   - **General** — could benefit any project using agentic (note for upstream contribution)

5. Output a structured report:

```
## Learning Report

### Completed Work Analyzed
- [count] completed tasks in workflows/done/

### Patterns Discovered

#### Skill Updates (existing skills that could be improved)
- **[skill-name]**: [what should change and why, citing which done/ task revealed this]

#### New Skill Candidates (domain knowledge not yet captured)
- **[proposed-skill-name]** (project-specific | general): [what knowledge it would encode, citing which done/ task revealed this]

#### Guardrails (things that went wrong and should be prevented)
- [pattern to avoid]: [what happened, how a skill or hook could prevent it]

### Recommendations
1. [Actionable recommendation with priority]
```

6. For each proposed change, ask the user for approval before making it:
   - "Would you like me to [create/update] `.claude/skills/[name].md`?"
   - Only proceed with explicit user confirmation
   - When creating new skills, follow the format in `.claude/skills/skill-creator.md`
   - When creating new skills, also update `skill-rules.json`, `skill-detector.sh`, and `CLAUDE.md`

7. If $ARGUMENTS is "dry" or empty, only output the report (no changes)
8. If $ARGUMENTS is "apply", output the report and then propose each change one by one

## Design Philosophy

This command embodies the principle that **domain-specific skills are most valuable but can't
be written a priori**. Generic methodology skills (debug, refactor) are net negative because
Claude already knows them. The skill library should grow organically from actual project
experience:

- Prefer narrow, concrete skills over broad, abstract ones
- A skill should inject **facts, patterns, or standards** Claude doesn't reliably know
- If Claude would do fine without the skill, don't create it
- Project-specific skills stay local; only genuinely reusable patterns are general

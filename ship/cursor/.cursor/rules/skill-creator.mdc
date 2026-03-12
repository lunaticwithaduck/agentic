---
description: Apply when the user asks about create skill, new skill, add skill, custom skill, skill template, make a skill. Also applies when working with .cursor/rules/* files.
globs: [".cursor/rules/**"]
alwaysApply: false
---

# Skill Creator

## Design Principle

Skills inject **domain-specific knowledge** Claude doesn't have reliably on its own.
They are NOT methodology templates. Before creating a skill, ask:

> "Does this inject facts, patterns, or standards that Claude lacks — or does it just
> describe a process Claude already knows?"

If Claude would do fine without the skill, don't create it. Good candidates:
- Specific tool syntax (Dockerfile best practices, Terraform patterns, Mermaid syntax)
- Specific standards with enumerable criteria (WCAG levels, OWASP Top 10)
- Specific domain checklists (SQL indexing patterns, cache invalidation strategies)
- Specific output tasks Claude doesn't naturally do (de-ai-ify text, extract PDF content)

Poor candidates (delete these, don't create more):
- How to debug, refactor, review code, write tests — Claude knows these
- Generic "best practices" without domain-specific content
- Methodology templates with phases and steps

## Skill File Format

Create the file at `.cursor/rules/<skill-name>.md`:

```markdown
---
description: Apply when the user asks about keyword1, keyword2. Also applies when working with .ext files.
globs: ["**/*.ext", "specific-file"]
alwaysApply: false
---

# Skill Title

## Purpose
[What specific domain knowledge does this inject? Be concrete.]

## [Domain Knowledge Sections]
[The actual expert content — patterns, standards, checklists, syntax examples]
[This is the value. Make it specific and actionable.]

## Failure Modes
[Anti-patterns, edge cases, and things that look right but go wrong in this domain.
Only include observed failures — not inferred ones. Format: "Avoid X because Y."
Leave this section empty or omit it if no real failures have been observed yet.
Do NOT populate this with generic caution — it must be specific and earned.]

## Output Format (optional)
[Only if the skill produces a specific structured output]
```

## Also Update skill-index.md

Add an entry to `.cursor/rules/skill-index.md`:

```json
"<skill-name>": {
  "keywords": ["keyword1", "keyword2"],
  "filePatterns": ["**/*.ext"],
  "toolTriggers": ["Read", "Write"]
}
```

`toolTriggers` options: `Read`, `Write`, `Edit`, `Bash`, `Glob`, `Grep`

## Also Update agent-instructions.md

Add the new skill to the appropriate category row in the Skills Library table in `agent-instructions.md`.

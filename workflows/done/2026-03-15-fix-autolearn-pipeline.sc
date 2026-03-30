---
domain: agentic-hooks
source_task: 2026-03-15-fix-autolearn-pipeline.md
date: 2026-03-15
keywords: post-write filePath file_path payload autolearn frontmatter schema
---

## Extracted Knowledge

### Hook payload field naming varies by platform

Claude Code's `PostToolUse` hook payload uses `file_path` (snake_case).
GitHub Copilot and some VS Code extension contexts use `filePath` (camelCase).
A hook that reads only one variant silently drops all events from the other —
no error, no warning, just a silent no-op. Always normalize:

```js
filePath = payload.file_path || payload.filePath || payload.path || '';
```

### `.sc` frontmatter `domain:` is the sole counter key

`post-write.cjs` counts `.sc` files by parsing the `domain:` YAML frontmatter field.
If a `.sc` file has no frontmatter, or has frontmatter without `domain:`, the count
stays zero forever — threshold is never reached — synthesis never arms. This means
instruction files that describe `.sc` format without explicitly listing required
frontmatter fields will produce uncountable candidates. The full required schema:

```yaml
---
domain: <broad-tech-name>      # REQUIRED — drives autolearn counting
source_task: <task-filename>
date: YYYY-MM-DD
keywords: [word1, word2, ...]  # 3-6 trigger words
---
```

### Instruction files must specify schema, not just format

If an instruction file says "write a `.sc` file with `## Extracted Knowledge`" but
omits the frontmatter spec, the model will produce well-structured markdown with no
frontmatter — valid markdown, broken pipeline. Schema requirements must be stated
explicitly in every instruction surface that describes `.sc` output.

## Proposed Skill Content

**Skill name:** `agentic-hooks` (extend existing)

Hook payload normalization and `.sc` schema enforcement are the two highest-failure
points in the autolearn pipeline. Add to the existing `agentic-hooks` skill:

1. Payload field normalization pattern (snake vs camelCase)
2. `.sc` required frontmatter fields with example
3. Warning: instruction files omitting schema = silent pipeline failure

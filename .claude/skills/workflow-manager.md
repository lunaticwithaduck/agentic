---
name: workflow-manager
description: Managing the idea-to-task-to-done pipeline
activate: when managing workflows, ideas, tasks, pipeline, or "workflow"
---

# Workflow Manager Skill

## Pipeline Overview

```
workflows/ideas/  -->  workflows/tasks/  -->  workflows/done/
   (capture)           (refine & do)          (archive)
```

## File Format

All workflow files are markdown with YAML frontmatter:

```markdown
---
title: Short descriptive title
created: YYYY-MM-DD
status: idea | task | done
author: name or agent
tags: [tag1, tag2]
priority: low | medium | high | critical
---

# Title

## Description
What this is about.

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
```

## Stage: Ideas (`workflows/ideas/`)

Ideas are rough captures. They do not need to be fully formed.

**Filename format**: `YYYY-MM-DD-short-slug.md`

**Required fields**: title, created, status (idea)

**Optional fields**: tags, priority, author

Creating an idea:
1. Generate filename from date and slugified title
2. Fill in the template with available information
3. Set status to `idea`

## Stage: Tasks (`workflows/tasks/`)

Tasks are refined ideas ready for implementation.

**Additional required fields when promoting**:
- Acceptance criteria (checkboxes)
- Priority
- Estimated complexity: small | medium | large

Promoting an idea to a task:
1. Read the idea file
2. Add acceptance criteria, priority, and complexity
3. Change status from `idea` to `task`
4. Add `promoted: YYYY-MM-DD` to frontmatter
5. Move file from `workflows/ideas/` to `workflows/tasks/`

## Stage: Done (`workflows/done/`)

Done items are completed tasks with outcome notes.

**Additional fields when completing**:
- `completed: YYYY-MM-DD`
- Outcome notes (what was actually done, any deviations)

Completing a task:
1. Read the task file
2. Verify all acceptance criteria are checked
3. Change status from `task` to `done`
4. Add `completed: YYYY-MM-DD` and outcome notes
5. Move file from `workflows/tasks/` to `workflows/done/`

## Status Reports

When asked for status, generate a summary:

```
## Workflow Status

### Ideas (X items)
- [priority] title (created date)

### In Progress (X items)
- [priority] title (promoted date)

### Done (X items, last 7 days)
- title (completed date)

### Metrics
- Ideas waiting: X
- Tasks in progress: X
- Completed this week: X
- Average time idea-to-done: X days
```

## Housekeeping

- Review ideas older than 30 days - promote or archive them
- Check for stale tasks (no progress in 14 days)
- Keep done items for reference; they serve as a project log

Clean up stale items in the project.

## Instructions

1. Determine the mode based on $ARGUMENTS:
   - If $ARGUMENTS is "dry" or empty: report findings without making changes
   - If $ARGUMENTS is "apply": actually perform cleanup actions (with confirmation)

2. Scan for stale items across these categories:

### Stale Ideas
- Scan `workflows/ideas/` for idea files older than 30 days
- List them and ask if they should be archived or deleted

### Stale Tasks
- Scan `workflows/tasks/` for task files older than 14 days with no recent updates
- Flag them as stale and suggest action

### Orphaned Git Branches
- Run `git branch` to list local branches
- Check each branch for recent commits using `git log -1 --format="%ci" [branch]`
- Flag branches with no commits in the last 30 days as orphaned

### Code Annotations
- Search the codebase for `TODO`, `FIXME`, and `HACK` comments
- Report the count by file

3. Output a cleanup report:

```
## Cleanup Report

### Stale Ideas (older than 30 days)
- [filename] — created [date], title: [title]

### Stale Tasks (older than 14 days, no updates)
- [filename] — created [date], title: [title]

### Orphaned Branches (no commits in 30 days)
- [branch-name] — last commit: [date]

### Code Annotations
- [filename]: [count] TODO, [count] FIXME, [count] HACK

### Summary
- Total stale ideas: [n]
- Total stale tasks: [n]
- Total orphaned branches: [n]
- Total code annotations: [n]
```

4. If in "apply" mode, perform cleanup actions:
   - Move stale ideas to `workflows/archive/`
   - Move stale tasks to `workflows/archive/`
   - Delete orphaned branches after user confirmation
   - Code annotations are reported only (not auto-fixed)

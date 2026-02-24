Trigger a code review of current changes.

## Instructions

1. Run `git diff` to see unstaged changes and `git diff --cached` for staged changes
2. If no changes found, check `git diff HEAD~1` for the last commit
3. Activate the `code-review` skill
4. Review all changes for:
   - Bugs and logic errors
   - Security issues
   - Performance concerns
   - Readability and code style
   - Test coverage gaps
5. Output a structured review with findings grouped by severity:

```
## Code Review

### Critical
- [file:line] Description of critical issue

### Warning
- [file:line] Description of warning

### Note
- [file:line] Description of minor note
```

6. End with a verdict: **APPROVE**, **CHANGES REQUESTED**, or **NEEDS DISCUSSION**
7. If $ARGUMENTS contains a file path, review only that file instead of all changes

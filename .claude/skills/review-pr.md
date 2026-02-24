---
name: review-pr
description: Structured pull request review checking for bugs, security, tests, and quality
activation:
  keywords: ["review pr", "review pull request", "pr review", "code review pr"]
  file_patterns: []
---

# Review PR

## Purpose
Perform a thorough, structured review of a pull request, checking for correctness, security, test coverage, and code quality.

## Instructions

### 1. Understand the PR
- Read the PR title and description to understand intent
- Use `gh pr view <number>` to get PR metadata
- Use `gh pr diff <number>` to see all changes
- Check linked issues for context

### 2. Review Each File Change
For every changed file, evaluate:
- **Correctness**: Does the logic do what it claims?
- **Edge cases**: Are boundary conditions handled?
- **Error handling**: Are errors caught and handled appropriately?
- **Naming**: Are names clear and consistent with the codebase?

### 3. Check for Bugs
- Off-by-one errors
- Null/undefined access
- Race conditions or concurrency issues
- Resource leaks (unclosed connections, file handles)
- Type mismatches or implicit conversions

### 4. Check Security
- Input validation and sanitization
- SQL injection, XSS, or other injection vectors
- Authentication and authorization checks
- Sensitive data exposure (logs, error messages)
- Hardcoded secrets or credentials

### 5. Check Test Coverage
- Are new features covered by tests?
- Are edge cases and error paths tested?
- Do tests actually assert meaningful behavior?
- Are existing tests updated if behavior changed?

### 6. Check for Breaking Changes
- API contract changes (request/response schemas)
- Database schema changes without migration
- Configuration changes requiring deployment steps
- Removed or renamed public interfaces

### 7. Verdict
- **Approve**: No issues or only minor nits
- **Request Changes**: Blocking issues that must be addressed
- **Comment**: Non-blocking suggestions for improvement

## Output Format
```
## PR Review: #[number] - [title]

### Summary
[1-2 sentence overview of the changes and their quality]

### Verdict: [Approve / Request Changes / Comment]

### Blocking Issues
- [ ] [file:line] [description of issue]

### Suggestions (non-blocking)
- [file:line] [suggestion]

### Positive Notes
- [What was done well]

### Checklist
- [x/blank] Correctness verified
- [x/blank] Security reviewed
- [x/blank] Tests adequate
- [x/blank] No breaking changes (or documented)
- [x/blank] Documentation updated if needed
```

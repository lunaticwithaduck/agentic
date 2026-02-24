---
name: code-review
description: Thorough code review with security, performance, and quality checks
activate: when asked to review code, PR review, code audit, or "review"
---

# Code Review Skill

## Process

1. **Read the full diff or file** - Understand the complete context before commenting
2. **Check each category below** - Go through systematically
3. **Report findings** - Use the output format at the bottom

## Review Categories

### Security (Critical)
- SQL injection, XSS, CSRF vulnerabilities
- Hardcoded secrets, API keys, credentials
- Unsafe deserialization or eval usage
- Missing input validation or sanitization
- Improper authentication/authorization checks
- OWASP Top 10 compliance:
  1. Broken access control
  2. Cryptographic failures
  3. Injection
  4. Insecure design
  5. Security misconfiguration
  6. Vulnerable/outdated components
  7. Authentication failures
  8. Data integrity failures
  9. Logging/monitoring failures
  10. Server-side request forgery

### Performance
- N+1 queries or unnecessary database calls
- Missing indexes for frequent queries
- Unbounded loops or recursion
- Large memory allocations or leaks
- Missing pagination for list endpoints
- Unnecessary re-renders (frontend)

### Readability
- Clear naming for variables, functions, types
- Functions under 50 lines; files under 300 lines
- No deeply nested conditionals (max 3 levels)
- Comments explain "why", not "what"
- Consistent code style with the rest of the project

### Test Coverage
- New code has corresponding tests
- Edge cases are covered (null, empty, boundary values)
- Error paths are tested
- No test logic in production code

### Architecture
- Single responsibility principle
- No circular dependencies
- Proper error handling and propagation
- API contracts match documentation

## Output Format

Report findings using this structure:

```
## Code Review: [file or PR name]

### Critical
- [SECURITY] Description of issue (file:line)
- [BUG] Description of issue (file:line)

### Warning
- [PERF] Description of concern (file:line)
- [DESIGN] Description of concern (file:line)

### Suggestion
- [STYLE] Description of suggestion (file:line)
- [TEST] Missing test case description

### Approved
- [OK] What looks good

### Summary
Overall assessment: APPROVE / REQUEST CHANGES / REJECT
Key items to address before merge: ...
```

Severity levels:
- **Critical**: Must fix before merge (security, bugs, data loss)
- **Warning**: Should fix, may cause issues later (performance, design)
- **Suggestion**: Nice to have, improves quality (style, tests)

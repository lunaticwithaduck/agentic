---
name: debug
description: Systematic debugging with hypothesis-driven root cause analysis
activation:
  keywords: ["debug", "bug", "error", "crash", "broken", "not working", "fix issue", "stack trace"]
  file_patterns: ["**/*"]
---

# Debug

## Purpose
Systematically diagnose and fix bugs using structured hypothesis-driven debugging rather than guessing.

## Instructions

### 1. Reproduce the Issue
- Get clear reproduction steps from the user
- Identify: what is expected vs. what actually happens?
- Note the environment (OS, runtime version, config)
- Determine if the issue is consistent or intermittent

### 2. Gather Context
- Read the relevant error message or stack trace carefully
- Check recent changes (git log, git diff) that may have introduced the bug
- Look at relevant log output
- Identify the failing code path

### 3. Form Hypotheses
List 2-4 likely causes, ordered by probability:
- Start with the simplest explanation
- Consider: wrong input, wrong state, wrong assumption, race condition
- Check for common pitfalls: off-by-one, null/undefined, type coercion, async timing

### 4. Test Hypotheses Systematically
For each hypothesis:
- Add targeted logging or assertions to confirm/deny
- Test ONE hypothesis at a time
- Eliminate hypotheses based on evidence, not intuition
- Narrow the scope: binary search through the code path if needed

### 5. Common Debugging Strategies
- **Binary search**: Comment out half the code, see if bug persists
- **Minimal reproduction**: Strip away everything unrelated
- **Rubber duck**: Explain the code line by line
- **Check assumptions**: Verify types, values, and state at each step
- **Read the error**: The error message often contains the answer
- **Check boundaries**: Input validation, array bounds, null checks

### 6. Fix and Verify
- Apply the minimal fix that addresses the root cause
- Do NOT fix symptoms; fix the underlying problem
- Add a test that would have caught this bug
- Verify the fix does not break other functionality
- Document what caused the bug if it was non-obvious

## Output Format
```
## Bug Report

### Symptom
[What is broken and how it manifests]

### Root Cause
[The actual underlying problem]

### Hypotheses Tested
1. [Hypothesis] - [confirmed/eliminated] - [evidence]

### Fix Applied
[Description of the fix and why it works]

### Prevention
[How to prevent similar bugs: test, lint rule, type check, etc.]
```

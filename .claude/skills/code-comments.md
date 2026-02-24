---
name: code-comments
description: Add meaningful code comments following best practices
activation:
  keywords: ["add comments", "code comments", "comment code", "document code", "annotate"]
  file_patterns: []
---

# Code Comments

## Purpose
Add useful, maintainable comments to code that explain the "why" behind decisions, document public APIs, and clarify non-obvious logic.

## Instructions

1. **Analyze existing style**:
   - Check how the project currently comments code
   - Match the existing comment style (block vs line, format, tone)
   - Follow any documented style guide in the repo

2. **Comment the "why", not the "what"**:
   - BAD: `// increment counter` above `counter++`
   - GOOD: `// Retry up to 3 times because the upstream API has transient failures`
   - Do NOT comment obvious code (assignments, simple conditionals, standard loops)

3. **Public API documentation**:
   - Add JSDoc, docstrings, or equivalent for every public function, class, and method
   - Document parameters with name, type, and description
   - Document return values and their types
   - Document thrown exceptions and when they occur
   - Add at least one usage example for complex APIs

4. **Business logic**:
   - Explain domain-specific rules ("Tax is calculated after discount per state regulation X")
   - Document magic numbers with their origin ("86400 = seconds in a day")
   - Explain complex conditionals in plain language
   - Note regulatory or compliance requirements

5. **Markers and annotations**:
   - Use `TODO(author): description` for planned work with context
   - Use `FIXME(author): description` for known bugs with context
   - Use `HACK:` for intentional workarounds with explanation of why
   - Use `NOTE:` for important context that is easy to miss
   - Never leave bare TODO/FIXME without explanation

6. **What to avoid**:
   - Commented-out code (delete it; git has history)
   - Redundant comments restating the code
   - Journal comments (use git log instead)
   - Closing brace comments (`} // end if`) unless deeply nested
   - Misleading or outdated comments (worse than no comment)

## Output Format

Return the modified code with comments added inline. Preserve all existing formatting and indentation. Match the project's comment style exactly.

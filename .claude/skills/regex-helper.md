---
name: regex-helper
description: Build, explain, and test regular expressions with visual breakdowns
activation:
  keywords: ["regex", "regular expression", "regexp", "pattern matching", "regex help"]
  file_patterns: []
---

# Regex Helper

## Purpose
Construct, explain, and test regular expressions step by step, with
visual breakdowns and consideration for edge cases.

## Instructions

1. **Understand the Requirement**
   - Clarify exactly what needs to be matched
   - Identify what should NOT match (negative cases)
   - Determine if partial or full-string matching is needed
   - Ask about the regex flavor (JavaScript, Python, PCRE, POSIX)

2. **Build the Regex Step by Step**
   - Start with the simplest pattern that matches the core requirement
   - Add specificity incrementally
   - Use named groups for readability where supported
   - Prefer non-greedy quantifiers when appropriate
   - Anchor with `^` and `$` when full-string matching

3. **Explain Each Part**
   - Break down the regex into components
   - Explain what each token, quantifier, and group does
   - Note any special behavior (backtracking, lookahead)
   - Highlight gotchas (greedy vs lazy, multiline mode)

4. **Test Against Examples**
   - Provide positive matches (should match) with expected captures
   - Provide negative matches (should not match)
   - Test edge cases:
     - Empty string
     - Very long input
     - Special characters
     - Unicode characters
     - Boundary conditions

5. **Consider Edge Cases**
   - Unicode and international characters
   - Multiline input
   - Performance (catastrophic backtracking)
   - Differences between regex flavors
   - Escaping requirements in different contexts (string literals, config files)

6. **Provide Multiple Flavors** (if needed)
   - JavaScript: `/pattern/flags`
   - Python: `r"pattern"` with `re` module flags
   - POSIX: basic vs extended regex differences
   - Note flavor-specific features (lookbehind limits, named groups syntax)

## Output Format

```
# Regex: [brief description]

## Pattern
`/your-regex-here/flags`

## Visual Breakdown
/  ^          # start of string
   (\w+)     # group 1: one or more word characters
   @         # literal @ symbol
   ([\w.-]+) # group 2: domain name
   \.        # literal dot
   ([a-z]{2,})  # group 3: TLD
   $          # end of string
/i           # case-insensitive flag

## Matches (should match)
| Input | Match | Group 1 | Group 2 |
|-------|-------|---------|---------|

## Non-matches (should not match)
| Input | Reason |
|-------|--------|

## Flavor Variations
- JavaScript: `pattern`
- Python: `pattern`

## Notes
- Edge cases and caveats
```

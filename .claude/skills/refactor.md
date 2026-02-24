---
name: refactor
description: Guided code refactoring with incremental changes that preserve behavior
activation:
  keywords: ["refactor", "restructure", "clean up", "improve code", "code smell"]
  file_patterns: ["**/*"]
---

# Refactor

## Purpose
Guide systematic code refactoring to improve code quality, readability, and maintainability while preserving existing behavior.

## Instructions

### 1. Identify Code Smells
Scan the target code for common issues:
- **Long methods** (>20 lines) - candidates for Extract Method
- **Large classes** (>200 lines) - candidates for Extract Class
- **Duplicated code** - candidates for Extract Method/Template Method
- **Long parameter lists** (>3 params) - candidates for Introduce Parameter Object
- **Complex conditionals** - candidates for Replace Conditional with Polymorphism
- **Feature envy** - method uses another class's data more than its own
- **Primitive obsession** - overuse of primitives instead of small objects

### 2. Propose Refactoring Strategy
Before making changes:
- List each smell found with its location
- Propose a specific refactoring technique for each
- Order changes from lowest to highest risk
- Confirm the approach before proceeding

### 3. Apply Incrementally
For each refactoring step:
- Make ONE logical change at a time
- Verify the change preserves behavior (run tests if available)
- Commit or checkpoint before the next change

### 4. Common Refactoring Techniques
- **Extract Method**: Pull a code block into a named method
- **Rename**: Improve naming for clarity (variables, methods, classes)
- **Move**: Relocate code to a more appropriate module/class
- **Inline**: Remove unnecessary indirection
- **Simplify Conditionals**: Guard clauses, decompose conditional, consolidate
- **Extract Variable**: Name complex expressions
- **Replace Magic Numbers**: Use named constants

### 5. Verify
- Run existing tests to confirm no behavioral changes
- Review the diff to ensure nothing was lost
- Check that the refactored code is genuinely simpler

## Output Format
```
## Refactoring Plan

### Smells Identified
1. [Smell] in [file:line] - [brief description]

### Proposed Changes
1. [Technique]: [what and why]
   - Risk: low/medium/high
   - Files affected: [list]

### Changes Applied
- [x] Change 1 - verified by [test/manual check]
- [x] Change 2 - verified by [test/manual check]

### Before/After Summary
[Key improvements made]
```

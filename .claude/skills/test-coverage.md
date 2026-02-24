---
name: test-coverage
description: Analyze test coverage gaps and write targeted tests for critical uncovered paths
activation:
  keywords: ["test coverage", "coverage report", "uncovered code", "coverage gaps", "improve coverage"]
  file_patterns: ["**/coverage/**", "**/.nyc_output/**", "**/htmlcov/**"]
---

# Test Coverage

## Purpose
Analyze existing test coverage, identify meaningful gaps, and write targeted tests for critical uncovered paths. Focus on risk, not vanity metrics.

## Instructions

### 1. Run Coverage Tool
- Identify the project's coverage tool (istanbul/nyc, coverage.py, jacoco, etc.)
- Run coverage with the existing test suite
- Generate a report showing per-file and per-line coverage

### 2. Identify Gaps
Review uncovered code and categorize:
- **Critical gaps**: Business logic, security checks, data validation
- **Important gaps**: Error handling, edge cases in core paths
- **Low priority**: Boilerplate, trivial code, generated code

### 3. Prioritize by Risk
Do NOT chase 100% coverage. Prioritize:

| Priority | What to Cover | Why |
|----------|--------------|-----|
| 1 | Business rules and domain logic | Bugs here have direct user impact |
| 2 | Input validation and error handling | Security and reliability |
| 3 | Integration points and boundaries | Where systems interact, bugs hide |
| 4 | Complex conditional logic | High cyclomatic complexity = high risk |
| 5 (skip) | Simple getters/setters | No meaningful bugs possible |
| 5 (skip) | Framework boilerplate | Tested by the framework |

### 4. Write Targeted Tests
For each identified gap:
- Write the minimum tests needed to cover the critical path
- Focus on testing behavior, not implementation details
- Use the Arrange-Act-Assert pattern
- Include both positive and negative test cases

### 5. Verify Improvement
- Re-run coverage after adding tests
- Confirm the targeted paths are now covered
- Ensure new tests actually pass

### 6. Report
Summarize what was found and what was addressed.

## Output Format
```
## Coverage Analysis

### Current Coverage
- Overall: [X]%
- [Module A]: [X]%
- [Module B]: [X]%

### Critical Gaps Found
| File | Lines | Risk | Description |
|------|-------|------|-------------|
| [path] | [range] | High | [uncovered business logic] |

### Tests Added
| Test File | Covers | Cases |
|-----------|--------|-------|
| [path] | [target file/function] | [count] |

### Updated Coverage
- Overall: [X]% -> [Y]% (+[Z]%)

### Remaining Gaps (acceptable)
- [Low-risk items intentionally left uncovered]
```

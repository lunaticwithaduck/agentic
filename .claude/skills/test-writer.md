---
name: test-writer
description: Write tests for existing code using Arrange-Act-Assert with meaningful coverage
activation:
  keywords: ["write test", "add test", "test this", "create test", "unit test", "write tests for"]
  file_patterns: ["**/*.test.*", "**/*.spec.*", "**/test_*", "**/*_test.*"]
---

# Test Writer

## Purpose
Write well-structured tests for existing code, covering happy paths, edge cases, and error conditions using the project's test framework.

## Instructions

### 1. Read the Code Under Test
- Understand the function/class/module being tested
- Identify its public interface (what should be tested)
- Note dependencies that may need mocking
- Find the project's existing test patterns and framework

### 2. Identify Test Cases
Organize by category:

**Happy Path**: Normal expected usage
- Standard input produces expected output
- Common use cases work correctly

**Edge Cases**: Boundary conditions
- Empty input (empty string, empty array, null)
- Single element collections
- Maximum/minimum values
- Boundary values (off-by-one)

**Error Cases**: Failure modes
- Invalid input types
- Missing required fields
- Network/IO failures (if applicable)
- Permission/authorization failures

### 3. Write Tests
Follow the Arrange-Act-Assert (AAA) pattern:
```
test("descriptive name of what is being tested", () => {
  // Arrange: Set up test data and dependencies
  // Act: Call the function/method under test
  // Assert: Verify the result matches expectations
});
```

Rules:
- One logical assertion per test (related assertions are fine)
- Use descriptive test names that read like specifications
- Tests should be independent (no shared mutable state)
- Tests should be deterministic (no flaky behavior)
- Follow the project's existing test naming conventions

### 4. Handle Dependencies
- Mock external services (HTTP, database, file system)
- Use dependency injection where possible
- Keep mocks simple; only mock what is necessary
- Verify mock interactions when behavior matters

### 5. Coverage Priorities
Focus coverage on what matters most:
1. Business logic and domain rules
2. Error handling and validation
3. Integration points
4. Edge cases in algorithms
5. Skip: trivial getters/setters, framework boilerplate

## Output Format
Write test files that match the project's conventions:
- File naming: match existing pattern (e.g., `*.test.ts`, `test_*.py`)
- Location: match existing test directory structure
- Framework: use whatever the project already uses
- Include a brief comment explaining what each test group covers

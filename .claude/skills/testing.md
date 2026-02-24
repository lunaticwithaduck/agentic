---
name: testing
description: Writing and running unit and integration tests
activate: when writing tests, testing, test coverage, TDD, or "test"
---

# Testing Skill

## Test Types

### Unit Tests
- Test a single function or module in isolation
- Mock external dependencies (database, APIs, filesystem)
- Should run in milliseconds
- Location: alongside source files or in a `tests/` directory matching source structure

### Integration Tests
- Test how components work together
- Use real (or containerized) dependencies where practical
- May take seconds to run
- Location: `tests/integration/` or similar top-level test directory

## Naming Conventions

Use descriptive names that explain the scenario and expected outcome:

```
test_[function]_[scenario]_[expected_result]
```

Examples:
- `test_createUser_withValidEmail_returnsUser`
- `test_createUser_withDuplicateEmail_throwsConflictError`
- `test_parseConfig_withMissingField_usesDefault`

## Structure: Arrange-Act-Assert

Every test should follow this pattern:

```
// Arrange - Set up test data and dependencies
// Act - Call the function under test
// Assert - Verify the result
```

Keep each section short. If arrange is complex, extract a helper function.

## What to Test

### Always Test
- Happy path (normal usage)
- Empty/null/undefined inputs
- Boundary values (0, -1, MAX_INT, empty string)
- Error conditions and exception handling
- State transitions
- Return values AND side effects

### Test Edge Cases
- Concurrent access (if applicable)
- Large inputs
- Unicode and special characters
- Timezone-sensitive operations
- Permission/authorization boundaries

### Do Not Test
- Third-party library internals
- Language/framework behavior
- Private methods directly (test through public API)
- Trivial getters/setters

## Coverage Expectations

<!-- TODO: Customize these thresholds for your project -->
- **New code**: 80% line coverage minimum
- **Critical paths** (auth, payments, data mutations): 95%+ coverage
- **Utilities/helpers**: 100% coverage (they are simple to test)
- Focus on meaningful coverage, not just hitting lines

## Running Tests

<!-- TODO: Customize test commands for your project -->
```bash
# Run all tests
# npm test / pytest / cargo test

# Run specific test file
# npm test -- --testPathPattern=filename
# pytest tests/test_filename.py
# cargo test test_name

# Run with coverage
# npm test -- --coverage
# pytest --cov
# cargo tarpaulin
```

## Test Quality Checklist

- [ ] Tests are independent (no shared mutable state between tests)
- [ ] Tests are deterministic (same result every run)
- [ ] Tests are fast (unit tests < 100ms each)
- [ ] Test names describe the behavior being tested
- [ ] No logic in tests (no if/else, loops, or complex setup)
- [ ] Failures produce clear error messages
- [ ] Mocks are minimal (only mock what you must)

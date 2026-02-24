---
name: e2e-testing
description: End-to-end test guidance for critical user flows with stable selectors and async handling
activation:
  keywords: ["e2e test", "end to end", "integration test", "playwright", "cypress", "selenium", "browser test"]
  file_patterns: ["**/e2e/**", "**/cypress/**", "**/playwright/**", "**/*.e2e.*"]
---

# E2E Testing

## Purpose
Guide the creation of reliable end-to-end tests that cover critical user flows without being flaky or brittle.

## Instructions

### 1. Identify Critical User Flows
Prioritize flows by business impact:
- Authentication (sign up, login, logout, password reset)
- Core workflows (the main thing users do)
- Payment/checkout flows
- Data creation and modification
- Permission-gated features

### 2. Write Stable Tests

**Use reliable selectors** (in order of preference):
1. `data-testid` attributes: `[data-testid="submit-button"]`
2. ARIA roles and labels: `getByRole('button', { name: 'Submit' })`
3. Text content: `getByText('Submit')`
4. Avoid: CSS classes, tag hierarchies, XPath (all break easily)

**Handle async operations**:
- Wait for specific conditions, not arbitrary timeouts
- Use built-in waiters: `waitForSelector`, `waitForResponse`
- Assert on visible state changes, not implementation details
- Set reasonable timeouts with clear failure messages

**Manage test data**:
- Each test should set up its own data (no shared state between tests)
- Use API calls or database seeds for setup, not UI interactions
- Clean up test data in afterEach/afterAll hooks
- Use unique identifiers to avoid collisions in parallel runs

### 3. Test Structure
```
describe("User Flow: [flow name]", () => {
  beforeEach(() => {
    // Set up test data via API
    // Navigate to starting page
  });

  test("completes the happy path", async () => {
    // Step 1: [user action]
    // Assert: [expected result]
    // Step 2: [user action]
    // Assert: [expected result]
  });

  test("handles error case", async () => {
    // Trigger error condition
    // Assert: error is displayed to user
  });

  afterEach(() => {
    // Clean up test data
  });
});
```

### 4. Avoid Flaky Tests
- Never use fixed `sleep()` or `wait()` calls
- Do not depend on animation timing
- Isolate tests from each other completely
- Use retry logic only at the framework level, not in tests
- Make assertions specific and deterministic

### 5. CI Considerations
- Run E2E tests in a consistent environment (containers)
- Record screenshots/videos on failure for debugging
- Set up test parallelization for speed
- Keep E2E suite focused; do not duplicate unit test coverage

## Output Format
Write test files using the project's E2E framework. Include:
- Clear test descriptions that read as user stories
- Setup and teardown for test data
- Assertions at each meaningful step
- Comments explaining non-obvious waits or workarounds

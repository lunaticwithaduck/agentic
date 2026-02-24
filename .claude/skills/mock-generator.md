---
name: mock-generator
description: Generate typed test mocks, stubs, and realistic fixture data for external dependencies
activation:
  keywords: ["mock", "stub", "fixture", "test data", "fake", "test double", "mock generator"]
  file_patterns: ["**/__mocks__/**", "**/fixtures/**", "**/factories/**", "**/mocks/**"]
---

# Mock Generator

## Purpose
Generate well-typed mock implementations and realistic fixture data for testing code that depends on external services, databases, or other modules.

## Instructions

### 1. Identify Dependencies to Mock
- Read the code under test to find external dependencies
- Categorize each dependency:
  - **HTTP/API clients**: External service calls
  - **Database**: Queries, repositories, ORMs
  - **File system**: Read/write operations
  - **Third-party SDKs**: Payment, email, cloud services
  - **Internal modules**: Other parts of the codebase

### 2. Create Mock Implementations
For each dependency, create a mock that:
- Implements the same interface/type as the real dependency
- Returns predictable, controlled responses
- Tracks calls for assertion (spy functionality)
- Supports configurable return values per test

```
// Example structure (adapt to project's language)
const mockUserService = {
  getById: jest.fn().mockResolvedValue({ id: "1", name: "Test User" }),
  create: jest.fn().mockResolvedValue({ id: "2", name: "New User" }),
  delete: jest.fn().mockResolvedValue(undefined),
};
```

### 3. Generate Fixture Data
Create realistic test data that:
- Follows the actual data schema/types
- Uses realistic but obviously fake values (no real emails/phones)
- Covers variants needed for different test scenarios
- Includes edge case data (empty strings, max length, special chars)

Guidelines for fixture data:
- Names: "Alice Test", "Bob Mock" (clearly fake but realistic format)
- Emails: "alice@test.example.com" (use example.com domain)
- IDs: Use UUIDs or sequential numbers consistently
- Dates: Use fixed dates, not Date.now() (deterministic tests)
- Amounts: Use values that make math obvious (100, 250, not 17.43)

### 4. Handle Edge Cases in Mocks
Create mock variants for:
- **Success response**: Normal happy path data
- **Empty response**: Empty arrays, null results
- **Error response**: Network errors, 404s, 500s, timeouts
- **Partial data**: Missing optional fields
- **Pagination**: Multiple pages of results

### 5. Document Mock Behavior
Add comments explaining:
- What the mock simulates
- What the default return values are
- How to override behavior for specific tests
- Any limitations compared to the real implementation

### 6. Mock Organization
- Place mocks near the tests that use them or in a shared mocks directory
- Follow project conventions for mock file naming
- Export factory functions that create fresh mock instances (avoid shared state)

## Output Format
Generate mock files matching the project's conventions:
- Typed mock implementations for each dependency
- Fixture data files with realistic test data
- Factory functions for creating configurable mock instances
- Brief documentation comments on each mock's behavior

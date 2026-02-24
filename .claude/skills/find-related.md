---
name: find-related
description: Find all code, files, and concepts related to a given file or module
activation:
  keywords: ["find related", "related files", "related code", "trace dependencies", "dependency graph", "impact analysis"]
  file_patterns: []
---

# Find Related Code

## Purpose
Given a file or concept, trace all related code across the codebase to
understand dependencies, impacts, and what else needs to change.

## Instructions

1. **Identify the Starting Point**
   - Accept a file path, function name, class name, or concept
   - Read the target file/code to understand its purpose
   - Note its exports, interfaces, and public API

2. **Trace Direct Dependencies**
   - **Imports**: what does this file import? Follow each import.
   - **Exports**: what does this file export? Search for all consumers.
   - **Configuration**: what config files affect this code?
   - **Types/Interfaces**: what type definitions does it use or define?

3. **Find Related Tests**
   - Search for test files that import or reference this code
   - Check for test fixtures and mock data related to it
   - Identify integration tests that exercise this code path

4. **Find Related Documentation**
   - Search for docs that reference this file or concept
   - Check README files in the same and parent directories
   - Look for inline documentation and JSDoc/docstrings
   - Find API documentation (OpenAPI specs, GraphQL schemas)

5. **Find Related Configuration**
   - Environment variables referenced by this code
   - Build configuration that affects this module
   - Feature flags that gate this functionality
   - Database migrations related to this feature

6. **Identify Tightly-Coupled Modules**
   - Find modules that are always changed together (check git log)
   - Identify circular dependencies
   - Note modules that share internal knowledge (implementation coupling)
   - Flag violations of dependency direction rules

7. **Suggest Change Impact**
   - List everything that would need to change if this code changes
   - Categorize by risk: definitely affected, possibly affected, unlikely
   - Highlight cross-service or cross-package impacts
   - Note required test updates

## Output Format

```
# Related Code Map: [starting point]

## Direct Dependencies (imports)
- file.ext: reason for dependency

## Consumers (imported by)
- file.ext: how it uses this code

## Related Tests
- test-file.ext: what it tests

## Related Config
- config-file: what configuration

## Related Documentation
- doc-file: what it documents

## Change Impact
### Must change together
- file: reason
### May need changes
- file: reason

## Dependency Diagram
[Mermaid diagram showing relationships]
```

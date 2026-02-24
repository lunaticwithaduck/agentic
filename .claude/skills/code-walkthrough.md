---
name: code-walkthrough
description: Generate guided code walkthroughs explaining execution flow and architecture
activation:
  keywords: ["code walkthrough", "code tour", "explain code", "trace code", "code flow", "reading order"]
  file_patterns: []
---

# Code Walkthrough

## Purpose
Create a guided tour of the codebase that explains execution flow,
design patterns, and architecture to help developers understand the code.

## Instructions

1. **Identify the Entry Point**
   - Find the main entry file (main, index, app, server)
   - Note how the application bootstraps and initializes
   - Document the startup sequence and dependency injection
   - Identify configuration loading and validation

2. **Trace Execution Flow**
   - Follow a typical request/operation from entry to completion
   - Document each function/method call along the path
   - Note branching points and decision logic
   - Show how data transforms at each step
   - Highlight async boundaries and concurrency patterns

3. **Explain Significant Steps**
   - For each major step in the flow:
     - What it does and why
     - What design pattern it uses
     - What would break if this step were removed
     - Key edge cases it handles
   - Use code snippets to illustrate points

4. **Note Design Patterns**
   - Identify patterns in use: MVC, Repository, Observer, Factory, etc.
   - Explain why each pattern was chosen for its context
   - Show how the pattern is implemented in this codebase
   - Note any deviations from the standard pattern

5. **Highlight Configuration Points**
   - Environment variables that change behavior
   - Feature flags and their effects
   - Configuration files and their schema
   - Runtime-configurable vs build-time settings

6. **Document Side Effects**
   - External API calls and their purpose
   - Database operations (reads, writes, transactions)
   - File system operations
   - Message queue publications/subscriptions
   - Cache interactions
   - Logging and metrics emission

7. **Create Reading Order**
   - Suggest the order in which to read files for understanding
   - Group files by concern (not just directory)
   - Start with the most foundational, end with the most specific
   - Note which files can be skipped initially

## Output Format

```
# Code Walkthrough: [Project/Feature Name]

## Reading Order
1. `path/to/file.ext` - Why to read this first
2. `path/to/next.ext` - What this builds on

## Architecture Overview
[Mermaid diagram of high-level architecture]

## Execution Flow: [Scenario Name]

### Step 1: [Entry Point]
- File: `path/to/file.ext`
- What happens: description
- Key code:
  ```language
  relevant snippet
  ```
- Design pattern: pattern name

### Step 2: [Next Step]
...

## Design Patterns Used
- Pattern: where and why

## Configuration Points
- Variable/setting: what it controls

## Side Effects Map
- External system: what operations, triggered by what
```

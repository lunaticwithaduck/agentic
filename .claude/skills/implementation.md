---
name: implementation
description: Feature implementation with planning, testing, and conventions
activate: when implementing features, writing code, building functionality
---

# Implementation Skill

## Process

Follow these steps in order. Do not skip ahead.

### 1. Understand
- Read the task/issue description fully
- Identify acceptance criteria
- List any ambiguities and resolve them before coding

### 2. Explore
- Read existing code in the relevant area
- Identify patterns, conventions, and utilities already in use
- Check for similar implementations to follow as examples
- Review the project's CLAUDE.md for conventions

### 3. Plan
- Break the work into small, testable increments
- Identify which files need changes
- Determine if work can be parallelized across subagents
- Write the plan as a checklist before coding

### 4. Implement
- Follow existing project conventions (naming, structure, patterns)
- Write the smallest change that satisfies the requirement
- Handle error cases explicitly - no silent failures
- Add input validation at system boundaries
- Use types/interfaces to make contracts explicit
- Keep functions focused - one purpose per function

### 5. Test
- Write tests alongside the implementation, not after
- Cover the happy path, edge cases, and error cases
- Follow the testing skill for conventions
- Run existing tests to ensure no regressions

### 6. Document
- Update any affected documentation
- Add JSDoc/docstrings for public APIs
- Record architectural decisions if the change is significant

### 7. Review
- Self-review the diff before reporting completion
- Check against the code-review skill criteria
- Verify all acceptance criteria are met

## Parallel Work with Subagents

When multiple independent changes are needed:
- Spawn worker subagents for each independent unit
- Each subagent should follow this same implementation process
- Coordinate through the PM agent for dependencies
- Merge results and run full test suite after all complete

## Anti-Patterns to Avoid

- Writing code before reading existing patterns
- Implementing without clear acceptance criteria
- Skipping tests for "simple" changes
- Making multiple unrelated changes in one unit of work
- Ignoring existing utilities and reimplementing from scratch
- Over-engineering beyond what the task requires

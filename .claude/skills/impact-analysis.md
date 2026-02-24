---
name: impact-analysis
description: Analyze the impact of proposed changes across files, modules, and APIs
activation:
  keywords: ["impact analysis", "what will break", "affected files", "change impact", "risk assessment", "blast radius"]
  file_patterns: ["**/*"]
---

# Impact Analysis

## Purpose
Before making significant changes, analyze the full blast radius to identify all affected files, modules, APIs, and tests.

## Instructions

### 1. Understand the Proposed Change
- What is being added, modified, or removed?
- Which files are directly changed?
- What interfaces or contracts are affected?

### 2. Trace Dependencies
- Search for all imports/references to the changed code
- Use grep/ripgrep to find usages across the codebase
- Follow the dependency chain: direct dependents, then their dependents
- Check both code dependencies and configuration references

### 3. Identify Affected Areas
For each affected file/module, determine:
- **Direct impact**: Code that directly uses the changed interface
- **Indirect impact**: Code that depends on directly impacted code
- **Test impact**: Tests that cover any affected code
- **Config impact**: Configuration files that reference changed components
- **Documentation impact**: Docs that describe changed behavior

### 4. Assess Risk
Rate each affected area:
- **High risk**: Breaking change to public API, data schema, or core logic
- **Medium risk**: Internal interface change, behavior modification
- **Low risk**: Additive change, no existing behavior modified

### 5. Flag Breaking Changes
Explicitly call out:
- API contract changes (added required fields, removed endpoints)
- Database schema changes requiring migration
- Configuration changes requiring deployment coordination
- Removed or renamed public interfaces
- Changed default behavior

### 6. Identify Required Updates
- Tests that need updating
- Documentation that needs revision
- Migration scripts that need writing
- Downstream services that need notification

## Output Format
```
## Impact Analysis: [description of change]

### Direct Changes
| File | Change | Risk |
|------|--------|------|
| [path] | [what changes] | High/Med/Low |

### Affected Dependencies
| File | Relationship | Impact |
|------|-------------|--------|
| [path] | [imports/calls X] | [what breaks/changes] |

### Breaking Changes
- [List any breaking changes]

### Required Updates
- [ ] [Test/doc/config that must be updated]

### Risk Summary
Overall risk: [High/Medium/Low]
Confidence: [High/Medium/Low]
```

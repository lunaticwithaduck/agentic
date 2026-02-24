---
name: code-smell-detector
description: Detect and report code smells with severity ratings and fix suggestions
activation:
  keywords: ["code smell", "code quality", "technical debt", "smell", "anti-pattern", "scan code"]
  file_patterns: ["**/*"]
---

# Code Smell Detector

## Purpose
Systematically scan code for common code smells, report findings with severity levels, and suggest concrete improvements.

## Instructions

### 1. Scan for These Code Smells

**Bloaters** (code that has grown too large):
- Long Method: methods over 20 lines
- Large Class: classes with too many responsibilities
- Long Parameter List: more than 3-4 parameters
- Data Clumps: groups of variables that appear together repeatedly

**Object-Orientation Abusers**:
- Feature Envy: method uses another object's data more than its own
- Inappropriate Intimacy: classes that access each other's internals
- Refused Bequest: subclass ignores most of parent's interface
- Switch/Case Chains: long switch statements that should be polymorphism

**Change Preventers**:
- Divergent Change: one class changed for many different reasons
- Shotgun Surgery: one change requires edits across many classes
- Parallel Inheritance: adding a subclass in one hierarchy requires one in another

**Dispensables**:
- Dead Code: unreachable or unused code
- Speculative Generality: unused abstractions "just in case"
- Duplicate Code: same logic in multiple places
- Lazy Class: class that does too little to justify its existence

**Couplers**:
- Primitive Obsession: using primitives instead of small value objects
- Middle Man: class that only delegates to another
- Message Chains: long chains of method calls (a.b().c().d())

### 2. Assess Severity
- **Critical**: Actively causing bugs or blocking changes
- **High**: Significantly harms readability or maintainability
- **Medium**: Noticeable friction but manageable
- **Low**: Minor style issue or slight improvement opportunity

### 3. Suggest Fixes
For each smell, provide a specific refactoring technique to address it.

## Output Format
```
## Code Smell Report: [file/module]

### Summary
- Critical: [count] | High: [count] | Medium: [count] | Low: [count]

### Findings
| # | Smell | Location | Severity | Description | Suggested Fix |
|---|-------|----------|----------|-------------|---------------|
| 1 | [name] | [file:line] | Critical | [what's wrong] | [how to fix] |
| 2 | [name] | [file:line] | High | [what's wrong] | [how to fix] |

### Priority Recommendations
1. [Most impactful fix to make first]
2. [Second priority]
3. [Third priority]
```

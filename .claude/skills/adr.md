---
name: adr
description: Create Architecture Decision Records with standard template and sequential numbering
activation:
  keywords: ["adr", "architecture decision", "decision record", "design decision", "document decision"]
  file_patterns: ["docs/decisions/**", "**/adr-*"]
---

# Architecture Decision Records

## Purpose
Create and manage Architecture Decision Records (ADRs) to document significant technical decisions with their context and consequences.

## Instructions

### 1. Determine the Decision
- Clarify what decision is being made
- Identify the stakeholders and participants
- Understand the constraints and requirements

### 2. Find the Next Number
- Check `docs/decisions/` for existing ADRs
- Use the next sequential number (e.g., if 0005 exists, use 0006)
- Create the directory if it does not exist: `mkdir -p docs/decisions`

### 3. Write the ADR
Use this template structure:

```markdown
# ADR-NNNN: [Title]

**Date**: YYYY-MM-DD
**Status**: [Proposed | Accepted | Deprecated | Superseded by ADR-XXXX]
**Participants**: [who was involved in the decision]

## Context

[What is the issue that we're seeing that is motivating this decision?
What are the forces at play (technical, business, team)?]

## Decision

[What is the change that we're proposing and/or doing?
State the decision clearly and concisely.]

## Alternatives Considered

### [Alternative 1]
- Pros: [list]
- Cons: [list]

### [Alternative 2]
- Pros: [list]
- Cons: [list]

## Consequences

### Positive
- [Good outcome 1]
- [Good outcome 2]

### Negative
- [Tradeoff or risk 1]
- [Tradeoff or risk 2]

### Neutral
- [Side effect that is neither good nor bad]

## Related
- [Links to related ADRs, issues, or documents]
```

### 4. File Naming
Use the format: `docs/decisions/NNNN-short-title.md`
Example: `docs/decisions/0006-use-postgres-for-primary-store.md`

### 5. Update Status of Related ADRs
If this decision supersedes a previous one, update the old ADR's status to "Superseded by ADR-NNNN".

## Output Format
Create the ADR file at the correct path with all sections filled in. Report the file path and a brief summary of the decision.

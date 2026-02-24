---
name: technical-proposal
description: Write structured technical proposals and RFCs with design, migration, and rollout plans
activation:
  keywords: ["technical proposal", "rfc", "design doc", "technical design", "architecture proposal", "adr"]
  file_patterns: ["**/rfcs/**", "**/proposals/**", "**/adrs/**", "**/design-docs/**"]
---

# Technical Proposal / RFC

## Purpose
Create a structured technical proposal that clearly communicates the problem,
solution, alternatives, and implementation plan for technical decisions.

## Instructions

1. **Problem Statement**
   - Describe the current situation and its limitations
   - Quantify the impact (performance numbers, error rates, developer time)
   - Explain why this needs to be solved now
   - Define who is affected and how

2. **Proposed Solution**
   - Describe the solution at a high level first
   - Include architecture diagrams (use Mermaid)
   - Explain the key technical decisions and their rationale
   - Define the public API or interface changes
   - Describe data model changes if applicable

3. **Alternatives Considered**
   - List at least 2-3 alternatives
   - For each: brief description, pros, cons, reason for rejection
   - Include "do nothing" as an alternative with its consequences
   - Be fair in the assessment; acknowledge trade-offs in the chosen solution

4. **Technical Design**
   - Detailed component design
   - Data flow diagrams
   - API contracts (request/response formats)
   - Database schema changes
   - Infrastructure requirements
   - Security considerations
   - Performance implications and benchmarks

5. **Migration Plan**
   - Steps to move from current state to proposed state
   - Backward compatibility strategy
   - Data migration approach
   - Feature flag strategy for gradual rollout
   - Rollback plan if issues are discovered

6. **Rollout Strategy**
   - Phased rollout plan with criteria for each phase
   - Monitoring and alerting for the new system
   - Success criteria for each phase
   - Timeline with milestones

7. **Effort Estimate**
   - Break down into work packages
   - Estimate each package (T-shirt sizes or days)
   - Identify parallelizable work
   - Note dependencies between packages

8. **Risks and Mitigations**
   - Technical risks and how to address them
   - Operational risks during migration
   - Dependencies on other teams or systems
   - Unknown unknowns and how to discover them

## Output Format

```
# RFC: [Title]

**Status**: Draft | In Review | Accepted | Rejected
**Author**: [name]
**Date**: [date]
**Reviewers**: [list]

## Problem Statement
## Proposed Solution
## Alternatives Considered
## Technical Design
## Migration Plan
## Rollout Strategy
## Effort Estimate
## Risks and Mitigations
## Open Questions
## References
```

Include Mermaid diagrams for architecture and data flow.

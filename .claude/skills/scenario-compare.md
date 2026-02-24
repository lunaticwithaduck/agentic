---
name: scenario-compare
description: Compare architectural approaches with weighted criteria and scoring matrix
activation:
  keywords: ["compare", "tradeoff", "which approach", "pros and cons", "evaluate options", "decision matrix"]
  file_patterns: []
---

# Scenario Compare

## Purpose
Systematically compare two or more architectural approaches or technical options using defined criteria and a scoring matrix.

## Instructions

### 1. Define the Scenarios
- Clearly name each option/approach being compared
- Write a 1-2 sentence description of each
- Ensure they are genuine alternatives solving the same problem

### 2. Define Evaluation Criteria
Select relevant criteria from this list (or add custom ones):
- **Performance**: Speed, throughput, latency
- **Scalability**: Ability to handle growth
- **Maintainability**: Ease of understanding and modifying
- **Complexity**: Implementation and operational complexity
- **Cost**: Development time, infrastructure, licensing
- **Team familiarity**: Team's existing knowledge and experience
- **Reliability**: Failure modes, recovery, data durability
- **Security**: Attack surface, compliance requirements
- **Time to market**: How quickly can this be delivered
- **Flexibility**: Ease of adapting to future requirements

### 3. Weight the Criteria
- Assign weights (1-5) based on what matters most for this decision
- Discuss weights with the user if priorities are unclear
- Not all criteria need to be included; pick the 4-6 most relevant

### 4. Score Each Scenario
- Rate each scenario against each criterion (1-5 scale)
- 1 = Poor, 2 = Below Average, 3 = Adequate, 4 = Good, 5 = Excellent
- Provide brief justification for each score

### 5. Calculate and Analyze
- Multiply score by weight for weighted scores
- Sum weighted scores per scenario
- Identify which scenario wins overall and per-criterion

### 6. Make a Recommendation
- State which option is recommended and why
- Note any caveats or conditions that might change the recommendation
- Identify what would need to be true for a different option to win

## Output Format
```
## Scenario Comparison: [decision being made]

### Options
1. **[Option A]**: [description]
2. **[Option B]**: [description]

### Evaluation Matrix
| Criteria | Weight | Option A | Option B | Notes |
|----------|--------|----------|----------|-------|
| [criterion] | [1-5] | [1-5] | [1-5] | [why] |
| **Weighted Total** | | **[sum]** | **[sum]** | |

### Recommendation
**[Option X]** is recommended because [rationale].

### Caveats
- [Condition that might change this recommendation]
```

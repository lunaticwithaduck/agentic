---
name: explain-code
description: Deep code explanation with structured walkthrough of purpose, flow, and design decisions
activation:
  keywords: ["explain", "how does this work", "walk me through", "what does this do", "understand code"]
  file_patterns: ["**/*"]
---

# Explain Code

## Purpose
Provide a deep, structured explanation of code to help developers understand purpose, control flow, design decisions, and mental models.

## Instructions

### 1. Read and Identify Scope
- Read the target code completely before explaining
- Identify the boundaries: single function, class, module, or system
- Note the language, framework, and any domain-specific patterns

### 2. Explain Purpose
- What problem does this code solve?
- Where does it fit in the larger system?
- What are the inputs and outputs?

### 3. Trace the Control Flow
- Walk through execution path step by step
- Identify entry points and exit points
- Note branching logic and loops
- Highlight error handling paths

### 4. Identify Patterns and Techniques
- Design patterns used (Factory, Observer, Strategy, etc.)
- Architectural patterns (MVC, Repository, Middleware, etc.)
- Language idioms and conventions
- Data structures and their purpose

### 5. Assess Complexity
- Time and space complexity of key operations
- Cognitive complexity (how hard is it to understand?)
- Coupling and cohesion observations

### 6. Build a Mental Model
- Provide an analogy or simplified model if helpful
- Describe the "shape" of the data as it flows through
- Identify the core invariants the code maintains

## Output Format
```
## Code Explanation: [name/file]

### Purpose
[What this code does and why it exists]

### High-Level Flow
1. [Step 1]
2. [Step 2]
3. [Step N]

### Key Components
- **[Component]**: [role and responsibility]

### Design Decisions
- [Decision]: [rationale]

### Patterns Used
- [Pattern]: [how it's applied here]

### Complexity Notes
- [Observation about complexity, performance, or readability]

### Mental Model
[A simplified way to think about this code]
```

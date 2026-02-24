---
name: system-design
description: System design guidance covering requirements, components, interfaces, and trade-offs
activation:
  keywords: ["system design", "architecture", "design system", "high level design", "component design"]
  file_patterns: []
---

# System Design

## Purpose
Guide the design of systems and subsystems by clarifying requirements, identifying components, defining interfaces, and documenting trade-offs.

## Instructions

### 1. Clarify Requirements

**Functional Requirements**:
- What are the core use cases?
- What are the inputs and outputs?
- What are the user-facing features?

**Non-Functional Requirements**:
- Performance targets (latency, throughput)
- Availability and reliability requirements
- Data consistency model (strong, eventual)
- Security and compliance constraints
- Scale expectations (users, data volume, request rate)

### 2. Identify Components
- Break the system into logical components/services
- Define each component's single responsibility
- Identify which components are stateful vs. stateless
- Determine synchronous vs. asynchronous communication

### 3. Define Interfaces
For each component boundary:
- Input/output data formats
- Communication protocol (REST, gRPC, events, queues)
- Error handling contract
- Authentication and authorization

### 4. Data Design
- Identify data entities and relationships
- Choose storage technology (SQL, NoSQL, cache, file)
- Define data access patterns (read-heavy, write-heavy)
- Plan for data growth and archival

### 5. Consider Cross-Cutting Concerns
- **Scalability**: Horizontal vs. vertical scaling strategy
- **Reliability**: Failure modes and recovery (retries, circuit breakers)
- **Observability**: Logging, metrics, tracing, alerting
- **Security**: Authentication, authorization, encryption, input validation
- **Deployment**: CI/CD, rollback strategy, feature flags

### 6. Document Trade-offs
Every design decision involves trade-offs. For each key decision:
- What was chosen and what was rejected
- Why this option was preferred
- What the downsides are and how they are mitigated

## Output Format
```
## System Design: [system name]

### Requirements
- [Functional requirement 1]
- [Non-functional: latency < Xms, Yk requests/sec]

### Component Diagram
[Mermaid diagram showing components and their relationships]

### Components
| Component | Responsibility | Stateful? | Communication |
|-----------|---------------|-----------|---------------|
| [name] | [what it does] | Yes/No | REST/gRPC/Events |

### Data Model
[Key entities and relationships]

### Key Trade-offs
| Decision | Chosen | Alternative | Rationale |
|----------|--------|-------------|-----------|
| [what] | [option A] | [option B] | [why A] |

### Open Questions
- [Unresolved design questions]
```

---
name: mermaid-diagram
description: Create Mermaid diagrams for visualizing architecture, flows, relationships, and processes
activation:
  keywords: ["mermaid", "diagram", "flowchart", "sequence diagram", "class diagram", "er diagram", "state diagram", "visualize"]
  file_patterns: []
---

# Mermaid Diagram Creation

## Purpose
Create clear, well-structured Mermaid diagrams to visualize architecture,
processes, data flows, and relationships in the codebase.

## Instructions

1. **Understand What to Visualize**
   - Clarify the subject and audience
   - Determine the level of detail needed
   - Identify the key entities and relationships
   - Decide what to include and what to omit for clarity

2. **Choose the Right Diagram Type**
   - **Flowchart** (`flowchart TD`): decision trees, process flows, algorithms
   - **Sequence** (`sequenceDiagram`): API calls, request/response flows, interactions
   - **Class** (`classDiagram`): object models, type hierarchies, interfaces
   - **ER** (`erDiagram`): database schemas, entity relationships
   - **State** (`stateDiagram-v2`): state machines, lifecycle, status transitions
   - **Gantt** (`gantt`): project timelines, parallel work streams
   - **Pie** (`pie`): proportional data, distribution breakdowns
   - **Mindmap** (`mindmap`): brainstorming, concept mapping, hierarchies
   - **Timeline** (`timeline`): chronological events, milestones
   - **Git graph** (`gitGraph`): branching strategies, release flows

3. **Build the Diagram**
   - Start with the main flow or primary entities
   - Add relationships and connections
   - Use clear, concise labels (not full sentences)
   - Group related elements with subgraphs where helpful
   - Keep the diagram readable (10-20 nodes maximum per diagram)
   - Split complex diagrams into multiple focused ones

4. **Apply Styling**
   - Use consistent node shapes for element types:
     - Rectangles for processes/actions
     - Diamonds for decisions
     - Rounded for start/end
     - Cylinders for databases
   - Use arrow styles to show relationship types:
     - Solid for direct dependencies
     - Dashed for optional/async
   - Add colors sparingly for emphasis or grouping
   - Use descriptive link labels

5. **Validate and Refine**
   - Verify Mermaid syntax is correct
   - Check that the diagram renders as intended
   - Ensure labels are readable (not too long)
   - Verify flow direction is intuitive (top-down or left-right)
   - Remove unnecessary complexity

6. **Add Context**
   - Include a title or heading above the diagram
   - Add a brief legend if using colors or special shapes
   - Note any simplifications made
   - Provide supporting text for complex diagrams

## Output Format

```markdown
## [Diagram Title]

[Brief description of what this diagram shows]

```mermaid
[diagram type]
    [diagram content]
```

### Notes
- Key points about the diagram
- Simplifications or assumptions made
```

For complex systems, provide multiple diagrams at different levels:
1. High-level overview (10 nodes or fewer)
2. Detailed views of specific subsystems
3. Sequence diagrams for key flows

---

## Timelines & Gantt Charts

When given a list of events or a project schedule, choose between:

- **`timeline`** — milestone events on a single axis, grouped by phase. Best for: "show me what happened when."
- **`gantt`** — tasks with durations and dependencies. Best for: "show me when work runs in parallel."

### Timeline example
```mermaid
timeline
    title Project Milestones
    section Q1
        Kickoff         : 2024-01-08
        Design complete : 2024-02-20
    section Q2
        Beta launch     : 2024-04-01
        Public launch   : 2024-06-15
```

### Gantt example
```mermaid
gantt
    title Feature Development
    dateFormat  YYYY-MM-DD
    section Backend
        API design       : done,    api,  2024-01-08, 5d
        Implementation   : active,  impl, after api,  10d
        Testing          : crit,          after impl, 5d
    section Frontend
        UI mockups       : done,          2024-01-08, 7d
        Integration      :               after impl, 7d
```

Pattern detection to include in output:
- **Gaps**: periods > 2 weeks with no activity — call them out
- **Clusters**: many events within a short window — label the phase
- **Critical path**: tasks where delay blocks everything else — mark `crit`

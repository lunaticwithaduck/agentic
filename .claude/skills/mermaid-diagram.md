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

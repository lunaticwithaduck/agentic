---
name: data-modeling
description: Design data models and ERD diagrams from requirements
activation:
  keywords: ["data model", "erd", "entity relationship", "data modeling", "domain model", "er diagram"]
  file_patterns: []
---

# Data Modeling & ERD Design

## Purpose
Translate business requirements into a well-structured data model with entity-relationship diagrams, considering access patterns and future growth.

## Instructions

1. **Extract entities from requirements**:
   - Read the requirements or domain description
   - Identify nouns as candidate entities (User, Order, Product)
   - Identify verbs as candidate relationships (places, contains, belongs to)
   - Distinguish entities from attributes (Address might be an entity or an attribute depending on reuse)
   - Look for implicit entities (a many-to-many relationship often implies a join entity)

2. **Define attributes for each entity**:
   - Identify the natural key (email for user, SKU for product)
   - Choose appropriate data types (string, integer, decimal, timestamp, enum)
   - Mark required vs optional attributes
   - Identify derived or computed attributes (total_price = quantity * unit_price)
   - Add metadata: created_at, updated_at, created_by

3. **Establish relationships**:
   - **One-to-one (1:1)**: Rare; consider merging into one entity unless separation is justified
   - **One-to-many (1:N)**: The "many" side holds the foreign key
   - **Many-to-many (M:N)**: Requires a junction/join table
   - Define cardinality and participation (required or optional on each side)
   - Document cascade behavior (what happens when a parent is deleted?)

4. **Create the ERD**:
   - Use Mermaid syntax for portability and version control
   - Show entity names, key attributes, and relationship labels
   - Use standard notation for cardinality (||--o{, }o--||, etc.)
   - Keep the diagram readable (10-15 entities max per diagram; split large models)

5. **Consider access patterns**:
   - What queries will run most frequently?
   - Which entities are read-heavy vs write-heavy?
   - Where will denormalization help performance?
   - What data is accessed together? (inform table design and indexing)

6. **Plan for growth**:
   - Which tables will grow fastest?
   - Where might partitioning be needed?
   - What access patterns might change at scale?
   - Document assumptions about data volume

## Output Format

```markdown
## Entity List
Brief description of each entity and its purpose.

## ERD
\```mermaid
erDiagram
    USER ||--o{ ORDER : places
    ORDER ||--|{ ORDER_ITEM : contains
    PRODUCT ||--o{ ORDER_ITEM : "included in"
    USER {
        uuid id PK
        string email UK
        string name
        timestamp created_at
    }
\```

## Assumptions & Decisions
- List of design decisions and their rationale
```

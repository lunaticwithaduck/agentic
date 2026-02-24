---
name: database-schema
description: Design database schemas with proper normalization, indexes, and constraints
activation:
  keywords: ["database schema", "schema design", "table design", "create table", "database design", "data model"]
  file_patterns: ["**/*.sql", "**/schema.prisma", "**/models/**", "**/migrations/**"]
---

# Database Schema Design

## Purpose
Design well-structured database schemas with appropriate data types, relationships, indexes, and constraints for the given domain.

## Instructions

1. **Identify entities and relationships**:
   - Extract entities from the domain description or requirements
   - Determine cardinality: one-to-one, one-to-many, many-to-many
   - Identify ownership and lifecycle dependencies (cascade behavior)
   - Look for polymorphic relationships and inheritance patterns

2. **Define tables and columns**:
   - Use singular table names (user, not users) or match project convention
   - Every table gets a primary key (prefer UUID or BIGINT auto-increment)
   - Add `created_at` and `updated_at` timestamps to all tables
   - Add `deleted_at` for soft deletes if the domain requires it
   - Choose the most specific data type (use ENUM, not VARCHAR for status fields)
   - Set NOT NULL as the default; allow NULL only with a reason

3. **Normalize to 3NF**:
   - 1NF: No repeating groups; each column holds one value
   - 2NF: No partial dependencies on composite keys
   - 3NF: No transitive dependencies
   - Denormalize intentionally when needed for read performance, and document why

4. **Add constraints**:
   - Foreign keys with appropriate ON DELETE behavior (CASCADE, SET NULL, RESTRICT)
   - UNIQUE constraints for natural keys (email, slug, etc.)
   - CHECK constraints for value validation
   - Default values where sensible

5. **Design indexes**:
   - Index every foreign key column
   - Index columns used in WHERE, ORDER BY, and JOIN clauses
   - Consider composite indexes for common multi-column queries
   - Use partial indexes for filtered queries
   - Do not over-index: each index slows writes

6. **Many-to-many relationships**:
   - Create explicit join tables
   - Add additional columns to join tables if the relationship has attributes
   - Consider whether the relationship has its own lifecycle

## Output Format

Provide SQL DDL statements (or the project's ORM schema format) with:
- Inline comments explaining design decisions
- An ERD in Mermaid syntax showing relationships
- A notes section documenting denormalization decisions and index rationale

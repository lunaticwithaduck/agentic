---
name: seed-generator
description: Generate realistic database seed data with proper relationships
activation:
  keywords: ["seed data", "seed database", "fake data", "test data", "seed generator", "fixtures", "sample data"]
  file_patterns: ["**/seeds/**", "**/seed.*", "**/fixtures/**"]
---

# Database Seed Data Generator

## Purpose
Analyze a database schema and generate realistic, relationship-aware seed data suitable for development and testing.

## Instructions

1. **Analyze the schema**:
   - Read all table definitions, columns, types, and constraints
   - Map foreign key relationships to determine insertion order
   - Identify required vs nullable columns
   - Note unique constraints (seed data must not violate them)
   - Find enum or check constraints to generate valid values

2. **Determine insertion order**:
   - Build a dependency graph from foreign keys
   - Insert parent tables before child tables
   - Handle circular references with nullable FKs (insert with NULL, update after)
   - Example order: roles -> users -> user_roles -> products -> orders -> order_items

3. **Generate realistic data**:
   - Use Faker-style patterns appropriate to each column type:
     - Names: realistic first/last names
     - Emails: based on generated names (john.doe@example.com)
     - Dates: within reasonable ranges, respecting ordering (created < updated)
     - Prices: realistic ranges with proper decimal places
     - Addresses: properly structured with city/state/zip
     - Text: lorem ipsum or domain-appropriate content
     - Status fields: distribute across valid enum values
   - Use deterministic seeds for reproducible data generation

4. **Include edge cases**:
   - At least one record with maximum-length strings
   - Nullable columns should have some NULL values
   - Include special characters in text fields (unicode, quotes, ampersands)
   - Include boundary values for numeric fields (0, negative if allowed, max)
   - Include records that test business logic edge cases

5. **Volume guidelines**:
   - Reference/lookup tables: all valid values
   - Primary entities (users, products): 20-50 records
   - Transaction tables (orders, events): 100-200 records
   - Enough data to test pagination, filtering, and sorting
   - Not so much that seed scripts take long to run

6. **Generate the seed script**:
   - Use the project's framework conventions (SQL, ORM seeds, migration-based)
   - Make seeds idempotent (safe to run multiple times)
   - Use upsert or check-before-insert where possible
   - Include clear comments for each group of seed data
   - Wrap in a transaction for atomicity

## Output Format

Provide a seed script in the project's preferred format (raw SQL, ORM seed file, or programmatic script) with comments explaining the data relationships and any edge cases included.

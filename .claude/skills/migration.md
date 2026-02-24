---
name: migration
description: Create safe, reversible database migrations
activation:
  keywords: ["migration", "database migration", "schema change", "alter table", "migrate", "db migrate"]
  file_patterns: ["**/migrations/**", "**/migrate/**", "**/alembic/**", "**/db/migrate/**"]
---

# Database Migration Guide

## Purpose
Create safe, reversible database migrations that handle schema changes with zero or minimal downtime in production environments.

## Instructions

1. **Assess the change**:
   - What is being added, changed, or removed?
   - Is it backward compatible with the current application code?
   - Will it lock tables? For how long? (Large table ALTERs can lock for minutes)
   - Can it be split into smaller, safer migrations?

2. **Create the migration**:
   - Use the project's migration framework (Prisma, Alembic, Knex, ActiveRecord, etc.)
   - Name migrations descriptively: `add_email_verified_to_users`, not `migration_042`
   - One logical change per migration file
   - Include both UP (apply) and DOWN (rollback) operations
   - Test the DOWN migration before deploying the UP

3. **Safe patterns for production**:
   - **Adding a column**: Add as nullable or with a default first, backfill data, then add NOT NULL constraint
   - **Removing a column**: Stop reading the column in code first, deploy, then drop the column in the next release
   - **Renaming a column**: Add new column, backfill, update code to use new column, drop old column (3-step deploy)
   - **Adding an index**: Use `CREATE INDEX CONCURRENTLY` (Postgres) or equivalent to avoid locking
   - **Changing a type**: Add new column with new type, migrate data, swap in code, drop old column

4. **Data migrations**:
   - Handle data transformations in batches (not one giant UPDATE)
   - Log progress for long-running data migrations
   - Make data migrations idempotent (safe to re-run)
   - Test with production-scale data volumes

5. **Pre-deployment checklist**:
   - Migration tested against a copy of production data
   - Rollback tested and verified
   - Application code works with both old and new schema (during rolling deploy)
   - Database backed up
   - Team notified of migration window

6. **Document breaking changes**:
   - Note which application version requires this migration
   - Document the order of operations (deploy code first, or migrate first?)
   - List any manual steps required
   - Update ORM models/schemas to match the new database state

## Output Format

Provide the migration file(s) in the project's framework format with inline comments explaining the change, a rollback procedure, and deployment instructions.

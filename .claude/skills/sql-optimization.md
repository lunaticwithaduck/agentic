---
name: sql-optimization
description: Optimize SQL queries using execution plans, indexes, and rewrites
activation:
  keywords: ["sql optimization", "slow query", "query performance", "explain", "sql tuning", "index optimization", "n+1"]
  file_patterns: ["**/*.sql", "**/queries/**"]
---

# SQL Query Optimization

## Purpose
Analyze and optimize slow SQL queries by examining execution plans, adding indexes, rewriting queries, and fixing common anti-patterns.

## Instructions

1. **Analyze the current query**:
   - Read the query and understand its intent
   - Identify the tables involved and their approximate row counts
   - Check existing indexes on those tables
   - Run `EXPLAIN ANALYZE` (Postgres) or `EXPLAIN` (MySQL) to see the execution plan
   - Look for: full table scans (Seq Scan), nested loops on large tables, high row estimates

2. **Common problems and fixes**:

   **Full table scans**:
   - Add an index on the columns used in WHERE and JOIN conditions
   - Consider composite indexes for multi-column filters
   - Check that index columns are not wrapped in functions (`WHERE YEAR(date)` cannot use an index on `date`)

   **N+1 queries**:
   - Identify ORM code that runs a query per item in a loop
   - Replace with a JOIN or a single IN query
   - Use eager loading (include/preload/joinedload in your ORM)

   **Subquery performance**:
   - Rewrite correlated subqueries as JOINs where possible
   - Use CTEs for readability but note they may not optimize the same as inline subqueries
   - EXISTS is usually faster than IN for subqueries

   **SELECT ***:
   - Replace with explicit column list
   - Reduces data transfer, may enable index-only scans

   **Missing pagination**:
   - Add LIMIT/OFFSET or cursor-based pagination
   - For large offsets, use keyset pagination (WHERE id > last_seen_id)

3. **Index design**:
   - Create indexes that match query patterns (column order matters in composite indexes)
   - Leading column in composite index should be the most selective
   - Consider covering indexes (include all selected columns) for hot queries
   - Use partial indexes for filtered queries (e.g., `WHERE status = 'active'`)
   - Drop unused indexes (they slow down writes)

4. **Query rewrites**:
   - Use JOINs instead of multiple separate queries
   - Aggregate in the database, not in application code
   - Use window functions for running totals, rankings, etc.
   - Batch large updates/deletes to avoid long locks

5. **Benchmark**:
   - Compare EXPLAIN output before and after changes
   - Measure actual execution time with realistic data volumes
   - Test under concurrent load if possible
   - Document the improvement (e.g., "Query time reduced from 2.3s to 15ms")

## Output Format

Provide: the original query, the optimized query with inline comments, any new indexes needed (as CREATE INDEX statements), and a summary of the improvement with before/after metrics.

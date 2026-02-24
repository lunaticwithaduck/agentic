---
name: performance-optimization
description: Profile-driven performance analysis and optimization with measurable results
activation:
  keywords: ["performance", "optimize", "slow", "latency", "bottleneck", "profile", "benchmark", "speed up"]
  file_patterns: ["**/*"]
---

# Performance Optimization

## Purpose
Identify and fix performance bottlenecks using measurement-driven optimization rather than guesswork.

## Instructions

### 1. Profile First, Do Not Guess
- Identify what "slow" means: response time, throughput, memory, startup?
- Use profiling tools appropriate to the runtime
- Measure current baseline performance with concrete numbers
- Identify the actual bottleneck before changing anything

### 2. Identify Bottlenecks
Check these categories in order of typical impact:
- **I/O operations**: database queries, network calls, file system
- **Algorithmic complexity**: O(n^2) loops, unnecessary iterations
- **Memory**: excessive allocations, leaks, large object graphs
- **Concurrency**: lock contention, thread pool exhaustion
- **Serialization**: JSON parsing, data transformation overhead

### 3. Optimize Hot Paths
Focus on the code that runs most frequently:
- Reduce unnecessary work (caching, memoization, lazy evaluation)
- Batch I/O operations (bulk queries, connection pooling)
- Avoid N+1 query patterns
- Use appropriate data structures (hash maps vs. lists for lookups)
- Minimize allocations in tight loops

### 4. Algorithmic Improvements
- Review time complexity of critical operations
- Consider space-time tradeoffs
- Look for opportunities to short-circuit early
- Evaluate if precomputation or indexing would help

### 5. Database and Query Optimization
- Check for missing indexes on frequently queried columns
- Review query plans (EXPLAIN)
- Eliminate N+1 queries with eager loading or joins
- Consider read replicas or caching for read-heavy workloads

### 6. Measure After
- Re-run the same benchmark after each change
- Quantify the improvement (percentage, absolute time)
- Verify no regressions in correctness
- Document the tradeoffs made

## Output Format
```
## Performance Analysis

### Baseline
- [Metric]: [value] (e.g., p95 latency: 450ms)

### Bottlenecks Found
| Location | Issue | Impact |
|----------|-------|--------|
| [file:line] | [description] | [high/medium/low] |

### Optimizations Applied
1. [Change]: [before] -> [after] ([improvement %])

### Final Results
- [Metric]: [before] -> [after] ([improvement])

### Tradeoffs
- [What was traded for performance: memory, complexity, etc.]
```

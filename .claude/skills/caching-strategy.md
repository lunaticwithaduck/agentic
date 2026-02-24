---
name: caching-strategy
description: Design and implement caching strategies for optimal performance
activation:
  keywords: ["caching", "cache", "redis", "cdn", "cache invalidation", "cache strategy"]
  file_patterns: ["**/cache*", "**/redis*", "**/memcached*"]
---

# Caching Strategy

## Purpose
Design and implement effective caching to improve performance and reduce
load, with proper invalidation strategies to maintain data consistency.

## Instructions

1. **Identify Cacheable Resources**
   - Read-heavy, write-light data (user profiles, config, product catalogs)
   - Expensive computations (aggregations, reports, search results)
   - External API responses with rate limits
   - Static or semi-static content (translations, feature flags)
   - Avoid caching: real-time data, frequently changing state, user-specific sensitive data

2. **Choose Cache Layer**
   - **Browser cache**: static assets, API responses with Cache-Control
   - **CDN**: public content, images, scripts, geographically distributed
   - **Application (in-memory)**: hot data, session data, small datasets
   - **Distributed (Redis/Memcached)**: shared across instances, larger datasets
   - **Database query cache**: expensive queries, materialized views
   - Often use multiple layers together

3. **Define Cache Keys**
   - Use consistent, descriptive key patterns: `resource:id:variant`
   - Include version or hash for cache busting
   - Namespace keys by service or feature
   - Keep keys short but unambiguous
   - Document the key schema

4. **Set TTLs (Time-to-Live)**
   - Match TTL to data volatility:
     - Static config: 24h+
     - User profiles: 5-15 minutes
     - Search results: 1-5 minutes
     - Real-time data: do not cache
   - Add jitter to TTLs to prevent stampede on expiry
   - Use shorter TTLs initially, increase based on monitoring

5. **Implement Cache Invalidation**
   - **Time-based**: TTL expiry (simplest, eventual consistency)
   - **Event-based**: invalidate on write/update operations
   - **Version-based**: increment version in key on change
   - Choose write strategy: write-through, write-behind, or write-around
   - Document what triggers invalidation for each cached resource

6. **Handle Thundering Herd**
   - Use lock/mutex for cache population (only one request rebuilds)
   - Implement stale-while-revalidate pattern
   - Use probabilistic early expiration
   - Pre-warm cache for predictable traffic spikes

7. **Set Cache Headers**
   - `Cache-Control`: max-age, s-maxage, no-cache, no-store, private, public
   - `ETag`: content hash for conditional requests
   - `Last-Modified`: timestamp-based conditional requests
   - `Vary`: specify which request headers affect the response

8. **Monitor**
   - Track hit rate, miss rate, eviction rate
   - Alert on sudden drop in hit rate
   - Monitor memory usage and eviction pressure
   - Log cache misses with reason for debugging

## Output Format

Provide:
1. Cache architecture diagram (which layers, what is cached where)
2. Cache key schema documentation
3. TTL configuration table
4. Invalidation strategy per resource
5. Implementation code for the chosen cache layer
6. Monitoring recommendations

---
name: rate-limiting
description: Design and implement rate limiting for APIs and services
activation:
  keywords: ["rate limit", "rate limiting", "throttle", "throttling", "api limits", "request limiting"]
  file_patterns: ["**/rate-limit*", "**/throttle*", "**/limiter*"]
---

# Rate Limiting Implementation

## Purpose
Design and implement rate limiting to protect services from abuse,
ensure fair usage, and maintain system stability.

## Instructions

1. **Choose Algorithm**
   - **Fixed Window**: simple, counts per time window. Can burst at boundaries.
   - **Sliding Window Log**: precise, stores each request timestamp. Memory heavy.
   - **Sliding Window Counter**: good balance of precision and efficiency.
   - **Token Bucket**: allows controlled bursts. Tokens refill at fixed rate.
   - **Leaky Bucket**: smooths out bursts. Processes at constant rate.
   - Recommendation: Token Bucket for APIs (allows bursts), Sliding Window for strict limits.

2. **Define Rate Limits**
   - Set limits per dimension: user, API key, IP address, endpoint
   - Consider different limits for:
     - Read vs write operations
     - Authenticated vs anonymous users
     - Different subscription tiers
     - Specific expensive endpoints
   - Start conservative, relax based on monitoring
   - Example: 100 req/min per user, 1000 req/min per API key

3. **Implement Response Headers**
   - `X-RateLimit-Limit`: maximum requests allowed in window
   - `X-RateLimit-Remaining`: requests remaining in current window
   - `X-RateLimit-Reset`: Unix timestamp when the window resets
   - `Retry-After`: seconds to wait (on 429 responses)
   - Include these headers on ALL responses, not just 429s

4. **Handle 429 Responses**
   - Return HTTP 429 Too Many Requests
   - Include clear error message with limit details
   - Include `Retry-After` header
   - Body should explain the limit and how to request increases
   - Log rate limit events for abuse detection

5. **Distributed Rate Limiting**
   - Use a shared store (Redis) for multi-instance deployments
   - Use atomic operations (MULTI/EXEC or Lua scripts in Redis)
   - Handle store unavailability (fail open or fail closed based on risk)
   - Consider eventual consistency in distributed counters
   - Use sliding window with Redis sorted sets for precision

6. **Configure Tiers**
   - Define tiers: free, basic, pro, enterprise
   - Map each tier to rate limits per endpoint category
   - Store tier info with the API key or user record
   - Allow temporary limit increases for migrations or events

7. **Bypass and Exceptions**
   - Internal service-to-service calls: bypass or higher limits
   - Health check endpoints: exempt from rate limiting
   - Admin endpoints: separate higher limits
   - Use separate rate limit keys for bypassed traffic
   - Document all exceptions

## Output Format

Lead with working code. Always provide:
1. Implementation code for the rate limiter (complete, runnable)
2. Middleware integration code showing where it plugs in
3. Algorithm choice with one-sentence justification (inline with code)
4. 429 response example with `Retry-After` and `X-RateLimit-*` headers shown
5. One-paragraph production note (Redis for distributed, X-Forwarded-For spoofing risk, fail-open vs fail-closed)

Do NOT open with theory or algorithm comparison — go straight to code.
Config tables and monitoring recommendations are optional; include them only if the implementation is complete.

## Failure Modes

- **Theory-first framing** — opening with algorithm comparison tables delays implementation code.
  Rubrics for implementation tasks weight working code highest; theory adds no score until code is present.

---
name: middleware-design
description: Design middleware and interceptor chains for cross-cutting concerns
activation:
  keywords: ["middleware", "interceptor", "pipeline", "filter chain", "cross-cutting"]
  file_patterns: ["**/middleware/**", "**/interceptors/**", "**/filters/**"]
---

# Middleware/Interceptor Design

## Purpose
Design and implement middleware chains that handle cross-cutting concerns
cleanly, with proper ordering and single responsibility.

## Instructions

1. **Identify Cross-Cutting Concerns**
   - Authentication and authorization
   - Request/response logging
   - Rate limiting and throttling
   - CORS handling
   - Compression (gzip, brotli)
   - Error handling and formatting
   - Request validation
   - Request ID / correlation ID injection
   - Security headers
   - Content negotiation

2. **Order Middleware Correctly**
   The order matters. Recommended sequence:
   1. Request ID generation (first, so all logs have it)
   2. Security headers
   3. CORS
   4. Rate limiting (reject early, save resources)
   5. Compression
   6. Body parsing
   7. Logging (request start)
   8. Authentication
   9. Authorization
   10. Input validation
   11. Route handler
   12. Error handling (last, catches everything)

3. **Keep Middleware Focused**
   - Each middleware handles exactly one concern
   - Keep middleware stateless when possible
   - Accept configuration via factory functions
   - Avoid tight coupling between middleware
   - Make middleware independently testable

4. **Handle Async Operations**
   - Ensure proper async/await or promise chain handling
   - Catch errors and pass to error-handling middleware
   - Set timeouts on external calls within middleware
   - Handle stream/chunked responses correctly

5. **Implement Short-Circuit Patterns**
   - Rate limiter returns 429 without reaching handler
   - Auth middleware returns 401/403 without reaching handler
   - Validation middleware returns 400 without reaching handler
   - Cache middleware returns cached response without reaching handler
   - Health check bypasses most middleware

6. **Conditional Middleware**
   - Apply middleware selectively to routes or groups
   - Skip middleware for health checks and static assets
   - Use different middleware stacks for public vs authenticated routes
   - Support feature flags to enable/disable middleware

7. **Document the Middleware Chain**
   - List all middleware in execution order
   - Document what each middleware does and when it short-circuits
   - Note any dependencies between middleware
   - Include timing/performance expectations

## Output Format

Provide:
1. Middleware chain diagram showing execution order
2. Implementation of each middleware function
3. Configuration and registration code
4. Documentation of the complete chain
5. Test examples for middleware in isolation and in chain

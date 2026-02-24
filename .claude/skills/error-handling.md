---
name: error-handling
description: Design and implement a consistent error handling strategy across the application
activation:
  keywords: ["error handling", "error strategy", "exception handling", "error classes", "error responses"]
  file_patterns: ["**/errors/**", "**/exceptions/**", "**/middleware/error*"]
---

# Error Handling Strategy

## Purpose
Define and implement a consistent, secure error handling strategy that properly
categorizes errors, provides useful responses, and maintains observability.

## Instructions

1. **Define Error Hierarchy**
   - **Operational errors**: expected failures (network timeout, invalid input, not found)
   - **Programmer errors**: bugs (null reference, type error, assertion failure)
   - Operational errors should be handled; programmer errors should crash and restart
   - Create a base error class with: code, message, statusCode, isOperational

2. **Create Typed Error Classes**
   - `ValidationError`: invalid input (400)
   - `AuthenticationError`: not authenticated (401)
   - `AuthorizationError`: not authorized (403)
   - `NotFoundError`: resource not found (404)
   - `ConflictError`: state conflict (409)
   - `RateLimitError`: too many requests (429)
   - `InternalError`: unexpected failure (500)
   - Each should carry a machine-readable error code (e.g., `USER_NOT_FOUND`)

3. **Implement Consistent Error Responses**
   - Use a standard envelope:
     ```json
     {
       "error": {
         "code": "VALIDATION_ERROR",
         "message": "Human-readable description",
         "details": [{"field": "email", "issue": "invalid format"}],
         "requestId": "uuid"
       }
     }
     ```
   - Map error classes to HTTP status codes
   - Include request ID for support correlation

4. **Error Logging**
   - Log full error details server-side (stack trace, context, user)
   - Include correlation/request IDs for tracing
   - Log at appropriate levels: warn for operational, error for programmer
   - Capture surrounding context (input data, user ID, action attempted)

5. **Async Error Handling**
   - Wrap async operations with proper error catching
   - Handle promise rejections globally as a safety net
   - Implement circuit breakers for external service calls
   - Set timeouts on all external calls
   - Use retry logic with exponential backoff for transient failures

6. **Graceful Degradation**
   - Define fallback behavior for each external dependency
   - Implement feature flags to disable failing features
   - Return cached/default data when live data is unavailable
   - Communicate degraded state to users clearly

7. **Security**
   - Never expose stack traces, internal paths, or system info to clients
   - Sanitize error messages of sensitive data
   - Use generic messages for auth failures (prevent enumeration)
   - Log security-related errors at higher severity

## Output Format

Provide:
1. Base error class definition
2. Specific error subclasses
3. Error handling middleware/interceptor
4. Example usage in a route/handler
5. Error response examples for each error type

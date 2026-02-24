---
name: logging-strategy
description: Implement structured logging with proper levels, context, and sensitive data masking
activation:
  keywords: ["logging", "log strategy", "structured logging", "log levels", "observability"]
  file_patterns: ["**/logger*", "**/logging*", "**/log.*", "**/winston*", "**/pino*"]
---

# Logging Strategy

## Purpose
Implement a structured logging strategy that enables effective debugging,
monitoring, and auditing while protecting sensitive data.

## Instructions

1. **Use Structured Logging**
   - Output logs as JSON for machine parsing
   - Include consistent fields: timestamp, level, message, service, context
   - Avoid string concatenation in log messages
   - Use key-value pairs for all contextual data
   - Example: `{"timestamp":"ISO8601","level":"info","message":"User created","userId":"123","service":"auth"}`

2. **Define Log Levels**
   - **fatal**: system is unusable, immediate attention required
   - **error**: operation failed, needs investigation
   - **warn**: unexpected condition, not a failure yet
   - **info**: significant business events (user signup, payment processed)
   - **debug**: detailed diagnostic info (function entry/exit, variable values)
   - Set default level by environment: production=info, staging=debug, dev=debug

3. **Include Context**
   - Request ID / correlation ID on every log entry
   - User ID (when authenticated)
   - Timestamp in ISO 8601 format (UTC)
   - Service name and version
   - Environment (production, staging, development)
   - For errors: stack trace, error code, input that caused failure

4. **Correlation IDs for Distributed Tracing**
   - Generate a unique ID at the entry point of each request
   - Propagate via headers (X-Request-ID, traceparent) across services
   - Include in all log entries within that request lifecycle
   - Pass to downstream service calls

5. **Log Rotation and Retention**
   - Configure rotation by size (100MB) or time (daily)
   - Retain logs based on environment and compliance needs
   - Compress rotated logs
   - Ship to central aggregation (ELK, Loki, CloudWatch, Datadog)

6. **Mask Sensitive Data**
   - Never log: passwords, tokens, credit card numbers, SSNs
   - Mask PII: email (show first 2 chars), phone (show last 4 digits)
   - Redact request/response bodies of sensitive endpoints
   - Use an allowlist approach for loggable fields where possible

7. **Performance Considerations**
   - Use async logging to avoid blocking the event loop
   - Avoid logging in hot loops
   - Use lazy evaluation for expensive log messages
   - Sample verbose logs in high-traffic production systems

## Output Format

Provide:
1. Logger configuration and initialization code
2. Middleware for request/response logging with correlation IDs
3. Examples of proper logging at each level
4. Sensitive data masking utility
5. Environment-specific configuration

---
name: monitoring
description: Set up monitoring, alerting, and observability for applications
activation:
  keywords: ["monitoring", "observability", "alerting", "metrics", "logging", "tracing", "dashboard"]
  file_patterns: ["**/monitoring/**", "**/prometheus/**", "**/grafana/**", "**/alerts/**"]
---

# Monitoring & Observability

## Purpose
Design monitoring, logging, and alerting strategies that provide visibility into application health and help diagnose issues quickly.

## Instructions

1. **Define key metrics (the four golden signals)**:
   - **Latency**: Response time for requests (p50, p95, p99)
   - **Traffic**: Request rate (requests per second)
   - **Errors**: Error rate (5xx responses, exceptions, failed jobs)
   - **Saturation**: Resource usage (CPU, memory, disk, connections)

2. **Application-specific metrics**:
   - Business metrics (signups, orders, payments)
   - Queue depth and processing time
   - Cache hit/miss ratios
   - External dependency response times
   - Background job success/failure rates

3. **Logging strategy**:
   - Use structured logging (JSON format) for machine parsing
   - Include: timestamp, level, message, request_id, user_id, service_name
   - Log levels: ERROR (action needed), WARN (investigate), INFO (state changes), DEBUG (dev only)
   - Do NOT log: passwords, tokens, PII, credit card numbers
   - Centralize logs (ELK, CloudWatch, Datadog, etc.)
   - Set retention policies by environment

4. **Alerting**:
   - Alert on symptoms (high error rate) not causes (high CPU)
   - Set thresholds based on SLOs, not arbitrary values
   - Use severity levels: critical (page), warning (ticket), info (log)
   - Include runbook links in alert messages
   - Avoid alert fatigue: every alert should be actionable
   - Group related alerts to reduce noise

5. **Distributed tracing** (for microservices):
   - Propagate trace IDs across service boundaries
   - Instrument HTTP clients and message consumers
   - Record span timing for each service hop
   - Use OpenTelemetry for vendor-neutral instrumentation

6. **Dashboards**:
   - Create a service overview dashboard (golden signals at a glance)
   - Create per-service detail dashboards
   - Include time range selectors and environment filters
   - Show SLO burn rate if applicable

## Output Format

Provide a monitoring plan document with: metrics inventory table, logging configuration, alerting rules with thresholds, and dashboard layout recommendations. Include code snippets for instrumentation where applicable.

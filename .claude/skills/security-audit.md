---
name: security-audit
description: Perform a comprehensive security audit of the codebase against OWASP Top 10 and common vulnerability patterns
activation:
  keywords: ["security audit", "security review", "owasp", "security check", "pentest", "security scan"]
  file_patterns: ["**/*.config.*", "**/.env*", "**/auth*", "**/login*"]
---

# Security Audit

## Purpose
Conduct a thorough security audit of the codebase, identifying vulnerabilities
aligned with OWASP Top 10 categories, insecure configurations, and common
security anti-patterns.

## Instructions

1. **Injection Flaws (OWASP A03)**
   - Search for raw SQL queries, string concatenation in queries
   - Check for command injection via `exec`, `spawn`, `system` calls
   - Look for LDAP, XML, and NoSQL injection vectors

2. **Broken Authentication (OWASP A07)**
   - Review authentication flows for weaknesses
   - Check password hashing algorithms (reject MD5, SHA1)
   - Verify session management (expiry, rotation, secure flags)
   - Look for missing MFA or brute-force protections

3. **Cross-Site Scripting (OWASP A03)**
   - Find unescaped user input rendered in HTML
   - Check for `innerHTML`, `dangerouslySetInnerHTML`, or template literals with user data
   - Verify Content-Security-Policy headers

4. **Insecure Deserialization (OWASP A08)**
   - Search for deserialization of untrusted data (pickle, yaml.load, JSON.parse on raw input)
   - Check for object injection patterns

5. **Hardcoded Secrets**
   - Grep for API keys, tokens, passwords, connection strings
   - Check `.env` files are in `.gitignore`
   - Look for secrets in config files, comments, or test fixtures

6. **Input Validation**
   - Verify all user inputs are validated and sanitized
   - Check for missing length limits, type checks, allowlists
   - Review file upload handling (type, size, path traversal)

7. **Dependency Vulnerabilities**
   - Run the appropriate audit tool (npm audit, pip-audit, etc.)
   - Flag dependencies with known CVEs

8. **CORS and Security Headers**
   - Check CORS configuration (reject wildcard origins in production)
   - Verify presence of: X-Content-Type-Options, X-Frame-Options, Strict-Transport-Security
   - Review CSP directives

9. **Sensitive Data Exposure (OWASP A02)**
   - Check for PII logged or exposed in error messages
   - Verify encryption at rest and in transit
   - Review API responses for over-fetching sensitive fields

## Severity Rating Guide

Rate every finding using this framework — do not rely on intuition:

**CRITICAL** — Exploitable with no special conditions; direct, immediate impact:
- Remote code execution (any vector)
- Authentication bypass (attacker logs in as any user without credentials)
- SQL injection in a login or admin endpoint
- Plaintext password storage (breach = full credential exposure)
- Exposed private keys or certificates in source

**HIGH** — Serious impact; may require one precondition (e.g., authenticated user):
- Hardcoded secrets / API keys / session signing keys in source code
  → Any repo access enables session forgery or full account takeover
- IDOR / broken object-level authorization (missing ownership checks)
  → Authenticated user can read/modify any other user's data by ID
- Session fixation — attacker can hijack any session post-login
- Reflected or stored XSS in an authenticated context
- SQL injection in non-login endpoints
- JWT secret exposed or algorithm set to `none`

**MEDIUM** — Real vulnerability but requires additional conditions or has limited impact:
- CSRF without sensitive state change
- Verbose error messages exposing stack traces or DB schema
- Missing rate limiting on non-auth endpoints
- Insecure direct object reference with partial data exposure only
- Weak (but not broken) cryptographic choices

**LOW** — Defense-in-depth gap; low standalone impact:
- Missing security headers (X-Content-Type-Options, X-Frame-Options)
- Overly broad CORS without credentials
- Dependency with no known exploit path
- Missing logging or audit trail

**Common misratings to avoid:**
- Hardcoded `secret_key` / `SECRET_KEY` = **HIGH**, not MEDIUM — session signing keys enable cookie forgery → full account takeover
- IDOR (missing auth on object endpoints) = **HIGH**, not LOW — OWASP A01 #1 ranked; full PII enumeration is a serious breach
- Missing HTTPS enforcement = **HIGH** in production, MEDIUM in dev
- `eval()` on user input = **CRITICAL** (remote code execution)

## Output Format

Present findings as a security report:

```
# Security Audit Report

## Summary
- Critical: X | High: X | Medium: X | Low: X

## Findings

### [CRITICAL] Finding Title
- **Category**: OWASP category
- **Location**: file:line
- **Description**: What the issue is
- **Impact**: What could happen if exploited
- **Recommendation**: How to fix it

### [HIGH] Finding Title
...
```

Order findings by severity: Critical > High > Medium > Low.
Include code snippets showing the vulnerable code and the recommended fix.

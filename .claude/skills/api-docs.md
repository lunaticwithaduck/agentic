---
name: api-docs
description: Generate comprehensive API documentation from route/endpoint definitions
activation:
  keywords: ["api docs", "api documentation", "document api", "endpoint docs", "swagger", "openapi"]
  file_patterns: ["**/routes/**", "**/controllers/**", "**/api/**", "**/endpoints/**"]
---

# API Documentation Generator

## Purpose
Scan a codebase for route/endpoint definitions and generate structured API documentation in OpenAPI-compatible or markdown format.

## Instructions

1. **Discover endpoints**: Search the codebase for route definitions, controller files, and API handler functions. Look for common patterns:
   - Route decorators (`@app.route`, `@Get`, `@Post`, `router.get`, etc.)
   - Framework-specific routing (Express, FastAPI, Gin, Spring, etc.)
   - Middleware and guard annotations for auth requirements

2. **For each endpoint, document**:
   - HTTP method (GET, POST, PUT, PATCH, DELETE)
   - Path with parameter placeholders (e.g., `/users/:id`)
   - Path parameters and query parameters with types
   - Request body schema (fields, types, required/optional, validation rules)
   - Response schema for success and error cases
   - Authentication/authorization requirements
   - Rate limiting or other middleware

3. **Extract from code**:
   - Read validation schemas (Zod, Joi, class-validator, Pydantic models)
   - Read TypeScript interfaces or type aliases used in request/response
   - Read existing comments, JSDoc, or docstrings
   - Check for response status codes in handler logic

4. **Generate documentation**:
   - Group endpoints by resource/domain (e.g., Users, Orders, Auth)
   - Include a curl or fetch example for each endpoint
   - Document error responses with status codes and messages
   - Add a summary table at the top listing all endpoints

5. **Validate completeness**:
   - Ensure every public route is documented
   - Verify parameter types match validation logic
   - Flag undocumented endpoints with TODO markers

## Output Format

```markdown
# API Documentation

## Overview
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/users | List all users |

## Endpoints

### GET /api/users
**Description**: List all users
**Auth**: Bearer token required
**Query Parameters**:
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| page | integer | No | Page number (default: 1) |

**Response 200**:
\```json
{ "data": [...], "total": 100 }
\```

**Example**:
\```bash
curl -H "Authorization: Bearer TOKEN" https://api.example.com/users?page=1
\```
```

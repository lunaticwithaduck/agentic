---
name: api-design
description: REST and GraphQL API design with endpoints, schemas, error handling, and versioning
activation:
  keywords: ["api design", "rest api", "graphql", "endpoint design", "api schema", "api contract"]
  file_patterns: ["**/routes/**", "**/api/**", "**/controllers/**", "**/schema*"]
---

# API Design

## Purpose
Design clean, consistent, and well-documented APIs following REST conventions or GraphQL best practices.

## Instructions

### 1. Define Resources
- Identify the core resources (nouns, not verbs)
- Determine resource relationships (one-to-many, many-to-many)
- Plan the URL hierarchy: `/resources/{id}/sub-resources`

### 2. Define Endpoints (REST)
For each resource, define standard operations:
| Method | Path | Purpose |
|--------|------|---------|
| GET | /resources | List (with pagination) |
| GET | /resources/{id} | Get single resource |
| POST | /resources | Create new resource |
| PUT | /resources/{id} | Full update |
| PATCH | /resources/{id} | Partial update |
| DELETE | /resources/{id} | Delete resource |

### 3. Request/Response Schemas
- Define request body schemas with required/optional fields
- Define response schemas consistently across endpoints
- Use consistent field naming (camelCase or snake_case, pick one)
- Include timestamps (created_at, updated_at) on resources

### 4. Error Handling
Use consistent error response format:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Human-readable message",
    "details": [{"field": "email", "issue": "invalid format"}]
  }
}
```
Standard HTTP status codes:
- 200: Success, 201: Created, 204: No Content
- 400: Bad Request, 401: Unauthorized, 403: Forbidden, 404: Not Found
- 409: Conflict, 422: Unprocessable Entity
- 500: Internal Server Error

### 5. Pagination
Choose a pagination strategy:
- **Offset**: `?page=2&per_page=20` (simple, allows jumping)
- **Cursor**: `?after=cursor_token&limit=20` (performant, stable)
- Include total count and navigation links in response

### 6. Versioning
- URL prefix: `/v1/resources` (most common)
- Header: `Accept: application/vnd.api+json; version=1`
- Plan for backward compatibility

### 7. Authentication
- Define auth mechanism (API key, OAuth2, JWT)
- Specify which endpoints require authentication
- Document rate limiting policy

## Output Format
```
## API Design: [service name]

### Base URL
`https://api.example.com/v1`

### Authentication
[Method and details]

### Endpoints
#### [Resource Name]
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /resources | Yes | List resources |

#### Request/Response Examples
[JSON examples for key endpoints]

### Error Codes
[Application-specific error codes and meanings]

### Pagination
[Strategy and format]
```

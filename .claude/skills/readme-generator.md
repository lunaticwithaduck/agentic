---
name: readme-generator
description: Generate or update README files based on project analysis
activation:
  keywords: ["readme", "generate readme", "update readme", "project readme", "write readme"]
  file_patterns: ["**/README.md", "**/readme.md"]
---

# README Generator

## Purpose
Analyze a project's structure, dependencies, and configuration to generate a comprehensive, scannable README file.

## Step 1: Analyze the Project

```bash
# Read these files first (in order of priority)
package.json / pyproject.toml / Cargo.toml / go.mod    # name, version, description, scripts
.env.example / config.example.*                         # required environment variables
Dockerfile / docker-compose.yml                         # deployment context
.github/workflows/                                      # CI/CD info, test commands
src/ / app/ / lib/                                      # understand what it actually does
```

## Step 2: Choose the Right Template

### Library / Package README
For npm packages, Python libraries, Rust crates, etc.

### Application README
For web apps, APIs, CLIs, desktop apps.

---

## Template: Application

```markdown
# Project Name

> One sentence that explains what it does and who it's for.
> Example: "REST API for managing team task boards, built with Fastify and PostgreSQL."

[![CI](https://github.com/org/repo/actions/workflows/ci.yml/badge.svg)](https://github.com/org/repo/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

- **[Feature 1]**: Brief description of the value it provides
- **[Feature 2]**: Brief description
- **[Feature 3]**: Brief description

## Prerequisites

- [Runtime] [version]+ — e.g., Node.js 18+, Python 3.11+
- [Database] — e.g., PostgreSQL 15
- [Other hard requirement]

## Quick Start

```bash
# Clone and install
git clone https://github.com/org/repo.git
cd repo
npm install           # exact command for this project

# Configure environment
cp .env.example .env
# Edit .env with your values (see Configuration section)

# Start development server
npm run dev
```

Visit http://localhost:3000

## Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | — | PostgreSQL connection string |
| `PORT` | No | `3000` | HTTP server port |
| `JWT_SECRET` | Yes | — | Secret for signing JWT tokens (min 32 chars) |

## Development

```bash
npm run dev          # Start dev server with hot reload
npm test             # Run unit tests
npm run test:e2e     # Run end-to-end tests
npm run lint         # Lint and typecheck
npm run build        # Production build
```

## Project Structure

```
src/
├── routes/          # HTTP route handlers
├── services/        # Business logic
├── models/          # Database models
├── middleware/      # Express middleware
└── utils/           # Shared utilities
tests/
├── unit/
└── e2e/
```

## Deployment

```bash
# Build
npm run build

# Set production environment variables, then:
npm start
```

See [docs/deployment.md](docs/deployment.md) for platform-specific guides (Docker, Fly.io, Railway).

## Contributing

1. Fork the repo and create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes and add tests
3. Ensure CI passes: `npm test && npm run lint`
4. Open a pull request

## License

[MIT](LICENSE) © [Author Name]
```

---

## Template: Library / Package

```markdown
# package-name

> One-sentence description of what the library does.

[![npm version](https://img.shields.io/npm/v/package-name)](https://www.npmjs.com/package/package-name)
[![CI](https://github.com/org/repo/actions/workflows/ci.yml/badge.svg)](...)

## Installation

```bash
npm install package-name
```

## Usage

```ts
import { mainExport } from 'package-name';

// Most common use case first
const result = mainExport({ option: 'value' });
```

## API

### `mainExport(options)`

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `option` | `string` | `'default'` | What it controls |

**Returns**: `ResultType`

**Throws**: `ValidationError` if options are invalid.

## License

[MIT](LICENSE)
```

---

## Badge Reference

```markdown
# CI status (GitHub Actions)
[![CI](https://github.com/ORG/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/ORG/REPO/actions/workflows/ci.yml)

# npm version
[![npm version](https://img.shields.io/npm/v/PACKAGE)](https://www.npmjs.com/package/PACKAGE)

# License
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

# Code coverage (Codecov)
[![codecov](https://codecov.io/gh/ORG/REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/ORG/REPO)
```

---

## Rules

- **Setup steps must be copy-pasteable** — run them yourself mentally; no "install dependencies" without the exact command
- **Every required env var must appear** in the Configuration table
- **Include a usage example** before any other documentation — show, don't just describe
- **If updating an existing README**: preserve custom sections, update only what's outdated, never delete without checking
- **Collapsible sections** (`<details>`) for: full env var list (>6 vars), full project structure tree, advanced configuration

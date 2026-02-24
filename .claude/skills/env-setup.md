---
name: env-setup
description: Analyze project requirements and generate environment setup instructions
activation:
  keywords: ["env setup", "environment setup", "dev setup", "install dependencies", "project setup"]
  file_patterns: ["**/package.json", "**/requirements.txt", "**/Cargo.toml", "**/go.mod", "**/Makefile", "**/docker-compose*", "**/.tool-versions"]
---

# Environment Setup

## Purpose
Analyze project requirements and generate complete environment setup
instructions, handling platform differences and version management.

## Instructions

1. **Analyze Project Requirements**
   - Identify all manifest files (package.json, requirements.txt, Cargo.toml, etc.)
   - Detect the language(s) and required runtime versions
   - Check for version manager configs (.nvmrc, .python-version, .tool-versions)
   - Identify required system-level dependencies
   - Note Docker/container requirements if applicable

2. **List Required Tools and Versions**
   - Runtime(s): language and exact version
   - Package manager(s): npm, yarn, pnpm, pip, cargo, etc.
   - Build tools: make, cmake, webpack, etc.
   - Database(s): PostgreSQL, MySQL, Redis, MongoDB, etc.
   - External services: message queues, search engines, etc.
   - Development tools: linters, formatters, pre-commit hooks

3. **Generate Setup Commands**
   - Order commands by dependency (install runtime before packages)
   - Include version manager installation if needed
   - Provide package installation commands
   - Include database creation and migration commands
   - Add seed data commands if available
   - Include build steps

4. **Handle Platform Differences**
   - Provide commands for macOS (Homebrew), Linux (apt/dnf), and Windows
   - Note WSL requirements for Windows
   - Handle architecture differences (x86 vs ARM/Apple Silicon)
   - Document Docker-based setup as a platform-agnostic alternative

5. **Create .env.example**
   - Scan codebase for environment variable references
   - Create `.env.example` with all required variables
   - Add descriptions as comments for each variable
   - Indicate which values are required vs optional
   - Note where to obtain values (API dashboards, team leads)

6. **Verify Installation**
   - Provide verification commands for each tool
   - Include a smoke test (e.g., run tests, start dev server)
   - Document expected output for successful setup
   - List common setup failures and their solutions

## Output Format

```
# Environment Setup Guide

## Prerequisites
| Tool | Version | Install Command | Verify |
|------|---------|----------------|--------|
| Node.js | 20.x | `nvm install 20` | `node -v` |

## Setup Steps

### 1. Clone and Install
\`\`\`bash
commands here
\`\`\`

### 2. Environment Configuration
\`\`\`bash
cp .env.example .env
# Edit .env with your values
\`\`\`

### 3. Database Setup
\`\`\`bash
commands here
\`\`\`

### 4. Verify Setup
\`\`\`bash
commands here
\`\`\`

## .env.example
\`\`\`
# variable descriptions and placeholders
\`\`\`

## Troubleshooting
| Issue | Solution |
|-------|----------|
```

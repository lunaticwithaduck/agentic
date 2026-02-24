---
name: readme-generator
description: Generate or update README files based on project analysis
activation:
  keywords: ["readme", "generate readme", "update readme", "project readme"]
  file_patterns: ["**/README.md", "**/readme.md"]
---

# README Generator

## Purpose
Analyze a project's structure, dependencies, and configuration to generate a comprehensive, scannable README file.

## Instructions

1. **Analyze the project**:
   - Read package.json, pyproject.toml, Cargo.toml, go.mod, or equivalent for project metadata
   - Identify the project's purpose from existing docs, comments, or code
   - List all dependencies and dev dependencies
   - Find configuration files (.env.example, config files)
   - Check for Docker, CI/CD, and deployment configs
   - Identify the test framework and how to run tests

2. **Determine sections needed**:
   - Project name and one-line description
   - Badges (build status, coverage, version, license)
   - Table of contents (for READMEs longer than 3 sections)
   - Features / highlights
   - Prerequisites and requirements
   - Installation / setup steps
   - Usage examples with code blocks
   - Configuration / environment variables
   - Project structure overview (if complex)
   - API reference or link to docs
   - Contributing guidelines
   - License

3. **Write content**:
   - Use clear, direct language
   - Start with what the project does in one sentence
   - Make setup steps copy-pasteable (exact commands)
   - Include at minimum one usage example
   - Document every required environment variable
   - Link to related documentation rather than duplicating

4. **If updating an existing README**:
   - Preserve custom content the author added
   - Update only outdated sections (deps, setup steps, etc.)
   - Do not remove sections without asking

## Output Format

Standard GitHub-flavored markdown with:
- H1 for project name
- Badges on the line immediately after the title
- Table of contents using markdown links
- Code blocks with language identifiers
- Collapsible sections (`<details>`) for lengthy content

---
name: onboarding-guide
description: Generate onboarding documentation for new team members joining the project
activation:
  keywords: ["onboarding", "getting started", "new developer", "project setup", "new team member", "first contribution"]
  file_patterns: ["**/CONTRIBUTING*", "**/SETUP*", "**/GETTING_STARTED*"]
---

# Onboarding Guide Generator

## Purpose
Analyze the project and generate comprehensive onboarding documentation
that enables a new team member to become productive quickly.

## Instructions

1. **Analyze Project Structure**
   - Map the directory structure and explain the purpose of each top-level directory
   - Identify the primary language(s) and framework(s)
   - Note the build system and package manager in use
   - Document the monorepo structure if applicable

2. **Identify Setup Steps**
   - List all prerequisites (runtime versions, tools, services)
   - Document installation steps from clone to running
   - Note required environment variables and how to obtain values
   - Include database setup, seed data, and migration steps
   - Document any required external services (APIs, databases, queues)

3. **Document Architecture**
   - Explain the high-level architecture (monolith, microservices, serverless)
   - Describe the data flow for a typical request
   - Identify key design patterns used throughout the codebase
   - Document the module/package dependency structure
   - Note any architectural decision records (ADRs) if they exist

4. **List Important Files**
   - Entry points (main files, index files, app bootstrap)
   - Configuration files and what they control
   - Shared utilities and helpers
   - Type definitions and interfaces
   - Database models/schemas

5. **Explain Development Workflow**
   - Branch naming convention
   - Commit message format
   - PR/MR process and review expectations
   - CI/CD pipeline stages and what they check
   - How to run tests (unit, integration, e2e)
   - How to run linters and formatters

6. **Note Common Gotchas**
   - Known issues or workarounds
   - Environment-specific quirks
   - Common mistakes new developers make
   - Things that look wrong but are intentional
   - Performance pitfalls to avoid

7. **Create First Contribution Guide**
   - Suggest good first issues or areas to explore
   - Walk through making a small change end-to-end
   - Explain how to verify the change works
   - Describe the review and merge process

## Output Format

```
# Project Onboarding Guide

## Quick Start
1. Step-by-step from zero to running

## Project Overview
- Architecture summary and diagram
- Key technologies and versions

## Directory Structure
- Annotated tree of important directories

## Key Files
- List of important files with descriptions

## Development Workflow
- Day-to-day development process

## Common Gotchas
- Things to watch out for

## Your First Contribution
- Guided walkthrough

## Resources
- Links to further documentation
```

---
name: ci-cd
description: Design CI/CD pipelines for automated testing, building, and deployment
activation:
  keywords: ["ci", "cd", "ci/cd", "pipeline", "github actions", "gitlab ci", "continuous integration", "continuous deployment"]
  file_patterns: ["**/.github/workflows/*.yml", "**/.gitlab-ci.yml", "**/Jenkinsfile", "**/.circleci/config.yml"]
---

# CI/CD Pipeline Designer

## Purpose
Create or improve CI/CD pipelines that automate linting, testing, building, and deploying applications reliably.

## Instructions

1. **Assess the project**:
   - Identify the language, framework, and package manager
   - Find existing test commands and lint commands
   - Check for build steps (compile, bundle, etc.)
   - Identify deployment targets (cloud provider, container registry, CDN)
   - Note any existing CI/CD configuration

2. **Design pipeline stages**:
   - **Lint**: Run code formatters and linters (fail fast, cheap to run)
   - **Test**: Run unit tests, then integration tests. Report coverage.
   - **Build**: Compile, bundle, or create artifacts. Tag with commit SHA.
   - **Security**: Run dependency audit and SAST scans
   - **Deploy staging**: Auto-deploy to staging on merge to main
   - **Deploy production**: Manual approval gate or tag-triggered

3. **Choose triggers**:
   - Pull requests: lint + test (block merge on failure)
   - Push to main: full pipeline including staging deploy
   - Tags (vX.Y.Z): production deployment
   - Scheduled: dependency audits, full integration suites

4. **Optimize performance**:
   - Cache dependencies between runs (node_modules, pip cache, go mod cache)
   - Parallelize independent jobs (lint and test can run simultaneously)
   - Use matrix builds for multiple versions/platforms only when needed
   - Skip unnecessary jobs with path filters

5. **Handle secrets**:
   - Use the CI platform's secret management (never hardcode)
   - Limit secret access to deployment jobs only
   - Rotate secrets on a schedule
   - Use OIDC for cloud provider authentication where possible

6. **Add quality gates**:
   - Require passing CI before merge
   - Set minimum code coverage thresholds
   - Add status badges to README
   - Notify on failure (Slack, email, etc.)

## Output Format

Provide the complete pipeline configuration file for the target CI platform (default: GitHub Actions) with inline comments explaining each section. Include a summary of what each job does and when it runs.

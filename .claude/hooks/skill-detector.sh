#!/usr/bin/env bash
# skill-detector.sh - UserPromptSubmit hook for automatic skill detection
# Reads JSON from stdin (contains user prompt), outputs skill evaluation instructions to stdout.
# Pure bash, no dependencies, designed for speed.

# Read stdin (JSON with user prompt) - we don't parse it, we just ensure the hook runs
read -r -t 1 INPUT 2>/dev/null || true

cat <<'SKILL_PROTOCOL'
MANDATORY SKILL ACTIVATION PROTOCOL

Before responding, you MUST evaluate each available skill for relevance to this prompt.
For each skill, determine: Is this skill relevant to what the user is asking?

Available skills by category:

[CODE QUALITY] code-review, refactor, explain-code, debug, performance-optimization, code-smell-detector
[GIT & GITHUB] git-commit, review-pr, create-pr, changelog, git-workflow
[ARCHITECTURE] adr, impact-analysis, dependency-graph, scenario-compare, system-design, api-design
[TESTING] testing, test-writer, test-coverage, e2e-testing, mock-generator
[DOCUMENTATION] api-docs, readme-generator, technical-writing, code-comments, jsdoc-generator
[CONTENT] pdf-extract, document-extract, summarize, de-ai-ify
[DEVOPS] dockerfile, ci-cd, deployment, monitoring, cost-analysis, infrastructure
[DATA] database-schema, migration, data-modeling, sql-optimization, seed-generator
[SECURITY] security-audit, vulnerability-scan, secrets-management, dependency-check
[FRONTEND] component-design, accessibility-audit, responsive-design, css-review, storybook
[BACKEND] error-handling, logging-strategy, caching-strategy, middleware-design, rate-limiting
[META] skill-creator, find-related, onboarding-guide, technical-proposal, code-walkthrough
[WORKFLOW] implementation, workflow-manager
[UTILITIES] regex-helper, env-setup, error-message-writer, convert-format, cli-builder, mermaid-diagram
[DESIGN] figma

ACTIVATION RULES:
1. Identify ALL relevant skills (usually 1-3 per prompt)
2. Activate each relevant skill using the Skill tool: Skill(skill-name)
3. ONLY THEN proceed with implementation
4. If NO skills are relevant, proceed directly without comment
5. DO NOT mention skills that are not relevant
6. Activating a skill without using it is wasteful. Mentioning a skill without activating it is WORTHLESS.
SKILL_PROTOCOL

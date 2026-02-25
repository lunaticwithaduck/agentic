---
name: ci-cd
description: Design CI/CD pipelines for automated testing, building, and deployment
activation:
  keywords: ["ci", "cd", "ci/cd", "pipeline", "github actions", "gitlab ci", "continuous integration", "continuous deployment", "workflow yml"]
  file_patterns: ["**/.github/workflows/*.yml", "**/.gitlab-ci.yml", "**/Jenkinsfile", "**/.circleci/config.yml"]
---

# CI/CD Pipeline Designer

## Purpose
Create or improve CI/CD pipelines that automate linting, testing, building, and deploying applications reliably and fast.

## Pipeline Skeleton (GitHub Actions)

This is the baseline to adapt for any project. Fill in the TODO sections.

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  # ── Lint ─────────────────────────────────────────────────────────────────
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4       # TODO: swap for your runtime
        with:
          node-version: lts/*
          cache: npm                      # TODO: npm | pnpm | yarn

      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck            # TODO: remove if not TS

  # ── Test ─────────────────────────────────────────────────────────────────
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: lts/*
          cache: npm

      - run: npm ci
      - run: npm test -- --coverage

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: coverage-report
          path: coverage/

  # ── Build ─────────────────────────────────────────────────────────────────
  build:
    runs-on: ubuntu-latest
    needs: [lint, test]                   # only build if lint+test pass
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: lts/*
          cache: npm

      - run: npm ci
      - run: npm run build

      - uses: actions/upload-artifact@v4
        with:
          name: build-${{ github.sha }}
          path: dist/                     # TODO: your build output dir
          retention-days: 7

  # ── Deploy Staging ────────────────────────────────────────────────────────
  deploy-staging:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    environment: staging
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-${{ github.sha }}
          path: dist/

      # TODO: replace with your deploy command
      - run: echo "Deploy to staging here"

  # ── Deploy Production (tag-triggered) ────────────────────────────────────
  deploy-production:
    runs-on: ubuntu-latest
    needs: build
    if: startsWith(github.ref, 'refs/tags/v')
    environment: production              # requires manual approval in GitHub settings
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-${{ github.sha }}
          path: dist/

      - run: echo "Deploy to production here"
```

## Dependency Cache Keys by Runtime

Pick the right cache setup. Wrong cache keys cause silent misses.

| Runtime | Setup Action | Cache Key Pattern |
|---------|-------------|-------------------|
| Node.js / npm | `actions/setup-node` + `cache: npm` | `node_modules` keyed to `package-lock.json` |
| Node.js / pnpm | `pnpm/action-setup` + `actions/setup-node` + `cache: pnpm` | `~/.pnpm-store` keyed to `pnpm-lock.yaml` |
| Node.js / bun | `oven-sh/setup-bun` + manual cache | `~/.bun/install/cache` keyed to `bun.lockb` |
| Python / pip | `actions/setup-python` + `cache: pip` | `~/.cache/pip` keyed to `requirements.txt` |
| Python / uv | `astral-sh/setup-uv` | `.venv` keyed to `pyproject.toml` |
| Go | `actions/setup-go` + `cache: true` | `~/go/pkg/mod` keyed to `go.sum` |
| Rust | manual `actions/cache` | `~/.cargo/registry`, `target/` keyed to `Cargo.lock` |
| Java / Maven | `actions/setup-java` + `cache: maven` | `~/.m2/repository` keyed to `pom.xml` |

Manual cache example (Rust):
```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.cargo/registry
      ~/.cargo/git
      target/
    key: ${{ runner.os }}-cargo-${{ hashFiles('**/Cargo.lock') }}
    restore-keys: ${{ runner.os }}-cargo-
```

## Secrets: OIDC vs Hardcoded

**Hardcoded (avoid in production):**
```yaml
- uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}      # rotates on compromise
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    aws-region: us-east-1
```

**OIDC (preferred — no long-lived credentials):**
```yaml
permissions:
  id-token: write
  contents: read

- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::123456789:role/GitHubActionsRole
    aws-region: us-east-1
    # No secrets needed — GitHub mints a short-lived token
```

OIDC requires a one-time IAM role setup with a trust policy for `token.actions.githubusercontent.com`. Same pattern works for GCP (Workload Identity Federation) and Azure (federated credentials).

## Optimizing Speed

```yaml
# Parallelize independent jobs
jobs:
  lint:    { ... }
  test:    { ... }
  # lint and test start simultaneously; build waits for both
  build:
    needs: [lint, test]

# Skip jobs on irrelevant changes
on:
  push:
    paths-ignore:
      - '**.md'
      - 'docs/**'

# Matrix builds — only when you actually need multi-version testing
strategy:
  matrix:
    node: [18, 20, 22]
  fail-fast: false    # don't cancel all matrix jobs if one fails
```

## Quality Gates

```yaml
# Minimum coverage threshold (Jest)
- run: npm test -- --coverage --coverageThreshold='{"global":{"lines":80}}'

# Fail on audit findings at high/critical
- run: npm audit --audit-level=high

# Block merge: require this workflow to pass via branch protection rules
# GitHub Settings → Branches → Require status checks: CI / lint, CI / test
```

## Platform Adapters

<details>
<summary>GitLab CI equivalent</summary>

```yaml
stages: [lint, test, build, deploy]

lint:
  stage: lint
  image: node:lts
  cache:
    key: $CI_COMMIT_REF_SLUG
    paths: [node_modules/]
  script: [npm ci, npm run lint]

test:
  stage: test
  script: [npm ci, npm test]
  coverage: '/Lines\s*:\s*(\d+\.?\d*)%/'
  artifacts:
    reports:
      coverage_report:
        coverage_format: cobertura
        path: coverage/cobertura-coverage.xml
```
</details>

---
title: AgentFi X38 — GitHub Actions CI workflow
created: 2026-05-21
status: done
completed: 2026-05-21
---

## Goal
PR-gating CI: install → lint → typecheck → vitest → build → Playwright. Runs on push to main + every PR.

## Files
- `.github/workflows/ci.yml`

## Steps
- [x] Single-job sequential CI to mirror local dev (lint → unit → build → e2e)
- [x] pnpm + Playwright browser cache for speed
- [x] `concurrency` group cancels in-flight runs on new push
- [x] Playwright report uploaded as artifact `on: failure()`
- [x] Verified green on real CI (run #26228692842)

## Outcome

Completed on 2026-05-21. Workflow fires on every push/PR to `main`. The first three runs (in order):

1. **Run 1** — failed on lint (1 error in methodology.tsx + 5 warnings). I'd never run `pnpm lint` locally; CI caught it. Fix: wrapped `// v0.1` in JSX braces; removed 3 unused imports/params; removed 2 unused eslint-disable directives.
2. **Run 2** — failed on the constitution unit test. CI had `PIN_CONSTITUTION_SNIPPET` set at the job level (intended for Playwright determinism), which leaked into vitest and short-circuited the constitution module before its mocked fetch could run. Fix: dropped the job-level env var; rely on `playwright.config.ts.webServer.env` which already sets it just for the Playwright child process.
3. **Run 3** — **success**. Green build at https://github.com/vpjonny/agentfi-terminal/actions/runs/26228692842

The lessons (both encoded in the workflow file as comments):
- Always run `pnpm lint` locally as part of the dev loop, not just `test` + `build`
- Env vars set at the GitHub Actions job level apply to EVERY step, including unit tests. Pinning needs to be step-scoped or webServer-scoped, not job-scoped.

**Skill candidate evaluation:**
- Technologies/frameworks touched: GitHub Actions workflow syntax, pnpm/setup-node/cache action combos, Playwright in CI
- Domain-specific knowledge: (a) `concurrency: cancel-in-progress: true` keyed by `github.workflow + github.ref` is the right pattern to kill superseded runs — without it you waste runner minutes on stale commits; (b) Cache Playwright browsers separately from pnpm because they're huge (~300MB) and `pnpm install` doesn't manage them; (c) job-level `env:` blocks leak into ALL steps — pin determinism vars step-scoped or via the tool's own config (e.g. playwright.config.ts.webServer.env); (d) `upload-artifact@v4` `on: failure()` is the standard pattern for surfacing CI diagnostics — always upload the test report, not just the screenshots; (e) `--with-deps` on `playwright install` only works when the binary cache misses; on cache hit, use `playwright install-deps chromium` to install system libs without re-downloading the browser.
- Verdict: GENERATE
- Reason: GitHub Actions env-scoping gotcha + Playwright CI caching pattern are non-obvious and would help future projects.

## Completion
Run `/complete workflows/tasks/2026-05-21-agentfi-x38-github-actions-ci.md`.

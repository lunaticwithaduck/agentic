---
domain: ci-cd
source_task: 2026-05-21-agentfi-x38-github-actions-ci.md
date: 2026-05-21
keywords: ["github-actions", "ci", "playwright-cache", "concurrency", "env-scoping", "pnpm-cache"]
---

## Extracted Knowledge

### Job-level `env:` leaks into every step — gotcha that bit us

In a GitHub Actions workflow, `env:` set at the job level applies to **every step** in that job, including unit tests. If a determinism-pin var (`PIN_CONSTITUTION_SNIPPET`, `MOCK_DATE`, etc.) is set there, vitest/jest tests that intentionally exercise the un-pinned path will fail.

```yaml
# WRONG — pin leaks into unit tests
jobs:
  test:
    env:
      PIN_CONSTITUTION_SNIPPET: "..."  # applies to lint, unit, build, e2e
    steps:
      - run: pnpm test --run   # constitution unit test sees the pin, breaks
      - run: pnpm exec playwright test
```

Fix: scope pins to the tool's own config (e.g. `playwright.config.ts.webServer.env`) OR to a specific step:

```yaml
# RIGHT — pin only when Playwright spawns the webServer
steps:
  - run: pnpm test --run
  - run: pnpm exec playwright test
    env:
      PIN_CONSTITUTION_SNIPPET: "..."
```

Or better — bake the pin into `playwright.config.ts.webServer.env` once, then nothing needs to be in the workflow.

### Cancel-in-progress concurrency

Every push to a branch wastes runner minutes if a previous push is still running. The fix:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Now a new push cancels the in-flight run on the same ref. Critical for active development branches; saves quota and gives faster feedback on the latest commit.

### Playwright browser cache — separate from pnpm cache

`pnpm install` doesn't touch `~/.cache/ms-playwright`. The browsers (~300MB) must be cached separately:

```yaml
- name: Cache Playwright browsers
  uses: actions/cache@v4
  id: playwright-cache
  with:
    path: ~/.cache/ms-playwright
    key: ${{ runner.os }}-playwright-${{ hashFiles('pnpm-lock.yaml') }}

- name: Install Playwright browsers
  if: steps.playwright-cache.outputs.cache-hit != 'true'
  run: pnpm exec playwright install --with-deps chromium

- name: Install Playwright system deps (cached browsers)
  if: steps.playwright-cache.outputs.cache-hit == 'true'
  run: pnpm exec playwright install-deps chromium
```

The split matters because:
- **Cache miss** → `playwright install --with-deps chromium` downloads the browser AND installs system libs in one go
- **Cache hit** → the browser is already on disk, but the runner image is ephemeral and system libs aren't preserved. `playwright install-deps chromium` installs the libs without re-downloading the binary.

Skip the second step and Playwright fails to launch chromium on cached runs with a `Host system is missing dependencies` error.

### `upload-artifact@v4` for failure diagnostics

Default Playwright produces `playwright-report/` (HTML reporter) and `test-results/` (per-test screenshots + traces + videos). On a CI failure, you can't ssh into the runner — these artifacts are how you debug:

```yaml
- name: Upload Playwright report on failure
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: playwright-report
    path: |
      playwright-report/
      test-results/
    retention-days: 7
```

`retention-days: 7` keeps storage costs sane. Don't use `if: always()` — green runs don't need artifacts and they cost real money.

### Run lint locally as part of dev loop

If your CI runs `pnpm lint` and your local dev loop only runs `pnpm test` + `pnpm build`, you'll learn about every lint error from a failed CI run. Either:

- Add `pnpm lint` to your local pre-commit
- Add a `pre-push` git hook
- Make CI failures fast: put `lint` as the FIRST step so it fails in 30s, not after the 15-minute Playwright run

The chosen ordering for agentfi-terminal: `Install → Cache browsers → Install deps → Lint → Unit → Build → E2E`. Lint is third (after the unavoidable setup steps), so a lint failure shows up in ~90s.

### `pnpm install --frozen-lockfile` in CI

Always. Without `--frozen-lockfile`, pnpm can mutate the lockfile mid-CI based on resolution differences, hiding lock drift. With it, any lockfile/package.json mismatch fails the install — which is exactly what you want CI to do.

### `node-version: 22` for Next 16

Next.js 16.x targets Node 22 LTS on Vercel. Pinning the CI to 22 means CI catches API-shape differences before Vercel does. The `.nvmrc` should match.

## Proposed Skill Content

A future `.claude/skills/ci-cd.md` (or merge into the existing ci-cd skill if present) covering:

- Job-level vs step-level env scoping (the gotcha)
- `concurrency.cancel-in-progress` for branch races
- Playwright browser cache pattern (separate from pnpm, two-branch install)
- `upload-artifact@v4` with `if: failure()` + retention
- Local dev loop must include lint OR CI puts lint as first non-setup step
- `--frozen-lockfile` always in CI
- Node version pinning (CI + `.nvmrc` must match the target deploy platform)

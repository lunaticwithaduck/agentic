#!/usr/bin/env bash
# playwright-setup.sh - Interactive Playwright E2E test setup
# Sets up Playwright in any JS/TS project with sensible defaults.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "================================"
echo "  Playwright E2E Setup"
echo "================================"
echo ""

# ── Detect package manager ───────────────────────────────────────────────────

detect_package_manager() {
  if [ -f "$PROJECT_DIR/bun.lockb" ] || [ -f "$PROJECT_DIR/bun.lock" ]; then
    echo "bun"
  elif [ -f "$PROJECT_DIR/pnpm-lock.yaml" ]; then
    echo "pnpm"
  elif [ -f "$PROJECT_DIR/yarn.lock" ]; then
    echo "yarn"
  else
    echo "npm"
  fi
}

PKG_MANAGER=$(detect_package_manager)
echo "Detected package manager: $PKG_MANAGER"
echo ""

# Allow override
read -rp "Use $PKG_MANAGER? [Y/n] " CONFIRM_PM
if [[ "$CONFIRM_PM" =~ ^[Nn]$ ]]; then
  echo ""
  echo "Select package manager:"
  echo "  1) npm"
  echo "  2) pnpm"
  echo "  3) yarn"
  echo "  4) bun"
  read -rp "Your choice: " PM_CHOICE
  case "$PM_CHOICE" in
    1) PKG_MANAGER="npm" ;;
    2) PKG_MANAGER="pnpm" ;;
    3) PKG_MANAGER="yarn" ;;
    4) PKG_MANAGER="bun" ;;
    *) echo "Invalid choice, using npm."; PKG_MANAGER="npm" ;;
  esac
fi

# ── Resolve install/exec commands ────────────────────────────────────────────

case "$PKG_MANAGER" in
  bun)
    INSTALL_CMD="bun add -d @playwright/test"
    EXEC_CMD="bunx"
    SCRIPT_RUN="bun run"
    ;;
  pnpm)
    INSTALL_CMD="pnpm add -D @playwright/test"
    EXEC_CMD="pnpm dlx"
    SCRIPT_RUN="pnpm"
    ;;
  yarn)
    INSTALL_CMD="yarn add -D @playwright/test"
    EXEC_CMD="yarn dlx"
    SCRIPT_RUN="yarn"
    ;;
  *)
    INSTALL_CMD="npm install -D @playwright/test"
    EXEC_CMD="npx"
    SCRIPT_RUN="npm run"
    ;;
esac

# ── Browser selection ─────────────────────────────────────────────────────────

echo ""
echo "Which browsers do you want to install?"
echo "  1) Chromium only  (fastest, smallest)"
echo "  2) Chromium + Firefox"
echo "  3) All (Chromium + Firefox + WebKit)"
echo ""
read -rp "Your choice [1]: " BROWSER_CHOICE
BROWSER_CHOICE="${BROWSER_CHOICE:-1}"

case "$BROWSER_CHOICE" in
  2) BROWSERS="chromium firefox" ;;
  3) BROWSERS="chromium firefox webkit" ;;
  *) BROWSERS="chromium" ;;
esac

# ── Config format ─────────────────────────────────────────────────────────────

echo ""
echo "Config format?"
echo "  1) TypeScript - playwright.config.ts  (recommended)"
echo "  2) JavaScript - playwright.config.js"
echo ""
read -rp "Your choice [1]: " CONFIG_FORMAT
CONFIG_FORMAT="${CONFIG_FORMAT:-1}"

if [[ "$CONFIG_FORMAT" == "2" ]]; then
  CONFIG_FILE="playwright.config.js"
  LANG="js"
else
  CONFIG_FILE="playwright.config.ts"
  LANG="ts"
fi

# ── Tests directory ───────────────────────────────────────────────────────────

echo ""
read -rp "E2E tests directory [e2e]: " TESTS_DIR
TESTS_DIR="${TESTS_DIR:-e2e}"

# ── Base URL ──────────────────────────────────────────────────────────────────

echo ""
read -rp "App base URL for tests [http://localhost:3000]: " BASE_URL
BASE_URL="${BASE_URL:-http://localhost:3000}"

# ── CI workflow ───────────────────────────────────────────────────────────────

echo ""
read -rp "Generate GitHub Actions workflow? [Y/n]: " WANT_CI
WANT_CI="${WANT_CI:-Y}"

# ── Confirm ───────────────────────────────────────────────────────────────────

echo ""
echo "──────────────────────────────────────"
echo "  Summary"
echo "──────────────────────────────────────"
echo "  Package manager : $PKG_MANAGER"
echo "  Browsers        : $BROWSERS"
echo "  Config file     : $CONFIG_FILE"
echo "  Tests directory : $TESTS_DIR/"
echo "  Base URL        : $BASE_URL"
if [[ "$WANT_CI" =~ ^[Yy]$ ]]; then
  echo "  GitHub Actions  : yes"
fi
echo ""
read -rp "Proceed? [Y/n] " PROCEED
PROCEED="${PROCEED:-Y}"
if [[ "$PROCEED" =~ ^[Nn]$ ]]; then
  echo "Aborted."
  exit 0
fi

# ── Install @playwright/test ──────────────────────────────────────────────────

echo ""
echo "[1/4] Installing @playwright/test..."
cd "$PROJECT_DIR"
eval "$INSTALL_CMD"
echo "  Done."

# ── Install browsers ──────────────────────────────────────────────────────────

echo ""
echo "[2/4] Installing browsers: $BROWSERS..."
for browser in $BROWSERS; do
  echo "  Installing $browser..."
  "$EXEC_CMD" playwright install "$browser" --with-deps
done
echo "  Done."

# ── Write config ──────────────────────────────────────────────────────────────

echo ""
echo "[3/4] Writing $CONFIG_FILE..."

# Build projects array from selected browsers
PROJECTS_BLOCK=""
for browser in $BROWSERS; do
  case "$browser" in
    chromium)
      PROJECTS_BLOCK="${PROJECTS_BLOCK}
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },"
      ;;
    firefox)
      PROJECTS_BLOCK="${PROJECTS_BLOCK}
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },"
      ;;
    webkit)
      PROJECTS_BLOCK="${PROJECTS_BLOCK}
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },"
      ;;
  esac
done

if [[ "$LANG" == "ts" ]]; then
  cat > "$PROJECT_DIR/$CONFIG_FILE" <<CONFIG
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './$TESTS_DIR',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    process.env.CI ? ['github'] : ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL ?? '$BASE_URL',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [$PROJECTS_BLOCK
  ],
});
CONFIG
else
  cat > "$PROJECT_DIR/$CONFIG_FILE" <<CONFIG
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './$TESTS_DIR',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    process.env.CI ? ['github'] : ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL ?? '$BASE_URL',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [$PROJECTS_BLOCK
  ],
});
CONFIG
fi

echo "  Written: $CONFIG_FILE"

# ── Scaffold example test ─────────────────────────────────────────────────────

mkdir -p "$PROJECT_DIR/$TESTS_DIR"

EXAMPLE_TEST="$PROJECT_DIR/$TESTS_DIR/example.spec.$LANG"
if [ ! -f "$EXAMPLE_TEST" ]; then
  cat > "$EXAMPLE_TEST" <<SPEC
import { test, expect } from '@playwright/test';

test('homepage loads', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/.+/);
});

test('basic navigation', async ({ page }) => {
  await page.goto('/');
  // TODO: replace with real assertions for your app
  await expect(page.locator('body')).toBeVisible();
});
SPEC
  echo "  Written: $TESTS_DIR/example.spec.$LANG"
fi

# ── Add package.json scripts ──────────────────────────────────────────────────

if [ -f "$PROJECT_DIR/package.json" ] && command -v node &> /dev/null; then
  echo ""
  read -rp "Add e2e scripts to package.json? [Y/n] " ADD_SCRIPTS
  ADD_SCRIPTS="${ADD_SCRIPTS:-Y}"
  if [[ "$ADD_SCRIPTS" =~ ^[Yy]$ ]]; then
    node - <<NODE
const fs = require('fs');
const path = '$PROJECT_DIR/package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));
pkg.scripts = pkg.scripts || {};
if (!pkg.scripts['test:e2e'])   pkg.scripts['test:e2e']   = 'playwright test';
if (!pkg.scripts['test:e2e:ui']) pkg.scripts['test:e2e:ui'] = 'playwright test --ui';
if (!pkg.scripts['test:e2e:debug']) pkg.scripts['test:e2e:debug'] = 'playwright test --debug';
fs.writeFileSync(path, JSON.stringify(pkg, null, 2) + '\n');
console.log('  Added: test:e2e, test:e2e:ui, test:e2e:debug');
NODE
  fi
fi

# ── GitHub Actions workflow ───────────────────────────────────────────────────

if [[ "$WANT_CI" =~ ^[Yy]$ ]]; then
  WORKFLOW_DIR="$PROJECT_DIR/.github/workflows"
  WORKFLOW_FILE="$WORKFLOW_DIR/playwright.yml"
  mkdir -p "$WORKFLOW_DIR"

  if [ -f "$WORKFLOW_FILE" ]; then
    echo ""
    echo "  $WORKFLOW_FILE already exists, skipping."
  else
    # Build browser install list for CI
    BROWSER_INSTALL_LIST=$(echo "$BROWSERS" | tr ' ' '\n' | sed 's/^/          - /' | tr '\n' ' ')

    cat > "$WORKFLOW_FILE" <<YAML
name: Playwright Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    timeout-minutes: 60
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: lts/*
          cache: '$PKG_MANAGER'

      - name: Install dependencies
        run: $INSTALL_CMD_CI

      - name: Install Playwright browsers
        run: $EXEC_CMD playwright install --with-deps $BROWSERS

      - name: Run Playwright tests
        run: $EXEC_CMD playwright test
        env:
          BASE_URL: \${{ secrets.BASE_URL || '$BASE_URL' }}

      - name: Upload test report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
          retention-days: 14
YAML

    echo "  Written: .github/workflows/playwright.yml"
  fi
fi

# ── .gitignore entries ────────────────────────────────────────────────────────

GITIGNORE="$PROJECT_DIR/.gitignore"
PW_ENTRIES="/playwright-report/
/test-results/
/blob-report/"

if [ -f "$GITIGNORE" ]; then
  if ! grep -q "playwright-report" "$GITIGNORE"; then
    echo "" >> "$GITIGNORE"
    echo "# Playwright" >> "$GITIGNORE"
    echo "$PW_ENTRIES" >> "$GITIGNORE"
    echo "  Updated: .gitignore"
  fi
else
  echo "# Playwright" > "$GITIGNORE"
  echo "$PW_ENTRIES" >> "$GITIGNORE"
  echo "  Created: .gitignore"
fi

# ── Done ──────────────────────────────────────────────────────────────────────

echo ""
echo "[4/4] Done."
echo ""
echo "================================"
echo "  Playwright setup complete!"
echo "================================"
echo ""
echo "Run your tests:"
echo "  $EXEC_CMD playwright test              # headless"
echo "  $EXEC_CMD playwright test --ui         # interactive UI mode"
echo "  $EXEC_CMD playwright test --debug      # step debugger"
echo "  $EXEC_CMD playwright show-report       # view last HTML report"
echo ""
echo "Edit your tests in: $TESTS_DIR/"
echo ""

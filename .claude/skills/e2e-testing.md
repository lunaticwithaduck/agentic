---
name: e2e-testing
description: End-to-end test guidance using Playwright — stable selectors, async handling, and CI setup
activation:
  keywords: ["e2e test", "end to end", "end-to-end", "playwright", "browser test", "integration test", "page object", "test:e2e"]
  file_patterns: ["**/e2e/**", "**/playwright/**", "playwright.config.*", "**/*.e2e.*", "**/*.spec.ts", "**/*.spec.js"]
---

# E2E Testing (Playwright)

## Setup

Use the project's Playwright setup script to bootstrap from scratch:

```bash
bash .claude/scripts/playwright-setup.sh
```

This installs `@playwright/test`, writes `playwright.config.ts`, scaffolds an example test,
adds `test:e2e` scripts to `package.json`, and optionally creates a GitHub Actions workflow.

## Running Tests

```bash
npx playwright test              # headless, all browsers
npx playwright test --ui         # interactive UI mode (great for writing tests)
npx playwright test --debug      # pause on each step
npx playwright test -g "login"   # filter by test name
npx playwright show-report       # open last HTML report
```

## Config Reference (`playwright.config.ts`)

```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
  ],
});
```

## Selector Strategy

Use selectors in this order (most to least stable):

```ts
// 1. data-testid — immune to CSS/copy changes
page.getByTestId('submit-button')

// 2. ARIA role + accessible name
page.getByRole('button', { name: 'Submit' })
page.getByRole('link', { name: /dashboard/i })

// 3. Label / placeholder
page.getByLabel('Email address')
page.getByPlaceholder('you@example.com')

// 4. Visible text (acceptable for headings/nav)
page.getByText('Welcome back')

// Avoid: CSS classes, nth-child selectors, XPath
```

## Test Structure

```ts
import { test, expect } from '@playwright/test';

test.describe('User Flow: checkout', () => {
  test.beforeEach(async ({ page }) => {
    // Set up via API, not UI — much faster and more reliable
    await page.request.post('/api/test/seed', { data: { cart: ['item-1'] } });
    await page.goto('/cart');
  });

  test('completes purchase with valid card', async ({ page }) => {
    await page.getByRole('button', { name: 'Checkout' }).click();
    await page.getByLabel('Card number').fill('4242 4242 4242 4242');
    await page.getByRole('button', { name: 'Pay now' }).click();

    await expect(page.getByRole('heading', { name: 'Order confirmed' })).toBeVisible();
    await expect(page).toHaveURL(/\/order\/\w+/);
  });

  test('shows error for declined card', async ({ page }) => {
    await page.goto('/checkout');
    await page.getByLabel('Card number').fill('4000 0000 0000 0002');
    await page.getByRole('button', { name: 'Pay now' }).click();

    await expect(page.getByRole('alert')).toContainText('card was declined');
  });

  test.afterEach(async ({ request }) => {
    await request.post('/api/test/cleanup');
  });
});
```

## Page Object Model

Use POM for flows you test frequently. Keep one class per page/component:

```ts
// e2e/pages/login.page.ts
import { type Page, type Locator } from '@playwright/test';

export class LoginPage {
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;

  constructor(private readonly page: Page) {
    this.emailInput    = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.submitButton  = page.getByRole('button', { name: 'Sign in' });
    this.errorMessage  = page.getByRole('alert');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }
}
```

```ts
// e2e/auth.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';

test('valid credentials redirect to dashboard', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.login('user@example.com', 'password123');
  await expect(page).toHaveURL('/dashboard');
});
```

## API-Assisted Setup (Fixtures)

```ts
// e2e/fixtures.ts
import { test as base, expect } from '@playwright/test';

type TestUser = { email: string; token: string };

export const test = base.extend<{ user: TestUser }>({
  user: async ({ request }, use) => {
    // Create test user via API
    const res = await request.post('/api/test/users', {
      data: { email: `test-${Date.now()}@example.com`, password: 'secret' },
    });
    const user = await res.json();

    await use(user);

    // Cleanup after test
    await request.delete(`/api/test/users/${user.id}`);
  },
});

export { expect };
```

## Anti-Patterns to Avoid

| Bad | Good |
|-----|------|
| `await page.waitForTimeout(2000)` | `await expect(element).toBeVisible()` |
| `page.locator('.btn-primary')` | `page.getByRole('button', { name: '…' })` |
| Setting up data via UI clicks | Set up via API in `beforeEach` |
| Shared state between tests | Each test owns its data end-to-end |
| `test.only` committed | Use `--grep` flag instead |

## CI Configuration (GitHub Actions)

The setup script generates `.github/workflows/playwright.yml`. Key points:
- `forbidOnly: true` in CI prevents accidentally committed `test.only`
- `retries: 2` handles transient network/render flakiness
- `workers: 1` in CI avoids resource contention on shared runners
- Artifacts: HTML report uploaded on every run for debugging failures

## Debugging Failures

```bash
# Re-run only failed tests with trace
npx playwright test --last-failed --trace on

# Open the trace viewer for a failed test
npx playwright show-trace test-results/*/trace.zip

# Run a single test headed (watch it in the browser)
npx playwright test auth.spec.ts --headed
```

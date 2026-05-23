---
domain: vitest
source_task: 2026-05-18-agentfi-t3-registry-and-metrics.md
date: 2026-05-18
keywords: ["vitest", "vitest-config", "test", "alias", "nextjs-testing"]
---

## Extracted Knowledge

### Vitest needs its own `@/` alias mapping
Even when `tsconfig.json` declares `"paths": { "@/*": ["./*"] }`, vitest does NOT pick it up automatically. The result is `Cannot find module '@/lib/x'` at test runtime with TS happily passing typecheck.

Fix: declare the alias in `vitest.config.ts` separately:

```ts
import { defineConfig } from "vitest/config";
import path from "node:path";

export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./"),
    },
  },
  test: {
    include: ["lib/**/*.test.ts"],
    environment: "node",
  },
});
```

The alias has to be in vitest's `resolve.alias`, not just tsconfig. They are two independent module resolvers.

### `environment: 'node'` vs `'jsdom'` matters for cold start
Default is `'node'`. For pure-TS lib tests (no React, no DOM), keep `'node'` — `jsdom` adds ~300ms startup. Only switch to `'jsdom'` for tests that touch `window`/`document` or render React components.

If you need a mix (lib tests fast, component tests in jsdom), use `environment: 'node'` globally and `// @vitest-environment jsdom` per-file pragma on component tests.

### Test discovery globs
Default `include` is `**/*.{test,spec}.?(c|m)[jt]s?(x)`. For a Next.js project this matches files in `app/` and `node_modules/` (the latter is excluded by default).

Tighten the glob to `lib/**/*.test.ts` if all tests live under `lib/` — this:
- Avoids accidentally executing tests dropped into `app/` (which usually shouldn't be tests)
- Speeds up cold test discovery
- Keeps it obvious where to drop new tests

### Test colocation: `lib/__tests__/` vs `*.test.ts` next to source
Either works with vitest. `__tests__/` directory:
- Easier to skim "what's tested" by listing one folder
- Cleaner imports (`../module` not `./module`)
- Mirrors Jest convention many devs already know

`*.test.ts` next to source:
- Co-discovered in IDE alongside the file under test
- Less indirection when refactoring

This project went with `lib/__tests__/` for skim-ability.

### Package scripts
```json
"scripts": {
  "test": "vitest run",       // one-shot, exits — for CI / pre-commit
  "test:watch": "vitest"      // watch mode — default vitest behavior
}
```
`vitest run` is non-obvious — without `run` the command stays in watch mode and never exits, which breaks CI.

### TypeScript globals
If you use `describe`/`it`/`expect` as globals (no import), add `"types": ["vitest/globals"]` to `tsconfig.json` AND `globals: true` to `vitest.config.ts`. Otherwise:
- Either use explicit imports: `import { describe, it, expect } from "vitest"` (this project's choice — keeps file headers explicit)
- Or accept the global-config dance

Explicit imports add 1 line per file but eliminate "where does `it` come from" confusion for new contributors.

## Proposed Skill Content

A future `.claude/skills/vitest.md` would cover:

**Section: Setup in a Next.js project**
- Install: `pnpm add -D vitest @vitest/coverage-v8`
- Always create `vitest.config.ts` even for minimal setups — duplicating tsconfig aliases is the #1 first-bug
- Default to `environment: 'node'` unless you actually render React

**Section: Path aliases**
- Vitest does NOT inherit tsconfig `paths` — must declare in `resolve.alias`
- Common gotcha: typecheck passes, runtime fails with module-not-found

**Section: Scripts**
- `vitest run` for CI / one-shot
- `vitest` (no `run`) for watch mode
- `vitest run --coverage` for coverage reports (requires `@vitest/coverage-v8`)

**Section: Test discovery**
- Tighten `include` to a specific dir to avoid surprises in monorepos / app-router projects
- Mixed environments via `// @vitest-environment` pragma per file

(No Failure Modes section yet — first vitest `.sc`.)

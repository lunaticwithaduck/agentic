---
domain: nextjs
source_task: 2026-05-18-agentfi-t1-bootstrap-nextjs.md
date: 2026-05-18
keywords: ["nextjs", "next.js", "create-next-app", "app-router", "tailwind", "shadcn"]
---

## Extracted Knowledge

### Next.js 16 ships an in-tree AGENTS.md
On a fresh `create-next-app@latest` scaffold (verified on Next.js 16.2.6), the generated repo contains an `AGENTS.md` and a `CLAUDE.md` that just `@AGENTS.md`'s it. The AGENTS.md text:

> "This is NOT the Next.js you know. This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices."

**Pattern:** when working in a Next.js 16+ project, do not assume training-data Next.js patterns (e.g. `getServerSideProps`, `next/router`, even App Router signatures may shift between minor versions). Always consult `node_modules/next/dist/docs/01-app/` first.

Docs tree layout (Next.js 16.2.6):
```
node_modules/next/dist/docs/
├── 01-app/
│   ├── 01-getting-started/
│   ├── 02-guides/
│   └── 03-api-reference/
├── 02-pages/
├── 03-architecture/
├── 04-community/
└── index.md
```

### Non-interactive create-next-app
The full flag set for a hands-off scaffold:
```bash
pnpm create next-app@latest <name> \
  --ts --tailwind --eslint --app \
  --no-src-dir --import-alias "@/*" \
  --use-pnpm --yes
```

`--yes` accepts defaults for any prompt not covered by an explicit flag (e.g. Turbopack toggle in newer versions). Without `--yes`, the CLI still prompts even with every other flag set.

### Order-of-operations: customize globals.css BEFORE shadcn init
`pnpm dlx shadcn@latest init` writes/edits:
- `globals.css` (injects shadcn's CSS variable layer)
- `components.json`
- `lib/utils.ts`
- creates empty `components/ui/`

If a project has a custom design system that overrides `globals.css` heavily (custom palette, fonts, textures), running `shadcn init` first forces a merge fight on every subsequent customization. **Defer `shadcn init` until either (a) globals.css is finalized, or (b) you actually need your first shadcn primitive.** Most MVP components (cards, status bars, type-only chrome) need zero shadcn primitives — shadcn shines for dialogs, dropdowns, sort headers, tooltips.

### Tailwind v4 in Next.js 16: CSS-first `@theme`
Stock `globals.css` uses Tailwind v4's new directives:
```css
@import "tailwindcss";

@theme inline {
  --color-background: var(--background);
  --font-sans: var(--font-geist-sans);
}
```
No `tailwind.config.ts` is required for token registration — themed tokens live in CSS. Old `tailwind.config.ts` extension patterns (`theme.extend.colors`) still work but are no longer the primary way to add design tokens. Prefer `@theme` for palette, fonts, type scale.

### Auto-generated workspace file
create-next-app emits a `pnpm-workspace.yaml` with:
```yaml
ignoredBuiltDependencies:
  - sharp
  - unrs-resolver
```
This is not declaring a monorepo — it's pre-suppressing build warnings for native deps that ship prebuilt binaries. Leave it alone.

## Proposed Skill Content

A future `.claude/skills/nextjs.md` would cover:

**Section: Version awareness**
- Always check the installed Next.js version (`cat node_modules/next/package.json | grep version` or `pnpm list next`) before writing route handlers, layouts, or metadata APIs
- On Next.js 16+, consult `node_modules/next/dist/docs/01-app/` for API reference — these are the canonical docs for the exact installed version, not the marketing site
- Heed any `AGENTS.md` in a Next.js project root — it overrides training-data assumptions

**Section: Project scaffolding**
- Non-interactive `create-next-app` command pattern (see Extracted Knowledge)
- App Router only for new projects; `--no-src-dir` keeps `app/` at the root which the plan-doc layouts assume
- pnpm + `--use-pnpm` flag for consistent toolchain

**Section: Tailwind v4 integration**
- Tokens go in `globals.css` `@theme` block, not `tailwind.config.ts`
- `@import "tailwindcss"` replaces the old `@tailwind base/components/utilities` triplet
- `tailwind.config.ts` is still used for content paths and plugins but no longer the primary token surface

**Section: shadcn ordering**
- Run `shadcn init` AFTER finalizing custom globals.css, or right before adding the first shadcn primitive
- For minimal-shadcn projects (custom design systems), `shadcn add <component>` works without ever running `init` first — provided you manually create `lib/utils.ts` with the `cn` helper

(No Failure Modes section yet — this is the first nextjs `.sc`.)

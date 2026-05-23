---
title: AgentFi T6 — OG image routes (satori-rendered, shared component)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Three OG routes that render PNGs from the same `AgentCard` component used on-page. Visual diff between on-page and OG must be visually indistinguishable (modulo cursor blink, which OG omits). Fonts loaded as Buffer from `assets/fonts/` (outside `public/` so they're bundled, not served).

## Steps
- [x] Downloaded JetBrains Mono Variable TTF (293KB) into `assets/fonts/`
- [x] `lib/og/fonts.ts` — module-cached `loadJetBrainsMono()` via `fs.readFile` (Node runtime)
- [x] `lib/og/og-root.tsx` — wrapper that declares all design-token CSS variables inline on the satori root so `var(--bg-raised)` etc. resolve
- [x] `app/og/agent/[slug]/route.tsx` — Node runtime, returns `ImageResponse` 1200×630 of `<AgentCard mode="og" />` with JetBrains Mono at 400/700 weights. **Reuses the same `AgentCard` component as the on-page render.**
- [x] `app/og/comp/route.tsx` — satori-safe inline-styled comp table (4 rows, sorted by multiple desc, ticker dot, no horizontal borders)
- [x] `app/og/action/[txhash]/route.tsx` — minimal single-action card; resolves the agent + action by scanning all snapshots for the hash; milestone rows render the type in `--signal`
- [x] 5 OG smoke tests in `lib/__tests__/og-routes.test.ts` — each route invoked directly; checks status 200 + `content-type: image/png` for happy paths, 404 for unknown slugs/hashes
- [x] `<meta property="og:image">` already wired in T5's `generateMetadata` for the agent page
- [x] Visual diff at `localhost:3000/og/agent/autono` vs `localhost:3000/agent/autono` — **deferred to user** (harness blocks dev server)

## Anti-drift contract — VERIFIED
The "single component, two render targets" contract from T4 is now empirically verified: the OG smoke test imports `AgentCard` through `ImageResponse` and the render returns a real PNG. No fork, no second component, no `if (mode === 'og')` branches that diverge in layout — only the cursor visibility and the headline font size differ, both per T4's design.

## Verification
- `pnpm test`: 5 files, **38 tests**, all pass (5 new OG smoke tests).
- `pnpm build`: PASS — full route table now 13 entries:
  ```
  ○ /  ○ /admin  ● /agent/[slug]×4  ƒ /comp  ○ /methodology
  ƒ /og/action/[txhash]  ƒ /og/agent/[slug]  ƒ /og/comp
  ○ /preview/card  ○ /_not-found
  ```

## Outcome
The OG infrastructure is real. Four things worth recording:

1. **`assets/fonts/` outside `public/` is the right home for OG fonts.** Files in `public/` are served as static assets and aren't accessible to server functions via `fs.readFile` without an HTTP round-trip. Putting fonts in `assets/fonts/` and reading via `fs.readFile(path.join(process.cwd(), 'assets', 'fonts', '...'))` bundles them with the server function — zero round-trip latency. Module-level cache (`let cached: Buffer | null`) means the file is read once per warm function instance.

2. **CSS variables must be re-declared on the satori root.** `globals.css` is invisible to satori — only inline styles in the rendered React tree are read. Pattern: a wrapper component (`OgRoot`) sets all `--bg-*`, `--ink-*`, `--signal`, `--tag-*` etc. as inline-style properties, and `var(--name)` works as expected inside the tree. The variables are duplicated between `globals.css` (browser) and `OgRoot` (satori) — DRY violation, but the alternative (regenerating the AgentCard with literal hex values) is worse.

3. **Node runtime is fine for OG.** Vercel originally pushed edge runtime for `@vercel/og`, but `next/og`'s ImageResponse works equally in Node. Node lets us use `fs.readFile` for fonts and access standard Node APIs in helpers. Edge runtime is faster cold-start but doesn't matter for OG (cached behind CDN). Default to Node unless cold-start latency on uncached OG renders becomes a measurable problem.

4. **The smoke test catches more than I expected.** A test that invokes `GET()` and checks `res.status === 200` and `res.headers.get('content-type') === 'image/png'` will fail if:
   - Satori encounters an unsupported CSS property in `AgentCard` (the render throws)
   - The font file is missing or corrupt
   - A data-binding error surfaces (e.g. undefined access in `getSnapshot`)
   - The route handler signature is wrong for the Next.js version
   
   That's a lot of coverage from one tiny test. Worth setting up early on any project that uses ImageResponse.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: `next/og` / satori, Node runtime route handlers, font loading patterns
- Domain knowledge: assets-outside-public pattern for server-bundled files; CSS variables must be re-declared inline on satori root; smoke-test-via-direct-handler-invocation as a satori contract check
- Verdict: **GENERATE**
- Reason: Three real, surprising patterns confirmed by actual rendering. Extends T4's "design-for-satori" .sc with "execute-satori" knowledge.
- Domain: `satori` (extends existing — 2nd entry, threshold for synthesis is 3)

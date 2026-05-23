---
title: AgentFi T5 — Page routes scaffold (/, /agent/[slug], /comp, /methodology, /admin)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Wire the five public/internal routes with mocked data so the navigation and shape of the product are real. No live indexer, no DB — every value comes from a `lib/mock-data.ts` module so swapping it for real reads later is one import change.

## Steps
- [x] `lib/agents.ts` extended with ETHY / BANKR / AETHER placeholder entries (addresses flagged in-comment as PLACEHOLDER)
- [x] `lib/mock-data.ts` — `SNAPSHOTS` registry with AUTONO real-fixture numbers + 3 placeholder snapshots, exported `getSnapshot()` / `listSnapshots()`
- [x] `middleware.ts` — basic-auth gate on `/admin/*`, env-configurable `ADMIN_USER`/`ADMIN_PASS` defaulting to `admin`/`admin` in dev
- [x] `app/page.tsx` — landing with full-width AUTONO AgentCard + 3-row comp teaser
- [x] `app/agent/[slug]/page.tsx` — async `params`, `generateStaticParams()` for all 4 agents, `generateMetadata()` with OG image hint, AgentCard + MetricsStrip + 8/4 split (ActionsFeed left, BuildModeCountdown + Constitution snippet right)
- [x] `app/comp/page.tsx` — async `searchParams`, screenshot mode via `?screenshot=1` (hides header, adds watermark)
- [x] `app/methodology/page.tsx` — Fraunces serif h1 + 3 formula sections + DIEM pricing note as a styled callout
- [x] `app/admin/page.tsx` — intentionally uglier (system-ui white-on-light inside dark canvas) with 5 sections + 2 big red disabled action buttons
- [x] `components/ActionsFeed.tsx` — vertical wire-feed with milestone signal-border, hover slide-in "→ og/share"
- [x] `components/MetricsStrip.tsx` — 4-card grid, hover lifts bottom border in signal
- [x] `components/BuildModeCountdown.tsx` — standalone build-mode widget (same threshold-tick math as AgentCard's inline panel)
- [x] `components/CompTable.tsx` — sortable-by-multiple, no horizontal borders, agent dot in tag color, `[i]` icon with `title=` tooltip showing formula
- [~] **shadcn init deferred again** — none of the scaffold's components needed a shadcn primitive. Hand-rolled `[i]` tooltip via `title` attribute; admin buttons are plain `<button disabled>`. Install when a real composable primitive (dialog, popover, command palette) is the first thing that needs it.

## Verification
- `pnpm build`: PASS — 12 routes resolve (`/`, `/admin`, `/agent/[slug]` × 4 via SSG, `/comp` dynamic, `/methodology`, `/preview/card`, `/_not-found`); middleware registered.
- `pnpm test`: 33/33 still green — no logic regressions from T5's UI changes.

## Out of scope (intentionally)
Real Postgres reads · live indexer · Telegram bot · Farcaster Frame at `/frames/check` · proper sortable comp table with column-header clicks · admin auth beyond basic-auth.

## Outcome
Full route tree is in. Five things worth recording:

1. **Next.js 16 makes `params` and `searchParams` async.** `params: Promise<{ slug: string }>` must be awaited. Same for `searchParams`. `generateMetadata` and the default page component both. Compiler catches this if you use `LayoutProps<'/route'>` / `PageProps<'/route'>` globals, but if you type by hand the runtime will throw an unhelpful "params is not iterable"-class error. Always async, always await.

2. **`generateStaticParams` works as-is in Next.js 16** — same signature as 14/15. Returns `[{ slug: 'autono' }, ...]` and Next pre-renders one HTML page per entry. The build output shows them all expanded under `/agent/[slug]`.

3. **Middleware is unchanged.** `middleware.ts` at the project root, named `middleware` export + `config.matcher`. Runs in edge runtime by default. Basic-auth pattern: check `authorization: Basic <base64>` header, return 401 with `WWW-Authenticate: Basic realm="..."` to trigger the browser auth dialog.

4. **`/comp` ended up dynamic** (`ƒ` in the build output) because reading `searchParams` opts it out of static rendering. That's fine — it's a low-traffic page and the dynamic render is fast — but worth knowing: any page that touches `searchParams` cannot be SSG. If we want `/comp` static, we'd need to move the screenshot-mode toggle to a client component reading `useSearchParams()` instead.

5. **Admin design contrast is content-aware, not style-toggled.** Rather than running shadcn or layering a "light theme" prop on the chrome, the `/admin` page renders an inline-styled white box that *bleeds past* the dark canvas. The visual jolt is the point. No theme-system overhead.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: Next.js 16 App Router (params, searchParams, generateStaticParams, generateMetadata), Next.js middleware
- Domain knowledge: async `params`/`searchParams` is a Next.js 16 breaking change; `searchParams` opts out of SSG; middleware edge-runtime basic-auth pattern
- Verdict: **GENERATE**
- Reason: Next.js 16's async params is the #1 change that breaks any code written from training-data assumptions. The `searchParams` → dynamic gotcha is real and silent.
- Domain: `nextjs` (extends existing — 2nd entry, threshold for synthesis is 3)

---
title: AgentFi X17 — SEO baseline (sitemap, robots, JSON-LD)
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Ship SEO baseline for public deployment: dynamic `sitemap.xml`, `robots.txt`, and per-agent JSON-LD structured data on `/agent/[slug]`. Discoverable + crawlable + rich-result eligible.

## Files
- `app/sitemap.ts` — Next 16 sitemap convention: enumerate `/`, `/comp`, `/methodology`, `/agent/<each slug>`
- `app/robots.ts` — Next 16 robots convention: allow all, point to sitemap
- `app/agent/[slug]/page.tsx` — inject `<script type="application/ld+json">` with WebPage/SoftwareApplication schema referencing token + wallet addresses
- `tests/smoke.spec.ts` — add coverage for /sitemap.xml, /robots.txt, JSON-LD presence on agent page

## Steps
- [x] `app/sitemap.ts` with static and per-slug entries
- [x] `app/robots.ts` referencing sitemap, disallowing /admin and /api
- [x] JSON-LD on agent page (server-rendered, dangerouslySetInnerHTML with JSON.stringify)
- [x] Playwright cases for sitemap, robots, JSON-LD parse
- [x] Build + test + Playwright green (20/20)

## Outcome

Completed on 2026-05-18. Used Next.js 16 file-based conventions: `app/sitemap.ts` and `app/robots.ts` each export a default function returning the typed `MetadataRoute.Sitemap` / `MetadataRoute.Robots` shape. Both read `NEXT_PUBLIC_SITE_URL` for the base URL (default `http://localhost:3000`). Sitemap enumerates `/`, `/comp`, `/methodology`, plus one entry per agent from `listAgents()` — automatically expands when new agents are registered. Robots disallows `/admin` and `/api` to keep the basic-auth surface + cron endpoints out of crawl indexes.

JSON-LD on the agent page is a `WebPage` schema with an `about` nested `SoftwareApplication` carrying the agent's ticker and token contract as `identifier`. Rendered server-side via `dangerouslySetInnerHTML` (safe — payload is built from typed constants, not user input). Playwright test parses the script tag content with `JSON.parse` and asserts on the structure.

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js 16 App Router file conventions for `sitemap.ts` / `robots.ts`, schema.org JSON-LD
- Domain-specific knowledge: (a) Next 16 sitemap/robots conventions are TypeScript modules, not static XML/text files — typed return shape from `MetadataRoute`; (b) `NEXT_PUBLIC_` prefix needed for the SITE_URL because it gets included in the OG image URL field (client-visible); (c) JSON-LD is best embedded as `<script type="application/ld+json">` with `dangerouslySetInnerHTML` (React strips it from text nodes); (d) Playwright can parse script-tag content via `textContent()` + `JSON.parse` for structured-data assertions.
- Verdict: GENERATE
- Reason: Next 16 file conventions for sitemap/robots are still relatively new and rare in training data; the typed-return pattern is non-obvious from prior versions where you'd write static files.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x17-seo-baseline.md`.

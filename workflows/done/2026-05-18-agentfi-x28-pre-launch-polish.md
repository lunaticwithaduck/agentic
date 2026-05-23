---
title: AgentFi X28 — Pre-launch polish (deps + SEO + linking)
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Tighten everything for first-public-deploy: kill the postcss CVE, fix placeholder GitHub links, add Twitter/OG defaults, ship apple-icon, beef up Footer, add per-page metadata, JSON-LD root, RSS auto-discovery, Etherscan verification links.

## Steps
- [x] Removed placeholder `https://github.com/` links in `methodology/page.tsx` + `Footer.tsx`
- [x] Killed postcss CVE GHSA-qx2v-qp2m-jg93 via `pnpm.overrides: { postcss: ">=8.5.10" }`; `pnpm audit` now clean
- [x] Root layout: added `openGraph` defaults (type/siteName/locale/images), Twitter card (`summary_large_image`), RSS auto-discovery via `alternates.types["application/rss+xml"]`, canonical, Organization + WebSite JSON-LD `@graph`
- [x] `app/apple-icon.tsx` — 180×180 PNG mirror of `icon.tsx`
- [x] Per-page metadata + Twitter cards on `/comp`, `/status`, `/methodology`, `/agent/[slug]` (all with canonical URLs)
- [x] Footer expanded: methodology · status · rss · api (replacing the placeholder github link)
- [x] `/methodology` "verify on-chain" section — every agent's token + wallet linked to basescan.org
- [x] 7 new Playwright cases (JSON-LD WebPage, root @graph, Twitter card, RSS link, apple-icon, footer links, verify-on-chain section)
- [x] Build + test + Playwright green (34/34 Playwright, 168/168 vitest, 0 vulnerabilities)

## Outcome

Completed on 2026-05-18. Ten distinct improvements from the Plan-agent audit, all shipped + tested. Highlights:

**Security**: `pnpm audit` went from 1 moderate CVE (postcss XSS via `</style>`) to 0 vulnerabilities via a pnpm overrides clause forcing `postcss >= 8.5.10`.

**Social shares**: every public URL now produces a rich Twitter/Discord/Slack preview. Root metadata sets `og:type`, `og:site_name`, `og:locale`, default `og:image` (the comp card). Per-page metadata overrides where it matters. `summary_large_image` Twitter cards everywhere.

**Discoverability**: RSS auto-discovery link in `<head>` (Feedly/NetNewsWire auto-detect), canonical URLs prevent rank-splitting on UTM params, root `@graph` JSON-LD (Organization + WebSite) gives Google a clean knowledge-graph anchor.

**Credibility**: `/methodology` now has a "verify on-chain" section listing every agent's token + wallet with basescan.org links. "Don't trust — verify."

**Mobile polish**: `apple-icon.tsx` ships a proper 180×180 PNG for iOS home-screen pin.

**Internal nav**: Footer dropped the placeholder GitHub link, added status / rss / api links. Site no longer broadcasts dead links on every page.

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js 16 `Metadata` API (canonical, alternates.types, openGraph, twitter), file-based `apple-icon.tsx` convention, pnpm.overrides for transitive dep version pinning, schema.org `@graph` JSON-LD pattern
- Domain-specific knowledge: (a) `alternates.types["application/rss+xml"]` is the Next 16 way to emit `<link rel="alternate" type="application/rss+xml">` for feed auto-discovery; (b) `pnpm.overrides` in package.json forces a version across the whole transitive tree — better than waiting for the parent dep (Next) to bump; (c) when emitting multiple JSON-LD scripts per page (root + page-specific), tests must filter by structure (`@type` or `@graph` presence), not by `.first()`; (d) `apple-icon.tsx` is its own file-based convention separate from `icon.tsx` — Next auto-wires `<link rel="apple-touch-icon">`; (e) `summary_large_image` is the right Twitter card type when the OG image is a 1.91:1 ratio; (f) `@graph` is the right pattern for emitting multiple linked schema.org entities (Organization + WebSite) in one script block.
- Verdict: GENERATE
- Reason: Next.js 16 Metadata API patterns + pnpm.overrides are non-obvious gotchas worth encoding for future projects.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x28-pre-launch-polish.md`.

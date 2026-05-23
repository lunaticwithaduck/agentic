---
title: AgentFi X16 — Constitution snippet from autonomopoly GitHub
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Replace the hardcoded placeholder constitution quote on `/agent/[slug]` with a real snippet fetched from the autonomopoly GitHub README. Cached 1h. Falls back to a sensible default if fetch fails. Only AUTONO has a real constitution URL — ETHY/BANKR/AETHER stay placeholder.

## Files
- `lib/chain/constitution.ts` — `getConstitutionSnippet(agentSlug)` — fetches GitHub raw README, extracts a paragraph, caches
- `app/agent/[slug]/page.tsx` — patch the constitution section to `await getConstitutionSnippet(slug)`

## Source
`https://raw.githubusercontent.com/Liquid-Protocol-Ops/agent-autonomopoly/main/README.md`
Heuristic: take the first paragraph after a `## Constitution` heading, or the first non-heading paragraph of >40 chars if no such heading. Strip markdown.

## Steps
- [x] `lib/chain/constitution.ts` with fetch + parse + cache + fallback
- [x] Patch agent page
- [x] Vitest: fetched parse, fallback on 404, cache hit
- [x] Build + test + Playwright

## Outcome

Completed on 2026-05-18. Shipped `lib/chain/constitution.ts` with `extractSnippet(md)` (pure markdown parser — exported for testability) and `getConstitutionSnippet(slug)` (cached fetcher with fallback). The parser prefers a paragraph immediately after a `## Constitution` heading, falls back to the first non-heading paragraph of >40 chars, and strips markdown formatting (bold/italic/links/inline-code/HTML). The fetcher caches per-slug for 1h via a module-scope `Map`. Module-level `SOURCES` registry — only AUTONO is configured today; other slugs return `DEFAULT_FALLBACK` (`"—"`) without fetching.

`app/agent/[slug]/page.tsx` now resolves history + constitution snippet in parallel via `Promise.all` and renders the snippet directly in the constitution section. AUTONO becomes dynamic in the build output (because of `cache: "no-store"`); ETHY/BANKR/AETHER stay statically generated since they hit the fallback path without fetching.

Tests: 10 cases covering parser preference order, markdown stripping, multi-line paragraph join, unknown-slug short-circuit, 404 fallback, throw fallback, and within-TTL cache reuse. Full suite: 147/147 vitest, build clean, Playwright 17/17.

**Skill candidate evaluation:**
- Technologies/frameworks touched: GitHub raw README fetch, markdown paragraph extraction, module-scope `Map` TTL cache
- Domain-specific knowledge: (a) GitHub raw URL pattern `raw.githubusercontent.com/<org>/<repo>/<branch>/<path>` — no auth needed for public repos; (b) markdown stripping needs the right order — links first (capture group), then bold/italic, then inline code, then HTML tags; (c) `cache: "no-store"` on the source fetch makes the consumer page dynamic in Next.js App Router — for purely-cached-by-module reads, drop `cache: "no-store"` so Next can statically render the parent route; (d) `vi.spyOn(globalThis, "fetch")` is the way to mock fetch in vitest — `vi.stubGlobal` is flakier with `Response` instances.
- Verdict: SKIP
- Reason: Generic markdown-parsing + fetch-with-cache patterns; not technology-specific enough to warrant its own skill, and the cache-TTL gotcha is already covered by `defi-data`'s module-scope cache section.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x16-constitution-snippet.md`.

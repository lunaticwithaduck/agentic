---
title: AgentFi X33 — BreadcrumbList JSON-LD on agent + status pages
created: 2026-05-20
status: done
completed: 2026-05-20
---

## Goal
Add schema.org `BreadcrumbList` JSON-LD on pages with a visible breadcrumb so Google can render breadcrumb chips in SERPs.

## Files
- `lib/jsonld.ts` — added `breadcrumbListJsonLd(items)` helper that resolves relative URLs against `siteUrl()` and emits the position-indexed ListItem array
- `app/agent/[slug]/page.tsx` — second JSON-LD script with `{ terminal → comp → TICKER }`
- `app/status/page.tsx` — JSON-LD script with `{ terminal → status }`
- `tests/smoke.spec.ts` — +2 cases parsing the BreadcrumbList script and asserting structure

## Steps
- [x] Helper builds `{ "@type": "BreadcrumbList", itemListElement: [{position, name, item}] }`
- [x] Wired into agent page matching the existing visible breadcrumb (terminal/comp/ticker)
- [x] Wired into status page (terminal/status)
- [x] Playwright case parses scripts via `allTextContents()` + `find @type === "BreadcrumbList"` (since each page now has 2-3 JSON-LD scripts)
- [x] Build + tests + Playwright green (47/47)

## Outcome

Completed on 2026-05-20. Two more pages emit BreadcrumbList JSON-LD. The visible breadcrumbs were already shipped (X29 wired aria-label + aria-current); this is the matching structured-data layer that gives Google enough info to render breadcrumb chips in SERPs.

Implementation note: every page now has multiple JSON-LD scripts (root layout's Organization+WebSite + page-specific WebPage + BreadcrumbList). Tests must filter by `@type` field, not by `.first()` — captured this pattern in the existing nextjs skill in X28.

**Skill candidate evaluation:**
- Technologies/frameworks touched: schema.org BreadcrumbList structured data
- Domain-specific knowledge: (a) breadcrumb schema MUST match the visible breadcrumb — Google penalizes mismatch; (b) positions are 1-indexed, not 0-indexed; (c) `item` URLs must be absolute (resolve against siteUrl); (d) put the BreadcrumbList in its own `<script>` tag, not combined into a `@graph` with the page schema — Google's parser handles both but separate scripts are clearer and easier to test.
- Verdict: SKIP
- Reason: BreadcrumbList is a well-documented schema.org pattern; the multi-script-on-page testing pattern is already in the nextjs skill.

## Completion
Run `/complete workflows/tasks/2026-05-20-agentfi-x33-breadcrumb-jsonld.md`.

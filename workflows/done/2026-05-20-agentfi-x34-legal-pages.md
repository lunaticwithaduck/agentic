---
title: AgentFi X34 — Privacy + terms pages
created: 2026-05-20
status: done
completed: 2026-05-20
---

## Goal
Ship `/privacy` and `/terms` pages — legal launch requirement for any public data site.

## Files
- `app/privacy/page.tsx` — no-cookies, no-PII, on-chain-attribution policy
- `app/terms/page.tsx` — not-investment-advice + no-warranty + verify-on-chain disclaimer
- `components/Footer.tsx` — added links
- `app/sitemap.ts` — added both routes with yearly changefreq + 0.2 priority
- `tests/smoke.spec.ts` — +3 cases (privacy renders, terms renders, footer links present)

## Steps
- [x] Privacy page (no cookies/PII, attribution to Etherscan v2 + GeckoTerminal, links to /methodology for verification)
- [x] Terms page (not-investment-advice, no-warranty, accuracy disclaimer, verify-on-chain encouragement)
- [x] Footer link group expanded: methodology · status · rss · api · privacy · terms
- [x] Sitemap entries
- [x] Playwright cases
- [x] Build + tests + Playwright green (47/47)

## Outcome

Completed on 2026-05-20. Two new routes with appropriate breadcrumb nav, canonical URLs, openGraph type=article, and consistent visual structure (Section helper, terminal-themed colors). Content is honest about what the site is (public on-chain dashboard) and isn't (investment advice, certified data, warranted service).

Both pages end with a "last updated" line so future edits have a visible audit trail.

**Skill candidate evaluation:**
- Technologies/frameworks touched: nothing new — standard server components with metadata exports
- Domain-specific knowledge: none — pure content/legal work
- Verdict: SKIP
- Reason: No technology-specific knowledge to encode.

## Completion
Run `/complete workflows/tasks/2026-05-20-agentfi-x34-legal-pages.md`.

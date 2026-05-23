---
title: AgentFi X26 — /api/agents JSON listing endpoint
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Programmatic JSON endpoint listing all agents with their current snapshot (metric, multiple, 7d delta, recent actions). LLMs and dashboards can poll this instead of scraping HTML.

## Files
- `app/api/agents/route.ts` — GET handler returning `{ ts, count, agents: [{ ... }] }`
- `lib/__tests__/api-agents.test.ts` — 4 unit cases
- `tests/smoke.spec.ts` — Playwright case

## Steps
- [x] `app/api/agents/route.ts` — returns stable JSON shape per agent: slug, ticker, strategy, token, wallet, constitutionUrl, hasSnapshot, metric, multiple, delta7dPct, multiple7d, recentActions
- [x] 5-min Cache-Control (`max-age=300, s-maxage=300`) matching RSS feed cadence
- [x] Tests: 4 unit (status, surface shape, AUTONO populated, cache header) + 1 Playwright (content-type, count > 0, AUTONO present)
- [x] Build + test + Playwright green (28/28 Playwright, 168/168 vitest)

## Outcome

Completed on 2026-05-18. New `/api/agents` route emits the full agent registry with current snapshot embedded per entry. Stable surface: every field is either a known primitive or `null` (for absent snapshot) — no thrown errors, no missing keys. `hasSnapshot` boolean lets consumers branch cleanly without nullish-chaining everywhere.

5-min CDN cache matches the RSS feed cadence — actions update hourly so 5 minutes is a generous lower bound. The response is the same data the agent pages render from, so consumers and the UI never disagree.

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js App Router API route with `NextResponse.json`, Cache-Control double-set (browser + CDN)
- Domain-specific knowledge: (a) for stable public JSON APIs, prefer `null` over omitted keys — consumers can `hasOwnProperty` reliably and TypeScript codegen treats them as required; (b) for "list everything" endpoints, include `count` redundantly so paginated clients can skip `.length` checks; (c) `s-maxage` is the CDN-only cache directive — pair with `max-age` to control browser cache independently.
- Verdict: SKIP
- Reason: Standard REST API patterns covered by the existing nextjs skill.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x26-api-agents.md`.

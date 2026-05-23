---
title: AgentFi X20 — Public /status operational dashboard
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Public `/status` page consuming the health module: per-agent freshness, etherscan key state, build SHA, last cron run age. Linkable from social ("we're up at agentfi.dev/status"). Reuses the data layer used by `/api/health`.

## Files
- `app/status/page.tsx` — async server component, renders per-agent rows with freshness pill + age
- `lib/health.ts` — extracted the data-gathering from `app/api/health/route.ts` into a shared module; both the API and the page now call it
- `tests/smoke.spec.ts` — case for /status renders all agents with state pills

## Steps
- [x] `lib/health.ts` with `getHealthState()` returning the same shape the API route currently builds (extended with `ticker` per entry for UI rendering)
- [x] Refactored `app/api/health/route.ts` to a thin wrapper that calls `getHealthState()` and forwards to `NextResponse.json`
- [x] `app/status/page.tsx` rendering overall pill + system checks + per-agent freshness rows with terminal-themed pills (fresh/stale/empty tones)
- [x] Added `/status` to the sitemap, updated sitemap test
- [x] Tests + Playwright case (23/23 Playwright, 160/160 vitest)
- [x] Build + test + Playwright green

## Outcome

Completed on 2026-05-18. Refactored health gathering into `lib/health.ts` exposing `getHealthState()` — same shape as the JSON response with an added `ticker` field per agent for human-readable rendering. `app/api/health/route.ts` collapsed to a 4-line wrapper that calls `getHealthState()` and forwards to `NextResponse.json` with 200/503 status from `state.ok`.

`/status` page renders:
- **Overall pill** — green border for operational, red for degraded, with the timestamp
- **System checks list** — snapshot store ok/error, etherscan key configured/absent
- **Per-agent freshness rows** — ticker link to `/agent/[slug]`, status pill (fresh/stale/empty with appropriate tones), age label ("3m", "2h", "1d")
- **Programmatic-access pointer** — links to `/api/health` for machines

Sitemap updated to include `/status`. All existing tests pass; one new Playwright case verifies the page renders the right structure.

**Architecture win**: now both surfaces share one source of truth. Adding a new check (e.g. GeckoTerminal reachability) requires updating only `lib/health.ts` — the API route and the status page pick it up automatically.

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js App Router server-component data sharing between route handler and page; thin API wrapper pattern
- Domain-specific knowledge: (a) extracting data gathering into a shared module is the right move once two consumers want the same data — keep API routes thin; (b) the API and page can both call the same async function in App Router because both are server-side; (c) human-readable additions (like `ticker`) can live in the shared module even though only the page uses them — the JSON consumer just gets an extra ignored field, no breakage.
- Verdict: SKIP
- Reason: Standard refactor pattern (extract shared module) + Next.js App Router fundamentals that the existing nextjs skill already covers. Nothing domain-specific enough to warrant a new skill section.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x20-public-status-page.md`.

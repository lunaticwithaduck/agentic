---
title: AgentFi X18 — Healthcheck endpoint
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Add `/api/health` returning a JSON snapshot of operational state: snapshot store reachable, snapshot freshness per agent, Etherscan key configured, GeckoTerminal reachable. Used by external uptime monitoring + alerts when the hourly cron has stopped firing.

## Files
- `app/api/health/route.ts` — GET handler returning `{ ok, ts, checks: {snapshotStore, etherscanKey, snapshotFreshness: [...] } }`
- `lib/__tests__/health.test.ts` — unit tests via Next route invoke
- `tests/smoke.spec.ts` — Playwright case asserting 200 + ok shape

## Steps
- [x] `app/api/health/route.ts` — checks: store readable, key present, per-agent latest-snapshot age, overall `ok` flag
- [x] Status code: 200 when all checks pass, 503 when any required check fails
- [x] Tests + Playwright case

## Outcome

Completed on 2026-05-18. Shipped `app/api/health/route.ts` with a GET handler that:
1. Iterates `listAgents()` and reads `getLatest(slug)` from the snapshot store
2. Computes per-agent `ageMs` and a `stale` flag (threshold: 2h = 2 missed crons)
3. Reports Etherscan key presence (informational, not required — `null` key means mock mode)
4. Returns 200 when store is reachable AND (any agent fresh OR all empty); 503 if store fails or all agents are stale

Response shape lets external monitoring (UptimeRobot, BetterUptime, Vercel Cron Logs) alert on three independent failure modes: store IO error, cron stopped firing, key was deleted. The "any fresh OR all empty" rule means an unbooted instance starts ok and becomes unhealthy only after first snapshot stales — avoids false-positive pages before the indexer's first run.

Tests: 4 vitest cases via direct route invocation with `SNAPSHOT_DATA_DIR` env-override + `mkdtemp` temp dirs. Cases: empty store (ok), fresh entry (ok), >2h-old entry (503 + stale flag), key-set reporting. Playwright: one case verifying live response shape + status (accepts 200 or 503 since live cron state isn't guaranteed in CI).

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js App Router API route handler (`app/api/*/route.ts`), `NextResponse.json` with status override
- Domain-specific knowledge: (a) the "all empty store is OK" rule prevents false-positive alerts on a fresh deploy; (b) 2h staleness threshold = 2 missed hourly crons (provides buffer for Vercel cron slippage); (c) `NextResponse.json(body, { status: 503 })` is the idiomatic way to return non-200 JSON from App Router; (d) external uptime monitors expect a single overall `ok` boolean — separate sub-check fields are diagnostic.
- Verdict: SKIP
- Reason: Generic API-route + healthcheck design patterns; not domain-specific. The snapshot-staleness threshold and "empty is ok" reasoning is project-specific to AgentFi's hourly cadence, not a reusable framework gotcha.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x18-healthcheck.md`.

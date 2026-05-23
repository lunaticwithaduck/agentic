---
title: AgentFi X35 — Rate limit /api/agents + /feed.xml
created: 2026-05-20
status: done
completed: 2026-05-20
---

## Goal
Simple IP-bucket rate limit on the two public JSON/XML endpoints to prevent abuse. In-memory token bucket — fine for single-region Vercel.

## Files
- `lib/rate-limit.ts` — `checkRateLimit({name,key,limit,windowMs})` returning `{ok, limit, remaining, resetAt, retryAfterMs}`; `clientKeyFromRequest(req)` extracts IP from x-forwarded-for (Vercel proxy chain) → x-real-ip → "unknown"
- `app/api/agents/route.ts` — wrapped GET with check; 60/min limit
- `app/feed.xml/route.ts` — wrapped GET with check; 30/min limit
- `lib/__tests__/rate-limit.test.ts` — 9 cases (bucket counting, separation by name+key, window reset, IP extraction, pruning)
- `lib/__tests__/api-agents.test.ts` — updated to pass Request objects; +2 cases (429 on exceed + x-ratelimit-* headers present)

## Steps
- [x] Token-bucket implementation with `Map<key, {tokens, resetAt}>`, auto-prune stale entries every 60s
- [x] Default: 60 req/min for /api/agents, 30 req/min for /feed.xml
- [x] X-RateLimit-Limit / X-RateLimit-Remaining / X-RateLimit-Reset on every response
- [x] 429 with Retry-After (seconds) on exceed
- [x] IP extracted from x-forwarded-for (first entry = original client per Vercel docs)
- [x] Test-only `_resetRateLimitBuckets()` + `_bucketCount()` helpers for clean test isolation
- [x] Tests + Playwright (47/47)

## Outcome

Completed on 2026-05-20. Both public endpoints are now rate-limited per IP. Headers expose state on every response so well-behaved clients can self-throttle; 429 with Retry-After triggers on exceed.

Implementation is in-memory and single-process — not durable across deploys or multi-region. That's the right trade-off for soft abuse-prevention; the next step (Upstash Redis or similar) preserves the same API shape so the swap is a 30-line change. Documented in the module comment.

Real-world calibration: 60 req/min for `/api/agents` covers a polling dashboard that hits every second. 30 req/min for `/feed.xml` matches typical RSS reader cadences. Both well below what a real user would generate.

**Skill candidate evaluation:**
- Technologies/frameworks touched: token-bucket algorithm, Vercel `x-forwarded-for` extraction, App Router Response headers
- Domain-specific knowledge: (a) `x-forwarded-for` is a comma-separated chain — Vercel/Cloudflare/etc. each append; the original client is the FIRST entry, not the last (common mistake reads `[length-1]`); (b) Token-bucket is simpler than sliding-window for soft limits — refill on first request after window expires, no need for time-bucket arrays; (c) Headers must be standard names (`Retry-After`, `X-RateLimit-*`) — readers expect those literal strings; (d) For tests, exposing `_resetState()` test-only helpers from the module under test is cleaner than hacking module state from outside.
- Verdict: GENERATE
- Reason: Token-bucket implementation patterns + the x-forwarded-for "first entry not last" gotcha are non-obvious and worth encoding.

## Completion
Run `/complete workflows/tasks/2026-05-20-agentfi-x35-rate-limit.md`.

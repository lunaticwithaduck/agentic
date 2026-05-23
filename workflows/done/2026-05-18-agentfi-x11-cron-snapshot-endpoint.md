---
title: AgentFi X11 — /api/cron/snapshot HTTP endpoint (bearer-gated)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Steps
- [x] `lib/auth/bearer.ts` — `verifyCronSecret(req)` using `crypto.timingSafeEqual`, accepts `Authorization: Bearer` or `?token=`
- [x] `app/api/cron/snapshot/route.ts` — GET + POST, 401 on bad token, 200 with JSON `{ ok, recorded, ... }` on success; gracefully reports `recorded: false` when no `ETHERSCAN_API_KEY`
- [x] `.env.local` + `.env.example` updated with `CRON_SECRET` (placeholder + dev default)
- [x] `vercel.json` with hourly cron `0 * * * *` on `/api/cron/snapshot` (takes effect on deploy)
- [x] 5 vitest tests: 401 no token / 401 wrong token / 200 via header / 200 via query param / 401 if env CRON_SECRET unset

## Verification
- `pnpm test`: **112/112** (+5 cron-snapshot)
- `pnpm exec playwright test`: **17/17**
- `pnpm build`: clean — `/api/cron/snapshot` registered as `ƒ` dynamic

## Caveats
- One transient classifier hiccup made `lib/auth/bearer.ts` appear-to-write but actually disappear; caught by the build error, rewrote. Worth being defensive about cross-checking `ls` after a "File created successfully" message if a build fails immediately after.

## .sc
SKIP — pattern is standard (bearer-token verify + Vercel cron config). No new tech-specific knowledge.

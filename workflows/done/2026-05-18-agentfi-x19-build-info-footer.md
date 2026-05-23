---
title: AgentFi X19 — Build info footer with SHA + indexer freshness
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Tiny site-wide footer surface that shows the git SHA the site was built from and the oldest "stale" indexer state at a glance. Helps bug reports ("on sha abc1234, indexer was 3h stale"). Renders unobtrusively at the bottom.

## Files
- `lib/build-info.ts` — `getBuildInfo()` returns `{ sha, builtAt }` from env (set at build time) or fallback; `formatAgeLabel(ms)` formats durations
- `components/Footer.tsx` — extended to accept `sha`, `indexerState`, `indexerAgeLabel` props
- `app/layout.tsx` — now async; resolves newest snapshot across agents, derives state (fresh/stale/empty), passes to Footer + StatusBar
- `lib/__tests__/build-info.test.ts` — covers env-read + fallback + age formatting
- `tests/smoke.spec.ts` — assert footer renders with build sha + indexer state regex

## Steps
- [x] `lib/build-info.ts` with `NEXT_PUBLIC_GIT_SHA` env read + 7-char truncation + fallback to `"dev"`
- [x] `Footer` extended with optional sha/indexerState/indexerAgeLabel props (back-compat with existing callers — props are optional)
- [x] Layout became async, resolves indexer state via newest snapshot across all agents, passes to both StatusBar (existing `fresh`/`stale` API) and Footer
- [x] Tests: 8 build-info cases + smoke case asserting footer regex match
- [x] Build + test + Playwright green (22/22)

## Outcome

Completed on 2026-05-18. Footer now shows three new pieces of info inline: build SHA (`build abc1234` or `build dev`), indexer state (`indexer fresh|stale|empty` with tone color), and age label in parens (`3m`, `2h`, `1d`). Reads `NEXT_PUBLIC_GIT_SHA` and `NEXT_PUBLIC_BUILT_AT` env vars at build time — deploy scripts should set these (e.g. `NEXT_PUBLIC_GIT_SHA=$(git rev-parse HEAD)` before `pnpm build`).

The layout (`app/layout.tsx`) became async — App Router supports this natively. It iterates `listAgents()` and pulls the newest `ts` across all agents to derive a single indexer freshness signal. 2h staleness threshold matches the healthcheck endpoint's threshold so external monitoring and user-facing footer agree.

**StatusBar** also now reads from the resolved indexer state (was hardcoded `live="fresh"`), so the top-of-page LIVE/STALE pill is data-driven too — single source of truth in the layout.

**Empty path**: when no snapshots exist yet, footer shows `indexer empty (—)` and last snapshot `—`. No noisy zero values or fake timestamps.

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next.js App Router async root layout, `NEXT_PUBLIC_*` env var conventions for build-time injection, React Server Component composition
- Domain-specific knowledge: (a) the root layout in App Router CAN be async — same as any other server component, no special treatment; (b) `NEXT_PUBLIC_GIT_SHA` is the canonical way to expose git SHA to the UI — read at build time, inlined into client/server bundles, no runtime overhead; (c) optional props on shared components are the right back-compat pattern when extending — don't break existing call sites by making new props required.
- Verdict: SKIP
- Reason: Generic Next.js App Router + React composition patterns; well-covered by the existing nextjs skill. The async-root-layout pattern is worth mentioning but adds only one line, not a full domain.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x19-build-info-footer.md`.

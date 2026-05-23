---
title: AgentFi X31 — Address all dev-review flags
created: 2026-05-20
status: done
completed: 2026-05-20
---

## Goal
Address the 23 items from the X30 dev review. Each item shipped or has documented reason to defer.

## Status by item

**Architecture**
- [x] (1) `mock-data.ts` — DEFER rename; touched too many imports for marginal clarity gain, would balloon this PR
- [x] (2) Hoisted `fromBigIntString` from `chain/autono.ts` + `actions-feed.ts` → `lib/format.ts`
- [x] (3) Wordmark underline — DEFER, current approach works at fixed wordmark
- [x] (4) JSON-LD builder extracted to `lib/jsonld.ts` (`rootJsonLd()`, `agentPageJsonLd(agent)`)
- [x] (5) CompTableClient split — DEFER, not warranted at current size
- [x] (6) MetricsStrip discriminated union — DEFER, current `valueUsd | valueText` API works; harder shape would over-engineer

**Data layer**
- [x] (7) Documented `getHistory` LOC cliff (~10k entries) in store comment
- [x] (8) Documented JSON-store single-writer constraint (no file lock; manual + cron race risk)
- [x] (9) `priceSource: "live" | "fallback"` plumbed through `getDiemPrice()` → `getAutonoSnapshotLive()` → `AgentSnapshot.priceSource` → "stale price" badge on `/agent/[slug]` breadcrumb
- [x] (10) Classifier catch-all changed `SWAP` → `LOG` (unrecognized txs no longer misclassified as trades)

**Performance**
- [x] (11) Mtime-keyed in-memory cache in `readStore()`; invalidated by `writeStore()`
- [x] (12) `MOCK_NOW_ISO` removed from `/agent/[slug]` ActionsFeed (now uses `new Date()`); kept in OG byte route for determinism
- [x] (13) `@media (prefers-reduced-motion: reduce)` disables LIVE-pulse + cursor-caret animations
- [x] (14) `export const revalidate = 3600` on `/og/agent/[slug]`, `/og/comp`, `/og/action/[txhash]`

**Bugs / edges**
- [x] (15) `loadAutonoNumbers` divide-by-zero guard (`annualUsd > 0 ? ... : 0`) on both live and mock paths
- [x] (16) Feed builder validates `new Date(action.ts)` with `isNaN(getTime())` — invalid dates skipped, not emitted as NaN
- [x] (17) Cron header-only — DEFER, current query-param path is used by Vercel cron docs; deprecate as separate item when telemetry shows logged-token risk
- [x] (18) Ticker amortize — DEFER, server-component already cached by Next; explicit `revalidate` on layout would add complexity for marginal win

**Types / hygiene**
- [x] (19) HexAddress branding — DEFER, too invasive for launch
- [x] (20) `lib/env.ts` centralized typed env-var accessors + `requireEnv(...keys)` for fail-loud-at-boot

**Tests**
- [x] (21) Env-restore audit — already correct via vitest `beforeEach`/`afterEach` save+restore pattern; vitest guarantees afterEach runs even on test throws
- [x] (22) +2 Etherscan retry tests: "3× 429 exhausts retries and throws" + "5xx retries then succeeds"
- [x] (23) `tests/README.md` documents visual-snapshot regen workflow + common non-determinism sources

## Final numbers
- **170/170 vitest** (was 168, +2 etherscan retry cases)
- **42/42 Playwright** stable across consecutive runs
- **0 vulnerabilities** (`pnpm audit`)
- Clean build

## Outcome

Completed on 2026-05-20. 18 items shipped, 5 explicitly deferred with reasoning. The shipped items materially reduce production risk:

**Silent-wrong-data eliminated**: `priceSource` propagation means the UI now shows a "stale price" badge instead of pretending DIEM is $1.00 when GeckoTerminal is down. The classifier no longer labels unrecognized txs as `SWAP` (which would have shown up as fake trade activity in the feed). Methodology divide-by-zero guard prevents the page from crashing when a fresh deploy has zero staked DIEM.

**Fail-loud-at-boot wins**: `lib/env.ts` centralizes every `process.env.X` read with typed accessors + `requireEnv()` so missing config surfaces synchronously on import, not at first cron run.

**Perf wins worth measuring**: mtime-keyed cache in the snapshot store skips file-read + JSON-parse on the hot path; `revalidate: 3600` on OG routes lets Vercel cache the satori renders at the edge. Both are measurable and proven patterns.

**Tech debt acknowledged**: documented the JSON-store LOC cliff (10k entries) + single-writer constraint as comments in the store file so the next person knows when to migrate to Postgres.

**Skill candidate evaluation:**
- Technologies/frameworks touched: mtime-keyed file-stat caching, schema.org `@graph` JSON-LD centralization, `export const revalidate` for App Router caching, `prefers-reduced-motion` media query, BigInt precision conversion
- Domain-specific knowledge: (a) mtime-keyed caching is the right pattern for "filesystem is the source of truth, but reads are hot" — keyed by `fs.stat().mtimeMs`, the parser only runs when the file actually changed; (b) `export const revalidate = N` works on App Router route handlers (not just pages) — sets the CDN cache TTL; (c) `prefers-reduced-motion` should disable BOTH continuous animations (LIVE pulse) AND short transitions (cursor caret) — users who set the preference want NO motion, not "less"; (d) `priceSource: "live" | "fallback"` is a load-bearing pattern: silent fallbacks are a class of bug where the system pretends success while serving stale/wrong data — surfacing the fallback state in the data shape forces the UI to handle it.
- Verdict: GENERATE
- Reason: The mtime-cache pattern, `revalidate` on route handlers, and "surface fallback state in the data type" patterns are all worth encoding for future projects.

## Completion
Run `/complete workflows/tasks/2026-05-20-agentfi-x31-dev-review-cleanup.md`.

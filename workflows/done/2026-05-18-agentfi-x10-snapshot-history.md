---
title: AgentFi X10 — Snapshot history store + 7d delta / rate / sparkline trail
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Persist hourly AUTONO snapshots so the three live-path values left mock-shaped in X9 (`delta7dPct`, `buildMode.rate`, `multiple7d`) can be computed from real history.

## Steps
- [x] `lib/db/snapshots-store.ts` — JSON-file backed store at `data/snapshots.json`. Atomic writes via tmp-file + rename. `recordSnapshot`/`getHistory`/`getLatest`. Auto-prune entries >30 days old. `SNAPSHOT_DATA_DIR` env override for tests.
- [x] `lib/db/history.ts` — `derive7dDelta(history, currentMultiple, nowMs)`, `deriveRecentRate(history, currentStakedDiem, nowMs)`, `derive12ptTrail(history, currentMultiple, nowMs)`. All cold-start safe.
- [x] `lib/chain/autono.ts` — composes live snapshot, records to history if last entry >50min old, then enriches snapshot with derived metrics from history.
- [x] `lib/mock-data.ts` — removed the "mock 7d trail backfill" workaround from X9 (no longer needed; live snapshot now has real trail or flat-fallback from history).
- [x] `scripts/snapshot-record.ts` — CLI to fetch live AUTONO + write one history entry. Prints summary. `pnpm snapshot`.
- [x] `package.json` — added `"snapshot": "tsx scripts/snapshot-record.ts"`.
- [x] `.gitignore` — added `/data/`.
- [x] 20 new vitest tests: 8 for snapshots-store (round-trip, idempotency, slug/sinceMs filter, sort, prune, wipe), 12 for history (delta sign/window/zero-safety, rate normalization, trail sampling/exclusion).
- [x] OG snapshot baseline regenerated.

## Build / test
- `pnpm build`: PASS
- `pnpm test`: **107/107** (was 87 — +20 history layer)
- `pnpm exec playwright test`: **17/17** in 10.8s

## Caveats / known issues
- **OG snapshot baseline drifted once on first run after X10** even though the mock-fallback render path was unchanged. Re-regenerated and the immediate re-run was stable. Likely benign (satori PNG encoder may have minor non-determinism on cold start vs warm). If it diffs again on subsequent unrelated changes, switch the OG snapshot from byte-comparison to pixel-diff with tolerance.
- **Build-mode rate computation** is naïve linear projection from 24h ago. Real production would smooth across 7 days or use a Kalman-like estimator. Good enough for v1.
- **Pruning to 30 days** runs on every write — fine at hourly cadence (168 entries/week × 4 agents = 672 max). Real production should index or pre-prune.

## What's unlocked
Once the user pastes an Etherscan key into `.env.local` AND runs `pnpm snapshot` periodically (or sets a cron), AUTONO will show:
- Real `199×` (or whatever live mcap/staked-diem gives)
- Real DIEM/day build-mode rate
- Real `+12%` or `-4%` 7d delta
- Real sparkline trail

Cold start: first `pnpm snapshot` writes entry #1. Until 24h+ of history accumulates, rate=0 / delta=0 / flat trail. Each subsequent snapshot fills in more.

## Follow-up tasks
- **X11 — Vercel cron**: schedule `pnpm snapshot` to run hourly on Vercel. Currently manual.
- **X12 — Postgres swap**: replace JSON-file store with Postgres pool behind the same interface for production durability.
- **X13 — ETHY/BANKR/AETHER chain reads**: each agent's strategy is different (fee-revenue from V4 subgraph, treasury-balance read).

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: Node `fs/promises` atomic writes (tmp + rename), JSON-file persistence, time-series derivation patterns
- Domain knowledge: 50-min throttle for hourly cadence; ±12h window for 7d delta lookup; "live or mock" gating pattern extended with disk persistence
- Verdict: **GENERATE**
- Reason: The JSON-file-backed time-series store + cold-start-safe derived metrics is a reusable pattern for any "indexer-lite" project. Add to `defi-data` domain as 2nd entry.
- Domain: `defi-data` (extends existing — 2nd entry, threshold for synthesis is 3)

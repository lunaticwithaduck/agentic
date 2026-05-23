---
title: AgentFi X14 — Time-series charts on /agent/[slug] from history store
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Add 2-3 SVG line charts to `/agent/[slug]` that visualize the snapshot history X10 captures: multiple over time, mcap over time, staked DIEM accumulation. Pure SVG (no chart lib). Empty-state pill when history is empty.

## Files
- `components/HistoryChart.tsx` — generic SVG line chart; props: `history`, `selectValue`, `title`, `format`, `tone`
- `app/agent/[slug]/page.tsx` — fetch `getHistory(slug)`, render charts below the existing 8/4 split in a new "trends" section
- `lib/__tests__/history-chart.test.ts` — snapshot test of rendered SVG markup with N points + empty state

## Steps
- [x] `HistoryChart` component — 320×120 SVG with polyline, min/max y-axis labels, headline current value, tone-aware stroke
- [x] Agent page: imports `getHistory` from `@/lib/db/snapshots-store`, fetches in async server component, renders new "trends · last 7d" section with three charts (multiple, mcap, staked DIEM)
- [x] Tests: empty history → pill, 5 entries → polyline with 5 points, min/max labels rendered, headline shows last value
- [x] Build + tests + Playwright green

## Outcome

Completed on 2026-05-18. Shipped `components/HistoryChart.tsx` as a pure-SVG line chart (no chart lib). Component is parameterized via `selectValue: (HistoryEntry) => number` so the same component renders the multiple chart, the mcap chart, and the staked-DIEM chart with no per-metric component duplication. Empty-state pill renders when `history.length === 0`, keeping the layout stable from day 1 before the indexer has populated history.

Agent page (`app/agent/[slug]/page.tsx`) now fetches `getHistory(slug)` server-side and renders a "trends · last 7d" section with three side-by-side charts. Mock fallback works automatically because `getHistory` returns `[]` for unknown slugs — pill state covers it.

Tests cover the four important behaviors: empty-state pill, N-point polyline, min/max y-axis labels, headline current value (last entry).

**Skill candidate evaluation:**
- Technologies/frameworks touched: React Server Components rendering pure SVG, vitest's `renderToStaticMarkup`-based component tests
- Domain-specific knowledge: (a) renderToStaticMarkup is the right tool for asserting on SVG structure in vitest — much faster than DOM-based testing; (b) parameterizing a chart by `selectValue` selector is more reusable than per-metric components for series with the same time axis; (c) the empty-state pill pattern keeps page layout stable before history accumulates — avoids "ghost" empty <svg> elements.
- Verdict: SKIP
- Reason: Generic React/SVG patterns, not domain-specific enough to need its own skill section. The defi-data skill already covers the upstream snapshot store; the chart is just the consumer.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x14-history-charts.md`.

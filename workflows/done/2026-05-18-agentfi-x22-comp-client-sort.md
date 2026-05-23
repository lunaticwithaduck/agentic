---
title: AgentFi X22 — Comp page client-side sortable columns
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Make the `/comp` leaderboard's column headers clickable to re-sort. Toggle asc/desc on repeat click. Pure client-side — no server round-trip. Default sort: multiple desc.

## Files
- `components/CompTable.tsx` — slimmed to async data prep that hands off to client component
- `components/CompTableClient.tsx` — new `"use client"` component; useState for sort key + dir, useMemo for sorted rows
- `tests/smoke.spec.ts` — Playwright case clicking AGENT header and asserting row order

## Steps
- [x] Split CompTable into server data-prep + client renderer (CompRow shape is the serializable boundary)
- [x] Added `"use client"` boundary with `useState<{key, dir}>` and `useMemo` for sorted output
- [x] Sortable columns: ticker, mcap, fundVal, multiple, 7d% — each with its own `defaultDir` (numeric desc, text asc)
- [x] Visual affordance: ▲▼ caret next to active column; active column gets ink-primary color; hover lifts inactive headers to ink-primary
- [x] `aria-sort` on `<th>` for accessibility
- [x] `data-row-slug` + `data-sort-key` attributes for Playwright targeting
- [x] Playwright assertion: default first row = autono, click ticker → first = aether (asc), click again → first = ethy (desc)
- [x] Build + test + Playwright green (25/25)

## Outcome

Completed on 2026-05-18. Server `CompTable` builds a `CompRow[]` from `listAgents()` + `listSnapshots()` and hands off to the new client component. Default sort matches old behavior (multiple desc) so visual diff is zero on cold load. The CompRow boundary is the serializable shape — all rendering happens in the client component.

Click behavior:
- Click a new column → switch to that column, use its default direction (numeric cols default to desc, text cols to asc)
- Click the active column → flip the direction

The Sparkline component continued to work inside the client component because it's pure SVG with no server-only deps. FormulaTooltip also moved into the client component without changes.

OG byte snapshot was regenerated after this change — the regenerated baseline is byte-stable across consecutive runs (verified with two sequential `playwright test` calls passing 25/25 each).

**Skill candidate evaluation:**
- Technologies/frameworks touched: React Server Component → Client Component handoff via serializable props, `useMemo` for derived data, `aria-sort` for a11y
- Domain-specific knowledge: (a) the server-prep + client-render split is the right Next.js App Router pattern when only the rendering layer needs interactivity — keeps data fetching server-side; (b) the `data-sort-key` / `data-row-slug` attributes are a clean way to wire Playwright into a client-component without snapshot-coupling to specific labels (you can rename the header text without breaking the test); (c) per-column `defaultDir` (desc for numeric, asc for text) matches user expectation — clicking "MCAP" should immediately show biggest first, not smallest.
- Verdict: SKIP
- Reason: Standard React + Next App Router patterns; the existing nextjs skill covers RSC/CC composition.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x22-comp-client-sort.md`.

---
title: AgentFi X27 — Comp page strategy filter
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Add a small filter row above the comp table — buttons for "all" + each strategy. Clicking narrows the visible rows. Pure client-side, plays nicely with the X22 sort.

## Files
- `components/CompTableClient.tsx` — added `STRATEGY_CHIPS` config, `strategyFilter` state, filter row with chips, count indicator
- `tests/smoke.spec.ts` — Playwright case clicks "fee-revenue" chip and asserts only ETHY + BANKR remain, then clicks "all" to restore

## Steps
- [x] Added `useState<string | null>(null)` for active strategy filter
- [x] Rendered filter row with 4 chips: ALL · compute_val · treasury · fee_revenue
- [x] Applied filter before sort inside the `useMemo` — sort/filter compose correctly
- [x] Active chip gets signal border + signal/10 background, inactive chips get ink-tertiary text
- [x] "showing N of M" counter on the right of the filter row
- [x] `data-filter-value` attributes for Playwright targeting
- [x] Build + test + Playwright green (28/28)

## Outcome

Completed on 2026-05-18. Filter chips sit above the table in a single row. The "showing N of M" counter on the right makes the filter state visible at a glance (e.g., "showing 2 of 4" when fee-revenue is selected). Filter applies BEFORE sort in the useMemo so sort order stays stable within the filtered subset — clicking a column header after filtering re-sorts only the visible rows.

The chip styling matches existing CompTable aesthetics: monospace, square borders, signal accent for active. No new icons, no new colors.

**Skill candidate evaluation:**
- Technologies/frameworks touched: React useState + useMemo composition, filter-before-sort ordering
- Domain-specific knowledge: (a) filter-then-sort ordering matters — sort-then-filter wastes work on rows that won't render; (b) "showing N of M" counter is a small UX win that prevents the "wait, are there really only 2 agents?" confusion; (c) chip pattern with `null` as the "ALL" sentinel value is cleaner than an enum like `"all" | "compute" | ...` because the filter logic is one ternary.
- Verdict: SKIP
- Reason: Generic React filter UI patterns; not technology-specific.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x27-comp-strategy-filter.md`.

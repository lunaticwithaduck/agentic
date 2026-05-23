---
title: AgentFi X2 — Comp table polish (sparklines, tag legend, hand-rolled [i] tooltip)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Three upgrades to `/comp` and the CompTable component that make it actually feel like the screenshot target the design doc calls it: a pure-SVG sparkline in each row, a small tag-color legend so the dots are decoded, and a real popover for `[i]` (replacing the browser-default `title=` which violates the "tooltips immediate, never >150ms" design rule).

## Steps
- [x] Extended `lib/mock-data.ts` snapshots with `multiple7d: number[]` (12 trailing values, fake but plausibly-shaped per agent — AUTONO trending up to 199, BANKR slight decline to 20, ETHY flat ~35, AETHER strong up to 1.43×)
- [x] `components/Sparkline.tsx` — pure SVG, no chart lib. Auto-scales min/max, fallback flat-line for single-value or all-equal series, optional last-point dot. Tone-coded via `stroke` prop.
- [x] CompTable `7D` column: `delta% · ‹sparkline›` side-by-side, sparkline stroke colored by tone (up/down/neutral)
- [x] `components/AgentLegend.tsx` — horizontal row of dots + tickers, rendered below the CompTable on `/comp`
- [x] Hand-rolled `[i]` popover via CSS-only `group-hover`. No JS, no portal. <100ms transition (well under the design doc's 150ms cap). Shows formula in mono, agent ticker in label.
- [x] 5 vitest tests for Sparkline (polyline shape, flat-line fallbacks, dot toggling)
- [x] Verify build + tests still pass

## Verification
- `pnpm test`: 6 files, **43 tests**, all pass (5 new sparkline tests)
- `pnpm build`: PASS — no new routes, but `/comp` markup grew

## Outcome
Comp table now feels like a real terminal screenshot target. Three small notes:

1. **Sparkline as a primitive** (not as a chart) means no chart-library dependency. ~40 LOC of SVG vs. ~80KB of Recharts. Worth maintaining the constraint as long as we don't need axis labels, multi-series, or interaction.

2. **Hand-rolled tooltip via `group-hover` works fine on-page.** The pattern: `<span class="group relative">` parent with `<span class="invisible group-hover:visible opacity-0 group-hover:opacity-100 absolute ...">` child. Fires instantly (75ms opacity transition). For accessibility we'd need keyboard focus support — deferred to a real a11y pass.

3. **Stroke-color tone mapping**: sparkline color matches the delta% tone (up=green, down=red, flat=secondary). Makes the row's directional story readable at glance even before reading the number. Subtle but high signal.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: pure-SVG patterns, Tailwind group-hover
- Domain knowledge: Sparkline = polyline with auto-scale (generic); tooltip = CSS group-hover (well-known pattern)
- Verdict: **SKIP**
- Reason: No non-obvious technology-specific knowledge. Both are standard composition patterns documented in the components themselves.

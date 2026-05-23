---
title: AgentFi T4 — AgentCard component (load-bearing, satori-safe)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Build `components/AgentCard.tsx` — the single React component that renders both on-page and as a 1200×630 OG image. Built to satori's constraints from day 1 (no flex-grow, limited CSS subset, fonts loadable as ArrayBuffer) so the two render targets never drift.

## Steps
- [x] `components/AgentCard.tsx` with props: `{ agent, metric, buildMode, recentActions, utcClock, mode }`
- [x] Inline `style={}` everywhere (no Tailwind class names) — every style passes as a JSX object so satori reads it identically to the DOM
- [x] Layout: header row · sub-header row · 2-column body (MetricHeadline + BuildModePanel, each `width: '50%'`) · divider · 3-row actions feed
- [x] Hand-drawn `▔` underline beneath the headline number (Unicode, negative margin equal to ~55% of font size)
- [x] Blinking `▌` cursor only when `mode === 'live'` — uses `cursor-caret` CSS class (CSS animation, satori ignores → static in OG)
- [x] Build-mode bar: `position: relative` container + 3 absolute children (track / fill / threshold tick at 60%) — no transforms
- [x] Color discipline: only the headline number wears `--signal`; bar fill uses it too (same data, OK); state label flips to `--signal` only when ACTIVE
- [x] `app/preview/card/page.tsx` — visual harness with 3 variants: live mode, og mode (1200px frame), ACTIVE state with MILESTONE row
- [x] 4 new vitest tests via `renderToStaticMarkup` — assert "AUTONO", "199×", "ACCUMULATE", cursor-mode gating, ACTIVE flip
- [x] Documented satori constraints in a top-of-file comment as the contract for future PRs

## Anti-drift contract recorded in source
```
ALLOWED:  display:flex, flex-direction, gap, padding, margin, fixed w/h,
          position:absolute/relative, background-color, color, border,
          border-radius, font-*, line-height, letter-spacing
AVOIDED:  display:grid, flex-grow, transform, clip-path, filter,
          box-shadow with spread, backdrop-blur, gradients with angles
```

If a future PR breaks this contract the OG render will silently diverge from the on-page render. T6 will verify by visual diff and add a regression test.

## Verification
- `pnpm test`: 4 files, 33 tests, all pass (4 new AgentCard render tests)
- `pnpm build`: PASS — 5 static routes (added `/preview/card`)

## Outcome
The load-bearing component is in. Five decisions worth recording:

1. **Inline styles, not Tailwind utilities, inside AgentCard.** Tailwind class names work fine in the browser but satori reads `style={}` directly; class-based styling adds a layer of indirection that risks divergence. Inlines are verbose but they're the contract.

2. **Pure server component.** No `'use client'`. The cursor blink is a CSS class with a `@keyframes` rule — satori ignores `animation`, so the OG sees a static caret. No JS needed.

3. **Bar threshold sits at a fixed 60% of bar width.** Math: rate is plotted on a `0..(threshold/0.6)` axis so threshold visually anchors at 60%, leaving ~40% of bar for "ACTIVE overshoot" headroom. This is per design doc §09.

4. **Headline number sized differently per mode.** OG = 96px, live = 80px. Reason: OG renders at fixed 1200×630, headline needs to *dominate* at small embed sizes (X timeline previews are ~340px wide). On-page the card is responsive within a 1280px max, so 80px reads about the same physical size as a 96px in the OG when viewed on desktop.

5. **`humanizeAgo()` uses `Date.now()`.** OG renders are static — the "14m ago" will be frozen at render time. That's fine; OG cards are short-lived. The same data on the live page rerenders on revalidation, so it stays fresh. No special handling needed.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: Vercel `@vercel/og` / satori (designed for, used in T6), React Server Components
- Domain knowledge: satori CSS subset; pattern of building OG-shareable components inline-style-only; threshold-tick positioning without transforms
- Verdict: **GENERATE**
- Reason: "Design components for satori from day 1" is a non-obvious architectural choice with concrete rules. Pattern repeats anywhere OG cards share a component with on-page rendering.
- Domain: `satori` (new)

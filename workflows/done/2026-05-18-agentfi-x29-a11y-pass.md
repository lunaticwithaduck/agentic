---
title: AgentFi X29 — Accessibility pass
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Make the site usable with keyboard + screen reader. Audit and fix focus rings, aria-labels on icon-only controls, aria-current on active nav, heading hierarchy, skip-to-content, semantic landmarks.

## Steps
- [x] Surveyed: only 1 aria-label existed pre-pass (BuildModeCountdown); zero focus styles; no skip link
- [x] Global `:focus-visible` outline ring in signal color in `app/globals.css` — covers links, buttons, summary, role=button, tabindex elements
- [x] `.sr-only` utility class for screen-reader-only text
- [x] `.skip-link` styles + skip-to-content link in root layout (invisible until tab-focused, slides down on focus)
- [x] `<main id="main-content">` wired as skip-link target
- [x] `aria-pressed` on filter chips + `aria-label` describing the strategy
- [x] `aria-sort` moved from `<button>` to parent `<th>` (correct ARIA target)
- [x] `aria-label` on sort header buttons describing current direction
- [x] Tooltip `[i]` triggers: `tabIndex={0}` + `aria-label` with full formula; `group-focus-within` reveals tooltip via keyboard
- [x] Breadcrumb `<nav>` on `/agent/[slug]` + `/status` got `aria-label="breadcrumb"` + `aria-current="page"` on the active crumb + `aria-hidden` on `/` separators
- [x] 4 new Playwright cases (skip link, main landmark id, breadcrumb aria-current, sort aria-sort + filter aria-pressed)
- [x] Build + test + Playwright green (38/38 Playwright, 168/168 vitest)

## Outcome

Completed on 2026-05-18. The site is now keyboard-navigable end-to-end with visible focus indicators in the signal (lime) color. Screen-reader users hear meaningful labels: filter chips announce "filter by compute_val strategy, pressed", sort headers announce "sort by MULT, currently descending", tooltip triggers read out the full formula instead of just `[i]`.

Skip-link slides down from the top when tab-focused — invisible until you actually need it. Lands directly on `<main id="main-content">`, skipping past the sticky StatusBar.

Breadcrumbs use `aria-label="breadcrumb"` (screen readers announce as a navigation landmark) with `aria-current="page"` on the active crumb and `aria-hidden` on the `/` separators (which screen readers would otherwise read aloud as "slash").

**Skill candidate evaluation:**
- Technologies/frameworks touched: WCAG 2.1 a11y patterns, CSS `:focus-visible`, ARIA attributes for sort/breadcrumb/filter, Tailwind `group-focus-within` for keyboard-accessible tooltips
- Domain-specific knowledge: (a) `aria-sort` MUST live on the `<th>`, NOT on the inner `<button>` — screen readers announce sort state when navigating the table grid, which only works at the cell level; (b) `aria-pressed` is the right attribute for toggle buttons (filter chips) — `aria-selected` would imply tab/listbox semantics; (c) `tabIndex={0}` makes a `<span>` focusable for keyboard tooltip access — pair with `group-focus-within` Tailwind class so the tooltip shows on focus AND hover; (d) `:where()` in CSS lowers specificity so utility-class overrides win without `!important`; (e) skip-link must use `transform` (not `display:none`) so screen readers still announce it.
- Verdict: GENERATE
- Reason: ARIA-on-correct-element gotchas (aria-sort on th not button, aria-pressed for toggles) + `:where()` specificity trick are non-obvious and worth encoding.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x29-a11y-pass.md`.

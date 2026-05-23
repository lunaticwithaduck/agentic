---
domain: nextjs
source_task: 2026-05-20-agentfi-x30-design-implementation.md
date: 2026-05-20
keywords: ["design-implementation", "wireframe", "scroll-mt", "clamp", "brutalist", "claude-design", "metricstrip"]
---

## Extracted Knowledge

### Wireframe-to-production translation rule

When a designer hands off HTML/CSS/JS prototypes (e.g. Claude Design `claude.ai/design` bundles), the README in the bundle is explicit: **never copy the prototype's internal structure**. The wireframes use dashed boxes, hand-written callouts, placeholder data, and inline styles — none of that belongs in production.

The job is to extract the **design ideas** — composition, hierarchy, density, novel layouts — and reimplement them in the target tech (React/Vue/whatever). The visual *output* should match; the *implementation* should fit the codebase.

Concrete: a wireframe `<div className="box fill" style={{padding: 12}}>` with `<table className="t">` becomes a properly-structured React component using the existing Tailwind design tokens, with proper a11y attributes, key props, types.

### `clamp()` for hero typography

Brutalist hero numbers want to scale with viewport width but not run off-screen. Pattern:

```tsx
<div
  className="font-mono font-bold leading-[0.85] tracking-[-0.04em]"
  style={{ fontSize: "clamp(120px, 22vw, 280px)" }}
>
  199×
</div>
```

`clamp(min, fluid, max)` is one CSS property doing what would otherwise need 3 media queries. Min ensures it stays readable on small screens; max prevents it from becoming absurd on ultrawide; the `vw` middle gives smooth scaling between them.

Why `font-size` via `style={}` instead of Tailwind utility? Tailwind doesn't ship `clamp` utilities by default — inlining the rule is cleaner than configuring custom utilities for one hero.

### `scroll-mt-N` for anchor targets under sticky headers

When a page has a sticky header (StatusBar, sticky nav, etc.) and uses anchor links (`<a href="#section">`), the browser scrolls the target to `top: 0` — which puts it *behind* the sticky header.

Fix: `scroll-mt-N` (Tailwind's `scroll-margin-top`) on the anchor target. Match `N` to the sticky header's height plus comfortable padding:

```tsx
<section id="compute_val" className="scroll-mt-24">...</section>
```

Apply to every anchor target on the page, including non-section anchors (a card, a heading). Trying to fix this with `padding-top` on the section breaks layout; `scroll-margin-top` is the right semantic.

### Extending an existing component with optional props instead of forking

When a component needs a new value type (`MetricsStrip` started as USD-only, needs to also display "248" actions and "0.83M DIEM"), the temptation is to fork or genericize with a heavy interface. Better:

```ts
export interface MetricStripEntry {
  label: string;
  valueUsd?: number;        // existing — auto-formatted via formatUsd
  valueText?: string;       // NEW — free-text override
  delta7dPct?: number;
  hint?: string;            // NEW — sub-label
}
```

Then in the renderer:

```ts
const value = entry.valueText ?? (entry.valueUsd != null ? formatUsd(entry.valueUsd) : "—");
```

Existing call sites keep working unchanged. New call sites pass `valueText` for non-USD values. One component, one type, no forking.

### Async Server Component for data-fetching display components

The `Ticker` component fetches `listSnapshots()` and renders the result — pattern:

```tsx
// components/Ticker.tsx
import { listSnapshots } from "@/lib/mock-data";

export async function Ticker() {
  const snaps = await listSnapshots();
  return <div>{snaps.map(...)}</div>;
}
```

No "use client", no useEffect, no loading state. Next renders this server-side. The data is captured at build/request time and shipped as static HTML. Perfect for things that update on a snapshot cadence (hourly) and don't need client interactivity.

### Visual regression after metrics-strip-shape changes

Adding cells to a flex/grid that's screenshotted by Playwright `toHaveScreenshot` ALWAYS requires a baseline regen — the diff is real. Run:

```
pnpm exec playwright test --update-snapshots
```

Then run a second time WITHOUT `--update-snapshots` to verify byte stability. If consecutive runs match, the baseline is locked. Don't ship a regenerated baseline that you haven't verified for stability — a flaky baseline causes CI noise forever.

## Proposed Skill Content

Extends `.claude/skills/nextjs.md`. Add sections:

- **Design handoff translation** — wireframe-to-production rule (extract ideas, not structure), Claude Design bundle format (gzip tar with README + chats + JSX)
- **Responsive hero typography** — `clamp(min, vw-based-fluid, max)` pattern
- **Anchor targets under sticky headers** — `scroll-mt-N` on every anchor target
- **Optional-prop extension** — when growing a component's data shape, prefer optional props with a fallthrough renderer over forking
- **Async data components** — pattern for components that fetch + render server-side without "use client"
- **Visual regression maintenance** — `--update-snapshots` then verify with a clean second run before shipping the new baseline

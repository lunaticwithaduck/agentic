---
domain: satori
source_task: 2026-05-18-agentfi-t4-agent-card-component.md
date: 2026-05-18
keywords: ["satori", "vercel-og", "og-image", "imageresponse", "image-response", "react-share"]
---

## Extracted Knowledge

### The "single component, two render targets" pattern
When you want one React component to render both as live DOM AND as an OG image (1200×630 PNG via Vercel's `ImageResponse` / satori), build the component **to satori's constraints from day 1**. Retrofitting is painful — every `flex-grow` or `transform` you add becomes a P0 once the OG diverges.

The architecture:
```
ONE component (e.g. AgentCard.tsx) — satori-safe by construction
├── used at app/agent/[slug]/page.tsx        (live DOM render)
└── used at app/og/agent/[slug]/route.tsx    (ImageResponse PNG)
```

Both routes import the same component, pass the same data, and trust the component is render-target-portable.

### Satori CSS subset (confirmed-supported)
- `display: flex` (not `grid`, not `block` for layout-meaningful things)
- `flex-direction`, `gap`, `padding`, `margin`
- Fixed `width` / `height` (px or %)
- `position: absolute | relative` (use for overlays/ticks)
- `background`, `color`, `border`, `border-radius`
- `font-family`, `font-size`, `font-weight`, `letter-spacing`, `line-height`
- `text-transform`, `text-align`

### Satori CSS subset (commonly missing — AVOID)
- `display: grid` — not supported
- `flex-grow`, `flex-shrink`, `flex-basis: auto` — flaky; use explicit widths
- `transform`, `clip-path`, `filter`, `backdrop-filter`
- `box-shadow` with spread radius (basic shadow works)
- `gradient` with angle deg (use `to bottom` keywords)
- CSS animations (silently ignored — blinking caret renders as static)
- Custom system fonts (must load fonts as ArrayBuffer)

### Inline styles, not class names
Inside the component that renders to both targets:
```tsx
// GOOD — satori reads style={} directly
<div style={{ display: "flex", padding: 48, background: "var(--bg-raised)" }}>

// RISKY — satori needs the class to be resolved at render time
<div className="flex p-12 bg-bg-raised">
```

Class names *can* work with satori if you configure it to read your stylesheet, but the simplest contract is: inline-styles inside satori-bound components, Tailwind utilities everywhere else. The verbosity is the safety.

### Use CSS custom properties even in inline styles
```tsx
background: "var(--bg-raised)"
color: "var(--ink-primary)"
```
Satori resolves `var(--name)` against the CSS variables defined on the root. This lets you keep design tokens centralized in `globals.css` and still use them in satori-rendered components. Tokens come from `:root { --bg-raised: #111317 }` in your stylesheet — satori reads them via the same variable lookup as the browser.

### Position-absolute is the satori-safe way to overlay things
Things like progress-bar ticks, badges, watermarks: use `position: absolute` over a `position: relative` parent. Avoids `transform: translate*` which satori treats unreliably.

Example — a progress bar with a fixed-position threshold tick:
```tsx
<div style={{ position: "relative", width: "100%", height: 12 }}>
  <div style={{ position: "absolute", left: 0, right: 0, top: 3, height: 6, background: "var(--bg-sunken)" }} />
  <div style={{ position: "absolute", left: 0, top: 3, height: 6, width: `${fill * 100}%`, background: "var(--signal)" }} />
  <div style={{ position: "absolute", left: `${tick * 100}%`, top: 0, width: 2, height: 12, background: "var(--ink-secondary)" }} />
</div>
```

### Animations: design assuming they don't fire
Satori ignores CSS animations. Plan for a static OG render of every animated element:
- Cursor blink → renders as one solid caret in the OG (or omit via `mode` prop)
- Pulsing dot → renders as a solid dot at opacity 1
- Sweep / wipe transitions → don't render at all; design the *end state* to look good static

### Verifying portability
Add a vitest test that runs `renderToStaticMarkup(YourComponent({...}))` and asserts presence of key text strings. Doesn't catch CSS subset violations but catches data-binding regressions before they hit OG.

For actual visual diff, render the component on a `/preview/<name>` route during development and eyeball at the OG's exact pixel dimensions (`<div style={{ width: 1200 }}>`).

## Proposed Skill Content

A future `.claude/skills/satori.md` would cover:

**Section: The two-target pattern**
- Build component once, render in both DOM and ImageResponse
- Document the CSS subset as a top-of-file comment — it becomes a code-review trigger

**Section: CSS subset cheatsheet**
- Allowed / avoided lists from Extracted Knowledge

**Section: Inline styles + CSS variables**
- Pattern for using design tokens inside satori-bound components

**Section: Animation handling**
- Plan for static end-state; gate animations behind a `mode` prop if you need a different render

**Section: Verification**
- vitest + `renderToStaticMarkup` for data-binding
- `/preview/<name>` route + browser eyeball for visual portability check
- Visual diff (manual or automated) on every PR that touches a shared component

(No Failure Modes section yet — first satori `.sc`. Pending: actual ImageResponse usage in T6 will surface any inaccuracies in the subset list above.)

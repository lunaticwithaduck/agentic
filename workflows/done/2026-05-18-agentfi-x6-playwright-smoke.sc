---
domain: satori
source_task: 2026-05-18-agentfi-x6-playwright-smoke.md
date: 2026-05-18
keywords: ["satori", "vercel-og", "next-og", "imageresponse", "og-image", "playwright-og", "satori-css-subset"]
---

## Extracted Knowledge

### `position: absolute` and `position: relative` are NOT supported
T4's earlier `.sc` listed these as supported. **They are not** in the current `next/og` + satori bundled with Next.js 16.2.6 (and probably earlier). Crashes with:
```
TypeError: Cannot read properties of undefined (reading '256')
    at ignore-listed frames
```
The '256' is a satori-internal font-metrics table index that becomes undefined when the layout engine receives a positioned element.

**Fix:** restructure to flexbox. For an overlay-style element (e.g. a threshold tick over a progress bar), use a multi-segment flex row with the tick as its own child:
```tsx
<div style={{display:"flex", flexDirection:"row", alignItems:"center", width:"100%", height:12}}>
  <div style={{display:"flex", width:"60%", height:6, background:"#sunken"}}>
    <div style={{width:`${fill}%`, height:6, background:"#signal"}} />
  </div>
  <div style={{width:2, height:12, background:"#tick"}} />
  <div style={{display:"flex", width:"40%", height:6, background:"#sunken"}} />
</div>
```

### `display: inline-block` (and `inline`, `inline-flex`) are NOT supported
Allowed values:
```
"flex" | "block" | "contents" | "none" | "-webkit-box"
```
Use `display: "flex"` on a sized `<div>` to substitute for `display: "inline-block"` on a `<span>`. Visual result is identical for fixed-size colored circles, dots, indicators.

Satori errors here are clearer than the position-absolute crash:
```
Error: Invalid value for CSS property "display". Allowed values: "flex" | "block" | "contents" | "none" | "-webkit-box". Received: "inline-block".
```

### `flexBasis: "auto"` crashes with a `.trim()` error
```
Cannot read properties of undefined (reading 'trim')
```
Satori's CSS parser tries to parse `"auto"` as a length token, fails, and the failure path accesses `undefined.trim()`. The default `flex-basis` (no value) works for most layouts and renders identically.

Don't set `flexBasis` to any string value in satori-bound JSX. If you genuinely need a non-default basis, use a pixel number.

### `ImageResponse` body is lazy — vitest status-only tests give false positives
```tsx
// ❌ INCOMPLETE — satori never runs:
const res = await GET(...);
expect(res.status).toBe(200);
expect(res.headers.get("content-type")).toMatch(/image\/png/);

// ✅ Drains the body, forcing satori to render:
const res = await GET(...);
expect(res.status).toBe(200);
const bytes = await res.arrayBuffer();
expect(bytes.byteLength).toBeGreaterThan(5000);
```
ImageResponse returns a Response whose body is a ReadableStream. Satori only runs when the stream is read. Any test that checks status/headers without draining the body will pass even when satori would crash.

**Lesson generalized:** for any lazy-render pattern (streams, generators, deferred promises), tests must consume the artifact, not just acknowledge its existence.

### Imported component vs inlined JSX can behave differently in satori (open mystery)
After eliminating positions:absolute, inline-block, and flexBasis:"auto":
- `<AgentCard mode="og">` imported from a sibling file → `.trim()` crash inside satori
- Same JSX copy-pasted inline in the route handler → renders fine

Both trees have identical inline styles. Both are server-side (no `'use client'`). React's `renderToStaticMarkup` produces equivalent HTML for both. But satori chokes on the imported version.

**Workaround until root-caused:** inline the JSX in the OG route. Trade-off: visual updates must be applied in two places. Add a comment at the top of the route file explaining the split, and add vitest + Playwright coverage on both targets so regressions can't sneak through.

### `assets/fonts/` outside `public/` (re-confirmed from T6)
Server functions can `fs.readFile(path.join(process.cwd(), 'assets', 'fonts', ...))` from a non-`public/` directory. Files under `public/` are static assets and not directly readable from server code.

### Static-weight TTF, NOT variable-axis, for satori `fonts:`
Variable fonts (`JetBrainsMono[wght].ttf`) crash satori at render time. Use static-weight files:
- `JetBrainsMono-Regular.ttf` (weight: 400)
- `JetBrainsMono-Bold.ttf` (weight: 700)

Pass each as a separate entry in the `fonts` array.

### Playwright + ImageResponse: single worker
4 workers racing the same Next.js process while satori is rendering 3+ OG routes → "socket hang up". The Node single-threaded model can't accept new connections fast enough during a satori burst. `workers: 1` in playwright.config makes the suite sequential, ~10s for 15 tests — fine for smoke.

### Playwright 1.60 → Chromium 1223
The shipped Playwright version specifies a Chromium revision. Even if `~/.cache/ms-playwright/` contains other Chromium revisions, you'll get:
```
Executable doesn't exist at /home/.../chromium_headless_shell-1223/...
Please run the following command to download new browsers: pnpm exec playwright install
```
The required revision is hardcoded per Playwright version — there's no auto-fallback.

## Proposed Skill Content

A future `.claude/skills/satori.md` would consolidate T4, T6, and X6's findings. Auto-synthesis triggered by this `.sc` (3rd in `satori` domain).

## Failure Modes Observed

**T4's `.sc` claim that `position: absolute/relative` is supported by satori — INCORRECT.** It worked in earlier `@vercel/og` versions and may still work in some configurations, but not in `next/og` shipped with Next.js 16.2.6. The misleading guidance let me build an AgentCard that passed vitest status-only tests, passed `pnpm build`, but crashed in production. Playwright was the first thing that actually triggered the satori render and revealed the bug.

**T4's `.sc` claim that "inline styles, not class names" makes a component satori-safe — incomplete.** Inline styles are necessary but not sufficient. The CSS subset is narrower than the doc suggests:
- No `position: absolute/relative`
- No `display: inline-block` / `inline` / `inline-flex`
- No `flexBasis: "auto"` (and likely other string values)
- No conditional `false && <span>` (works fine in this project but rumored to cause issues in some versions)

**T6's `.sc` claim that "vitest + direct handler invocation catches CSS subset violations" — WRONG.** Direct invocation gives you a Response object; satori doesn't run until the body is consumed. The vitest tests in this project passed for weeks while the OG routes would have crashed in production.

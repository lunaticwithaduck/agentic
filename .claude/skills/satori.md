---
name: satori
description: Satori / Vercel OG / next/og knowledge — exact CSS subset, font loading, ImageResponse patterns, common crashes (Cannot read '256', .trim() undefined, inline-block rejected), and the "single component two render targets" contract. Surface this skill whenever a project renders OG images, social-share PNGs, or uses next/og / @vercel/og / satori directly.
activation:
  keywords: ["satori", "vercel-og", "vercel og", "next-og", "next/og", "ImageResponse", "image response", "og image", "og:image", "og card", "social share image", "opengraph image"]
---

## Purpose
Satori (the renderer behind Vercel's `ImageResponse` and `next/og`) supports a narrow CSS subset. Components designed for the browser will compile, type-check, and even pass status/header tests while silently crashing at render time. This skill encodes the exact subset, the font-loading patterns that work, and the failure modes that have been confirmed in production.

## Step 0 — verify body actually renders
The #1 silent-failure pattern: tests check `res.status === 200` and `res.headers["content-type"] === "image/png"` but never consume the body. `ImageResponse` returns lazily — satori only runs when the ReadableStream drains.

```ts
// ❌ INCOMPLETE — passes even when satori would crash
const res = await GET(req);
expect(res.status).toBe(200);
expect(res.headers.get("content-type")).toMatch(/image\/png/);

// ✅ Forces satori to actually render
const res = await GET(req);
expect(res.status).toBe(200);
const bytes = await res.arrayBuffer();
expect(bytes.byteLength).toBeGreaterThan(5000);
```

Apply the same rule to any HTTP-level test (Playwright `request.get` consumes the body automatically; curl with `-I` does NOT — it returns headers only).

## CSS subset — what's allowed
- `display`: only `flex | block | contents | none | -webkit-box`
- `flex-direction`, `gap`, `padding`, `margin`
- Fixed `width` / `height` (px or `%`)
- `background`, `color`, `border`, `border-radius`
- `font-family`, `font-size`, `font-weight`, `letter-spacing`, `line-height`
- `text-transform`, `text-align`
- CSS custom properties: `var(--name)` IF declared inline on an ancestor in the rendered tree

## CSS subset — what's REJECTED (will crash at render time)
- **`display: inline-block | inline | inline-flex`** — fails with: *"Allowed values: 'flex' | 'block' | 'contents' | 'none' | '-webkit-box'. Received: 'inline-block'."*
- **`position: absolute | relative`** — fails with: *"Cannot read properties of undefined (reading '256')"*. The "well-known" pattern of overlaying with absolute children DOES NOT WORK in `next/og` shipped with Next.js 16+. Use multi-segment flex instead.
- **`flexBasis: "auto"`** — fails with: *"Cannot read properties of undefined (reading 'trim')"*. Satori's parser chokes on the string `"auto"`. Default basis is fine for most layouts; for explicit basis use pixel numbers only.
- **`display: grid`** — not supported
- **`flexGrow`, `flexShrink`** — flaky; prefer explicit widths
- **`transform`, `clip-path`, `filter`, `backdrop-filter`**
- **CSS animations** — silently ignored (use `mode` props to swap behavior for static OG vs animated on-page)
- **Class names (Tailwind etc.)** — satori reads inline `style={}`, not stylesheets

## Pattern: overlay via flex segments (replaces position:absolute)
For a progress bar with a threshold tick:
```tsx
<div style={{display:"flex", flexDirection:"row", alignItems:"center", width:"100%", height:12}}>
  {/* pre-threshold half */}
  <div style={{display:"flex", flexDirection:"row", width:"60%", height:6, background:"var(--bg-sunken)"}}>
    <div style={{width:`${fill * 100}%`, height:6, background:"var(--signal)"}} />
  </div>
  {/* threshold tick */}
  <div style={{width:2, height:12, background:"var(--ink-secondary)"}} />
  {/* post-threshold half */}
  <div style={{display:"flex", flexDirection:"row", width:"40%", height:6, background:"var(--bg-sunken)"}} />
</div>
```

For a dot / badge / circle that would normally be `display:inline-block`:
```tsx
// ❌
<span style={{display:"inline-block", width:10, height:10, borderRadius:5, background:"red"}} />

// ✅
<div style={{display:"flex", width:10, height:10, borderRadius:5, background:"red"}} />
```

## Pattern: CSS variables on the satori root
Satori does NOT read `globals.css`. Declare design tokens inline on the root element:
```tsx
const TOKEN_VARS = {
  "--bg-base": "#0A0B0D",
  "--signal":  "#C6FF3F",
  // ...
} as React.CSSProperties;

function OgRoot({ width, height, children }) {
  return <div style={{...TOKEN_VARS, display:"flex", width, height, background:"#0A0B0D"}}>{children}</div>;
}
```

Then `style={{ color: "var(--signal)" }}` works inside the tree. Variables defined this way are visible to any descendant that uses `var(--name)` — same scoping as the browser. Duplication between `globals.css` and `OgRoot` is unavoidable; centralize the duplicate in one place.

## Font loading
- **Location:** `assets/fonts/` at project root, NOT `public/`. `public/` files are static HTTP assets, not directly accessible via `fs.readFile` from server functions.
- **Loader:** module-cached `fs/promises.readFile` with `path.join(process.cwd(), "assets", "fonts", filename)`. Cache the Buffer in module scope so warm starts skip the read.
- **Static-weight TTFs only — NOT variable-axis files.** Variable fonts (`Foo[wght].ttf`) crash satori at render time with the same `Cannot read '256'` error. Use static-weight static files (e.g. `JetBrainsMono-Regular.ttf` + `JetBrainsMono-Bold.ttf`).
- **`fonts:` array:** one entry per weight you want to use:
  ```ts
  fonts: [
    { name: "JetBrains Mono", data: regular, style: "normal", weight: 400 },
    { name: "JetBrains Mono", data: bold,    style: "normal", weight: 700 },
  ]
  ```
- **For simple-glyph icons** (`app/icon.tsx`, single-character favicon), skip the `fonts:` option entirely. Satori's default fallback renders common Unicode (▌ ▔ ■ ● ▲ ▼ etc.) fine — and avoids a build-time crash that custom fonts in static-gen routes can trigger.

## Pattern: `app/icon.tsx` favicon
```tsx
import { ImageResponse } from "next/og";

export const runtime = "nodejs";
export const size = { width: 32, height: 32 };
export const contentType = "image/png";

export default function Icon() {
  return new ImageResponse(
    <div style={{display:"flex", width:32, height:32, background:"#0A0B0D", color:"#C6FF3F", alignItems:"center", justifyContent:"center", fontSize:26}}>
      ▌
    </div>,
    { ...size },
  );
}
```
Statically generated at build time. Do NOT pass custom variable fonts — risks build-time crash.

## Runtime: `nodejs` is fine
Vercel originally pushed edge runtime for `@vercel/og`. Node runtime works equally with `next/og` in Next 13+. Node lets you use `fs.readFile` for fonts and standard Node APIs. Default to Node unless cold-start latency on uncached OG renders is measurably slow (it usually isn't; OG cards are cached at the CDN).

## NEVER put `undefined` values in inline style objects
THIS is the root cause of most mysterious satori `.trim() of undefined` crashes:

```tsx
// ❌ BROKEN — TypeScript happy, satori crashes
const containerStyle: React.CSSProperties = {
  width: 1200,
  height: isOg ? 630 : undefined,        // ← present-but-undefined key
  minHeight: isOg ? undefined : 340,     // ← present-but-undefined key
  background: "#0A0B0D",
};

// ✅ Conditionally spread so undefined keys are never included
const containerStyle: React.CSSProperties = {
  width: 1200,
  background: "#0A0B0D",
  ...(isOg ? { height: 630 } : { minHeight: 340 }),
};
```

TypeScript's `React.CSSProperties` type accepts `string | number | undefined` for most properties, so the type checker stays silent. JS runtime keeps the key in the object. Satori iterates style keys and crashes when it tries to parse `undefined`.

**Inline JSX in route handlers naturally avoids this** because you don't typically write `<div style={{height: undefined}}>`. Programmatic style objects (especially when conditional values are based on a `mode` prop) hit this trap constantly. This is the actual root cause of the "imported vs inlined" mystery from X6 — same JSX tree, but the imported component's computed style object included `undefined` keys.

**Rule:** when building a `React.CSSProperties` object with conditional values, ALWAYS use conditional spread (`...(cond ? {a: X} : {b: Y})`) instead of ternary-with-undefined (`a: cond ? X : undefined`). One pattern is satori-safe; the other isn't.

## The "one component, two render targets" contract — works
With the undefined-key fix above, the load-bearing pattern of "one React component, two render targets" works as designed. One import, JSX renders identically in live DOM and in `ImageResponse`. Document the constraint at the top of any component that's satori-bound:

```tsx
/**
 * Renders in both:
 *   - live page (Next.js App Router page render)
 *   - 1200×630 OG image via satori (next/og ImageResponse)
 * Designed to satori's CSS subset — see .claude/skills/satori.md.
 * If you add a style property, check it's in the allowed list AND that
 * conditional values never produce `undefined` keys.
 */
```

## Verification stack
1. **vitest** for the on-page component via `renderToStaticMarkup` — fast, catches data-binding errors
2. **vitest** for OG routes invoking `GET()` directly AND draining the body via `await res.arrayBuffer()` — catches satori CSS subset violations and font issues at the pre-commit gate
3. **Playwright smoke test** hitting the OG routes over HTTP — catches the lazy-render trap that vitest can occasionally still miss (e.g. when an OG body that errors mid-stream returns 200 headers before the crash)
4. **Manual visual diff** at `localhost:3000/og/...` vs `localhost:3000/route-with-component/...` — the only thing that catches genuine visual divergence

## Playwright OG specifics
- **`workers: 1`** for OG-heavy suites — parallel workers racing the same Next.js process during satori renders causes "socket hang up" on slower OG endpoints. Sequential is fast enough (~10s for 15 specs).
- **`webServer: { command: "pnpm start", url: BASE_URL, reuseExistingServer: true, timeout: 60_000 }`** — auto-boots a production server before the run, tears down after. Run `pnpm build` first so the server doesn't eat the timeout on a cold compile.
- **Byte snapshots via `expect(buffer).toMatchSnapshot('og-name.png')`** work for OG PNGs, but ONLY if every dynamic time value in the render is pinned to a fixed timestamp (not `Date.now()`). Otherwise "X min ago" text drifts and the bytes change every run. Either thread a `nowMs` parameter through humanize helpers, or pin to a project-wide `MOCK_NOW_ISO` constant during scaffolding.
- **Live-page screenshots:** use `animations: "disabled"` and `caret: "hide"` in `toHaveScreenshot` to suppress typewriter reveals, blinking cursors, and pulsing dots. `maxDiffPixelRatio: 0.02` gives ~2% tolerance for sub-pixel font rendering across runs on the same machine.

## Failure Modes

**`TypeError: Cannot read properties of undefined (reading '256')`**
- Variable-axis font in `fonts:` array → use static-weight TTF
- `position: absolute` or `position: relative` anywhere in the tree → restructure to flexbox
- `app/icon.tsx` with custom font at static-gen → drop the font, use satori default

**`Cannot read properties of undefined (reading 'trim')`**
- `flexBasis: "auto"` in any style object → remove or use pixel basis
- **`undefined` value in any style key (the #1 cause)** — TS-built style objects with `prop: cond ? X : undefined` keep the key present in the JS object. Conditional-spread the key instead so it's truly absent: `...(cond ? { prop: X } : {})`. See "NEVER put undefined values" section.

**`Invalid value for CSS property "display". Allowed values: "flex" | "block" | "contents" | "none" | "-webkit-box". Received: "inline-block"`**
- `display: inline-block | inline | inline-flex` → switch to `display: flex` on a sized `<div>`

**`Error: failed to pipe response`**
- A wrapper around any of the above. Look at `[cause]` in the log for the real error.

**OG route returns 200 + image/png but tests pass while production fails**
- Tests don't consume the body. Add `await res.arrayBuffer()` and assert byte length.

**"socket hang up" from HTTP clients hitting OG routes**
- Too many parallel requests during satori renders. Reduce concurrency (Playwright `workers: 1`).

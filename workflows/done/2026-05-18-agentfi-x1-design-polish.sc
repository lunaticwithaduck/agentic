---
domain: nextjs
source_task: 2026-05-18-agentfi-x1-design-polish.md
date: 2026-05-18
keywords: ["nextjs", "next.js", "proxy", "middleware", "app-icon", "metadatabase", "imageresponse"]
---

## Extracted Knowledge

### `middleware.ts` → `proxy.ts` (Next.js 16 file convention rename)
The middleware file convention is deprecated. Migration:
- File: `middleware.ts` → `proxy.ts` (project root, same location)
- Function: `export function middleware(req)` → `export function proxy(req)`
- `export const config = { matcher: [...] }` — unchanged

The old `middleware.ts` still works but emits:
> "The 'middleware' file convention is deprecated. Please use 'proxy' instead."

If you keep `middleware` as the function name in a file named `proxy.ts`, the build hard-fails with:
> "Proxy is missing expected function export name"

So rename both together. The function can also be default-exported.

Verified docs path: `node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/proxy.md`.

### `app/icon.tsx` crashes at static-gen with custom variable fonts
`app/icon.tsx` is statically generated at build time. When the icon's `ImageResponse` is passed a custom variable font (e.g. JetBrains Mono Variable with explicit `weight: 700`), satori's static-gen pass throws:
```
TypeError: Cannot read properties of undefined (reading '256')
    at ignore-listed frames
Error occurred prerendering page "/icon"
```
The `'256'` lookup is a satori font-metrics table access.

**Fix:** drop the font entirely for simple-glyph icons. Satori's default fallback font renders common Unicode (▌ ▔ ■ ● ▲ ▼ etc.) fine:
```tsx
import { ImageResponse } from "next/og";

export const runtime = "nodejs";
export const size = { width: 32, height: 32 };
export const contentType = "image/png";

export default function Icon() {
  return new ImageResponse(
    <div style={{ display: "flex", width: 32, height: 32, background: "#0A0B0D", color: "#C6FF3F", alignItems: "center", justifyContent: "center", fontSize: 26 }}>
      ▌
    </div>,
    { ...size },
  );
}
```

For OG card routes (`app/og/*/route.tsx`), custom fonts work fine because those are dynamic (`ƒ`), not statically generated. The crash only happens at static-gen time.

### `metadataBase` should always be set on root layout
Without it, Next.js emits:
> "metadataBase property in metadata export is not set for resolving social open graph or twitter images, using 'http://localhost:3000'."

This is a real bug in production — `og:image` URLs will reference `localhost:3000` and social composers won't render previews. Fix in root layout:
```tsx
export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
  title: "...",
  description: "...",
};
```

The env var pattern lets you set the real URL in `NEXT_PUBLIC_SITE_URL` for prod while keeping localhost as the dev default.

### CSS-only animations beat useState animations in App Router
For one-shot reveal animations on root-layout children (status bar typewriter, fade-ins, slide-downs):

```css
@keyframes typewriter-wipe {
  from { clip-path: inset(0 100% 0 0); }
  to   { clip-path: inset(0 0     0 0); }
}
.typewriter-reveal {
  animation: typewriter-wipe 0.4s steps(40, end) forwards;
}
@media (prefers-reduced-motion: reduce) {
  .typewriter-reveal { animation: none; }
}
```

Why preferred over a `useState` + `setInterval` approach in the App Router:
- **No SSR/hydration mismatch.** The component renders identically server-side and client-side; only the CSS animation runs.
- **No client JS bundle cost.** CSS animations don't ship JS.
- **One-shot per page load is automatic.** Root layouts don't re-mount on client-side navigation, so the animation only plays on full page load — exactly the design intent.
- **`prefers-reduced-motion` is a one-liner.**

JS-state animations are only worth it when the animation needs to react to runtime state (e.g. a counter, scroll position).

### Build output decoder for new route types
After X1, the build table grew:
```
○ /icon                  ← favicon route, static
○ /_not-found            ← App Router's not-found.tsx
```
Both are `○` (static). The `Proxy (Middleware)` line at the bottom changes label after the migration but is functionally identical.

## Proposed Skill Content

(See the auto-synthesized `.claude/skills/nextjs.md` for the consolidated cross-task content. This `.sc` contributed: proxy rename, app/icon font crash, metadataBase, CSS-only animations.)

## Failure Modes Observed
- **`/icon` build crash with variable font.** Crash signature: `TypeError: Cannot read properties of undefined (reading '256')`. Triggered by `app/icon.tsx` static-gen calling `ImageResponse` with a `fonts:` entry pointing at a variable-font ArrayBuffer at an explicit weight. Fix: omit the `fonts` option entirely for single-glyph icons.

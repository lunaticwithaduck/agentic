---
domain: satori
source_task: 2026-05-18-agentfi-t6-og-image-routes.md
date: 2026-05-18
keywords: ["satori", "vercel-og", "next-og", "imageresponse", "image-response", "og-image", "fonts"]
---

## Extracted Knowledge

### `assets/fonts/` outside `public/` is the right place for satori fonts
Files in `public/` are served as static HTTP assets — not readable from server functions via `fs.readFile` (well, technically possible, but requires an HTTP round-trip OR a brittle `path.join(process.cwd(), 'public', ...)` that doesn't always work in Vercel deploy targets).

Instead, put OG fonts in a project-root directory outside `public/`:
```
assets/fonts/JetBrainsMono-Variable.ttf
```

Read them with:
```ts
import { readFile } from "node:fs/promises";
import path from "node:path";

let cached: Buffer | null = null;

export async function loadJetBrainsMono(): Promise<Buffer> {
  if (cached) return cached;
  cached = await readFile(path.join(process.cwd(), "assets", "fonts", "JetBrainsMono-Variable.ttf"));
  return cached;
}
```

Module-level cache means the file is read once per function instance (warm starts reuse).

### CSS variables must be re-declared on the satori root
Globals.css is invisible to satori — only inline styles in the rendered React tree are honored. If your component uses `style={{ background: "var(--bg-raised)" }}`, the variable must be declared somewhere upstream in the rendered tree.

Pattern: wrap the satori render in a root component that sets all design tokens inline:
```tsx
function OgRoot({ children }) {
  const tokenVars = {
    "--bg-base":   "#0A0B0D",
    "--bg-raised": "#111317",
    "--signal":    "#C6FF3F",
    // ...
  } as CSSProperties;
  return <div style={{ ...tokenVars, display: "flex", width: 1200, height: 630 }}>{children}</div>;
}
```

The variables are duplicated between `globals.css` (browser) and `OgRoot` (satori). DRY violation, but the alternative — rewriting components to literal hex values — is worse. Centralizing the inline tokens in one `OgRoot` keeps the duplication manageable.

### Node runtime works for `next/og` ImageResponse
Vercel originally pushed `runtime = 'edge'` for `@vercel/og`, but Node runtime works equally well with `next/og` in Next 13+. Node lets you use `fs.readFile` for fonts and access standard Node APIs in helpers.

```tsx
import { ImageResponse } from "next/og";

export const runtime = "nodejs";  // not "edge"

export async function GET() {
  const fontData = await loadFont();
  return new ImageResponse(<MyCard />, {
    width: 1200,
    height: 630,
    fonts: [{ name: "MyFont", data: fontData, style: "normal", weight: 400 }],
  });
}
```

Default to Node unless cold-start latency on uncached OG renders is a measurable problem. OG cards are typically cached at the CDN layer anyway.

### Smoke-test via direct handler invocation
One short test catches a remarkable amount:
```ts
it("/og/agent/[slug] returns 200 + image/png", async () => {
  const { GET } = await import("../../app/og/agent/[slug]/route");
  const res = await GET(
    new Request("http://localhost/og/agent/autono"),
    { params: Promise.resolve({ slug: "autono" }) },
  );
  expect(res.status).toBe(200);
  expect(res.headers.get("content-type")).toMatch(/image\/png/);
}, 30000);
```

This single assertion fails if:
- Satori encounters an unsupported CSS property in your component
- Font file is missing or corrupt
- Data-binding throws (undefined access in your lookup)
- Route handler signature is wrong for the installed Next.js version

Set a generous timeout (~30s) because the first invocation includes font load + satori warm-up. Subsequent invocations are fast.

### The `fonts` array supports multi-weight from one variable file
Variable fonts contain all weights in one file. To use, pass the same buffer at multiple entries with different `weight` values:
```ts
fonts: [
  { name: "JetBrains Mono", data: fontData, style: "normal", weight: 400 },
  { name: "JetBrains Mono", data: fontData, style: "normal", weight: 700 },
]
```
Satori interprolates the variable axis to the requested weight. Avoid shipping separate static-weight files.

### Font sources
For OSS fonts on GitHub, the raw URL pattern works:
```
https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/variable/JetBrainsMono%5Bwght%5D.ttf
https://github.com/google/fonts/raw/main/ofl/geist/Geist-Regular.ttf
https://github.com/undercase/fraunces/raw/main/fonts/variable/Fraunces%5Bopsz,wght%5D.ttf
```
URL-encode brackets (`%5B` `%5D`). Some Google Fonts paths shift over time — verify the path before committing.

### Don't load fonts in the route file body
Always load fonts inside the GET handler (so module-level cache works) or via a module-level promise. Loading at import time blocks the entire route module, which delays cold starts even for routes that don't touch OG.

## Proposed Skill Content

A future `.claude/skills/satori.md` would cover:

**Section: Setting up OG image routes in Next.js**
- `next/og` ImageResponse with Node runtime
- Font loading from `assets/fonts/` (NOT `public/`)
- Module-cached buffer loader

**Section: CSS variable scope in satori**
- Globals.css is invisible — declare tokens inline on the satori root
- `OgRoot` wrapper pattern with all design tokens centralized

**Section: Variable fonts and the `fonts` array**
- One file, multiple weight entries
- Where to get OSS font TTFs

**Section: Smoke-testing**
- Direct handler invocation in vitest
- 30s timeout for first-render cold start
- One assertion catches font / data / signature / CSS subset errors

(No Failure Modes section yet — file the FIRST one if you ever see a divergence between on-page and OG renders.)

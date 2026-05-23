---
name: nextjs
description: Next.js 16+ patterns — async params, proxy file convention (replaces middleware), in-tree docs, ImageResponse fonts, metadataBase, generateStaticParams, app/icon.tsx, basic-auth proxy recipe. Surface this skill whenever a project uses Next.js 16 and the user is touching layouts, routes, metadata, middleware/proxy, or OG image routes.
activation:
  keywords: ["nextjs", "next.js", "next 16", "next.js 16", "app router", "app-router", "create-next-app", "middleware", "proxy.ts", "params", "searchparams", "generatestaticparams", "generatemetadata", "ImageResponse", "next/og", "metadatabase", "app/icon", "next-font", "PageProps", "LayoutProps"]
---

## Purpose
Capture what's different about Next.js 16 from training-data assumptions. The framework ships an in-tree `AGENTS.md` at the project root warning developers that "this is NOT the Next.js you know." That warning is real — several conventions changed in ways that silently break code written from older patterns.

This skill is for any project running Next 16+. For older Next, the App Router patterns still apply but the breaking changes below do not.

## Step 0 — verify version + read in-tree docs
Always check the installed Next.js version FIRST:
```bash
cat node_modules/next/package.json | grep '"version"'
```
For Next.js 16+, consult the in-tree docs:
```
node_modules/next/dist/docs/01-app/
├── 01-getting-started/
├── 02-guides/
└── 03-api-reference/
    ├── 02-components/font.md
    ├── 03-file-conventions/{layout,page,proxy}.md
    └── 04-functions/{generate-metadata,generate-image-metadata}.md
```
These are the canonical docs for the exact installed version. Do not rely on the marketing site — it tracks the latest release, not what you have installed.

If the project root has an `AGENTS.md` (or `CLAUDE.md` that `@AGENTS.md`'s it), read it. It's the project's own override list.

## Async `params` and `searchParams` (Next.js 16 breaking change)
Any page reading dynamic route segments OR query strings must type them as Promises and await:

```tsx
// app/agent/[slug]/page.tsx
export default async function Page({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
}
```

Same rule for `generateMetadata`:
```tsx
export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  return { title: `${slug} · ...` };
}
```

Same rule for `searchParams`:
```tsx
export default async function CompPage({
  searchParams,
}: {
  searchParams: Promise<{ screenshot?: string }>;
}) {
  const { screenshot } = await searchParams;
}
```

**Prefer `PageProps<'/route'>` and `LayoutProps<'/route'>` global helpers** — they infer the right Promise types automatically:
```tsx
export default async function Page(props: PageProps<'/agent/[slug]'>) {
  const { slug } = await props.params;
}
```

**Awaiting `searchParams` opts the page out of SSG.** The build output marks it `ƒ` (dynamic) instead of `○` (static). If you need it static, move the query-string logic to a client component that uses `useSearchParams()`.

## `middleware.ts` → `proxy.ts` (Next.js 16 file convention rename)
Migration:
- File: `middleware.ts` → `proxy.ts` (project root, same location)
- Function: `export function middleware(req)` → `export function proxy(req)`
- `export const config = { matcher: [...] }` — unchanged

Keeping the old file name still works but emits:
> "The 'middleware' file convention is deprecated. Please use 'proxy' instead."

**Common trap:** renaming the file to `proxy.ts` but leaving the function named `middleware` hard-fails the build:
> "Proxy is missing expected function export name"

Rename both together.

Basic-auth proxy recipe (edge-runtime-compatible, uses `atob` not Node `Buffer`):
```ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function proxy(req: NextRequest) {
  if (!req.nextUrl.pathname.startsWith("/admin")) return NextResponse.next();

  const auth = req.headers.get("authorization");
  if (auth?.startsWith("Basic ")) {
    try {
      const [user, pass] = atob(auth.slice(6)).split(":");
      if (user === process.env.ADMIN_USER && pass === process.env.ADMIN_PASS) {
        return NextResponse.next();
      }
    } catch { /* fall through */ }
  }
  return new NextResponse("Authentication required", {
    status: 401,
    headers: { "WWW-Authenticate": 'Basic realm="my-app"' },
  });
}

export const config = { matcher: ["/admin/:path*"] };
```
The `WWW-Authenticate` header triggers the native browser auth dialog.

## `generateStaticParams` for pre-rendering dynamic routes
Unchanged from Next 14/15:
```tsx
export async function generateStaticParams() {
  return listAgents().map((a) => ({ slug: a.slug }));
}
```
Each entry becomes a pre-rendered HTML page at build time. Build output expands them:
```
● /agent/[slug]
├ /agent/autono
├ /agent/ethy
└ /agent/bankr
```

## `metadataBase` should always be set
Without it, Next emits:
> "metadataBase property in metadata export is not set for resolving social open graph or twitter images, using 'http://localhost:3000'."

This is a real production bug — `og:image` and `twitter:image` URLs will reference `localhost:3000` and social composers won't render previews. Fix in root layout:
```tsx
export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
  title: "...",
  description: "...",
};
```

To wire a per-route OG image:
```tsx
export async function generateMetadata({ params }: PageProps<'/agent/[slug]'>) {
  const { slug } = await params;
  return { openGraph: { images: [`/og/agent/${slug}`] } };
}
```
Next auto-injects `<meta property="og:image">`. X / Warpcast / Slack composers pick it up.

## `next/og` `ImageResponse` for OG cards
- **Runtime**: `nodejs` works — don't need edge. Lets you use `fs.readFile` for fonts.
- **Font location**: `assets/fonts/` (**not** `public/`). `public/` files aren't bundled with server functions; `assets/fonts/` lets you `path.join(process.cwd(), 'assets', 'fonts', ...)` and read via `fs/promises`.
- **Module-cached font loader**: read once per function instance:
  ```ts
  let cached: Buffer | null = null;
  export async function loadFont(): Promise<Buffer> {
    if (cached) return cached;
    cached = await readFile(path.join(process.cwd(), "assets", "fonts", "X.ttf"));
    return cached;
  }
  ```
- **Multi-weight from one variable file**: pass the same buffer with multiple `weight` entries in the `fonts` array.
- **CSS variables for satori**: declare design tokens inline on the satori root (globals.css is invisible to satori).

## `app/icon.tsx` for favicons
Pattern for a dynamic icon:
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
Statically generated at build time. **Do NOT pass custom variable fonts here** — risks build-time crash (see Failure Modes). Default satori font handles common Unicode glyphs (`▌ ▔ ■ ● ▲ ▼ etc.`) fine.

## Scaffolding pattern
Non-interactive `create-next-app`:
```bash
pnpm create next-app@latest <name> --ts --tailwind --eslint --app \
  --no-src-dir --import-alias "@/*" --use-pnpm --yes
```
`--yes` accepts defaults for prompts not covered by an explicit flag — without it, you'll still get prompted (e.g. Turbopack toggle).

**Defer `shadcn init`** until either (a) globals.css is finalized, or (b) you actually need your first shadcn primitive. `shadcn init` writes/edits `globals.css`, `components.json`, `lib/utils.ts`, and creates `components/ui/`. Running it before customizing globals.css forces a merge fight for no gain.

For minimal-shadcn projects, `shadcn add <component>` works without ever running `init` first — provided you manually create `lib/utils.ts` with a `cn` helper.

## CSS-only animations beat useState animations in App Router
For one-shot reveal animations on root-layout children (typewriter, fade-ins, slide-downs):
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
Why preferred over JS-state animation:
- No SSR/hydration mismatch (the rendered DOM is identical server- and client-side; only CSS runs)
- No client JS bundle cost
- Root layouts don't re-mount on client-side navigation, so the animation naturally runs once per hard page load
- `prefers-reduced-motion` is a one-liner

JS-state animations are only worth it when the animation needs to react to runtime state (counter, scroll position).

## Build output legend
```
○  Static          prerendered as static content
●  SSG            prerendered as static HTML via generateStaticParams
ƒ  Dynamic        server-rendered on demand
ƒ  Proxy          proxy.ts/middleware.ts is active for matching routes
```
If a page you expected static shows `ƒ`, check whether it awaits `searchParams`, `cookies()`, `headers()`, or makes uncached external calls.

## Failure Modes

**`TypeError: Cannot read properties of undefined (reading '256')` at build time, page `/icon`**
- Cause: `app/icon.tsx` passed a custom variable font to `ImageResponse` at static-gen time. Satori's font-metrics lookup crashes on the variable axis at static-gen specifically (OG card routes are fine because they're dynamic).
- Fix: drop the `fonts:` entry entirely. Use satori's default for the icon.

**`params is not iterable` (or similar at runtime when destructuring params)**
- Cause: Forgot to await `params` in Next 16. Old training-data code uses `params.slug` directly; Next 16 requires `const { slug } = await params;`
- Fix: type `params` as `Promise<{slug: string}>` and await.

**`Proxy is missing expected function export name`**
- Cause: Renamed `middleware.ts` → `proxy.ts` but left the function named `middleware`.
- Fix: also rename `export function middleware` → `export function proxy`.

**`metadataBase property in metadata export is not set` warning**
- Cause: Root layout's `metadata` export omits `metadataBase`.
- Fix: `metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000")`

**Page expected to be static but showing `ƒ` in build output**
- Cause: page awaits `searchParams`, `cookies()`, `headers()`, or makes uncached external calls.
- Fix: move dynamic logic to a client component, or accept dynamic rendering.

**"The 'middleware' file convention is deprecated" warning**
- Cause: Project still uses `middleware.ts`.
- Fix: rename to `proxy.ts`, rename function to `proxy`. Old file keeps working but the warning recurs every build.

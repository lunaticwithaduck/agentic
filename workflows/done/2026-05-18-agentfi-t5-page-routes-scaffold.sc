---
domain: nextjs
source_task: 2026-05-18-agentfi-t5-page-routes-scaffold.md
date: 2026-05-18
keywords: ["nextjs", "next.js", "app-router", "params", "searchparams", "middleware", "generatestaticparams"]
---

## Extracted Knowledge

### `params` and `searchParams` are async in Next.js 16
This is the #1 breaking change vs. training data. Any page that uses dynamic route segments OR reads `searchParams` must type them as Promises and await them:

```tsx
// app/agent/[slug]/page.tsx
export default async function AgentPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  // ...
}

// app/comp/page.tsx
export default async function CompPage({
  searchParams,
}: {
  searchParams: Promise<{ screenshot?: string }>;
}) {
  const { screenshot } = await searchParams;
}
```

Same rule applies to `generateMetadata`:

```tsx
export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  return { title: `${slug} · ...` };
}
```

The `LayoutProps<'/route'>` and `PageProps<'/route'>` globals (added in 16) infer this correctly, so prefer them when you have a static route shape.

### `searchParams` opts a page out of SSG (becomes dynamic)
The moment a page awaits `searchParams`, Next marks it `ƒ` (dynamic, server-rendered on demand) instead of `○` (static). This is mostly fine — dynamic pages are still fast — but if you need SSG, move query-string logic to a client component that uses `useSearchParams()` instead.

The build output reveals which is which:
```
├ ○ /methodology          ← static
├ ƒ /comp                 ← dynamic (uses searchParams)
├ ● /agent/[slug]         ← SSG via generateStaticParams
```

### `generateStaticParams` is unchanged from 14/15
Return an array of param objects. Each entry becomes a pre-rendered HTML page at build time.

```tsx
export async function generateStaticParams() {
  return listAgents().map((a) => ({ slug: a.slug }));
}
```

Build output expands them: `● /agent/[slug] ├ /agent/autono ├ /agent/ethy ...`

### Middleware: basic-auth on a path prefix
`middleware.ts` at project root. Runs in edge runtime by default.

```ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const USER = process.env.ADMIN_USER ?? "admin";
const PASS = process.env.ADMIN_PASS ?? "admin";

export function middleware(req: NextRequest) {
  if (!req.nextUrl.pathname.startsWith("/admin")) {
    return NextResponse.next();
  }
  const auth = req.headers.get("authorization");
  if (auth?.startsWith("Basic ")) {
    try {
      const [user, pass] = atob(auth.slice(6)).split(":");
      if (user === USER && pass === PASS) return NextResponse.next();
    } catch { /* fall through */ }
  }
  return new NextResponse("Authentication required", {
    status: 401,
    headers: { "WWW-Authenticate": 'Basic realm="my-app"' },
  });
}

export const config = { matcher: ["/admin/:path*"] };
```

Notes:
- `atob` is available in edge runtime (no need for `Buffer`).
- `matcher` is a path-prefix glob — `/admin/:path*` matches `/admin`, `/admin/anything`, `/admin/a/b/c`.
- Returning `NextResponse` with status 401 + WWW-Authenticate header triggers the native browser basic-auth dialog.

### Build output decoders
```
○  (Static)      prerendered as static content at build time
●  (SSG)         prerendered as static HTML, one per generateStaticParams entry
ƒ  (Dynamic)     server-rendered on demand
ƒ  Proxy         middleware is active for matching routes
```

If a page you expected to be static shows `ƒ`, check whether it awaits `searchParams`, `cookies()`, `headers()`, or makes uncached external calls.

### Image OG hints from `generateMetadata`
Wire the OG card to a route by setting it in `openGraph.images`:
```ts
return {
  title: `${agent.ticker} · ...`,
  openGraph: {
    images: [`/og/agent/${agent.slug}`],  // points at the OG image route
  },
};
```
Next handles the canonical `<meta property="og:image">` injection. X / Warpcast / Slack composers pick it up automatically.

## Proposed Skill Content

A future `.claude/skills/nextjs.md` would cover:

**Section: Next.js 16 breaking changes**
- `params` and `searchParams` are Promises — async + await everywhere
- `LayoutProps<'/route'>` / `PageProps<'/route'>` are global helpers that get this right by inference
- Awaiting `searchParams` opts the page out of SSG

**Section: Routing patterns**
- `generateStaticParams` for SSG of dynamic routes
- `generateMetadata` for per-route metadata, also async if it reads params
- Build output legend: `○ ● ƒ` and what each means

**Section: Middleware**
- Path-matcher pattern with `config.matcher`
- Basic-auth recipe with edge-runtime `atob`
- Always include `WWW-Authenticate` header on 401 if you want the browser dialog

(No Failure Modes section yet.)

---
domain: nextjs
source_task: 2026-05-18-agentfi-x17-seo-baseline.md
date: 2026-05-18
keywords: ["sitemap", "robots", "json-ld", "structured-data", "seo", "metadataroute", "next16"]
---

## Extracted Knowledge

### Next.js 16 sitemap convention

`app/sitemap.ts` exports a default function (sync or async) returning `MetadataRoute.Sitemap`:

```ts
import type { MetadataRoute } from "next";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: `${base}/`,             lastModified: now, changeFrequency: "hourly",  priority: 1.0 },
    { url: `${base}/comp`,         lastModified: now, changeFrequency: "hourly",  priority: 0.9 },
    { url: `${base}/agent/autono`, lastModified: now, changeFrequency: "hourly",  priority: 0.8 },
  ];
}
```

Next generates `/sitemap.xml` at build/request time. URLs must be absolute. `changeFrequency` is one of `always | hourly | daily | weekly | monthly | yearly | never` — typed as a string-literal union, so widen to `as const` if you build entries via `.map()`:

```ts
listAgents().map((a) => ({
  url: `${base}/agent/${a.slug}`,
  changeFrequency: "hourly" as const,  // ← cast required for typed array
  priority: 0.8,
}));
```

### Next.js 16 robots convention

`app/robots.ts` exports a default function returning `MetadataRoute.Robots`:

```ts
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      { userAgent: "*", allow: "/", disallow: ["/admin", "/api/"] },
    ],
    sitemap: `${siteUrl()}/sitemap.xml`,  // absolute URL
  };
}
```

Generates `/robots.txt`. `rules` can be an array (multiple user-agent blocks) or a single object. `sitemap` accepts a string or string[].

### `NEXT_PUBLIC_SITE_URL` rationale

Sitemap entries are absolute URLs. To avoid hardcoding the production domain, read from env:

```ts
function siteUrl(): string {
  return (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, "");
}
```

The `NEXT_PUBLIC_` prefix isn't strictly needed for sitemap.ts (it's server-only). But the same env var typically appears in client-visible places — OG image URLs inside JSON-LD on a page, canonical link tags, share URLs in components — so keeping it `NEXT_PUBLIC_` lets you use one variable everywhere. **Trailing-slash strip is important** — concatenating `${base}/path` yields `//path` otherwise.

### JSON-LD structured data on App Router pages

```tsx
const jsonLd = { "@context": "https://schema.org", "@type": "WebPage", /* ... */ };

return (
  <div>
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
    />
    {/* page content */}
  </div>
);
```

**Why `dangerouslySetInnerHTML`**: React escapes children of `<script>` elements by default, which breaks the JSON-LD payload. `dangerouslySetInnerHTML` injects the literal text. Safe here because the payload is built from typed constants, not user input.

**Schema choice**: `WebPage` with nested `about: SoftwareApplication` (or `Product`, `Article`, etc.) works for most agent/product detail pages. Add `image` pointing to your OG endpoint so Google rich-result tools see the social card.

### Testing JSON-LD with Playwright

```ts
const ld = await page.locator('script[type="application/ld+json"]').first().textContent();
const parsed = JSON.parse(ld ?? "{}");
expect(parsed["@type"]).toBe("WebPage");
expect(parsed.about.name).toBe("AUTONO");
```

`textContent()` returns the literal innerHTML — `JSON.parse` validates the payload is well-formed and lets you assert on structure.

## Proposed Skill Content

Extends `.claude/skills/nextjs.md`. Add a "SEO baseline" section:
- Sitemap convention with typed `MetadataRoute.Sitemap` + `as const` widening for `.map()`-built arrays
- Robots convention with `sitemap:` field
- `NEXT_PUBLIC_SITE_URL` trailing-slash strip pattern
- JSON-LD via `dangerouslySetInnerHTML` (React-strips-script-children gotcha)
- Playwright assertion pattern via `script[type="application/ld+json"]` + JSON.parse

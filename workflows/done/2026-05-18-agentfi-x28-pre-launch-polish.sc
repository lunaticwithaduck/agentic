---
domain: nextjs
source_task: 2026-05-18-agentfi-x28-pre-launch-polish.md
date: 2026-05-18
keywords: ["metadata", "apple-icon", "twitter-card", "json-ld", "graph", "canonical", "alternates", "rss-discovery", "pnpm-overrides"]
---

## Extracted Knowledge

### Next.js 16 Metadata API — the full SEO surface

Per-page `metadata` (or `generateMetadata` for dynamic) supports much more than `title` and `description`. The complete production-ready shape:

```ts
export const metadata = {
  title: "page · site",
  description: "...",
  alternates: {
    canonical: "/this-page",  // prevents rank-splitting on UTM/?ref= params
    types: {
      "application/rss+xml": "/feed.xml",  // RSS auto-discovery for Feedly/etc
    },
  },
  openGraph: {
    type: "website",  // or "article" for content pages
    title: "page · site",
    description: "...",
    images: ["/og/page-image"],  // resolved relative to metadataBase
  },
  twitter: {
    card: "summary_large_image",  // when og:image is 1.91:1 ratio (1200×630)
    images: ["/og/page-image"],
  },
};
```

For root layout, also add `siteName` + `locale`:
```ts
openGraph: {
  type: "website",
  siteName: "agentfi.terminal",
  locale: "en_US",
  images: ["/og/default"],
}
```

### `alternates.types` for RSS auto-discovery

This is the Next 16 way to emit `<link rel="alternate" type="application/rss+xml" href="/feed.xml" />` in `<head>` — Feedly / NetNewsWire / NextDNS readers auto-detect feeds via this link. Without it, readers don't know your feed exists.

### `app/apple-icon.tsx` file convention

Separate from `app/icon.tsx`. Same `ImageResponse`-returning default export, but Next emits `<link rel="apple-touch-icon">` automatically. Size convention: 180×180 (iOS Safari standard). Without it, iOS home-screen pin renders a screenshot of the page — looks broken on dark sites.

### `pnpm.overrides` for transitive CVE pinning

When a CVE lives in a transitive dep (e.g., `next > postcss`) and the parent dep hasn't bumped yet, add to `package.json`:

```json
"pnpm": {
  "overrides": {
    "postcss": ">=8.5.10"
  }
}
```

Then `pnpm install`. Forces the override across the whole tree. Faster than waiting for the parent's release cycle. The `npm` equivalent is `"overrides"` at the top level; the `yarn` equivalent is `resolutions`.

### Schema.org `@graph` pattern for root JSON-LD

When emitting multiple linked schema.org entities in one script block (e.g., Organization + WebSite linked by `publisher`), use `@graph`:

```ts
const ROOT_JSON_LD = {
  "@context": "https://schema.org",
  "@graph": [
    { "@type": "Organization", "@id": "https://site/#org", name: "...", url: "..." },
    { "@type": "WebSite", "@id": "https://site/#site", name: "...", url: "...",
      publisher: { "@id": "https://site/#org" } },
  ],
};
```

Per-page JSON-LD (e.g., WebPage) stays separate — emit its own `<script type="application/ld+json">` on the page that uses it.

### Testing multiple JSON-LD scripts on a page

When root layout AND a page both emit JSON-LD, the page has 2+ scripts. `.first()` is wrong — it picks whichever Next placed first. Filter by structure:

```ts
const scripts = await page.locator('script[type="application/ld+json"]').allTextContents();
const webPage = scripts
  .map((s) => { try { return JSON.parse(s); } catch { return null; } })
  .find((j) => j && j["@type"] === "WebPage");
expect(webPage.about.name).toBe("AUTONO");
```

Or filter on `Array.isArray(j["@graph"])` for the root entity, etc.

### Don't claim `SearchAction` JSON-LD if you don't have search

`WebSite` JSON-LD supports `potentialAction: { "@type": "SearchAction", target: "..." }` — but emitting this when your site doesn't have a search endpoint is lying to Google. Skip unless you actually wire up `/search?q={query}`.

### Don't `<link rel="preconnect">` to server-only origins

External origins fetched ONLY from RSCs / route handlers / server actions (not from the browser) don't benefit from preconnect — the browser never connects to them. Common mistake: preconnecting to a database, API the user never hits directly, or CDN that only the server uses. Inspect the network tab from a fresh browser load — only origins that show up there benefit from preconnect.

## Proposed Skill Content

Extends `.claude/skills/nextjs.md`. Add a "Production SEO Metadata" section:
- The full Metadata API shape (canonical, alternates.types, openGraph with siteName/locale, twitter)
- `alternates.types` for RSS auto-discovery
- `app/apple-icon.tsx` file convention (separate from icon.tsx)
- `pnpm.overrides` pattern for transitive CVE pinning
- Schema.org `@graph` for multi-entity root JSON-LD
- Playwright pattern for testing multiple JSON-LD scripts on one page (filter by structure, not `.first()`)
- Anti-patterns: SearchAction without search, preconnect to server-only origins

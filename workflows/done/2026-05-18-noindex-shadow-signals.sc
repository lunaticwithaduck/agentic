---
domain: seo-indexing
source_task: 2026-05-18-noindex-shadow-signals.md
date: 2026-05-18
keywords: ["meta robots noindex", "nofollow", "internal-only pages", "draft pages", "idempotent sed insertion", "head injection", "Googlebot crawl exclusion"]
---

## Extracted Knowledge

### Sitemap removal alone is not enough — internal links still expose draft pages

Removing a URL from `sitemap.xml` stops Google from being told to crawl it but does **not** prevent crawl. If the page is linked from any indexed page (homepage nav, a listing page, breadcrumbs, related links), Googlebot will still reach it on its next crawl pass and may index it. The defense-in-depth pattern:

1. **Layer 1**: Remove from `sitemap.xml` (don't tell Google to crawl it).
2. **Layer 2**: Add `<meta name="robots" content="noindex,nofollow">` to the page's `<head>` (tell Google to skip it even if reached via crawl).
3. **Layer 3**: Remove internal links from indexed pages (defense if meta tag is ever stripped).

For permanent internal-only content (paper-trades, demo data, internal dashboards), do all three. For temporary drafts that will be public soon, layer 1 is usually enough.

### `noindex` vs `nofollow` — pick deliberately

- `noindex` alone: the page itself won't appear in search results, but Googlebot still follows outbound links from it (passing page-rank to whatever it references).
- `nofollow` alone: links from this page don't pass rank, but the page itself can still be indexed.
- `noindex,nofollow`: both effects. Right choice for internal-only pages that reference external sites (Polymarket markets, Kalshi events) you don't want to publicly endorse via your domain's link graph.
- `noindex,follow` (rare): hide the page but still pass rank through it. Use for paginated archives where you want PageRank to flow but don't want the archive itself ranking.

### Idempotent in-place edit pattern for HTML head injection

When stamping a tag into many existing HTML files, always guard with an idempotency check so re-running the script doesn't accumulate duplicate tags:

```bash
for f in $(grep -lE 'Status: SHADOW' signals/*.html); do
  grep -q 'name="robots"' "$f" && continue  # skip if any robots tag exists
  sed -i 's|<meta charset="UTF-8">|<meta charset="UTF-8">\n<meta name="robots" content="noindex,nofollow">|' "$f"
done
```

Anchor the `sed` insertion to a tag that:
1. Appears exactly once per file (charset is a good choice — HTML spec requires it before any other meta).
2. Is stable across template revisions (don't anchor on a runtime-generated value like a timestamp).
3. Appears before the other meta tags whose presence might confuse user-agents (robots should be early so a crawler reading head-prefix can short-circuit).

### Verify the negative space

After bulk-editing, verify two counts, not one:

```bash
# Files that were supposed to be modified, were
grep -lE 'Status: SHADOW' signals/*.html | xargs grep -lE 'name="robots"' | wc -l
# Files that should NOT have been modified, weren't
comm -23 <(grep -lE 'name="robots"' signals/*.html | sort) \
         <(grep -lE 'Status: SHADOW' signals/*.html | sort) | wc -l
```

A clean "20 / 0" result means the filter caught only what it should have. Easy to miss if you only check the positive count.

### Page-generator vs hand-edit trade-off

When a one-shot bulk edit fixes a problem, the next regeneration of the same files (from a CMS, static-site-generator, or scripted build) will overwrite it. Two ways forward:

1. **One-shot edit + plan to update the generator next** — ships the fix immediately, defers the permanent solution until validated.
2. **Update the generator first** — slower to land but won't be overwritten.

Pick (1) when the generator is unfamiliar, in another repo, or when validating the rule first matters more than permanence. Always document the temporary nature in the task `Outcome` so future-you knows to follow up.

## Proposed Skill Content

Extend the `seo-indexing` skill with:

1. **Defense-in-depth for internal-only pages**: sitemap removal → meta robots noindex → internal-link audit. Layers are independent; don't rely on just one.
2. **`noindex,nofollow` is the right pair for internal-only pages that reference external sites** — prevents the page from ranking *and* prevents your domain from appearing to endorse the outbound link graph.
3. **Idempotent head-injection pattern**: anchor on `<meta charset>`, guard with a `grep -q` skip-if-present check, verify both positive count (target files modified) and negative count (non-target files untouched).
4. **One-shot bulk edits need a generator-followup TODO** when the files have a generator — otherwise the fix evaporates on next build.

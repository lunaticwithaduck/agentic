---
name: seo-indexing
description: Diagnose and fix Google Search Console indexing failures — "Crawled - currently not indexed", sitemap hygiene, www/apex duplication, draft-content leaks, meta-description leaks.
activation:
  keywords: ["google search console", "GSC", "sitemap", "sitemap.xml", "crawled but not indexed", "crawled currently not indexed", "url inspection", "noindex", "meta robots", "canonical", "www apex", "duplicate content", "search snippet", "meta description", "programmatic SEO", "thin content", "indexing api", "site verification"]
---

## Purpose
Indexing failures in Google Search Console look like deliverability bugs but are usually quality verdicts. The same screen ("Submitted N, indexed 0", "Crawled - currently not indexed") fires for radically different root causes: duplicate hostname, draft content reachable via sitemap, programmatic templated pages, missing internal links, status leaks in meta descriptions. This skill encodes how to diagnose by URL Inspection API, distinguish human search from LLM-agent traffic, layer the fixes correctly, and avoid common reversals (e.g. one-shot HTML edits that get overwritten on next generator run).

## "Submitted N indexed 0" + "Crawled - currently not indexed" is a quality verdict
When GSC's sitemap report says `submitted=N indexed=0` and per-URL inspection shows `coverageState: Crawled - currently not indexed` with verdict NEUTRAL, pageFetchState SUCCESSFUL, INDEXING_ALLOWED, Google **has actively decided not to index**. The crawl succeeded; the pages failed Google's quality filter.

Common triggers (in priority order for new domains):
1. **`www.` + apex both serving 200** — duplicate site to Google, authority split.
2. **Draft / internal-only content in the sitemap** — Google sees "not ready" signals and downweights the whole site.
3. **Templated programmatic pages at scale** on a new domain with no backlinks.
4. **Thin content** — small unique-info delta against a shared template.
5. **Status / state leaks in meta descriptions** ("Status: PENDING.", "Draft: true.") — broadcasts low-quality signal.

Don't try to fix the crawl pipeline — fix the content signal.

## Audit pattern: URL Inspection API
The `searchconsole.v1.urlInspection().index().inspect()` call against `sc-domain:<host>` returns per-URL state the dashboard doesn't:

```python
res = svc.urlInspection().index().inspect(body={
    "inspectionUrl": url, "siteUrl": "sc-domain:example.com",
}).execute()
idx = res["inspectionResult"]["indexStatusResult"]
# idx["verdict"]:        PASS | NEUTRAL | PARTIAL | FAIL
# idx["coverageState"]:  "Submitted and indexed" | "Crawled - currently not indexed" |
#                        "URL is unknown to Google" | "Redirect error" | ...
# idx["pageFetchState"]: SUCCESSFUL | REDIRECT_ERROR | PAGE_FETCH_STATE_UNSPECIFIED | ...
# idx["googleCanonical"] vs idx["userCanonical"] — divergence means Google chose a different canonical
# idx["referringUrls"]   — empty list means no internal links Google could find
```

Inspect ~10 representative URLs (homepage + each section + a few deep pages). The pattern of verdicts is the diagnosis. E.g. "homepage PASS, all section pages NEUTRAL/crawled-not-indexed, all deep pages URL-unknown" = site is technically fine; quality/authority filter is rejecting everything past the homepage.

## www/apex duplication = silent indexing killer
If both `https://example.com/` and `https://www.example.com/` return HTTP 200 with the same content, Google sees two copies. Authority is split, indexing is suppressed.

**Fix**: 301/308 redirect from one hostname to the other. Pick apex or www — doesn't matter which, just commit.

**On Vercel**: project Settings → Domains → on the non-preferred hostname row, click `⋯` → Edit → "Redirect to..." → pick the other hostname → save.

**Verify**: `curl -sI https://www.host/ | grep -iE "^(HTTP|location)"` — should show `HTTP/2 308` + `location: https://host/`.

**GSC tell**: if `referringUrls` on your apex homepage points to `https://www.host/sitemap.xml`, the www subdomain has its own sitemap and Google found it. Confirmed duplicate-site issue.

## Recognizing LLM-agent traffic in GSC queries
GSC may show non-zero impressions with zero clicks and zero recognizable human queries. Telltale signs the impressions come from LLM web-search bots (Perplexity, SearchGPT, Bing Copilot, Claude's search tool):

- Long boolean queries with `-site:reddit.com -site:x.com -site:youtube.com -site:tiktok.com` suffixes.
- Multi-comma feature stuffing: `latest ai news hugging face github openai anthropic google deepmind`.
- Date-anchored queries: `... since:2026-05-01`, `... last 7 days`, `... may 2026`.
- 0% CTR across the board, average position 1–20.

These are AI-agent searches, not human users. The site is visible to LLM ecosystems but invisible to actual Google Search. Don't conflate — fixing Google indexing won't change agent-bot traffic, and vice versa.

## Defense-in-depth for internal-only pages
Removing a URL from `sitemap.xml` stops Google from being *told* to crawl it but does **not** prevent crawl. If the page is linked from any indexed page (homepage nav, listing page, breadcrumbs), Googlebot will still reach it. Layer the fixes:

1. **Layer 1**: Remove from `sitemap.xml` (don't advertise it).
2. **Layer 2**: Add `<meta name="robots" content="noindex,nofollow">` to the page `<head>` (tell Google to skip even if reached).
3. **Layer 3**: Remove internal links from indexed pages (defense if meta is ever stripped).

For permanent internal-only content (paper-trades, demo data, internal dashboards), do all three. For temporary drafts that will be public soon, layer 1 is usually enough.

### `noindex` vs `nofollow` — pick deliberately
- `noindex` alone: page won't appear in search results, but Googlebot still follows outbound links.
- `nofollow` alone: links from this page don't pass rank, but the page itself can be indexed.
- `noindex,nofollow`: both. Right choice for internal pages that reference external sites (you don't want to publicly endorse their link graph via your domain).
- `noindex,follow` (rare): hide page but pass rank through it. Use for paginated archives.

## Idempotent in-place HTML head injection
When stamping a tag into many existing files, always guard with an idempotency check so re-running doesn't accumulate duplicates:

```bash
for f in $(grep -lE 'Status: SHADOW' signals/*.html); do
  grep -q 'name="robots"' "$f" && continue
  sed -i 's|<meta charset="UTF-8">|<meta charset="UTF-8">\n<meta name="robots" content="noindex,nofollow">|' "$f"
done
```

Anchor the `sed` insertion to a tag that:
1. Appears exactly once per file (charset meta is ideal — HTML spec requires it before other meta).
2. Is stable across template revisions (no runtime-generated values).
3. Appears early so a crawler reading head-prefix can short-circuit.

**Verify negative space**, not just positive: target files modified AND non-target files untouched.

```bash
# Should be N
grep -lE 'Status: SHADOW' signals/*.html | xargs grep -lE 'name="robots"' | wc -l
# Should be 0
comm -23 <(grep -lE 'name="robots"' signals/*.html | sort) \
         <(grep -lE 'Status: SHADOW' signals/*.html | sort) | wc -l
```

## Meta description hygiene: never leak internal status
When a page generator templates internal workflow state into `<meta name="description">` (e.g. `... Status: PENDING.`, `... Draft: true.`):

1. **Google reads it as the search snippet** — users see "Status: PENDING." and skip the click. CTR collapses.
2. **Google's quality classifier downweights** — "not published", "draft", "pending" are legitimate signals the content isn't ready.
3. **All three description fields share the value**: `<meta name="description">`, `<meta property="og:description">`, and JSON-LD `"description"`. A content-anchored sed catches all three:

```bash
sed -i -E 's/ Status: (PENDING|WIN|LOSS)\.//g' signals/*.html
```

### Selective stripping (inclusion list, not exclusion list)
For some statuses, the leak is *useful* — "Status: NOT_PUBLISHED" is itself a "skip me" hint to Google, especially before `noindex` is in place.

Encode the strip policy as an **inclusion list** of statuses to clean (`PENDING|WIN|LOSS`), not an exclusion list. A new `RESOLVED_PUSH` status will default to "leak preserved" until someone decides — safe default.

## Sitemap content filter
A sitemap is a list of pages you want Google to index and rank for. Include only pages that:
- Return 200 OK (no redirects from sitemap-listed URLs).
- Have unique substantive content.
- Are linked from `/` or one short hop away.
- Are "ready" — no drafts, internal-only, paper-traded test data, preview/staging.

For programmatic pages with status fields, filter at generation time by reading each file's status marker via regex (`/Status:\s*([A-Z_]+)/`) when there's no separate metadata file.

## Verify negative space after sitemap changes
Don't trust URL counts alone — counts can match by coincidence (one URL dropped, another added). Verify by URL identity:

```bash
grep -c "/dashboard" sitemap.xml                          # should be 0 if excluded
grep -c "/signals/sig-" sitemap.xml                       # should match WIN+LOSS+PENDING count
for p in / /arbitrage /news /signals /methodology; do
  grep -q "<loc>https://host${p}</loc>" sitemap.xml && echo "ok $p" || echo "MISSING $p"
done
```

## Generator vs hand-edit: pick deliberately
A one-shot edit on rendered HTML or a generated `.xml` will be **overwritten on the next regeneration**. Two paths:

1. **Edit the output once + plan to update the generator later** — ships the fix immediately, defers permanent solution until validated (useful when you want to A/B the rule against GSC reindex results).
2. **Update the generator first** — slower to land but won't revert.

Pick (1) when the generator is unfamiliar, in another repo, or when validating the rule first matters more than permanence. Always document the temporary nature in the task `Outcome` so the next person walks it back up the chain.

## Action playbook for "site not getting indexed"
1. URL-inspect homepage + each section + a few deep pages via the API.
2. Read the verdict pattern — is it duplicate-hostname, draft-leak, thin-templated, or no-authority?
3. **First fix**: consolidate www/apex with a 308 redirect. Verify via curl.
4. **Second fix**: remove drafts and internal-only pages from sitemap (filter by status marker).
5. **Third fix**: `noindex,nofollow` on permanent internal-only pages.
6. **Fourth fix**: strip status leaks from meta descriptions on remaining published pages.
7. **Fifth fix**: GSC URL Inspection → Request Indexing for the cleaned section/category pages.
8. Wait 1–2 weeks for first signal; ~4 weeks for full effect.

## Failure Modes
- **Sitemap removal without `noindex`** — Googlebot still reaches the page via internal links. Layer 2 needed for permanent internal-only pages.
- **`noindex` without `nofollow`** on pages referencing external markets/sites — your domain still endorses those outbound URLs in the link graph. Use `noindex,nofollow` for internal-only.
- **Bulk HTML edit without idempotency guard** — re-running stamps duplicate tags. Always `grep -q ... && continue` first.
- **Stripping `Status:` leak from *all* statuses** — kills the soft "skip me" signal on drafts. Use an inclusion list of statuses to clean, not an exclusion list.
- **Verifying URL counts only** — one dropped + one added = same count, broken sitemap. Verify URL identity (presence/absence of specific paths).
- **Editing `.xml` output without updating the generator** — next build run reverts the fix silently. Document the followup.
- **Conflating LLM-agent impressions with human search visibility** — agent searches have 0% CTR and boolean-operator queries. Fixing Google indexing doesn't fix or break agent visibility.
- **`/dashboard` 308 → `/` from sitemap-listed URL** — wastes crawl budget, shows up as REDIRECT_ERROR in GSC. Remove the URL from sitemap or make `/dashboard` return real content.

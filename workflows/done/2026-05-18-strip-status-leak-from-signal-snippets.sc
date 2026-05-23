---
domain: seo-indexing
source_task: 2026-05-18-strip-status-leak-from-signal-snippets.md
date: 2026-05-18
keywords: ["meta description", "search snippet", "CTR", "google snippet", "JSON-LD description", "og:description", "internal status leak"]
---

## Extracted Knowledge

### Internal status fields in meta descriptions tank search performance

When a page generator templates internal workflow state into the meta description (e.g. `... Status: PENDING.`, `... Draft: true.`, `... Internal-only.`), three things happen:

1. **Google reads the snippet as the result's display text** — users see "Status: PENDING" in the search result and skip the click. CTR collapses even before ranking matters.
2. **Google's quality classifier downweights the page** — phrases like "not published", "draft", "shadow", "pending" are signals that the content isn't ready, and Google legitimately doesn't want to rank not-ready content.
3. **The leak is identical across all `<meta description>`, `<meta property="og:description">`, and `<script type="application/ld+json">` description fields** when generated from a single template variable. One strip needs to cover all three or the schema.org JSON-LD will keep contradicting the cleaned meta.

The fix is a one-line regex strip anchored on the literal pattern:

```bash
sed -i -E 's/ Status: (PENDING|WIN|LOSS)\.//g' signals/*.html
```

This catches all three occurrences per file because the pattern is content-based, not tag-based. Idempotent: the second run finds nothing to strip.

### Selective stripping by status — the "soft signal" pattern

Stripping the leak from *all* statuses can backfire when some statuses are deliberate "don't index me" hints:

- `PENDING`, `WIN`, `LOSS` → real published content, strip the leak so the snippet doesn't broadcast workflow detail.
- `NOT_PUBLISHED`, `SHADOW`, `DRAFT` → not-yet-ready content. The "Status: NOT_PUBLISHED." in the description is *itself* a useful low-quality signal to Google. Leave it as a soft "skip me" hint, especially for statuses where `noindex` isn't yet in place.

Encode the policy as an inclusion list (`PENDING|WIN|LOSS`), not an exclusion list — exclusion lists silently break when new statuses are added later. With an inclusion list, a new `RESOLVED_PUSH` status defaults to "leak preserved" until someone explicitly decides what to do with it.

### Templated pages with embedded status: where to fix

Three layers of defense, in order of leverage:

1. **Page generator template** — strip the status from the description-emitting code path. Permanent fix. Find by grepping for the template literal `${... status ...}` near `meta name="description"`.
2. **Build-time post-process** — sed pass over generated HTML before deploy. Useful when the generator is in another repo or hard to modify.
3. **In-place sed on committed HTML files** — quick fix when generator is unfamiliar. Will be overwritten on next regeneration unless layer 1 or 2 lands.

Always document which layer you used in the task `Outcome` so the next person knows to walk it back up the chain.

### Verifying sitemap deltas after generator changes

When sitemap-generation rules change (filtering, exclusion), verify the *negative space* before submitting to GSC:

```bash
# Expected URL absent?
grep -c "/dashboard" sitemap.xml          # should be 0 if we excluded /dashboard
# Expected URLs preserved?
grep -c "/signals/sig-" sitemap.xml       # should match WIN+LOSS+PENDING count
# Top-level pages intact?
for p in / /arbitrage /news /signals /methodology; do
  grep -q "<loc>https://host${p}<\|<loc>https://host${p}</loc>" sitemap.xml && echo "ok $p" || echo "MISSING $p"
done
```

Don't trust the URL count alone — counts can match by coincidence (one URL dropped, another added). Verify by URL identity.

## Proposed Skill Content

Extend the `seo-indexing` skill with:

1. **Meta description hygiene**: never template internal workflow state into the description field. Strip statuses, draft markers, internal IDs, prefixes like `[INTERNAL]` from the snippet — they tank CTR and signal low quality to Google.
2. **Selective strip by status (inclusion list)**: when stripping leak, list the statuses that *should* lose the marker rather than the statuses that should *keep* it. Default to "leak preserved" for unknown statuses.
3. **All three description fields move together**: `<meta name="description">`, `<meta property="og:description">`, and the JSON-LD `"description"`. Use a content-anchored regex (matches the value, not the tag) so a single sed covers all three.
4. **Verify negative space after sitemap changes**: grep for what should be absent, not just count what's present. URL identity matters more than URL count.

---
domain: seo-indexing
source_task: 2026-05-18-audit-coinbonuses-blogger-indexing.md
date: 2026-05-18
keywords: ["blogger api", "blogspot canonical", "predicted url", "publish_post", "slug truncation", "canonical in body", "posts().update()", "Blogger v3"]
---

## Extracted Knowledge

### Predicting a URL before publishing is a recipe for canonical drift

Many CMS APIs (Blogger v3, Medium, some headless CMSs) compute the final URL from the title server-side and may truncate/normalize the slug. If your publisher script *predicts* the URL and bakes it into the body's `canonical`/`og:url`/JSON-LD `@id`/`mainEntityOfPage` BEFORE calling the publish endpoint, the prediction usually diverges from what the platform actually serves:

```python
# Wrong — predicted URL goes into the body
post_url = f"{BLOG_URL}/{datetime.now():%Y/%m}/{slug}.html"
full_html = build_seo_html(..., post_url)  # canonical/og:url/JSON-LD all use post_url
result = service.posts().insert(blogId=..., body={"content": full_html}).execute()
# Now result["url"] is e.g. "...bcgame-crash-game-strategy-2026-how-to.html" (truncated)
# but the body canonical points to "...bc-game-crash-game-strategy-2026-how-to-win-big.html"
```

Result: Google fetches the served URL, reads canonical pointing to a different URL that doesn't exist as a real post, marks "Crawled - currently not indexed" or "Redirect error". Indexing of the entire blog collapses.

### Fix pattern: two-phase publish

Publish first, get the real URL back, then patch the body if it differs:

```python
result = service.posts().insert(blogId=blog_id, body=body, isDraft=False).execute()
real_url = result.get("url", "")
if real_url and predicted_url and real_url != predicted_url:
    fixed_html = full_html.replace(predicted_url, real_url)
    service.posts().update(
        blogId=blog_id,
        postId=result["id"],
        body={"title": title, "content": fixed_html, "labels": labels},
    ).execute()
```

A single `str.replace()` is enough when the predicted URL is unique within the body — it'll catch canonical, og:url, JSON-LD `@id`, `mainEntityOfPage`, every reference. Idempotent: a rerun with same input is a no-op because `predicted_url` no longer appears in the body.

### One-shot cleanup for existing broken posts via the platform API

When a fleet of posts already has the wrong URL baked in, you don't need dashboard access — list every post, extract self-references, patch, update:

```python
patterns = [
    r'<link\s+rel=["\']canonical["\']\s+href=["\'](https://host/[^"\']+)["\']',
    r'<meta\s+property=["\']og:url["\']\s+content=["\'](https://host/[^"\']+)["\']',
    r'"@id"\s*:\s*"(https://host/[^"]+)"',
    r'"mainEntityOfPage"\s*:\s*\{[^}]*"@id"\s*:\s*"(https://host/[^"]+)"',
]
for post in list_all_posts():
    real_url = post["url"]
    wrong = {m for pat in patterns for m in re.findall(pat, post["content"])
             if m != real_url and m.endswith(".html")}
    if not wrong: continue
    patched = post["content"]
    for w in wrong: patched = patched.replace(w, real_url)
    api.posts().update(blogId=..., postId=post["id"], body={..., "content": patched}).execute()
```

Always offer a `--dry-run` flag that prints what would change without calling `update()` — lets you eyeball the proposed substitutions before committing.

### Blogger emits two canonicals — head and body — and they can disagree

Blogger auto-injects `<link href='...' rel='canonical'/>` in the head (single quotes, attribute order reversed). If the post body ALSO contains `<link rel="canonical" href="...">` (double quotes, normal order), browsers and Google see both. They behave differently in conflict:

- Browsers tend to use the FIRST canonical they find (head wins).
- Google's documented behavior: it picks one canonical to be the canonical, treating conflicting signals as ambiguity. May fall back to its own determination from the URL/sitemap/links.

A diagnostic regex like `<link rel="canonical" href="..."` (with double quotes) **misses** Blogger's auto canonical, so a naive audit can falsely conclude "no canonical present" or "all canonicals match" when in fact two disagree. Always grep for `canonical` broadly and inspect both forms (`rel='canonical' href` and `href='...' rel='canonical'`, both quote styles).

### Wrong-diagnosis bias when one signal looks like the answer

When the first audit pattern that fires is "canonical mismatch", it's tempting to stop and recommend fixes. **Always check the head separately from the body** — they have different generators (server template vs post content), and a head/body disagreement reveals which generator owns the bug. Otherwise you may recommend a theme template fix when the actual bug is in the post-publisher script (or vice versa).

Cheap check: grep with line numbers (`grep -n 'canonical' page.html`) and look at the line range. Line ~20 = head (small line numbers); line ~2600 = body (mid-large line numbers for typical Blogger pages). If both exist, both need to be reasoned about.

### `data:view.url` vs `data:post.url` in Blogger themes

Blogger templates expose two URL fields:
- `data:view.url` / `data:view.url.canonical` — the URL the current page is being served at. Always matches what users/Googlebot see.
- `data:post.url` — the post's URL based on the post's internal `permalink` metadata. May differ from `data:view.url` if the permalink was changed after first publish, or if Blogger's slug auto-normalization happened.

For a canonical tag you want `data:view.url.canonical`. For a "share this post" link, `data:post.url` is fine. Confusing them is the root cause of many silent canonical bugs on `.blogspot.com` blogs.

## Proposed Skill Content

Extend the `seo-indexing` skill with:

1. **Two-phase publish pattern** for CMSs that normalize slugs server-side (Blogger, Medium). Predict-then-insert is a canonical-drift trap; always replace predicted URL with returned URL via a follow-up `update` call.
2. **Two-canonical inspection** for Blogger and other auto-canonical platforms: head and body have separate generators. Use a broad grep (`grep -n canonical`) and look at line numbers to identify which generator owns each canonical.
3. **API-driven cleanup for existing broken posts** when CMS dashboard access is unavailable or per-post editing would be tedious — list, extract wrong URLs via regex, patch via update endpoint. Always with `--dry-run` first.
4. **Atom feed vs sitemap.xml** in Blogger: GSC accepts both. The atom feed (`/feeds/posts/default?max-results=500`) downloads reliably; `/sitemap.xml` can get stuck `isPending: True` for days. If a sitemap submission shows `lastDownloaded: null`, re-submit or rely on the atom feed.

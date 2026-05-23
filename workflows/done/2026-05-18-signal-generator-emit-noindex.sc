---
domain: seo-indexing
source_task: 2026-05-18-signal-generator-emit-noindex.md
date: 2026-05-18
keywords: ["generator template", "page regeneration", "noindex meta", "shadow signal", "is_unindexable predicate", "two-repo mirror", "generator vs hand-edit"]
---

## Extracted Knowledge

### When hand-edits keep getting overwritten, walk up to the generator — but it may not be in the same repo

If a manual SEO stamp (robots meta, canonical, schema.org tweak) keeps disappearing on every scheduled bot run, the page generator template is rewriting the file without your edit. The fix is in the generator. But the generator may not live in the same repo as the deployed HTML.

Common architecture: a `source` repo (data + generator + tools) writes output into a separate `deployed` repo via a sync/mirror step. The deployed repo is what the static host (Vercel/Netlify/Cloudflare Pages) reads. Hand-editing the deployed repo gets wiped on the next mirror cycle.

Find the generator by greping outside the deployed repo:

```bash
find /home/user -maxdepth 4 -type f \( -name "*.py" -o -name "*.js" \) \
     -not -path "*/node_modules/*" \
     | xargs grep -lE "signals/sig-|generate.*signal|writeFile.*signal" 2>/dev/null \
     | head -20
```

Confirm by reading the file: look for the same `<meta>` / `<script type="application/ld+json">` blocks as the deployed HTML.

### The "is_unindexable" predicate: combine state fields, not just one

A page is unindexable for any of these reasons:
- Status string is in a draft set (e.g. `NOT_PUBLISHED`, `SHADOW`, `DRAFT`).
- A separate boolean flag marks it internal (e.g. `is_shadow=True`, `internal_only=True`).
- A resolved-but-still-internal case (e.g. a paper-trade SHADOW that resolved WIN — status flipped but `is_shadow` stays True).

Compute one predicate that ORs across all these conditions, then drive every downstream decision from it:

```python
is_unindexable = is_shadow or status in ("NOT_PUBLISHED", "SHADOW")

# Robots meta tag
robots_meta = '<meta name="robots" content="noindex,nofollow">' if is_unindexable else ''

# Description leak: only append soft "skip me" signal on unindexable pages
status_suffix = f" Status: {status}." if is_unindexable else ""
```

Single-predicate-many-decisions is more maintainable than scattering `if status == "X" or is_shadow:` across the template. New unindexable conditions only need to update one line.

### F-string conditional emission in HTML templates

Python f-strings can render a tag conditionally with a ternary, without needing a separate template engine:

```python
return f"""<!DOCTYPE html>
<head>
<meta charset="UTF-8">
{'<meta name="robots" content="noindex,nofollow">' if is_unindexable else ''}
<meta name="viewport" content="width=device-width, initial-scale=1">
...
</head>"""
```

When the condition is false, the line becomes an empty line — visible in the rendered HTML but harmless. Don't worry about pretty whitespace; HTML doesn't care.

### Two-repo mirror: which repo do you commit to?

When `source-repo/tools/generator.py` writes output that's mirrored to `deployed-repo/`, **commit the fix to the source repo, not the deployed repo**. Reasons:

1. The source repo is what the bot re-runs from. Fix lands once, applies forever.
2. The deployed repo gets overwritten on every bot run anyway. Fixes there are temporary.
3. The source repo's commit history will read "feat: generator now emits X" — a meaningful narrative. The deployed repo's history is auto-generated chore commits.

If the deployed site has a critical bug right now (drafts indexable, broken canonical), you can still hand-stamp the deployed repo as a tourniquet — but pair it with the source-repo fix in the same session, and document in the task `Outcome` that the deployed-repo stamp is temporary and will be replaced by the next bot regen.

### Test the generator locally before pushing

After editing the generator, run it once to verify output before committing:

```bash
python3 tools/generate_signal_pages.py
# Then spot-check the output dir
grep -l 'name="robots"' dashboard/signals/*.html | wc -l   # expected count
grep -l 'Status:' dashboard/signals/*.html | wc -l         # expected count
comm -23 <(...) <(...) | wc -l                              # negative-space check
```

Counts should match your expected partition. Negative-space check catches false positives (e.g. noindex stamped on indexable pages). If counts don't match, the predicate is wrong — fix before committing.

### Don't commit locally-regenerated deployed-side files when the bot will redo them

After running the generator locally for verification, you'll see modifications in the output directory. Resist the urge to commit those — the bot's next scheduled run will produce identical output and commit it as a normal chore. Committing them yourself creates a noisy duplicate of work the bot will redo.

Exception: if the deployed site is broken right now and waiting for the next bot cycle is unacceptable, push the regenerated output as a tourniquet. But pair with the source-repo fix.

## Proposed Skill Content

Extend the `seo-indexing` skill with:

1. **Walk up to the generator** when hand-edits keep getting overwritten on scheduled runs. The generator may live in a separate repo (source vs deployed) — grep outside the deployed repo's filesystem path.
2. **One predicate, many decisions**: compute `is_unindexable = (state-A or state-B or ...)` once, then drive noindex emission, description-leak retention, sitemap exclusion, listing-page filter, etc. from it. Avoid scattering conditions.
3. **F-string conditional tag emission** is the simplest way to render optional `<meta>` tags from a Python template. Empty-line output is fine.
4. **Two-repo mirror policy**: source repo gets the durable fix; deployed repo only gets tourniquet stamps when something's broken right now. Pair them in the same session.
5. **Verify generator output locally before push** with positive + negative-space counts.

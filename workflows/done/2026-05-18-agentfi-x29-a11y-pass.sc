---
domain: accessibility-audit
source_task: 2026-05-18-agentfi-x29-a11y-pass.md
date: 2026-05-18
keywords: ["a11y", "aria", "aria-sort", "aria-pressed", "aria-current", "focus-visible", "skip-link", "screen-reader"]
---

## Extracted Knowledge

### ARIA-on-correct-element gotchas

**`aria-sort` belongs on `<th>`, NOT on the inner sort `<button>`.** Screen readers announce sort state when the user navigates the table grid (cell-level), not when they tab to a button. If you put `aria-sort` on the button, the screen-reader announcement is wrong.

```tsx
<th aria-sort={isActive ? "ascending" : "none"}>
  <button onClick={handleSort} aria-label="sort by mcap, currently ascending">
    MCAP
  </button>
</th>
```

**`aria-pressed` is the right attribute for toggle buttons** like filter chips. Not `aria-selected` (implies tab/listbox semantics) or `aria-current` (implies "this is the current page in a nav").

```tsx
<button
  type="button"
  aria-pressed={isActive}
  aria-label={`filter by ${label} strategy`}
>
  {label}
</button>
```

**`aria-current="page"` belongs on the breadcrumb leaf**, not on parent links. Use it with `aria-hidden` on the `/` separators (otherwise screen readers read "slash" out loud).

```tsx
<nav aria-label="breadcrumb">
  <Link href="/">terminal</Link>
  <span aria-hidden>/</span>
  <span aria-current="page">AUTONO</span>
</nav>
```

### Keyboard-accessible tooltip via `tabIndex={0}` + `group-focus-within`

A hover-only tooltip (CSS `group-hover:visible`) is inaccessible by keyboard. Make the trigger focusable AND show the tooltip on focus:

```tsx
<span className="group relative">
  <span tabIndex={0} aria-label={fullText} className="cursor-help">[i]</span>
  <span
    role="tooltip"
    className="invisible group-hover:visible group-focus-within:visible ..."
  >
    {fullText}
  </span>
</span>
```

The `aria-label` on the trigger is the fallback — screen readers announce the full text even without showing the tooltip.

### Global `:focus-visible` ring via `:where()`

Tailwind utility classes have higher specificity than naked element selectors. Use `:where()` to lower specificity so per-element overrides win without `!important`:

```css
:where(a, button, summary, [role="button"], [tabindex]):focus-visible {
  outline: 2px solid var(--signal);
  outline-offset: 2px;
}
```

Use `:focus-visible` not `:focus` — the latter triggers on mouse click which is visual noise. `:focus-visible` only triggers for keyboard nav (and form elements).

### Skip-to-content link

Must use `transform: translateY(-200%)` to hide (NOT `display: none` or `visibility: hidden` — those remove from accessibility tree). Slides in on focus:

```css
.skip-link {
  position: absolute; left: 0; top: 0; z-index: 100;
  padding: 8px 12px;
  background: var(--signal); color: var(--bg-base);
  transform: translateY(-200%);
  transition: transform 0.15s ease-out;
}
.skip-link:focus { transform: translateY(0); }
```

Skip target is the `<main id="main-content">`. Place the skip link BEFORE any sticky/persistent chrome (StatusBar, nav) so it actually skips past them.

### `.sr-only` utility

Standard pattern for visually hidden but screen-reader-announced text:

```css
.sr-only {
  position: absolute;
  width: 1px; height: 1px;
  padding: 0; margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

The `clip` rect + 1px size combo defeats screen-reader heuristics that skip purely zero-sized elements.

## Proposed Skill Content

Extends `.claude/skills/accessibility-audit.md`. Add an "ARIA on the correct element" section:
- `aria-sort` → `<th>`, not button
- `aria-pressed` for toggles, `aria-current` for nav, `aria-selected` for listbox
- Breadcrumb pattern: `aria-label="breadcrumb"`, `aria-current="page"`, `aria-hidden` on separators
- Tooltip pattern: `tabIndex={0}` + `aria-label` + `group-focus-within`
- `:where()` for low-specificity global focus styles
- Skip-link via `transform: translateY(-200%)` (not display:none)

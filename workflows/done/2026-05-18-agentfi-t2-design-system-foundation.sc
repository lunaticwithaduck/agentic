---
domain: tailwind
source_task: 2026-05-18-agentfi-t2-design-system-foundation.md
date: 2026-05-18
keywords: ["tailwind", "tailwindcss", "tailwind-v4", "design-tokens", "css-variables", "next-font"]
---

## Extracted Knowledge

### Tailwind v4 `@theme inline` replaces `tailwind.config.ts` for tokens
Tailwind v4 ships with a CSS-first design-token system. Inside `globals.css`:

```css
@import "tailwindcss";

:root {
  --bg-base: #0A0B0D;
  --signal:  #C6FF3F;
}

@theme inline {
  --color-bg-base: var(--bg-base);
  --color-signal:  var(--signal);
}
```

This generates Tailwind utilities `bg-bg-base`, `text-signal`, `border-signal`, etc. No `tailwind.config.ts` is required. The old pattern of `theme.extend.colors` in `tailwind.config.{js,ts}` still works but is no longer canonical.

### Variable name → utility name mapping is direct
The Tailwind utility class is derived **verbatim** from the CSS variable slug after the `--color-` prefix:
- `--color-bg-base`     → `bg-bg-base`, `text-bg-base`, `border-bg-base`
- `--color-ink-primary` → `bg-ink-primary`, `text-ink-primary`
- `--color-signal`      → `bg-signal`, `text-signal`

Implication: variable names are **brand-visible** in the source. Pick them as if they're public API — your developers will type them constantly. Stuttering names like `bg-bg-base` happen when the category prefix matches; either accept it or rename the variable group.

### Other `@theme` token categories
The `--color-` prefix is one of several. Tailwind v4 recognizes:
- `--color-*` → color utilities (`bg-`, `text-`, `border-`, `from-`, etc.)
- `--font-*` → `font-*` utilities (`font-mono`, `font-sans`, `font-serif`)
- `--text-*` → `text-*` font-size utilities
- `--spacing-*` → spacing scale
- `--breakpoint-*` → custom breakpoints
- `--radius-*` → border-radius
- `--shadow-*` → shadow utilities

For custom non-Tailwind sizes (e.g. a one-off `88px` display number) you can declare arbitrary `--text-display-xl` and reference via `text-[var(--text-display-xl)]` or `text-display-xl` if you use `inline` keyword.

### `next/font/google` composition with Tailwind v4
Pattern to use 3+ Google fonts as CSS variables on `<html>`:

```tsx
import { Geist, JetBrains_Mono, Fraunces } from "next/font/google";

const geist = Geist({ variable: "--font-geist-sans", subsets: ["latin"], display: "swap" });
const mono  = JetBrains_Mono({ variable: "--font-jetbrains-mono", subsets: ["latin"], display: "swap" });
const serif = Fraunces({ variable: "--font-fraunces", subsets: ["latin"], display: "swap", axes: ["opsz"] });

<html className={`${geist.variable} ${mono.variable} ${serif.variable}`}>
```

Then alias them as semantic names in `@theme inline`:

```css
@theme inline {
  --font-sans:  var(--font-geist-sans);
  --font-mono:  var(--font-jetbrains-mono);
  --font-serif: var(--font-fraunces);
}
```

Now `font-sans`, `font-mono`, `font-serif` utilities all work. The intermediate CSS variable indirection means swapping fonts (e.g. JetBrains Mono → Berkeley Mono once licensed) is one-line at the font-loader call, no consumer changes.

### `axes` parameter for variable fonts
Some variable Google fonts expose custom axes (e.g. Fraunces has `opsz`, `SOFT`, `WONK`). `next/font/google` only accepts axes it recognizes — passing an unknown axis errors. Stick to standard axes (`opsz`, `wght`, `wdth`, `slnt`, `ital`) unless you've verified the font ships the custom one and Next supports it.

### Satori-safe typed underline pattern
For a "hand-drawn" underline beneath text (terminal/typewriter aesthetic):

```tsx
const TEXT = "agentfi.terminal";
const UNDERLINE = "▔".repeat(TEXT.length);  // U+2594 UPPER ONE EIGHTH BLOCK

<span className="inline-flex flex-col leading-none font-mono">
  <span>{TEXT}</span>
  <span aria-hidden className="-mt-[10px] text-ink-tertiary">{UNDERLINE}</span>
</span>
```

Why not CSS `border-bottom`?
- `border-bottom` renders at a different baseline than typed characters, so it doesn't visually rhyme with other typed elements.
- Satori (the renderer behind Vercel's `ImageResponse` / OG cards) supports basic CSS but border alignment can drift between on-page and OG renders.
- The Unicode-character approach renders identically in both because both use the same font glyph.

`-mt-[Npx]` value tunes how tight the underline sits to the descender; eyeball it per font.

## Proposed Skill Content

A future `.claude/skills/tailwind.md` would cover:

**Section: Tailwind v4 — what changed**
- `@import "tailwindcss"` replaces the `@tailwind base/components/utilities` triplet
- `@theme inline { --color-X: var(--X) }` is the canonical place to register design tokens
- No `tailwind.config.ts` needed for tokens; only needed for plugins or custom content globs
- Variable-name → utility-name mapping is verbatim (after the category prefix)

**Section: Token categories**
- Full list: `--color-*`, `--font-*`, `--text-*`, `--spacing-*`, `--breakpoint-*`, `--radius-*`, `--shadow-*`
- Naming: variable slugs are brand-visible in source code; pick deliberately

**Section: Font composition pattern**
- next/font/google loader → `variable: '--font-x'` → `<html className={font.variable}>` → `@theme inline` alias → Tailwind utility
- Indirection layer lets you swap fonts at the loader call without touching consumers

**Section: Tailwind in OG / satori contexts**
- Satori supports a subset of CSS; design components to that subset from day 1 if they'll render via `ImageResponse`
- Prefer Unicode-glyph decorations over `border-*` when they need to match across on-page + OG renders

(No Failure Modes section yet — first tailwind `.sc`.)

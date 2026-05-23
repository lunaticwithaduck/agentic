---
title: AgentFi T2 — Design system foundation (palette, fonts, layout chrome)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Translate the TERMINAL-NOIR design doc into the codebase: CSS variables for the full palette, a free-tier font stack (JetBrains Mono Variable / Geist / Fraunces), a tuned Tailwind type scale, and the base layout shell — sticky status bar, `agentfi.terminal` wordmark with hand-drawn underline, faint grid texture, footer. After this task every page inherits the right canvas, ink, and chrome.

## Steps
- [x] Add palette CSS variables in `app/globals.css` — full palette per design doc §04
- [x] Token registration via Tailwind v4 `@theme inline` block (no `tailwind.config.ts` needed)
- [x] Load fonts via `next/font/google`: Geist (`--font-geist-sans`), JetBrains_Mono (`--font-jetbrains-mono`), Fraunces (`--font-fraunces`) — all variable, `display: swap`, `axes: ['opsz']` on Fraunces
- [x] Replace stock `app/layout.tsx`: dark-only, mono body, 1280px max-width content, noise overlay utility on `<main>`
- [x] `components/StatusBar.tsx` — client component, sticky 32px, wordmark + UTC clock (1s tick) + BASE block + ●LIVE pulse
- [x] `components/Wordmark.tsx` — `full` variant with `▔` (U+2594) underline + cursor; `inline` variant for status bar
- [x] `components/Footer.tsx` — methodology + github + last snapshot timestamp
- [x] `app/page.tsx` — placeholder "SCAFFOLD READY" body + 6 swatch cards for visual palette check
- [~] Browser eyeball at 1440px / 1280px — **deferred to user** (`pnpm dev` blocked by harness)

## Font decision (locked)
Paid faces (Berkeley Mono / Söhne / Tiempos) are deferred — JetBrains Mono Variable / Geist / Fraunces are the launch stack. Wordmark `-0.03em` tracking values are tuned for Berkeley; T2 uses `-0.02em` which reads cleaner with JetBrains' wider apertures.

## Verification
`pnpm build` (Next.js 16.2.6 + Turbopack): **PASS** — compiled in 2.5s, TypeScript clean in 2.0s, 4 static pages generated. No runtime errors expected; design system tokens resolve through `@theme inline` to Tailwind utilities (`bg-bg-base`, `text-ink-primary`, `text-signal`, etc.).

## Outcome
Design system foundation is in. Three things worth recording:

1. **Tailwind v4 `@theme inline` pattern works cleanly with `next/font/google` CSS variables.** Pattern: font loader exposes `variable: '--font-x'` → spread onto `<html className={font.variable}>` → reference inside `@theme inline { --font-mono: var(--font-x) }`. Tailwind then generates `font-mono` utility. No `tailwind.config.ts` needed.

2. **Color naming carries through verbatim.** `--color-bg-base` in `@theme inline` → `bg-bg-base` utility class. Tailwind v4 derives utility names directly from the `--color-X` slug. Means CSS variable names are *brand-visible* and worth picking carefully.

3. **The "hand-drawn underline" trick** uses Unicode `▔` (U+2594 UPPER ONE EIGHTH BLOCK) repeated `TEXT.length` times, rendered in same font/size as the text above, with a `-mt-[Npx]` negative margin to tuck it under. This avoids CSS `border-bottom` (which would render at a different baseline than the typed underline) and survives `ImageResponse`/satori rendering — crucial for T6's OG cards.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies/frameworks touched: Tailwind v4, next/font/google, Next.js 16 root layout
- Domain-specific knowledge: Tailwind v4's `@theme inline` directive replaces `tailwind.config.ts` token extension; CSS variable slug → utility class name mapping is direct (`--color-X` → `bg-X` / `text-X`); next/font multi-font composition via CSS variables; Unicode `▔` underline trick for satori-safe typed underlines
- Verdict: **GENERATE**
- Reason: Tailwind v4's `@theme inline` syntax is brand-new and not in pre-2026 training data; the variable→utility name derivation is non-obvious. Worth a `tailwind` domain.
- Domain: `tailwind` (new — no existing .sc covers Tailwind)

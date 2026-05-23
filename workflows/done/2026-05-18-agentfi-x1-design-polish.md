---
title: AgentFi X1 — Design polish (favicon, 404 page, status-bar typewriter)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Three small-but-load-bearing first-impression upgrades the design doc specifies but the scaffold left unbuilt: a `▌` favicon (replacing the default Next.js icon), a terminal-themed `not-found` page (replacing Next's stock 404), and the typewriter reveal on the StatusBar's first mount per design doc §10.

## Steps
- [x] `app/icon.tsx` — ImageResponse-rendered 32×32 favicon, `▌` glyph in `--signal` on `--bg-base`, satori default font (NOT custom JetBrains Mono — see Outcome). `runtime = 'nodejs'`.
- [x] `app/not-found.tsx` — terminal-noir 404: "compute_val not found · agent dropped offline · 0xDEAD…BEEF" + last-snapshot/stale-for info + `$ cd /terminal` link back home with blinking cursor.
- [x] Pure-CSS typewriter via `@keyframes typewriter-wipe` + `clip-path: inset(...)` on the StatusBar content. Respects `prefers-reduced-motion`. Runs once on root-layout mount (which only mounts on full page load since the layout is sticky across client-side navigations).
- [x] **Bonus fix surfaced during build**: `middleware.ts` → `proxy.ts` per Next.js 16 deprecation (function renamed `middleware` → `proxy`, file moved, config block unchanged).
- [x] **Bonus fix surfaced during build**: added `metadataBase: new URL(...)` to root layout metadata (kills the `metadataBase not set` warning + makes OG image URLs resolve correctly in social composers).
- [x] Verify build + tests still pass.

## Verification
- `pnpm build`: PASS — `/icon` is `○` (static), `/_not-found` registered, route table now 14 entries.
- `pnpm test`: 5 files, **38 tests**, all pass — no regressions.

## Outcome
Three intended ships + two breaking-change discoveries:

1. **`app/icon.tsx` cannot carry a custom variable font at build-time.** First attempt loaded JetBrains Mono Variable with `weight: 700` like the OG routes do. Build crashed: `TypeError: Cannot read properties of undefined (reading '256')` inside satori's static-gen pass. The fix was to drop the font entirely and rely on satori's default fallback — the `▌` glyph (U+258C) renders fine in it. **Rule:** for simple-glyph icons, prefer no-font-load. For OG cards with mixed-content layouts, fonts are still fine because OG routes are dynamic (`ƒ`), not static-gen.

2. **`middleware.ts` is deprecated in Next.js 16 — renamed to `proxy.ts`.** Surfaced as a build-time warning (not an error — middleware.ts still works). Migration: file `middleware.ts` → `proxy.ts`, exported function `middleware` → `proxy`, `config.matcher` unchanged. Build output legend changed from "Middleware" to "Proxy (Middleware)" to signal the transition.

3. **`metadataBase` should always be set on the root layout's metadata export.** Otherwise Next falls back to `http://localhost:3000` for resolving `og:image` and `twitter:image` URLs — which means production OG previews will reference `localhost:3000/og/...` URLs that don't resolve. Fix:
   ```tsx
   metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
   ```

4. **Pure-CSS typewriter is the right call vs. JS-state typewriter.** Initial design used `useState` + `setInterval` to advance a `revealedCount`. Switching to `clip-path: inset(0 100% 0 0) → inset(0 0 0 0)` with `steps(40, end)` over 400ms eliminated all hydration/SSR concerns (no flash, no mismatch warnings) and added zero JS to the bundle. The animation only fires on hard page load because the root layout doesn't remount on client-side navigation.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: Next.js 16 (`app/icon.tsx`, proxy convention, `metadataBase`), pure-CSS animation patterns
- Domain knowledge: variable-font crash at static-gen; middleware→proxy rename; metadataBase fallback gotcha; CSS-only animation avoiding SSR mismatch
- Verdict: **GENERATE**
- Reason: Three real Next.js 16 gotchas confirmed by this task — all worth recording.
- Domain: `nextjs` (extends existing — **3rd entry, hits synthesis threshold of 3**)
- **Auto-generated skill `.claude/skills/nextjs.md` in this same response.**

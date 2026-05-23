---
title: AgentFi X32 — Error boundaries (route + global)
created: 2026-05-20
status: done
completed: 2026-05-20
---

## Goal
Add Next.js error boundaries so snapshot-store throws / cron failures / chain-fetch failures render a terminal-themed fallback UI instead of the raw Next error page.

## Files
- `app/global-error.tsx` — top-level fallback for layout/root crashes
- `app/error.tsx` — landing-page fallback
- `app/agent/[slug]/error.tsx` — agent-page-specific
- `app/comp/error.tsx` — comp table fallback
- `app/status/error.tsx` — status page fallback
- `components/ErrorPanel.tsx` — shared client component used by all route-level boundaries

## Steps
- [x] `global-error.tsx` with inline-styled fallback (must render own html/body since root layout has failed)
- [x] Shared `ErrorPanel` client component with title/subtitle/digest/error.message details + retry button
- [x] Route-level `error.tsx` files for /, /agent/[slug], /comp, /status — each calls ErrorPanel with route-specific copy
- [x] All are Client Components ("use client") with `error: Error & { digest?: string }` + `reset: () => void` props per Next 16 contract
- [x] Build + tests + Playwright green (42/42)

## Outcome

Completed on 2026-05-20. Five error boundaries shipped: one top-level (global-error.tsx for root layout crashes) + four route-scoped boundaries that share `ErrorPanel`. Each renders a terminal-themed fallback with route-specific subtitle, optional digest, expandable error message, and a retry button that calls Next's `reset()`.

Key Next 16 contract notes worth remembering:
- `global-error.tsx` MUST render its own `<html><body>` because by the time it's invoked, the root layout has already failed
- Route-level `error.tsx` files MUST be Client Components — the `reset` function is a client-side callback
- `error.digest` is auto-populated by Next when an error originates server-side; safe to show users

**Skill candidate evaluation:**
- Technologies/frameworks touched: Next 16 App Router error boundary file conventions
- Domain-specific knowledge: (a) `global-error.tsx` is structurally different — must wrap in `<html><body>` because the layout it normally lives inside has failed; (b) the `reset` callback re-renders the boundary's subtree, NOT the whole app; (c) `error.digest` is Next's server-side error fingerprint, safe to surface; (d) one `ErrorPanel` client component imported by N route-level `error.tsx` files is cleaner than N copies of the same JSX.
- Verdict: GENERATE
- Reason: Next 16 error boundary conventions (global vs route, html-wrapping requirement, client component requirement) are non-obvious and a common source of "why doesn't this work" frustration.

## Completion
Run `/complete workflows/tasks/2026-05-20-agentfi-x32-error-boundaries.md`.

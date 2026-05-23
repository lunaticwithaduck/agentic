---
title: AgentFi T1 — Bootstrap Next.js project at /home/jojo/agentfi-terminal
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Stand up an empty but runnable Next.js 16 (App Router, TypeScript) project at `/home/jojo/agentfi-terminal/` with Tailwind v4 initialized. Local-only — no Vercel link, no env vars, no external API keys yet. After this task `pnpm dev` serves a stock landing page.

## Steps
- [x] Create `/home/jojo/agentfi-terminal/` directory
- [x] Run `pnpm create next-app@latest` with App Router + TS + Tailwind + ESLint + import-alias `@/*`
- [x] Confirm Next.js version installed — got **16.2.6** (React 19.2.4, Tailwind 4.3, TypeScript 5.9.3)
- [~] Initialize `shadcn` CLI — **DEFERRED to T5** (see Outcome)
- [x] Add `.nvmrc` pinning Node 20
- [x] Replace stock `README.md` with project-specific one
- [~] `pnpm dev` boot check — **deferred to user** (harness correctly blocks long-running servers; `create-next-app` succeeded + types generated, so structure is valid)
- [x] `git init` — done; **first commit deferred to user** (global rule: commits only on explicit ask)

## Notes / decisions to lock here
- Package manager: pnpm (consistent with the plan doc)
- App Router only — no pages dir
- Do NOT install Alchemy / Neynar / X API / Viem yet — those land with their respective wiring tasks

## Outcome
Scaffold created cleanly at `/home/jojo/agentfi-terminal/` on Next.js 16.2.6. Two scope adjustments worth recording:

1. **shadcn init deferred from T1 → T5.** `shadcn init` mutates `globals.css` and seeds `components/ui/`. T2 (next) heavily rewrites `globals.css` with the TERMINAL-NOIR palette, fonts, and grid texture. Running shadcn before T2 creates a merge conflict to resolve for no gain — none of T2/T3/T4's components need shadcn primitives (they're all custom for satori-safety). T5 is where `/admin` tables, `/comp` sort headers, and `/methodology` tooltips first actually use shadcn primitives — install there.

2. **Important Next.js 16 caveat surfaced.** Auto-generated `AGENTS.md` (which `CLAUDE.md` @-includes) warns: *"This is NOT the Next.js you know. This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code."* Docs tree is at `node_modules/next/dist/docs/01-app/{01-getting-started,02-guides,03-api-reference}`. Subsequent tasks (T2 layout, T5 routing, T6 OG `ImageResponse` edge runtime) must consult this before writing code — do not lean on training-data Next.js patterns.

3. **Deferred for user approval:** `pnpm dev` smoke test + initial git commit. Both are one-line follow-ups once user signs off on scaffold.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies/frameworks touched in this task: Next.js 16, pnpm, Tailwind v4, create-next-app
- Domain-specific knowledge involved: Next.js 16 ships an in-tree `AGENTS.md` warning that APIs differ from training-data assumptions; full docs tree is bundled at `node_modules/next/dist/docs/`; create-next-app supports full non-interactive scaffolding via combined flags; shadcn init should be ordered AFTER any globals.css customization to avoid merge conflicts
- Verdict: **GENERATE**
- Reason: Next.js 16's in-tree docs convention and the shadcn-init ordering are both non-obvious gotchas specific to current Next.js + shadcn versions that future tasks in this project (and any new Next.js 16 project) will benefit from
- Domain: `nextjs` (new — no existing .sc covers Next.js)

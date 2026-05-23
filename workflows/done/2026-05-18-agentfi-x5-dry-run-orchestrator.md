---
title: AgentFi X5 — Dry-run orchestrator + /admin/dry-run view
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Tie X3 (templates) + X4 (materiality) together: an orchestrator that scans recent actions, filters by materiality, picks a template, renders the caption, builds the OG URL, and emits a list of *intended* posts. Dry-run only — no Neynar, no X API calls. Surfaces output via `/admin/dry-run` and a `pnpm dry-run-poster` CLI.

## Steps
- [x] `lib/poster/orchestrator.ts` — `processActions(input) → IntendedPost[]`. Both surfaces (farcaster + x) by default. Per-surface daily cap counter. OG URL via `ogBaseUrl` override or `NEXT_PUBLIC_SITE_URL` env or localhost default.
- [x] Skipped entries kept in the output array with their materiality reason — admin view can show *why*.
- [x] `app/admin/dry-run/page.tsx` — intentionally-uglier table (system-ui white) inside the dark canvas. Behind `proxy.ts` basic-auth. 4-stat summary header + per-row table with template + caption + length + warnings.
- [x] `scripts/dry-run-poster.ts` — CLI that loads mock snapshots, runs the orchestrator, prints intended + skipped to stdout
- [x] Installed `tsx` as devDep, added `"dry-run-poster": "tsx scripts/dry-run-poster.ts"` to package.json
- [x] 6 vitest tests: per-surface fan-out, caption-only-when-material, reason propagation, daily-cap-fires-at-exactly-5, ogBaseUrl override, empty-actions
- [x] Verify build + tests + CLI all work

## Verification
- `pnpm test`: 9 files, **77 tests**, all pass
- `pnpm build`: PASS — new route `/admin/dry-run` registered
- `pnpm dry-run-poster`: prints 10 intended posts (AUTONO ×3 × 2 surfaces + ETHY SWAP × 2 surfaces) + 4 skipped (BANKR + AETHER SWAPs blocked by daily-cap-reached). Templates substitute correctly. OG URLs resolve. Warnings would surface for over-cap captions.

Sample output:
```
[x        ] AUTONO · MILESTONE · milestone_v1
    ⚡ AUTONO build_rate ▲ 30% · 199× · https://agentfi.dev/agent/autono
    og: http://localhost:3000/og/action/0x12be43...
```

## Outcome
Publishing engine's load-bearing piece is in. Four notes:

1. **Per-surface counters, not global.** The cap is "5 per surface per day" — so farcaster and x have independent counters. The orchestrator gets this right; the test verifies it. Easy to mis-design as a single counter shared across surfaces, which would halve the daily volume.

2. **CLI = `pnpm dry-run-poster` works end-to-end** because tsx handles the `@/` path aliases at runtime. Confirmed by running the script and seeing the templated output. Saves boot-the-dev-server time when iterating on templates or materiality thresholds.

3. **OG URL resolution order**: explicit `ogBaseUrl` override → `process.env.NEXT_PUBLIC_SITE_URL` → `http://localhost:3000`. The same hierarchy as the metadataBase fix in X1. Future env-var management (e.g. a `.env.example`) should document both.

4. **Skipped reasons are visible everywhere** — the same reason string appears in `IntendedPost.decision.reason`, in the admin table, and in the CLI output. Debugging "why didn't this post" is one read, not three different log surfaces.

## Out of scope (left for later, intentional)
- Real Neynar / X API calls (these are the v1 "flip the switch" step)
- Per-tx dedupe via `actions.posted_at` (needs DB)
- Cron scheduling at 15min intervals (needs `vercel.json` + cron config)
- A/B selection between template versions (only `_v1` shipped)

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: pure-TS rule engine, tsx CLI scripts, side-effect-free orchestrator pattern
- Domain knowledge: project-specific orchestrator wiring (templates + materiality + OG URL construction)
- Verdict: **SKIP**
- Reason: The orchestrator is project-specific business glue. The general "build dry-run before live" pattern is engineering hygiene, not technology-specific knowledge worth a skill file.

---
title: AgentFi X30 — Implement Claude Design wireframes across all surfaces
created: 2026-05-20
status: done
completed: 2026-05-20
---

## Goal
Upgrade production design across 6 surfaces using the Claude Design wireframes bundle. User explicitly chose "Push hard — show me unexpected layouts" — implement design ideas from both A (clinical) and B (brutalist) variants where they're load-bearing.

## Source bundle
`https://api.anthropic.com/v1/design/h/EOOq-PLSxwAkJD_iySMJPA` — gzipped tar with `README.md`, chat transcript, `wf-pages.jsx`/`wf-pages2.jsx`/`wf-shared.jsx`, `styles.css`, `index.html` (12 wireframe artboards across 6 surfaces with A/B per surface).

## Steps
- [x] Marquee ticker component (`components/Ticker.tsx`) — one-line summary of all agents, top of landing
- [x] Brutalist hero on landing — 120–280px clamp lime number, agent rail (4 rows with sparkline + delta on the right), "what is a compute multiple?" card + comp teaser below
- [x] 6-cell metrics strip on `/agent/[slug]` (mcap, compute_val, treasury, staked DIEM, holders, actions/24h) — MetricsStrip now accepts `valueText` + `hint` for non-USD values
- [x] Time-range tabs (visual only) on HistoryChart — `1h 24h 7d 30d all` with `all` active
- [x] TOC rail on `/methodology` — sticky 2-col aside with 6 anchors + version tag, scroll-mt-24 on all sections for clean jump targets
- [x] 4-tile system-check grid on `/status` (indexer / etherscan / snapshotter / satori og) with live/down badges
- [x] 3-up performance sparklines on `/status` (block lag / p50 query / snap interval — placeholder data)
- [x] Updated `/status` test to match new system-check labels (was looking for old "snapshot store" / "etherscan key" text)
- [x] +6 new Playwright cases (marquee ticker, brutalist hero, 6-cell strip, methodology TOC, status tiles, status perf section)
- [x] Regenerated visual baseline for `/agent/autono` live screenshot
- [x] Build + test + Playwright green (42/42 Playwright, 168/168 vitest)

## Decisions
- Did NOT swap CompTable for scatter (B variant) — the X22 sort + X27 filter table is the canonical surface; scatter would be a separate `?view=scatter` view-mode and that's scope creep beyond this design implementation
- Did NOT implement /agent live-tape (B variant) — current AgentCard + history charts is closer to brief; live-tape is interesting but the chat showed user landed on `A standard` for the agent page
- Did NOT swap OG card for brutalist single-number (B variant) — current AgentCard renders both on-page AND OG from one component; that property is more valuable than the brutalist treatment
- Time-range tabs on HistoryChart are visual-only (no data wiring) — current store only has hourly snapshots; wiring real ranges is a future task when telemetry lands
- Performance sparklines on /status are placeholder — labeled "placeholder — telemetry lands post-deploy" so users aren't misled
- Status of `/og/agent/[slug]` left unchanged — already polished from prior tasks

## Outcome

Completed on 2026-05-20. Pulled the load-bearing design ideas from the wireframe bundle and implemented them in the production app. The terminal-noir aesthetic stays exactly the same (same palette, same JetBrains Mono everywhere, same dot-grid + noise overlay); what changed is **layout density and composition** on each surface.

**Landing** is the most dramatic transformation — went from a vertical stack of sections (wordmark → featured AgentCard → comp teaser) to a brutalist marquee + oversized hero number + agent-strip rail layout. The 280px lime "199×" hero number is now the first thing readers see after the ticker.

**Agent page** got functional density: 6-cell metrics strip with mcap/compute_val/treasury/staked DIEM/holders/actions/24h. Some values are placeholders ("indexer pending") but the structure is in place for when telemetry lands.

**Methodology** got a proper docs structure with a sticky TOC rail, version tag, and anchored sections. Reads more like a v1.0 protocol doc than a marketing page.

**Status** got the 4-tile system-check grid + 3-up perf sparklines — looks like a real ops dashboard instead of a list of bullets.

**Skill candidate evaluation:**
- Technologies/frameworks touched: React Server Components composition (Ticker is async server component), Tailwind grid layouts (`lg:col-span-8` brutalist hero), `clamp()` for responsive display type (`clamp(120px, 22vw, 280px)`), `scroll-mt-24` for anchor offset under sticky headers
- Domain-specific knowledge: (a) `clamp(min, fluid, max)` is the right CSS for hero typography that scales but doesn't run away on ultrawide; (b) `scroll-mt-N` on anchor targets prevents them from disappearing under a sticky header — without it, jumping to `#section` puts the heading at the very top of viewport, hidden by sticky chrome; (c) when an existing component (MetricsStrip) needs to grow new value types, prefer adding optional props (`valueText`, `hint`) over forking — keeps the API one type; (d) wireframe-to-production translation: NEVER copy the prototype's internal structure (dashed boxes, hand-written callouts) — extract design *ideas* (composition, density, hierarchy) and re-implement in the target tech.
- Verdict: GENERATE
- Reason: The "wireframe to production" translation rule + `scroll-mt` anchor offset gotcha + clamp-for-hero-type pattern are non-obvious enough to encode.

## Completion
Run `/complete workflows/tasks/2026-05-20-agentfi-x30-design-implementation.md`.

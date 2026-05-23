---
title: AgentFi X7 — Playwright visual snapshot pinning
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Lock the current visual state of the AgentCard (live + OG) so any future PR that drifts the rendering fails loudly. Adds `toHaveScreenshot()` + `toMatchSnapshot()` baselines for the agent page + the OG PNG.

## Steps
- [x] Pinned OG `humanizeAgo` time source to `MOCK_NOW_ISO` (was `Date.now()`) so OG byte-snapshots are stable. Comment in code points to where this needs to change when real data lands.
- [x] `tests/snapshots.spec.ts` — two cases: OG PNG byte-comparison via `toMatchSnapshot('og-agent-autono.png')`, live-page screenshot of `<main>` via `toHaveScreenshot('agent-autono-live.png', { animations: 'disabled', caret: 'hide', maxDiffPixelRatio: 0.02 })`.
- [x] Generated baselines with `pnpm exec playwright test snapshots.spec.ts --update-snapshots`. Output: `tests/snapshots.spec.ts-snapshots/og-agent-autono-chromium-linux.png` (49KB) + `agent-autono-live-chromium-linux.png` (80KB).
- [x] Verified `.gitignore` excludes only transient `playwright-report/`, `test-results/`, `.playwright/` — baselines under `tests/.../snapshots/` are tracked (committable).
- [x] Re-ran `pnpm exec playwright test` — **17/17 passing** in 10.1s (15 smoke + 2 snapshot).
- [x] vitest unchanged: 77/77.
- [x] Added a one-paragraph note to `.claude/skills/satori.md` Playwright section covering the time-pinning + animation-disabling pattern.

## Update workflow
When the visual deliberately changes (new field, layout tweak):
```bash
pnpm exec playwright test snapshots.spec.ts --update-snapshots
git add tests/snapshots.spec.ts-snapshots/*.png
git commit -m "snapshot: bump baselines for <reason>"
```
Without `--update-snapshots`, snapshot tests fail loudly with a side-by-side diff in `test-results/`.

## Verification
- `pnpm test`: **77/77**
- `pnpm exec playwright test`: **17/17 in 10.1s**

## Outcome
Visual baselines pinned. AgentCard live page + OG PNG both covered.

Two notes:
1. **MOCK_NOW_ISO timestamp pinning** is a scaffolding-era trick — when real chain data lands, OG renders will need a snapshot-time-of-render from the indexer (not `Date.now()`) to keep snapshots deterministic. Marked with a code comment.
2. **Cross-machine fragility:** baselines are tagged `-chromium-linux` so they're machine-specific. If we ever run snapshots in CI on a different OS, we'll need a separate baseline set OR a Docker-based test runner that matches local. Known follow-up; documented as a constraint in the spec file's header comment.

## .sc — Skill candidate evaluation
- Technologies touched: Playwright Test (`toHaveScreenshot`, `toMatchSnapshot`, animation control)
- Domain knowledge: time-pinning for deterministic OG byte-snapshots; `animations: 'disabled'` + `caret: 'hide'` for stable live screenshots; baselines are machine-tagged
- Verdict: **SKIP** (incremental)
- Reason: The deterministic-time + animation-disable patterns are useful but were folded into the existing `.claude/skills/satori.md` directly. A standalone `.sc` would duplicate that knowledge. No new domain warranted.

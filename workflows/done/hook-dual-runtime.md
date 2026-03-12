---
title: Hook dual-runtime maintenance burden
status: solved
opened: 2026-03-12
closed: 2026-03-12
---

## Problem

Each hook now ships in two forms: a `.sh` (bash + python3) version and a `.js` (Node.js) version.
`settings.json` runs the `.js` files. The `.sh` files are effectively dead code.

Any bug fix or feature change to hook logic must be applied in two places, or the files drift.

## Options

1. **Delete `.sh` files** — go all-in on `.js`. Node is guaranteed by Claude Code; bash/python3 are not.
   Zero duplication, no maintenance burden. Downside: loses the bash reference.

2. **Make `.sh` files thin shims** — each `.sh` becomes a one-liner (`node "$(dirname "$0")/skill-detector.js"`).
   Logic lives only in `.js`. `.sh` shims let people call hooks manually with bash if they prefer.

## Recommendation

Option 1 (delete `.sh`) is cleanest once we're confident JS parity is solid.
Option 2 (shims) is a good middle ground if we want to keep the bash entry points.

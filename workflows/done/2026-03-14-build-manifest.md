---
title: Build manifest — ship/.build-manifest.json
created: 2026-03-14
completed: 2026-03-14
status: done
---

## Goal

Write `ship/.build-manifest.json` at the end of every `buildScripts/build.sh` run,
capturing git state, version, platform file counts, and the latest bench run timestamp.

## Steps

- [x] Add manifest generation to build.sh (after stamp version step)
- [x] Verify output is valid JSON with all expected fields

## Outcome

Completed on 2026-03-14. Added ~20 lines to the end of `buildScripts/build.sh` after the
version stamp step. Captures: `built_at` (UTC ISO timestamp), `git_sha`, `git_branch`,
`git_dirty` (boolean via `git status --porcelain | grep -q .`), `version`, `platforms` array,
`file_counts` per platform (reuses CC_COUNT/CUR_COUNT/COP_COUNT already in scope), and
`bench_run` (basename of latest metrics JSON in `bench/results/metrics/`). Written to
`ship/.build-manifest.json` which travels with the ship artifact. Verified: valid JSON,
`git_dirty: true` correctly reflects uncommitted changes on first run.

---
title: benchmark with-infra run uses isolated .claude/ temp dir instead of project root
created: 2026-02-28
promoted: 2026-02-28
completed: 2026-02-28
status: done
author: user
tags: [bench, e2e, compare]
priority: high
complexity: medium
---

# benchmark with-infra run uses isolated .claude/ temp dir instead of project root

## Description

Currently `run_with_infra_subprocess` in `bench/e2e/compare.py` runs the with-infra
claude -p call in `ROOT_DIR` (the agentic project root). This means the run loads the
agentic project's own CLAUDE.md, which describes the infrastructure itself — not a
typical user project. This is noise for generic coding tasks and makes the benchmark
self-referential rather than measuring reusable infra value.

The fix: copy only `.claude/` into a second temp directory and run the with-infra call
there. This tests the reusable parts of the infrastructure (skills, hooks, settings,
agents) without contaminating the context with agentic-project-specific instructions.

## Notes

- without-infra: empty temp dir (no .claude/, no hooks) — unchanged
- with-infra: temp dir + `.claude/` copied in — tests the portable infrastructure
- Optionally add a minimal generic CLAUDE.md to the temp dir to simulate a real install

## Acceptance Criteria

- [x] `run_with_infra_subprocess` copies `.claude/` into a fresh temp dir instead of using ROOT_DIR
- [x] The with-infra temp dir does NOT contain the agentic project's CLAUDE.md or any other root files
- [x] Both runs (with-infra and without-infra) still produce valid cached responses
- [x] Existing cache keys (`with-infra-sub`, `without-infra-sub`) remain compatible or are versioned
- [x] `bench/run.sh --suite=04` completes without errors after the change
- [x] Old cached results are cleared so the next run generates fresh comparisons from the correct setup

## Outcome

Completed on 2026-02-28. Changed `run_with_infra_subprocess` in `bench/e2e/compare.py`
to copy `.claude/` into a fresh `tempfile.TemporaryDirectory` instead of running in
`ROOT_DIR`. The with-infra run now gets skills, hooks, settings, and agents — but not
the agentic project's own CLAUDE.md. Cleared all `with-infra-sub.txt` and
`judgement-sub.json` cache files so the next bench run generates fresh, correctly
isolated comparisons.

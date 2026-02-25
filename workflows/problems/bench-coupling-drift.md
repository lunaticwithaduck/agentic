# Problem: Bench Coupling Drift

**Date identified:** 2026-02-26
**Status:** Solved — see `workflows/done/bench-claude-md-guardrail.md`

---

## The Problem

As the `bench/` directory grew from a simple script into a multi-file system (~15 files, 5 suites, an E2E runner, a pretty reporter, a changelog, and a cleanup script), changes to one file started silently breaking others.

The bench has several tight coupling points that have no automated enforcement:

1. **metrics.sh JSON schema ↔ report.py** — `report.py` reads specific keys from the metrics JSON by name. Rename a key in `metrics.sh` and `report.py` silently shows wrong data or crashes.

2. **compare.py output ↔ 04-task-quality.sh** — The suite parses `infra_wins`, `vanilla_wins`, `infra_win_rate`, etc. from compare.py's `--json` output. Rename any field in compare.py and the suite breaks silently (it just reads wrong values).

3. **compare.py cache naming ↔ cleanup.sh globs** — `cleanup.sh` uses glob patterns (`with-infra*.txt`, `judgement*.json`) matching the cache file names compare.py writes. Rename a cache file and cleanup stops working with no error.

4. **Suite file names ↔ report.py SUITE_LABELS** — `report.py` has a hardcoded dict mapping suite names to display labels. Add a new suite without adding its label, and it shows up with its raw filename in the pretty report.

5. **tasks.json expected_skills ↔ skill library** — `tasks.json` references skill names in `expected_skills`. Delete a skill and these references become stale, causing fixture mismatches.

6. **block-secrets.sh contract ↔ 03-hook-security.sh** — Suite 03 relies on a specific exit-code and stdout format from `block-secrets.sh`. Change the contract and the suite fails or gives misleading results.

## Why It Matters

These aren't bugs that cause immediate test failures — they're **drift bugs**: changes that appear to work locally but silently invalidate the measurement. A bench that measures the wrong thing is worse than no bench.

The problem compounds across sessions: Claude Code picks up where it left off with no memory of what was changed, so it can easily modify one end of a coupling without realizing the other end also needs updating.

## Root Cause

No authoritative mapping existed of "if you change X, update Y". The coupling was implicit in the code, spread across 15 files, and only discoverable by reading them all.

## Solution

→ See `workflows/done/bench-claude-md-guardrail.md`

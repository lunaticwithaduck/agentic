---
title: Suite 06 token cost measurement
created: 2026-02-28
promoted: 2026-02-28
completed: 2026-02-28
status: done
author: user
tags: [bench, suite-06, tokens, cost]
priority: high
complexity: medium
---

# Suite 06 token cost measurement

## Description

New benchmark suite measuring token overhead of the agentic infrastructure.
Runs tasks in API mode for both conditions (with-infra vs without-infra) and
captures input/output token counts from the Anthropic API usage field.
Reports overhead (extra input tokens from skill loading + CLAUDE.md),
output token delta, and total cost ratio.

## Notes

- Requires API mode (subprocess doesn't expose token counts)
- Needs ANTHROPIC_API_KEY
- api_call() in compare.py already calls the API — token_compare.py extends it to capture usage{}
- Suite 06 measures cost; correlation with suite 04 quality scores answers worth-it

## Acceptance Criteria

- [x] bench/e2e/token_compare.py created — runs API mode, captures input/output tokens per condition
- [x] bench/suites/06-token-cost.sh created — calls token_compare.py, reports metrics, has pass/fail thresholds
- [x] Metrics reported: input overhead (tokens), output delta (tokens), cost ratio (with/without)
- [x] Suite skips gracefully when ANTHROPIC_API_KEY is not set
- [x] Results saved to bench/results/tokens/ as JSON

## Outcome

Completed on 2026-02-28. Created bench/e2e/token_compare.py — mirrors compare.py's API mode
but captures the usage{} field from Anthropic API responses to record input/output token counts
per condition. Created bench/suites/06-token-cost.sh with two thresholds: cost ratio < 10x
(hard ceiling) and < 5x (efficiency target). Dry-run estimates show ~2,500 avg input token
overhead and ~5x cost ratio, which passes the 10x ceiling but may trigger the 5x target on
tasks with multiple skills loaded. Results cached in bench/results/tokens/.

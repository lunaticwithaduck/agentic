---
title: Run Suite 04 E2E Task Quality Comparison
created: 2026-02-25
status: idea
author: evaluation
tags: [bench, validation, high-impact]
priority: high
---

# Run Suite 04 E2E Task Quality Comparison

## Description

The core value proposition of agentic — that activating specialized skills meaningfully
improves Claude's output — remains unvalidated. Suite 04 and `bench/e2e/compare.py` are
fully implemented with 5 well-designed tasks and rubrics. The only missing piece is running
it with an `ANTHROPIC_API_KEY`.

Running this suite would either:
- **Validate the project** (skills produce measurably better output) → strong marketing signal
- **Surface what needs to change** (skills don't help or hurt) → actionable improvement data

## Notes

- Created via project evaluation
- Infrastructure is ready: `compare.py`, `tasks.json`, suite 04 all exist
- Cost estimate: ~5 tasks × 2 conditions × ~$0.03/call + 5 judge calls ≈ $0.50
- Results cache in `bench/results/e2e/` so reruns are free

## Possible Acceptance Criteria

- [ ] Run `ANTHROPIC_API_KEY=... bash bench/run.sh --suite=04` successfully
- [ ] Document results in the bench changelog
- [ ] If infra wins < 60%, analyze which skills need improvement
- [ ] Add results summary to README or docs/EVALUATION.md

---
title: Add CI Workflow for Benchmark Suite
created: 2026-02-25
status: idea
author: evaluation
tags: [ci, bench, automation]
priority: high
---

# Add CI Workflow for Benchmark Suite

## Description

A GitHub Actions workflow that runs `bench/run.sh` (suites 01-03 and 05) on every PR would
catch regressions in skill detection accuracy and hook security. Suite 04 can be skipped in
CI since it requires an API key, or optionally enabled via a repository secret.

This would:
- Prevent accidental regressions when modifying skills or hook patterns
- Make the changelog self-maintaining (each PR gets a bench entry)
- Demonstrate the infrastructure working in a real CI environment
- Serve as documentation for how adopters could add their own CI integration

## Notes

- Created via project evaluation
- Suites 01, 02, 03, and 05 require no API keys and run in ~7 seconds total
- Suite 04 could optionally run if `ANTHROPIC_API_KEY` is set as a repo secret
- The bench runner already has `--no-changelog` flag for CI environments

## Possible Acceptance Criteria

- [ ] `.github/workflows/bench.yml` runs on push to main and on PRs
- [ ] Suites 01-03 and 05 run without API keys
- [ ] Suite 04 conditionally runs if ANTHROPIC_API_KEY secret is available
- [ ] Workflow fails if any suite fails (exit code propagation)
- [ ] Bench metrics are uploaded as an artifact for historical tracking

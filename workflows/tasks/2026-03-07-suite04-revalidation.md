---
title: Re-run Suite 04 and validate current benchmark baseline
created: 2026-03-07
status: task
author: agent
tags: [bench, suite-04, validation]
priority: medium
complexity: low
---

# Re-run Suite 04 and validate current benchmark baseline

## Description

Suite 04 results are from 2026-02-28. Significant changes have landed since then
(fixture auto-gen, synthesis instruction updates, skill format changes). Need a
fresh baseline after eq04 and eq07 fixes land.

## Acceptance Criteria

- [ ] Suite 04 re-run with --no-cache to force fresh LLM responses
- [ ] New results recorded and compared to Feb baseline
- [ ] CONTEXT.md updated with new numbers
- [ ] Win rate >= 70% (same bar as before) or regression explained

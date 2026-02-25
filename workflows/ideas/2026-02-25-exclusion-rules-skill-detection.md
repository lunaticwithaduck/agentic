---
title: Add Negative/Exclusion Rules to Skill Detection
created: 2026-02-25
status: idea
author: evaluation
tags: [skill-detection, accuracy, medium-impact]
priority: medium
---

# Add Negative/Exclusion Rules to Skill Detection

## Description

The current keyword matching in `skill-rules.json` is purely additive — a keyword either
matches or it doesn't. The 7 remaining false positives in suite 02 (debug: 2,
logging-strategy: 1, review-pr: 1, mock-generator: 1, middleware-design: 1,
technical-proposal: 1) are all cases where a keyword legitimately appears in a prompt but
the skill isn't actually relevant.

Adding an optional `exclude_keywords` field to skill rules would allow patterns like:
```json
{
  "debug": {
    "keywords": ["debug", "stack trace", "error", ...],
    "exclude_keywords": ["deploy debug", "debug log level"]
  }
}
```

This would reduce false positives without requiring increasingly specific keywords.

## Notes

- Created via project evaluation
- Would require changes to: `skill-rules.json` schema, `02-skill-detection.sh` Python logic
- Current F1 is 93.3% — exclusion rules could push it toward 96-97%
- The bench suite would immediately measure the improvement

## Possible Acceptance Criteria

- [ ] `skill-rules.json` schema supports optional `exclude_keywords` field
- [ ] Detection logic in suite 02 checks exclusions before confirming a match
- [ ] At least 3 of the 7 current FPs are fixed via exclusion rules
- [ ] F1 score improves (verified by bench run)
- [ ] No regressions in existing true positives

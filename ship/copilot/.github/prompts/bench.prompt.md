---
description: Show the latest benchmark results in a pretty terminal report.
mode: agent
---

## Instructions

Run the bench report script and display the output:

```bash
python3 bench/lib/report.py $ARGUMENTS
```

The script reads the latest metrics JSON from `bench/results/metrics/` and renders a
formatted table with per-suite scores, pass/fail counts, timing, suite-specific
metrics (F1, FPR/FNR, win rates), and a delta vs the previous run.

**Flags you can pass via arguments:**
- *(no args)*  — latest run summary
- `--all`      — latest run + score history sparklines across all stored runs
- `--suite=02` — focus on a specific suite number

**If no metrics exist yet:**
Tell the user to run `bash bench/run.sh` first.

After showing the output, note whether suite 04 is showing as skipped (needs a real
terminal) or has actual results, and offer to run the bench if the user wants fresh data.

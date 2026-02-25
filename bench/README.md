# bench/

Benchmark suite for the agentic infrastructure. Measures whether the infrastructure
is actually doing what it claims — and tracks regressions over time.

## Quick start

```bash
bash bench/run.sh
```

## What it measures

| Suite | What it tests | Key metric | Requires |
|-------|--------------|------------|----------|
| `01-infrastructure` | Directory structure, configs, skill/hook/agent/command counts | Pass/fail | — |
| `02-skill-detection` | Keyword routing accuracy against 100 labeled prompts | Precision / Recall / F1 | — |
| `03-hook-security` | block-secrets.sh against 85 safe/dangerous commands | False positive/negative rates | — |
| `04-task-quality` | LLM-judged response quality: with infra vs. without | Win rate / avg score delta | `ANTHROPIC_API_KEY` |

## Outputs

**Results live in `bench/results/`:**

- `metrics/<timestamp>.json` — full machine-readable data per run (gitignored)
- `changelog.md` — human-readable diff of every run, newest first

The changelog is the primary tool for tracking what's improving vs. regressing:

```
## 2026-02-25T18:25:34Z  |  sha: 13f2365

Overall score: 0.9402  (no previous run)

| Suite              | Score  | Passed | Failed | Prev | Delta |
|--------------------|--------|--------|--------|------|-------|
| 01-infrastructure  | 1.0000 | 24     | 0      | —    | —     |
| 02-skill-detection | 0.5000 | 2      | 2      | —    | —     |
| 03-hook-security   | 0.9438 | 84     | 5      | —    | —     |
```

## Flags

```bash
bash bench/run.sh --suite=02          # run only suites matching "02"
bash bench/run.sh --no-changelog      # skip changelog update
bash bench/run.sh --quiet             # suppress per-test output (TODO)

# Suite 04 only (requires ANTHROPIC_API_KEY):
export ANTHROPIC_API_KEY=sk-ant-...
bash bench/run.sh --suite=04

# E2E options passed through to compare.py:
python3 bench/e2e/compare.py --task=eq01        # single task
python3 bench/e2e/compare.py --judge-only        # re-judge cached responses
python3 bench/e2e/compare.py --no-cache          # force re-run all API calls
python3 bench/e2e/compare.py --dry-run           # no API calls, fake scores
```

## Thresholds

Suite thresholds are deliberately set at achievable-but-meaningful levels:

| Metric | Threshold | Rationale |
|--------|-----------|-----------|
| Skill detection precision | ≥ 70% | Prevents excessive noise in skill activation |
| Skill detection recall | ≥ 70% | Ensures relevant skills aren't missed |
| Skill detection F1 | ≥ 70% | Balanced harmonic mean |
| Hook false negative rate | ≤ 5% | Dangerous commands must not slip through |
| Hook false positive rate | ≤ 10% | Over-blocking degrades usability |

Adjust thresholds in `suites/02-skill-detection.sh` and `suites/03-hook-security.sh`
as the infrastructure matures.

## Baseline (first run: 2026-02-25)

```
Overall: 94.0%  (110/117 tests passing)

01-infrastructure:  100%  (24/24)   — perfect
02-skill-detection:  62%  F1        — precision low (over-triggering keywords)
03-hook-security:   94.4% accuracy  — 4 known FPs (.env.example, git add .gitignore)
```

## What the results tell you

**Suite 02 low precision (49.3%)**: The keyword rules in `skill-rules.json` are too broad —
they match unrelated prompts. Improve by making keywords more specific or adding
negative/exclusion rules. Each keyword improvement should push precision up toward 70%.

**Suite 03 false positives**: `cat .env.example` and `git add .gitignore` trigger the
hook's regex patterns. The patterns could be tightened (e.g., `cat.*\.env[^.]` to avoid
matching `.env.example`) — each fix pushes the false positive rate lower.

## Adding tests

- **Skill prompts**: Add entries to `bench/fixtures/skill-prompts.json`
- **Hook commands**: Add entries to `bench/fixtures/hook-commands.json`
- **New suites**: Drop a numbered `.sh` file in `bench/suites/` — it's auto-discovered

Suite scripts are **sourced** (not executed) by `run.sh`, so they have access to
`$ROOT_DIR`, `print_pass`, `print_fail`, `print_skip`, `print_info`, `SUITE_PASSED`,
and `SUITE_FAILED`. Set `SUITE_JSON` at the end for suite-specific metrics.

#!/usr/bin/env bash
# bench/run.sh — Agentic infrastructure benchmark runner
#
# Usage:
#   ./bench/run.sh                    # run all suites
#   ./bench/run.sh --suite=02         # run a single suite by number prefix
#   ./bench/run.sh --no-changelog     # skip changelog update
#   ./bench/run.sh --quiet            # suppress per-test output
#
# Results are written to bench/results/metrics/<timestamp>.json
# Changelog is appended to bench/results/changelog.md

set -euo pipefail

BENCH_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$BENCH_DIR/.." && pwd)"

# Source shared libs
source "$BENCH_DIR/lib/colors.sh"
source "$BENCH_DIR/lib/metrics.sh"

# Parse args
SUITE_FILTER=""
WRITE_CHANGELOG=true
QUIET=false

for arg in "$@"; do
  case "$arg" in
    --suite=*) SUITE_FILTER="${arg#--suite=}" ;;
    --no-changelog) WRITE_CHANGELOG=false ;;
    --quiet) QUIET=true; export BENCH_QUIET=true ;;
    --help|-h)
      echo "Usage: $0 [--suite=<prefix>] [--no-changelog] [--quiet]"
      exit 0
      ;;
  esac
done

# ─── Header ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${CYAN}╔══════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║    Agentic Infrastructure Bench      ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════╝${NC}"
echo -e "  ${DIM}Root: $ROOT_DIR${NC}"
echo -e "  ${DIM}Time: $(date -u +"%Y-%m-%d %H:%M:%S UTC")${NC}"

# ─── Init metrics ─────────────────────────────────────────────────────────────
metrics_init

# ─── Discover and run suites ──────────────────────────────────────────────────
SUITE_DIR="$BENCH_DIR/suites"
TOTAL_PASSED=0
TOTAL_FAILED=0
SUITE_COUNT=0
FAILED_SUITES=()

for suite_file in "$SUITE_DIR"/*.sh; do
  [ -f "$suite_file" ] || continue

  suite_basename="$(basename "$suite_file" .sh)"

  # Apply filter if set
  if [ -n "$SUITE_FILTER" ] && [[ "$suite_basename" != *"$SUITE_FILTER"* ]]; then
    continue
  fi

  SUITE_COUNT=$((SUITE_COUNT + 1))
  SUITE_PASSED=0
  SUITE_FAILED=0
  SUITE_JSON=""

  suite_start=$(_now_ms)

  # Source the suite (gives it access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED)
  # shellcheck source=/dev/null
  source "$suite_file"

  suite_end=$(_now_ms)
  suite_duration=$((suite_end - suite_start))

  TOTAL_PASSED=$((TOTAL_PASSED + SUITE_PASSED))
  TOTAL_FAILED=$((TOTAL_FAILED + SUITE_FAILED))

  if [ "$SUITE_FAILED" -gt 0 ]; then
    FAILED_SUITES+=("$suite_basename")
  fi

  # Register in metrics
  metrics_record_suite "$suite_basename" "$SUITE_PASSED" "$SUITE_FAILED" "$suite_duration" "${SUITE_JSON:-}"

  echo ""
  echo -e "  ${DIM}Duration: ${suite_duration}ms  |  Passed: ${SUITE_PASSED}  |  Failed: ${SUITE_FAILED}${NC}"
done

if [ "$SUITE_COUNT" -eq 0 ]; then
  echo ""
  echo -e "${YELLOW}No suites found matching filter '${SUITE_FILTER}'.${NC}"
  exit 0
fi

# ─── Overall summary ──────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
TOTAL=$((TOTAL_PASSED + TOTAL_FAILED))
if [ "$TOTAL" -gt 0 ]; then
  OVERALL_SCORE=$(python3 -c "print(f'{$TOTAL_PASSED / $TOTAL:.1%}')")
else
  OVERALL_SCORE="N/A"
fi

if [ "$TOTAL_FAILED" -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}All tests passed${NC}  ($TOTAL_PASSED/$TOTAL)  Score: ${OVERALL_SCORE}"
else
  echo -e "  ${RED}${BOLD}$TOTAL_FAILED test(s) failed${NC}  ($TOTAL_PASSED/$TOTAL passed)  Score: ${OVERALL_SCORE}"
  echo -e "  ${RED}Failed suites: ${FAILED_SUITES[*]}${NC}"
fi

# ─── Write metrics ────────────────────────────────────────────────────────────
METRICS_OUT=$(metrics_finalize)
echo ""
echo -e "  ${DIM}Metrics: $METRICS_OUT${NC}"

# ─── Changelog ────────────────────────────────────────────────────────────────
if [ "$WRITE_CHANGELOG" = true ]; then
  PREV_JSON=$(metrics_load_previous)
  metrics_write_changelog "$METRICS_OUT" "${PREV_JSON:-}"
  echo -e "  ${DIM}Changelog: $ROOT_DIR/bench/results/changelog.md${NC}"
fi

echo ""

# ─── Pretty report ────────────────────────────────────────────────────────────
python3 "$BENCH_DIR/lib/report.py" 2>/dev/null || true

# Exit with failure if any tests failed
[ "$TOTAL_FAILED" -eq 0 ]

#!/usr/bin/env bash
# bench/lib/colors.sh — Terminal color helpers

# Colors (POSIX-safe, disabled when not a terminal)
if [ -t 1 ]; then
  RED='\033[0;31m'
  GREEN='\033[0;32m'
  YELLOW='\033[0;33m'
  CYAN='\033[0;36m'
  BOLD='\033[1m'
  DIM='\033[2m'
  NC='\033[0m'
else
  RED='' GREEN='' YELLOW='' CYAN='' BOLD='' DIM='' NC=''
fi

print_suite() {
  echo ""
  echo -e "${BOLD}${CYAN}━━━ $1 ━━━${NC}"
}

print_pass() {
  [ "${BENCH_QUIET:-false}" != "true" ] && echo -e "  ${GREEN}✓${NC} $1"
  SUITE_PASSED=$((SUITE_PASSED + 1))
}

print_fail() {
  echo -e "  ${RED}✗${NC} $1"   # failures always visible
  SUITE_FAILED=$((SUITE_FAILED + 1))
}

print_skip() {
  [ "${BENCH_QUIET:-false}" != "true" ] && echo -e "  ${YELLOW}~${NC} $1"
  return 0
}

print_info() {
  [ "${BENCH_QUIET:-false}" != "true" ] && echo -e "  ${DIM}→ $1${NC}"
  return 0
}

print_metric() {
  # print_metric "Label" "value" [good|warn|bad]
  local label="$1" value="$2" status="${3:-}"
  case "$status" in
    good) echo -e "  ${GREEN}${label}:${NC} ${BOLD}${value}${NC}" ;;
    warn) echo -e "  ${YELLOW}${label}:${NC} ${BOLD}${value}${NC}" ;;
    bad)  echo -e "  ${RED}${label}:${NC} ${BOLD}${value}${NC}" ;;
    *)    echo -e "  ${label}: ${BOLD}${value}${NC}" ;;
  esac
}

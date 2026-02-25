#!/usr/bin/env bash
# bench/e2e/cleanup.sh — Clean up e2e result caches
#
# Removes cached responses and/or judgements from bench/results/e2e/.
# Caches are keyed by mode: api produces with-infra.txt / without-infra.txt / judgement.json;
# subprocess produces the -sub variants.
#
# Usage:
#   bash bench/e2e/cleanup.sh                # dry-run: show what would be removed
#   bash bench/e2e/cleanup.sh --all          # remove everything
#   bash bench/e2e/cleanup.sh --responses    # remove cached response text files
#   bash bench/e2e/cleanup.sh --judgements   # remove cached judgement JSON files
#   bash bench/e2e/cleanup.sh --summaries    # remove aggregate summary JSON files
#
# Combine flags freely:
#   bash bench/e2e/cleanup.sh --responses --judgements

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
E2E_RESULTS="$SCRIPT_DIR/../results/e2e"

CLEAN_RESPONSES=false
CLEAN_JUDGEMENTS=false
CLEAN_SUMMARIES=false
DRY_RUN=true

for arg in "$@"; do
  case "$arg" in
    --all)        CLEAN_RESPONSES=true; CLEAN_JUDGEMENTS=true; CLEAN_SUMMARIES=true; DRY_RUN=false ;;
    --responses)  CLEAN_RESPONSES=true;  DRY_RUN=false ;;
    --judgements) CLEAN_JUDGEMENTS=true; DRY_RUN=false ;;
    --summaries)  CLEAN_SUMMARIES=true;  DRY_RUN=false ;;
    --dry-run)    DRY_RUN=true ;;
    -h|--help)
      echo "Usage: $0 [--all|--responses|--judgements|--summaries] [--dry-run]"
      echo ""
      echo "  --responses   Remove with-infra*.txt and without-infra*.txt"
      echo "  --judgements  Remove judgement*.json"
      echo "  --summaries   Remove summary-*.json at the e2e results root"
      echo "  --all         Remove all of the above"
      echo "  --dry-run     Show what would be removed (default when no action flag given)"
      exit 0
      ;;
    *)
      echo "Unknown flag: $arg  (use --help for usage)" >&2
      exit 1
      ;;
  esac
done

if [ ! -d "$E2E_RESULTS" ]; then
  echo "E2E results directory not found: $E2E_RESULTS"
  exit 0
fi

removed=0

_remove() {
  local pattern="$1"
  local depth="${2:--maxdepth 3}"
  # shellcheck disable=SC2086
  while IFS= read -r -d '' f; do
    if [ "$DRY_RUN" = true ]; then
      echo "  [dry-run] $f"
    else
      echo "  removed:  $f"
      rm -f "$f"
    fi
    removed=$((removed + 1))
  done < <(find "$E2E_RESULTS" $depth -name "$pattern" -print0 2>/dev/null)
}

echo ""
echo "E2E Cache Cleanup"
echo "━━━━━━━━━━━━━━━━━"

if [ "$CLEAN_RESPONSES" = true ]; then
  echo ""
  echo "Responses (with-infra*.txt, without-infra*.txt):"
  _remove "with-infra*.txt"
  _remove "without-infra*.txt"
fi

if [ "$CLEAN_JUDGEMENTS" = true ]; then
  echo ""
  echo "Judgements (judgement*.json):"
  _remove "judgement*.json"
fi

if [ "$CLEAN_SUMMARIES" = true ]; then
  echo ""
  echo "Summaries (summary-*.json):"
  _remove "summary-*.json" "-maxdepth 1"
fi

echo ""
if [ "$DRY_RUN" = true ]; then
  if [ "$removed" -eq 0 ]; then
    echo "Cache is empty — nothing to clean."
  else
    echo "Dry run: $removed file(s) would be removed."
    echo "Run with --all, --responses, --judgements, or --summaries to actually remove."
  fi
else
  echo "$removed file(s) removed."
fi
echo ""

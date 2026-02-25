# bench/suites/03-hook-security.sh
# Suite: Hook Security
# Tests block-secrets.sh against a labeled corpus of safe and dangerous commands.
# Computes false positive rate, false negative rate, and overall accuracy.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "03 · Hook Security"

SUITE_PASSED=0
SUITE_FAILED=0

HOOK="$ROOT_DIR/.claude/hooks/block-secrets.sh"
FIXTURES="$ROOT_DIR/bench/fixtures/hook-commands.json"

if [ ! -f "$HOOK" ]; then
  print_fail "block-secrets.sh not found — skipping suite"
  SUITE_JSON="{}"
  return 0
fi

if [ ! -x "$HOOK" ]; then
  print_fail "block-secrets.sh is not executable — skipping suite"
  SUITE_JSON="{}"
  return 0
fi

if [ ! -f "$FIXTURES" ]; then
  print_skip "bench/fixtures/hook-commands.json not found — skipping suite"
  print_info "Run: create bench/fixtures/hook-commands.json to enable this suite"
  SUITE_JSON="{}"
  return 0
fi

# ── Extract commands from fixtures ────────────────────────────────────────────
BLOCK_CMDS=$(python3 -c "
import json
with open('$FIXTURES') as f:
    data = json.load(f)
for item in data.get('should_block', []):
    print(item['id'] + '|' + item['cmd'] + '|' + item.get('reason',''))
")

ALLOW_CMDS=$(python3 -c "
import json
with open('$FIXTURES') as f:
    data = json.load(f)
for item in data.get('should_allow', []):
    print(item['id'] + '|' + item['cmd'] + '|' + item.get('reason',''))
")

# ── Test: commands that MUST be blocked ───────────────────────────────────────
echo ""
print_info "Testing commands that must be blocked..."

block_passed=0
block_failed=0
false_negatives=()  # should block but didn't

while IFS='|' read -r id cmd reason; do
  [ -z "$id" ] && continue
  # Escape cmd for JSON embedding
  json_input="{\"command\":$(python3 -c "import json,sys; print(json.dumps(sys.argv[1]))" "$cmd")}"
  exit_code=0
  bash "$HOOK" "$json_input" > /dev/null 2>&1 || exit_code=$?
  if [ "$exit_code" -eq 2 ]; then
    block_passed=$((block_passed + 1))
    SUITE_PASSED=$((SUITE_PASSED + 1))
  else
    block_failed=$((block_failed + 1))
    SUITE_FAILED=$((SUITE_FAILED + 1))
    false_negatives+=("[$id] $cmd")
  fi
done <<< "$BLOCK_CMDS"

if [ "$block_failed" -eq 0 ]; then
  print_pass "All $block_passed dangerous commands correctly blocked"
else
  print_fail "$block_failed dangerous command(s) NOT blocked (false negatives)"
  for fn in "${false_negatives[@]}"; do
    print_info "  Not blocked: $fn"
  done
fi

# ── Test: commands that MUST be allowed ───────────────────────────────────────
echo ""
print_info "Testing commands that must be allowed..."

allow_passed=0
allow_failed=0
false_positives=()  # should allow but blocked

while IFS='|' read -r id cmd reason; do
  [ -z "$id" ] && continue
  json_input="{\"command\":$(python3 -c "import json,sys; print(json.dumps(sys.argv[1]))" "$cmd")}"
  exit_code=0
  bash "$HOOK" "$json_input" > /dev/null 2>&1 || exit_code=$?
  if [ "$exit_code" -eq 0 ]; then
    allow_passed=$((allow_passed + 1))
    SUITE_PASSED=$((SUITE_PASSED + 1))
  else
    allow_failed=$((allow_failed + 1))
    SUITE_FAILED=$((SUITE_FAILED + 1))
    false_positives+=("[$id] $cmd")
  fi
done <<< "$ALLOW_CMDS"

if [ "$allow_failed" -eq 0 ]; then
  print_pass "All $allow_passed safe commands correctly allowed"
else
  print_fail "$allow_failed safe command(s) incorrectly blocked (false positives)"
  for fp in "${false_positives[@]}"; do
    print_info "  Incorrectly blocked: $fp"
  done
fi

# ── Summary metrics ───────────────────────────────────────────────────────────
TOTAL_BLOCK=$(echo "$BLOCK_CMDS" | grep -c '|' || echo 0)
TOTAL_ALLOW=$(echo "$ALLOW_CMDS" | grep -c '|' || echo 0)
TOTAL_TESTS=$((TOTAL_BLOCK + TOTAL_ALLOW))

FN_RATE=$(python3 -c "print(round($block_failed / $TOTAL_BLOCK, 4) if $TOTAL_BLOCK > 0 else 0.0)")
FP_RATE=$(python3 -c "print(round($allow_failed / $TOTAL_ALLOW, 4) if $TOTAL_ALLOW > 0 else 0.0)")
ACCURACY=$(python3 -c "print(round(($block_passed + $allow_passed) / $TOTAL_TESTS, 4) if $TOTAL_TESTS > 0 else 1.0)")

echo ""
print_info "Block corpus: $TOTAL_BLOCK commands | Allow corpus: $TOTAL_ALLOW commands"
print_info "False negative rate (missed blocks): $FN_RATE"
print_info "False positive rate (over-blocking): $FP_RATE"

# Pass/fail on rate thresholds
if python3 -c "import sys; sys.exit(0 if $FN_RATE <= 0.05 else 1)" 2>/dev/null; then
  print_pass "False negative rate $FN_RATE <= 5% threshold"
else
  print_fail "False negative rate $FN_RATE exceeds 5% threshold (dangerous commands slipping through)"
fi

if python3 -c "import sys; sys.exit(0 if $FP_RATE <= 0.10 else 1)" 2>/dev/null; then
  print_pass "False positive rate $FP_RATE <= 10% threshold"
else
  print_fail "False positive rate $FP_RATE exceeds 10% threshold (too many safe commands blocked)"
fi

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON="{\"false_negative_rate\":$FN_RATE,\"false_positive_rate\":$FP_RATE,\"accuracy\":$ACCURACY,\"block_corpus_size\":$TOTAL_BLOCK,\"allow_corpus_size\":$TOTAL_ALLOW}"

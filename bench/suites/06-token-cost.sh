# bench/suites/06-token-cost.sh
# Suite: Token Cost
# Measures the token overhead of agentic infrastructure vs vanilla Claude.
# Uses subprocess mode (real claude -p runs) — same approach as suite 04.
# Token counts come from the usage{} field in claude's --output-format=json response.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "06 · Token Cost"

SUITE_PASSED=0
SUITE_FAILED=0
SUITE_JSON="{}"

TOKEN_COMPARE="$ROOT_DIR/bench/e2e/token_compare.py"
TASKS_FILE="$ROOT_DIR/bench/e2e/tasks.json"

# ── Preflight ──────────────────────────────────────────────────────────────────
if [ ! -f "$TOKEN_COMPARE" ]; then
  print_fail "bench/e2e/token_compare.py not found"
  return 0
fi

if [ ! -f "$TASKS_FILE" ]; then
  print_fail "bench/e2e/tasks.json not found"
  return 0
fi

if [ -n "${CLAUDECODE:-}" ]; then
  print_skip "Suite 06 cannot run inside a Claude Code session (subprocess mode is blocked)"
  print_info "This suite is intended to be run by Claude. To run it:"
  print_info "  env -u CLAUDECODE bash bench/run.sh --suite=06"
  return 0
fi

TASK_COUNT=$(python3 -c "import json; d=json.load(open('$TASKS_FILE')); print(len(d))" 2>/dev/null || echo 0)
print_info "Measuring token cost for $TASK_COUNT tasks via subprocess | model: claude-sonnet-4-6"
print_info "Results cached in bench/results/tokens/ — use --no-cache to force re-run"
echo ""

# ── Run token comparison ───────────────────────────────────────────────────────
_TOK_STDERR=$(mktemp)
set +e
TOKEN_OUT=$(python3 "$TOKEN_COMPARE" --json 2>"$_TOK_STDERR")
TOKEN_EXIT=$?
set -e

if [ -s "$_TOK_STDERR" ]; then
  while IFS= read -r _line; do
    print_info "$_line"
  done < "$_TOK_STDERR"
fi
rm -f "$_TOK_STDERR"

if [ "$TOKEN_EXIT" -ne 0 ]; then
  print_fail "token_compare.py exited with code $TOKEN_EXIT"
  [ -n "$TOKEN_OUT" ] && print_info "Output: ${TOKEN_OUT:0:300}"
  return 0
fi

if ! echo "$TOKEN_OUT" | python3 -m json.tool > /dev/null 2>&1; then
  print_fail "token_compare.py returned invalid JSON"
  print_info "${TOKEN_OUT:0:300}"
  return 0
fi

# ── Parse results ──────────────────────────────────────────────────────────────
AVG_OVERHEAD=$(echo "$TOKEN_OUT" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['avg_input_overhead'])")
AVG_OUT_DELTA=$(echo "$TOKEN_OUT" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['avg_output_delta'])")
AVG_RATIO=$(echo "$TOKEN_OUT" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['avg_cost_ratio'])")

# Per-task table
_JSON_TMP=$(mktemp)
printf '%s' "$TOKEN_OUT" > "$_JSON_TMP"
python3 - "$_JSON_TMP" <<'PYEOF'
import json, sys
data = json.load(open(sys.argv[1]))
for t in data.get("tasks", []):
    if t.get("dry_run"):
        print(f"  [{t['task_id']}] {t['title'][:45]:<45}  [dry run]")
    else:
        sign = "+" if t["output_delta"] >= 0 else ""
        print(
            f"  [{t['task_id']}] {t['title'][:45]:<45}  "
            f"in +{t['input_overhead']:>6,}  "
            f"out {sign}{t['output_delta']:>5,}  "
            f"ratio {t['cost_ratio']:.2f}x"
        )
PYEOF
rm -f "$_JSON_TMP"

echo ""
print_info "Avg input overhead: +${AVG_OVERHEAD} tokens  |  Avg output delta: ${AVG_OUT_DELTA}  |  Avg ratio: ${AVG_RATIO}x"

# ── Pass/fail thresholds ───────────────────────────────────────────────────────

# Threshold 1: cost ratio < 10x (overhead must be reasonable)
# Rationale: if infra costs 10x the tokens, it can't possibly be worth it
python3 -c "import sys; sys.exit(0 if $AVG_RATIO < 10.0 else 1)" 2>/dev/null \
  && print_pass "Avg cost ratio ${AVG_RATIO}x < 10x ceiling (overhead is bounded)" \
  || print_fail "Avg cost ratio ${AVG_RATIO}x >= 10x — infrastructure overhead is too high"

# Threshold 2: cost ratio < 5x (informational — tighter target)
python3 -c "import sys; sys.exit(0 if $AVG_RATIO < 5.0 else 1)" 2>/dev/null \
  && print_pass "Avg cost ratio ${AVG_RATIO}x < 5x target (good efficiency)" \
  || print_fail "Avg cost ratio ${AVG_RATIO}x >= 5x — consider trimming skill content"

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON=$(echo "$TOKEN_OUT" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(json.dumps({
  'avg_input_overhead': d['avg_input_overhead'],
  'avg_output_delta':   d['avg_output_delta'],
  'avg_cost_ratio':     d['avg_cost_ratio'],
  'task_count':         d['task_count'],
}))
")

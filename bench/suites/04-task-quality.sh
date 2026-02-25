# bench/suites/04-task-quality.sh
# Suite: E2E Task Quality
# Compares Claude response quality WITH vs WITHOUT agentic infrastructure context.
# Requires ANTHROPIC_API_KEY. Skips gracefully if not set.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "04 · Task Quality (E2E)"

SUITE_PASSED=0
SUITE_FAILED=0
SUITE_JSON="{}"

COMPARE="$ROOT_DIR/bench/e2e/compare.py"
TASKS_FILE="$ROOT_DIR/bench/e2e/tasks.json"
E2E_MODE="${BENCH_E2E_MODE:-subprocess}"

# ── Preflight ──────────────────────────────────────────────────────────────────
if [ ! -f "$COMPARE" ]; then
  print_fail "bench/e2e/compare.py not found"
  return 0
fi

if [ ! -f "$TASKS_FILE" ]; then
  print_fail "bench/e2e/tasks.json not found"
  return 0
fi

if [ "$E2E_MODE" = "subprocess" ] && [ -n "${CLAUDECODE:-}" ]; then
  print_skip "subprocess mode cannot run inside a Claude Code session (nested session block)"
  print_info "Run from a real terminal:  bash bench/run.sh --suite=04"
  print_info "Or use api mode:           BENCH_E2E_MODE=api ANTHROPIC_API_KEY=sk-ant-... bash bench/run.sh --suite=04"
  return 0
fi

if [ -z "${ANTHROPIC_API_KEY:-}" ] && [ "$E2E_MODE" = "api" ]; then
  print_skip "ANTHROPIC_API_KEY not set — skipping E2E suite (api mode requires it)"
  print_info "Export ANTHROPIC_API_KEY, or run from a real terminal for subprocess mode (default)"
  return 0
fi

TASK_COUNT=$(python3 -c "import json; d=json.load(open('$TASKS_FILE')); print(len(d))" 2>/dev/null || echo 0)
print_info "Running $TASK_COUNT tasks | mode: $E2E_MODE | model: claude-sonnet-4-6"
print_info "Results cached in bench/results/e2e/ — use --no-cache to force re-run"
if [ "$E2E_MODE" = "subprocess" ]; then
  print_info "subprocess mode: spawning real claude -p runs (project root vs temp dir)"
fi
echo ""

# ── Run comparison ────────────────────────────────────────────────────────────
COMPARE_OUT=$(python3 "$COMPARE" --json --mode="$E2E_MODE" 2>&1)
COMPARE_EXIT=$?

if [ $COMPARE_EXIT -eq 2 ]; then
  # API key error already handled above, but catch for robustness
  print_fail "API key error from compare.py"
  return 0
fi

if ! echo "$COMPARE_OUT" | python3 -m json.tool > /dev/null 2>&1; then
  print_fail "compare.py returned invalid output"
  print_info "$COMPARE_OUT"
  return 0
fi

# ── Parse results ─────────────────────────────────────────────────────────────
INFRA_WINS=$(echo "$COMPARE_OUT"    | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['infra_wins'])")
VANILLA_WINS=$(echo "$COMPARE_OUT"  | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['vanilla_wins'])")
TIES=$(echo "$COMPARE_OUT"          | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['ties'])")
INFRA_WIN_RATE=$(echo "$COMPARE_OUT"| python3 -c "import json,sys; d=json.load(sys.stdin); print(d['infra_win_rate'])")
AVG_DELTA=$(echo "$COMPARE_OUT"     | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['avg_delta'])")
AVG_WITH=$(echo "$COMPARE_OUT"      | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['avg_with_score'])")
AVG_WITHOUT=$(echo "$COMPARE_OUT"   | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['avg_without_score'])")
PASSED_OVERALL=$(echo "$COMPARE_OUT"| python3 -c "import json,sys; d=json.load(sys.stdin); print(d['pass'])")

# Per-task results
echo "$COMPARE_OUT" | python3 - <<'PYEOF'
import json, sys
data = json.load(sys.stdin)
for t in data.get("tasks", []):
    winner = t["winner"]
    delta = t["delta"]
    sign = "+" if delta >= 0 else ""
    label = {
        "A":   "INFRA wins",
        "B":   "VANILLA wins",
        "TIE": "TIE",
    }.get(winner, winner)
    print(f"  [{t['task_id']}] {t['title'][:45]:<45}  {t['with_score']:.2f} vs {t['without_score']:.2f}  {sign}{delta:.2f}  [{label}]")
PYEOF

echo ""
print_info "Infrastructure wins: $INFRA_WINS  |  Vanilla wins: $VANILLA_WINS  |  Ties: $TIES"
print_info "Avg score — with: $AVG_WITH/5.0  |  without: $AVG_WITHOUT/5.0  |  delta: $AVG_DELTA"

# ── Pass/fail thresholds ──────────────────────────────────────────────────────

# Threshold 1: infra wins or ties >= 60% of tasks
if [ "$PASSED_OVERALL" = "True" ]; then
  print_pass "Infrastructure wins/ties $(python3 -c "print(f'{$INFRA_WIN_RATE:.1%}')") >= 60% threshold"
else
  print_fail "Infrastructure wins/ties $(python3 -c "print(f'{$INFRA_WIN_RATE:.1%}')") < 60% threshold"
fi

# Threshold 2: average delta is positive
python3 -c "import sys; sys.exit(0 if $AVG_DELTA > 0 else 1)" 2>/dev/null \
  && print_pass "Average score delta is positive ($AVG_DELTA)" \
  || print_fail "Average score delta is not positive ($AVG_DELTA)"

# Threshold 3: average with-infra score > 3.0 (passing quality)
python3 -c "import sys; sys.exit(0 if $AVG_WITH > 3.0 else 1)" 2>/dev/null \
  && print_pass "Average with-infra score $AVG_WITH > 3.0" \
  || print_fail "Average with-infra score $AVG_WITH <= 3.0 (quality concern)"

# Threshold 4: no task where vanilla wins by > 0.5 (catastrophic regression)
CATASTROPHIC=$(echo "$COMPARE_OUT" | python3 -c "
import json, sys
data = json.load(sys.stdin)
bad = [t for t in data.get('tasks', []) if t['winner'] == 'B' and t['without_score'] - t['with_score'] > 0.5]
print(len(bad))
")
if [ "$CATASTROPHIC" -eq 0 ]; then
  print_pass "No tasks with catastrophic regression (vanilla beating infra by >0.5)"
else
  print_fail "$CATASTROPHIC task(s) where vanilla beats infra by >0.5 points"
fi

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON=$(echo "$COMPARE_OUT" | python3 -c "import json,sys; d=json.load(sys.stdin); print(json.dumps({'infra_wins':d['infra_wins'],'vanilla_wins':d['vanilla_wins'],'ties':d['ties'],'infra_win_rate':d['infra_win_rate'],'avg_delta':d['avg_delta'],'avg_with_score':d['avg_with_score'],'avg_without_score':d['avg_without_score']}))")

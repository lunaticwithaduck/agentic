#!/usr/bin/env bash
# bench/lib/metrics.sh — Metrics accumulation and reporting helpers

METRICS_FILE=""
METRICS_START_TIME=""

# Capture start time in milliseconds
_now_ms() {
  python3 -c "import time; print(int(time.time() * 1000))"
}

# Initialize a new metrics run
metrics_init() {
  METRICS_START_TIME=$(_now_ms)
  METRICS_FILE="$(mktemp /tmp/bench-metrics-XXXXXX.json)"
  cat > "$METRICS_FILE" <<EOF
{
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "git_sha": "$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || echo "unknown")",
  "suites": {}
}
EOF
}

# Record suite results into the metrics JSON
# metrics_record_suite <name> <passed> <failed> <duration_ms> [extra_json_fields]
metrics_record_suite() {
  local name="$1" passed="$2" failed="$3" duration_ms="$4"
  local extra="${5:-}"
  local total=$((passed + failed))
  local score
  if [ "$total" -eq 0 ]; then
    score="1.0"
  else
    score=$(python3 -c "print(round($passed / $total, 4))")
  fi

  local suite_json
  suite_json=$(python3 -c "
import json, sys
extra = $extra if '$extra' != '' else {}
obj = {'passed': $passed, 'failed': $failed, 'score': $score, 'duration_ms': $duration_ms}
obj.update(extra)
print(json.dumps(obj))
" 2>/dev/null || echo "{\"passed\":$passed,\"failed\":$failed,\"score\":$score,\"duration_ms\":$duration_ms}")

  python3 - "$METRICS_FILE" "$name" "$suite_json" <<'PYEOF'
import json, sys
path, suite_name, suite_data = sys.argv[1], sys.argv[2], sys.argv[3]
with open(path) as f:
    data = json.load(f)
data["suites"][suite_name] = json.loads(suite_data)
with open(path, "w") as f:
    json.dump(data, f, indent=2)
PYEOF
}

# Finalize metrics: compute overall score, write to results/
metrics_finalize() {
  local results_dir="$ROOT_DIR/bench/results/metrics"
  mkdir -p "$results_dir"

  local end_time
  end_time=$(_now_ms)
  local total_duration=$((end_time - METRICS_START_TIME))

  python3 - "$METRICS_FILE" "$total_duration" "$results_dir" <<'PYEOF'
import json, sys, os
from datetime import datetime

path, total_ms, results_dir = sys.argv[1], int(sys.argv[2]), sys.argv[3]
with open(path) as f:
    data = json.load(f)

# Aggregate overall
total_passed = sum(s.get("passed", 0) for s in data["suites"].values())
total_failed = sum(s.get("failed", 0) for s in data["suites"].values())
total_tests = total_passed + total_failed
overall_score = round(total_passed / total_tests, 4) if total_tests > 0 else 1.0

data["overall"] = {
    "score": overall_score,
    "passed": total_passed,
    "failed": total_failed,
    "duration_ms": total_ms
}

# Write to results/metrics/<timestamp>.json
ts = data["timestamp"].replace(":", "-").replace("T", "_").rstrip("Z")
out_path = os.path.join(results_dir, f"{ts}.json")
with open(out_path, "w") as f:
    json.dump(data, f, indent=2)

print(out_path)
PYEOF
}

# Load the previous metrics run (second-most-recent JSON in results/metrics/)
metrics_load_previous() {
  local results_dir="$ROOT_DIR/bench/results/metrics"
  local files
  files=$(ls -t "$results_dir"/*.json 2>/dev/null | tail -n +2 | head -n 1)
  if [ -n "$files" ]; then
    cat "$files"
  fi
}

# Append a changelog entry comparing current vs previous
metrics_write_changelog() {
  local current_file="$1"
  local prev_json="$2"
  local changelog="$ROOT_DIR/bench/results/changelog.md"

  python3 - "$current_file" "$changelog" <<PYEOF
import json, sys, os
from datetime import datetime

current_path, changelog_path = sys.argv[1], sys.argv[2]
with open(current_path) as f:
    cur = json.load(f)

prev_text = """$prev_json"""
prev = json.loads(prev_text) if prev_text.strip() else None

lines = []
ts = cur["timestamp"]
sha = cur.get("git_sha", "unknown")
overall = cur.get("overall", {})
cur_score = overall.get("score", 0)

# Header
lines.append(f"## {ts}  |  sha: {sha}")
lines.append("")

# Overall score with delta
if prev and "overall" in prev:
    prev_score = prev["overall"].get("score", 0)
    delta = cur_score - prev_score
    arrow = "↑" if delta > 0 else ("↓" if delta < 0 else "→")
    sign = "+" if delta >= 0 else ""
    lines.append(f"**Overall score:** {cur_score:.4f}  (prev: {prev_score:.4f}, {sign}{delta:.4f} {arrow})")
else:
    lines.append(f"**Overall score:** {cur_score:.4f}  (no previous run)")

lines.append("")

# Suite table
lines.append("| Suite | Score | Passed | Failed | Prev | Delta |")
lines.append("|-------|-------|--------|--------|------|-------|")
for name, suite in cur.get("suites", {}).items():
    score = suite.get("score", 0)
    passed = suite.get("passed", 0)
    failed = suite.get("failed", 0)
    prev_score_s = "—"
    delta_s = "—"
    if prev and name in prev.get("suites", {}):
        ps = prev["suites"][name].get("score", 0)
        d = score - ps
        sign = "+" if d >= 0 else ""
        arrow = "↑" if d > 0 else ("↓" if d < 0 else "→")
        prev_score_s = f"{ps:.4f}"
        delta_s = f"{sign}{d:.4f} {arrow}"
    lines.append(f"| {name} | {score:.4f} | {passed} | {failed} | {prev_score_s} | {delta_s} |")

lines.append("")
lines.append("---")
lines.append("")

entry = "\n".join(lines)

# Prepend to changelog (newest first)
existing = ""
if os.path.exists(changelog_path):
    with open(changelog_path) as f:
        existing = f.read()

with open(changelog_path, "w") as f:
    if not existing.startswith("# Bench Changelog"):
        f.write("# Bench Changelog\n\n")
        f.write("Tracks each benchmark run. Newest entries first.\n\n")
        f.write("---\n\n")
    else:
        # Write existing header
        header_end = existing.find("---\n\n") + 5
        f.write(existing[:header_end])
    f.write(entry)
    # Write rest of existing (skip old header)
    if existing.startswith("# Bench Changelog"):
        header_end = existing.find("---\n\n") + 5
        f.write(existing[header_end:])
PYEOF
}

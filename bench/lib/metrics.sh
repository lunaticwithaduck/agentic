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

# Finalize metrics: compute overall score, write .json + .md to results/metrics/
# Prints the path to the generated .md file.
metrics_finalize() {
  local results_dir="$ROOT_DIR/bench/results/metrics"
  mkdir -p "$results_dir"

  local end_time
  end_time=$(_now_ms)
  local total_duration=$((end_time - METRICS_START_TIME))

  python3 - "$METRICS_FILE" "$total_duration" "$results_dir" <<'PYEOF'
import json, sys, os

path, total_ms, results_dir = sys.argv[1], int(sys.argv[2]), sys.argv[3]
with open(path) as f:
    data = json.load(f)

ts        = data["timestamp"]          # e.g. "2026-02-25T18:48:00Z"
sha       = data.get("git_sha", "unknown")
slug      = ts.replace(":", "-").replace("T", "_").rstrip("Z")  # filename-safe

# Aggregate overall
total_passed = sum(s.get("passed", 0) for s in data["suites"].values())
total_failed = sum(s.get("failed", 0) for s in data["suites"].values())
total_tests  = total_passed + total_failed
overall_score = round(total_passed / total_tests, 4) if total_tests > 0 else 1.0

data["overall"] = {
    "score":       overall_score,
    "passed":      total_passed,
    "failed":      total_failed,
    "duration_ms": total_ms,
}

# ── Write JSON (for machine comparison) ──────────────────────────────────────
json_path = os.path.join(results_dir, f"{slug}.json")
with open(json_path, "w") as f:
    json.dump(data, f, indent=2)

# ── Write Markdown (human-readable result snapshot) ───────────────────────────
def score_icon(score, skipped=False):
    if skipped: return "⏭"
    return "✅" if score >= 1.0 else ("⚠️" if score >= 0.7 else "❌")

lines = []
lines.append(f"# Bench Results: {ts.replace('T', ' ').rstrip('Z')} UTC")
lines.append("")
lines.append(f"**git:** `{sha}`  ")
pct = f"{overall_score:.1%}"
lines.append(f"**overall:** {pct} — {total_passed}/{total_tests} passing")
lines.append("")

# Suite table
lines.append("| Suite | Score | Pass | Fail | Duration |")
lines.append("|-------|-------|------|------|----------|")
for name, suite in data["suites"].items():
    passed   = suite.get("passed", 0)
    failed   = suite.get("failed", 0)
    score    = suite.get("score", 1.0)
    dur      = suite.get("duration_ms", 0)
    skipped  = (passed == 0 and failed == 0)
    icon     = score_icon(score, skipped)
    pass_s   = "—" if skipped else str(passed)
    fail_s   = "—" if skipped else str(failed)
    score_s  = "skipped" if skipped else f"{score:.1%}"
    lines.append(f"| {name} | {icon} {score_s} | {pass_s} | {fail_s} | {dur}ms |")

lines.append("")

# Failing checks section
failing_suites = [
    (name, suite) for name, suite in data["suites"].items()
    if suite.get("failed", 0) > 0
]
if failing_suites:
    lines.append("## Failing checks")
    lines.append("")
    for name, suite in failing_suites:
        lines.append(f"- **{name}** — {suite.get('failed', 0)} check(s) failed")
    lines.append("")

# Per-suite detail blocks for suites with extra metrics
lines.append("## Suite details")
lines.append("")
for name, suite in data["suites"].items():
    extra_keys = {k: v for k, v in suite.items()
                  if k not in {"passed", "failed", "score", "duration_ms"}}
    if not extra_keys:
        continue
    lines.append(f"### {name}")
    lines.append("")
    lines.append("| Metric | Value |")
    lines.append("|--------|-------|")
    for k, v in extra_keys.items():
        if isinstance(v, float) and v <= 1.0:
            v_str = f"{v:.1%}"
        else:
            v_str = str(v)
        lines.append(f"| {k.replace('_', ' ').title()} | {v_str} |")
    lines.append("")

md_path = os.path.join(results_dir, f"{slug}.md")
with open(md_path, "w") as f:
    f.write("\n".join(lines))

print(md_path)
PYEOF
}

# Load the previous metrics run's JSON (second-most-recent .json in results/metrics/)
metrics_load_previous() {
  local results_dir="$ROOT_DIR/bench/results/metrics"
  local prev
  prev=$(ls -t "$results_dir"/*.json 2>/dev/null | sed -n '2p')
  [ -n "$prev" ] && cat "$prev"
}

# Append a stub changelog entry that links to the metrics file.
# Humans fill in the "what we tried" section.
metrics_write_changelog() {
  local md_path="$1"
  local prev_json="$2"
  local changelog="$ROOT_DIR/bench/results/changelog.md"
  local md_rel="metrics/$(basename "$md_path")"

  python3 - "$md_path" "$prev_json" "$changelog" "$md_rel" <<'PYEOF'
import json, sys, os

md_path, prev_json_str, changelog_path, md_rel = \
    sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]

# Read current run's JSON sibling
json_path = md_path.replace(".md", ".json")
with open(json_path) as f:
    cur = json.load(f)

prev = json.loads(prev_json_str) if prev_json_str.strip() else None

ts        = cur["timestamp"]
sha       = cur.get("git_sha", "unknown")
overall   = cur.get("overall", {})
cur_score = overall.get("score", 0)

# Score line with delta
if prev and "overall" in prev:
    prev_score = prev["overall"].get("score", 0)
    delta = cur_score - prev_score
    sign  = "+" if delta >= 0 else ""
    arrow = "↑" if delta > 0 else ("↓" if delta < 0 else "→")
    score_line = f"{cur_score:.1%}  (prev: {prev_score:.1%}, {sign}{delta:+.1%} {arrow})"
else:
    score_line = f"{cur_score:.1%}  (first run)"

# Suite delta table
table_rows = []
for name, suite in cur.get("suites", {}).items():
    score = suite.get("score", 1.0)
    skipped = suite.get("passed", 0) == 0 and suite.get("failed", 0) == 0
    score_s = "skipped" if skipped else f"{score:.1%}"
    delta_s = "—"
    if prev and name in prev.get("suites", {}) and not skipped:
        ps = prev["suites"][name].get("score", 0)
        d  = score - ps
        sign  = "+" if d >= 0 else ""
        arrow = "↑" if d > 0.001 else ("↓" if d < -0.001 else "→")
        delta_s = f"{sign}{d:.1%} {arrow}"
    table_rows.append(f"| {name} | {score_s} | {delta_s} |")

lines = []
lines.append(f"## {ts}  |  sha: `{sha}`  |  [results]({md_rel})")
lines.append("")
lines.append(f"**Score:** {score_line}")
lines.append("")
lines.append("| Suite | Score | Δ |")
lines.append("|-------|-------|---|")
lines.extend(table_rows)
lines.append("")
lines.append("> **Notes:** <!-- What was attempted. What changed. What worked or didn't. -->")
lines.append("> _No notes recorded — edit this entry to document the experiment._")
lines.append("")
lines.append("---")
lines.append("")

entry = "\n".join(lines)

# Ensure changelog exists with header
if not os.path.exists(changelog_path):
    with open(changelog_path, "w") as f:
        f.write("# Bench Changelog\n\n")
        f.write("Each entry links to a full metrics snapshot and has space for experiment notes.\n")
        f.write("Edit the **Notes** section of each entry to document what you tried and why.\n\n")
        f.write("---\n\n")

with open(changelog_path) as f:
    existing = f.read()

# Prepend new entry after the header block
header_end_marker = "---\n\n"
idx = existing.find(header_end_marker)
if idx == -1:
    insert_at = len(existing)
    prefix = existing
    suffix = ""
else:
    insert_at = idx + len(header_end_marker)
    prefix = existing[:insert_at]
    suffix = existing[insert_at:]

with open(changelog_path, "w") as f:
    f.write(prefix)
    f.write(entry)
    f.write(suffix)
PYEOF
}

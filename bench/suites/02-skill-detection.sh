# bench/suites/02-skill-detection.sh
# Suite: Skill Detection
# Tests whether skill-rules.json keyword patterns correctly route realistic prompts.
# Computes precision, recall, and F1 across the labeled fixture corpus.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "02 · Skill Detection"

SUITE_PASSED=0
SUITE_FAILED=0

RULES="$ROOT_DIR/.claude/skills/skill-rules.json"
FIXTURES="$ROOT_DIR/bench/fixtures/skill-prompts.json"

if [ ! -f "$RULES" ]; then
  print_fail "skill-rules.json not found — skipping suite"
  SUITE_JSON="{}"
  return 0
fi

if [ ! -f "$FIXTURES" ]; then
  print_skip "bench/fixtures/skill-prompts.json not found — skipping suite"
  print_info "Run: create bench/fixtures/skill-prompts.json to enable this suite"
  SUITE_JSON="{}"
  return 0
fi

# ── Run detection via python3 ─────────────────────────────────────────────────
RESULT_JSON=$(python3 - "$RULES" "$FIXTURES" <<'PYEOF'
import json, sys, re

rules_path, fixtures_path = sys.argv[1], sys.argv[2]

with open(rules_path) as f:
    rules = json.load(f)

with open(fixtures_path) as f:
    fixtures = json.load(f)

# Build keyword index: skill -> [keywords]
skill_keywords = {}
for skill, data in rules.items():
    skill_keywords[skill] = [kw.lower() for kw in data.get("keywords", [])]

results = []
global_tp = 0
global_fp = 0
global_fn = 0

for item in fixtures:
    prompt = item["prompt"].lower()
    expected = set(item.get("expected", []))
    prompt_id = item.get("id", "?")

    # Predict: any skill whose keyword appears as a substring of the prompt
    predicted = set()
    for skill, keywords in skill_keywords.items():
        for kw in keywords:
            if kw in prompt:
                predicted.add(skill)
                break

    tp = expected & predicted
    fp = predicted - expected
    fn = expected - predicted

    global_tp += len(tp)
    global_fp += len(fp)
    global_fn += len(fn)

    results.append({
        "id": prompt_id,
        "prompt": item["prompt"][:80],
        "expected": sorted(expected),
        "predicted": sorted(predicted),
        "tp": sorted(tp),
        "fp": sorted(fp),
        "fn": sorted(fn),
    })

precision = global_tp / (global_tp + global_fp) if (global_tp + global_fp) > 0 else 1.0
recall    = global_tp / (global_tp + global_fn) if (global_tp + global_fn) > 0 else 1.0
f1        = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0

# Find worst misses (most FNs)
worst = sorted(results, key=lambda r: len(r["fn"]), reverse=True)[:5]

# Per-skill aggregation
skill_stats = {}
for r in results:
    for skill in r["tp"]:
        skill_stats.setdefault(skill, {"tp": 0, "fp": 0, "fn": 0})["tp"] += 1
    for skill in r["fp"]:
        skill_stats.setdefault(skill, {"tp": 0, "fp": 0, "fn": 0})["fp"] += 1
    for skill in r["fn"]:
        skill_stats.setdefault(skill, {"tp": 0, "fp": 0, "fn": 0})["fn"] += 1

for skill, s in skill_stats.items():
    p  = s["tp"] / (s["tp"] + s["fp"]) if (s["tp"] + s["fp"]) > 0 else 1.0
    rc = s["tp"] / (s["tp"] + s["fn"]) if (s["tp"] + s["fn"]) > 0 else 1.0
    f1_s = 2 * p * rc / (p + rc) if (p + rc) > 0 else 0.0
    s.update({"precision": round(p, 3), "recall": round(rc, 3), "f1": round(f1_s, 3)})

print(json.dumps({
    "precision": round(precision, 4),
    "recall": round(recall, 4),
    "f1": round(f1, 4),
    "total_prompts": len(fixtures),
    "global_tp": global_tp,
    "global_fp": global_fp,
    "global_fn": global_fn,
    "worst_misses": worst,
    "per_skill": skill_stats,
    "all_results": results
}))
PYEOF
)

if [ -z "$RESULT_JSON" ]; then
  print_fail "Skill detection script returned no output"
  SUITE_JSON="{}"
  return 0
fi

# ── Parse and display results ─────────────────────────────────────────────────
python3 - "$RESULT_JSON" <<'PYEOF'
import json, sys

data = json.loads(sys.argv[1])

precision = data["precision"]
recall    = data["recall"]
f1        = data["f1"]
total     = data["total_prompts"]
tp        = data["global_tp"]
fp        = data["global_fp"]
fn        = data["global_fn"]
worst     = data["worst_misses"]
all_r     = data["all_results"]

def score_label(v):
    if v >= 0.85: return "good"
    if v >= 0.70: return "warn"
    return "bad"

# Per-prompt pass/fail
passed = sum(1 for r in all_r if len(r["fn"]) == 0 and len(r["fp"]) == 0)
total_prompts = len(all_r)

print(f"  Prompts tested: {total_prompts}")
print(f"  Perfect matches (no FP/FN): {passed}/{total_prompts}")
print(f"  TP={tp}  FP={fp}  FN={fn}")
print()

# Metrics with color hint via labels
labels = {
    "precision": score_label(precision),
    "recall": score_label(recall),
    "f1": score_label(f1),
}

metrics = [
    ("Precision", f"{precision:.1%}"),
    ("Recall",    f"{recall:.1%}"),
    ("F1 Score",  f"{f1:.1%}"),
]
for name, val in metrics:
    print(f"  {name}: {val}")

# Worst misses
if worst:
    print()
    print("  Worst misses (most undetected expected skills):")
    for r in worst:
        if r["fn"]:
            missed = ", ".join(r["fn"])
            print(f"    [{r['id']}] {r['prompt'][:60]}...")
            print(f"           missed: {missed}")

# Per-skill breakdown — sorted by F1 ascending (worst first)
per_skill = data.get("per_skill", {})
if per_skill:
    print()
    print("  Per-skill breakdown (sorted by F1, worst first):")
    print(f"  {'Skill':<30} {'P':>6} {'R':>6} {'F1':>6}  {'TP':>3} {'FP':>3} {'FN':>3}")
    print(f"  {'-'*30} {'------':>6} {'------':>6} {'------':>6}  {'---':>3} {'---':>3} {'---':>3}")
    sorted_skills = sorted(per_skill.items(), key=lambda x: x[1]["f1"])
    for skill, s in sorted_skills:
        flag = ""
        if s["precision"] < 0.5:
            flag += " ← low precision"
        if s["recall"] < 0.5:
            flag += " ← low recall"
        print(f"  {skill:<30} {s['precision']:>6.1%} {s['recall']:>6.1%} {s['f1']:>6.1%}  {s['tp']:>3} {s['fp']:>3} {s['fn']:>3}{flag}")
PYEOF

# ── Pass/Fail decisions ───────────────────────────────────────────────────────
PRECISION=$(echo "$RESULT_JSON" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['precision'])")
RECALL=$(echo "$RESULT_JSON"    | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['recall'])")
F1=$(echo "$RESULT_JSON"        | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['f1'])")
TOTAL_PROMPTS=$(echo "$RESULT_JSON" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['total_prompts'])")

# Thresholds (adjust as skill coverage improves)
_check() { python3 -c "import sys; sys.exit(0 if ($1) else 1)" 2>/dev/null; }

_check "$PRECISION >= 0.70" \
  && print_pass "Precision $(python3 -c "print(f'{$PRECISION:.1%}')" 2>/dev/null || echo $PRECISION) >= 70%" \
  || print_fail "Precision $(python3 -c "print(f'{$PRECISION:.1%}')" 2>/dev/null || echo $PRECISION) < 70%"

_check "$RECALL >= 0.70" \
  && print_pass "Recall $(python3 -c "print(f'{$RECALL:.1%}')" 2>/dev/null || echo $RECALL) >= 70%" \
  || print_fail "Recall $(python3 -c "print(f'{$RECALL:.1%}')" 2>/dev/null || echo $RECALL) < 70%"

_check "$F1 >= 0.70" \
  && print_pass "F1 $(python3 -c "print(f'{$F1:.1%}')" 2>/dev/null || echo $F1) >= 70%" \
  || print_fail "F1 $(python3 -c "print(f'{$F1:.1%}')" 2>/dev/null || echo $F1) < 70%"

_check "$TOTAL_PROMPTS >= 10" \
  && print_pass "Fixture corpus has $TOTAL_PROMPTS prompts (>= 10)" \
  || print_fail "Fixture corpus has only $TOTAL_PROMPTS prompts (< 10)"

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON=$(echo "$RESULT_JSON" | python3 -c "import json,sys; d=json.load(sys.stdin); print(json.dumps({'precision':d['precision'],'recall':d['recall'],'f1':d['f1'],'total_prompts':d['total_prompts'],'tp':d['global_tp'],'fp':d['global_fp'],'fn':d['global_fn']}))")

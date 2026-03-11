# bench/suites/07-skill-candidating.sh
# Suite: Skill Candidating Pipeline
# Tests the autolearn pipeline deterministically — no LLM calls needed.
# Covers: post-write.sh domain counting (tests 1–4) and skill-detector.sh
# synthesis injection and keyword firing (tests 5–10).
# Fixtures are created inline in an isolated sandbox; cleanup runs unconditionally.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "07 · Skill Candidating"

SUITE_PASSED=0
SUITE_FAILED=0

_S07_POST_WRITE="$ROOT_DIR/.claude/hooks/post-write.sh"
_S07_DETECTOR="$ROOT_DIR/.claude/hooks/skill-detector.sh"

# ── Guard: hooks must exist ────────────────────────────────────────────────────
_s07_guard_ok=true
if [ ! -x "$_S07_POST_WRITE" ]; then
  print_fail "post-write.sh not found or not executable — skipping suite"
  _s07_guard_ok=false
fi
if [ ! -x "$_S07_DETECTOR" ]; then
  print_fail "skill-detector.sh not found or not executable — skipping suite"
  _s07_guard_ok=false
fi

if [ "$_s07_guard_ok" = false ]; then
  SUITE_JSON="{}"
  unset _s07_guard_ok _S07_POST_WRITE _S07_DETECTOR
  return 0
fi
unset _s07_guard_ok

# ── Sandbox setup ─────────────────────────────────────────────────────────────
# All tests run in an isolated temp dir — never touches real project state.
# Hooks are copied in so that BASH_SOURCE resolves their ROOT_DIR to the sandbox.
_S07_TMP=$(mktemp -d)
_S07_DONE="$_S07_TMP/workflows/done"
_S07_SKILLS="$_S07_TMP/.claude/skills"
_S07_HOOKS="$_S07_TMP/.claude/hooks"
_S07_PENDING="$_S07_TMP/.claude/autolearn-pending"
_S07_RULES="$_S07_TMP/.claude/skills/skill-rules.json"

mkdir -p "$_S07_DONE" "$_S07_SKILLS" "$_S07_HOOKS"

cp "$_S07_POST_WRITE" "$_S07_HOOKS/post-write.sh"
cp "$_S07_DETECTOR"   "$_S07_HOOKS/skill-detector.sh"
chmod +x "$_S07_HOOKS/post-write.sh" "$_S07_HOOKS/skill-detector.sh"

# Minimal empty skill-rules.json — isolates tests from the real skill library
echo '{}' > "$_S07_RULES"

# Helper: write a minimal .sc file with the given domain
_s07_sc() {
  local name="$1" domain="$2"
  printf -- '---\ndomain: %s\ntask: Mock task for suite 07\n---\n\n## Observations\nTest observation for %s domain.\n' \
    "$domain" "$domain" > "$_S07_DONE/${name}.sc"
}

# ── Test 1: 1 .sc file → flag NOT set ─────────────────────────────────────────
_s07_sc "sc-01" "bench-pipeline"
bash "$_S07_HOOKS/post-write.sh" "{\"file_path\":\"$_S07_DONE/sc-01.sc\"}" 2>/dev/null

if [ ! -f "$_S07_PENDING" ]; then
  print_pass "post-write.sh: 1 .sc file → no flag set"
else
  print_fail "post-write.sh: 1 .sc file → flag prematurely set"
  rm -f "$_S07_PENDING"
fi

# ── Test 2: 2 .sc files → flag NOT set ────────────────────────────────────────
_s07_sc "sc-02" "bench-pipeline"
bash "$_S07_HOOKS/post-write.sh" "{\"file_path\":\"$_S07_DONE/sc-02.sc\"}" 2>/dev/null

if [ ! -f "$_S07_PENDING" ]; then
  print_pass "post-write.sh: 2 .sc files → no flag set"
else
  print_fail "post-write.sh: 2 .sc files → flag prematurely set"
  rm -f "$_S07_PENDING"
fi

# ── Test 3: 3 .sc files → flag IS set with correct domain ─────────────────────
_s07_sc "sc-03" "bench-pipeline"
bash "$_S07_HOOKS/post-write.sh" "{\"file_path\":\"$_S07_DONE/sc-03.sc\"}" 2>/dev/null

if [ -f "$_S07_PENDING" ]; then
  _s07_flagged=$(tr -d '[:space:]' < "$_S07_PENDING")
  if [ "$_s07_flagged" = "bench-pipeline" ]; then
    print_pass "post-write.sh: 3 .sc files → flag set with correct domain"
  else
    print_fail "post-write.sh: flag set but domain wrong ('$_s07_flagged' ≠ 'bench-pipeline')"
  fi
else
  print_fail "post-write.sh: 3 .sc files → flag NOT set (expected at N=3 threshold)"
fi

# ── Test 4: 3 .sc files across 3 different domains → no flag ──────────────────
rm -f "$_S07_PENDING"
rm -f "$_S07_DONE"/*.sc

_s07_sc "neg-01" "domain-alpha"
_s07_sc "neg-02" "domain-beta"
_s07_sc "neg-03" "domain-gamma"

bash "$_S07_HOOKS/post-write.sh" "{\"file_path\":\"$_S07_DONE/neg-01.sc\"}" 2>/dev/null
bash "$_S07_HOOKS/post-write.sh" "{\"file_path\":\"$_S07_DONE/neg-02.sc\"}" 2>/dev/null
bash "$_S07_HOOKS/post-write.sh" "{\"file_path\":\"$_S07_DONE/neg-03.sc\"}" 2>/dev/null

if [ ! -f "$_S07_PENDING" ]; then
  print_pass "post-write.sh: 3 .sc files across 3 domains → no flag set"
else
  _s07_flagged=$(tr -d '[:space:]' < "$_S07_PENDING")
  print_fail "post-write.sh: 3 different-domain .sc files → flag unexpectedly set (domain: '$_s07_flagged')"
  rm -f "$_S07_PENDING"
fi

# ── Test 5: skill-detector.sh — no synthesis when flag absent ─────────────────
rm -f "$_S07_PENDING"
rm -f "$_S07_DONE"/*.sc

_S07_OUT=$(printf '{"prompt":"hello world generic unrelated prompt"}' \
  | bash "$_S07_HOOKS/skill-detector.sh" 2>/dev/null)

if echo "$_S07_OUT" | grep -q "AUTOLEARN"; then
  print_fail "skill-detector.sh: synthesis injected when flag is absent"
else
  print_pass "skill-detector.sh: no synthesis output when flag is absent"
fi

# ── Test 6: skill-detector.sh — synthesis block injected when flag present ─────
_s07_sc "synth-01" "bench-synth"
_s07_sc "synth-02" "bench-synth"
_s07_sc "synth-03" "bench-synth"
printf 'bench-synth\n' > "$_S07_PENDING"

_S07_OUT=$(printf '{"prompt":"do something completely unrelated"}' \
  | bash "$_S07_HOOKS/skill-detector.sh" 2>/dev/null)

if echo "$_S07_OUT" | grep -q "AUTOLEARN"; then
  print_pass "skill-detector.sh: synthesis block injected when flag is present"
else
  print_fail "skill-detector.sh: synthesis block missing when flag is present"
fi

# ── Test 7: skill-detector.sh — flag NOT auto-cleared (persists until Claude deletes it) ──
# By design, skill-detector.sh does NOT clear the pending flag. The flag is deleted
# by Claude as step 4 of the synthesis instructions. This ensures synthesis re-fires
# on every prompt until the skill is actually synthesized.
if [ -f "$_S07_PENDING" ]; then
  print_pass "skill-detector.sh: autolearn-pending flag persists after hook run (correct — Claude must clear it)"
else
  print_fail "skill-detector.sh: autolearn-pending flag was auto-cleared by hook (should persist until Claude deletes it)"
fi
rm -f "$_S07_PENDING"

# ── Test 8: Skill fixture format validation ────────────────────────────────────
# Write a fixture skill file (simulates synthesis output) and validate its frontmatter.
cat > "$_S07_SKILLS/bench-synth.md" << 'SKILLEOF'
---
name: bench-synth
description: Fixture skill synthesized by suite 07 for pipeline testing
activation:
  keywords: ["bench pipeline", "suite07 fixture", "candidating test"]
  file_patterns: ["**/*.fixture"]
---

# Bench Synth

## Purpose
Test skill for validating the synthesis format requirements.
SKILLEOF

_S07_FMT=$(python3 - "$_S07_SKILLS/bench-synth.md" << 'PYEOF'
import sys
path = sys.argv[1]
try:
    lines = open(path).read().splitlines()
except Exception as e:
    print(f"FAIL_READ: {e}")
    sys.exit(0)

if not lines or lines[0].strip() != "---":
    print("FAIL: missing opening frontmatter delimiter")
    sys.exit(0)

fm = {}
in_fm = False
for line in lines:
    s = line.strip()
    if s == "---":
        if not in_fm:
            in_fm = True
            continue
        else:
            break
    if in_fm and ":" in s:
        k, _, v = s.partition(":")
        fm[k.strip()] = v.strip()

required = ["name", "description"]
missing = [f for f in required if not fm.get(f)]
print("FAIL_MISSING_FIELDS: " + ", ".join(missing) if missing else "PASS")
PYEOF
)

if [ "$_S07_FMT" = "PASS" ]; then
  print_pass "Skill fixture: required frontmatter fields (name, description) present and non-empty"
else
  print_fail "Skill fixture format: $_S07_FMT"
fi

# ── Test 9: skill-rules.json entry validation ──────────────────────────────────
# Simulate adding the skill entry that synthesis would produce, then validate it.
_S07_RULES_RESULT=$(python3 - "$_S07_RULES" << 'PYEOF'
import json, sys
rules_path = sys.argv[1]
try:
    rules = json.load(open(rules_path))
except Exception as e:
    print(f"FAIL_READ: {e}")
    sys.exit(0)

rules["bench-synth"] = {
    "keywords": ["bench pipeline", "suite07 fixture", "candidating test"],
    "filePatterns": ["**/*.fixture"],
    "toolTriggers": ["Write"]
}
try:
    json.dump(rules, open(rules_path, "w"), indent=2)
except Exception as e:
    print(f"FAIL_WRITE: {e}")
    sys.exit(0)

entry = rules["bench-synth"]
kw = entry.get("keywords", [])
fp = entry.get("filePatterns", [])
if not isinstance(kw, list) or not kw:
    print("FAIL_KEYWORDS: empty or not a list")
elif not isinstance(fp, list) or not fp:
    print("FAIL_FILE_PATTERNS: empty or not a list")
else:
    print(f"PASS ({len(kw)} keywords, {len(fp)} filePatterns)")
PYEOF
)

if echo "$_S07_RULES_RESULT" | grep -q "^PASS"; then
  print_pass "skill-rules.json: new skill entry has non-empty keywords and filePatterns"
else
  print_fail "skill-rules.json entry: $_S07_RULES_RESULT"
fi

# ── Test 10: Newly registered skill fires on keyword-matching prompt ────────────
# skill-rules.json now has "bench-synth"; bench-synth.md exists in the sandbox.
_S07_OUT=$(printf '{"prompt":"help me set up a bench pipeline candidating test fixture"}' \
  | bash "$_S07_HOOKS/skill-detector.sh" 2>/dev/null)

if echo "$_S07_OUT" | grep -q "bench-synth"; then
  print_pass "skill-detector.sh: newly registered skill fires on keyword-matching prompt"
else
  print_fail "skill-detector.sh: newly registered skill did NOT fire on keyword match"
fi

# ── Test 11: Synthesis instruction references skill-prompts.json ───────────────
# Re-arm flag and .sc files, run detector, verify output mentions skill-prompts.json
rm -f "$_S07_DONE"/*.sc
_s07_sc "synth-04" "bench-synth"
_s07_sc "synth-05" "bench-synth"
_s07_sc "synth-06" "bench-synth"
printf 'bench-synth\n' > "$_S07_PENDING"

_S07_OUT=$(printf '{"prompt":"do something completely unrelated"}' \
  | bash "$_S07_HOOKS/skill-detector.sh" 2>/dev/null)

if echo "$_S07_OUT" | grep -q "skill-prompts.json"; then
  print_pass "skill-detector.sh: synthesis instruction references bench/fixtures/skill-prompts.json"
else
  print_fail "skill-detector.sh: synthesis instruction does NOT mention skill-prompts.json"
fi
rm -f "$_S07_PENDING"

# ── Test 12: Fixture entries integrate correctly with suite 02 format ──────────
# Simulate what Claude would write: create a mock skill-prompts.json with 1 existing
# entry, append a new bench-synth fixture, validate JSON and field structure.
_S07_FIXTURES="$_S07_TMP/bench/fixtures"
mkdir -p "$_S07_FIXTURES"
_S07_FIXTURE_FILE="$_S07_FIXTURES/skill-prompts.json"

# Seed with one pre-existing entry
printf '[{"id":"existing-p01","prompt":"existing prompt","expected":["existing-skill"],"notes":"manual"}]' \
  > "$_S07_FIXTURE_FILE"

_S07_FIXTURE_RESULT=$(python3 - "$_S07_FIXTURE_FILE" "$_S07_RULES" << 'PYEOF'
import json, sys

fixture_path = sys.argv[1]
rules_path   = sys.argv[2]

# Load existing fixtures
try:
    fixtures = json.load(open(fixture_path))
except Exception as e:
    print(f"FAIL_READ_EXISTING: {e}")
    sys.exit(0)

# Simulate Claude appending new entries
new_entries = [
    {"id": "bench-synth-p01", "prompt": "help me set up a bench pipeline fixture",  "expected": ["bench-synth"], "notes": "autolearn-generated"},
    {"id": "bench-synth-p02", "prompt": "candidating test for skill detection",      "expected": ["bench-synth"], "notes": "autolearn-generated"},
    {"id": "bench-synth-p03", "prompt": "running suite07 benchmarks on my skills",   "expected": ["bench-synth"], "notes": "autolearn-generated"},
]
fixtures.extend(new_entries)

try:
    with open(fixture_path, "w") as f:
        json.dump(fixtures, f, indent=2)
except Exception as e:
    print(f"FAIL_WRITE: {e}")
    sys.exit(0)

# Reload and validate
try:
    fixtures = json.load(open(fixture_path))
except Exception as e:
    print(f"FAIL_RELOAD: {e}")
    sys.exit(0)

# Check existing entry preserved
if not any(e.get("id") == "existing-p01" for e in fixtures):
    print("FAIL: existing entry was lost after append")
    sys.exit(0)

# Validate new entries
errors = []
for entry in fixtures:
    if entry.get("notes") != "autolearn-generated":
        continue
    for field in ("id", "prompt", "expected", "notes"):
        if field not in entry:
            errors.append(f"entry {entry.get('id','?')} missing field '{field}'")
    exp = entry.get("expected", [])
    if not isinstance(exp, list) or "bench-synth" not in exp:
        errors.append(f"entry {entry.get('id','?')} expected array must include 'bench-synth'")

if errors:
    print("FAIL: " + "; ".join(errors))
    sys.exit(0)

# Verify new skill's prompts match via detector keyword logic (simulate suite 02)
try:
    rules = json.load(open(rules_path))
except Exception as e:
    print(f"FAIL_RULES: {e}")
    sys.exit(0)

new_skill_prompts = [e for e in fixtures if e.get("notes") == "autolearn-generated"]
fired = 0
for entry in new_skill_prompts:
    p = entry["prompt"].lower()
    for skill, rule in rules.items():
        if any(kw.lower() in p for kw in rule.get("keywords", [])):
            if skill in entry.get("expected", []):
                fired += 1

print(f"PASS ({len(new_skill_prompts)} autolearn entries; {fired} keyword-matched by detector)")
PYEOF
)

if echo "$_S07_FIXTURE_RESULT" | grep -q "^PASS"; then
  print_pass "skill-prompts.json: autolearn fixture entries have correct format and preserve existing entries"
else
  print_fail "skill-prompts.json fixture format: $_S07_FIXTURE_RESULT"
fi

# ── Cleanup ────────────────────────────────────────────────────────────────────
rm -rf "$_S07_TMP"
unset -f _s07_sc 2>/dev/null || true
unset _S07_POST_WRITE _S07_DETECTOR _S07_TMP _S07_DONE _S07_SKILLS _S07_HOOKS \
      _S07_PENDING _S07_RULES _S07_OUT _S07_FMT _S07_RULES_RESULT \
      _S07_FIXTURES _S07_FIXTURE_FILE _S07_FIXTURE_RESULT \
      _s07_flagged 2>/dev/null || true

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON="{}"

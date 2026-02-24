#!/usr/bin/env bash
# validate.sh - Validates the agentic infrastructure setup
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

PASSED=0
FAILED=0

pass() {
  echo "  PASS: $1"
  PASSED=$((PASSED + 1))
}

fail() {
  echo "  FAIL: $1"
  FAILED=$((FAILED + 1))
}

echo "=== Agentic Infrastructure Validation ==="
echo "Root: $ROOT_DIR"
echo ""

# --- 1. Directory structure ---
echo "[1] Directory structure"
for dir in workflows/ideas workflows/tasks workflows/done; do
  if [ -d "$ROOT_DIR/$dir" ]; then
    pass "$dir/ exists"
  else
    fail "$dir/ missing"
  fi
done

# --- 2. Settings JSON ---
echo "[2] Settings JSON"
SETTINGS="$ROOT_DIR/.claude/settings.json"
if [ -f "$SETTINGS" ]; then
  if python3 -m json.tool "$SETTINGS" > /dev/null 2>&1; then
    pass "settings.json exists and is valid JSON"
  else
    fail "settings.json exists but is NOT valid JSON"
  fi
else
  fail "settings.json not found"
fi

# --- 3. Hook scripts ---
echo "[3] Hook scripts"
HOOK_DIR="$ROOT_DIR/.claude/hooks"
if [ -d "$HOOK_DIR" ]; then
  for hook in "$HOOK_DIR"/*.sh; do
    [ -f "$hook" ] || continue
    name="$(basename "$hook")"
    if [ -x "$hook" ]; then
      pass "$name is executable"
    else
      fail "$name is NOT executable"
    fi
  done
else
  fail "hooks/ directory not found"
fi

# --- 4. Skill frontmatter ---
echo "[4] Skill frontmatter"
SKILL_DIR="$ROOT_DIR/.claude/skills"
if [ -d "$SKILL_DIR" ]; then
  for skill in "$SKILL_DIR"/*.md; do
    [ -f "$skill" ] || continue
    name="$(basename "$skill")"
    first_line="$(head -n 1 "$skill")"
    if [ "$first_line" = "---" ]; then
      pass "$name has YAML frontmatter"
    else
      fail "$name missing YAML frontmatter (first line: '$first_line')"
    fi
  done
else
  fail "skills/ directory not found"
fi

# --- 5. Skill rules JSON ---
echo "[5] Skill rules"
RULES="$SKILL_DIR/skill-rules.json"
if [ -f "$RULES" ]; then
  if python3 -m json.tool "$RULES" > /dev/null 2>&1; then
    pass "skill-rules.json exists and is valid JSON"
  else
    fail "skill-rules.json exists but is NOT valid JSON"
  fi
else
  fail "skill-rules.json not found"
fi

# --- 6. Skill coverage ---
echo "[6] Skill coverage (skills vs skill-rules.json)"
if [ -f "$RULES" ]; then
  RULES_KEYS="$(python3 -c "import json,sys; data=json.load(open(sys.argv[1])); print('\n'.join(sorted(data.keys())))" "$RULES")"
  MISSING_SKILLS=""
  for skill in "$SKILL_DIR"/*.md; do
    [ -f "$skill" ] || continue
    name="$(basename "$skill" .md)"
    if echo "$RULES_KEYS" | grep -qx "$name"; then
      : # covered
    else
      MISSING_SKILLS="$MISSING_SKILLS $name"
    fi
  done
  if [ -z "$MISSING_SKILLS" ]; then
    pass "All skill .md files have entries in skill-rules.json"
  else
    fail "Skills missing from skill-rules.json:$MISSING_SKILLS"
  fi
else
  fail "Cannot check coverage - skill-rules.json missing"
fi

# --- 7. Agent definitions ---
echo "[7] Agent definitions"
AGENT_DIR="$ROOT_DIR/.claude/agents"
if [ -d "$AGENT_DIR" ]; then
  count=0
  for agent in "$AGENT_DIR"/*.md; do
    [ -f "$agent" ] || continue
    count=$((count + 1))
  done
  if [ "$count" -gt 0 ]; then
    pass "$count agent definitions found in agents/"
  else
    fail "No .md files found in agents/"
  fi
else
  fail "agents/ directory not found"
fi

# --- 8. Commands ---
echo "[8] Commands"
CMD_DIR="$ROOT_DIR/.claude/commands"
if [ -d "$CMD_DIR" ]; then
  count=0
  for cmd in "$CMD_DIR"/*.md; do
    [ -f "$cmd" ] || continue
    count=$((count + 1))
  done
  if [ "$count" -gt 0 ]; then
    pass "$count command definitions found in commands/"
  else
    fail "No .md files found in commands/"
  fi
else
  fail "commands/ directory not found"
fi

# --- 9. Hook detector sync ---
echo "[9] Hook detector sync"
DETECTOR="$ROOT_DIR/.claude/hooks/skill-detector.sh"
if [ -f "$DETECTOR" ]; then
  DETECTOR_CONTENT="$(cat "$DETECTOR")"
  MISSING_FROM_DETECTOR=""
  for skill in "$SKILL_DIR"/*.md; do
    [ -f "$skill" ] || continue
    name="$(basename "$skill" .md)"
    if echo "$DETECTOR_CONTENT" | grep -q "$name"; then
      : # found
    else
      MISSING_FROM_DETECTOR="$MISSING_FROM_DETECTOR $name"
    fi
  done
  if [ -z "$MISSING_FROM_DETECTOR" ]; then
    pass "All skills are referenced in skill-detector.sh"
  else
    fail "Skills missing from skill-detector.sh:$MISSING_FROM_DETECTOR"
  fi
else
  fail "skill-detector.sh not found"
fi

# --- Summary ---
echo ""
echo "=== Summary: $PASSED checks passed, $FAILED checks failed ==="
if [ "$FAILED" -gt 0 ]; then
  exit 1
fi

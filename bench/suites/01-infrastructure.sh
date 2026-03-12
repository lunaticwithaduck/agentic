# bench/suites/01-infrastructure.sh
# Suite: Infrastructure
# Validates the agentic directory structure, configs, and file integrity.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "01 · Infrastructure"

SUITE_PASSED=0
SUITE_FAILED=0

# ── Required directories ───────────────────────────────────────────────────────
for dir in \
  workflows/ideas \
  workflows/tasks \
  workflows/done \
  .claude/skills \
  .claude/hooks \
  .claude/agents \
  .claude/commands \
  .claude/scripts; do
  if [ -d "$ROOT_DIR/$dir" ]; then
    print_pass "$dir/ exists"
  else
    print_fail "$dir/ missing"
  fi
done

# ── settings.json ─────────────────────────────────────────────────────────────
SETTINGS="$ROOT_DIR/.claude/settings.json"
if [ -f "$SETTINGS" ]; then
  if python3 -m json.tool "$SETTINGS" > /dev/null 2>&1; then
    print_pass "settings.json is valid JSON"
  else
    print_fail "settings.json exists but is NOT valid JSON"
  fi
  # Check all 4 hook types are present
  for hook_type in UserPromptSubmit PreToolUse PostToolUse Stop; do
    if python3 -c "import json,sys; d=json.load(open(sys.argv[1])); assert '$hook_type' in d.get('hooks',{})" "$SETTINGS" 2>/dev/null; then
      print_pass "settings.json has $hook_type hook"
    else
      print_fail "settings.json missing $hook_type hook"
    fi
  done
else
  print_fail "settings.json not found"
fi

# ── Hook scripts ──────────────────────────────────────────────────────────────
HOOK_DIR="$ROOT_DIR/.claude/hooks"
for hook in skill-detector.sh block-secrets.sh post-write.sh post-stop.sh; do
  hook_path="$HOOK_DIR/$hook"
  if [ -f "$hook_path" ]; then
    if [ -x "$hook_path" ]; then
      print_pass "$hook is executable"
    else
      print_fail "$hook exists but is NOT executable"
    fi
  else
    print_fail "$hook not found"
  fi
done

# ── skill-rules.json ──────────────────────────────────────────────────────────
RULES="$ROOT_DIR/.claude/skills/skill-rules.json"
if [ -f "$RULES" ]; then
  if python3 -m json.tool "$RULES" > /dev/null 2>&1; then
    RULES_COUNT=$(python3 -c "import json; d=json.load(open('$RULES')); print(len(d))")
    print_pass "skill-rules.json is valid JSON ($RULES_COUNT skills)"
  else
    print_fail "skill-rules.json is NOT valid JSON"
  fi
else
  print_fail "skill-rules.json not found"
fi

# ── Skill .md files have YAML frontmatter ─────────────────────────────────────
SKILL_DIR="$ROOT_DIR/.claude/skills"
skills_ok=0
skills_bad=0
for skill in "$SKILL_DIR"/*.md; do
  [ -f "$skill" ] || continue
  first_line="$(head -n 1 "$skill")"
  if [ "$first_line" = "---" ]; then
    skills_ok=$((skills_ok + 1))
  else
    print_fail "$(basename "$skill") missing YAML frontmatter"
    skills_bad=$((skills_bad + 1))
  fi
done
if [ "$skills_bad" -eq 0 ] && [ "$skills_ok" -gt 0 ]; then
  print_pass "All $skills_ok skill .md files have YAML frontmatter"
elif [ "$skills_bad" -gt 0 ]; then
  : # individual failures already printed
else
  print_fail "No skill .md files found"
fi

# ── Skills coverage: every .md has a rule, every rule has a .md ───────────────
if [ -f "$RULES" ]; then
  result=$(python3 - "$SKILL_DIR" "$RULES" <<'PYEOF'
import json, os, sys
skill_dir, rules_path = sys.argv[1], sys.argv[2]
with open(rules_path) as f:
    rules = set(json.load(f).keys())
mds = {os.path.splitext(f)[0] for f in os.listdir(skill_dir) if f.endswith(".md")}
missing_rules = sorted(mds - rules)
orphan_rules = sorted(rules - mds)
if missing_rules:
    print(f"FAIL_MD_NO_RULE: {', '.join(missing_rules)}")
if orphan_rules:
    print(f"FAIL_RULE_NO_MD: {', '.join(orphan_rules)}")
if not missing_rules and not orphan_rules:
    print("PASS")
PYEOF
)
  if [ "$result" = "PASS" ]; then
    print_pass "All skills have both a .md file and a skill-rules.json entry"
  else
    while IFS= read -r line; do
      print_fail "Coverage gap: $line"
    done <<< "$result"
  fi
fi

# ── skill-detector hook exists ────────────────────────────────────────────────
# The detector reads skill-rules.json dynamically — no skill names are hardcoded.
# Coverage is already verified above (all skills have a skill-rules.json entry).
# Here we just confirm the detector file exists (.js or .sh).
if [ -f "$ROOT_DIR/.claude/hooks/skill-detector.js" ] || [ -f "$ROOT_DIR/.claude/hooks/skill-detector.sh" ]; then
  print_pass "skill-detector hook exists (coverage via skill-rules.json)"
else
  print_fail "skill-detector hook not found (.js or .sh)"
fi

# ── Agent count ───────────────────────────────────────────────────────────────
AGENT_DIR="$ROOT_DIR/.claude/agents"
agent_count=$(find "$AGENT_DIR" -name "*.md" 2>/dev/null | wc -l | tr -d ' ')
if [ "$agent_count" -ge 3 ]; then
  print_pass "$agent_count agent definitions found"
else
  print_fail "Only $agent_count agent definitions (expected >= 3)"
fi

# ── Command count ─────────────────────────────────────────────────────────────
CMD_DIR="$ROOT_DIR/.claude/commands"
cmd_count=$(find "$CMD_DIR" -name "*.md" 2>/dev/null | wc -l | tr -d ' ')
if [ "$cmd_count" -ge 5 ]; then
  print_pass "$cmd_count command definitions found"
else
  print_fail "Only $cmd_count command definitions (expected >= 5)"
fi

# ── validate.sh exits 0 ───────────────────────────────────────────────────────
VALIDATE="$ROOT_DIR/.claude/scripts/validate.sh"
if [ -f "$VALIDATE" ] && [ -x "$VALIDATE" ]; then
  if bash "$VALIDATE" > /dev/null 2>&1; then
    print_pass "validate.sh exits 0"
  else
    print_fail "validate.sh exits non-zero (run it manually for details)"
  fi
else
  print_skip "validate.sh not found or not executable"
fi

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON="{}"

#!/usr/bin/env bash
# skill-detector.sh - UserPromptSubmit hook for deterministic skill pre-filtering
#
# Reads the user prompt from stdin JSON, matches against skill-rules.json keywords,
# and injects matched skill content directly as context.
#
# Outputs NOTHING if no skills match — zero overhead for non-domain prompts.
# Outputs matched skill content directly — no AI evaluation, no Skill tool calls.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

RULES="$ROOT_DIR/.claude/skills/skill-rules.json"
SKILL_DIR="$ROOT_DIR/.claude/skills"

# Read full stdin (prompt JSON)
INPUT=$(cat 2>/dev/null)
[ -z "$INPUT" ] && exit 0

# Require python3 and skill-rules.json — fail silently, don't break the session
command -v python3 >/dev/null 2>&1 || exit 0
[ -f "$RULES" ] || exit 0

# Write INPUT to a temp file to avoid shell-escaping issues with arbitrary prompt text
_TMP=$(mktemp)
printf '%s' "$INPUT" > "$_TMP"

ROOT_DIR="$ROOT_DIR" RULES="$RULES" SKILL_DIR="$SKILL_DIR" TMP_INPUT="$_TMP" \
python3 << 'PYEOF'
import json, sys, os

root_dir   = os.environ["ROOT_DIR"]
rules_path = os.environ["RULES"]
skills_dir = os.environ["SKILL_DIR"]
tmp_path   = os.environ["TMP_INPUT"]

try:
    raw = open(tmp_path).read()
    os.unlink(tmp_path)
    data = json.loads(raw)
    prompt = (
        data.get("prompt") or
        data.get("message") or
        data.get("user_prompt") or
        ""
    )
    if not prompt:
        sys.exit(0)
except Exception:
    sys.exit(0)

try:
    rules = json.load(open(rules_path))
except Exception:
    sys.exit(0)

prompt_lower = prompt.lower()

matched = []
for skill_name, rule in rules.items():
    if any(kw.lower() in prompt_lower for kw in rule.get("keywords", [])):
        matched.append(skill_name)

if not matched:
    sys.exit(0)

# Inject matched skill content directly — no AI evaluation step needed
parts = []
for skill_name in matched:
    path = os.path.join(skills_dir, f"{skill_name}.md")
    try:
        content = open(path).read()
        parts.append(
            f"[AUTO-ACTIVATED SKILL: {skill_name}]\n{content}\n[END SKILL: {skill_name}]"
        )
    except Exception:
        pass

if parts:
    label = "skill was" if len(parts) == 1 else "skills were"
    print(f"The following {label} automatically activated based on your prompt. Apply the domain knowledge in your response:\n")
    print("\n\n".join(parts))
PYEOF

exit 0

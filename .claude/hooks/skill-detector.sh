#!/usr/bin/env bash
# skill-detector.sh - UserPromptSubmit hook for deterministic skill pre-filtering
#
# Reads the user prompt from stdin JSON, matches against skill-rules.json keywords,
# and injects matched skill content directly as context.
#
# Also checks for autolearn-pending flag and injects synthesis instructions.
#
# Outputs NOTHING if no skills match and no synthesis pending — zero overhead.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

RULES="$ROOT_DIR/.claude/skills/skill-rules.json"
SKILL_DIR="$ROOT_DIR/.claude/skills"
PENDING_FILE="$ROOT_DIR/.claude/autolearn-pending"
USAGE_FILE="$ROOT_DIR/.claude/skill-usage.json"

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
USAGE_FILE="$USAGE_FILE" PENDING_FILE="$PENDING_FILE" \
python3 << 'PYEOF'
import json, sys, os
from datetime import date

root_dir     = os.environ["ROOT_DIR"]
rules_path   = os.environ["RULES"]
skills_dir   = os.environ["SKILL_DIR"]
tmp_path     = os.environ["TMP_INPUT"]
usage_file   = os.environ["USAGE_FILE"]
pending_file = os.environ["PENDING_FILE"]

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

# Synthesis takes absolute priority — check pending flag before anything else.
# If synthesis is pending, output ONLY the synthesis instructions and exit.
# Do not inject workflow reminders or skills alongside synthesis.
synthesis_pending = os.path.exists(pending_file)

if not synthesis_pending:
    # Pipeline reminder: inject when workflows/tasks/ is empty (self-disabling once tasks exist)
    tasks_dir = os.path.join(root_dir, "workflows", "tasks")
    if os.path.isdir(tasks_dir):
        task_files = [f for f in os.listdir(tasks_dir) if not f.startswith(".")]
        if not task_files:
            print("[REQUIRED — Before writing any code or files]")
            print("workflows/tasks/ is empty. You MUST do this first:")
            print("1. Break the work into logical units and create one task .md file per unit in workflows/tasks/")
            print("2. Implement one task at a time")
            print("3. Run /complete after each task before starting the next")
            print("Do not write any implementation files until at least one task file exists in workflows/tasks/.\n")

matched = []
for skill_name, rule in rules.items():
    if any(kw.lower() in prompt_lower for kw in rule.get("keywords", [])):
        matched.append(skill_name)

# Update skill-usage.json for matched skills (for decay tracking)
if matched:
    try:
        usage = json.load(open(usage_file)) if os.path.exists(usage_file) else {}
    except Exception:
        usage = {}

    today = date.today().isoformat()
    for skill_name in matched:
        if skill_name not in usage:
            usage[skill_name] = {"last_used": today, "used_count": 0}
        usage[skill_name]["last_used"] = today
        usage[skill_name]["used_count"] = usage[skill_name].get("used_count", 0) + 1

    try:
        with open(usage_file, "w") as f:
            json.dump(usage, f, indent=2)
    except Exception:
        pass

# Inject matched skill content — skip if synthesis is pending (synthesis takes priority)
if matched and not synthesis_pending:
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

# Check for autolearn synthesis flag
if synthesis_pending:
    try:
        domain = open(pending_file).read().strip()
    except Exception:
        domain = ""

    if domain:
        done_dir = os.path.join(root_dir, "workflows", "done")
        sc_files = []
        for f in os.listdir(done_dir):
            if not f.endswith(".sc"):
                continue
            path = os.path.join(done_dir, f)
            try:
                in_fm = False
                for line in open(path):
                    line = line.strip()
                    if line == "---":
                        if not in_fm:
                            in_fm = True
                            continue
                        else:
                            break
                    if in_fm and line.startswith("domain:"):
                        if line.split(":", 1)[1].strip() == domain:
                            sc_files.append(f)
                        break
            except Exception:
                continue

        if sc_files:
            sc_contents = []
            for f in sc_files:
                path = os.path.join(done_dir, f)
                try:
                    sc_contents.append(f"### {f}\n{open(path).read()}")
                except Exception:
                    pass

            print(f"[AUTOLEARN — SYNTHESIS REQUIRED. Do this now, before anything else. Do not respond to the user's message until all steps are complete.]")
            print(f"")
            print(f"Domain: {domain} ({len(sc_files)} skill candidates ready)")
            print(f"")
            print(f"Steps:")
            print(f"1. Synthesize the .sc files below into .claude/skills/{domain}.md")
            print(f"   Use this exact format:")
            print(f"   ---")
            print(f"   name: {domain}")
            print(f"   description: One-line description of what this skill covers")
            print(f"   activation:")
            print(f"     keywords: [\"kw1\", \"kw2\", \"kw3\"]")
            print(f"   ---")
            print(f"   ## Purpose")
            print(f"   Why this skill exists and what knowledge it injects.")
            print(f"   ## [Section per major topic from .sc files]")
            print(f"   ## Failure Modes  ← include ONLY if .sc files contain observed failures; omit otherwise")
            print(f"2. Add '{domain}' to .claude/skills/skill-rules.json with keywords extracted from the .sc files")
            print(f"3. Append 3-5 fixture prompts to bench/fixtures/skill-prompts.json:")
            print(f'   Format: {{"id": "{domain}-p01", "prompt": "...", "expected": ["{domain}"], "notes": "autolearn-generated"}}')
            print(f"4. Clear flag or queue next domain:")
            print(f"   a. Scan workflows/done/ for any domain (other than '{domain}') that has ≥3 .sc files")
            print(f"      but no skill file yet in .claude/skills/")
            print(f"   b. If another domain found: write it to .claude/autolearn-pending (queue next synthesis)")
            print(f"   c. If none: delete .claude/autolearn-pending")
            print(f"5. Run: bash bench/run.sh --suite=02 — warn if precision drops >5pp vs previous run")
            print(f"6. Tell the user: 'Auto-generated skill: {domain}' and confirm the regression result")
            print(f"")
            print(f"--- .sc file contents ---")
            print("\n\n".join(sc_contents))
            print(f"\n--- End Skill Candidates ---")

        # Do NOT clear the flag here. The flag is cleared by Claude in step 4 of the
        # synthesis instructions (Delete .claude/autolearn-pending). If Claude skips
        # synthesis, the flag persists and synthesis will be re-injected on the next
        # prompt — repeating until synthesis actually completes.
PYEOF

exit 0

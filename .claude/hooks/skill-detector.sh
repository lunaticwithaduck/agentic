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

# Inject matched skill content directly — no AI evaluation step needed
if matched:
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
if os.path.exists(pending_file):
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

            print(f"\n\n[AUTOLEARN: SKILL SYNTHESIS REQUIRED]")
            print(f"Domain '{domain}' has reached {len(sc_files)} skill candidates. Synthesize them into a skill now.")
            print(f"\nBefore proceeding with the user's request, do the following:")
            print(f"1. Read the {len(sc_files)} .sc files below")
            print(f"2. Synthesize the extracted knowledge into a single skill file")
            print(f"3. Write the skill to .claude/skills/{domain}.md (use the standard skill format)")
            print(f"   - Include a '## Failure Modes' section if any .sc files contain failure observations")
            print(f"   - If no failures are documented yet, omit the section entirely — do not invent anti-patterns")
            print(f"4. Add an entry to .claude/skills/skill-rules.json with keywords from the .sc files")
            print(f"5. Generate fixture prompts for the new skill and append them to bench/fixtures/skill-prompts.json:")
            print(f"   - Write 3-5 prompts a developer would genuinely type when they need this skill")
            print(f"   - Use natural, casual phrasing — how a developer actually writes, not formal domain jargon")
            print(f"   - Format each entry as:")
            print(f'     {{"id": "{domain}-p01", "prompt": "...", "expected": ["{domain}"], "notes": "autolearn-generated"}}')
            print(f"   - Use sequential IDs: {domain}-p01, {domain}-p02, {domain}-p03, etc.")
            print(f"   - If bench/fixtures/skill-prompts.json exists: read it, append the new entries, write back the full array")
            print(f"   - If bench/fixtures/skill-prompts.json does not exist: create it as a JSON array of just the new entries")
            print(f"   - Aim for variety: different task types, phrasings, and complexity levels within the domain")
            print(f"6. Delete the file .claude/autolearn-pending")
            print(f"7. Tell the user: 'Auto-generated skill: {domain} (from {len(sc_files)} skill candidates)' and report how many fixture prompts were added to bench/fixtures/skill-prompts.json")
            print(f"8. Run a Suite 02 precision regression check:")
            print(f"   - Run: bash bench/run.sh --suite=02")
            print(f"   - Find the most recent previous metrics file in bench/results/metrics/ (second-newest by timestamp)")
            print(f"   - Compare the new Suite 02 precision to the previous run's precision")
            print(f"   - If precision dropped more than 5 percentage points: warn the user that the new '{domain}' skill")
            print(f"     may be causing keyword pollution — its keywords may be too broad and firing on unrelated prompts")
            print(f"   - If precision held or improved: confirm the skill passed the regression check")
            print(f"\n--- Skill Candidate Contents ---\n")
            print("\n\n".join(sc_contents))
            print(f"\n--- End Skill Candidates ---")

        # Clear the flag regardless (even if no .sc files found)
        try:
            os.unlink(pending_file)
        except Exception:
            pass
PYEOF

exit 0

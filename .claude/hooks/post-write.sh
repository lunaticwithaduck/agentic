#!/usr/bin/env bash
# post-write.sh - PostToolUse hook for Write and Edit operations
# Runs after files are written or edited.
# - Validates JSON files automatically
# - Detects .sc (skill candidate) files and flags domains for synthesis at N=3

# Accept tool input from $1 (command-line arg) OR stdin — handles both Claude Code delivery modes
TOOL_INPUT="${1:-}"
if [ -z "$TOOL_INPUT" ]; then
  TOOL_INPUT=$(cat 2>/dev/null)
fi

# Extract file_path from the tool input JSON (handles both Write and Edit)
FILE_PATH=""
if [ -n "$TOOL_INPUT" ]; then
  FILE_PATH=$(echo "$TOOL_INPUT" | python3 -c "
import json, sys
try:
    d = json.loads(sys.stdin.read())
    print(d.get('file_path', ''))
except Exception:
    pass
" 2>/dev/null)
fi

[ -z "$FILE_PATH" ] && exit 0
[ -f "$FILE_PATH" ] || exit 0

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Validate JSON files after write
case "$FILE_PATH" in
  *.json)
    if ! python3 -m json.tool "$FILE_PATH" > /dev/null 2>&1; then
      echo "WARNING: $FILE_PATH is not valid JSON" >&2
    fi
    ;;
esac

# Skill candidating: detect .sc file writes in workflows/done/
# Match both absolute (/path/to/workflows/done/foo.sc) and relative (workflows/done/foo.sc)
case "$FILE_PATH" in
  *workflows/done/*.sc)
    command -v python3 >/dev/null 2>&1 || exit 0

    DONE_DIR="$ROOT_DIR/workflows/done"
    PENDING_FILE="$ROOT_DIR/.claude/autolearn-pending"
    THRESHOLD=3

    # Extract domain from the newly written .sc file, count matching .sc files
    python3 << PYEOF
import os, sys

sc_path = "$FILE_PATH"
# Normalize to absolute path (in case agent used a relative path)
if not os.path.isabs(sc_path):
    sc_path = os.path.join("$ROOT_DIR", sc_path)
done_dir = "$DONE_DIR"
pending_file = "$PENDING_FILE"
threshold = $THRESHOLD

# Extract domain from the new .sc file
domain = None
try:
    in_frontmatter = False
    for line in open(sc_path):
        line = line.strip()
        if line == "---":
            if not in_frontmatter:
                in_frontmatter = True
                continue
            else:
                break
        if in_frontmatter and line.startswith("domain:"):
            domain = line.split(":", 1)[1].strip()
            break
except Exception:
    sys.exit(0)

if not domain:
    sys.exit(0)

# Count .sc files with this same domain
count = 0
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
                    count += 1
                break
    except Exception:
        continue

if count >= threshold:
    with open(pending_file, "w") as f:
        f.write(domain + "\n")
PYEOF
    ;;
esac

exit 0

#!/usr/bin/env bash
# post-write.sh - PostToolUse hook for Write and Edit operations
# Runs after files are written or edited. Validates JSON files automatically.

TOOL_INPUT="${1:-}"

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

# Validate JSON files after write
case "$FILE_PATH" in
  *.json)
    if ! python3 -m json.tool "$FILE_PATH" > /dev/null 2>&1; then
      echo "WARNING: $FILE_PATH is not valid JSON" >&2
    fi
    ;;
esac

exit 0

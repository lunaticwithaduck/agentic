#!/usr/bin/env bash
# post-write.sh - PostToolUse hook for Write/Edit operations
# Runs after any file is written or edited.
#
# This script receives the tool input as a JSON string via $1.
# Use this to trigger linting, formatting, type-checking, or other validation.
#
# Exit codes:
#   0 - Success (no issues)
#   1 - Warning (shown to Claude but does not block)
#   2 - Error (blocks and asks Claude to fix)

set -euo pipefail

TOOL_INPUT="${1:-}"

if [ -z "$TOOL_INPUT" ]; then
  exit 0
fi

# Extract the file path from the JSON input
FILE_PATH=$(echo "$TOOL_INPUT" | grep -oP '"file_path"\s*:\s*"([^"]*)"' | sed 's/"file_path"\s*:\s*"//;s/"$//' || true)

if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# Get the file extension
EXT="${FILE_PATH##*.}"

# TODO: Customize - Add your project-specific validation below
# Examples:
#
# TypeScript/JavaScript linting:
# if [[ "$EXT" == "ts" || "$EXT" == "tsx" || "$EXT" == "js" || "$EXT" == "jsx" ]]; then
#   npx eslint --fix "$FILE_PATH" 2>/dev/null || true
# fi
#
# Python formatting:
# if [[ "$EXT" == "py" ]]; then
#   ruff check --fix "$FILE_PATH" 2>/dev/null || true
#   ruff format "$FILE_PATH" 2>/dev/null || true
# fi
#
# Rust formatting:
# if [[ "$EXT" == "rs" ]]; then
#   rustfmt "$FILE_PATH" 2>/dev/null || true
# fi
#
# Shell script validation:
# if [[ "$EXT" == "sh" ]]; then
#   shellcheck "$FILE_PATH" 2>/dev/null || true
# fi

# Check for accidental secret content in written files
if grep -qiE '(AKIA[A-Z0-9]{16}|sk-[a-zA-Z0-9]{48}|ghp_[a-zA-Z0-9]{36})' "$FILE_PATH" 2>/dev/null; then
  echo "WARNING: File $FILE_PATH may contain hardcoded secrets (API keys detected)."
  echo "Please remove any secrets and use environment variables instead."
  exit 2
fi

exit 0

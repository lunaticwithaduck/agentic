#!/usr/bin/env bash
# block-secrets.sh - PreToolUse hook for Bash commands
# Blocks commands that would expose secrets, credentials, or sensitive files.
#
# This script receives the tool input as a JSON string via $1.
# It exits with code 2 and a message to block the command,
# or exits with code 0 to allow it.

set -euo pipefail

TOOL_INPUT="${1:-}"
if [ -z "$TOOL_INPUT" ]; then
  TOOL_INPUT=$(cat 2>/dev/null)
fi

if [ -z "$TOOL_INPUT" ]; then
  exit 0
fi

# Extract the command from the JSON input
# Claude Code wraps tool params under "tool_input"; fall back to flat for older versions
COMMAND=$(echo "$TOOL_INPUT" | python3 -c "
import json, sys
try:
    d = json.loads(sys.stdin.read())
    payload = d.get('tool_input', d)
    print(payload.get('command', ''))
except Exception:
    pass
" 2>/dev/null)

if [ -z "$COMMAND" ]; then
  exit 0
fi

# Safe .env suffixes: template/example/schema files are not secrets.
# Allow these through before applying the broad .env block pattern.
SAFE_ENV_VARIANTS='\.env\.(example|sample|template|schema|type|dist|template\.local)'
if echo "$COMMAND" | grep -qiE "$SAFE_ENV_VARIANTS"; then
  exit 0
fi

# Patterns that indicate secret/credential exposure
BLOCKED_PATTERNS=(
  'cat.*\.env'
  'cat.*credentials'
  'cat.*\.pem'
  'cat.*\.key'
  'cat.*secret'
  'echo.*\$.*PASSWORD'
  'echo.*\$.*SECRET'
  'echo.*\$.*TOKEN'
  'echo.*\$.*API_KEY'
  'printenv'
  '^env$'
  'cat.*/etc/shadow'
  'cat.*/etc/passwd'
  'cat.*id_rsa'
  'cat.*id_ed25519'
  'base64.*\.env'
  'base64.*\.pem'
  'base64.*\.key'
  'curl.*-d.*password'
  'curl.*-d.*secret'
  'curl.*-d.*token'
)

for pattern in "${BLOCKED_PATTERNS[@]}"; do
  if echo "$COMMAND" | grep -qiE "$pattern"; then
    echo "BLOCKED: Command appears to access secrets or credentials."
    echo "Pattern matched: $pattern"
    echo "If this is intentional, run the command manually outside Claude Code."
    exit 2
  fi
done

# Block git commands that would commit secret files
COMMIT_SECRET_PATTERNS=(
  'git add.*\.env'
  'git add.*credentials'
  'git add.*\.pem'
  'git add.*\.key'
  'git add -A'
  'git add \.[[:space:]]*$'
)

for pattern in "${COMMIT_SECRET_PATTERNS[@]}"; do
  if echo "$COMMAND" | grep -qiE "$pattern"; then
    echo "BLOCKED: Command may stage secret files."
    echo "Pattern matched: $pattern"
    echo "Use specific file names with git add instead."
    exit 2
  fi
done

exit 0

#!/usr/bin/env bash
# build-cursor.sh — builds ship/cursor/ distribution target
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# If REPO_ROOT is not set by parent build.sh, derive it from script location
REPO_ROOT="${REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
SHIP_DIR="${REPO_ROOT}/ship/cursor"
BUILD_SRC="${REPO_ROOT}/buildScripts/src"
BUILD_LIB="${REPO_ROOT}/buildScripts/lib"
CLAUDE_DIR="${REPO_ROOT}/.claude"

echo "  [cursor] Repo root : ${REPO_ROOT}"
echo "  [cursor] Ship dir  : ${SHIP_DIR}"

# ---------------------------------------------------------------------------
# 1. Create directory structure
# ---------------------------------------------------------------------------
echo "  [cursor] Creating directory structure..."
mkdir -p \
  "${SHIP_DIR}/.cursor/hooks" \
  "${SHIP_DIR}/.cursor/rules" \
  "${SHIP_DIR}/.cursor/commands" \
  "${SHIP_DIR}/workflows/ideas" \
  "${SHIP_DIR}/workflows/tasks" \
  "${SHIP_DIR}/workflows/done" \
  "${SHIP_DIR}/workflows/problems"

# ---------------------------------------------------------------------------
# 2. Copy shared JS hooks from .claude/hooks/
# ---------------------------------------------------------------------------
echo "  [cursor] Copying shared hooks..."
for hook in block-secrets.cjs post-write.cjs post-stop.cjs; do
  src="${CLAUDE_DIR}/hooks/${hook}"
  if [ -f "${src}" ]; then
    cp "${src}" "${SHIP_DIR}/.cursor/hooks/${hook}"
    echo "  [cursor]   Copied ${hook}"
  else
    echo "  [cursor]   WARNING: ${hook} not found at ${src}"
  fi
done

# ---------------------------------------------------------------------------
# 3. Copy Cursor-specific hooks from buildScripts/src/cursor-hooks/
# ---------------------------------------------------------------------------
echo "  [cursor] Copying Cursor-specific hooks..."
for hook in cursor-skill-injector.cjs cursor-session-start.cjs; do
  src="${BUILD_SRC}/cursor-hooks/${hook}"
  if [ -f "${src}" ]; then
    cp "${src}" "${SHIP_DIR}/.cursor/hooks/${hook}"
    echo "  [cursor]   Copied ${hook}"
  else
    echo "  [cursor]   ERROR: ${hook} not found at ${src}"
    exit 1
  fi
done

# ---------------------------------------------------------------------------
# 4. Generate hooks.json
# ---------------------------------------------------------------------------
echo "  [cursor] Generating hooks.json..."
node "${BUILD_LIB}/generate-hooks-json.js" \
  "${SHIP_DIR}/.cursor/hooks.json"

# Verify it is valid JSON
node -e "JSON.parse(require('fs').readFileSync('${SHIP_DIR}/.cursor/hooks.json', 'utf8')); console.log('  [cursor]   hooks.json is valid JSON');"

# ---------------------------------------------------------------------------
# 5. Convert skill-creator to Cursor rule format
# ---------------------------------------------------------------------------
echo "  [cursor] Converting skill-creator skill..."
node "${BUILD_LIB}/convert-skill.js" \
  "${CLAUDE_DIR}/skills/skill-creator.md" \
  "${SHIP_DIR}/.cursor/rules/skill-creator.mdc"

# ---------------------------------------------------------------------------
# 6. Generate skill-index.md — only include skills that actually ship
# ---------------------------------------------------------------------------
echo "  [cursor] Generating skill-index.mdc (shipped skills only)..."
# Build a filtered skill-rules.json containing only the rules that have a
# corresponding .mdc file in the output rules directory.
_SHIPPED_RULES=$(node -e "
  const fs = require('fs');
  const path = require('path');
  const rulesDir = '${SHIP_DIR}/.cursor/rules';
  const allRules = JSON.parse(fs.readFileSync('${CLAUDE_DIR}/skills/skill-rules.json', 'utf8'));
  const shipped = {};
  for (const [name, rule] of Object.entries(allRules)) {
    if (fs.existsSync(path.join(rulesDir, name + '.mdc'))) {
      shipped[name] = rule;
    }
  }
  process.stdout.write(JSON.stringify(shipped));
")
_SKILL_INDEX_TMP=$(mktemp)
echo "${_SHIPPED_RULES}" > "${_SKILL_INDEX_TMP}"
node "${BUILD_LIB}/generate-skill-index.js" \
  "${_SKILL_INDEX_TMP}" \
  "${SHIP_DIR}/.cursor/rules/skill-index.mdc"
rm -f "${_SKILL_INDEX_TMP}"
unset _SHIPPED_RULES _SKILL_INDEX_TMP

# ---------------------------------------------------------------------------
# 7. Copy cursor-specific rules from buildScripts/src/cursor-rules/
# ---------------------------------------------------------------------------
echo "  [cursor] Copying cursor-specific rules..."
for rule in agent-instructions.mdc workflow-gate.mdc; do
  src="${BUILD_SRC}/cursor-rules/${rule}"
  if [ -f "${src}" ]; then
    cp "${src}" "${SHIP_DIR}/.cursor/rules/${rule}"
    echo "  [cursor]   Copied ${rule}"
  else
    echo "  [cursor]   ERROR: ${rule} not found at ${src}"
    exit 1
  fi
done

# ---------------------------------------------------------------------------
# 8. Copy all commands from .claude/commands/
# ---------------------------------------------------------------------------
echo "  [cursor] Copying commands..."
if [ -d "${CLAUDE_DIR}/commands" ]; then
  cmd_count=0
  for cmd in "${CLAUDE_DIR}/commands"/*.md; do
    [ -f "${cmd}" ] || continue
    cp "${cmd}" "${SHIP_DIR}/.cursor/commands/"
    cmd_count=$((cmd_count + 1))
  done
  echo "  [cursor]   Copied ${cmd_count} command(s)"
else
  echo "  [cursor]   WARNING: .claude/commands/ not found"
fi

# ---------------------------------------------------------------------------
# 9. Apply Cursor-specific command overrides from buildScripts/src/cursor-commands/
# ---------------------------------------------------------------------------
echo "  [cursor] Applying cursor-specific command overrides..."
CURSOR_CMDS_SRC="${BUILD_SRC}/cursor-commands"
if [ -d "${CURSOR_CMDS_SRC}" ]; then
  override_count=0
  for override in "${CURSOR_CMDS_SRC}"/*.md; do
    [ -f "${override}" ] || continue
    cp "${override}" "${SHIP_DIR}/.cursor/commands/"
    override_count=$((override_count + 1))
    echo "  [cursor]   Overrode $(basename "${override}")"
  done
  echo "  [cursor]   Applied ${override_count} override(s)"
else
  echo "  [cursor]   WARNING: buildScripts/src/cursor-commands/ not found — no overrides applied"
fi

# ---------------------------------------------------------------------------
# 10. Copy Cursor-specific setup.sh
# ---------------------------------------------------------------------------
echo "  [cursor] Copying setup.sh..."
if [ -f "${BUILD_SRC}/cursor-setup.sh" ]; then
  cp "${BUILD_SRC}/cursor-setup.sh" "${SHIP_DIR}/setup.sh"
  chmod +x "${SHIP_DIR}/setup.sh"
else
  echo "  [cursor]   ERROR: buildScripts/src/cursor-setup.sh not found"
  exit 1
fi

# ---------------------------------------------------------------------------
# 11. Create workflow .gitkeep files
# ---------------------------------------------------------------------------
echo "  [cursor] Creating workflow .gitkeep files..."
for dir in ideas tasks done problems; do
  touch "${SHIP_DIR}/workflows/${dir}/.gitkeep"
done

# ---------------------------------------------------------------------------
# Verify output
# ---------------------------------------------------------------------------
echo "  [cursor] Verifying output..."

# Verify hooks.json has required event names
node -e "
  const h = JSON.parse(require('fs').readFileSync('${SHIP_DIR}/.cursor/hooks.json', 'utf8'));
  const required = ['sessionStart', 'afterFileEdit', 'beforeShellExecution', 'stop'];
  for (const ev of required) {
    if (!h.hooks[ev]) { console.error('  [cursor] ERROR: missing hook event: ' + ev); process.exit(1); }
  }
  console.log('  [cursor]   All required hook events present');
"

# Verify skill-index.mdc has alwaysApply: true
if grep -q "alwaysApply: true" "${SHIP_DIR}/.cursor/rules/skill-index.mdc"; then
  echo "  [cursor]   skill-index.mdc has alwaysApply: true"
else
  echo "  [cursor]   ERROR: skill-index.mdc missing alwaysApply: true"
  exit 1
fi

# Verify skill-creator.mdc has correct Cursor frontmatter (alwaysApply: false, no top-level activation:)
node -e "
  const content = require('fs').readFileSync('${SHIP_DIR}/.cursor/rules/skill-creator.mdc', 'utf8');
  // Extract only the frontmatter (between first and second ---)
  const match = content.match(/^---\n([\s\S]*?)\n---/);
  if (!match) { console.error('  [cursor]   ERROR: skill-creator.md has no frontmatter'); process.exit(1); }
  const fm = match[1];
  if (fm.includes('activation:')) { console.error('  [cursor]   ERROR: skill-creator.mdc frontmatter still has activation: block'); process.exit(1); }
  if (!fm.includes('alwaysApply:')) { console.error('  [cursor]   ERROR: skill-creator.mdc frontmatter missing alwaysApply field'); process.exit(1); }
  console.log('  [cursor]   skill-creator.mdc has correct Cursor frontmatter');
"

file_count=$(find "${SHIP_DIR}" -type f | wc -l | tr -d ' ')
echo "  [cursor] Done. ${file_count} files in ship/cursor/"

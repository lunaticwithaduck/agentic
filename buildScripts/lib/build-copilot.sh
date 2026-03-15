#!/usr/bin/env bash
# build-copilot.sh — builds ship/copilot/ distribution target
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# If REPO_ROOT is not set by parent build.sh, derive it from script location
REPO_ROOT="${REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
SHIP_DIR="${REPO_ROOT}/ship/copilot"
BUILD_SRC="${REPO_ROOT}/buildScripts/src"
BUILD_LIB="${REPO_ROOT}/buildScripts/lib"
CLAUDE_DIR="${REPO_ROOT}/.claude"

echo "  [copilot] Repo root : ${REPO_ROOT}"
echo "  [copilot] Ship dir  : ${SHIP_DIR}"

# ---------------------------------------------------------------------------
# 1. Create directory structure
# ---------------------------------------------------------------------------
echo "  [copilot] Creating directory structure..."
mkdir -p \
  "${SHIP_DIR}/.github/hooks" \
  "${SHIP_DIR}/.github/prompts" \
  "${SHIP_DIR}/.github/agents" \
  "${SHIP_DIR}/.github/skills/skill-creator" \
  "${SHIP_DIR}/.github/instructions" \
  "${SHIP_DIR}/workflows/ideas" \
  "${SHIP_DIR}/workflows/tasks" \
  "${SHIP_DIR}/workflows/done" \
  "${SHIP_DIR}/workflows/problems"

# ---------------------------------------------------------------------------
# 2. Copy shared JS hooks from .claude/hooks/ (with .github/ platform detection)
# ---------------------------------------------------------------------------
echo "  [copilot] Copying shared hooks..."
for hook in block-secrets.cjs post-write.cjs post-stop.cjs; do
  src="${CLAUDE_DIR}/hooks/${hook}"
  if [ -f "${src}" ]; then
    cp "${src}" "${SHIP_DIR}/.github/hooks/${hook}"
    echo "  [copilot]   Copied ${hook}"
  else
    echo "  [copilot]   WARNING: ${hook} not found at ${src}"
  fi
done

# ---------------------------------------------------------------------------
# 3. Copy Copilot-specific hook (skill-detector-copilot.cjs → skill-detector.cjs)
# ---------------------------------------------------------------------------
echo "  [copilot] Copying Copilot-specific hooks..."
COPILOT_SKILL_DETECTOR="${BUILD_SRC}/copilot-hooks/skill-detector-copilot.cjs"
if [ -f "${COPILOT_SKILL_DETECTOR}" ]; then
  cp "${COPILOT_SKILL_DETECTOR}" "${SHIP_DIR}/.github/hooks/skill-detector.cjs"
  echo "  [copilot]   Copied skill-detector-copilot.cjs -> skill-detector.cjs"
else
  echo "  [copilot]   ERROR: skill-detector-copilot.cjs not found at ${COPILOT_SKILL_DETECTOR}"
  exit 1
fi

# ---------------------------------------------------------------------------
# 4. Generate hooks.json
# ---------------------------------------------------------------------------
echo "  [copilot] Generating hooks.json..."
node "${BUILD_SRC}/copilot-hooks/generate-hooks-json-copilot.js" \
  "${SHIP_DIR}/.github/hooks/hooks.json"

# Verify it is valid JSON
node -e "JSON.parse(require('fs').readFileSync('${SHIP_DIR}/.github/hooks/hooks.json', 'utf8')); console.log('  [copilot]   hooks.json is valid JSON');"

# ---------------------------------------------------------------------------
# 5. Convert skill-creator to Copilot directory format
# ---------------------------------------------------------------------------
echo "  [copilot] Converting skill-creator skill..."
cp "${CLAUDE_DIR}/skills/skill-creator.md" \
   "${SHIP_DIR}/.github/skills/skill-creator/SKILL.md"
echo "  [copilot]   Copied skill-creator.md -> .github/skills/skill-creator/SKILL.md"

# ---------------------------------------------------------------------------
# 6. Generate filtered skill-rules.json (skill-creator only)
# ---------------------------------------------------------------------------
echo "  [copilot] Generating skill-rules.json (skill-creator only)..."
node -e "
  const fs = require('fs');
  const allRules = JSON.parse(fs.readFileSync('${CLAUDE_DIR}/skills/skill-rules.json', 'utf8'));
  const shipped = {};
  for (const [name, rule] of Object.entries(allRules)) {
    if (fs.existsSync('${SHIP_DIR}/.github/skills/' + name + '/SKILL.md')) {
      shipped[name] = rule;
    }
  }
  fs.writeFileSync('${SHIP_DIR}/.github/skills/skill-rules.json', JSON.stringify(shipped, null, 2));
  console.log('  [copilot]   Generated skill-rules.json with skills: ' + Object.keys(shipped).join(', '));
"

# Rewrite filePatterns entries: .claude/skills/ → .github/skills/ in the copied file only
python3 -c "
import json, sys
path = '${SHIP_DIR}/.github/skills/skill-rules.json'
rules = json.load(open(path))
for rule in rules.values():
    if 'filePatterns' in rule:
        rule['filePatterns'] = [p.replace('.claude/skills/', '.github/skills/') for p in rule['filePatterns']]
json.dump(rules, open(path, 'w'), indent=2)
print('  [copilot]   Rewrote filePatterns .claude/skills/ -> .github/skills/ in skill-rules.json')
"

# ---------------------------------------------------------------------------
# 7. Copy copilot-instructions.md
# ---------------------------------------------------------------------------
echo "  [copilot] Copying copilot-instructions.md..."
COPILOT_INSTR="${BUILD_SRC}/copilot-rules/copilot-instructions.md"
if [ -f "${COPILOT_INSTR}" ]; then
  cp "${COPILOT_INSTR}" "${SHIP_DIR}/.github/copilot-instructions.md"
  echo "  [copilot]   Copied copilot-instructions.md"
else
  echo "  [copilot]   ERROR: copilot-instructions.md not found at ${COPILOT_INSTR}"
  exit 1
fi

# ---------------------------------------------------------------------------
# 8. Copy workflow-gate.instructions.md
# ---------------------------------------------------------------------------
echo "  [copilot] Copying workflow-gate.instructions.md..."
WORKFLOW_GATE="${BUILD_SRC}/copilot-rules/workflow-gate.instructions.md"
if [ -f "${WORKFLOW_GATE}" ]; then
  cp "${WORKFLOW_GATE}" "${SHIP_DIR}/.github/instructions/workflow-gate.instructions.md"
  echo "  [copilot]   Copied workflow-gate.instructions.md"
else
  echo "  [copilot]   WARNING: workflow-gate.instructions.md not found at ${WORKFLOW_GATE}"
fi

# ---------------------------------------------------------------------------
# 9. Convert all commands via convert-to-prompt.js -> .github/prompts/
# ---------------------------------------------------------------------------
echo "  [copilot] Converting commands to prompts..."
if [ -d "${CLAUDE_DIR}/commands" ]; then
  prompt_count=0
  for cmd in "${CLAUDE_DIR}/commands"/*.md; do
    [ -f "${cmd}" ] || continue
    basename="${cmd##*/}"
    stem="${basename%.md}"
    out="${SHIP_DIR}/.github/prompts/${stem}.prompt.md"
    node "${BUILD_LIB}/convert-to-prompt.js" "${cmd}" "${out}"
    prompt_count=$((prompt_count + 1))
  done
  echo "  [copilot]   Converted ${prompt_count} command(s) to prompt(s)"
else
  echo "  [copilot]   WARNING: .claude/commands/ not found"
fi

# ---------------------------------------------------------------------------
# 10. Apply Copilot-specific command overrides from buildScripts/src/copilot-commands/
# ---------------------------------------------------------------------------
echo "  [copilot] Applying copilot-specific command overrides..."
COPILOT_CMDS_SRC="${BUILD_SRC}/copilot-commands"
if [ -d "${COPILOT_CMDS_SRC}" ]; then
  override_count=0
  for override in "${COPILOT_CMDS_SRC}"/*.md; do
    [ -f "${override}" ] || continue
    stem="$(basename "${override}" .md)"
    out="${SHIP_DIR}/.github/prompts/${stem}.prompt.md"
    node "${BUILD_LIB}/convert-to-prompt.js" "${override}" "${out}"
    override_count=$((override_count + 1))
    echo "  [copilot]   Overrode ${stem}.prompt.md"
  done
  echo "  [copilot]   Applied ${override_count} override(s)"
else
  echo "  [copilot]   WARNING: buildScripts/src/copilot-commands/ not found — no overrides applied"
fi

# ---------------------------------------------------------------------------
# 11. Convert all agents via convert-to-agent.js -> .github/agents/
# ---------------------------------------------------------------------------
echo "  [copilot] Converting agents..."
if [ -d "${CLAUDE_DIR}/agents" ]; then
  agent_count=0
  for agent in "${CLAUDE_DIR}/agents"/*.md; do
    [ -f "${agent}" ] || continue
    basename="${agent##*/}"
    stem="${basename%.md}"
    out="${SHIP_DIR}/.github/agents/${stem}.agent.md"
    node "${BUILD_LIB}/convert-to-agent.js" "${agent}" "${out}"
    agent_count=$((agent_count + 1))
  done
  echo "  [copilot]   Converted ${agent_count} agent(s)"
else
  echo "  [copilot]   WARNING: .claude/agents/ not found"
fi

# ---------------------------------------------------------------------------
# 12. Apply Copilot agent overrides from buildScripts/src/copilot-agents/
# ---------------------------------------------------------------------------
echo "  [copilot] Applying Copilot agent overrides..."
COPILOT_AGENTS_SRC="${BUILD_SRC}/copilot-agents"
if [ -d "${COPILOT_AGENTS_SRC}" ]; then
  agent_override_count=0
  for override in "${COPILOT_AGENTS_SRC}"/*.md; do
    [ -f "${override}" ] || continue
    stem="$(basename "${override}" .md)"
    out="${SHIP_DIR}/.github/agents/${stem}.agent.md"
    node "${BUILD_LIB}/convert-to-agent.js" "${override}" "${out}"
    agent_override_count=$((agent_override_count + 1))
    echo "  [copilot]   Overrode ${stem}.agent.md"
  done
  echo "  [copilot]   Applied ${agent_override_count} agent override(s)"
else
  echo "  [copilot]   WARNING: buildScripts/src/copilot-agents/ not found — no agent overrides applied"
fi

# ---------------------------------------------------------------------------
# 13. Copy copilot-specific setup.sh
# ---------------------------------------------------------------------------
echo "  [copilot] Copying setup.sh..."
COPILOT_SETUP="${BUILD_SRC}/copilot-setup.sh"
if [ -f "${COPILOT_SETUP}" ]; then
  cp "${COPILOT_SETUP}" "${SHIP_DIR}/setup.sh"
  chmod +x "${SHIP_DIR}/setup.sh"
  echo "  [copilot]   Copied copilot-setup.sh -> setup.sh"
else
  echo "  [copilot]   ERROR: copilot-setup.sh not found at ${COPILOT_SETUP}"
  exit 1
fi

# ---------------------------------------------------------------------------
# 14. Create workflow .gitkeep files
# ---------------------------------------------------------------------------
echo "  [copilot] Creating workflow .gitkeep files..."
for dir in ideas tasks done problems; do
  touch "${SHIP_DIR}/workflows/${dir}/.gitkeep"
done

# ---------------------------------------------------------------------------
# Verify output
# ---------------------------------------------------------------------------
echo "  [copilot] Verifying output..."

# Verify hooks.json has required event names
node -e "
  const h = JSON.parse(require('fs').readFileSync('${SHIP_DIR}/.github/hooks/hooks.json', 'utf8'));
  const required = ['UserPromptSubmit', 'PreToolUse', 'PostToolUse', 'Stop'];
  for (const ev of required) {
    if (!h.hooks[ev]) { console.error('  [copilot] ERROR: missing hook event: ' + ev); process.exit(1); }
  }
  console.log('  [copilot]   All required hook events present');
"

# Verify key files exist
KEY_FILES=(
  "${SHIP_DIR}/.github/hooks/skill-detector.cjs"
  "${SHIP_DIR}/.github/hooks/block-secrets.cjs"
  "${SHIP_DIR}/.github/hooks/post-write.cjs"
  "${SHIP_DIR}/.github/hooks/post-stop.cjs"
  "${SHIP_DIR}/.github/hooks/hooks.json"
  "${SHIP_DIR}/.github/skills/skill-creator/SKILL.md"
  "${SHIP_DIR}/.github/skills/skill-rules.json"
  "${SHIP_DIR}/.github/copilot-instructions.md"
  "${SHIP_DIR}/.github/instructions/workflow-gate.instructions.md"
  "${SHIP_DIR}/setup.sh"
)

all_ok=true
for f in "${KEY_FILES[@]}"; do
  if [ -f "${f}" ]; then
    echo "  [copilot]   OK: ${f##*ship/copilot/}"
  else
    echo "  [copilot]   ERROR: missing ${f##*ship/copilot/}"
    all_ok=false
  fi
done

if [ "${all_ok}" != "true" ]; then
  echo "  [copilot] ERROR: key file verification failed"
  exit 1
fi

file_count=$(find "${SHIP_DIR}" -type f | wc -l | tr -d ' ')
echo "  [copilot] Done. ${file_count} files in ship/copilot/"

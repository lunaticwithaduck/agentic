#!/usr/bin/env bash
# build-claude-code.sh — builds ship/claude-code/ from source
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DEST="${REPO_ROOT}/ship/claude-code"

echo "  [claude-code] Building from ${REPO_ROOT}"
echo "  [claude-code] Destination: ${DEST}"

# ---------------------------------------------------------------------------
# 1. Create directory structure
# ---------------------------------------------------------------------------
mkdir -p \
  "${DEST}/.claude/hooks" \
  "${DEST}/.claude/skills" \
  "${DEST}/.claude/agents" \
  "${DEST}/.claude/commands" \
  "${DEST}/.claude/scripts" \
  "${DEST}/workflows/ideas" \
  "${DEST}/workflows/tasks" \
  "${DEST}/workflows/done" \
  "${DEST}/workflows/problems"

# ---------------------------------------------------------------------------
# 2. Hooks — copy all .js and .sh files, preserve executable on .sh
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying hooks..."
for f in "${REPO_ROOT}/.claude/hooks/"*.js "${REPO_ROOT}/.claude/hooks/"*.sh; do
  [ -f "${f}" ] || continue
  cp "${f}" "${DEST}/.claude/hooks/"
done
chmod +x "${DEST}/.claude/hooks/"*.sh

# ---------------------------------------------------------------------------
# 3. Skills — ONLY skill-creator.md
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying skill-creator.md..."
cp "${REPO_ROOT}/.claude/skills/skill-creator.md" "${DEST}/.claude/skills/"

# ---------------------------------------------------------------------------
# 4. skill-rules.json — filtered to skill-creator entry only
# ---------------------------------------------------------------------------
echo "  [claude-code] Generating filtered skill-rules.json..."
python3 - <<'PYEOF'
import json, os, sys

repo_root = os.environ.get("REPO_ROOT")
if not repo_root:
    print("ERROR: REPO_ROOT not set", file=sys.stderr)
    sys.exit(1)

src = os.path.join(repo_root, ".claude", "skills", "skill-rules.json")
dst = os.path.join(repo_root, "ship", "claude-code", ".claude", "skills", "skill-rules.json")

with open(src) as f:
    rules = json.load(f)

if "skill-creator" not in rules:
    print("ERROR: skill-creator key not found in skill-rules.json", file=sys.stderr)
    sys.exit(1)

filtered = {"skill-creator": rules["skill-creator"]}

with open(dst, "w") as f:
    json.dump(filtered, f, indent=2)
    f.write("\n")

print(f"  [claude-code] skill-rules.json written with {len(filtered)} entry")
PYEOF

# ---------------------------------------------------------------------------
# 5. Agents — all .md files
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying agents..."
for f in "${REPO_ROOT}/.claude/agents/"*.md; do
  [ -f "${f}" ] || continue
  cp "${f}" "${DEST}/.claude/agents/"
done

# ---------------------------------------------------------------------------
# 6. Commands — all .md files
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying commands..."
for f in "${REPO_ROOT}/.claude/commands/"*.md; do
  [ -f "${f}" ] || continue
  cp "${f}" "${DEST}/.claude/commands/"
done

# ---------------------------------------------------------------------------
# 7. Scripts — validate.sh only
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying scripts/validate.sh..."
cp "${REPO_ROOT}/.claude/scripts/validate.sh" "${DEST}/.claude/scripts/"
chmod +x "${DEST}/.claude/scripts/validate.sh"

# ---------------------------------------------------------------------------
# 8. settings.json
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying settings.json..."
cp "${REPO_ROOT}/.claude/settings.json" "${DEST}/.claude/"

# ---------------------------------------------------------------------------
# 9. Root files
# ---------------------------------------------------------------------------
echo "  [claude-code] Copying CLAUDE.md..."
cp "${REPO_ROOT}/CLAUDE.md" "${DEST}/"

if [ -f "${REPO_ROOT}/setup.sh" ]; then
  echo "  [claude-code] Copying setup.sh..."
  cp "${REPO_ROOT}/setup.sh" "${DEST}/"
  chmod +x "${DEST}/setup.sh"
else
  echo "  [claude-code] setup.sh not found — skipping"
fi

# ---------------------------------------------------------------------------
# 10. Workflow placeholder files
# ---------------------------------------------------------------------------
echo "  [claude-code] Creating workflow .gitkeep files..."
touch \
  "${DEST}/workflows/ideas/.gitkeep" \
  "${DEST}/workflows/tasks/.gitkeep" \
  "${DEST}/workflows/done/.gitkeep" \
  "${DEST}/workflows/problems/.gitkeep"

echo "  [claude-code] Done."

#!/usr/bin/env bash
# setup.sh - Initialize agentic infrastructure for a GitHub Copilot project
# Run this after copying ship/copilot/ contents into your project root.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "================================"
echo "  agentic - AI-first project setup"
echo "================================"
echo ""

# 1. Create workflow directories
echo "[1/3] Creating workflow directories..."
mkdir -p "$SCRIPT_DIR/workflows/ideas"
mkdir -p "$SCRIPT_DIR/workflows/tasks"
mkdir -p "$SCRIPT_DIR/workflows/done"
mkdir -p "$SCRIPT_DIR/workflows/problems"
echo "  - workflows/ideas/"
echo "  - workflows/tasks/"
echo "  - workflows/done/"
echo "  - workflows/problems/"
echo ""

# 2. Verify .github infrastructure
echo "[2/3] Verifying .github infrastructure..."
mkdir -p "$SCRIPT_DIR/.github/hooks"
mkdir -p "$SCRIPT_DIR/.github/skills"
mkdir -p "$SCRIPT_DIR/.github/prompts"
mkdir -p "$SCRIPT_DIR/.github/agents"
mkdir -p "$SCRIPT_DIR/.github/instructions"

# Verify hooks.json is present
if [ -f "$SCRIPT_DIR/.github/hooks/hooks.json" ]; then
  echo "  hooks.json present"
else
  echo "  WARNING: .github/hooks/hooks.json not found — hook events will not fire"
fi

# Verify copilot-instructions.md is present
if [ -f "$SCRIPT_DIR/.github/copilot-instructions.md" ]; then
  echo "  copilot-instructions.md present"
else
  echo "  WARNING: .github/copilot-instructions.md not found — project instructions will not load"
fi

# Verify Node.js is available (required for hooks)
if command -v node &> /dev/null; then
  echo "  Node.js is available: $(node --version)"
else
  echo "  WARNING: Node.js not found — hooks require Node.js to run"
  echo "  Install from: https://nodejs.org"
fi

echo "  All directories verified."
echo ""

# 3. Check for GitHub Copilot
echo "[3/3] Checking for GitHub Copilot..."
if command -v gh &> /dev/null; then
  echo "  GitHub CLI (gh) is available: $(gh --version | head -1)"
  echo "  Ensure GitHub Copilot extension is installed: gh extension install github/gh-copilot"
else
  echo "  GitHub CLI not found in PATH — install from: https://cli.github.com"
  echo "  Then install Copilot extension: gh extension install github/gh-copilot"
fi
echo ""

echo "================================"
echo "  Setup complete!"
echo "================================"
echo ""
echo "Next steps:"
echo "  1. Open this project in VS Code or another editor with GitHub Copilot"
echo "  2. Use the '/setup' prompt in Copilot Chat to personalize for your project"
echo "  3. Use the '/idea' prompt to start capturing work items"
echo "  4. Customize .github/copilot-instructions.md with your project details"
echo ""
echo "Skill system:"
echo "  - Skills are auto-injected on every prompt (Layer 3 — deterministic keyword match)"
echo "  - Reference a skill manually: #.github/skills/skill-name/SKILL.md in Copilot Chat (Layer 2)"
echo "  - copilot-instructions.md describes available skills so the model knows what exists (Layer 1)"
echo "  - Domain-specific skills are auto-generated from completed work (autolearn)"
echo ""

#!/usr/bin/env bash
# setup.sh - Initialize agentic infrastructure for a Cursor project
# Run this after copying ship/cursor/ contents into your project root.

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

# 2. Verify .cursor infrastructure
echo "[2/3] Verifying .cursor infrastructure..."
mkdir -p "$SCRIPT_DIR/.cursor/hooks"
mkdir -p "$SCRIPT_DIR/.cursor/rules"
mkdir -p "$SCRIPT_DIR/.cursor/commands"

# Verify hooks.json is present
if [ -f "$SCRIPT_DIR/.cursor/hooks.json" ]; then
  echo "  hooks.json present"
else
  echo "  WARNING: .cursor/hooks.json not found — hook events will not fire"
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

# 3. Check for Cursor
echo "[3/3] Checking for Cursor..."
if command -v cursor &> /dev/null; then
  echo "  Cursor is installed."
else
  echo "  Cursor CLI not found in PATH — that's fine if you launch Cursor from the app."
  echo "  Make sure hooks.json is picked up: open this project folder in Cursor."
fi
echo ""

echo "================================"
echo "  Setup complete!"
echo "================================"
echo ""
echo "Next steps:"
echo "  1. Open this project folder in Cursor"
echo "  2. Use the 'setup' slash command to personalize for your project"
echo "  3. Use the 'idea' slash command to start capturing work items"
echo "  4. Customize .cursor/rules/agent-instructions.md with your project details"
echo ""
echo "Skill system:"
echo "  - Skills are auto-injected after file edits (Layer 3 — deterministic)"
echo "  - Type @skill-name in chat to attach a skill manually (Layer 2)"
echo "  - skill-index.md is always active so the model knows what skills exist (Layer 1)"
echo "  - Domain-specific skills are auto-generated from completed work (autolearn)"
echo ""

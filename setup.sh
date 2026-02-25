#!/usr/bin/env bash
# setup.sh - Initialize agentic infrastructure for a new project
# Run this after cloning the agentic repo into your project.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "================================"
echo "  agentic - AI-first project setup"
echo "================================"
echo ""

# 1. Create workflow directories
echo "[1/4] Creating workflow directories..."
mkdir -p "$SCRIPT_DIR/workflows/ideas"
mkdir -p "$SCRIPT_DIR/workflows/tasks"
mkdir -p "$SCRIPT_DIR/workflows/done"
echo "  - workflows/ideas/"
echo "  - workflows/tasks/"
echo "  - workflows/done/"
echo ""

# 2. Ensure .claude directories exist
echo "[2/4] Verifying .claude infrastructure..."
mkdir -p "$SCRIPT_DIR/.claude/skills"
mkdir -p "$SCRIPT_DIR/.claude/hooks"
mkdir -p "$SCRIPT_DIR/.claude/commands"
mkdir -p "$SCRIPT_DIR/.claude/agents"
mkdir -p "$SCRIPT_DIR/.claude/scripts"

# Make hook scripts executable
chmod +x "$SCRIPT_DIR/.claude/hooks/"*.sh 2>/dev/null || true
chmod +x "$SCRIPT_DIR/.claude/scripts/"*.sh 2>/dev/null || true
echo "  All directories and permissions verified."
echo ""

# 3. Check for Claude Code
echo "[3/4] Checking for Claude Code..."
if command -v claude &> /dev/null; then
  echo "  Claude Code is installed: $(claude --version 2>/dev/null || echo 'version unknown')"
else
  echo "  Claude Code not found."
  echo "  Install it from: https://docs.anthropic.com/en/docs/claude-code"
  echo "  After installing, run this script again."
fi
echo ""

# 4. MCP server setup
echo "[4/4] MCP Server setup"
echo ""
echo "  Common MCP servers you may want to install:"
echo "    - context7: Library documentation lookup"
echo "    - filesystem: Enhanced file access"
echo ""
echo "  To set up MCP servers interactively, run:"
echo "    bash .claude/scripts/mcp-setup.sh"
echo ""

echo "================================"
echo "  Setup complete!"
echo "================================"
echo ""
echo "Next steps:"
echo "  1. Run 'claude' to start Claude Code"
echo "  2. Use /setup to personalize for your project"
echo "  3. Use /idea to start capturing work items"
echo "  4. Customize CLAUDE.md with your project details"
echo ""
echo "Optional:"
echo "  Add Playwright E2E tests:"
echo "    bash .claude/scripts/playwright-setup.sh"
echo ""

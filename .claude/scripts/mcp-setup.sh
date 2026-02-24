#!/usr/bin/env bash
# mcp-setup.sh - Interactive MCP server configuration
# Helps set up common MCP servers for use with Claude Code.

set -euo pipefail

echo "================================"
echo "  MCP Server Setup"
echo "================================"
echo ""

# Check for Claude Code
if ! command -v claude &> /dev/null; then
  echo "Error: Claude Code (claude) is not installed or not in PATH."
  echo "Install it from: https://docs.anthropic.com/en/docs/claude-code"
  exit 1
fi

echo "Select MCP servers to install (enter numbers separated by spaces):"
echo ""
echo "  1) context7     - Library documentation lookup"
echo "  2) filesystem   - Enhanced file access for your project"
echo "  0) Cancel"
echo ""
read -rp "Your selection: " SELECTIONS

if [[ "$SELECTIONS" == "0" || -z "$SELECTIONS" ]]; then
  echo "No servers selected. Exiting."
  exit 0
fi

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

for selection in $SELECTIONS; do
  case "$selection" in
    1)
      echo ""
      echo "Installing context7..."
      echo "  Running: claude mcp add context7 -- npx -y @upstash/context7-mcp"
      claude mcp add context7 -- npx -y @upstash/context7-mcp
      echo "  context7 installed successfully."
      ;;
    2)
      echo ""
      echo "Installing filesystem..."
      echo "  This server will have access to: $PROJECT_DIR"
      echo "  Running: claude mcp add filesystem -- npx -y @anthropic/mcp-filesystem $PROJECT_DIR"
      claude mcp add filesystem -- npx -y @anthropic/mcp-filesystem "$PROJECT_DIR"
      echo "  filesystem installed successfully."
      ;;
    *)
      echo ""
      echo "  Unknown selection: $selection (skipping)"
      ;;
  esac
done

echo ""
echo "================================"
echo "  MCP setup complete!"
echo "================================"
echo ""
echo "Restart Claude Code for the new servers to take effect."
echo ""

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
echo "  1) context7        - Library documentation lookup"
echo "  2) filesystem      - Enhanced file access for your project"
echo "  3) figma-remote    - Figma design context via OAuth (recommended)"
echo "  4) figma-desktop   - Figma design context via local desktop app"
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
    3)
      echo ""
      echo "Installing figma-remote (OAuth)..."
      echo "  This connects to Figma's hosted MCP endpoint."
      echo "  After installation you will need to authenticate:"
      echo "    1. Start Claude Code and run: /mcp"
      echo "    2. Select 'figma' -> 'Authenticate'"
      echo "    3. Allow access in the browser dialog"
      echo ""
      echo "  Scope options:"
      echo "    1) project  - Available in this project only (default)"
      echo "    2) user     - Available in all your projects"
      read -rp "  Scope [1]: " FIGMA_SCOPE
      FIGMA_SCOPE="${FIGMA_SCOPE:-1}"
      if [[ "$FIGMA_SCOPE" == "2" ]]; then
        SCOPE_FLAG="--scope user"
        echo "  Running: claude mcp add --scope user --transport http figma https://mcp.figma.com/mcp"
        claude mcp add --scope user --transport http figma https://mcp.figma.com/mcp
      else
        SCOPE_FLAG=""
        echo "  Running: claude mcp add --transport http figma https://mcp.figma.com/mcp"
        claude mcp add --transport http figma https://mcp.figma.com/mcp
      fi
      echo "  figma-remote registered. Run /mcp in Claude Code to authenticate."
      ;;
    4)
      echo ""
      echo "Installing figma-desktop (local)..."
      echo "  Prerequisites:"
      echo "    - Figma desktop app must be open"
      echo "    - Open any Design file and enter Dev Mode (Shift+D)"
      echo "    - Enable 'Desktop MCP server' in the Dev Mode panel"
      echo ""
      echo "  The server runs at: http://127.0.0.1:3845/mcp"
      echo "  Running: claude mcp add --transport http figma-desktop http://127.0.0.1:3845/mcp"
      claude mcp add --transport http figma-desktop http://127.0.0.1:3845/mcp
      echo "  figma-desktop registered. Keep the Figma desktop app running while using it."
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

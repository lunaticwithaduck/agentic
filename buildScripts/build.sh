#!/usr/bin/env bash
# build.sh — assembles ship/claude-code/ and ship/cursor/
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SHIP_DIR="${REPO_ROOT}/ship"

VERSION="$(tr -d '[:space:]' < "${REPO_ROOT}/VERSION")"

echo "==> Agentic distribution build"
echo "    Version   : ${VERSION}"
echo "    Repo root : ${REPO_ROOT}"
echo "    Ship dir  : ${SHIP_DIR}"
echo ""

# ---------------------------------------------------------------------------
# Clean ship/
# ---------------------------------------------------------------------------
echo "==> Cleaning ship/..."
rm -rf "${SHIP_DIR}"
mkdir -p "${SHIP_DIR}/claude-code" "${SHIP_DIR}/cursor"

# ---------------------------------------------------------------------------
# Build platforms
# ---------------------------------------------------------------------------
CLAUDE_CODE_STATUS=0
CURSOR_STATUS=0

echo "==> Building claude-code platform..."
export REPO_ROOT
if bash "${SCRIPT_DIR}/lib/build-claude-code.sh"; then
  CLAUDE_CODE_STATUS=0
else
  CLAUDE_CODE_STATUS=$?
  echo "ERROR: build-claude-code.sh failed (exit ${CLAUDE_CODE_STATUS})"
fi

echo ""
echo "==> Building cursor platform..."
if bash "${SCRIPT_DIR}/lib/build-cursor.sh"; then
  CURSOR_STATUS=0
else
  CURSOR_STATUS=$?
  echo "ERROR: build-cursor.sh failed (exit ${CURSOR_STATUS})"
fi

echo ""

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo "==> Build summary"
echo ""

if [ -d "${SHIP_DIR}/claude-code" ]; then
  CC_COUNT=$(find "${SHIP_DIR}/claude-code" -type f | wc -l | tr -d ' ')
  echo "    claude-code : ${CC_COUNT} files"
  if [ "${CLAUDE_CODE_STATUS}" -ne 0 ]; then
    echo "                  (BUILD FAILED)"
  fi
else
  echo "    claude-code : (not built)"
fi

if [ -d "${SHIP_DIR}/cursor" ]; then
  CUR_COUNT=$(find "${SHIP_DIR}/cursor" -type f | wc -l | tr -d ' ')
  echo "    cursor      : ${CUR_COUNT} files"
  if [ "${CURSOR_STATUS}" -ne 0 ]; then
    echo "                  (BUILD FAILED)"
  fi
else
  echo "    cursor      : (not built)"
fi

echo ""

# ---------------------------------------------------------------------------
# Exit non-zero if either platform failed
# ---------------------------------------------------------------------------
if [ "${CLAUDE_CODE_STATUS}" -ne 0 ] || [ "${CURSOR_STATUS}" -ne 0 ]; then
  echo "==> Build FAILED"
  exit 1
fi

# Stamp version into each ship target
echo "${VERSION}" > "${SHIP_DIR}/claude-code/.version"
echo "${VERSION}" > "${SHIP_DIR}/cursor/.version"
echo "==> Stamped version ${VERSION} into ship targets"

echo "==> Build complete"

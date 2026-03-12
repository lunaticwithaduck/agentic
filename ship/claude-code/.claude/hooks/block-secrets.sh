#!/usr/bin/env bash
# Shim — delegates to the JS implementation
{ [ -n "${1:-}" ] && echo "$1" || cat; } | node "$(dirname "${BASH_SOURCE[0]}")/block-secrets.js"

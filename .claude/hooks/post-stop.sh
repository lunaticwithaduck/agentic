#!/usr/bin/env bash
# Shim — delegates to the JS implementation
exec node "$(dirname "${BASH_SOURCE[0]}")/post-stop.cjs"

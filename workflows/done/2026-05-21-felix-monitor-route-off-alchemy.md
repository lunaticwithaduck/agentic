---
title: Route Felix monitor's per-block reads off Alchemy
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Felix monitor was hitting Alchemy on every block sweep (~80-120 eth_calls/sec across 39 markets). Cut that to near-zero by routing reads through the public RPC rotation.

## Steps
- [x] Patched `rpcCall` to detect Alchemy URL and reroute to LOGS_RPCS (preserves the `(rpc, method, params)` signature so all existing callers work)
- [x] WSS subscription unchanged (still on Alchemy — push is free)
- [x] Restarted felix-monitor; HF sweep still produces correct values (same at-risk positions found)

## Outcome
Completed 2026-05-21. Felix monitor's Alchemy CU consumption drops from ~93-140 $/mo to ~$0. Total fleet Alchemy estimate now ~$35/mo (down from $124-170).

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-monitor-route-off-alchemy.md`.

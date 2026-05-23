---
title: Drop redundant HL+HP HTTP monitors (WSS variants cover detection)
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
HL + HP each had both HTTP (Alchemy-hitting) and WSS (off-Alchemy) monitors. WSS detects faster and cheaper. Disable HTTP variants to save ~$30/mo.

## Steps
- [x] Migrated balance watcher from `hyperlend/monitor.js` → `hyperlend/monitor-wss.js` (with public RPC rotation + Alchemy fallback)
- [x] Restarted `hyperlend-monitor-wss` — verified balance check fires
- [x] `systemctl --user disable --now hyperlend-monitor hypurrfi-monitor`
- [x] Both inactive; unit files preserved (`/home/jojo/.config/systemd/user/`) for easy re-enable

## Final fleet (15 services, 5 chains)
- Bend: indexer/monitor/executor (Berachain — Morpho fork)
- Sonic-silo: indexer/monitor/executor (Sonic — Silo V2)
- HyperLend: indexer/**monitor-wss**/executor (HyperEVM — Aave V3 fork)
- HypurrFi: indexer/**monitor-wss**/executor (HyperEVM — Aave V3 fork)
- Felix: indexer/monitor/executor (HyperEVM — Morpho Blue Vanilla Markets)

All executors 🟢 LIVE.

## Alchemy economics
- Before this session: ~$124-170/mo (Felix monitor unrouted)
- After felix-monitor reroute: ~$35/mo
- **After dropping HL+HP HTTP monitors: ~$5/mo**

Saved ~$120-165/mo for ~15 min of work.

## Completion
Run `/complete workflows/tasks/2026-05-21-drop-redundant-http-monitors.md`.

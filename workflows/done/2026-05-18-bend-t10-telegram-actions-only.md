---
title: Bend Telegram — actions + errors only, drop watching/info alerts
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Steps
- [x] monitor.js: removed startup Telegram, removed ARM Telegram, removed FIRE Telegram (monitor still logs + writes armed file, no Telegram)
- [x] executor.js: removed startup Telegram, removed pre-signed Telegram
- [x] Restarted bend-monitor and bend-executor — silent restart, no spurious Telegrams

## Remaining Telegram surface (actions + errors only)
| Alert | Source | Class |
|---|---|---|
| 🔥 FIRING ${market} ${borrower} | executor | ACTION |
| ✅ broadcast via ${rpc} — tx ${hash} | executor | ACTION result |
| 🎉 WON — profit $X tx [...](berascan link) | executor | ACTION result |
| 💀 reverted — tx ${hash} | executor | ERROR |
| ❌ broadcast FAIL: ${err} | executor | ERROR |

Internal pre-sign + arming is now silent — no Telegram noise during normal "watching positions" operation.

## Outcome

Completed 2026-05-18. Stripped Telegram down to action+error alerts only per user request. Monitor no longer emits startup/ARM/FIRE Telegrams. Executor no longer emits startup/pre-signed Telegrams. The only Telegrams you'll receive going forward are:
1. A position is FIRING (broadcast initiated)
2. Broadcast result (won/reverted)
3. Errors (broadcast failures)

Internal pre-signing and arming still happen silently — the bot remains fully autonomous, just quieter.

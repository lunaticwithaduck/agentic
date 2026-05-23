---
title: Wire premium RPC as fire-path for Monad
created: 2026-05-22
completed: 2026-05-22
status: done (pending user action to add key)
---

## What was done
- **Confirmed Alchemy does NOT yet support Monad mainnet** — only testnet. Use dRPC paid / QuickNode / Ankr instead.
- Added `HTTP_RPC_FIRE` to `monad/config.js` — populated from `MONAD_RPC_HTTP_FIRE` env var, null fallback
- Modified `monad/executor.js` broadcast path to use `HTTP_RPC_FIRE` when available, falling back to public `HTTP_RPC` otherwise
- Restarted `monad-executor.service` to pick up the change

## User action required to activate
1. Sign up for premium RPC on a provider that supports Monad mainnet:
   - **dRPC** ($X/mo, removes 25/sec throttle — we're already familiar with their endpoint format)
   - **QuickNode** (Monad mainnet plan)
   - **Ankr** (premium plan)
2. Add to `/home/jojo/automation/.env`:
   ```
   MONAD_RPC_HTTP_FIRE=https://<your-premium-endpoint>
   ```
3. Restart: `systemctl --user restart monad-executor.service`
4. On the next fire, you'll see in the log:
   ```
   broadcast via premium https://...***
   ```

## Behavior without the key (current state)
Executor broadcasts via `https://rpc.monad.xyz` (public, free) just like before. Function unaffected; just slightly higher latency + small risk of public-RPC throttling on the broadcast tx.

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-alchemy-fire-only.md`.

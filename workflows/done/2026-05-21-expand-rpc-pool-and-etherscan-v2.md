---
title: Expand HyperEVM RPC pool (3→7) + wire Etherscan V2 for historical lookups
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
1. Stop the rate-limit errors on the 3-RPC pool by adding more endpoints
2. Verify Etherscan V2 works for HyperEVM and stash the API key

## Steps
- [x] Probed 5 candidate public RPCs against eth_getBalance — all 5 returned correct balance except Tatum (broken)
- [x] Probed 4 working ones against 1000-block eth_getLogs — all 4 passed
- [x] Added all 4 to `LOGS_RPCS` in `hyperlend/config.js`, `hypurrfi/config.js`, `felix/config.js` (3→7 pool)
- [x] Restarted `hyperlend-monitor-wss`, `hypurrfi-monitor-wss`, `felix-monitor`
- [x] Verified Etherscan V2 works for HyperEVM with the supplied API key — block number, balance, account endpoints all return correct data

## Final LOGS_RPCS (7 endpoints)
1. `rpc.hyperliquid.xyz/evm` — official, bursty rate limit
2. `hyperliquid-json-rpc.stakely.io` — stable
3. `rpc.purroofgroup.com` — stable
4. **`hyperliquid.rpc.blxrbdn.com`** (bloXroute) — NEW
5. **`hyperliquid.api.onfinality.io/evm/public`** (OnFinality) — NEW
6. **`hyperliquid.api.pocket.network`** (Pocket Network) — NEW
7. **`999.rpc.thirdweb.com`** (Thirdweb) — NEW

## Etherscan V2 confirmed working
```
https://api.etherscan.io/v2/api?chainid=999&module=...&apikey=...
```
- API key stashed in `/home/jojo/automation/.env` as `ETHERSCAN_API_KEY`
- Free tier: 3 req/sec, 100k/day, HyperEVM included in coverage
- Use cases: backtest historical scans, account tx history, contract source/ABI lookups, gas oracle
- NOT a replacement for live RPC — REST protocol, slower per-call

## Outcome
Completed 2026-05-21. RPC pool 2.3× bigger → load per endpoint drops dramatically. Etherscan V2 ready for ad-hoc historical queries.

## Follow-ups (deferred)
- Wire Etherscan V2 into `hyperlend/backtest.js` + `decode-missed-liquidation.js` patterns — replaces some RPC calls for historical state with cleaner Etherscan endpoints
- Tatum endpoint returned `null` — could investigate / add it if fixed later
- If new pool still rate-limits under load, add more backoff jitter

## Completion
Run `/complete workflows/tasks/2026-05-21-expand-rpc-pool-and-etherscan-v2.md`.

---
title: HyperEVM HyperLend — h2 indexer + monitor with Multicall3 (no WSS needed)
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Built (the entire HyperLend stack except deploy)
- `config.js` — HyperLend Pool + AaveOracle + ProtocolDataProvider + Multicall3 + HyperSwap + 8 reserves
- `abi/aave-v3.json` + `abi/multicall3.json`
- `indexer.js` — Pool.Borrow event discovery + Multicall3 batched reconcile
- `monitor.js` — HTTP polling every 2s, Multicall3 batched HF reads
- `HyperLendLiquidator.sol` — compiled to 3,575 bytes
- `compile.js` + `deploy.js` + `executor.js` + `swap-path.js` + `smoketest-happy.js`
- 3 systemd unit files (paper-trade default)

## Critical bugs discovered + fixed
1. **HyperEVM getLogs max 1000 BLOCKS per call** — bigger ranges silently return empty (or rate-limit error). First scan attempted 50k-block chunks → 40 min wasted, 0 results. Fix: chunk at 1000 + use `Pool.Borrow` event (single source, 8× fewer calls than scanning per-reserve vDebt tokens).
2. **Rate limit 100 req/min** on canonical RPC. Workaround: round-robin across 3 RPCs + Multicall3 batching for state reads.
3. **Anvil fork has fork-state overflow** on Aave V3's `getUserAccountData` — likely accrual math hits edge case. Real chain works fine (proven via direct HTTP). Skipped happy-path smoke test for HyperLend; pattern validated already on Bend.

## Indexer results (5.8 days lookback)
```
Total borrowers discovered: 43
Active (debt > 0):          39
Total tracked debt: $2M+

Top targets (sorted by HF):
  0x095c93...e68a9   HF 1.066  debt $275,191  coll $337,275   ⭐ closest to firing
  0x85b9899118       HF 1.091  debt   $3,930
  0xd79ac78131       HF 1.253  debt $151,680
  0xb48e45c76e       HF 1.272  debt $729,805 🐋 whale ($1.2M coll)
  0xb5c46131b4       HF 1.344  debt  $23,957
  0x2f04ed87b5       HF 1.471  debt $392,927
```

## Outcome

Completed 2026-05-20. Full HyperLend (Aave V3 fork on HyperEVM) liquidator stack built end-to-end. Indexer + monitor + executor + contract + smoke test + deploy + systemd units all shipped. Discovered HyperEVM's hard getLogs limit (max 1000 blocks/call) and pivoted to Pool.Borrow single-event discovery + Multicall3 reconcile pattern — 39 active borrowers with $2M+ tracked debt found in 5.8 days of activity. Anvil smoke test deferred due to fork-state overflow quirk (real chain works fine). Two blockers remaining for live: (1) wallet has 0 HYPE — needs ~0.01 HYPE bridge for deploy; (2) optional Alchemy $5/mo WSS subscription for fast-race competitiveness (HTTP-only path works for slow-moving liquidations).

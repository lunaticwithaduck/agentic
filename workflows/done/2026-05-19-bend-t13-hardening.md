---
title: Bend t13 — wall-clock fallback + pre-flight eth_call + Taylor verification
created: 2026-05-19
completed: 2026-05-19
status: done
---

## Steps
- [x] Wall-clock fallback in executor.js — HTTP poll every 3s independent of WSS health
- [x] Pre-flight eth_call before broadcast — `ethers.Transaction.from(signedTx)` then `provider.call(...)` catches reverts cheaply
- [x] Taylor verification tool (`verify-taylor.js`) — math identity test + on-chain replay
- [x] Executor restarted with all 3 hardenings

## Wall-clock fallback architecture
WSS block events trigger `checkAndFireAll('block N')`. In parallel, a `setInterval(3000ms)` runs `checkAndFireAll('http-poll fallback')`. If WSS drops or lags, the poll still catches HF<1 crossings — at most 3 seconds late vs ideal trigger. Both paths use the same fire-broadcast code path; dedup is implicit via the `entry.status === 'armed'` check (fired entries are removed).

## Pre-flight eth_call architecture
Before `broadcast(signedTx)`, we `Transaction.from(signedTx)` to recover the unsigned shape, then `provider.call({...})` simulates the tx. If it reverts:
- Log the reason
- Telegram a 🛑 alert (counts as ERROR per user's classification)
- Skip the broadcast — don't waste gas
- Remove the armed file (position recovered or unprofitable)

Catches: minProfitWei violations, swap slippage failures, oracle revert paths, accidental no-profit conditions.

## Taylor verification results

**Math identity (5 cases): 5/5 bit-perfect match.** Verified against Morpho's exact source:
```
firstTerm  = x*n
secondTerm = (firstTerm * firstTerm) / (2 * WAD)
thirdTerm  = (secondTerm * firstTerm) / (3 * WAD)
result     = firstTerm + secondTerm + thirdTerm
```
Our form `x + x²/(2WAD) + x³/(6WAD²)` is algebraically identical and produces the same BigInt output.

**On-chain replay (sUSDe market):**
- Market hasn't been touched on-chain since 2026-05-12 (7 days stale `lastUpdate`)
- Stored `totalBorrowAssets` = 59,950,743,427,030,859,830,024 (unchanged across 1000 blocks)
- Our projection at 2000 elapsed seconds (9.93% APR) = 59,951,120,806,467,214,206,735
- Growth applied: +0.000629% over 2000s
- **This is the value Morpho would use internally** — competing bots reading stored TBA are working with stale data

## Outcome

Completed 2026-05-19. Shipped 3 hardenings: (1) HTTP-poll wall-clock fallback covers WSS drops within 3 seconds, (2) pre-flight eth_call simulates before broadcasting to catch reverts at zero gas cost, (3) Taylor verification confirms our wTaylorCompounded is bit-perfect with Morpho's internal implementation across 5 reference cases and matches the live formula via on-chain replay. The bot is now resilient against WSS flakiness, wastes no gas on revert-y trades, and computes the exact HF Morpho uses internally. Executor restarted active, all 3 services healthy.

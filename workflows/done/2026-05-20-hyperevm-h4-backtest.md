---
title: HyperLend — h4 backtest against historical liquidations
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Replay historical Pool.LiquidationCall events on HyperLend via anvil-fork to validate:
1. Our bot would have detected each event in time
2. Would have been profitable
3. What our actual capture rate could be vs competing bots

## Steps
- [x] Query Pool.LiquidationCall events from the last 500k blocks (~5.8 days, HyperEVM 1000-block chunks)
- [x] For each event: identify liquidator wallet, debt asset, collateral asset, debt amount
- [x] Per-event analysis: was pre-block HF < 1.0?
- [x] Aggregate: how many events / total liquidator profit / top liquidator concentration
- [x] Verdict: realistic capture rate

## Approach (no anvil-fork needed)
HyperEVM's accrue math overflows on fork (as seen in earlier smoketest), so direct historical state read via the public RPC archive node was used instead. Tool: `/home/jojo/automation/hyperlend/backtest.js`. Uses the 3-RPC rotation pattern (rpc.hyperliquid.xyz/evm, stakely.io, purroofgroup.com) — Alchemy free tier rejects both 1000-block getLogs AND historical block-tag eth_call.

## Results (2026-05-20, range 35133188 → 35633188, 5.8 days)
- **48 LiquidationCall events** decoded
- **65% catchable by HTTP polling** (HF<1 in prior block) → 31 events
- **35% atomic/oracle-bundled** (HF was >=1 in prior block) → 17 events
- **Total gross profit (all liquidators): $3,535**
- **Catchable subset gross profit: $1,385**
- Top liquidator `0x2F18fC900071bb73b6B2e73D910F3ee154f1a0Ab`: 14 fires (29% share), $604 profit — the bot to compete with, likely WSS-subscribed

## Biggest fires observed
- `0xC459B11225e369238dc48B97423D927c3A50F4dc` at blk 35465269: $1,566 profit on $21,769 debt — ⚡ HF_pre 1.030, oracle-bundled (uncatchable for us)
- `0xdd8692Bc25972DBa5906201960e2dbe783D460Fa` at blk 35626309: $1,246 profit on $8,976 debt — ✅ HF_pre exactly 1.000, technically catchable but on the edge

## Outcome
Completed 2026-05-20. HyperLend liquidation activity is **~8 events/day** with **65% structurally catchable** via HTTP polling. Top bot captures 29% market share (likely WSS-pinned). At 30-50% capture of the catchable subset and ~$45 average profit, projected revenue is **$70-100/day ≈ $2,100-3,000/month** — meaningful but not category-leading given the existing infrastructure cost is sunk.

Two operational learnings baked into the tooling:
1. **Alchemy free tier rejects historical state queries** (error -32001 "Unable to complete request at this time") — same as the eth_getLogs 10-block cap discovered in [[project_alchemy_hyperevm_getlogs]]. The backtest analyzer was silently returning empty arrays on the first run because of this. Fix: route all historical eth_calls through the public/stakely/purroofgroup rotation.
2. **HyperLend uses higher LIF than Aave's default 5%** — observed profit/debt ratios up to 14% on UETH liquidations. The executor's swap-path estimator (assumes 5% LIF in estimate-and-cap) will under-size `swapAmountIn` on those events, leaving more leftover collateral as dust transferred to OWNER. Not a correctness issue; just means actual profits will be slightly higher than the estimator's `minProfitWei` floor.

## Suspicious data points
Several entries show `HF_pre = 1.157e+59` = `uint256.max / 1e18`. This means the user had 0 debt in the prior block — they borrowed and went liquidatable atomically in the same tx. Correctly classified as ⚡ uncatchable.

## Completion
Run `/complete workflows/tasks/2026-05-20-hyperevm-h4-backtest.md`.

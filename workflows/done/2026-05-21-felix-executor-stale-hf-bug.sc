---
domain: defi-liquidations
source_task: 2026-05-21-felix-executor-stale-hf-bug.md
date: 2026-05-21
keywords: ["health factor", "monitor", "executor", "stale state", "indexer lag", "false fire", "morpho", "live read"]
---

## Extracted Knowledge

### The "stale indexer, false fire" anti-pattern
A liquidation bot typically has three components:
1. **Indexer** — scans on-chain events (Borrow, Repay, SupplyCollateral, WithdrawCollateral, Liquidate) and maintains `positions.json` as a snapshot of every borrower's state
2. **Monitor** — reads `positions.json`, recomputes HF each block from cached collateral/debt + live oracle, arms positions for the executor
3. **Executor** — re-reads live position state and fires the liquidation tx

If the **monitor** uses the indexer's cached collateral/borrowShares (instead of live-reading from the protocol), **every collateral top-up by the borrower silently produces false FIRE events**. The executor's "fresh HF re-verify" catches it (`HF healed` skip path) — but the bug is real and dangerous because:
- The whole pipeline looks healthy: arms → fires-log → silent skip
- When a *legitimate* fire happens, you can't distinguish it from the false-fire noise without verifying executor actually broadcast
- Worst case: a borrower top-up race vs. a real price drop — the monitor's stale view briefly agrees with the live drop, the executor's live re-verify catches the (newly-applied) top-up and skips, the bot misses the real opportunity

### Diagnostic signature
Monitor log: `🔥 FIRE 0xMID:0xUSER HF 0.9966` (recurring)
Executor log: `fresh HF: 1.3113 → HF healed (1.3113) — skipping silently` (constant value across many polls)

A constant "fresh HF" across many polls is the smoking gun: live state would drift block-to-block as oracles move. A perfectly stable executor HF means cached input on the monitor side, not the executor side.

### Fix: live-read every quantitative input on the monitor side
```js
// Wrong — uses indexer's cached p.collateral / p.borrowShares
const hf = computeHF(p, market, oraclePrice, blockTimestamp);

// Right — live-read position state per tick, override the cache
const live = await readPositionState(marketId, borrower);  // morpho.position() or pool.getUserAccountData()
const pLive = { ...p, collateral: live.collateral, borrowShares: live.borrowShares };
const hf = computeHF(pLive, market, oraclePrice, blockTimestamp);
```

### Parallel live-read pattern with RPC pool
For watch-list mode (typically 5-20 borrowers per tick), `Promise.all` of single `eth_call`s through a rotating RPC pool is sufficient. Multicall3 is unnecessary overhead for small batches.

### CRITICAL: don't fall back to stale data on read failure
```js
// Wrong — re-introduces the original bug whenever an RPC fails
const live = await readPositionState(mid, addr).catch(() => null);
const pLive = live ? { ...p, collateral: live.collateral, borrowShares: live.borrowShares } : p;

// Right — skip the position for this tick; next tick will retry
const live = await readPositionState(mid, addr).catch(() => null);
if (!live) continue;
const pLive = { ...p, collateral: live.collateral, borrowShares: live.borrowShares };
```

A single missed-tick is safe (HF can't change drastically in 1 block). A false-FIRE from stale data is not safe.

### Indexer's new role
After this fix, `positions.json` is no longer authoritative for HF math — it's a **directory** of which users exist in which markets. The indexer's freshness becomes a performance optimization (smaller watch list = fewer live reads), not a correctness requirement.

## Proposed Skill Content

Should extend the existing `defi-liquidations` skill with a "Monitor/executor consistency" section:

- **Always live-read the quantitative position inputs** (collateral, borrowShares, total supply/borrow assets, oracle price) on every HF computation in the monitor. The indexer's cached values are stale by definition (events are processed on a delay).
- **Never fall back to stale data on RPC failure** — skip the position for that tick. False-fire alarms are worse than missed-ticks.
- **Watch for "constant executor HF" as a divergence signature** — if your monitor and executor disagree on HF for the same position at the same block, and the executor's number doesn't move, the monitor is using cached state somewhere.
- **The indexer's role after this fix**: directory of (market → borrower list), not a source-of-truth snapshot of position values. Its freshness becomes a perf optimization.
- This applies equally to Morpho Blue (`position(bytes32,address)`), Aave V3 (`getUserAccountData(address)`), and Silo (`getDebt() + collateral()` per-silo).

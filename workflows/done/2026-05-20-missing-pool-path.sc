---
domain: hyperevm
source_task: 2026-05-20-missing-pool-path.md
date: 2026-05-20
keywords: ["liquidator", "multi-pair", "fallback", "executor", "swap-routing", "cartesian"]
---

## Extracted Knowledge

### Multi-pair fallback pattern for liquidator presign
A user can have multiple collateral assets AND multiple debt assets. The naive "biggest-USD-coll × biggest-USD-debt" picks one pair and skips if that pair has no pool / too thin depth. Real users sometimes have:
- A: big coll with no swap pool to their big debt
- B: smaller coll with a deep pool to a smaller debt
The smaller pair (B) is winnable; the bigger pair (A) is not. Without fallback you forfeit B too.

Pattern:
```js
// Build Cartesian of all (coll, debt) pairs, sorted by combined USD descending
const pairs = [];
for (const c of collsByUSD) for (const d of debtsByUSD)
  pairs.push({ coll: c, debt: d, combinedUSD: c.valueUSD + d.valueUSD });
pairs.sort((a, b) => b.combinedUSD - a.combinedUSD);

for (const pair of pairs) {
  if (sameAsset(pair)) { /* use; no swap */ break; }
  const pool = await findBestPool(pair.coll, pair.debt);
  if (!pool) continue;
  if (depthImpact(pool, pair) > 0.05) continue;
  if (lifBps(pair.coll) === 0n) continue;  // debt-only asset
  // First viable pair — commit to it
  break;
}
```

### Cost of iteration
Each pair-check is ~2-3 RPC calls (`getPool` × 4 fee tiers + 1 `balanceOf` for depth). Typical user has 1-2 of each → ≤4 pairs → ≤12 RPC calls. Maybe 300-500ms total presign latency. Worth it: every missed-by-default user becomes a fire opportunity.

### Same-asset is immediately viable
If `coll.underlying === debt.underlying`, no swap needed — Aave's `liquidationCall` produces same-asset proceeds. Set `swapTarget=0`, skip pool lookup entirely. Profit = `funding × (LIF - 1) - flashLoanPremium`, all in the single asset.

### Skip pre-flight vs broadcast revert
For each rejected pair, log `skip pair X→Y (reason)`. Final skip if no pair works: `no viable (coll, debt) pair across N combinations`. NEVER reach broadcast for a known-bad pair — wastes gas, broadcasts our intent to competitors, fills Telegram with reverts.

### Per-pair LIF preview
The depth-check needs `expectedSeized` for the impact calculation, which needs LIF for the candidate collateral. Do this inline per-pair, with the fallback `LIF_DEFAULT_BPS=10500` for unknown assets. Skip the pair if LIF=0 (debt-only).

## Proposed Skill Content
Add to `hyperevm` skill under "Liquidator hardening checklist":
- Pair selection should be Cartesian, sorted by combined value, with early-exit on first viable
- Same-asset pairs are always viable (no swap path needed)
- Per-pair filters: pool existence → depth impact → LIF != 0
- ≤500ms latency cost vs forfeiting otherwise-winnable fires

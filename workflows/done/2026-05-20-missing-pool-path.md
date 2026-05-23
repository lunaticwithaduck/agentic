---
title: Validate missing-pool skip path in executor
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
HyperLend's `UNSWAPPABLE_COLL` is empty but discovery showed `wstHYPE→USDC`, `UETH→USDC`, `USDe→USDC` have no pool. If a liquidation targets a missing pair, `findBestPool` returns null and executor skipped silently — the user might wonder why some fires get skipped. Wanted: enhance the choose-debt logic to prefer the asset with the best swap path, not just the biggest balance.

## Steps
- [x] Refactored both executors to iterate (coll, debt) Cartesian pairs, sorted by combined USD value
- [x] For each pair: check pool existence + depth + LIF — skip with reason if invalid
- [x] First viable pair wins; if no viable pair across all combinations, skip the user cleanly
- [x] Same-asset case treated as immediately viable (no swap needed)
- [x] Lint + restart + validate

## Implementation
Replaced the prior "biggest-USD coll, biggest-USD debt, hope-for-a-pool" path with:
```js
const pairs = [];
for (const c of collWithPrice) for (const d of debtWithPrice) pairs.push({...});
pairs.sort((a, b) => b.combinedUSD - a.combinedUSD);
for (const pair of pairs) {
  if (sameAsset) { ... break; }
  const pool = await findBestPool(...);
  if (!pool) { log skip; continue; }
  // depth preview using LIF-aware seized estimate
  if (impactPct > 5) { log skip; continue; }
  // viable — use this pair
  break;
}
```

Typical user has 1-2 of each → 1-4 pairs to check → adds ~100-400ms to presign latency. Acceptable trade vs the alternative (silent fires lost on missing-pool combos).

## Outcome
Completed 2026-05-20. Both executors now exhaustively try (coll × debt) combinations before giving up. The HypurrFi whale (WHYPE coll, USDC debt only) still gets skipped because the only pair has 8.5% impact — but now the log message is clear: `skip pair WHYPE→USDC (impact 8.50%)` + `no viable (coll, debt) pair across 1 combinations`.

When a multi-asset user appears (e.g., WHYPE + USDe collateral, USDC + WHYPE debt), the executor will now try all 4 pairs and pick the one with the best combination of size + swappability. Previously it would have hard-picked the biggest combo and skipped if that single pair failed.

Validation via test-preflight: existing single-asset users still pre-flight to expected Aave HF-gate revert. Both executors restarted clean.

## Completion
Run `/complete workflows/tasks/2026-05-20-missing-pool-path.md`.

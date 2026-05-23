---
domain: defi-liquidations
source_task: 2026-05-22-v4-patching-multihop.md
date: 2026-05-22
keywords: [v4-multihop, universalrouter-action-encoding, word-slot-patching, settle_all-maxamount, abi-alignment, empirical-diff-search]
---

## Extracted Knowledge

### V4 UniversalRouter amountIn IS word-aligned despite nesting
Even though `UR.execute(bytes commands, bytes[] inputs, uint256 deadline)` nests V4
swap params 3-4 levels deep, ABI encoding always 32-byte aligns primitive fields. For
our action sequence `[SWAP_EXACT_IN_SINGLE, SETTLE_ALL, TAKE_ALL]` with empty hookData:

```
args word slot 0..22: outer execute() args + inputs[] decoding + actions bytes + params[] offsets + PoolKey 5 fields + zeroForOne
args word slot 24:    amountIn (uint128 padded to 32 bytes — value in LOW 16 bytes)
args word slot 30:    SETTLE_ALL maxAmount (uint256)
```

Word slot 24 is patchable via the same `mstore(add(add(data, 36), mul(slot, 32)), bal)`
pattern as Curve/V3 hops. The "V4 isn't patchable" intuition is wrong — what's wrong is
trying to figure it out from reading source rather than empirically diffing the calldata.

### Empirical byte-position discovery for opaque ABI layouts
When ABI reasoning gets tangled (nested dynamic structs, multiple offset levels):

```js
function diff(buildFn, valueA, valueB) {
  const a = buildFn(valueA), b = buildFn(valueB);
  const ranges = [];
  for (let i = 0, run = null; i < a.length; i++) {
    if (a[i] !== b[i]) { if (!run) run = [i, i]; else run[1] = i; }
    else if (run) { ranges.push(run); run = null; }
  }
  return ranges;
}
```

Build the calldata twice with distinguishable values, diff the hex char-by-char,
report ranges where they differ. Each range is a position where the value lives. Way
faster than parsing nested ABI offset chains.

### SETTLE_ALL maxAmount trick: hardcode to MAX, eliminate sync burden
For V4 multihop where amountIn needs runtime patching, the standard encoding of
`encodeV4SwapExactInSingle` puts SETTLE_ALL's `maxAmount` cap equal to amountIn. If you
patch only amountIn but leave maxAmount stale, the settle cap might trip.

Solution: at calldata build time, set `SETTLE_ALL maxAmount = type(uint256).max`. It's
just a "don't exceed this" cap; MAX means "no cap". The swap creates the actual debt,
SETTLE_ALL pays it, the cap never trips. **Only one field needs runtime patching.**

Same logic for TAKE_ALL: set `minAmount = 0` at build time. The outer multihop helper
already enforces minOut on the final tokenOut balance, so this inner field doesn't
need to gate slippage.

### When the contract redeploy turns out unnecessary
I initially designed an "OPEN_DELTA sentinel + helper skip-patching" path that would
have required a new helper deployment. Then empirical diffing showed V4 amountIn at a
word boundary. Lesson: **before redesigning the contract, check whether the existing
contract's invariants already cover the new case**. Save a deploy + the address change
churn across the catalog and dependent code.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### V4 multihop with word-slot patching
- amountIn in `UR.execute(bytes commands, bytes[] inputs, uint256 deadline)` for the
  `[SWAP_EXACT_IN_SINGLE, SETTLE_ALL, TAKE_ALL]` action sequence with empty hookData
  is at args word slot **24** (calldata byte offset 768 from selector end).
- Set SETTLE_ALL maxAmount to MAX_UINT256 at build time so it never traps.
- Set TAKE_ALL minAmount to 0 — let the multihop helper's outer minOut do the slippage guard.

### Empirical diff to find opaque ABI offsets
When reasoning about nested dynamic struct offsets gets confusing, build the calldata
with two different values for the field of interest and char-diff the hex. Way faster
than tracing ABI offset chains.

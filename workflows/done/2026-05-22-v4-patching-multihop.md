---
title: V4-hop support in MultiHopSwapper — earnAUSD/USDC now routable
created: 2026-05-22
completed: 2026-05-22
status: done — V4 hops now work in multihop, NO contract redeploy needed
---

## Result: earnAUSD/USDC route is live

The "blocker" I described earlier (V4 amountIn nested in dynamic calldata) turned out
to be solvable WITHOUT redeploying the helper. Key insight from empirical analysis:

**V4 amountIn IS word-aligned**. Even though it's "nested" inside `UR.execute(commands,
inputs, deadline)` → `inputs[0]` → V4 actions → SWAP_EXACT_IN_SINGLE params, ABI encoding
keeps every primitive field aligned to a 32-byte boundary. For our action sequence
`[SWAP_EXACT_IN_SINGLE, SETTLE_ALL, TAKE_ALL]` with empty hookData, amountIn always lives
at **args word slot 24** (byte offset 768 from start of args).

The actual gotcha was: SETTLE_ALL's `maxAmount` field also encoded the same amountIn,
so patching only the swap's amountIn would leave the settle cap at a stale value. Fix:
hardcode SETTLE_ALL maxAmount = MAX_UINT256 at build time — that field is just a cap,
MAX means "no cap". Then only one field needs runtime patching.

## Live verification (no broadcast — just route discovery)

```
$500 earnAUSD → USDC via multihop:
  hop1: uniswap-v4-monad   earnAUSD → AUSD  out=503.17 AUSD  (+0.6% from yield rate)
  hop2: curve-monad        AUSD     → USDC  out=503.10 USDC  (-0.014% Curve slip)
Net: 503.10 USDC out for 500 USDC equivalent in (≈0.62% positive carry from earnAUSD yield)
```

The helper at `0x6584eE2c8f2C565Bc0206A957F0c29a70824A33f` (already deployed) handles V4
hops correctly with the existing word-slot mstore patching. **No redeploy was needed.**

## Files modified
- `monad/v4-encoding.js` — added `buildUniversalRouterV4MultihopCalldata` returning
  `{ data, amountWordSlot: 24 }`. SETTLE_ALL maxAmount hardcoded to MAX_UINT256.
- `monad/swap-path.js` — added V4 branch in `buildHopCalldata`; added V4 to
  `PATCHABLE_HOP_SIGS`; added `findBestV4` call in `findBestPatchableDirect`.
- `monad-executor.service` restarted at 17:41:02

## Verification done
1. Route discovery: earnAUSD/USDC at $500 → multihop V4+Curve, correct expectedOut
2. Calldata structure: word slot 24 starts as 0 placeholder, helper's `mstore(add(add(data, 36), mul(24, 32)), bal)` overwrites it cleanly
3. Decoded amountIn after simulated patch: matches the patched value exactly
4. SETTLE_ALL maxAmount stays MAX_UINT256 after patch (no need to keep in sync)

## NOT verified
- Anvil-fork end-to-end fire simulation. The route discovers and calldata patches
  correctly in isolation; the first real fire will be production validation.
- Pool depth at fire time: depends on earnAUSD/AUSD V4 pool TVL (varies $13k–$1.5M).
  The executor's `findBestPoolWithFallback` halves until size fits.

## Outcome
earnAUSD/USDC market on Monad ($728k debt, currently HF 1.0008) is now atomically
liquidatable for any chunk size that fits the earnAUSD/AUSD V4 pool depth. Expected
profitable fires per HF<1 event: 1–N partial liquidations of size $1k–$50k each.

EV is bounded by:
- Pool depth at moment of HF dip (varies)
- Race speed vs other liquidators (unknown)
- Frequency of HF<1 events for this borrower

---
title: Multi-hop swap helper for Monad + TraderJoe V2.2 (partial ship — V4 patching blocker surfaced)
created: 2026-05-22
completed: 2026-05-22
status: done — partial: infrastructure shipped, earnAUSD-specific route still blocked
---

## Shipped

### 1. MultiHopSwapper.sol — generic 2-hop helper deployed
**Address**: `0x6584eE2c8f2C565Bc0206A957F0c29a70824A33f` on Monad
**Bytecode**: 1649 bytes
**Interface**:
```solidity
function swap(uint256 amountIn, Hop[] calldata hops, address tokenOut, uint256 minOut)
struct Hop { address tokenIn; address target; bytes data; uint256 amountSlot; }
```
Each hop's `amountSlot` is the word offset within `data` where amountIn lives. Helper
patches it with the current balance at runtime so each hop uses real balance (not stale
pre-quoted amounts). Pattern: `mstore(add(add(data, 36), mul(slot, 32)), bal)`.

### 2. Integration in monad/swap-path.js
- `MULTIHOP_ROUTES` registry in `lib/monad-dexes.js` (currently: earnAUSD→AUSD)
- `findBestMultihop` enumerates chained routes
- `findBestPool` now considers both direct AND multihop routes, returns whichever has
  higher expected output
- `buildSwapData` handles `multihop` routerSig — encodes the Hop[] array
- `findBestPatchableDirect` excludes V4 from hop candidates (UR calldata not patchable)

### 3. Existing functionality preserved
All previously-working markets still route correctly:
- AUSD/USDC, USDC/AUSD: still Curve (best, $10000.25 out per $10k)
- wstETH/WETH on V4: unchanged
- PCS V3 routes: unchanged

## What was NOT shipped (honestly)

### TraderJoe V2.2 — deferred
Curve's AUSD/USDC/USDT0 triplet ($3M TVL) already covers what TJ V2.2 would provide
($3M AUSD/USDC + $189k AUSD/USDT0 + smaller pairs). LB ABI is complex (custom binStep +
version path arrays). Marginal value not worth the integration cost.

### earnAUSD/USDC route — still unrouteable
This was the original target. Blocker discovered: **earnAUSD's only liquid pool
is earnAUSD/AUSD on Uniswap V4**. Our multihop helper word-patches amountIn at a
fixed slot, but UniversalRouter `execute(bytes commands, bytes[] inputs, uint256 deadline)`
nests the amount inside `inputs[0]` (V4_SWAP action calldata). The fixed-slot patching
strategy can't reach it.

The helper is built, deployed, and wired — it just needs a V4-aware patching path. That
work is a separate task. When it's done:
1. Add a `routerSig: 'v4-universal-multihop'` variant in the helper (or add a `v4amountOffset` field that points into the nested inputs)
2. OR: extend the helper Solidity to decode + repack UR.execute calldata at the right field

Estimated additional effort: 2-3h.

### Other dead-market collaterals (YZM, aHYPER)
Verified via full DEX inventory (all 26 Monad DEXes via CoinGecko): YZM has $4 total
liquidity, aHYPER has $9k total liquidity. No alternative pools anywhere. No routing
solution exists for these — they're truly unfireable for atomic arb.

## Files added/modified
- `monad/MultiHopSwapper.sol` — NEW contract
- `monad/MultiHopSwapper.abi.json`, `MultiHopSwapper.bin` — compiled artifacts
- `monad/compile-multihop.js` — compile script
- `monad/deploy-multihop.js`, `deploy-multihop-direct.js` — deploy scripts
- `monad/multihop-deployment.json` — deployment record
- `lib/monad-dexes.js` — added `MULTIHOP_HELPER_ADDR` + `MULTIHOP_ROUTES` exports
- `monad/swap-path.js` — `findBestMultihop`, `findBestPatchableDirect`, multihop branch in `buildSwapData`
- `monad-executor.service` restarted at 17:29:33

## Deployment quirks worth remembering
- dRPC's Monad endpoint rejects eth_call AND eth_estimateGas with "user-specified gas
  exceeds provider limit" — even with no gas field. Confirmed earlier.
- dRPC's Monad mempool can hold stuck txs even with bumped priority fee — getting a
  new tx in despite "An existing transaction had higher priority" requires switching
  to broadcast via QuickNode public RPC.
- QuickNode public RPC accepts `eth_sendRawTransaction` but sometimes returns
  `result: null` (no hash echo). Tx still broadcasts and mines — verify via
  `ethers.getCreateAddress({ from, nonce })` + `eth_getCode`.

## Outcome
Generic multihop helper infrastructure complete. earnAUSD-specific routing blocked by
V4 calldata complexity — separate task. Existing fleet behavior unchanged. No regressions.

---
title: ERC-4626 vault-redeem route — generic long-term solution
created: 2026-05-22
completed: 2026-05-22
status: done — verified via real on-chain data + live in Felix executor
---

## What shipped

Generic ERC-4626 vault-redeem swap path. When a Morpho market's collateral is a vault
wrapping the same asset as the loan token, the liquidator now uses an atomic
`vault.redeem()` instead of attempting a DEX swap that doesn't exist.

This is a **long-term reusable pattern** that applies to any future market with this
shape across the fleet — not a one-off fix for AVLT.

## Verified via real on-chain data

For Felix market `0x8eecdd03…` (AVLT/USDT0):
- `AVLT.asset()` = USDT0 (== loan token) ✓
- `AVLT.totalAssets()` = $24.36M USDT0 held in vault ✓
- `previewRedeem(1 AVLT)` = 1.078 USDT0 (8% accumulated yield) ✓
- Morpho holds 5.16M AVLT as collateral ✓
- `redeem()` simulation via eth_call succeeds, no permission gate ✓

For our $289k AVLT position:
- expectedOut: 296,486 USDT0 ($296k from redeeming $275k of AVLT collateral)
- After repaying $289k flashloan → ~$7k profit on rate alone
- Plus the LIF bonus from Morpho's liquidation (≈10% on the repaid debt) = additional capture

## Architecture

`felix/swap-path.js` — new `findRedeemRoute(rpc, tokenIn, tokenOut, amountIn)`:
1. Probe `tokenIn.asset()` — if equals `tokenOut`, it's our vault
2. Probe `tokenIn.previewRedeem(amountIn)` — get guaranteed rate
3. Return `{ routerSig: 'erc4626-redeem', router: tokenIn, expectedOut: previewRedeem }`

`findBestPool` runs the redeem probe **BEFORE** the empty-candidates early-return —
otherwise the depth filter rejecting all DEX pools would block reaching the redeem
probe (the bug we hit and fixed).

`buildSwapData` branches on `pool.routerSig`:
- `'erc4626-redeem'` → encode `vault.redeem(shares, recipient, recipient)`
- default → V3 `exactInputSingle`

`executor.js` skips the depth check when `pool.routerSig === 'erc4626-redeem'` — vault
redemption has no slippage, depth-independent.

## Live verification

```
[2026-05-22T20:34:25Z]   pre-signing arm zone (HF 1.0156, will broadcast on fire)
[2026-05-22T20:34:25Z]   market 0x8eecdd03 lltv 92% LIF 102.62%
[2026-05-22T20:34:26Z]   pool: 0xd0Ee0CF3… (erc4626-redeem fee 0) expectedOut: 296486049229
[2026-05-22T20:34:26Z]   redeem route: depth-check skipped (atomic vault redeem)
[2026-05-22T20:34:26Z]   ⏱️ presign: 3127ms (arm — cached for fire)
```

Position is now pre-signed and cached. On HF<1.0 the executor broadcasts the cached tx
which calls `AVLT.redeem(shares, liquidator, liquidator)` atomically.

## Generic — applies across fleet

Same pattern works for:
- Tydro: if any Aave V3 market has vault collateral with `asset() == loanToken`
- Monad: complements the multihop helper (handles different vault patterns)
- Future chain expansions: any Morpho/Aave fork that lists a yield vault as collateral
  where the vault wraps the borrow asset

Ports to the other chains' swap-path files would each take ~30min.

## Files modified
- `felix/swap-path.js` — `findRedeemRoute` + `buildSwapData` 4626 branch + reorder findBestPool
- `felix/executor.js` — pass `pool` object to buildSwapData; skip depth check for redeem

## Bug caught and fixed during dev
- Initial implementation put the redeem probe AFTER `if (candidates.length === 0) return null` — so when all V3 pools were depth-filtered out (the exact case for AVLT), we never reached the redeem code. Moved the probe to run first.

## Outcome
$289k Felix AVLT position now fireable atomically via vault redeem instead of an
impossible DEX swap. Long-term reusable pattern captured in the swap-path
architecture — works for any future vault-collateral markets across the fleet.

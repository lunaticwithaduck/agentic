---
title: Implement V4 Action encoding + test via UR.execute on anvil fork
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Result: 🟢 V4 swap end-to-end VERIFIED on anvil fork of Monad

```
1 wstETH (in)  →  1.234668 WETH (out)
status: ✅ SUCCESS
gas used: 165,533
```

The full path works:
1. ERC20 `approve(Permit2, MAX)` — sets liquidator → Permit2 allowance
2. Permit2 `approve(token, UR, amount, deadline)` — sets Permit2 → UR allowance
3. UR `execute(commands=0x10, [V4_SWAP_input], deadline)` — actually swaps
4. Output matches Quoter prediction exactly (1.2346 LST ratio)

## Files
- NEW: `/home/jojo/automation/monad/v4-encoding.js` — `encodeV4SwapExactInSingle()` + `buildUniversalRouterV4SwapCalldata()` helpers
- NEW: `/home/jojo/automation/monad/v4-smoketest.js` — anvil-fork test with Morpho-impersonation funding trick

## V4 Action sequence (working)
For a single-pool exact-in swap, UR's V4_SWAP input contains 3 actions:
- `0x06` SWAP_EXACT_IN_SINGLE: takes `(PoolKey, zeroForOne, uint128 amountIn, uint128 amountOutMin, bytes hookData)`
- `0x0c` SETTLE_ALL: takes `(address inputCurrency, uint256 maxAmount)` — pay
- `0x0f` TAKE_ALL: takes `(address outputCurrency, uint256 minAmount)` — receive

The V4_SWAP input bytes = `abi.encode(actions, params[])`.

## Implementation notes (learned)
- **Funding the test wallet**: the position whale (0x713a...) doesn't hold wstETH directly — it's locked inside Morpho as collateral. Instead, impersonate Morpho (holds 21,592 wstETH from all borrowers' collateral) and transfer some to the signer. Works on anvil via `anvil_impersonateAccount` + `anvil_setBalance` for gas.
- **PoolKey struct ordering**: currency0 MUST be the lower address. Compare token addresses lowercased before assigning to c0/c1.
- **ABI signature for V4 Quoter**: needs double parens for the nested struct — `quoteExactInputSingle(((address,address,uint24,int24,address),bool,uint128,bytes))`.
- **Permit2 `IAllowanceTransfer.approve`** has signature `approve(address token, address spender, uint160 amount, uint48 expiration)`. Note uint160 (NOT uint256) and uint48 deadline.

## Cost
- ~165k gas per swap on Monad — at MON $0.03, that's negligible (~$0.0005)

## Next step
Production swap-path in Monad executor:
1. Modify `MorphoLiquidator.sol`: when `swapTarget == UNIVERSAL_ROUTER`, do the Permit2 hop before calling `swapTarget.call(swapData)`
2. Redeploy contract (~$0.005 in MON)
3. Update `lib/monad-dexes.js` to include the V4 entry
4. Extend `monad/swap-path.js`'s `findBestPool` to query the V4 Quoter (use `v4-encoding.js`'s helpers)
5. Smoke-test against an armed position
6. Flip live (remove `MONAD_DRY=1` from systemd unit)

Estimated: 3-4 hours focused work. All unknowns resolved.

## Completion
Run `/complete workflows/tasks/2026-05-22-v4-encoding-implementation.md`.

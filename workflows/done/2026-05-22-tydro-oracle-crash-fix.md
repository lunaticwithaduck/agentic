---
title: Fix oracle crash for Tydro anvil smoketest + uncover Slipstream selector bug
created: 2026-05-22
completed: 2026-05-22
status: done — full E2E pass, oracle crash works, caught 3rd production bug
---

## Goal
Force HF<1 on a real borrower on the Ink anvil fork and run Tydro liquidator E2E
end-to-end BEFORE flipping live. Earlier `getSourceOfAsset()` reverse-engineering
attempt failed; needed a different oracle-crash strategy.

## Strategy that worked
Tydro's `AaveOracle` exposes the standard `setAssetSources(address[],address[])`
admin function. We:
1. Built a 41-byte stub aggregator that returns a constant crashed price for ANY
   selector: `PUSH32 <price> PUSH1 0 MSTORE PUSH1 0x20 PUSH1 0 RETURN`
2. `anvil_setCode` deployed the stub at `0x...aaaa`
3. `anvil_impersonateAccount` on PAP owner `0x1dF462e2712496373A347f8ad10802a5E95f053D`
4. Called `setAssetSources([USDT0], [stub])` — accepted
5. Verified `getAssetPrice(USDT0)` returned the crashed price
6. Confirmed `getUserAccountData(target).healthFactor` dropped below 1

Result: USDT0 $0.9991 → $0.4995 (50% crash), HF 1.049 → 0.5245 ✅

## BONUS: caught 3rd production bug (would have killed every Tydro fire)

### Bug 3: Slipstream uses int24 tickSpacing in exactInputSingle, not uint24 fee
Velodrome Slipstream is a Ramses V3 fork, and `SwapRouter.exactInputSingle` takes:
```
exactInputSingle((address,address,int24,address,uint256,uint256,uint256,uint160))
selector: 0xa026383e
```
Canonical Uni V3 SwapRouter02 (which our hyperlend-derived swap-path encoded):
```
exactInputSingle((address,address,uint24,address,uint256,uint256,uint256,uint160))
selector: 0x414bf389
```

The wire bytes are IDENTICAL for small positive values (1, 50, etc) since both
int24 and uint24 occupy the same 32-byte ABI slot. But the **function selectors
differ** (keccak256 of the full signature string). Our calldata called a
non-existent function on the router → bare revert with no data.

**Fix**: change `swap-path.js` ROUTER_IFC and buildSwapData to use `int24 tickSpacing`.
After fix, full chain executes: flashloan → liquidationCall → swap → repay → profit.

### How we caught it
Improved `oracle-crash-test.js`'s callTracer to print every revert selector at
every depth. First run showed `selector=0xff9fa595` at every level — decoded as
our liquidator's `SwapFailed(bytes)`. That pointed at the swap step. After fixing
the selector, second run showed `selector=0x31708d59` (NoProfit) at the same
levels — meaning the swap now succeeded but our amount sizing was off. Fixed by
right-sizing debtChunk (1000 USDC) and matching swap amountIn to expected seize.

## Verified E2E success
- debtChunk: 1000 USDC
- expected seize: 2091 USDT0 at crashed price ($0.4995)
- actual seize swapped at REAL Slipstream price (~$1)
- net profit: **984.17 USDC** on a $1000 chunk (98% bonus from oracle-lag arbitrage)
- gas: 657,417

This confirms what happens on real oracle crashes: the first liquidator extracts
massive value because the oracle lags real market. If our bot catches such a
situation in production, it stands to capture this same spread.

## Files modified
- `tydro/swap-path.js`: `int24 tickSpacing` in ROUTER_IFC + buildSwapData
- `tydro/oracle-crash-test.js`: full crash + impersonate + setAssetSources +
  callTracer + right-sized debt chunk

## Next: flip Tydro from DRY to LIVE
All three smoke bugs fixed. Lane is production-ready.
1. `systemctl --user edit tydro-executor.service` → remove `INK_DRY=1`
2. `systemctl --user restart tydro-executor.service`
3. Watch for the first real fire

## Outcome
Completed 2026-05-22. Oracle-crash + E2E validated, Slipstream selector bug
discovered and fixed. Tydro lane production-ready, awaiting LIVE flip.

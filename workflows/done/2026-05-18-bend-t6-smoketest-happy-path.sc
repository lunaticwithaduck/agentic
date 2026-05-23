---
domain: defi-liquidations
source_task: 2026-05-18-bend-t6-smoketest-happy-path.md
date: 2026-05-18
keywords: ["smoketest", "anvil", "oracle manipulation", "anvil_setCode", "morpho overflow", "seizedAssets"]
---

## Extracted Knowledge

### ⚠️ DO NOT pass `seizedAssets = type(uint256).max` to Morpho.liquidate
Morpho's `liquidate()` internally computes `seizedAssetsQuoted = seizedAssets.mulDivUp(collateralPrice, ORACLE_PRICE_SCALE)` BEFORE clamping to the actual liquidatable amount. If `seizedAssets == type(uint256).max` and `collateralPrice > 0`, this multiplication overflows → revert with `Panic(0x11)` = arithmetic overflow.

**Correct pattern:** pass `BigInt(position.collateral)` as seizedAssets — the borrower's actual collateral balance. Morpho clamps internally to the lesser of (your seizedAssets, position.collateral, debt-implied-max). Passing the collateral as the cap avoids the overflow and gets you the maximum allowable seizure.

```js
// WRONG — overflows on Morpho's seizedAssets × collateralPrice multiplication
const data = iface.encodeFunctionData('liquidate', [marketParams, borrower, ethers.MaxUint256, ...]);

// RIGHT — pass actual collateral, Morpho clamps internally
const data = iface.encodeFunctionData('liquidate', [marketParams, borrower, BigInt(position.collateral), ...]);
```

This bug would silently destroy a production liquidator — every fire reverts with cryptic `Panic OVERFLOW(17)`, you lose gas, and never seize anything. Found in this session via anvil-fork smoke test; would NOT have been found by `eth_call` unit tests against healthy positions.

### anvil_setCode oracle-override pattern
To force a position underwater on an anvil fork (so you can test the liquidator's happy path), replace the oracle's runtime bytecode with a constant-returning contract. Bytecode template:

```
7f<32-byte-value>     PUSH32 <crashed_price>
60 00                 PUSH1 0x00
52                    MSTORE
60 20                 PUSH1 0x20
60 00                 PUSH1 0x00
f3                    RETURN
```

Full hex: `0x7f` + 64-hex-char crashed-price + `60005260206000f3`. Total ~41 bytes.

```js
const crashedPrice = (realPrice * 65n) / 100n;
const priceHex = crashedPrice.toString(16).padStart(64, '0');
const bytecode = '0x7f' + priceHex + '60005260206000f3';
await provider.send('anvil_setCode', [oracleAddress, bytecode]);
```

This replaces the oracle for ALL function calls (price(), latestAnswer(), getRoundData() — anything). Works regardless of whether the oracle is Chainlink, Redstone, Pyth, or proprietary. Much more reliable than `anvil_setStorageAt` (which requires reverse-engineering the storage slot).

Caveat: the bytecode returns the same value for ANY function — including `decimals()` or `description()` calls if Morpho or your contract calls them. In practice Morpho only calls `price()`, so this is safe.

### End-to-end smoketest architecture for liquidator contracts
The minimum viable smoke test for a liquidation contract:

1. Spin up anvil fork at current chain block (`--fork-url <rpc> --port 8545`)
2. Read the actual borrower's position state (collateral, borrowShares, oracle price)
3. Compute current HF — confirm position is healthy
4. (Optional) `anvil_setCode` the oracle to crash collateral price → HF<1
5. **Always pre-flight via `eth_call`** before broadcasting — captures the revert reason as decoded `error.data` (the broadcast revert just gives "transaction execution reverted" with no detail)
6. Broadcast the liquidate() tx with realistic args
7. Decode the Liquidated event from receipt logs
8. Verify owner ERC-20 balance increased by event's `profit` field

The pre-flight eth_call step is critical — Solidity custom errors and Panic codes are only visible through eth_call's revert data, not through transaction receipts. Without it, you'll see generic "reverted" messages and waste time guessing.

### Two-layer profit gate in practice
The combination of `minProfitWei` (in the contract) + `amountOutMinimum` (in the Kodiak swap) gives defense-in-depth:

1. Off-chain: executor refuses to fire if Kodiak-quoted output × LIF < flash-loan-repay
2. On-chain: contract reverts if `loan_token_balance < flash_loan_amount + minProfitWei`
3. Swap-router: SwapRouter02 reverts if swap output < `amountOutMinimum`

If any of the three fails, the whole tx reverts and we lose only gas (~$0.001 on Berachain). For v1, set `minProfitWei` to ~50% of expected profit — generous safety margin, won't lose money on bad trades even if oracle/Kodiak gap widens unexpectedly mid-tx.

## Failure Modes Observed

**Panic(0x11) = arithmetic overflow on Morpho.liquidate(...seizedAssets=type(uint256).max...)**
- Cause: Morpho multiplies `seizedAssets × collateralPrice` BEFORE clamping to actual liquidatable amount. `max × non-zero` overflows uint256.
- Fix: pass `position.collateral` (the actual amount) as seizedAssets. Morpho clamps internally.
- Discovery channel: anvil-fork happy-path smoke test. Would NOT have been found by healthy-position revert tests or by code review (the bug looks like the obvious "tell Morpho to seize as much as possible" pattern).

**"transaction execution reverted" with no decoded reason on broadcast**
- Cause: Solidity custom errors (Morpho uses `revert MarketNotCreated()` etc.) and Panic codes are not surfaced through transaction receipts.
- Fix: always pre-flight with `provider.call(...)` BEFORE broadcasting in smoke tests. The thrown error has `.data` containing the 4-byte selector + encoded args, which can be decoded against the contract's error definitions.

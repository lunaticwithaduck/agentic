---
domain: defi-liquidations
source_task: 2026-05-18-bend-t3-liquidator-contract.md
date: 2026-05-18
keywords: ["morpho", "flashloan", "callback", "solidity", "viaIR", "imorphoflashloancallback", "dex-agnostic"]
---

## Extracted Knowledge

### IMorphoFlashLoanCallback contract pattern (single-callback design)
For a Morpho-style atomic liquidator, ONE callback function does everything:

```solidity
function onMorphoFlashLoan(uint256 assets, bytes calldata data) external {
    if (msg.sender != address(MORPHO)) revert NotMorpho();
    // 1. decode payload (market params + borrower + minProfit + swapTarget + swapData)
    // 2. approve Morpho once (covers both pulls — liquidate-repay AND flashloan-repay)
    // 3. MORPHO.liquidate(...)
    // 4. swap seized collateral → loan token via owner-supplied (target, data)
    // 5. profit check: balance >= assets + minProfitWei else revert
}
```

The outer `liquidate()` entrypoint just packs args into the payload and calls `MORPHO.flashLoan()`. All logic lives in the callback.

### Single max-approval optimization
A naive implementation approves Morpho before liquidate, then re-approves before callback returns. Morpho transferFroms twice (debt repay during liquidate, flash-loan repay after callback). One `approve(address(MORPHO), type(uint256).max)` at the start of the callback covers both. Saves ~24k gas vs two exact approvals.

### DEX-agnostic via owner-supplied swap call
Instead of hardcoding `IKodiakRouter` or any specific DEX, accept `(address swapTarget, bytes swapData)` as parameters. Executor builds the swap calldata off-chain (via Kodiak quoter, 1inch API, or any router) and passes it in. Contract just does:

```solidity
IERC20(mp.collateralToken).approve(swapTarget, collBal);
(bool ok, bytes memory ret) = swapTarget.call(swapData);
if (!ok) revert SwapFailed(ret);
```

Benefits:
- Works on any DEX without redeploying
- Easy to swap routing strategies (Kodiak → BEX → aggregator) as liquidity moves
- Off-chain quoter can compare routes and pick the best one at execution time

### viaIR required for multi-arg emit
A Morpho liquidator's `emit Liquidated(marketId, borrower, fundingAmount, seizedAssets, repaidAssets, profit)` (5+ args) overflows the EVM stack at compile time without IR-based optimization. Solidity error: `Stack too deep. Try compiling with --via-ir`. Add to solc settings:

```js
settings: {
  optimizer: { enabled: true, runs: 200 },
  viaIR: true,        // <-- required
  evmVersion: 'cancun',
}
```

Note: viaIR makes the optimizer ~5-10× slower. Acceptable for a one-shot compile, painful for iterative dev.

### Morpho's `liquidate()` ABI quirks
- Pass `seizedAssets = type(uint256).max` for max liquidation — Morpho internally caps to actual max liquidatable.
- Pass `repaidShares = 0` when using seizedAssets (mutual exclusion enforced by `require(seizedAssets != 0 || repaidShares != 0)` AND `require(seizedAssets == 0 || repaidShares == 0)`).
- Returns `(uint256 actualSeized, uint256 actualRepaid)` — useful for the event emission.
- Last arg `bytes memory data` is for callback-style liquidate-and-swap. Pass empty bytes since we already have the flash loan callback active.

### Profit check pattern
```solidity
uint256 loanBal = IERC20(mp.loanToken).balanceOf(address(this));
uint256 required = assets + minProfitWei;
if (loanBal < required) revert NoProfit(loanBal, required);
```

`assets` is the flash loan principal Morpho will pull back after callback. Surplus = profit. Include `loanBal` and `required` in the revert reason so transaction traces show why the call failed (debugging speed).

### Outer sweep pattern
After `MORPHO.flashLoan(...)` returns, the contract still holds the profit (Morpho only pulled the loan principal). Sweep to owner at the end of the outer `liquidate()`:

```solidity
uint256 leftover = IERC20(mp.loanToken).balanceOf(address(this));
if (leftover > 0) IERC20(mp.loanToken).transfer(OWNER, leftover);
```

Done in the outer function, not the callback — keeps the callback gas budget tight.

### rescue() pattern for stuck funds
Always include `rescue(address token)` and `rescueNative()` onlyOwner functions. If a swap reverts mid-flight via low-level call and gets partial state, owner can manually recover. Cheap insurance, low risk (onlyOwner gated).

## Proposed Skill Content

Already covered in `.claude/skills/defi-liquidations.md` under "Atomic contract" section. This `.sc` extends with the implementation details above (single-approval, DEX-agnostic, viaIR requirement, profit check format).

---
domain: defi-liquidations
source_task: 2026-05-22-monad-v4-anvil-smoketest.md
date: 2026-05-22
keywords: ["uniswap-v4", "universal-router", "permit2", "v4-quoter", "v4-poolkey", "v4-actions"]
---

## Extracted Knowledge

### Uniswap V4 architecture quick reference (different from V3)
- **Singleton PoolManager** holds ALL pool state on one contract. No per-pool deploy.
- **PoolKey** identifies a pool: `(currency0, currency1, fee, tickSpacing, hooks)`. currency0 must be the lower address. hooks=0x0 for vanilla pools.
- **No factory.getPool()** — you don't ask the factory if a pool exists. You query the Quoter or StateView directly with the PoolKey; if there's liquidity, you get a number.
- **Quoter ABI** (verified working on Monad):
  ```solidity
  function quoteExactInputSingle(
    ((address,address,uint24,int24,address), bool, uint128, bytes)
  ) returns (uint256 amountOut, uint256 gasEstimate);
  ```
  The outer struct wraps `(PoolKey, zeroForOne, exactAmount, hookData)`. ABI signature has **double parens** to denote the nested struct.

### Verify V4 pool depth before assuming you can route through it
On a new chain, V4 pools are easy to deploy (anyone can create a poolKey by calling `PoolManager.initialize`). But many are empty placeholders. The Quoter is the source of truth:
```js
// Sizes to probe: 0.01, 1, 10, 100, 1000 of the input token
// Look for: rate stays flat → small slippage → big swaps revert (= max capacity)
for (const size of [10n**16n, 10n**18n, 10n**19n, 100n*10n**18n, 1000n*10n**18n]) {
  const r = await quoter.quoteExactInputSingle.staticCall([poolKey, zeroForOne, size, "0x"]);
  // r[0] = amountOut, r[1] = gasEstimate
}
```
The size at which revert happens ≈ pool's effective capacity. Useful for capping per-fire seize amounts.

### UniversalRouter token-pull is Permit2-based
Unlike V3 SwapRouter02 (uses standard `transferFrom` via ERC20.approve), UniversalRouter pulls tokens via Permit2. Two layers of approval needed:
1. `IERC20.approve(PERMIT2, max)` — one-time, idempotent. Sets liquidator → Permit2 allowance.
2. `IAllowanceTransfer(PERMIT2).approve(token, UR, amount, deadline)` — per call OR with infinite deadline. Sets Permit2 → UR allowance.

Permit2 canonical address (every chain): `0x000000000022D473030F116dDEE9F6B43aC78BA3`.

Implication for liquidator contracts: if `swapTarget == UniversalRouter`, the contract MUST do the Permit2 hop before calling `swapTarget.call(swapData)`. A naive `IERC20.approve(swapTarget, amount)` doesn't work.

### UR command encoding
```solidity
function execute(bytes commands, bytes[] inputs, uint256 deadline);
```
- `commands` = packed bytes, one per command. Common: `0x00 = V3_SWAP_EXACT_IN`, `0x10 = V4_SWAP`, `0x05 = TRANSFER`, `0x0a = PERMIT2_PERMIT`.
- `inputs[i]` = ABI-encoded params for that command. V4_SWAP's input is an ABI-encoded V4 Action sequence (e.g., `SETTLE` + `SWAP_EXACT_IN_SINGLE` + `TAKE_ALL`).

V4 Action encoding is intricate — easier to import `@uniswap/v4-sdk` than to hand-roll. Reference: examples in `universal-router/test` directory of the GH repo.

### When you can skip the full anvil execution smoke
For a route validation, **the Quoter alone is sufficient confidence**. The Quoter computes the same swap math the executor would. If the Quoter returns a non-zero number for your trade size, the route is viable. The remaining question (does UR correctly route the command?) is the same complexity as writing the production code — better to test once during implementation than twice (smoke + prod).

## Proposed Skill Content

Extend `defi-liquidations` with a V4 section:

- **V4 PoolKey identification**: `(currency0=lower addr, currency1=higher addr, fee, tickSpacing, hooks=0x0 vanilla)`. No factory.getPool lookup — use Quoter or StateView directly.
- **Quoter ABI gotcha**: double-paren signature `quoteExactInputSingle(((address,address,uint24,int24,address),bool,uint128,bytes))`. ABI-encoders often need explicit `(...)` wrapping for the nested struct.
- **Liquidator + UniversalRouter requires Permit2 allowance dance.** Direct `ERC20.approve(swapTarget, amount)` does NOT work. Add: `ERC20.approve(PERMIT2, max)` + `Permit2.approve(token, UR, amount, deadline)`.
- **Probe V4 depth via the Quoter at multiple sizes** to understand the pool's effective capacity — revert size ≈ pool depth limit.
- **Skip full UR execution smoke if Quoter passes.** Save the work for the implementation session.

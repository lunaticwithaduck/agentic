---
title: Scope UniversalRouter integration for Monad lane (unlocks V4 + V3 + V2)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Recommendation: **Implement.** ~1 focused day.

Verified all V4 primitives work on Monad: the wstETH/WETH 0.01% pool has real liquidity (V4 Quoter returns sensible quotes), and the addresses are stable.

## Verified V4 + UR primitives on Monad

| Contract | Address | Verified |
|----------|---------|----------|
| PoolManager (V4 core) | `0x188d586ddcf52439676ca21a244753fa19f9ea8e` | ✓ via Uniswap docs |
| **UniversalRouter** | `0x0d97dc33264bfc1c226207428a79b26757fb9dc3` | ✓ recommended entry |
| UniversalRouter 2.1.1 | `0xfdf682f51fe81aa4898f0ae2163d8a55c127fbc7` | newer fork |
| **V4 Quoter** | `0xa222dd357a9076d1091ed6aa2e16c9742dd26891` | ✓ live-tested, returns sensible quote |
| StateView | `0x77395f3b2e73ae90843717371294fa97cc419d64` | for off-chain reads |
| Permit2 | `0x000000000022D473030F116dDEE9F6B43aC78BA3` | canonical (same on every chain) |

Live test: V4 Quoter on `(wstETH, WETH, fee=100, tickSpacing=1, hooks=0x0)`:
- Input: 0.01 wstETH (10^16 wei)
- Output: 0.012346 WETH (1.23 LST premium — normal)
- Gas est: 68,824

## Architecture

### V4 Quoter ABI (confirmed working signature)
```solidity
function quoteExactInputSingle((PoolKey poolKey, bool zeroForOne, uint128 exactAmount, bytes hookData))
  returns (uint256 amountOut, uint256 gasEstimate);

struct PoolKey {
  address currency0;   // lower-address token
  address currency1;   // higher-address token
  uint24 fee;          // e.g. 100 = 0.01%
  int24 tickSpacing;   // canonical: 1 for fee=100
  address hooks;       // 0x0 for vanilla pools
}
```

### UniversalRouter entry point
```solidity
function execute(bytes commands, bytes[] inputs, uint256 deadline) external payable;
```
- `commands`: one byte per command — `0x10 = V4_SWAP`, `0x00 = V3_SWAP_EXACT_IN`, `0x05 = TRANSFER`, `0x0a = PERMIT2_PERMIT`
- `inputs[i]`: ABI-encoded params for command[i]

### Token approval model
UniversalRouter pulls tokens via **Permit2**, not direct ERC20 approval. Our liquidator's `IERC20.approve(swapTarget, amount)` doesn't work with UR as-is.

Two viable options:

**Option A — pre-approve Permit2 once, allowance-based (no signing)**
```solidity
// One-time in liquidator (or first liquidation)
IERC20(collateralAsset).approve(PERMIT2, type(uint256).max);

// Per liquidation
IAllowanceTransfer(PERMIT2).approve(collateralAsset, swapTarget /*UR*/, uint160(amount), uint48(deadline));
IUniversalRouter(swapTarget).execute(commands, inputs, deadline);
```
Cleanest, deterministic, no signatures. **Recommended.**

**Option B — TRANSFER command pre-deposits to UR**
Use UR's TRANSFER (0x05) command to move tokens into UR first, then swap with `recipient=msg.sender`. Single tx, but adds an extra command and may require direct allowance via UR's Payments lib.

Option A wins on clarity.

## Required contract change

`MorphoLiquidator.sol` line 122-129 currently:
```solidity
if (!p.receiveSToken && p.swapTarget != address(0)) {
  uint256 collBal = IERC20(p.collateralToken).balanceOf(address(this));
  if (collBal > 0) {
    IERC20(p.collateralToken).approve(p.swapTarget, collBal);
    (bool ok, bytes memory ret) = p.swapTarget.call(p.swapData);
    if (!ok) revert SwapFailed(ret);
  }
}
```

Needs a per-call Permit2 hop. Cleanest: add a `permit2Approve` flag in LiquidateParams, or just always go through Permit2 when swapTarget != V3 SwapRouter02. Simpler: hardcode the dual-path:

```solidity
// New constants
address constant PERMIT2 = 0x000000000022D473030F116dDEE9F6B43aC78BA3;
address constant UNIVERSAL_ROUTER = 0x0d97dc33264bfc1c226207428a79b26757fb9dc3;

// In receiveFlashLoan, replace the approve+call block:
if (!p.receiveSToken && p.swapTarget != address(0)) {
  uint256 collBal = IERC20(p.collateralToken).balanceOf(address(this));
  if (collBal > 0) {
    if (p.swapTarget == UNIVERSAL_ROUTER) {
      // Permit2 path
      IERC20(p.collateralToken).approve(PERMIT2, type(uint256).max);  // idempotent
      IAllowanceTransfer(PERMIT2).approve(
        p.collateralToken, UNIVERSAL_ROUTER, uint160(collBal), uint48(block.timestamp + 60)
      );
    } else {
      // Direct allowance (V3 SwapRouter02 path)
      IERC20(p.collateralToken).approve(p.swapTarget, collBal);
    }
    (bool ok, bytes memory ret) = p.swapTarget.call(p.swapData);
    if (!ok) revert SwapFailed(ret);
  }
}
```

**Requires a contract redeploy** (~$0.005 in MON gas). Old contract stays deployed but the new one supersedes in `cfg.LIQUIDATOR`.

## Swap-path code (off-chain)

Extend `lib/monad-dexes.js` to mark UR + V4 capabilities:
```js
const MONAD_DEXES = [
  {
    name: 'uniswap-v3-monad',
    factory: '0x204faca1764b154221e35c0d20abb3c525710498',
    router:  '0xfe31f71c1b106eac32f1a19239c9a9a72ddfb900',
    quoter:  '0x661e93cca42afacb172121ef892830ca3b70f08d',
    feeTiers: [100, 500, 3000, 10000],
    routerSig: 'v3-classic',
  },
  {
    name: 'uniswap-v4-monad',
    factory: null,                                          // V4 is singleton — no per-pool deploy
    router:  '0x0d97dc33264bfc1c226207428a79b26757fb9dc3',   // UniversalRouter
    quoter:  '0xa222dd357a9076d1091ed6aa2e16c9742dd26891',   // V4 Quoter
    poolManager: '0x188d586ddcf52439676ca21a244753fa19f9ea8e',
    feeTiers: [100, 500, 3000, 10000],
    routerSig: 'v4-universal',
  },
];
```

Extend `swap-path.js` `findBestPool`:
- For V3 entries: use existing factory.getPool + V3 Quoter
- For V4 entries: probe the V4 Quoter with each `(fee, tickSpacing)` combination directly (no factory lookup needed — V4 is poolKey-based)

For the chosen V4 pool, `buildSwapData` returns UR-encoded calldata:
```js
function buildSwapDataV4(poolKey, amountIn, amountOutMin, recipient, deadline) {
  const COMMAND_V4_SWAP = '0x10';
  const inputs = [encodeV4Actions(...)];  // ABI-encoded sequence of v4 Actions (SWAP_EXACT_IN_SINGLE + SETTLE + TAKE)
  return universalRouterInterface.encodeFunctionData('execute', [COMMAND_V4_SWAP, inputs, deadline]);
}
```

V4 Actions encoding is the trickiest part. Reference: `@uniswap/v4-sdk` or examples in the `permit2-relay` repo.

## Markets that become capturable
- ✅ wstETH/WETH whale cluster ($16.5M @ HF 1.002) — V4 wstETH/WETH pool $535k
- ✅ earnAUSD/USDC ($2.79M) — V4 AUSD/USDC $3.88M + TraderJoe AUSD/USDC $3M
- ✅ Anything with AUSD or WMON as loan token — V4 dominates
- ❌ wsrUSD/USD1, syzUSD/USDC, YZM/USDC — no DEX market for swap-back (filter out in monitor)

## Effort + risk
- V4 swap-path code (~300 lines): ~3-4 hours
- Contract Permit2 modification + redeploy: ~1 hour
- Smoke test against wstETH/WETH on anvil: ~1 hour
- **Total: 5-6 hours focused**
- Risk: medium — V4 action encoding is intricate; small errors revert silently

## Outcome
Scoped 2026-05-22. Recommend implementing. Next step: smoke-test the V4 quoter path directly (no contract change) to validate the encoding before committing to the contract redeploy.

## Completion
Run `/complete workflows/tasks/2026-05-22-scope-universal-router.md`.

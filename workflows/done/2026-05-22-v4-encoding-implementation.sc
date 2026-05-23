---
domain: defi-liquidations
source_task: 2026-05-22-v4-encoding-implementation.md
date: 2026-05-22
keywords: ["uniswap-v4", "universal-router", "v4-action-encoding", "permit2", "anvil-impersonation", "swap-exact-in-single"]
---

## Extracted Knowledge

### Complete working V4 swap encoding for UniversalRouter
```js
// V4 Action codes (from v4-periphery/libraries/Actions.sol)
const V4_ACTIONS = {
  SWAP_EXACT_IN_SINGLE: 0x06,
  SETTLE_ALL:            0x0c,
  TAKE_ALL:              0x0f,
};
// UR command code (from universal-router/libraries/Commands.sol)
const UR_COMMANDS = { V4_SWAP: 0x10 };

function encodeV4SwapExactInSingle(poolKey, zeroForOne, amountIn, amountOutMin) {
  const abi = ethers.AbiCoder.defaultAbiCoder();

  const actions = '0x' + [V4_ACTIONS.SWAP_EXACT_IN_SINGLE, V4_ACTIONS.SETTLE_ALL, V4_ACTIONS.TAKE_ALL]
    .map(c => c.toString(16).padStart(2, '0')).join('');

  const swapParams = abi.encode(
    ['tuple(tuple(address,address,uint24,int24,address) poolKey, bool zeroForOne, uint128 amountIn, uint128 amountOutMinimum, bytes hookData)'],
    [{ poolKey, zeroForOne, amountIn, amountOutMinimum: amountOutMin, hookData: '0x' }],
  );
  const inputCurrency  = zeroForOne ? poolKey.currency0 : poolKey.currency1;
  const outputCurrency = zeroForOne ? poolKey.currency1 : poolKey.currency0;
  const settleParams = abi.encode(['address','uint256'], [inputCurrency, amountIn]);
  const takeParams   = abi.encode(['address','uint256'], [outputCurrency, amountOutMin]);

  // V4_SWAP input = abi.encode(actions, params[])
  return abi.encode(['bytes','bytes[]'], [actions, [swapParams, settleParams, takeParams]]);
}

function buildUniversalRouterV4SwapCalldata({poolKey, zeroForOne, amountIn, amountOutMin, deadline}) {
  const urIfc = new ethers.Interface(['function execute(bytes,bytes[],uint256) payable']);
  const commands = '0x' + UR_COMMANDS.V4_SWAP.toString(16).padStart(2, '0');
  const v4Input = encodeV4SwapExactInSingle(poolKey, zeroForOne, amountIn, amountOutMin);
  return urIfc.encodeFunctionData('execute', [commands, [v4Input], deadline]);
}
```

Verified working: 1 wstETH → 1.234668 WETH at 165k gas on Monad anvil fork.

### Permit2 approval dance for UniversalRouter
UR pulls tokens via Permit2, NOT direct ERC20 approval. Two-layer approval:
```js
// Layer 1 (one-time, idempotent) — liquidator → Permit2
await erc20Token.approve(PERMIT2, ethers.MaxUint256);

// Layer 2 (per call) — Permit2 → UR
const permit2 = new ethers.Contract(PERMIT2, [
  'function approve(address token, address spender, uint160 amount, uint48 expiration)'
], signer);
await permit2.approve(token, UR_ADDRESS, amountIn, deadline);

// Layer 3 — actually swap
await ur.execute(commands, inputs, deadline);
```

Permit2 canonical address (all chains): `0x000000000022D473030F116dDEE9F6B43aC78BA3`.

**Critical types**: Permit2's `approve` takes `uint160 amount, uint48 expiration` — NOT uint256/uint256. Using wrong types gives confusing reverts.

### V4 Quoter ABI gotcha (nested struct double-parens)
```js
// The Quoter signature has the entire single-arg as a tuple wrapper:
const ifc = new ethers.Interface([
  'function quoteExactInputSingle(((address,address,uint24,int24,address),bool,uint128,bytes)) returns (uint256,uint256)'
]);
// Note the OUTER parens around the tuple — one set for the function param, one for the struct
const args = [[[c0, c1, fee, tickSpacing, hooks], zeroForOne, amountIn, '0x']];
const data = ifc.encodeFunctionData('quoteExactInputSingle', args);
```

V4 Quoter on Monad: `0xa222dd357a9076d1091ed6aa2e16c9742dd26891`.

### Funding an anvil test wallet with ERC20 via protocol impersonation
For testing swaps with a token the test wallet doesn't naturally hold, find a known protocol that holds the token (lending markets, DEX pools) and impersonate it:
```js
await provider.send('anvil_impersonateAccount', [HOLDER]);
await provider.send('anvil_setBalance', [HOLDER, '0x' + (10n**20n).toString(16)]);  // gas
const holderSigner = await provider.getSigner(HOLDER);
const token = new ethers.Contract(TOKEN, ['function transfer(address,uint256) returns (bool)'], holderSigner);
await (await token.transfer(TEST_WALLET, amount)).wait();
```

Way easier than reverse-engineering ERC20 storage layouts for `anvil_setStorageAt`. Good holders: lending protocols (Morpho, Aave), DEX pools (Uniswap PoolManager), wrappers.

**Watch out**: borrower wallets in lending markets usually DON'T hold tokens directly — their deposits are inside the protocol contract.

### V4 PoolKey rules
- `currency0` = lower address (compare lowercased hex)
- `currency1` = higher address
- `fee` is the fee tier in hundredths of bps (100 = 0.01%)
- `tickSpacing` is canonical for the fee: 100→1, 500→10, 3000→60, 10000→200
- `hooks` is `0x0000000000000000000000000000000000000000` for vanilla pools
- `zeroForOne` = true if you're swapping currency0 → currency1

Pass these consistently across Quoter calls and swap encoding or you'll get `PoolNotInitialized()` reverts.

## Proposed Skill Content

Extend `defi-liquidations` with a "V4 + UniversalRouter integration recipe":

- **Encoding template**: SWAP_EXACT_IN_SINGLE + SETTLE_ALL + TAKE_ALL action sequence, abi-encoded as (bytes actions, bytes[] params). Use the verified helper above as starting point.
- **Permit2 approval dance**: never use direct ERC20.approve(UR, amount). Always: ERC20.approve(Permit2, max) + Permit2.approve(token, UR, amount, deadline). Note Permit2's uint160/uint48 types.
- **V4 Quoter signature has double-paren** for the nested struct — `((address,address,uint24,int24,address),bool,uint128,bytes)`.
- **For anvil ERC20 funding**, impersonate a protocol that holds the token (Morpho, Aave, PoolManager) instead of hunting storage slots.
- **Borrower addresses in lending protocols don't hold tokens directly** — their stake is inside the protocol contract. Impersonate the protocol contract, not the borrower.
- **Gas per V4 swap**: ~165k on Monad. Cheap enough to ignore for profit-gate purposes.

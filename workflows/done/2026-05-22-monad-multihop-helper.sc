---
domain: defi-liquidations
source_task: 2026-05-22-monad-multihop-helper.md
date: 2026-05-22
keywords: [multihop-swap-helper, amount-slot-patching, universalrouter-nested-calldata, drpc-mempool-stuck, quicknode-null-hash, ethers-getcreateaddress, monad-deploy]
---

## Extracted Knowledge

### Generic 2-hop swap helper pattern with word-slot amountIn patching
For cross-DEX chained swaps from a single liquidator entry point:

```solidity
struct Hop { address tokenIn; address target; bytes data; uint256 amountSlot; }

function swap(uint256 amountIn, Hop[] calldata hops, address tokenOut, uint256 minOut) external {
    IERC20(hops[0].tokenIn).transferFrom(msg.sender, address(this), amountIn);
    for (uint i; i < hops.length; i++) {
        uint256 bal = IERC20(hops[i].tokenIn).balanceOf(address(this));
        IERC20(hops[i].tokenIn).approve(hops[i].target, 0);
        IERC20(hops[i].tokenIn).approve(hops[i].target, bal);
        bytes memory data = hops[i].data;
        uint256 slot = hops[i].amountSlot;
        assembly { mstore(add(add(data, 36), mul(slot, 32)), bal) }
        (bool ok, ) = hops[i].target.call(data);
        require(ok);
    }
    uint256 out = IERC20(tokenOut).balanceOf(address(this));
    require(out >= minOut);
    IERC20(tokenOut).transfer(msg.sender, out);
}
```

Word slot constants:
- Curve `exchange(int128 i, int128 j, uint256 dx, uint256 min_dy)`: dx at slot **2**
- Uni V3 `exactInputSingle((addr,addr,uint24,addr,uint256,uint256,uint256,uint160))`: amountIn at slot **5**

The `mstore` formula `add(add(data, 36), mul(slot, 32))`:
- `data + 32` skips the bytes length header
- `+ 4` skips the function selector
- `+ slot * 32` reaches the target word

### Uniswap V4 UniversalRouter calldata is NOT word-patchable
`UR.execute(bytes commands, bytes[] inputs, uint256 deadline)` — the amount lives inside
`inputs[0]` which itself contains V4_SWAP action calldata with PoolKey + params. The
ABI offsets to reach the inner amount field depend on dynamic-length array layout and
are NOT at a fixed word slot from the start of `execute`'s calldata.

Workarounds when you need V4 hops in multihop:
- Custom V4-aware patching in helper (decodes UR action format, repacks)
- OR pre-quote V4 hop and accept slippage risk on downstream hop
- OR add a `tokenIn → V4 → intermediary` "router-of-routers" that consumes own balance
  rather than caller-specified amount

### dRPC Monad endpoint quirks (confirmed 2026-05-22)
- Blocks ALL `eth_call` regardless of gas value (or absence)
- Blocks ALL `eth_estimateGas` for the same reason
- Mempool can hold "high priority" txs that refuse to be displaced even with 50+ gwei
  priority fee — error `-32603 "An existing transaction had higher priority"`
- BUT: `eth_sendRawTransaction`, `eth_blockNumber`, `eth_chainId`, `eth_getTransactionCount`,
  `eth_gasPrice`, `eth_getTransactionReceipt` all work fine

For Monad deployments: use QuickNode (`rpc.monad.xyz`) for reads + broadcasts, paced
under 25/sec. dRPC is fine for production fires (eth_sendRawTransaction) but useless
for sims and unreliable for deploys.

### QuickNode `eth_sendRawTransaction` sometimes returns `result: null`
The tx still broadcasts and mines. Don't trust the absence of a returned hash as a
failure signal. Verify successful deploy via:
```js
const addr = ethers.getCreateAddress({ from: owner, nonce });
const code = await provider.call('eth_getCode', [addr, 'latest']);
// code.length > 2 → contract deployed
```

### Per-chain wallet behavior on shared signer
Nonces are per-chain. Same MIBERA_PK can be used by Felix (HyperEVM) + Monad + Tydro
(Ink) executors in parallel without lock contention because each chain has independent
nonce sequences. But within a chain, the wallet-lock pattern (`/tmp/<chain>-wallet.lock`)
prevents sibling processes (monitor, executor, manual scripts) from racing on the
same nonce.

When manually deploying while the chain's executor is running: the executor may bump
the nonce mid-deploy. Always re-read pending nonce IMMEDIATELY before signing, and
explicitly pass `nonce: ...` to ethers' `factory.deploy()` rather than relying on its
internal auto-fetch (which can lag).

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Generic multi-hop swap helper for cross-DEX chained swaps
When a liquidator can only do ONE `swapTarget.call(swapData)` per fire but the
profitable route is cross-DEX (e.g., V3 + Curve), deploy a small helper:
- Generic `swap(amountIn, hops[], tokenOut, minOut)` interface
- Each hop has `tokenIn`, `target`, `data`, `amountSlot`
- Helper patches the current tokenIn balance into `data` at `amountSlot * 32` (word
  offset after 4-byte selector) via inline assembly `mstore`
- Works for any DEX whose swap function takes amountIn at a fixed ABI word slot
  (V3 SwapRouter02 exactInputSingle, Curve exchange) — NOT V4 UR.execute (nested calldata)

### dRPC Monad: write-path only
dRPC's paid Monad endpoint blocks eth_call AND eth_estimateGas regardless of gas.
Use it ONLY for `eth_sendRawTransaction` (production fires). For everything else (sims,
deploys, dryruns) use QuickNode public RPC with 300ms pacing.

### QuickNode null-hash on broadcast
QuickNode's `eth_sendRawTransaction` may return `result: null` despite the tx
broadcasting successfully. Verify by computing `getCreateAddress({ from, nonce })`
+ `eth_getCode` rather than trusting the missing hash response.

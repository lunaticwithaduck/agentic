---
domain: defi-liquidations
source_task: 2026-05-22-monad-partial-liq-strategy.md
date: 2026-05-22
keywords: ["partial-liquidation", "pool-depth", "uniswap-v4", "morpho-liquidate", "cascade-fire", "seize-amount", "out-of-gas"]
---

## Extracted Knowledge

### The `seizedAssets == swap.amountIn` invariant
For a Morpho liquidator with a pre-built swap calldata, the amount passed to `Morpho.liquidate(seizedAssets, ...)` MUST equal the `amountIn` encoded in the swap calldata. If they diverge:
- Morpho seizes LESS than swap expects → swap's `transferFrom` fails (insufficient balance) → tx reverts
- Morpho seizes MORE than swap expects → contract holds extra collateral (acceptable; gets swept to OWNER in the contract's leftover-collateral sweep)

Passing `MaxUint256` as `seizedAssets` (relying on Morpho to clamp) is wrong unless you can guarantee Morpho's clamp lands exactly on your swap's `amountIn`. Easier: pass the exact computed `expectedSeized` so Morpho seizes EXACTLY that much.

### Cap `expectedSeized` at borrower's collateral
For deeply-underwater positions, the formula `(borrowed × LIF) / oraclePrice` can exceed the borrower's actual collateral (Morpho would clamp internally, but your swap won't). Always:
```js
const rawExpectedSeized = (seizedValue * ORACLE_SCALE) / oraclePrice;
const expectedSeized = rawExpectedSeized > collateral ? collateral : rawExpectedSeized;
```
This bug only surfaces on deeply-underwater positions (HF << 1). Normal-HF fires (HF 1.001-1.05) won't hit it. Easy to miss without an oracle-crash smoke test.

### Partial-liquidation pattern via pool-depth fallback
When the requested seize size exceeds a single-swap pool capacity (V4 PoolManager OOG on huge swaps, V3 returns 0 quote), don't skip — iteratively halve until the quoter accepts:
```js
async function findBestPoolWithFallback(rpc, tokenIn, tokenOut, requestedAmount) {
  let amount = BigInt(requestedAmount);
  let pool = await findBestPool(rpc, tokenIn, tokenOut, amount);
  if (pool) return { pool, amount };
  for (let i = 0; i < 6; i++) {       // 64× reduction range
    amount = amount / 2n;
    if (amount === 0n) return null;
    pool = await findBestPool(rpc, tokenIn, tokenOut, amount);
    if (pool) return { pool, amount };
  }
  return null;
}
```

Use the returned `amount` for BOTH `Morpho.seizedAssets` AND `swap.amountIn`. Position re-arms naturally after each partial fire — chain drains the whale over several txs.

### Cascade-fire economics
For a `$X` whale where one partial fire seizes 1/k of collateral at LIF margin `m`:
- Per-fire profit ≈ `$X × (1/k) × m / (1 + m)` (in loan-token terms)
- Total profit across draining = `~$X × m / (1 + m)` (same as a single full fire would yield, assuming pool reloads)
- Trade-off: pays gas per fire instead of once; but unlocks whales that single-fire can't capture.

For verified Monad example (1196 wstETH whale, V4 0.01% pool, k=8 partial fires):
- Per-fire seize: ~143 wstETH = $50k profit
- Total potential: ~$300-400k drained over ~8 fires
- Gas cost: ~457k × 8 = ~3.7M gas total ≈ <$1 at MON $0.03

### V4 out-of-gas diagnosis via `debug_traceTransaction`
When a V4 swap reverts in mysterious ways, anvil's `debug_traceTransaction` with `callTracer` reveals the exact subcall that failed:
```bash
curl -X POST http://localhost:8546 \
  -d '{"method":"debug_traceTransaction","params":["0xHASH",{"tracer":"callTracer"}],...}'
```
The output is a nested tree of calls. Walk it looking for `"error": "out of gas"` or `"output": "0x08c379a0..."` (Error(string) selector — decode the string from offset 8+64+64).

For V4, OOG inside the PoolManager almost always means: the swap tried to consume more than the pool's active liquidity at the current tick range. Halve the amount and retry.

### Off-chain swap-calldata pre-build is brittle vs on-chain dynamic amounts
The architectural tension: building swap calldata off-chain requires knowing the EXACT `amountIn`, but the actual seized amount is dynamic (set by Morpho's internal logic). The solution we landed on:
1. Compute `expectedSeized` deterministically from the same inputs Morpho uses
2. Cap it at `collateral` AND at pool-safe amount
3. Pass that exact number as both Morpho's `seizedAssets` parameter and the swap's `amountIn`
4. The contract's leftover-token sweep handles any tiny rounding leftover

This is the cleanest fix without rewriting the liquidator contract to dynamically rebuild swap calldata mid-tx (which would require including the swap-path SDK on-chain — much heavier).

## Proposed Skill Content

Extend `defi-liquidations` with:

- **`Morpho.seizedAssets == swap.amountIn` is mandatory** when pre-building swap calldata off-chain. Pass the exact `expectedSeized` (NOT MaxUint256) so the seize matches the swap.
- **Always cap `expectedSeized` at the borrower's `collateral`.** Deeply-underwater HF<<1 positions silently break otherwise — only surfaces in oracle-crash testing.
- **Partial-liquidation via iterative-halving fallback** when pool depth < seize amount. Trade more gas for the ability to capture whales that single-fire can't. Logic is mechanical: `for i in 0..6, halve, retry`.
- **`debug_traceTransaction` with callTracer** is the right tool for V4 OOG diagnosis. The nested error tree shows exactly which PoolManager subcall ran out of gas, and from there it's almost always "shrink the swap size".
- **Run an oracle-crash production smoketest with paid RPC** before flipping any new Morpho-on-V4 chain live. Public RPCs throttle the cascading fork state-fetches and you'll never get past the discovery step. This caught two production bugs we'd have eaten gas on.

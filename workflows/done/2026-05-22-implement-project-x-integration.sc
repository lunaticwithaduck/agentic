---
domain: defi-liquidations
source_task: 2026-05-22-implement-project-x-integration.md
date: 2026-05-22
keywords: ["dex-discovery", "router-agnostic", "uniswap-v3", "multi-dex", "contract-enumeration", "etherscan-v2", "quoter", "factory"]
---

## Extracted Knowledge

### Finding an undocumented DEX's Router + Quoter on-chain
When a DEX has no public docs/SDK but you can identify ONE contract (e.g., the factory from CoinGecko), you can reliably find the rest:

1. **Find the factory's deployer** via Etherscan V2 (`module=contract&action=getcontractcreation`). Even sketchy chains usually have an etherscan-style explorer that supports this.

2. **Enumerate the deployer's contract creations** via `module=account&action=txlist` — V3 deployments deploy multiple peripheral contracts in sequence (Factory → SwapRouter → NFPM → NFTDescriptor → QuoterV2). They cluster within a small block range.

3. **Verify each candidate's `factory()` pointer** matches the live factory. Multiple V3 deployments by the same deployer are common (test deploy → real deploy); the right one points to your target factory.

4. **Resolve contract names** via `module=contract&action=getsourcecode` — verified contracts return "SwapRouter", "NonfungiblePositionManager", "QuoterV2". Unverified ones need selector probing.

5. **Selector probe for the Quoter**: try `quoteExactInputSingle((address,address,uint256,uint24,uint160))` selector against unverified candidates with a static call. The one that returns sensible output (~$59 for 0.001 WHYPE → USDT0 = ~59000 wei with 6-dec output) is the Quoter.

### Router-agnostic liquidator design
If you control the liquidator contract, accept `swapTarget` + `swapData` as **per-call parameters** rather than hardcoding a router:
```solidity
function liquidate(..., address swapTarget, bytes calldata swapData) external {
  ...
  if (collBal > 0 && swapTarget != address(0)) {
    IERC20(collateral).approve(swapTarget, collBal);
    (bool ok,) = swapTarget.call(swapData);
  }
}
```
This lets the off-chain executor swap routers freely (project-x ↔ hyperswap ↔ aggregator) without redeploying. The `approve` is per-call, so allowance is fresh per tx — switching routers has zero state cost.

### Multi-DEX best-quote pattern for V3 forks
For chains with multiple Uni V3 forks (HyperEVM has 5+, Sonic has 6+):
```js
async function findBestPool(rpc, tokenIn, tokenOut, amountIn) {
  // Enumerate every DEX × every fee tier
  const candidates = (await Promise.all(DEXES.flatMap(d => d.feeTiers.map(async fee => {
    const pool = await factory.getPool(tokenIn, tokenOut, fee);
    return pool !== ZeroAddress ? { dex: d.name, router: d.router, quoter: d.quoter, fee, pool } : null;
  })))).filter(Boolean);

  // Quote each at the REAL amountIn (not a hardcoded probe — see swap-path.sc lesson)
  const quoted = await Promise.all(candidates.map(async c => ({
    ...c,
    expectedOut: c.quoter
      ? await quoter.quoteExactInputSingle({...})  // precise
      : await spotQuoteFromSlot0(c.pool, ...)       // fallback for DEXes without quoters
  })));

  // Filter reverts/zeros, pick max
  return quoted.filter(q => q.expectedOut > 0n).sort(byExpectedOut)[0];
}
```

### Quoter availability varies
Project-X has QuoterV2 (precise quote with price impact). HyperSwap V3 doesn't expose a public quoter — fall back to `slot0` spot-price math (`(sqrtPriceX96^2) / 2^96` for token1/token0 ratio). The fallback ignores price impact but is good enough for the executor's profit-gate to do a sanity check.

### V3 fork compatibility
Project-X uses canonical Uni V3 (fees 100/500/3000/10000 → tickSpacing 1/10/60/200). Compare to Shadow (Sonic) which uses tickSpacing AS the fee parameter (Ramses V3 fork). The router ABI is different between these:
- Canonical V3: `exactInputSingle((tokenIn, tokenOut, uint24 fee, recipient, deadline, amountIn, amountOutMinimum, sqrtPriceLimitX96))`
- Ramses V3: `exactInputSingle((tokenIn, tokenOut, int24 tickSpacing, recipient, deadline, amountIn, amountOutMinimum, sqrtPriceLimitX96))`

Always confirm before assuming ABI compatibility — `factory()` pointer being right doesn't mean the router signature is.

### When DEX SDK / docs are unavailable
Standard fallback sequence:
1. CoinGecko `/onchain/networks/{slug}/dexes` for inventory + top pool TVLs
2. Etherscan V2 multichain API (`chainid=999` for HyperEVM, `146` for Sonic, `80094` for Berachain etc.) for verified contracts
3. On-chain `factory()` / selector tests for unverified peripherals
4. WebFetch the project's main site only as a last resort (often JS-rendered SPAs that don't return contract info via plain HTTP)

## Proposed Skill Content

Extend `defi-liquidations` with a "DEX integration playbook" section:

- **Always verify ABI compatibility before assuming a V3 fork is canonical** — Ramses V3 (Sonic Shadow), Algebra V3 (SwapX), and Uniswap V4 hooks all differ at the router signature level, not just the factory.
- **Liquidator contracts should accept `swapTarget` as a per-call parameter** so adding a new DEX is a code-only off-chain change. Don't hardcode router addresses on-chain.
- **Multi-DEX best-quote pool selection** — enumerate every (DEX × fee tier) for the pair, quote each at the real `amountIn`, return the best with its router address. Filter `expectedOut === 0n` reverts before sorting.
- **DEX discovery without docs**: deployer enumeration via Etherscan V2 → verify factory pointer → selector-probe unverified candidates. Documented working example: discovered Project-X's full V3 deployment in ~20 minutes with zero docs.
- **CoinGecko on-chain endpoints** (`/onchain/networks/{slug}/...`) are the best free way to discover which DEXes exist on a chain and their depth ranking. Use it before assuming you know the routing landscape.

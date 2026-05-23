---
domain: defi-liquidations
source_task: 2026-05-22-monad-local-smoketest.md
date: 2026-05-22
keywords: ["anvil-fork", "smoketest", "pool-liquidity", "v3-empty-pool", "anvil-setcode", "oracle-override"]
---

## Extracted Knowledge

### `getPool() != 0x0` does NOT mean the pool has liquidity
On a new V3 deployment, factories will return non-zero addresses for pools that have been *created* (someone deployed the proxy) but have *no LP positions* yet. The `factory.getPool(tokenA, tokenB, fee)` check is necessary but insufficient.

The truth is in:
1. **Quoter**: if `quoteExactInputSingle(...)` reverts with "Unexpected error" for any non-zero amount, the pool is empty.
2. **Pool state**: `pool.liquidity()` returns 0 AND `token0.balanceOf(pool)` returns 0.
3. **Routing test**: a tiny probe (10^14 wei for 18-dec) should quote to something non-zero for any real pool. If even that reverts, the pool is dead.

**Smoketest finding on Monad 2026-05-22**: Uniswap V3 has wstETH/WETH pools at fees 100/500/3000 — all three are EMPTY despite the factory acknowledging them. So liquidating the $16.5M wstETH/WETH whale cluster requires Curve or another DEX, not Uni V3.

### Anvil-fork smoketest hangs if discovery routes through the fork
A common pitfall: smoke-test code calls `findBestPool(FORK_RPC, ...)`. The fork transparently fetches state from the upstream public RPC for each `eth_call`. Multi-step discovery (factory.getPool × N fee tiers + quoter calls × N pools) cascades into a flurry of upstream calls that all get rate-limited.

**Fix**: route read-only discovery through MAINNET RPC, not the fork. The pools are the same; we just write to the fork. Keeps smoketest fast even on rate-limited chains.

```js
// ❌ slow — discovery hits forked RPC which proxies every call to public upstream
const pool = await swapPath.findBestPool(FORK_RPC, coll, loan, probe);

// ✅ fast — discovery hits mainnet directly, fork only used for write/simulate
const pool = await swapPath.findBestPool(cfg.HTTP_RPC, coll, loan, probe);
```

### anvil_setCode for oracle manipulation
Override an oracle's bytecode with a constant-returning stub to crash a price into liquidatable territory:

```js
const crashedPrice = (rawPrice * 8500n) / 10000n;  // 15% drop
const priceHex = crashedPrice.toString(16).padStart(64, '0');
// PUSH32 <value> PUSH1 0 MSTORE PUSH1 0x20 PUSH1 0 RETURN
const bytecode = '0x7f' + priceHex + '60005260206000f3';
await provider.send('anvil_setCode', [oracleAddress, bytecode]);
```

Same pattern works for any view function that returns a uint256. Useful for forcing a fire-path test against currently-healthy mainnet positions.

### Smoke test should reveal structural gaps, not just code bugs
A good smoketest doesn't just verify "the code runs" — it verifies "the lane can capture the inventory it was built for". The Monad smoke test we ran today:
- ✅ Confirmed code paths work
- ✅ Confirmed contract deployed correctly
- 🚨 **Revealed we can't atomically fire any current at-risk position** because Uni V3 has no liquidity in those pairs

That third bullet was the entire point. Don't flip live based on "smoketest passes" — flip live based on "smoketest proves the lane captures the inventory you audited".

## Proposed Skill Content

Extend `defi-liquidations` with a "Pre-live verification" section:

- **Pool existence ≠ pool liquidity.** Always check quoter/pool state in addition to factory.getPool. Empty pools are common on new V3 deployments.
- **Route smoketest discovery through mainnet, not the fork.** Forked calls cascade into upstream rate limits and hang.
- **`anvil_setCode` with a 32-byte constant return** is the canonical oracle-override trick — works for any view-returning-uint256 oracle, no slot-hunting needed.
- **A smoketest that "passes" on healthy mode is not green-light.** Verify the swap-back actually works for the currently at-risk inventory before flipping live.

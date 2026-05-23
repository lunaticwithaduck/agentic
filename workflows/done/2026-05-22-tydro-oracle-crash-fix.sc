---
domain: defi-liquidations
source_task: 2026-05-22-tydro-oracle-crash-fix.md
date: 2026-05-22
keywords: [slipstream, velodrome, ramses, exactInputSingle, tickSpacing, aave-oracle, setAssetSources, anvil, impersonate, callTracer]
---

## Extracted Knowledge

### Slipstream / Ramses V3 fork SwapRouter uses `int24 tickSpacing`, not `uint24 fee`
- Canonical Uni V3 SwapRouter02 `exactInputSingle` selector: `0x414bf389`
  `exactInputSingle((address,address,uint24,address,uint256,uint256,uint256,uint160))`
- Slipstream / Velodrome / Ramses V3 SwapRouter selector: `0xa026383e`
  `exactInputSingle((address,address,int24,address,uint256,uint256,uint256,uint160))`
- Wire bytes are identical for small positive values (1, 50, 100, 200, 2000 etc),
  but **function selectors differ** since keccak256 of the signature string differs.
- Symptom: bare revert with no return data from the SwapRouter call.
- Affects: Velodrome Slipstream (Ink), Ramses (Arbitrum/Avalanche), Aerodrome
  Slipstream (Base) and any Ramses-fork V3. The factory also uses tickSpacing
  but `getPool(tokenA, tokenB, tickSpacing)` selector matches uint24's, since
  the parameter naming changed in the router only.
- Fix: change the router interface fragment to use `int24 tickSpacing` so ethers
  computes the right selector when encoding `exactInputSingle`.

### Oracle-crash strategy for Aave V3 forks with adapter oracles
- Many Aave V3 forks (Tydro, etc) use Chaos Labs or other adapter oracles where
  `getSourceOfAsset()` returns a feed ID, not a Chainlink aggregator address.
- Don't try to reverse the adapter. Instead, exploit Aave V3's standard
  `AaveOracle.setAssetSources(address[], address[])` admin function.
- Strategy:
  1. `anvil_impersonateAccount(papOwner)` where papOwner is the PoolAddressesProvider's
     ACL admin (find via `PoolAddressesProvider.getACLAdmin()` or trace setAssetSources
     calls in mainnet history)
  2. Deploy stub aggregator that returns a constant price for ANY selector:
     ```
     bytecode = 0x7f<32-byte-price>60005260206000f3
     PUSH32 price | PUSH1 0 | MSTORE | PUSH1 0x20 | PUSH1 0 | RETURN
     ```
     (41 bytes total, works for `latestAnswer()`, `latestRoundData()`, etc.)
  3. `anvil_setCode(stubAddr, bytecode)`
  4. As impersonated admin: `oracle.setAssetSources([target], [stubAddr])`
  5. Verify `getAssetPrice(target)` returns crashed price
  6. Verify `Pool.getUserAccountData(borrower).healthFactor < 1`

### Right-sizing debt chunk in oracle-crash smoke tests
- After oracle crash, seize amounts can be huge (massive token amounts at deflated
  oracle prices). Slipstream/V3 pools won't have depth for full seize.
- Cover a small `debtChunk` (e.g. 1000 USDC) instead of MaxUint256.
- Compute expected seize before broadcasting:
  ```
  expectedSeize = debtChunk * LIF_BPS / 10000 * debtOraclePrice / collOraclePrice
  ```
  Both denominated in 1e8 base, decimals must match.
- Use 95% of expectedSeize as swap `amountIn` for safety margin against rounding.

### callTracer + custom-error selector decoding diagnoses deep Aave V3 reverts
- `debug_traceTransaction(hash, { tracer: 'callTracer' })` gives nested call tree.
- For each call with output, check:
  - `0x08c379a0` prefix → `Error(string)` (Aave V3 legacy error codes like "35")
  - `0x4e487b71` prefix → `Panic(uint256)`
  - 4-byte selector (anything else) → custom error
- Custom error 4-byte selectors must be matched against the contract's ABI:
  Aave V3 newer errors, our liquidator's `NoProfit(uint256,uint256)` `SwapFailed(bytes)` etc.
- Print selector at every depth — the OUTERMOST reverting call propagates the
  selector, but the ACTUAL revert origin is the deepest call with non-zero output.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Slipstream/Ramses V3 fork integration gotcha
When integrating Slipstream (Velodrome Ink, Aerodrome Base), Ramses V3 (Arbitrum),
or any V3 fork derived from Ramses:
- SwapRouter `exactInputSingle` uses `int24 tickSpacing` not `uint24 fee`
- Selector 0xa026383e, not 0x414bf389
- Wire-compatible for small positive tickSpacings, but selector differs
- Symptom of using wrong interface: bare revert with no data from SwapRouter
- Diagnose: trace and inspect inner call's revert selector; if your liquidator's
  `SwapFailed(bytes)` (e.g. 0xff9fa595) propagates up, the inner call is the issue.

### Oracle-crash smoke test recipe
For Aave V3 forks (HyperLend, HypurrFi, Tydro) on anvil:
1. Locate AaveOracle admin via PoolAddressesProvider.getACLAdmin
2. Impersonate admin via anvil_impersonateAccount
3. Deploy 41-byte stub via anvil_setCode (returns constant price for any selector)
4. Call setAssetSources to swap collateral asset's source
5. Right-size debtChunk based on pool depth, not MaxUint256

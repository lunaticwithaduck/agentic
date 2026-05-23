---
name: hyperevm
description: HyperEVM (Hyperliquid L1 EVM, chainId 999) — RPC tier gotchas, Aave V3 fork patterns, dual-block gas model, liquidation race tuning, and address checksum strictness.
activation:
  keywords: ["hyperevm", "hyperliquid", "hyperlend", "hypurrfi", "hyperswap", "alchemy", "eth_getLogs", "eth_call", "rpc.hyperliquid.xyz", "stakely.io", "purroofgroup", "big block", "small block", "liquidation", "race-bot"]
---

## Purpose
HyperEVM looks like a standard EVM but has several quirks that bite hard at runtime: free-tier RPCs silently reject common calls, the public RPC has bursty rate limits, the chain has a dual-block model with a 60s gap between modes, and Aave V3 forks (HyperLend, HypurrFi) customize parameters in non-obvious ways. This skill captures the gotchas that produced 30+ minute silent failures during the 4-chain liquidator bring-up.

## RPC tiering — pick the right endpoint for the call

| Use case | Endpoint | Why |
|---|---|---|
| Latest-tag `eth_call`, multicall, `getUserAccountData` | Alchemy (`hyperliquid-mainnet.g.alchemy.com/v2/<KEY>`) | Fast + reliable on free tier |
| WSS new-block subscription | Alchemy WSS | Free tier supports it |
| `eth_getLogs` (any block range > 10) | Public/stakely/purroofgroup rotation | **Alchemy free tier caps `eth_getLogs` at 10 blocks** with `-32600` error — silently breaks indexers |
| Historical block-tag `eth_call` | Public/stakely/purroofgroup rotation | **Alchemy free tier rejects with `-32001 "Unable to complete request at this time"`** — silently breaks backtesters |
| Sending raw transactions | Alchemy or any | Either works |

**Working alternate RPCs** (no API key, free):
- `https://rpc.hyperliquid.xyz/evm` — official; bursty rate-limit cooldowns when hammered
- `https://hyperliquid-json-rpc.stakely.io` — stable
- `https://rpc.purroofgroup.com` — stable

**Broken RPCs**:
- `https://hyperliquid.drpc.org` — `eth_blockNumber` not implemented
- `https://1rpc.io/hyperliquid` — free tier exhausts within minutes
- `https://rpc.ankr.com/hyperliquid_evm` — requires API key

**Rotation pattern** for indexers and backtesters:
```js
const LOGS_RPCS = [
  'https://rpc.hyperliquid.xyz/evm',
  'https://hyperliquid-json-rpc.stakely.io',
  'https://rpc.purroofgroup.com',
];
// MAX_PARALLEL = 3 (one per provider). Throughput ≈ 1 chunk/sec.
// Each call rotates URL on retry to evade per-provider rate-limit cooldown.
```

## eth_getLogs hard limits
- HyperEVM caps `eth_getLogs` at **1000 blocks per call** even on the public RPC. Larger windows fail silently.
- Bursty rate limit on public RPC: 6 parallel triggers a ~30s IP-cooldown. Settle on `MAX_PARALLEL=3` across 3 endpoints.
- Backoff: `1000ms + 600ms × attempts + random(0..300ms)` to avoid synchronized retries.

## Dual-block gas model

| Block type | Gas cap | Interval | Use for |
|---|---|---|---|
| **Small** (default) | 2M gas | ~1s | Race transactions: liquidations, MEV, arb |
| **Big** (opt-in) | 30M gas | **60s** | Contract deployment, low-priority background |

Big-block opt-in is via L1-action precompile at `0x3333333333333333333333333333333333333333` and is **persistent per EOA** — once enabled, ALL future txs from that EOA go through 60s big-block flushes. **Never enable big-block for a race wallet** — you'll lose every fire to faster competitors.

Practical liquidator gas budget: **1.9M** (15% safety under 2M cap). If `provider.estimateGas()` returns > 1.9M, skip the fire instead of broadcasting a guaranteed-revert tx.

## Aave V3 fork patterns (HyperLend, HypurrFi, etc.)

**Standard interfaces work**:
- `Pool.getReservesList() → address[]` — dynamic reserve discovery (use this instead of hardcoding, especially on forks with many reserves like HypurrFi's 19)
- `Pool.getReserveData(asset) → (..., aTokenAddress, ..., variableDebtTokenAddress, ...)` — token addresses
- `Pool.getUserAccountData(user) → (totalCollBase, totalDebtBase, availableBorrows, currentLiqThreshold, ltv, healthFactor)` — health summary; base unit is **1e8 USD**

**AaveOracle** (each fork has its own address; discover via `Pool.ADDRESSES_PROVIDER().getPriceOracle()`):
- `getAssetPrice(asset) → uint256` in 8-decimal USD
- HyperLend AaveOracle: `0xC9Fb4fbE842d57EAc1dF3e641a281827493A630e`
- HypurrFi AaveOracle: `0x9BE2ac1ff80950DCeb816842834930887249d9A8`

**Liquidation flow** (with HyperSwap V3):
1. flashLoan debt asset
2. `liquidationCall(coll, debt, user, MaxUint256, false)` — Pool clamps to min(funding, balance, closeFactorLimit)
3. Approve HyperSwap router, call `exactInputSingle` to convert seized collateral → debt asset
4. Repay flash loan + premium (0.05%)
5. Transfer leftover dust to OWNER

**Close-factor (Aave V3 default)**:
- HF < 0.95e18 → can liquidate 100% of debt
- HF ≥ 0.95e18 → capped at 50%

**LIF (LIQUIDATION_BONUS) is per-asset, not chain-wide**:
- Aave V3 default is 5% but forks customize. HyperLend observed up to **14%** on UETH liquidations.
- For estimate-and-cap swap-path math: under-estimate (use 5%) is safe (leftover dust to OWNER). Over-estimate would cause swap-insufficient-input revert. **Never inflate the LIF assumption.**

**HF error encodings differ between forks**:
- HypurrFi: `revert: 45` (string `"45"` = `HEALTH_FACTOR_NOT_BELOW_THRESHOLD`)
- HyperLend: custom selector `0x930bb771` (same semantic, different encoding)
- Both are expected at pre-flight when user has HF ≥ 1.0

## EIP-55 address checksum strictness in ethers v6
- ethers v6 **strict-rejects** mixed-case addresses with wrong EIP-55 checksum at the encode boundary, not at construction.
- `ethers.getAddress("0xCAfE...")` (mixed-case wrong checksum) throws.
- `ethers.getAddress("0xcafe...")` (all-lowercase) **succeeds** and returns the correct EIP-55 form.
- **Defensive load-time normalization**: every address in config should be passed through `ethers.getAddress(addr.toLowerCase())`. Real-world incident: HypurrFi POOL `0xcecce0EB...` (wrong checksum) crashed reconcile *after* the 9-min getLogs phase completed — extremely late failure.

## Liquidator race hardening checklist

Apply all of these to any HyperEVM liquidator executor:

1. **Fresh-HF re-verify** at top of `presignLiquidation`. Skip silently if `healthFactor >= 1e18` — eliminates Telegram revert spam from healed-back-up positions.
2. **`provider.estimateGas` before signing**. Wrap in try/catch — catch is a free pre-pre-flight bailout. If estimate > 1.9M, skip rather than broadcast a guaranteed-revert tx.
3. **USD-value collateral pick**, not raw balance. `value = balance × oraclePrice / 10^decimals / 10^8` then sort descending. Same logic for debt. Filter unswappable assets (Pendle PTs, beHYPE) before sorting.
4. **Hourly balance watcher**. Piggyback on monitor's poll loop. `eth_getBalance(signer)` every ~1800 polls. Telegram alert at < 0.005 HYPE threshold with 6h dedup. Prevents silent out-of-funds from killing the bot.
5. **Shared-wallet nonce mutex** when multiple executors share one EOA (HyperLend + HypurrFi). File-based lock via `fs.mkdirSync(/tmp/hyperevm-wallet.lock)` (atomic on POSIX). Wrap presign + broadcast in `withLock`; move `waitForTransaction` outside to free the sibling. Stale-lock detection via mtime > 30s. Without this: concurrent `getTransactionCount('pending')` from both processes can return the same nonce → second broadcast silently fails with "nonce too low".
6. **Revert decoder with expected/actionable classification**. HyperLend uses Solidity custom errors (4-byte selectors like `0x930bb771 = HealthFactorNotBelowThreshold()`); HypurrFi uses string codes (`revert "45"`). Build a selector table mapping each to `expected: true` (silent skip) or `false` (Telegram alert). Extract revert data via `err.data ?? err.error?.data ?? err.info?.error?.data` to handle ethers v6 wrapping variants.
7. **Per-asset LIF lookup, not hardcoded 5%**. Aave V3 `Pool.getConfiguration(asset)` returns a uint256 bitmap; LIQUIDATION_BONUS is **bits 32-47** (basis points, e.g. 11500 = 15% bonus = 1.15 LIF). HyperLend ranges 10800-12000; HypurrFi 10800-11200. Bonus=0 means asset is debt-only — guard against picking it as collateral.
8. **Pool depth check at presign**. Cheap proxy: `tokenIn.balanceOf(poolAddress)`. If `swapAmountIn / poolReserve > 5%`, skip — slippage will likely cause `NoProfit` revert. Better to skip cleanly than waste gas on a guaranteed failure.
9. **Same-asset coll==debt handling**. When user supplies and borrows the same asset, Aave's `liquidationCall` keeps proceeds in that asset — no swap needed. Set `swapTarget = address(0)` and `swapData = '0x'`. Profit = `funding × (LIF - 1) - flashLoanPremium`. Building a self-swap (A→A) would just revert.
10. **Multi-pair Cartesian fallback**. If the biggest-USD (coll, debt) pair has no pool / too thin / LIF=0, try the next combination. Sort pairs by `combinedUSD` descending; first viable wins. Cost: ~300-500ms presign latency for ≤4 pairs. Reward: don't forfeit otherwise-winnable fires when the top pair fails.

## Revert decoder table

Pre-computed selectors for common Aave V3 / HyperLend / HypurrFi reverts plus our custom liquidator errors:

| Selector | Signature | Treat as |
|---|---|---|
| `0x930bb771` | `HealthFactorNotBelowThreshold()` | ✅ silent skip |
| `0x6679996d` | `HealthFactorLowerThanLiquidationThreshold()` | ✅ silent skip |
| `0x6d305815` | `ReserveFrozen()` | ⚠️ alert |
| `0xd37f5f1c` | `ReservePaused()` | ⚠️ alert |
| `0x823d7200` | `AssetPaused()` | ⚠️ alert |
| `0x40753f33` | `ReserveNotActive()` | ⚠️ alert |
| `0x911ceb81` | `CollateralCannotCoverNewBorrow()` | ⚠️ alert |
| `0xa5897d94` | `NotEnoughCollateralToLiquidate()` | ⚠️ alert |
| `0x31708d59` | `NoProfit(uint256,uint256)` (our contract) | ⚠️ alert |
| `0xff9fa595` | `SwapFailed(bytes)` (our contract) | ⚠️ alert |
| String "45" | Aave V3 string-revert code | ✅ silent skip |
| String "46" | `CollateralCannotCoverNewBorrow` (string) | ⚠️ alert |

## WSS-driven monitor architecture (sub-second detection)

For racing against WSS-pinned competitors, HTTP polling at 2s is too slow. Upgrade pattern:

**Subscription**: `eth_subscribe("newHeads")` via Alchemy WSS. Free tier supports it. Use the `ws` package (not ethers' WebSocketProvider — simpler reconnect).

**Watch list**: don't multicall ALL borrowers every block. Maintain a filtered list of borrowers with `HF < threshold` (HyperLend: 1.20 → ~3 users; HypurrFi: 1.35 → 1 user). Rebuild every 60s via HTTP full sweep.

**Per-block evaluation**: fire-and-forget `evalBlock(blockNum).catch(...)` — never `await` inside the message handler. At ~1s block times, multicall takes 300–500ms; awaiting would queue and fall behind.

**Reconnect**: WSS drops silently. Pattern: on `close`, `setTimeout(connect, delay)`, exponential backoff to 30s cap. Separately track `lastBlockAt`; if no block in 10s, force `ws.close()` to trigger reconnect.

**Run alongside HTTP monitor**: both write to the same `data/armed/<key>.json`; first-to-detect wins. WSS is primary, HTTP is resilience fallback. No coordination needed — file write is idempotent.

**Per-chain threshold tuning**: pick the threshold based on observed HF density. Too tight (HF<1.10) → empty watch list when nobody's close to fire. Too wide → unnecessary RPC load. HyperLend at 1.20 caught 3 users; HypurrFi at 1.35 caught 1 (the only meaningful position).

**Subscription response shape**:
```json
{"jsonrpc":"2.0","method":"eth_subscription",
 "params":{"subscription":"0x…","result":{"number":"0x…","hash":"…",…}}}
```

## Backtest pattern for any Aave V3 fork
1. Scan `LiquidationCall(address,address,address,uint256,uint256,address,bool)` event over N blocks (topic `0xe413a321e8681d831f4dbccbca790d2952b56f977908e45be37335533e005286`) via chunked getLogs.
2. For each event: query `Pool.getUserAccountData(user)` at `block - 1` via historical RPC.
3. **Catchable** by HTTP polling iff `healthFactor < 1e18` in prior block.
4. **`healthFactor == MaxUint256 (1.157e+59 when divided by 1e18)`** = user had 0 debt in prior block → atomic same-block oracle-bundle borrow. Uncatchable, classify as ⚡.
5. Profit estimate: `(collateralAmount × collPrice / 10^collDec - debtToCover × debtPrice / 10^debtDec) / 1e8` using AaveOracle prices at the same block.
6. Aggregate by liquidator address → market share + concentration risk.

Observed HyperLend market (2026-05-20): ~8 liquidations/day, 10+ liquidator wallets, top bot 29% share (WSS-pinned), 65% events structurally catchable by HTTP polling, 35% oracle-bundled.

## Failure Modes Observed
- **Alchemy free tier silent failures**: indexer ran 37 min hitting `eth_getLogs` 10-block cap with retry-loop returning `[]` per chunk → would have written empty positions.json. Fix: route log scans through public RPC rotation.
- **EIP-55 late crash**: HypurrFi POOL address with wrong checksum passed all early validation; crashed only after 9-min scan completed and reconcile started encoding multicall. Fix: `ethers.getAddress(addr.toLowerCase())` at config load.
- **ARM_THRESHOLD bug from copy-paste**: `102n * 10n ** 17n` (10.2e18 ≈ infinity) instead of `102n * 10n ** 16n` (1.02e18) → every healthy position armed and spam-fired. Fix: explicit comments + reproducible test against a HF=3+ healthy user.

---
domain: hyperevm
source_task: 2026-05-20-harden-hyperevm-executors.md
date: 2026-05-20
keywords: ["hyperevm", "aave", "liquidator", "race-bot", "executor", "gas-estimate", "small-block", "big-block"]
---

## Extracted Knowledge

### HyperEVM dual-block model: choose carefully
- **Small blocks**: 2M gas cap, ~1s interval. Default for `eth_sendRawTransaction`. Right for race-tier liquidations.
- **Big blocks**: 30M gas cap, **60s interval**. Opt-in via L1-action precompile at `0x3333333333333333333333333333333333333333`. **Fatal for racing** — competitors win in 1s while your tx sits in mempool waiting for the big-block flush.
- Practical max gasLimit for a liquidator: **1.9M** (15% safety margin under 2M cap)
- If `provider.estimateGas()` returns > 1.9M: SKIP the fire instead of broadcasting. The 5 minutes of edge cases lost is worth not wasting gas on guaranteed failures.

### Fresh-HF re-verify before broadcast
- Monitor writes armed payload at HF<1.02; executor reads it 1-5s later. HF often heals back above 1.0 in that window.
- Without fresh check: every healed-back-up arm causes a pre-flight revert and a 🛑 Telegram alert → spam.
- Pattern: at top of `presignLiquidation`, call `Pool.getUserAccountData(user)` with `'latest'` tag. If `healthFactor >= 1e18`, return null silently (no Telegram, no broadcast attempt).
- Side benefit: detects users who already got liquidated by a competitor between arm and fire.

### USD-value collateral selection, not raw balance
- Aave V3 users typically hold multiple collateral types. Sorting by raw balance picks the asset with most *tokens*, not most *value*.
- Buggy: user with 1000 USOL ($0.01 each) + 1 UBTC ($100k) → sort by balance gives USOL ($10) over UBTC ($100k).
- Correct: `value_USD = balance × oraclePrice / 10^decimals / 10^8` then sort descending.
- Same logic for debt selection (rare for users to have many debts, but consistent).

### Unswappable Aave V3 reserves on HyperEVM
- **HypurrFi**: PT-kHYPE-13NOV2025, PT-kHYPE-19MAR2026 (Pendle Principal Tokens — only redeemable via Pendle router or at maturity), beHYPE (Belt aggregator wrap — limited HyperSwap liquidity)
- **HyperLend**: all 8 reserves have HyperSwap pools to USDC or USD₮0
- Filter at executor presign: `swappableColl = collateral.filter(c => !UNSWAPPABLE_COLL.has(c.symbol))`. If empty → skip the fire (we'd just no-op anyway).

### Gas-estimation revert == Aave-side bailout
- `provider.estimateGas({...})` reverts when the tx would revert at execution. Use this as a free pre-pre-flight check.
- Cheaper than the manual `provider.call(...)` pre-flight because it short-circuits earlier.
- Wrap in try/catch, log shortMessage, return null on revert — no Telegram noise.

### Balance watcher pattern for race wallets
- Race executors broadcast frequently. Native-gas balance can drift down silently until txs start failing with "out of funds".
- Polling pattern: piggyback on the monitor's existing poll loop. Every N polls (~1 hour), `eth_getBalance(signer, 'latest')`.
- Dedup Telegram alerts with `Date.now() - alertedAt > 6 * 60 * 60 * 1000`. One alert per 6h = signal, not noise.
- HyperEVM specific: 0.005 HYPE ≈ 2 fires of headroom at 2M gas × 1 gwei. Set threshold there.

### HyperLend uses higher-than-default LIF
- Aave V3 default LIF = 5%. HyperLend observed up to 14% on UETH liquidations (backtest 2026-05-20).
- Implication for estimate-and-cap: under-estimating seized collateral with 5% LIF → leftover dust transferred to OWNER. Safe direction. NEVER over-estimate (would cause swap-insufficient-input revert).
- Reserves config likely sets per-asset `LIQUIDATION_BONUS` higher for volatile collateral. Don't assume Aave defaults.

## Proposed Skill Content

A consolidated `hyperevm` skill should cover:
1. **RPC tiering**: Alchemy free tier (limits 10-block getLogs and rejects historical eth_call); public/stakely/purroofgroup rotation for log scans + archive reads. See [[project_alchemy_hyperevm_getlogs]] + [[hyperevm-backtest]].
2. **eth_getLogs hard limits**: HyperEVM 1000-block per call; 3-way rotation at MAX_PARALLEL=3 ≈ 1 chunk/sec.
3. **Aave V3 fork patterns**: dynamic reserves via `Pool.getReservesList()`, base unit 1e8 USD via AaveOracle, `getUserAccountData` returns `(coll, debt, available, threshold, ltv, healthFactor)`.
4. **EIP-55 address strictness in ethers v6**: lowercase-then-getAddress at config load (HypurrFi POOL had wrong checksum — crashed late, after 9-min scan).
5. **Dual-block gas model**: 2M small / 30M big; never enable big-block for race wallets.
6. **Liquidator hardening checklist**: fresh-HF re-verify, gas-estimate bail-out, USD-value sort, unswappable filter, balance watcher.
7. **LIF assumption**: 5% is Aave default but forks customize per-asset; observed up to 14% on HyperLend UETH.
8. **Backtest pattern**: scan LiquidationCall events, query pre-block HF via historical eth_call on public RPC, `HF=MaxUint256` signals oracle-bundle.

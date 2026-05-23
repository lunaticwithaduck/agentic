---
domain: hyperevm
source_task: 2026-05-20-hyperevm-h4-backtest.md
date: 2026-05-20
keywords: ["hyperevm", "backtest", "liquidation", "alchemy", "historical state", "archive node"]
---

## Extracted Knowledge

### Alchemy free tier on HyperEVM, part 2
- Builds on [[project_alchemy_hyperevm_getlogs]]: also **rejects historical block-tag eth_call** with `-32001 "Unable to complete request at this time."`
- This is silent if the caller swallows errors via Promise.all + null fallback — backtester ran successfully for 9 min and reported "0 events analyzed" before the bug was caught
- Public RPC `rpc.hyperliquid.xyz/evm` and `hyperliquid-json-rpc.stakely.io` honor historical state queries fine
- Pattern: use Alchemy only for `latest`-tag reads (monitor, executor pre-flight). Route every historical query through the public-RPC rotation.

### HyperEVM does NOT need anvil-fork for backtesting
- Earlier smoketests tried `anvil --fork-url https://rpc.hyperliquid.xyz/evm` and got `accrueInterest` overflows on InterestRateStrategy contracts
- Direct historical eth_call to a real archive node is simpler, faster, and free
- For backtesting LiquidationCall events: scan logs → for each event read `Pool.getUserAccountData(user)` at `blockNumber - 1` → catchable iff HF<1e18
- USD profit: `(collateralAmount × collPrice / 10^collDec - debtToCover × debtPrice / 10^debtDec) / 1e8` using AaveOracle prices at the same block

### Aave V3 LiquidationCall event shape
```solidity
event LiquidationCall(
    address indexed collateralAsset,   // topic[1]
    address indexed debtAsset,         // topic[2]
    address indexed user,              // topic[3]
    uint256 debtToCover,               // data
    uint256 liquidatedCollateralAmount,// data
    address liquidator,                // data
    bool receiveAToken                 // data
);
```
Topic hash: `0xe413a321e8681d831f4dbccbca790d2952b56f977908e45be37335533e005286`

### HyperLend liquidator landscape (snapshot 2026-05-20)
- ~8 liquidations/day across 10+ liquidator wallets
- Top liquidator captures ~29% of fires (likely WSS-subscribed)
- 65% of fires are structurally catchable by HTTP polling (HF<1 in prior block)
- 35% are atomic/oracle-bundled — same-block oracle update + liquidation, uncatchable without bundle access
- Most "catchable" fires have `HF_pre = 0.998-0.999` (right at the gate) — need sub-second polling to win, not 5s

### `HF_pre = 1.157920892373162e+59` interpretation
- Equals `uint256.max / 1e18` = `(2^256-1) / 1e18`
- Means the user had 0 debt in the prior block (Aave returns MaxUint256 for HF when debt is 0)
- Borrowed AND became liquidatable in the same tx — classic oracle-bundle pattern
- Correctly classified as ⚡ uncatchable

### HyperLend uses higher-than-default LIF
- Observed profit/debt ratios up to 14% on UETH liquidations
- Aave V3 default LIF is 5% (LIQUIDATION_BONUS = 10500)
- HyperLend's reserve config likely sets LIF higher for volatile collateral
- Implication for our estimate-and-cap swap-path wiring: real seized collateral > our 5%-LIF estimate, so `swapAmountIn = 0.97 × expected` will leave dust (good — goes to OWNER), never reverts for under-supply

## Proposed Skill Content

Backtest pattern for any Aave V3 fork on a chain with available archive RPC:
1. Scan `LiquidationCall` event over N blocks via chunked getLogs (1000-block windows on HyperEVM)
2. For each event, query `Pool.getUserAccountData(user)` at `block - 1`
3. catchable iff `healthFactor < 1e18` (== 1.0 in fixed point)
4. Profit = `collateralValueUSD - debtValueUSD` using AaveOracle prices at same block
5. Aggregate by liquidator address → market share + concentration risk

Bookkeeping: filter `HF = MaxUint256` to detect oracle-bundled atomic borrows (they show as "infinite HF in prior block" — user had no debt at all).

---
domain: defi-liquidations
source_task: 2026-05-18-flash-loan-week1-research.md
date: 2026-05-18
keywords: ["liquidation", "morpho", "flash loan", "lif", "blend", "liquity", "beraborrow", "bend", "berachain", "blur", "race-bot", "lending"]
---

## Extracted Knowledge

### Morpho Blue / Bend — the cleanest liquidation race target
Bend on Berachain is a Morpho Blue fork. Singleton: `0x24147243f9c08d835C218Cda1e135f8dFD0517D0`. Key mechanics:

- **Liquidate ABI:** `liquidate(MarketParams calldata marketParams, address borrower, uint256 seizedAssets, uint256 repaidShares, bytes calldata data)` returns `(uint256, uint256)`. Permissionless — anyone can call.
- **Liquidation Incentive Factor (LIF):** `LIF = min(maxLIF, 1 / (CURSOR × (1 - LLTV)))` where `CURSOR = 0.3`, `maxLIF = 1.15`. **For any LLTV ≥ ~0.75, LIF caps at the 1.15 max (= 15% bonus to liquidator).** Bend's own docs claim 5% — this is incorrect. Verify by computing: at LLTV=0.86, `1/(0.3 × 0.14) = 23.8`, `min(1.15, 23.8) = 1.15`.
- **Free flash loan in the same contract:** `flashLoan(address token, uint256 assets, bytes data)`, 0% fee, callback `IMorphoFlashLoanCallback.onMorphoFlashLoan(uint256 assets, bytes data)`. This enables atomic `flashLoan → liquidate → swap collateral → repay` in one external call.
- **Isolated markets:** Each market is `(loanToken, collateralToken, oracle, irm, lltv)`. Market ID is `keccak256(abi.encode(marketParams))`. Markets are independent — one market crash doesn't socialize losses.

### How to tell a Morpho Blue singleton from a MetaMorpho vault
Both addresses look similar in registries and docs may label them ambiguously. Probe:
- Singleton has `DOMAIN_SEPARATOR()` (selector `0x3644e515`). Reverts on vault.
- Vault has `MORPHO()` returning the singleton address. Reverts on singleton.
- Vault has ERC-4626 surface: `asset()` (`0x38d52e0f`), `name()`, `symbol()`. All revert on singleton.

If `DOMAIN_SEPARATOR()` succeeds and `MORPHO()` reverts → singleton.

### Morpho event topic table
Topics computed via `ethers.id(signature)`. The Bend/Morpho-Blue layout:

| Event | Topic |
|---|---|
| `Borrow(bytes32,address,address,address,uint256,uint256)` | `0x570954540bed6b1304a87dfe815a5eda4a648f7097a16240dcd85c9b5fd42a43` |
| `Supply(bytes32,address,address,uint256,uint256)` | `0xedf8870433c83823eb071d3df1caa8d008f12f6440918c20d75a3602cda30fe0` |
| `Withdraw(bytes32,address,address,address,uint256,uint256)` | `0xa56fc0ad5702ec05ce63666221f796fb62437c32db1aa1aa075fc6484cf58fbf` |
| `Repay(bytes32,address,address,uint256,uint256)` | `0x52acb05cebbd3cd39715469f22afbf5a17496295ef3bc9bb5944056c63ccaa09` |
| `SupplyCollateral(bytes32,address,address,uint256)` | `0xa3b9472a1399e17e123f3c2e6586c23e504184d504de59cdaa2b375e880c6184` |
| `WithdrawCollateral(bytes32,address,address,address,uint256)` | `0xe80ebd7cc9223d7382aab2e0d1d6155c65651f83d53c8b9b06901d167e321142` |
| `Liquidate(bytes32,address,address,uint256,uint256,uint256,uint256,uint256)` | `0xa4946ede45d0c6f06a0f5ce92c9ad3b4751452d2fe0e25010783bcab57a67e41` |
| `CreateMarket(bytes32,(address,address,address,address,uint256))` | `0xac4b2400f169220b0c0afdde7a0b32e775ba727ea1cb30b35f935cdaab8683ac` |
| `AccrueInterest(bytes32,uint256,uint256,uint256)` | `0x9d9bd501d0657d7dfe415f779a620a62b78bc508ddc0891fbbd8b7ac0f8fce87` |
| `FlashLoan(address,address,uint256)` | `0xc76f1b4fe4396ac07a9fa55a415d4ca430e72651d37d3401f3bed7cb13fc4f12` |

**Morpho Liquidate event layout (data field, 5 × 32 bytes):**
```
slot 0: repaidAssets       (uint256, in loan-token units)
slot 1: repaidShares       (uint256)
slot 2: seizedAssets       (uint256, in collateral-token units)
slot 3: badDebtAssets      (uint256, 0 if collateral fully covered debt)
slot 4: badDebtShares      (uint256)
```
Liquidator gross profit (in loan-token units) = `repaidAssets × (LIF - 1)`. To convert seizedAssets to USD, use the market's oracle price — or infer it from the repay/seize ratio if you know LIF: `implied_collateral_price = (repaidAssets × LIF) / seizedAssets`.

### Liquity V2 forks (Beraborrow) — permissionless but gas-comp-only
Liquity V2 pattern (and forks like Beraborrow): liquidation IS permissionless, but **bulk of seized collateral goes to the Stability Pool depositors, not the liquidator.** The msg.sender only gets:
- Flat gas comp: `ETH_GAS_COMPENSATION` constant = `0.0375 ETH` (or chain-native equivalent)
- Plus `min(0.5% × seizedCollateral, 2 LST_units)`

Net: a single $200k Den liquidation might pay the liquidator ~$10-20 total. Only worth it on `batchLiquidate(address[])` calls where you amortize gas across many positions.

### Blur Blend NFT lending — DEAD as a race target
Blend's `seize()` function is gated to `msg.sender == lien.lender`. The NFT goes directly to the lender. **There is no third-party liquidator role, no MEV race, no bonus.** Don't waste time building for it.

Even if there were a race, atomic exit is blocked: `BlurExchangeV2.takeBid` requires a fresh off-chain oracle signature keyed on `msg.sender`. A flash-loan contract cannot atomically pre-sign its own future calldata, so `flashLoan → seizeNFT → Blur.takeBid → repay` is structurally impossible.

The closest adjacent play is becoming the new lender via `refinanceAuctionByOther()`, but that's a yield/credit business (you hold a defaulted loan), not a fast-execution arb.

### Berachain RPC quirks for liquidation bots
- **publicnode.com**: getLogs accepts ~50k-block ranges cleanly; 100k blocks fail when running 14+ chunks in parallel due to rate limits. Use 50k chunks with 3x parallel max.
- **publicnode.com** is NOT an archive node — `eth_getCode(addr, oldBlock)` returns "historical state not available" beyond ~last 128-1000 blocks. To find contract deploy blocks, scan `eth_getLogs` for the first event (e.g. CreateMarket) instead of binary-searching getCode.
- **WSS endpoint**: `wss://berachain-rpc.publicnode.com` — works for `newHeads` subscription. Same pattern as MIBERA's loan-132 execute.js.
- **Dual-broadcast pattern**: `Promise.any([primary, fallback])` against `https://berachain-rpc.publicnode.com` + `https://rpc.berachain.com` (the official QuickNode-backed RPC). First to ack wins.

### Berascan API surface (2026)
- Berascan V1 API (`api.berascan.com`) is deprecated. All public calls return: "You are using a deprecated V1 endpoint, switch to Etherscan API V2."
- New endpoint: `https://api.etherscan.io/v2/api?chainid=80094&module=...&action=...`
- Requires Etherscan API key (free tier exists). Without a key, falls back to scraping the HTML pages — slow but works.

### Race architecture pattern (general, reusable for any on-chain liquidation)
The MIBERA pattern proves out on race-driven liquidation. Key components:
1. **Indexer** — long-lived, tracks every open position via event log subscription
2. **Monitor** — recomputes health factor each block for top-N closest-to-liquidation positions; full-rescan every ~60s
3. **Hot executor** — pre-signs the tx as soon as a position is "armed" (HF < threshold); holds the signed tx in memory
4. **Trigger** — WSS newHeads sub fires the broadcast on the block where HF crosses 1.0; dual-RPC race for delivery
5. **Wall-clock fallback** — backup timer fires at T+Ns in case WSS dropped events
6. **Atomic contract** — `onMorphoFlashLoanCallback` (or equivalent) does flashLoan + liquidate + DEX swap + repay; reverts if profit < minProfitWei

### Picking targets — empirical filters
A liquidation venue is worth racing if ALL of:
1. **Permissionless** liquidation entry point (no `globalOperator` gating, no whitelist)
2. **Liquidator receives the bonus directly** to `msg.sender` (not socialized to a Stability Pool, not paid as fixed gas comp)
3. **Same-tx exit** for the seized collateral on a DEX (not held inventory)
4. **Flash loan source** available at same-tx scope (Balancer V2 / Morpho singleton / Aave V3 — fees 0-0.05%)
5. **Real activity** in the last 30 days (≥4-8 liquidations/month based on history)

If any of these fail, skip the venue — the race economics don't work.

## Failure Modes Observed

### Confidently wrong about Blend on first pass
On the first research pass I ranked NFTfi/Blend as the highest-EV opportunity ($500-10k/hit, less competition). The verification pass killed it at the contract level — should have read `Blend.sol` source BEFORE the recommendation, not after. Lesson: for any on-chain race opportunity, verify the actual liquidation function exists and is permissionless by reading source code, not relying on protocol docs or market intuition. Docs lie about who can liquidate.

### Wrong about LIF being 5% (docs error)
Docs at `docs.berachain.com/bend/learn/liquidation.md` claim LIF = 1.05 at LLTV 86%. Plugging into the actual Morpho formula `min(1.15, 1/(0.3 × (1-LLTV)))` gives 1.15 (capped at max). All Bend markets pay 15% bonus, not 5%. The docs' example math is wrong. Lesson: when a doc gives a numeric example that's load-bearing for the decision, verify by computing from the cited formula.

### Confused "Morpho (Vault)" label
Bend's docs page lists the singleton with the label "Morpho (Vault): 0x24147243...". The word "Vault" suggested MetaMorpho vault, which doesn't emit Liquidate events. Sample event topics from the address didn't match my computed Morpho event topics → assumed wrong contract. Probing `MORPHO()` (vault) vs `DOMAIN_SEPARATOR()` (singleton) confirmed it IS the singleton — the label is just confusingly named.

## Proposed Skill Content

A future `.claude/skills/defi-liquidations.md` would cover:

**Section: Pre-build verification — read the source**
- Before recommending any liquidation venue, verify by reading the actual `liquidate()` / `seize()` / equivalent function source. Confirm permissionless caller, confirm liquidator receives the bonus to `msg.sender`, confirm collateral exit can compose atomically with the seizure.
- Protocols that look like races on the surface but aren't: Blend (lender-only), Dolomite (globalOperator-gated), Liquity V2 forks (gas-comp only, bulk to SP).
- Docs can be wrong about both mechanics and numeric examples — sanity-check formulas.

**Section: Morpho Blue mechanics**
- LIF formula and where it caps
- Atomic `flashLoan` + `liquidate` pattern
- Singleton vs MetaMorpho vault disambiguation
- Liquidate event layout and decode pattern
- Computing implied collateral price from repaid/seized ratio

**Section: Race architecture (reusable across protocols)**
- Indexer / Monitor / Executor / Trigger / Fallback / Atomic contract split
- WSS pre-sign + dual-broadcast pattern
- Block-position race vs oracle-update race (when applicable)

**Section: Chain-specific quirks**
- Berachain RPC limits (publicnode 50k getLogs blocks)
- Berascan V2 API migration
- Distinguish chains with Pyth (block-race-able) vs Chainlink (threshold-update)

**Section: Filter for "is this venue worth racing"**
- 5-point checklist: permissionless, msg.sender bonus, atomic exit, flash loan source, real activity

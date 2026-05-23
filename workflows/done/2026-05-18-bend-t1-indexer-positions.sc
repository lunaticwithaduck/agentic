---
domain: defi-liquidations
source_task: 2026-05-18-bend-t1-indexer-positions.md
date: 2026-05-18
keywords: ["indexer", "morpho", "position", "eth_getlogs", "wss", "berachain", "bend"]
---

## Extracted Knowledge

### Morpho Blue position-indexer architecture
For Morpho-style isolated-market lending, the right indexer pattern is **events for discovery, on-chain reads for state**:

1. **Discovery** — enumerate `(marketId, borrower)` pairs by scanning `SupplyCollateral` + `Borrow` events. These are the only two ways a borrower position is born. Topics:
   - SupplyCollateral: `0xa3b9472a1399e17e123f3c2e6586c23e504184d504de59cdaa2b375e880c6184` (id, caller, onBehalf)
   - Borrow: `0x570954540bed6b1304a87dfe815a5eda4a648f7097a16240dcd85c9b5fd42a43` (id, caller, onBehalf, receiver)
   - In both, `onBehalf` is `topics[3]` — that's the borrower
2. **State reconciliation** — for each discovered pair, call `position(bytes32, address)` directly on the singleton. Never derive position state from event deltas — too easy to miss an event and drift. The on-chain state is the truth.
3. **WSS tail** — subscribe to position-affecting topics only (SupplyCollateral, WithdrawCollateral, Borrow, Repay, Liquidate). On each event, re-read `position()` for the affected `(marketId, borrower)` and update the registry.

### `position()` return decoding
Morpho's `position` getter returns a struct via Solidity ABI as 3 slots:

```
slot 0 (uint256): supplyShares    # lender-side claim on supplied liquidity
slot 1 (uint128): borrowShares    # borrower-side debt shares (NOT assets)
slot 2 (uint128): collateral      # raw collateral token amount (NOT USD)
```

Total = 96 bytes. The uint128 fields are right-padded to 32 bytes each in ABI encoding.

A non-zero supplyShares with zero borrowShares/collateral = pure lender. Skip these — they're not liquidation candidates.

### Shares-to-assets conversion (Morpho)
Borrow shares ARE NOT assets. They scale with accrued interest. Convert:

```
borrowAssets = borrowShares × totalBorrowAssets / totalBorrowShares
```

where `totalBorrowAssets` and `totalBorrowShares` come from the `market(Id)` read on the same block. Re-read totals on every reconcile — they change every block due to interest accrual.

### Per-market collateral decimals MUST be read per token
Different markets use different collateral decimals. WBTC = 8, WETH/most ERC-20s = 18, USDC = 6, etc. Read once per market by calling `decimals()` (selector `0x313ce567`) on the collateral token. Cache it in the market record.

A single hardcoded `/1e18` divisor will silently mis-display WBTC (off by 10 orders of magnitude). The data persisted is fine — only the formatting is wrong, which makes debugging hard.

### Discover ALL markets via CreateMarket events
Docs may list a partial set. Scan `CreateMarket` (topic `0xac4b2400f169220b0c0afdde7a0b32e775ba727ea1cb30b35f935cdaab8683ac`) from genesis to find the complete market set. On Bend we found 1 market beyond the 6 documented.

### Filter WSS subscription to position-affecting events only
A naive `Object.values(TOPICS)` OR-filter pulls in FlashLoan, AccrueInterest, CreateMarket — none of which change positions. On Bend, a single arb bot (`0xc1fad5...`) fires ~1500 FlashLoan events/hour. The filter should be:

```js
const topics = [[SUPPLY_COLLATERAL, WITHDRAW_COLLATERAL, BORROW, REPAY, LIQUIDATE]];
```

This drops ~99% of event volume on Bend.

### publicnode.com getLogs limits + parallelization
- Single call: up to 50k blocks reliably; 100k often fails with "could not coalesce error"
- Parallel calls: 3-at-a-time works; 14+ parallel chunks trigger rate-limit failures (NO descriptive error message)
- Retry-with-shrink pattern: on error, halve chunk size and retry up to 4 times before giving up

### Atomic JSON persistence pattern
```js
function atomicWrite(file, data) {
  const tmp = file + '.tmp';
  fs.writeFileSync(tmp, data);
  fs.renameSync(tmp, file);  // atomic on POSIX
}
```
Prevents corruption if the process dies mid-write. Worth doing for any long-running indexer.

### Cursor-based incremental updates
Persist `{ lastScanned: blockNumber }` to disk after each successful scan. On subsequent runs, scan only from `cursor.lastScanned + 1`. Avoid re-scanning the CreateMarket history when markets are already cached.

## Proposed Skill Content

Continues building the `defi-liquidations` skill from the research `.sc`. Adds:

**Section: Indexer architecture for Morpho-style lending**
- Discovery (events) vs reconciliation (on-chain reads) split
- WSS subscription topic filtering to drop non-position-affecting noise
- Atomic persistence + cursor-based incremental updates

**Section: Reading Morpho state on-chain**
- `position()` struct decoding (uint256 + uint128 + uint128 = 96 bytes)
- Shares-to-assets conversion formula
- Per-market collateral decimals discovery

**Section: RPC scanning patterns**
- publicnode 50k-block limit + parallel-3 sweet spot
- Retry-with-shrink on getLogs failure
- Mining historical state when public nodes don't keep archive data

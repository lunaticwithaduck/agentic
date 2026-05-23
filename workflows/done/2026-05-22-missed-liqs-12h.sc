---
domain: defi-liquidations
source_task: 2026-05-22-missed-liqs-12h.md
date: 2026-05-22
keywords: [missed-liquidations-audit, eth_getLogs-chunking, event-topic-decoding, dust-competitor, rpc-fallback-pool, competitive-analysis]
---

## Extracted Knowledge

### Liquidation event topic + decoding cheat sheet
- **Aave V3 LiquidationCall**: topic0 = `0xe413a321e8681d831f4dbccbca790d2952b56f977908e45be37335533e005286`
  - topics[1] = collateralAsset, topics[2] = debtAsset, topics[3] = user
  - data layout (4 words): debtToCover(32) | liquidatedCollateralAmount(32) | liquidator(32-padded) | receiveAToken(32-bool)
  - **liquidator is at data offset 64**, last 20 bytes of the 3rd 32-byte word

- **Morpho Blue Liquidate**: topic0 = `0xa4946ede45d0c6f06a0f5ce92c9ad3b4751452d2fe0e25010783bcab57a67e41`
  - topics[1] = marketId, topics[2] = caller (the liquidator), topics[3] = borrower
  - data: repaidAssets, repaidShares, seizedAssets, badDebtAssets, badDebtShares (5 × uint256)
  - **Liquidator in topic2** (much easier to filter than Aave V3)

### Chunked eth_getLogs with fallback RPC pool
Public RPCs have wildly varying limits:
- Ink (rpc-gel): ~10k blocks/call OK
- HyperEVM stakely / purroofgroup: ~500 blocks/call OK
- Berachain (rpc.berachain.com): ~10k blocks/call OK
- Monad QuickNode: ~500 blocks/call before rate limit
- Berachain publicnode: 1200 req/min rate cap

Pattern: try each RPC in turn on failure. Keep chunks small enough that the most-restrictive RPC accepts them.

```js
async function rpc(rpcs, method, params) {
  let lastErr;
  for (const url of rpcs) {
    try { return await rpcOne(url, method, params); }
    catch (e) { lastErr = e; await new Promise(r => setTimeout(r, 200)); }
  }
  throw lastErr;
}
```

### "5 liquidations missed" doesn't mean "5 opportunities lost"
Always materialize USD-value per fire before concluding. A competitor capturing 5
liquidations might be a dust-scavenger running net-zero or losing money. Real check:

```
profit_usd = seized × collateralPriceUsd - repaid × loanPriceUsd - gas_cost
```

If profit_usd < $1 per fire, the "competitor" is wasting gas, not extracting real value.
We should NOT reactively lower our `MIN_DEBT_USD` filter to chase dust.

### Felix dust example (2026-05-22)
5 liquidations on Felix WHYPE/USDC market over 12h by same competitor. Total profit
across all 5: **$0.21**. Per fire: $0.04. HyperEVM gas $0.01-0.05. Competitor running
near zero. Our `MIN_DEBT_USD = 20` filter correctly skipped these — no action needed.

### When zero-liq-volume is the noteworthy signal
A 12h audit returning zero liquidations on 5 of 6 scanned chains tells you the broader
market has been stable, not that your bot is broken. Pair this with the cliff-scope
data (positions near HF=1.0 but holding) for the full picture.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Missed-liquidation audit pattern
For ongoing competitive monitoring, periodically scan each protocol's liquidation
event for the last N hours, filter out your own liquidator addresses, materialize
USD profit per fire. Decisions follow: if per-fire profit > MIN_DEBT × LIF, tune the
monitor; if dust, leave the filters alone.

### Aave V3 + Morpho event topic + decoder
Memorize: Aave V3 LiquidationCall topic `0xe413a321…`, Morpho Liquidate topic
`0xa4946ede…`. Aave V3 liquidator in data offset 64; Morpho caller in topic2.

### RPC fallback pool pattern for log scans
Don't hardcode one RPC. Maintain a per-chain ordered list; try each on failure. Chunk
sizes calibrated to the most-restrictive provider in the pool.

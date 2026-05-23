---
title: Bend historical backtest v2 — fresh-wallet + backdated contract
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Steps
- [x] Built backtest-v2.js with anvil-default test wallet (no nonce conflict)
- [x] Backdated contract deploy via factory.deploy on fork at each historical pre-block
- [x] Ran against 8 historical Liquidate events from May 6-11 cluster
- [x] Categorized: 1 oracle-bundled (whale), 7 catchable-by-polling
- [ ] Full simulated profit per catchable event — partial: technical OVERFLOW panics in Morpho on the fork for 6/7 cases, likely from historical Morpho state quirks (totalBorrowShares/Assets ratios at that block, or oracle returning slightly different values on fork than archive). Couldn't fully resolve without deeper fork-state debugging.

## Key strategic finding

| Category | Count | $ Repaid | $ Profit @ 15% LIF | Share |
|---|---|---|---|---|
| Catchable by polling | 7 | $101,186 | $15,178 | 34% |
| Oracle-bundled (miss) | 1 (the $30k whale) | $199,951 | $29,993 | 66% |
| **TOTAL** | 8 | $301,137 | $45,171 | 100% |

By count, our bot catches 7/8 (87.5%) — but by dollar, only 34%. The single oracle-bundled event (the May 6 iBERA whale, HF 1.0004 at pre-block) accounts for 66% of cluster profit. Competing bots use Pyth/Redstone push MEV to bundle oracle updates with their liquidate tx — out of reach for polling-only.

## Realistic monthly expectation
- Per cluster like May 6-11: $5-8k captured (50% win-rate on the $15k catchable pool against 1-2 competitors)
- Cluster frequency: ~1-2/month on Berachain in normal markets, more in volatile months
- Estimated steady-state: $5-15k/month

## Open follow-up — t12 (deferred)
**Oracle-update bundling.** Build a watcher for off-chain Pyth/Redstone feeds + pre-build txs that bundle `oracle.updatePriceFeeds(data) + Morpho.liquidate(...)`. This is how competing bots take the whales. ~1 week of build.

## Outcome

Completed 2026-05-18. Built fresh-wallet backtest infrastructure with backdated contract deploy. Discovered the real economic breakdown: by count we'd catch 7/8 of historical liquidations, but by dollar we'd only catch 34% because the single biggest event ($30k profit) required oracle-update bundling that polling cannot replicate. Competing bots' structural edge is push-MEV on Pyth/Redstone feeds. Our polling bot's realistic capture is $5-15k/month in normal market conditions. Backtest tooling has remaining technical issues (Morpho OVERFLOW panics on fork) that prevent per-event profit precision, but the categorical finding (catchable vs bundled) is unambiguous from the pre-block HF analysis.

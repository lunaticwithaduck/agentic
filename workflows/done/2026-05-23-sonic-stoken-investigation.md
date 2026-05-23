---
title: Investigate atomic liquidation path for Sonic Silo V2 sTokenRequired positions
created: 2026-05-23
completed: 2026-05-23
status: done
---

## Goal
Borrower 0xbf5b0bc2 sat at cliff ~13 min yesterday with $4M wS debt (~$280k profit
at full liquidation). Investigate whether a DEX-based atomic liquidation path was missed.

## Steps
- [x] Probe live silo state
- [x] Check all 6 Silo V2 share token addresses for DEX presence
- [x] Verify Beets-wrapped derivatives
- [x] Conclude

## Outcome

Completed 2026-05-23. **Verdict: no atomic liquidation path exists today. Confirmed via exhaustive check.**

### Live state at probe time
- `silo0.getLiquidity()` = 0 (wS side)
- `silo1.getLiquidity()` = 0 (USDC.e side)
- `USDC.balanceOf(silo1)` = $87,924 physical — but protocol invariants block withdraw
- → atomic `silo.redeem()` reverts in-tx

### All 6 Silo V2 share tokens checked via CoinGecko onchain
| Share token | Address | DEX pools |
|---|---|---|
| sWS collateral | `0xf55902DE…` | **0** |
| sWS debt | `0xE5c066B2…` | **0** |
| sWS protected | `0xAecD6cBf…` | **0** |
| sUSDC collateral | `0x322e1d53…` | 3 (Beets-wrapped, max $264 reserve) |
| sUSDC debt | `0xbc4eF1B5…` | **0** |
| sUSDC protected | `0x0B960e95…` | **0** |

CoinGecko onchain endpoint covers all Sonic DEXes (Shadow, Equalizer, Beethoven, Curve-on-Sonic). Result is comprehensive.

### Why no path exists
1. **No sWS DEX pools at all** — across all 3 variants
2. **sUSDC only has Beets-wrapped pools** maxing $264 reserve — useless for $100k+ trades
3. **Direct `silo.redeem()` reverts** when protocol enforces 100% utilization invariants — physical underlying exists but is locked

### Confirmed structural block (NOT executor code bug)
This matches the prior [[sonic-stoken-handling]] .sc verdict. The current state is identical
to what was documented yesterday — these positions are uncapturable by atomic flashloan
liquidation. Capturing them requires:
- Non-atomic multi-tx with own-capital funding (~$200k+ wS pre-stocked)
- Multi-day capital lockup waiting for `silo.getLiquidity() > 0`
- Silo bad-debt exposure during the hold window

### Decisions to prepare for user
1. Build liquidity watchdog (passive, cheap, informs us when silo briefly opens)
2. Add silo `0xf55902DE…` to `UNFIREABLE_MARKETS` in `fleet-cliff-report.js` (suppress false-hope alerts)
3. Build non-atomic V2 with own-capital funding (~$200k+ exposure) — only worth it if user values $280k single-fire upside enough to accept the capital risk

## Skill candidate evaluation
- Technologies/frameworks touched: Silo V2 share-token discovery via CoinGecko onchain endpoint, Beets-wrapped derivatives audit, silo `getLiquidity()` vs physical balance discrepancy
- Domain-specific knowledge involved: the 6-share-tokens-per-market exhaustive check pattern; physical underlying balance ≠ withdrawable liquidity (protocol invariants); Beets-wrapped sTokens as a derivative class that creates dust DEX pools but doesn't unlock liquidation paths
- Verdict: **GENERATE**
- Reason: New "exhaustive share-token DEX audit" methodology; new "physical balance vs protocol-enforced liquidity" failure mode; confirms structural verdict from prior .sc with live data — strengthens the rule.

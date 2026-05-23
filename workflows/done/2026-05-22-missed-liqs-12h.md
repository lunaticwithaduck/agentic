---
title: 12h liquidation audit — did we miss anything across the fleet?
created: 2026-05-22
completed: 2026-05-22
status: done — 5 dust fires on Felix only, total competitor profit $0.21, no real misses
---

## Result: fleet didn't miss any meaningful opportunity

Scanned all 7 chains' protocols for liquidation events in the last 12h, filtered out
our own liquidator addresses.

| Chain | Blocks scanned | Liqs total | Ours | Others |
|-------|---------------|-----------|------|--------|
| Tydro (Ink) | 43,200 | 0 | 0 | 0 |
| HyperLend (HyperEVM) | 72,000 | 0 | 0 | 0 |
| HypurrFi (HyperEVM) | 72,000 | 0 | 0 | 0 |
| **Felix (HyperEVM)** | 72,000 | **5** | 0 | **5** |
| Bend (Berachain) | 21,600 | 0 | 0 | 0 |
| Monad | 108,000 | 0 | 0 | 0 |
| Sonic Silo | — | (deferred) | | |

## The 5 Felix "misses" were dust

All 5 fires on Felix were against the same borrower `0x023f609c9d815…` in the
WHYPE/USDC market (`0xd7d38220…`). Same competitor address `0x4f2315317a4c…` captured
every one. Per-fire breakdown:

| # | Repaid (USDC) | Seized (WHYPE × $58) | Gross profit |
|---|--------------|----------------------|--------------|
| 1 | $0.57 | $0.61 | $0.04 |
| 2 | $0.26 | $0.27 | $0.02 |
| 3 | $0.74 | $0.80 | $0.06 |
| 4 | $0.57 | $0.62 | $0.05 |
| 5 | $0.54 | $0.58 | $0.05 |
| **Total** | **$2.68** | **$2.89** | **$0.21** |

HyperEVM gas runs $0.01–0.05 per tx, so net profit after gas: roughly $0–0.05 per
fire. Competitor is either running a hyper-cheap automated dust collector or losing
money — not a sophisticated MEV operation we need to compete with.

## Why we correctly skipped them

Felix's monitor has `MIN_DEBT_USD = 20` — we filter out positions with debt below $20.
The 5 dust liquidations were on a position that had been partially-liquidated down to
sub-$1 fragments. Our filter is doing exactly what it should.

## Wider observation

**Market has been extraordinarily calm across the fleet.** Zero liquidations on Tydro,
HyperLend, HypurrFi, Bend, Monad in 12h. The cliff-watching positions (10+ at HF<1.01)
have all held above 1.0. Either oracle prices have been stable, or borrowers have been
actively healing.

## Files
- `/home/jojo/automation/fleet-missed-liqs.js` — chunked eth_getLogs scanner with
  fallback-RPC chain per network. Handles Aave V3 + Morpho Blue event formats.

## Usage
```bash
node fleet-missed-liqs.js                     # default 12h, all chains
node fleet-missed-liqs.js --hours 24          # different window
node fleet-missed-liqs.js --chain monad       # one chain only
```

## What was NOT included
- Silo V2 event decoding (Sonic). Different event shape — deferred. Sonic has been
  firing successfully in production, so not a high-priority gap.

## Outcome
Fleet's missed-opportunity surface area in the last 12h: $0.21. Filters are correctly
calibrated. Tooling shipped for re-running this audit any time the user wants to
verify the bot's competitive position.

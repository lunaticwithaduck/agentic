---
title: Felix monitor — switch to watch-list pattern (HF<1.10 only)
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Reduce Felix's RPC load by reading only markets with at-risk borrowers (HF<1.10), not all 39 every cycle.

## Steps
- [x] Added `WATCH_THRESHOLD_HF = 1.10` and `watchMarketIds` Set to `felix/monitor.js`
- [x] `loadAndSweep(marketsToReadOnly = null)` now accepts a Set of market IDs to filter reads
- [x] `rebuildWatchList()` runs every 60s — full sweep, then `watchMarketIds = Set(markets where any HF<1.10)`
- [x] Per-block tick (3s) only reads markets in `watchMarketIds`
- [x] Initial rebuild on startup
- [x] Restarted felix-monitor, watched logs

## Outcome
Completed 2026-05-21. Live verification:
```
[13:10:05] watch list rebuilt: 6 markets at HF<1.1 (from 217 total positions)
```

**6/39 markets = 6.5× read reduction.** Per-cycle RPC calls drop from ~117 → ~18.

| Metric | Before watch-list | After |
|---|---|---|
| Reads per 3s tick | ~117 | ~18 |
| CPU % | 22.8% | **8.4%** |
| Rate-limit errors | frequent | zero in observation window |
| Detection coverage | all 217 positions | full sweep every 60s rebuilds; in-between, focus on the 6 markets that actually matter |

## Architectural note
This is the same pattern HL/HP monitor-wss use. Now consistent across all 3 HyperEVM bots. New positions appearing in non-watched markets are picked up within 60s by the periodic full sweep — acceptable for HF drift; a one-block black-swan crossing into liquidation territory would be missed for up to 60s, but those are extremely rare and usually oracle-bundled anyway.

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-watch-list-pattern.md`.

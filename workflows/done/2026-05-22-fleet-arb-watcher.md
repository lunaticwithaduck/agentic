---
title: Build passive fleet-wide DEX-arb tripwire watcher (overnight-safe)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Passive overnight watcher across all chains we operate on + Somnia. Telegram alert
only when a tradeable arb gap appears between any two DEXes on the same pair.

## Steps
- [x] Create `/home/jojo/automation/arb-watch/` dir
- [x] Write `check.js` implementing gap detection + change-based dedup
- [x] Write systemd user unit + timer files
- [x] Enable + start timer; run one manual pass to verify wiring
- [x] Send confirmation Telegram to prove end-to-end works

## Outcome

Completed 2026-05-22. Fleet arb-watch live.

**Files shipped:**
- `/home/jojo/automation/arb-watch/check.js` — single Node script, ~120 lines
- `/home/jojo/automation/state/arb-watch.json` — persistent dedup (24h backstop)
- `~/.config/systemd/user/arb-watch.service` — oneshot runner
- `~/.config/systemd/user/arb-watch.timer` — `OnUnitActiveSec=4h`, persistent

**Chains monitored:** berachain, hyperevm, sonic, monad, ink, somnia (via CoinGecko
on-chain endpoint with our existing demo API key — 30/min limit, 6 calls per sweep
at 2.5s pacing).

**Tripwire (change-based dedup, fires once per unique chain+pair until 24h backstop):**
- Gap ≥ 1.5% between 2 DEXes on same canonical token pair
- AND min pool reserve ≥ $50k

**Detection robustness:**
- Fixed base/quote orientation pitfall: pools labeled "USDC/WMON" can have either
  token as base; canonical pair key sorts by token address and price normalizes to
  "lower-address-token per higher-address-token". Initial dry run showed a fake
  139786% gap before this fix; lowering threshold to 0.1% post-fix correctly
  identified real same-pair divergences (Monad USDC/WMON 0.51%, Ink USD₮0/WETH 0.32%,
  Somnia USDC.e/WSOMI 0.55%, none over the 1.5% threshold — matches reality).

**Telegram wiring:**
- Reuses `/home/jojo/automation/mibera/sweeper/telegram` send helper
- First live sweep completed with 0 alerts (production thresholds)
- Confirmation message sent to user's Telegram

**Cadence:** every 4h (6 sweeps/day, ~$0 in API budget at our CoinGecko key).

**Next scheduled fire:** ~04:50 EEST.

## Skill candidate evaluation
- Technologies/frameworks touched: CoinGecko on-chain endpoint (`/api/v3/onchain/networks/{slug}/pools`), systemd user timers (oneshot pattern), change-based-dedup persistent state, GeckoTerminal base/quote orientation normalization
- Domain-specific knowledge involved: the canonical-token-sort approach for cross-pool price comparison; the 139786% false positive as a teaching example; pacing API calls below CG Demo 30/min limit; reusing shared telegram sender from `mibera/sweeper`
- Verdict: **GENERATE**
- Reason: First fleet-arb-watcher pattern. The orientation-fix + canonical pair key pattern is reusable for any cross-DEX comparison tool. The "lower threshold to verify detection logic before deploying" pattern is also reusable.

---
title: Cliff-scope Telegram alert for positions near firing range
created: 2026-05-22
completed: 2026-05-22
status: done — script shipped, Telegram delivery confirmed, systemd timer running every 5 min
---

## Shipped

### `/home/jojo/automation/fleet-cliff-report.js`
Scans all 7 chains' `data/healthfactors.json`. Positions with HF in (1.0, threshold]
and debt > $50 get reported with rough probability + expected profit. Default
threshold: 1.002 (20bps before fire).

### Format

```
🎯 Under scope — might fire soon
2 positions within 20bps of HF=1.0

• `Monad` · `0x76f768db…` · HF `1.0004` · _0x300a4c4f_
  ⚠️ no DEX route · debt $12,774 (unfireable)
• `Monad` · `0x8be46b25…` · HF `1.0042` · _0x8bdb7d2c_
  ~58% chance · est. $274 on success · debt $3,258
```

### Heuristics

- **Chance %** = `max(0, 100 - (HF - 1) × 10000)` — linear; HF 1.0001 → 99%, 1.001 → 90%, 1.005 → 50%, 1.01 → 0%. Coarse estimate; meant as a relative ranking, not precise probability.
- **$ on success** = `debt × chain.lifAvg × 0.70` — 30% haircut for slippage / pool depth.
  - Aave V3 chains (Tydro, HyperLend, HypurrFi): lifAvg ≈ 7.5–8%
  - Morpho chains (Felix, Bend, Monad): lifAvg ≈ 12%
  - Silo (Sonic): lifAvg ≈ 6.5%

### Special handling: unfireable markets

Hardcoded `UNFIREABLE_MARKETS` set on Monad covers `0x67c3a8f2…` (YZM, $4 DEX
liquidity) and `0x300a4c4f…` (aHYPER, withdrawal-queue vault, no atomic redeem).
These get reported with "⚠️ no DEX route · unfireable" label so they don't show
misleading profit estimates.

### Anti-spam: dedup per (chain, borrower) for 30 min

State persisted in `/home/jojo/automation/state/cliff-scope-dedup.json`. Re-alerting
the same position is gated to once every 30 min. `--force` bypasses.

### Automation: systemd timer every 5 min

- `~/.config/systemd/user/fleet-cliff-report.service` — oneshot
- `~/.config/systemd/user/fleet-cliff-report.timer` — every 5 min after boot+2min
- Logs to `/home/jojo/automation/logs/fleet-cliff-report.log`
- Status: ✅ enabled, ✅ active

### CLI flags

```
node fleet-cliff-report.js                    # default: threshold 1.002, send Telegram
node fleet-cliff-report.js --dry              # print only, no send
node fleet-cliff-report.js --force            # ignore dedup
node fleet-cliff-report.js --threshold 1.005  # widen scope
```

## Verified delivery
Test send 2026-05-22 17:52 with `--force --threshold 1.005` returned `sent ✓`
and the message landed in the Telegram channel.

## Outcome
User will receive a Telegram report within 5 min of any fleet position dropping to
HF ≤ 1.002 with debt > $50. Unfireable yield-vault collateral is flagged distinctly
so it doesn't generate false "easy money" alerts. Dedup prevents spam during sustained
near-cliff states.

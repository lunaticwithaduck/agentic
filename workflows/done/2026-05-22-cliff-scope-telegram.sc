---
domain: defi-liquidations
source_task: 2026-05-22-cliff-scope-telegram.md
date: 2026-05-22
keywords: [cliff-scope-alert, telegram-monitoring, systemd-timer, unfireable-market-registry, dedup-state-json, manual-env-parsing]
---

## Extracted Knowledge

### Cliff-scope alert pattern for liquidation fleets
Beyond armed/fire signals from the per-chain executor, a fleet-wide periodic scanner
catches "about to fire" positions across chains and sends a single consolidated
Telegram with rough probability + expected profit. Reduces the user's need to manually
scan each chain's data files.

### Linear chance% heuristic gives sensible rankings
For "how likely is this to fire soon" estimation:
```
chance% = max(0, min(100, 100 - (HF - 1) × 10000))
```
- HF 1.0001 → 99%
- HF 1.0010 → 90%
- HF 1.0050 → 50%
- HF 1.0100 → 0%

It's a relative ranker, not a real probability. Don't overstate accuracy in the report.

### Expected-profit estimate via per-chain LIF averages
```
profitUsd = debt × lifAvg × 0.70    # 30% haircut for slippage / pool depth
```
Per-chain averages (rough):
- Aave V3 (Tydro, HyperLend, HypurrFi): 7.5–8%
- Morpho Blue forks (Felix, Bend, Monad): 12%
- Silo V2 (Sonic): 6.5%

Apply a haircut (0.7) because real fires usually have slippage and partial-fill caps.

### Unfireable-market registry prevents false "easy money" alerts
Maintain a hardcoded set of marketIds known to have NO atomic DEX route (yield-vault
collateral with non-atomic redemption, or zero AMM liquidity). Report them with a
distinct "⚠️ no DEX route · unfireable" label rather than estimating profit. Otherwise
the user gets spammed with "$X profit on success" for positions that physically can't
be liquidated.

Example for Monad:
```
const UNFIREABLE_MARKETS = new Set([
  '0x67c3a8f2…',  // YZM — $4 total DEX liquidity
  '0x300a4c4f…',  // aHYPER — withdrawal-queue vault
]);
```

### Manual .env parsing when script lives outside node_modules trees
Scripts at `/home/jojo/automation/foo.js` can't `require('dotenv')` because the chain
dirs (which DO have node_modules) are siblings, not parents. Two options:
1. `require('/path/to/chain/node_modules/dotenv')` — fragile, couples to specific chain
2. Inline the parser (~5 lines):
   ```js
   try {
     const envText = fs.readFileSync(envPath, 'utf8');
     for (const line of envText.split('\n')) {
       const m = line.match(/^([A-Z_][A-Z0-9_]*)\s*=\s*(.*)$/);
       if (m && !process.env[m[1]]) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
     }
   } catch (e) { /* warning */ }
   ```
Option 2 is portable and has zero dependencies.

### systemd user timer for periodic monitoring jobs
```ini
# foo.timer
[Timer]
OnBootSec=2min
OnUnitActiveSec=5min
Persistent=true
```
- `OnBootSec` delays first run after boot (avoid race during boot sequence)
- `OnUnitActiveSec` re-fires N after the previous run STARTED (not finished — relevant for long-running jobs)
- `Persistent=true` catches up missed runs after suspend/resume

`systemctl --user enable --now foo.timer` enables + starts in one shot.

### Dedup via JSON state file
Store `{ "chain:borrower": lastAlertedAtMs }`. On each run, evict entries older than
N minutes, filter incoming matches against the remaining set. Atomic via simple
`writeFileSync` after the alert lands.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Fleet cliff-scope Telegram reporter pattern
For multi-chain liquidation bots, add a periodic scanner that scans every chain's
healthfactors.json for positions near HF=1.0 and sends a consolidated alert. Three
components: (1) per-chain HF extractor with kind-specific decoding, (2) unfireable
market registry, (3) systemd timer at 5-min cadence with dedup state.

### Manual .env for scripts outside node_modules
Don't pull `dotenv` for one-off scripts living above all node_modules trees. Inline
the ~5-line parser instead. Zero dependencies, portable.

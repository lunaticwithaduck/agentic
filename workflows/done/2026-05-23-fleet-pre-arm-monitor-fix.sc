---
domain: defi-liquidations
source_task: 2026-05-23-fleet-pre-arm-monitor-fix.md
date: 2026-05-23
keywords: [pre-arm-monitor, arm-zone-write, cliff-edge-oscillation, armed-file-semantics, fire-vs-arm, multi-chain-audit, monitor-vs-executor-interface]
---

## Extracted Knowledge

### Armed file = pre-sign trigger, NOT fire trigger
The common mistake (made on 5 of 7 chains in our fleet) is: monitor writes armed file
ONLY when position is strictly underwater (HF<1.0 / !solvent). This treats the armed
file as a "fire now" signal. Wrong.

Correct semantics:
- **armed file = "executor: pay attention, this position is at the edge"**
- **fire decision = made by executor per-tick by calling maxLiquidation**
- Monitor writes armed file when HF enters arm zone (e.g., HF<1.02)
- Executor reads armed file, polls maxLiquidation each block, fires when amounts > 0

This decouples monitor sweep cadence from fire timing. A 60s-sweep monitor can still
capture sub-second cliff events because the executor (polling every block) does the
actual fire decision.

### Cliff-edge oscillation = lost opportunities under naïve schemes
A position at LTV = LT exactly (ratio 1.000 / HF 1.000) is technically solvent per
protocol's `isSolvent` view. But it can briefly cross underwater between monitor
sweeps due to interest accrual or oracle price movement. If the monitor only writes
armed file on `!solvent`, every brief crossing produces:
- Monitor sweep at t=0: solvent → no armed file
- Position crosses underwater at t=10s
- Position rebounds to solvent at t=30s
- Monitor sweep at t=60s: solvent → no armed file
- Result: 20-second fire window missed, executor never even tried

Real example: Sonic 0xbf5b0bc2 on 2026-05-22, $4M wS debt, 13 minutes at ratio 1.000,
self-rescued. Zero armed files written by sonic-silo/monitor.js. Same bug existed on
hyperlend, hypurrfi, bend.

### The fire-flag pattern in armed payload
When writing armed file in arm zone (HF<1.02 but >=1.0), tag the payload with
`fire: false`. When in fire zone (HF<1.0), tag with `fire: true`. Executor can
optimize:
- `fire: false` → presign, hold, poll maxLiquidation each block
- `fire: true` → broadcast immediately, no need to wait

```js
const isFire = BigInt(r.healthFactor) < FIRE_THRESHOLD_HF;
const armedFile = path.join(armedDir, key + '.json');
fs.writeFileSync(tmp, JSON.stringify({ ...r, fire: isFire }, null, 2));
fs.renameSync(tmp, armedFile);
```

### Fleet-wide audit checklist for arm-vs-fire semantics
For any new chain monitor, verify:
1. ✓ Armed file writes when HF in `(FIRE_THRESHOLD, ARM_THRESHOLD)` band, not only at FIRE
2. ✓ ARM_THRESHOLD is set above FIRE_THRESHOLD (typically FIRE+2%)
3. ✓ Payload includes `fire: bool` flag so executor knows whether to pre-sign or broadcast
4. ✓ Telegram alert only on fire transitions, not pre-arm (avoids noise on oscillating positions)
5. ✓ Executor handles "armed but maxLiquidation returned 0" gracefully (log + wait)

### Telegram noise control with pre-arm semantic
Pre-arming generates more armed file writes. If you also Telegram on every armed
write, the user gets spam during cliff-edge oscillation. Convention:
- Telegram only when `fire: true` first becomes true (transition from arm → fire)
- Or only on actual successful broadcast
- Pre-arm writes silent (logged but not Telegrammed)

## Failure Modes Observed

### Yesterday's $4M Sonic cliff lost to log-only-at-arm semantic
The monitor's `else if (r.ltvRatio >= ARM_THRESHOLD_LTV_RATIO)` branch only logged
"🎯 arm" and updated an in-memory dedup cache. No file write. Executor never knew
the position existed in the arm zone. Estimated cost: $1.8-4.7k per cliff event.

Same bug present on hyperlend, hypurrfi, bend — but those markets either had no
cliff event during the affected window OR the chains have more reliable monitor
sweep cadence (sub-second WSS) that masked the issue. Bug existed on all 4 anyway.

### Audit silently passed all 4 chains because no fires were attempted
The 7-chain fleet was in production for weeks with this bug. It wasn't caught
because audit was on the FIRE PATH (does liquidationCall succeed when armed), not
the ARM-FILE-EXISTS PATH (does the file get written when it should). Lesson:
when designing a multi-stage pipeline (indexer → monitor → executor → contract),
test the HANDOFF between each stage in isolation, not just the end-to-end success
of fires that DO happen.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (extends monitor architecture section):

### Armed file is the pre-sign trigger, not the fire trigger
Monitor writes armed file in ARM ZONE (HF below ARM_THRESHOLD, e.g., 1.02), not
only when underwater. Executor reads armed files, polls maxLiquidation each tick,
fires when amounts > 0. This decouples sweep cadence from fire timing.

### Tag armed payload with `fire: bool` flag
Allows executor to know whether to pre-sign-and-wait (fire=false) or broadcast
immediately (fire=true). Critical optimization for sub-second fire windows.

### Audit arm-vs-fire semantics on every monitor in the fleet
For each chain, verify the armed-file write happens in the arm zone (not just
fire zone). Easy to miss because all the pretty fire-path telemetry works fine
while the pre-arm path silently drops opportunities.

### Telegram only on fire transitions, never on pre-arm writes
Pre-arm writes will be frequent during cliff-edge oscillation. Don't Telegram
each one — it'll spam. Only alert when `fire: true` is reached and executor
actually broadcasts.

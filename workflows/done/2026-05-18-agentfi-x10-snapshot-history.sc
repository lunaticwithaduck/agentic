---
domain: defi-data
source_task: 2026-05-18-agentfi-x10-snapshot-history.md
date: 2026-05-18
keywords: ["snapshot-history", "json-store", "atomic-write", "time-series", "indexer", "7d-delta", "sparkline"]
---

## Extracted Knowledge

### JSON-file-backed time-series store with atomic writes
For an indexer-lite project (hourly cadence, small N, single-writer), a JSON file in `data/snapshots.json` is enough — no Postgres needed for scaffolding. Pattern:

```ts
async function writeStore(store: StoreFile): Promise<void> {
  await ensureDir();
  const target = storePath();
  const tmp = `${target}.tmp-${process.pid}-${Date.now()}`;
  await writeFile(tmp, JSON.stringify(store, null, 2), "utf-8");
  await rename(tmp, target);  // atomic on POSIX
}
```

`rename` is atomic — readers never see a partially-written file. The tmp suffix includes PID + ts to avoid collisions if multiple writers run concurrently (one will lose, but neither corrupts).

Swap-out path to Postgres: replace `readStore`/`writeStore` internals with a pool query; keep the public API (`recordSnapshot`, `getHistory`, `getLatest`) identical so consumers don't change.

### Test-friendly via env-var data dir override
```ts
function dataDir(): string {
  return process.env.SNAPSHOT_DATA_DIR ?? path.join(process.cwd(), "data");
}
```
Tests set `process.env.SNAPSHOT_DATA_DIR` to a unique `mkdtemp()` path per test, then `rm -rf` it on teardown. No real-file pollution, no test interference, no module re-import gymnastics.

### Test data must use realistic timestamps when the store prunes
If the store auto-prunes entries older than N days, test entries with `ts: 1000` (1970) get pruned immediately on write. Use `Date.now()`-relative timestamps in test data:
```ts
const NOW = Date.now();
await recordSnapshot(mk({ ts: NOW - 1000 }));  // ✓
await recordSnapshot(mk({ ts: 1000 }));        // ✗ pruned
```

### Throttle writes at the data-source layer, not the consumer
```ts
const RECORD_THROTTLE_MS = 50 * 60 * 1000; // hourly cadence: <50min = duplicate
const latest = await getLatest(slug);
if (!latest || now - latest.ts > RECORD_THROTTLE_MS) {
  await recordSnapshot(newEntry);
}
```
Consumers (pages, OG routes) don't need to know about throttling. They just call the snapshot resolver and it handles dedup. Server-side calls cap at one disk write per ~hour even if a hot path renders 100×/min.

### Cold-start-safe derived metrics
Every derivation should return a sensible default when history is sparse:
```ts
export function derive7dDelta(history, currentMultiple, nowMs = Date.now()): number {
  const past = findNearest(history, nowMs - 7*DAY, 12*HR);
  if (!past || past.multiple === 0) return 0;  // ← fallback
  return ((currentMultiple - past.multiple) / past.multiple) * 100;
}
```
Same for rate (returns 0) and trail (returns flat array of current value). Means the UI renders the same shape from day 1 — just with mostly-zero values until history accumulates.

### `findNearest` with a tolerance window beats exact-match
For "value from 7 days ago" lookups, hourly snapshots won't land exactly on `now - 168h`. Use a window:
```ts
function findNearest(history, targetMs, windowMs) {
  let best = null, bestDelta = Infinity;
  for (const e of history) {
    const d = Math.abs(e.ts - targetMs);
    if (d <= windowMs && d < bestDelta) { best = e; bestDelta = d; }
  }
  return best;
}
```
±12h window for 7d delta, ±6h for 24h rate — generous enough to find a sample even if the indexer missed a few hours.

### Trail sampling — even spacing across time range
```ts
const N = recent.length;
for (let i = 0; i < 12; i++) {
  const frac = i / 11;
  const idx = Math.min(N - 1, Math.round(frac * (N - 1)));
  out.push(recent[idx].multiple);
}
```
Index-based (not time-based) sampling — if the history has gaps, you still get 12 evenly-distributed points. Simpler than bucket-by-time and visually equivalent for a sparkline.

### CLI script via tsx + cron-friendly exit codes
```ts
async function main() {
  if (!hasKey()) { console.error("..."); process.exit(2); }
  const snap = await getLive();
  if (!snap) { console.error("..."); process.exit(3); }
  // ... print summary, exit 0
}
main().catch((err) => { console.error(err); process.exit(1); });
```
Distinct exit codes per failure mode let cron alerting differentiate "no key configured" (exit 2) from "fetch failed" (exit 3) from "unknown error" (exit 1). Useful when piping into systemd or PagerDuty.

### Beware `*/N` in JSDoc comments
JSDoc parser treats `*/` as comment-close. Writing `*/60 * * * *` (cron expression for "every minute" in a comment) closes the comment mid-text and breaks the build. Either:
- Replace with the explicit equivalent: `0,1,2,...,59 * * * *` (not great)
- Use a different format: `"0 * * * *"` (hourly) or `0 */1 * * * *` (every minute) — wait that has the same problem
- Escape: write "every minute" in prose, or wrap in code-fence: `\`*/60 * * * *\``

Caught a build crash with this in `scripts/snapshot-record.ts` — easy mistake to repeat.

## Proposed Skill Content

Extends `defi-data` (1st entry was X9 — Etherscan v2 + GeckoTerminal). When 3rd entry triggers synthesis, `.claude/skills/defi-data.md` would have:
- Etherscan v2 multichain (from X9)
- GeckoTerminal patterns (from X9)
- Gated live-or-mock resolution (from X9)
- **JSON-file time-series store with atomic writes (this entry)**
- **Cold-start-safe derived metrics with tolerance windows (this entry)**
- **CLI script exit-code conventions for cron (this entry)**
- **`*/` in JSDoc comments breaks the build (this entry)**

(2nd entry in domain — synthesis at 3.)

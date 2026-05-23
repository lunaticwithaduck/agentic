---
name: defi-data
description: On-chain data ingestion patterns — Etherscan v2 multichain, GeckoTerminal, JSON-file time-series stores, tx classification
activation:
  keywords: ["etherscan", "basescan", "geckoterminal", "evm", "on-chain", "token-supply", "tokentx", "txlist", "classifier", "snapshot-history", "atomic-write", "indexer", "7d-delta", "sparkline", "erc20", "defi"]
---

## Purpose

Knowledge for projects that ingest on-chain EVM data (balances, supplies, tx histories, prices) and turn it into derived metrics for a UI. Covers the Etherscan v2 multichain API, GeckoTerminal as a free price source, tx classifier patterns, and lightweight JSON-file time-series storage.

## Etherscan v2 multichain

**One key, all EVM chains.** Etherscan v2 (released 2024) replaces per-chain keys with a single key + `chainid` query param.

```
https://api.etherscan.io/v2/api?chainid=8453&module=...&action=...&apikey=...
```

Common chain IDs:
- `1` Ethereum mainnet · `10` Optimism · `137` Polygon · `8453` Base · `42161` Arbitrum One · `56` BSC · `43114` Avalanche C-Chain

Free tier: **5 req/sec, 100k req/day**. Pro raises both. Get a key at https://etherscan.io/myapikey — works immediately.

### Common endpoints

| Action | Params | Returns |
|---|---|---|
| Token supply | `module=stats&action=tokensupply&contractaddress=...` | Raw bigint string |
| Wallet token balance | `module=account&action=tokenbalance&address=...&contractaddress=...&tag=latest` | Raw bigint string |
| ERC-20 transfers | `module=account&action=tokentx&address=...&sort=desc&page=1&offset=20` | Array of `{blockNumber, timeStamp, hash, from, to, value, tokenSymbol, tokenDecimal, ...}` |
| Native txs | `module=account&action=txlist&address=...&sort=desc&page=1&offset=20` | Array of `{blockNumber, timeStamp, hash, methodId, functionName, input, isError, ...}` |
| Contract ABI | `module=contract&action=getabi&address=...` | ABI JSON string |
| Contract creation | `module=contract&action=getcontractcreation&contractaddresses=...` | `{contractCreator, txHash}[]` |

### Envelope shape — `status: "0"` is ambiguous

```json
{ "status": "0" | "1", "message": "OK" | "...", "result": <any> }
```

`status: "0"` means EITHER "no records found" (valid empty result) OR an actual error (result starts with "Error" or "Invalid"). Don't blindly throw — check `typeof result === "string" && result.startsWith("Error")` first; otherwise treat as empty.

### Exponential backoff

429s arrive above 5 req/sec. Standard pattern:

```ts
for (let attempt = 0; attempt < 3; attempt++) {
  const res = await fetch(url);
  if (res.status === 429 || res.status >= 500) {
    await sleep(250 * Math.pow(2, attempt));
    continue;
  }
  return await res.json();
}
```

250 / 500 / 1000 ms is usually enough.

### Server-only env var hygiene

Use unprefixed `ETHERSCAN_API_KEY`, never `NEXT_PUBLIC_ETHERSCAN_API_KEY`. The prefix exposes the key to client bundles → key leaks to every browser. Server-only wrappers should use unprefixed names exclusively.

## Etherscan v2 — tx classification gotchas

When building an "activity feed" by classifying wallet txs into types (CLAIM / STAKE / LP / SWAP / LOG / MILESTONE):

1. **`txlist` and `tokentx` are not subsets**. ERC-20 activity where the wallet is the *recipient* (rewards contract calling `transfer(wallet, amount)`) shows up in `tokentx` but NOT in `txlist` (because `from` is the contract). Union both lists by `hash` and dedupe; never iterate one alone.

2. **Filter `isError === "1"`**. Etherscan returns failed txs in `txlist` with full method/value data. Without this filter the feed shows phantom swaps/claims that never executed.

3. **`functionName` is the full Solidity signature**, e.g. `"claim(uint256)"`, `"addLiquidity(address,address,uint256,...)"`. Use `\bword\b` regex boundaries — don't `===` match. Suggested ordering for the matcher table:
   ```ts
   const FN_HINTS = [
     { match: /\bclaim\b/i,                                          type: "CLAIM" },
     { match: /\bstake|deposit|bond\b/i,                             type: "STAKE" },
     { match: /\baddLiquidity|mint|modifyPosition|increaseLiquidity\b/i, type: "LP" },
     { match: /\bswap|exactInput|exactOutput\b/i,                    type: "SWAP" },
     { match: /\blog|emit|recordLog\b/i,                             type: "LOG" },
   ];
   ```

4. **Multi-token transfer in same tx = LP add/remove.** The cleanest LP signal isn't the function name (often obscure on aggregators) — it's `new Set(transfers.map(t => t.tokenSymbol)).size >= 2` after grouping transfers by `txHash`.

5. **`tokenDecimal` and `value` are stringified integers** ("18", "6", "1000000000000000000"). Always `Number(tx.tokenDecimal || 18)` before using as a BigInt exponent. Use `BigInt(raw)` to preserve precision, then divide by `10n ** decimals`.

6. **Lowercase before comparing addresses.** Etherscan returns mixed-case (checksum form for some, lowercase for others). `tx.to.toLowerCase() === agentWallet.toLowerCase()` — never compare without normalizing.

### Classifier layering pattern

```ts
function classifyTx(tx, transfers, ctx) {
  // 1. functionName hint (most specific)
  if (tx?.functionName) {
    for (const { match, type } of FN_HINTS) {
      if (match.test(tx.functionName)) {
        const byTransfers = classifyByTransfers(transfers, agentLower);
        return byTransfers
          ? { type, detail: byTransfers.detail }
          : { type, detail: tx.functionName.slice(0, 40) };
      }
    }
  }
  // 2. transfer-shape heuristic
  const byTransfers = classifyByTransfers(transfers, agentLower);
  if (byTransfers) return byTransfers;
  // 3. generic SWAP fallback
  return { type: "SWAP", detail: tx?.functionName?.slice(0, 40) ?? "—" };
}
```

The `detail` string is best-effort built from transfer amounts even when the type comes from the function-name hint — gives richer UX than echoing the signature alone.

### Aggregator: union by hash, sort desc

`Map<hash, EtherscanTokenTx[]>` groups transfers. Walk `txs` first (preferred — has `functionName`), then leftover transfer-only hashes. Sort combined list `desc` by `ts` (where `ts = Number(timeStamp) * 1000` — Etherscan returns seconds-since-epoch as a string). Cap to max.

## GeckoTerminal — free price source

`/api/v2/networks/<chain>/tokens/<addr>` returns `{price_usd, fdv_usd, market_cap_usd, total_supply}`. Free, no key, ~30 req/min.

- Chain slug is lowercase: `base`, `ethereum`, `arbitrum`, `optimism`, `polygon`, `bsc`, `avax`
- Token address must be lowercased
- Returns 404 if no pool data — handle gracefully
- For mcap: prefer `market_cap_usd` (circulating-supply aware) → fall back to `fdv_usd` → compute `total_supply × price_usd`

## Gated live-or-mock resolution

For projects that should work with OR without an API key (dev without, prod with):

```ts
async function resolveSnapshot() {
  if (hasKey()) {
    try {
      const live = await getLive();
      if (live) return live;
    } catch (err) {
      console.warn("[snapshot] live failed, falling back:", err);
    }
  }
  return MOCK_DATA;
}
```

Lets you ship app + tests + CI without a key, then flip to live by adding it to `.env.local`. No code changes.

## JSON-file time-series store

For indexer-lite projects (hourly cadence, small N, single-writer), a JSON file is enough — no Postgres needed for scaffolding.

### Atomic writes via tmp + rename

```ts
async function writeStore(store: StoreFile): Promise<void> {
  await ensureDir();
  const target = storePath();
  const tmp = `${target}.tmp-${process.pid}-${Date.now()}`;
  await writeFile(tmp, JSON.stringify(store, null, 2), "utf-8");
  await rename(tmp, target);  // atomic on POSIX
}
```

`rename` is atomic — readers never see a partial file. Tmp suffix includes PID + ts to avoid concurrent-writer collisions (one will lose, neither corrupts).

**Swap-out path to Postgres**: replace `readStore`/`writeStore` internals with a pool query; keep public API (`recordSnapshot`, `getHistory`, `getLatest`) identical so consumers don't change.

### Env-var data dir override for tests

```ts
function dataDir(): string {
  return process.env.SNAPSHOT_DATA_DIR ?? path.join(process.cwd(), "data");
}
```

Tests set `SNAPSHOT_DATA_DIR` to a unique `mkdtemp()` path per test, then `rm -rf` on teardown. No real-file pollution.

### Test data must use realistic timestamps when store prunes

If the store auto-prunes entries older than N days, test entries with `ts: 1000` (1970) get pruned immediately:

```ts
const NOW = Date.now();
await recordSnapshot(mk({ ts: NOW - 1000 }));  // ✓
await recordSnapshot(mk({ ts: 1000 }));        // ✗ pruned
```

### Throttle at the data layer, not consumers

```ts
const RECORD_THROTTLE_MS = 50 * 60 * 1000; // hourly cadence: <50min = duplicate
const latest = await getLatest(slug);
if (!latest || now - latest.ts > RECORD_THROTTLE_MS) {
  await recordSnapshot(newEntry);
}
```

Consumers (pages, OG routes) don't know about throttling. They call the snapshot resolver and it handles dedup. Hot path renders 100×/min → still one disk write/hour.

## Derived metrics — cold-start-safe

Every derivation returns a sensible default when history is sparse:

```ts
export function derive7dDelta(history, currentMultiple, nowMs = Date.now()): number {
  const past = findNearest(history, nowMs - 7*DAY, 12*HR);
  if (!past || past.multiple === 0) return 0;  // ← fallback
  return ((currentMultiple - past.multiple) / past.multiple) * 100;
}
```

Same for rate (returns 0) and trail (returns flat array of current value). The UI renders the same shape from day 1, just with mostly-zero values until history accumulates.

### `findNearest` with tolerance window beats exact-match

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

±12h for 7d delta, ±6h for 24h rate — generous enough to find a sample even if the indexer missed a few hours.

### Trail sampling — index-based even spacing

```ts
const N = recent.length;
for (let i = 0; i < 12; i++) {
  const frac = i / 11;
  const idx = Math.min(N - 1, Math.round(frac * (N - 1)));
  out.push(recent[idx].multiple);
}
```

Index-based (not time-based) — if history has gaps, you still get 12 evenly-distributed points. Simpler than bucket-by-time and visually equivalent for a sparkline.

## CLI scripts for cron — exit-code conventions

```ts
async function main() {
  if (!hasKey())  { console.error("..."); process.exit(2); }
  const snap = await getLive();
  if (!snap)      { console.error("..."); process.exit(3); }
  // ... print summary, exit 0
}
main().catch((err) => { console.error(err); process.exit(1); });
```

Distinct exit codes per failure mode let cron alerting differentiate "no key configured" (2) from "fetch failed" (3) from "unknown error" (1). Useful when piping into systemd / PagerDuty / Vercel cron.

## Failure Modes Observed

### `*/N` in JSDoc comments breaks the build

JSDoc parser treats `*/` as comment-close. Writing `*/60 * * * *` (cron expression) inside `/** ... */` closes the comment mid-text and breaks TypeScript parsing. Workarounds:
- Write the cron in prose (`"every minute"`) instead of as a literal
- Wrap in inline code-fence inside the comment
- Move the example to a string literal outside the docblock

Caught a build crash with this in `scripts/snapshot-record.ts`.

### Async-server-component cascade in Next.js App Router

Replacing a sync data source (mock const) with an async one (live fetch) means every consumer must become async too. Server Components support this natively — just `async function` and `await`. Affected surfaces in App Router:
- `app/**/page.tsx` — usually already async (Next 16 params are Promises)
- `app/**/route.tsx` — handlers always async
- `components/<X>.tsx` that fetch data — make them `async function`; Next renders them as Server Components

Type-only circular imports between data and chain layers are fine when both sides use `import type { ... }` — TypeScript strips them at compile.

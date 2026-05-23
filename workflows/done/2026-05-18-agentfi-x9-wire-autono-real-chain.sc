---
domain: defi-data
source_task: 2026-05-18-agentfi-x9-wire-autono-real-chain.md
date: 2026-05-18
keywords: ["etherscan", "etherscan-v2", "basescan", "geckoterminal", "evm", "on-chain", "token-supply"]
---

## Extracted Knowledge

### Etherscan v2 multichain: one key, all EVM chains
Etherscan released v2 of the API in 2024. **Single key works for every supported EVM chain via the `chainid` query param.** No more separate Basescan / Arbiscan / Optimistic-Etherscan keys.

```
https://api.etherscan.io/v2/api?chainid=8453&module=...&action=...&apikey=...
```

Base chain ID is **8453**. Other common chainids:
- 1 = Ethereum mainnet
- 10 = Optimism
- 137 = Polygon
- 8453 = Base
- 42161 = Arbitrum One
- 56 = BSC
- 43114 = Avalanche C-Chain

Free-tier limits: **5 req/sec, 100k req/day**. Enough for an hourly indexer per agent. Pro tier raises both.

Get a key at https://etherscan.io/myapikey — works immediately, no waiting.

### Common Etherscan v2 endpoints (Base)

| Action | Module + Action | Returns |
|---|---|---|
| Token total supply | `module=stats&action=tokensupply&contractaddress=...` | Raw bigint string |
| Wallet token balance | `module=account&action=tokenbalance&address=...&contractaddress=...&tag=latest` | Raw bigint string |
| ERC-20 transfers | `module=account&action=tokentx&address=...&sort=desc&page=1&offset=20` | Array of `{blockNumber, timeStamp, hash, from, to, value, tokenSymbol, tokenDecimal, ...}` |
| Native txs | `module=account&action=txlist&address=...&sort=desc&page=1&offset=20` | Array of `{blockNumber, timeStamp, hash, methodId, functionName, input, isError, ...}` |
| Contract ABI | `module=contract&action=getabi&address=...` | ABI JSON string |
| Contract creation | `module=contract&action=getcontractcreation&contractaddresses=...` | Array of `{contractCreator, txHash}` |

### Envelope shape and `status: "0"` handling
All v2 responses come in this envelope:
```json
{ "status": "0" | "1", "message": "OK" | "...", "result": <any> }
```

`status: "0"` means EITHER:
- "No records found" (valid response — empty result array)
- An actual error (result is a string starting with "Error" or "Invalid")

Don't blindly throw on `status: "0"`. Check `typeof result === "string" && result.startsWith("Error")` first; otherwise treat as empty.

### Exponential backoff
Returns 429 when rate-limited (above 5 req/sec). Standard pattern:
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
250ms / 500ms / 1000ms is usually enough.

### GeckoTerminal as the free price source
GeckoTerminal exposes `/api/v2/networks/<chain>/tokens/<addr>` — returns `{price_usd, fdv_usd, market_cap_usd, total_supply}` for any traded token. Free tier: ~30 req/min (no key).

Path uses lowercase chain slug:
- `base`, `ethereum`, `arbitrum`, `optimism`, `polygon`, `bsc`, `avax`

Token address should be lowercased. Returns 404 if no pool data — handle gracefully.

For mcap specifically: prefer `market_cap_usd` if populated (circulating-supply aware), fall back to `fdv_usd` or compute `total_supply × price_usd`.

### Pattern: gated live-or-mock resolution
For projects that should work both with and without an API key (dev without key, prod with key):
```ts
import { hasKey } from "./chain/etherscan";

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
Lets you ship the entire app + tests + CI without a key, and flip to live by just adding the key to `.env.local`. No code changes.

### Async-server-component cascade
Replacing a sync data source with an async one in Next.js App Router means every consumer becomes async. Server Components support this natively — just `async function` and `await`. Affected surfaces:
- `app/**/page.tsx` — usually already async (Next 16 params are Promises)
- `app/**/route.tsx` — handlers are always async
- `components/<X>.tsx` that fetch data — make them `async function`; Next renders them as Server Components

Type-only circular imports between data layer and chain layer are fine when both sides use `import type { ... }` — TypeScript strips them at compile.

### `process.env.NEXT_PUBLIC_X` vs `process.env.X`
For Etherscan keys: **use unprefixed `ETHERSCAN_API_KEY`**. The `NEXT_PUBLIC_` prefix exposes the variable to client bundles, which would leak the key to every browser. Server-only fetch wrappers should use unprefixed names exclusively.

## Proposed Skill Content

A future `.claude/skills/defi-data.md` would consolidate Etherscan v2, GeckoTerminal, and similar APIs into one skill. Topics:
- Etherscan v2 multichain (chainid table, common endpoints, envelope shape, backoff)
- GeckoTerminal patterns (chain slugs, address lowercasing, mcap precedence)
- Gated live-or-mock pattern for projects that ship without keys
- Async-server-component cascade for replacing sync data sources
- Server-only env var hygiene (no `NEXT_PUBLIC_` for keys)

(1st entry — synthesis threshold is 3. Future on-chain data tasks should add to this domain rather than creating sub-domain names.)

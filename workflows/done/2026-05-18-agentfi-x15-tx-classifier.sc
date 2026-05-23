---
domain: defi-data
source_task: 2026-05-18-agentfi-x15-tx-classifier.md
date: 2026-05-18
keywords: ["etherscan", "txlist", "tokentx", "classifier", "evm", "erc20"]
---

## Extracted Knowledge

### Etherscan v2 tx classification gotchas

1. **`txlist` and `tokentx` are not subsets of each other**. ERC-20 token activity where the agent wallet is the *recipient* (e.g. a rewards contract calling `transfer(wallet, amount)`) appears in `tokentx` but NOT in `txlist` (because `from` is the contract, not the wallet). To build a complete activity feed, union the hashes from both and dedupe — never just iterate one list.

2. **Filter `isError === "1"`** before classifying. Etherscan returns failed txs in `txlist` with their full method/value data; if you don't skip them, the feed shows phantom swaps/claims that never executed.

3. **`functionName` is the full Solidity signature**, e.g. `"claim(uint256)"`, `"addLiquidity(address,address,uint256,uint256,uint256,uint256,address,uint256)"`. Use `\bword\b` regex boundaries — don't `===` match. Method ordering for the regex table: `claim` / `stake|deposit|bond` / `addLiquidity|mint|modifyPosition|increaseLiquidity` / `swap|exactInput|exactOutput` / `log|emit|recordLog`.

4. **Multi-token transfer in same tx = LP add/remove**. The cleanest LP signal isn't the function name (which can be obscure on aggregators); it's `new Set(transfers.map(t => t.tokenSymbol)).size >= 2` grouped by `txHash`.

5. **`tokenDecimal` is a stringified integer** ("18", "6"). Always `Number(tx.tokenDecimal || 18)` before using as `BigInt` exponent. Same for `value` — it's a stringified raw integer, use `BigInt(raw)` to preserve precision, then divide by `10n ** decimals`.

6. **Direction is decided by lowercase compare**: `tx.to.toLowerCase() === agentWallet.toLowerCase()`. Etherscan returns mixed-case addresses (checksum form for some, lowercase for others) — never compare without normalizing.

### Classifier layering pattern

```ts
function classifyTx(tx, transfers, ctx) {
  // 1. functionName hint (most specific)
  if (tx?.functionName) {
    for (const { match, type } of FN_HINTS) {
      if (match.test(tx.functionName)) {
        const byTransfers = classifyByTransfers(transfers, agentLower);
        return byTransfers ? { type, detail: byTransfers.detail } : { type, detail: tx.functionName.slice(0, 40) };
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

The `detail` string is best-effort from transfer amounts even when the type comes from the function-name hint — gives a richer UX than just echoing the signature.

### Aggregator: union by hash, sort desc

Group transfers into `Map<hash, EtherscanTokenTx[]>`. Walk `txs` first (preferred — has `functionName`). Then walk leftover transfer-only hashes. Sort the combined list `desc` by `ts` (which is `Number(timeStamp) * 1000` — Etherscan returns seconds-since-epoch as a string). Cap to `max`.

### Test fixture pattern (vitest)

Builder helpers `tx(over)` and `transfer(over)` with sensible defaults make per-case tests one-line setups:
```ts
const t = tx({ functionName: "claim(uint256)", to: WALLET });
const transfers = [transfer({ to: WALLET, value: "5000000000000000000" })];
```
This keeps the fixture noise out of the assertion. Each test then asserts only on `result.type` and (loosely) on `result.detail`.

## Proposed Skill Content

The existing `.claude/skills/defi-data.md` covers GeckoTerminal + DIEM-price patterns. Add a section there titled **Etherscan v2 chain activity classification** with:

- The two-list union (txlist + tokentx) rule
- `isError` filter requirement
- `functionName` regex matching with word boundaries (sample table)
- Multi-symbol-in-tx = LP heuristic
- `tokenDecimal`/`value` BigInt handling pattern
- Lowercase address compare reminder
- Tests: builder-pattern fixtures for the two Etherscan shapes

---
domain: defi-liquidations
source_task: 2026-05-22-unify-revert-decoder.md
date: 2026-05-22
keywords: [revert-decoder, error-string-abi, drpc-eth-call, multi-chain-decoder, decodeErrorString, hex-08c379a0]
---

## Extracted Knowledge

### Error(string) reverts need ABI-decode, not regex over message
A decoder that classifies by selector first will MIS-CLASSIFY `Error(string)` reverts as
`unknown-selector` and flag them actionable. The Error(string) selector `0x08c379a0` is
the wrapper — the actual classification needs the decoded string.

ABI layout of `0x08c379a0` prefixed data:
```
0x08c379a0
<32 bytes: offset (always 0x20)>
<32 bytes: string length>
<padded N×32 bytes: UTF-8 string bytes>
```

JS decode:
```js
function decodeErrorString(data) {
  if (!data || !data.startsWith('0x08c379a0') || data.length < 138) return null;
  const len = parseInt(data.slice(74, 138), 16);
  if (!Number.isFinite(len) || len === 0 || len > 1024) return null;
  return Buffer.from(data.slice(138, 138 + len*2), 'hex').toString('utf8');
}
```

Without this, HypurrFi `Error("45")`, Felix/Bend/Monad `Error("position is healthy")`,
and any future Morpho-fork string-Errors are flagged as Telegram-worthy anomalies on
every healthy-position pre-flight. Decoder must try Error(string) ABI-decode FIRST,
selector lookup SECOND.

### One shared decoder for the whole fleet
Don't write per-chain revert classification. Pre-load into `lib/revert-decoder.js`:
- All Aave V3 modern custom-error selectors (`0x930bb771 HealthFactorNotBelowThreshold()`
  and friends)
- All Aave V3 legacy numeric codes ("35", "45")
- All Morpho fork string messages ("position is healthy")
- All Silo V2 custom errors (`0x5e26aa2a UserIsSolvent()`)
- Your liquidator contract's custom errors (`NoProfit`, `SwapFailed`, etc.)

Each entry has `{ name, kind, expected }`. `expected: true` means healthy-position
silent skip; `false` means actionable anomaly. Production executor's pre-flight reads
this flag to decide whether to Telegram-alert or silent-skip.

### dRPC's Monad endpoint blocks ALL eth_call methods
Confirmed via direct curl 2026-05-22:
- Every eth_call (any gas, no gas, any data) returns:
  `{"error":{"message":"user-specified gas exceeds provider limit","code":-32603}}`
- Other methods (eth_blockNumber, eth_chainId, eth_sendRawTransaction) work
- The error message is misleading — it fires even with NO gas field

Practical implication: dRPC Monad is fine for **broadcast** but NOT for sims/dryruns/
preflights. Production executor should split: reads on QuickNode, writes on dRPC.

### Restart executors after decoder updates
Node caches `require()` per-process. Long-running executors keep the OLD decoder in
memory even after the source file is updated. After patching shared infra, you must
`systemctl --user restart <chain>-executor.service` for every chain that imports it.
Confirm with `tail logs/executor.log | grep "mode:"` to see fresh startup line.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Cross-chain revert decoder must handle all 4 encodings + ABI-decode Error(string)
For a fleet that spans Aave V3 modern (Tydro, HyperLend), Aave V3 legacy (HypurrFi),
Morpho forks (Felix, Bend, Monad), and Silo V2 (Sonic), the decoder must:
1. Try `decodeErrorString(data)` BEFORE selector lookup. The `0x08c379a0` selector is a
   wrapper — the actual semantics are in the embedded string.
2. Pre-load string-table entries for numeric codes AND prose ("position is healthy")
3. Pre-load selector-table entries for all four protocol families' custom errors
4. Return `{ expected: true|false }` so executors uniformly decide silent-skip vs alert

### dRPC: write-only for Monad, not for eth_call sims
Paid dRPC Monad endpoint blocks every eth_call regardless of gas. Use QuickNode/public
RPC for reads (rate-limited) and dRPC for broadcasts (no rate limit). Don't try to
consolidate.

### Reload shared infra after edits
Edits to `lib/*.js` don't take effect in running executors until systemd restart. After
patching a shared module, `systemctl --user restart` every executor that imports it.

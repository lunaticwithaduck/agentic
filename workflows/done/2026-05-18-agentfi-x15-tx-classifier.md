---
title: AgentFi X15 — Proper TX classifier (lib/actions-feed.ts)
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Replace the heuristic `classifyTransfer` in `lib/chain/autono.ts` with a proper classifier that cross-references the regular tx list with token transfers, using `methodId` / `functionName` from the tx + `tokenSymbol` + counterparty as signals. Same input → same `ActionEvent` shape; better recall + less false-positives.

## Approach
- Pull `getTxList` + `getTokenTransfers` for the wallet in parallel
- Group transfers by `txHash`
- For each tx: methodId or functionName name often hints (`claim(...)`, `addLiquidity(...)`, `swap(...)`)
- Combined heuristic order: explicit method match → token-symbol pattern → fallback to SWAP
- Detect MILESTONE separately (caller-tagged or via threshold-cross logic in a future task — leave as plumb-through for now)

## Files
- `lib/actions-feed.ts` — `classifyTx(tx, transfers, ctx) → { type, detail }`; `buildActionEvents(txList, transfers, wallet) → ActionEvent[]`
- `lib/chain/autono.ts` — use `buildActionEvents()` instead of inline `classifyTransfer`
- `lib/__tests__/actions-feed.test.ts` — covers each action type + ambiguity fallback

## Steps
- [x] `lib/actions-feed.ts` with the classifier + `buildActionEvents` aggregator
- [x] Refactor `chain/autono.ts` to use the new function (also `getTxList` call added)
- [x] Vitest tests: explicit claim, lp add, swap, stake, ambiguous → SWAP fallback
- [x] Build + test + Playwright

## Outcome

Completed on 2026-05-18. Shipped `lib/actions-feed.ts` with two exports: `classifyTx(tx, transfers, ctx)` and `buildActionEvents(txs, transfers, ctx, max=4)`. Classifier uses a FN_HINTS regex table over `tx.functionName` first (claim / stake / addLiquidity / swap / log), then falls through to a transfer-shape heuristic — multi-symbol same-tx → LP, single-token by symbol+direction → CLAIM/STAKE/LP/SWAP. Errored txs (`isError === "1"`) are skipped. `buildActionEvents` groups by txHash and unions with transfer-only entries (tx initiated by a contract, not the wallet) so we don't miss claim-from-rewards-contract activity. `lib/chain/autono.ts` now fetches `getTxList` in the same `Promise.all` and delegates to `buildActionEvents`; dead inline `classifyTransfer` removed.

Tests: 13 new cases in `lib/__tests__/actions-feed.test.ts` covering each action type + the fallthrough paths + case-insensitive wallet compare. Suite: 137/137 vitest, build clean, Playwright 17/17.

**Skill candidate evaluation:**
- Technologies/frameworks touched in this task: Etherscan v2 tx/tokentx endpoints, TypeScript classifier composition for EVM chain activity, vitest fixture-builder pattern
- Domain-specific knowledge involved: (a) `functionName` strings from Etherscan are signatures like `"claim(uint256)"` — regex with `\b` word-boundaries works against them; (b) multi-token transfers in the same tx are the cleanest LP add/remove signal — symbol heuristic alone misclassifies; (c) some on-chain activity (rewards/claims from a contract) shows up in `tokentx` but not in `txlist` because `from` is the contract, not the wallet — must union by hash, not subset; (d) `isError === "1"` must be filtered or the feed will show failed swaps; (e) `tokenDecimal` field is a stringified int — must `Number()` it before exponent math.
- Verdict: GENERATE
- Reason: Domain-specific Etherscan classification gotchas (txlist vs tokentx union, isError filtering, decimal handling) that future chain-data tasks will hit.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x15-tx-classifier.md`.

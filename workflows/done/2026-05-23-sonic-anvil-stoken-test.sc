---
domain: defi-liquidations
source_task: 2026-05-23-sonic-anvil-stoken-test.md
date: 2026-05-23
keywords: [anvil-fork, hand-typed-selector-bug, silo-v2-getliquidity, stoken-required-partial-fire, partial-liquidation-within-liquidity, anvil-setcode-proportionality, foundry-cast-trace]
---

## Extracted Knowledge

### ALWAYS use ABI-derived selectors, never hand-typed 4-byte hex
The most expensive mistake from this investigation: I used `0x0fc63d10` as the selector for `getLiquidity()` based on memory/guess. Wrong. Real selector is `0xc7b9d530` (per `cast sig "getLiquidity()"`).

The wrong selector hit some OTHER function on the silo contract that returned 0 every time, leading to "silo has zero liquidity" verdict. The silo actually has $153M of wS liquidity.

**Rule:** for any custom contract probe, use one of:
- `cast call --rpc-url RPC ADDR "functionName()(returnType)"` (foundry)
- `ethers.id("functionName()").slice(0, 10)` (Node)
- `web3.utils.keccak256("functionName()").slice(0, 10)` (Web3)

Never type a 4-byte selector by hand or copy from another contract — it's a 1-in-4-billion accident waiting to happen.

### Silo V2 sTokenRequired flag: "amount exceeds liquidity", not "liquidity is zero"
`SiloLens.maxLiquidation(silo, hookReceiver, borrower)` returns `(collateralToLiquidate, debtToRepay, sTokenRequired, fullLiquidation)`.

`sTokenRequired = true` means: **at the maxLiquidation amount, the collateral silo doesn't have enough underlying to give you in tokens, so you'd receive shares (sTokens) instead.**

It does NOT mean the silo is at zero liquidity. It means the seize amount EXCEEDS the silo's current liquidity. For a PARTIAL liquidation capped at `silo.getLiquidity()`, sTokenRequired flips to false and the liquidator receives underlying tokens.

### Partial-liquidation-within-liquidity pattern
For Silo V2 markets where maxLiquidation returns sTokenRequired=true:
```js
const fullColl = collateralToLiquidate;
const fullDebt = debtToRepay;
const liq = await silo1.getLiquidity();  // available withdrawable
if (liq === 0n) return null;  // truly zero, can't partial
// Scale down: partial seize fits in liquidity with margin
const partialColl = liq * 95n / 100n;  // 5% safety margin
const partialDebt = (partialColl * fullDebt) / fullColl;
// Now call liquidationCall with maxDebtToCover=partialDebt; sTokenRequired will be false
```

This unlocks fires that our v1 executor was wrongly skipping. Profit per fire = LIF × partialColl. For 6.5% LIF on $70k partial seize = ~$4.3k per fire.

### anvil_setCode constant-return stubs break proportional oracle math
Standard mock oracle bytecode: `PUSH32 value PUSH1 0 MSTORE PUSH1 0x20 PUSH1 0 RETURN` — returns the same constant for any input.

This works for SIMPLE solvency checks ("is oracle value < threshold") but BREAKS when the protocol relies on proportional scaling. Silo V2's partial liquidation flow re-checks solvency using `oracle.quote(amount, token)` expecting `amount × pricePerToken`. A constant-return stub gives the same value for amount=1 and amount=1e30 → math breaks → revert (we saw `LiquidationNotAllowed`).

**Fix:** write a PROPORTIONAL stub that does `return amount × constant_factor / 1e18`. Requires MULDIV bytecode (~50 bytes more). Or use anvil_setStorageAt to manipulate the oracle's stored price slot if you can find it.

### `cast trace` is the right tool to diagnose deep custom-error reverts
For Silo V2 (and any modular protocol with proxy + lib calls + custom errors), `cast call --trace` shows the full call stack with each delegatecall and the exact frame where the revert originates. Saved hours vs trying to interpret bare custom-error selectors.

### Foundry anvil for liquidation smoketests on Sonic — quick setup pattern
```bash
nohup anvil --fork-url https://rpc.soniclabs.com --port 8545 --silent --auto-impersonate \
  > anvil.log 2>&1 &
# anvil block matches mainnet within ~1 block
# Test wallet: anvil-default account 0 = 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
# To fund with wS: anvil_setBalance native + call wS.deposit() {value: amount}
# To fund with USDC.e: anvil_impersonateAccount on a known whale, transfer
```

The `--auto-impersonate` flag lets `eth_sendTransaction` from any address work without explicit impersonateAccount calls — convenient for prototyping.

## Failure Modes Observed

### Hand-typed selector returned 0 for ANY input — masquerading as "feature off"
The single root cause for an entire day of "structurally blocked" investigation: a wrong selector returning 0. Lesson: when "feature seems disabled / value is zero / contract returns nothing," **first verify your selector matches the function name via cast/ethers**. Don't write multi-thousand-word analyses on top of a 4-byte typo.

### "Structural verdict" verified by 3 external consultants was wrong because they all reasoned from my wrong premise
The hooks-from-other-LLM consultants validated my (wrong) "getLiquidity=0" premise and reasoned forward from there to the same wrong conclusion. None of them re-derived the live state from chain — they trusted the user's framing. **Always re-probe primitive facts on-chain before trusting any chain of reasoning.** External validation is worth less than 10 seconds of `cast call` against the actual contract.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Use ABI-derived selectors, never hand-typed
For any contract probe, derive the selector from the function signature via cast/ethers/web3. A hand-typed selector hits a 1-in-4-billion accidental match returning misleading values.

### Silo V2 sTokenRequired = "amount exceeds liquidity", NOT "no liquidity"
A partial liquidation capped at `silo.getLiquidity()` flips sTokenRequired to false and produces atomic underlying receipt. The blanket-skip-on-sTokenRequired pattern in v1 executors leaves real partial fires unclaimed.

### Partial-liquidation pattern for Silo V2 unfireable markets
Read silo.getLiquidity(), scale debt-to-cover proportionally, fire with receiveSToken=false. Profit = LIF × partial_seize (capped by liquidity, not by borrower's total collateral).

### Proportional oracle stubs (not constant-return) for anvil_setCode
Constant-return stubs break proportional protocol math (e.g. Silo V2's solvency re-check). Use a MULDIV-based stub if you need to crash an oracle proportionally.

### cast trace is the right tool for custom-error revert diagnosis
For modular protocols with proxies + libs + custom errors, `cast call --trace` shows the full call stack and the exact frame where the revert originates. First tool to reach for when you see a bare 4-byte custom error.

---
title: Flash loan opportunity research — Week 1
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Validate (or kill) three flash-loan strategies before committing dev time:
1. NFTfi mainnet liquidations (Blend, NFTfi, Arcade) — phase A candidate
2. New-chain lending liquidations (Sonic, HyperEVM, Berachain) — phase B candidate
3. DEX-DEX arb on emerging chains — phase C bolt-on

Decision gate: if Blend liquidator profit/hit has compressed below $300 OR Sonic+HyperEVM liquidation activity is <2/day combined, phase A is dead and we pivot or kill.

## Research questions

### NFTfi (Blend specifically)
- Blend Dutch-auction frequency: how many auctions/week in last 90 days?
- Average liquidator profit per auction historically (90d / 30d / 7d trends)
- Which collections have ≥10 auctions in last 30 days (= our coverage allowlist)
- Blur bid pool: API endpoint for live top-bid quote, rate limits, auth requirements
- Same-tx exit feasibility: can `Blend.repay() → receive NFT → Blur.fulfillBid()` actually compose in one tx?

### New-chain lending
- Sonic: TVL + liquidation history for Silo, Avalon, LayerBank, Beets Vault
- HyperEVM: TVL + liquidation history for HypurrFi, HyperLend, Felix
- Berachain: TVL + liquidation history for Beraborrow, Dolomite, Bend
- Oracle stack per protocol (Pyth = block-race; Chainlink = threshold-update)
- Flash loan availability: is Balancer Vault deployed? Aave V3 fork present?
- Exit liquidity: can seized USDC/WETH/native be swapped <2% slippage on chain's biggest DEX?

### Cross-cutting infra
- RPC quality per chain (public vs paid; WSS support)
- Gas cost per liquidate-and-exit tx estimate per chain

## Steps
- [x] Dispatch parallel research agents (Blend stats, new-chain lending stats, flash-loan source audit)
- [x] Synthesize findings into a go/no-go memo per phase
- [x] Phase A (NFTfi/Blend): **KILLED** — Blend has no third-party liquidator role (`seize()` lender-only); Blur bid pool requires per-caller off-chain oracle sigs that don't compose atomically; NFT lending market down 97%
- [x] Phase B (new-chain lending): **GO**, revised ordering — Berachain first (proven infra), HyperEVM second, Sonic third
- [x] Phase C (DEX arb): **bolt-on confirmed** — Balancer V3 0% flash loans deployed across all target chains
- [x] Verify Beraborrow `liquidate()` ABI + event signature — Liquity V2 pattern, permissionless, but **gas-comp only** (~0.0375 WETH + min(0.5% coll, 2 LST)); bulk goes to LSP. Only worth it on batch liquidations.
- [x] Verify Bend (Morpho fork) liquidation permissionlessness — **YES, full Morpho mechanics**: permissionless 5%-ish LIF to msg.sender, isolated markets, free flash loan in same singleton via `flashLoan(token, amount, data)`.
- [x] Verify Dolomite — **GATED** to `globalOperator`, cannot race. Drop from plan.
- [x] Confirm flash loan source on Berachain — **Bend's Morpho singleton** is the answer (0% fee, atomic same-tx). BEX flash loans disabled via prohibitive protocol fee.
- [x] Identify live Pyth feed addresses — Pyth deployed at `0x2880aB155794e7179c9eE2e38200202908C17B43` but **not used by our targets**. Beraborrow uses Redstone push, Bend uses per-market oracles (Redstone for launch markets), Dolomite uses Chainlink/Redstone/Chronicle. No oracle-update sandwich needed.

## Findings summary

**Final priority:** Bend (Morpho fork) is the primary build target. Beraborrow is opportunistic batch-only secondary. Dolomite is out. Architecture: pure mempool + block-position race (same as MIBERA — no oracle-update bundle).

**Build-phase blockers identified:**
- Bend singleton address not on Berascan — must scrape from `bend.berachain.com/lend` app bundle or `berachain/contracts-metamorpho-v1.1` deploy artifacts
- Bend markets must be discovered via `CreateMarket` event log scan
- Beraborrow per-collateral DenManager discovery via `NECT.borrowerOperations() → BorrowerOperations.denManagers(asset)`

**Income re-estimate (revised UP after on-chain scrape):**

Bend singleton: `0x24147243f9c08d835C218Cda1e135f8dFD0517D0` (verified — Morpho Blue fork, NOT a MetaMorpho vault)

Markets (6): WBTC/HONEY ($162k borrowed, LLTV 86%), sUSDe/HONEY ($60k, 91.5%), WETH/HONEY ($57k, 86%), WBERA/HONEY ($1k, 77%), wgBERA/HONEY (drained), iBERA/HONEY (drained). All loans denominated in HONEY (Berachain stablecoin).

**Liquidation history (last 1M blocks / 23 days):**
- 8 liquidations, ALL in May 6-11 cluster (5.3-day window), zero since
- Total liquidator profit: **$45,171**
- Avg per hit: **$5,646** (largest: $30k iBERA on May 6)
- Only 2 active liquidator bots: `0x0568ccb3...` (caught the whale, $30k), `0x22c3462e...` (caught cleanup, $15k)
- **LIF = 15%** at all Bend LLTVs (Morpho formula caps at max for LLTV ≥ ~75%); docs claim 5% but that's wrong

**Flash loan dominance:** Single bot `0xc1fad5730e...` doing 1,400+ flashloans/hr from Bend singleton. Bend's free 0% flashloan is being used as arb capital by an existing operator — not the lending side.

**Updated income estimate:** $50-200k/year addressable as 3rd-of-3 liquidator. Cluster-driven (volatility events), not steady. Single-event capture during major dump can be $30-100k.

Decision: **STRONG GO on Bend.** Adding Beraborrow batch + Sonic Silo V2 + Phase C arb still on track.

**Kill condition:** if Bend paper-trade in build-Week-1 shows <2 liquidations/week, kill the whole new-chain plan and stay on MIBERA.

## Build-phase tasks created (2026-05-18)
- [x] `2026-05-18-bend-t1-indexer-positions.md` — event scraper + position registry
- [x] `2026-05-18-bend-t2-monitor-healthfactor.md` — HF computation + Telegram alerts
- [x] `2026-05-18-bend-t3-liquidator-contract.md` — Solidity atomic contract + Foundry tests
- [x] `2026-05-18-bend-t4-executor-race.md` — race executor + paper-trade gate
- [x] `2026-05-18-bend-t5-systemd-deploy.md` — production deployment on jojo-os laptop

## Outcome

Completed 2026-05-18 (same-day research → decision). Killed Phase A (NFTfi/Blend) after verifying at the contract level that no third-party liquidator role exists on Blend (`seize()` is lender-only) and Blur's bid pool's oracle-signature requirement breaks atomic flash-loan composability. Confirmed Phase B is genuinely profitable: Bend on Berachain showed $45k of liquidator profit in a 5-day cluster (May 6-11), only 2 competing bots, 15% LIF (not 5% as docs claim). Verified the singleton at `0x24147243...` is the real Morpho Blue, mapped all 6 markets, identified flash loan source (Bend itself, 0% fee), and proved my home laptop is already serving as a VPS via systemd. Created 5 build-phase task files (t1-t5) covering indexer, monitor, contract, executor, and deployment. Next: start t1 (Bend position indexer).

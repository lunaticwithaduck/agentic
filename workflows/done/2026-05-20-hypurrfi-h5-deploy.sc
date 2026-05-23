---
domain: hyperevm
source_task: 2026-05-20-hypurrfi-h5-deploy.md
date: 2026-05-20
keywords: ["hyperevm", "hyperliquid", "hypurrfi", "hyperlend", "alchemy", "rpc", "getLogs"]
---

## Extracted Knowledge

### Alchemy free tier on HyperEVM
- `eth_getLogs` is capped at a **10-block range** — not advertised on the dashboard
- Error returned: `{"code":-32600,"message":"Under the Free tier plan, you can make eth_getLogs requests with up to a 10 block range..."}`
- Naive retry loops swallow this silently and return empty arrays — burned 37 min on this exact failure mode
- `eth_call` / `eth_blockNumber` / `eth_getTransactionCount` work fine on free tier
- WSS subscriptions also work (received consecutive blocks fine)

### HyperEVM public RPC behavior (`rpc.hyperliquid.xyz/evm`)
- Accepts 1000-block `eth_getLogs` windows (this is the *HyperEVM* hard limit)
- Bursty rate limit: a few seconds of pounding triggers cooldown returning `{"code":-32005,"message":"rate limited"}`
- Cooldown is short (~30s) — recovers on its own
- Even at `MAX_PARALLEL=2` it rate-limits if used as the sole endpoint
- Safe pattern: rotate across 3 endpoints, ~1 chunk/sec per provider

### Working alternate HyperEVM RPCs (no auth, no API key)
- `https://hyperliquid-json-rpc.stakely.io` — works, 1000-block getLogs OK (occasionally returns "invalid block range")
- `https://rpc.purroofgroup.com` — works, 1000-block getLogs OK
- `https://hyperliquid.drpc.org` — broken (`eth_blockNumber` returns "method does not exist")
- `https://1rpc.io/hyperliquid` — free tier exhausted quickly
- `https://rpc.ankr.com/hyperliquid_evm` — requires API key

### EIP-55 checksum with ethers v6
- ethers v6 *strict-rejects* mixed-case addresses with wrong checksum at the encode boundary, not at construction
- `ethers.getAddress("0xCAfE...")` (mixed) throws if checksum is wrong
- `ethers.getAddress("0xcafe...")` (all-lowercase) **succeeds** and returns the correct EIP-55 form
- HypurrFi Pool address `0xcecce0EB...` (wrong checksum, mixed case) crashed reconcile *after* the 9-min getLogs phase completed — extremely late failure
- **Defensive pattern**: at indexer/monitor/executor startup, pass every address through `ethers.getAddress(addr.toLowerCase())` to normalize before use

### Aave V3 dynamic reserve discovery
- `Pool.getReservesList() -> address[]` returns underlying tokens
- `Pool.getReserveData(asset) -> (..., aTokenAddress, ..., variableDebtTokenAddress, ...)` returns the rest
- Pattern works on HyperLend (8 reserves), HypurrFi (19 reserves), any standard Aave V3 fork
- HypurrFi specific: 19 reserves include PT (Pendle principal tokens), foreign-chain wraps (USOL, XAUt0, thBILL), and LST wraps (kHYPE, beHYPE, wstHYPE) — symbol/decimal queries via ERC20 succeed on all

### HyperEVM gas/deploy economics
- Liquidator contract deploys for ~840k gas
- At ~$5.50/HYPE and observed gas price, that's ~$0.07–$0.10 USD
- Storage cost for `Pool` constructor arg is trivial — parameterization is essentially free
- Same wallet (0x8Defac...) can hold gas for multiple chain deploys; ~0.01 HYPE = several deploys worth

## Proposed Skill Content

A skill `hyperevm` covering:
1. **RPC tier-and-routing** decision matrix — when to use Alchemy vs public vs paid; how each chokes
2. **eth_getLogs gotchas** — 1000-block hard limit, free-tier 10-block cap, rate-limit signatures
3. **Aave V3 fork integration template** (since both HyperLend and HypurrFi follow this) — reserves discovery, ProtocolDataProvider auto-detection, getUserAccountData base-unit (1e8 USD)
4. **Address normalization** — always lowercase-then-getAddress at config load
5. **HyperEVM dual-block model** — 2M small (1s) vs 30M big (60s, opt-in); gas headroom planning
6. **MAX_PARALLEL tuning** for log scans across multiple free RPCs (sweet spot: 3-way rotation, ≤3 parallel)

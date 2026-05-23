---
title: HyperEVM HyperLend — h1 research before porting
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Verdict: BUILD

| Item | Value |
|---|---|
| Primary target | HyperLend (Aave V3 fork, $438M TVL, $230M open debt) |
| Secondary target | HypurrFi (Aave V3 fork, $151M+ TVL — same ABI) |
| Build effort | 3-5 days (Aave V3 ABI = drop-in for our existing contract pattern) |
| Expected capture | $2-8k/mo net at moderate competition |
| Free WSS available? | **NO** — Hyperliquid public RPC is HTTP only |
| Recommended RPC | Alchemy $5/mo entry plan (WSS + 25rps + 15× public throughput) |

## Contract addresses (HyperEVM chain id 999)

### HyperLend
- Pool (proxy): `0x00A89d7a5A02160f20150EbEA7a2b5E4879A1A8b`
- Pool impl: `0xBEBb62C7FF8B96dB4325D9481c44e09A92d49B06`
- PoolAddressesProvider: `0x72c98246a98bFe64022a3190e7710E157497170C`
- AaveOracle: `0xC9Fb4fbE842d57EAc1dF3e641a281827493A630e`
- ProtocolDataProvider: `0x4f4d4cA1e0a8A21FE0B460613bEbe917f2eb4326`
- Flash loan fee: **4 bps (0.04%)** — cheaper than Aave default 5 bps

### HypurrFi
- Pool (proxy): `0xcecce0EB9DD2Ef7996e01e25DD70e461F918A14b`
- Pool impl: `0x980bdd9cf1346800f6307e3b2301ffd3ce8c7523`

### DEX exit — HyperSwap V3 (Uni V3 compatible, NO Shadow-style quirks)
- Factory: `0xB1c0fa0B789320044A6F623cFe5eBda9562602E3`
- SwapRouter01: `0x4E2960a8cd19B467b82d26D83fAcb0fAE26b094D`
- WHYPE (wrapped native): `0x5555555555555555555555555555555555555555`

## Liquidation ABI (verbatim Aave V3)
```solidity
function liquidationCall(
    address collateralAsset,
    address debtAsset,
    address user,
    uint256 debtToCover,
    bool receiveAToken
) external;
```

Flash loan via same Pool:
```solidity
function flashLoanSimple(
    address receiverAddress,
    address asset,
    uint256 amount,
    bytes calldata params,
    uint16 referralCode
) external;
```

Receiver implements `executeOperation(asset, amount, premium, initiator, params)`.

## Critical gotchas
1. **Dual-block architecture**: liquidations land in small blocks (1s cadence, **2M gas cap**). Big blocks (60s, 30M gas) require opt-in via HyperCore `evmUserModify{usingBigBlocks: true}`. Our liquidation tx is ~700k-1.4M — fits, but single-hop swaps only.
2. **No free WSS** — must use Alchemy $5/mo OR HTTP polling at 100 req/min limit (workable only for slow-moving liquidations, not race scenarios).
3. **Flash loan fee 4 bps NOT 5** — saves a few bps vs mainnet Aave default.
4. **HyperLend is a forked-with-mods Aave V3.0.2** — liquidation flow itself is unmodified, but Permit/PriceOracleSentinel/FlashLoanLogic have minor mods. Verify ABI against deployed bytecode before signing.

## Top markets
- kHYPE pool: $304M TVL (dominant)
- Aave-style isolation/eMode supported
- Active loan pairs: HYPE-LSTs vs stables (USDe, USDT0), wstHYPE/USDe loops
- Per-market LLTV/bonus: read live via `getReserveConfigurationData(asset)` from ProtocolDataProvider

## Outcome

Completed 2026-05-20. HyperEVM HyperLend is the third chain to build. Architecture port is the cleanest of the three — Aave V3 ABI is verbatim what our existing SiloLiquidator pattern uses (minus the partial-liquidation hook indirection). Two protocols at $738M combined TVL, single Aave V3 codebase (no Silo V2 / Morpho Blue divergence to handle), HyperSwap V3 is vanilla Uni V3 (no Shadow tickSpacing quirks). Single decisive blocker: no free WSS on HyperEVM, $5/mo Alchemy plan is the path. Once that's authorized, 3-5 days of build to flip live.

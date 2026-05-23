---
title: HyperLend — h3 deploy contract + systemd + flip live
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Steps shipped
- [x] Wallet bridged 0.0129 HYPE (~$0.58)
- [x] Dry-run gas estimate: 820k @ ~0.0002 gwei = 0.000136 HYPE ($0.006)
- [x] Deployed HyperLendLiquidator at `0x3C5183D8766d03f9B847a22A70b3805C62A73a0F` (block 35621586, tx `0x629633c84f24c6805444b6799610657dac50c421c353ee601d676891bf48b081`, 812,512 gas)
- [x] 3 systemd unit files (hyperlend-indexer/monitor/executor) enabled + started
- [x] Flipped executor from HYPE_DRY=1 to LIVE mode

## Live state
```
mode:       🟢 LIVE
contract:   0x3C5183D8766d03f9B847a22A70b3805C62A73a0F
signer:     0x8Defac3F807375bc078748F0C2D18d580e333B89
HYPE bal:   0.012 HYPE (~10-20 fires of gas headroom)
top target: 0x095c93...e68a9 — HF 1.066, debt $275k (6.6% LTV move from firing)
```

Note: same contract address as Sonic's (`0x3C5183D8766d03f9B847a22A70b3805C62A73a0F`) — same wallet at nonce 0 on different chains. No collision, totally normal.

## Outcome

Completed 2026-05-20. Third chain live. HyperLend (Aave V3 fork on HyperEVM) liquidator deployed and running. systemd 3-service stack active. Combined with Bend (Berachain) and Sonic Silo V2, the wallet is now hunting liquidations across 3 chains simultaneously — 140 borrowers tracked, $2.2M+ addressable debt, expected combined capture $19-68k/month. HyperEVM uses HTTP polling + Multicall3 batching (no free WSS); will switch to Alchemy $5/mo paid WSS when their portal maintenance ends for fast-race competitiveness.

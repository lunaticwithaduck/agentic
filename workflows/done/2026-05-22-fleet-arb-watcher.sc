---
domain: defi-liquidations
source_task: 2026-05-22-fleet-arb-watcher.md
date: 2026-05-22
keywords: [arb-watcher, fleet-monitor, coingecko-onchain-api, base-quote-normalization, systemd-oneshot-timer, change-based-dedup, telegram-tripwire]
---

## Extracted Knowledge

### CoinGecko on-chain endpoint = best free multi-chain DEX data source
For a multi-chain arb/gap watcher, the CoinGecko on-chain endpoint
(`https://api.coingecko.com/api/v3/onchain/networks/{slug}/pools`) delivers:
- Top 20 pools per chain per page
- Per pool: base/quote token IDs, pool reserve in USD, 24h volume, USD prices,
  price-in-base-token, price-in-quote-token, DEX identifier
- Works for all our chains: berachain, hyperevm, sonic, monad, ink, somnia
- Free Demo API key gives 30 req/min, 10k/month — easily covers a 4h cadence × 6 chains

GeckoTerminal's public `api.geckoterminal.com/api/v2/...` works for established chains
but **may return 0 pools for newer chains** (Somnia returned empty on `geckoterminal.com`
but populated on `coingecko.com/.../onchain/...`). Use the authenticated CoinGecko
on-chain endpoint as the primary; GeckoTerminal as fallback only.

### Canonical-token-sort pair key for cross-pool comparison
**Critical pitfall:** two pools on the same pair can have token order reversed
(Pool A: base=USDC, quote=WMON; Pool B: base=WMON, quote=USDC). The fields
`base_token_price_quote_token` and `quote_token_price_base_token` are pool-local —
comparing them naively yields ridiculous "gaps" (saw 139786% in a real run).

Fix pattern: canonicalize by sorting token IDs alphabetically, then read the price
that gives "lower-token per 1 higher-token" from each pool:
```js
const [tokA, tokB] = basId < quoId ? [basId, quoId] : [quoId, basId];
let priceAperB;
if (basId < quoId) priceAperB = parseFloat(a.base_token_price_quote_token);
else               priceAperB = parseFloat(a.quote_token_price_base_token);
```
Now `priceAperB` is comparable across pools regardless of how each pool ordered its
tokens. The "max - min" gap is then the real DEX-DEX divergence.

### Verify detection logic by lowering threshold pre-deploy
Before shipping any tripwire monitor, lower the thresholds way past production
(e.g., 0.1% gap, $5k depth) and run a dry pass. If the detection finds REAL
same-pair multi-DEX matches at low thresholds — your detection logic works.
If it finds only nonsense numbers — there's a bug.

Production thresholds should be set so that real (but non-actionable) divergences
DON'T fire. The dry-low-threshold test gives confidence both halves work.

This caught the base/quote orientation bug in our fleet-arb-watcher pre-ship.

### Pacing API calls below free-tier rate limits
For CoinGecko Demo (30 req/min, 6 chain endpoints per sweep), insert 2.5s
between calls (`await new Promise(r => setTimeout(r, 2500))`) — keeps the
whole sweep at 15s which is well under the per-minute window. Single sweep
is ~12-15s wall-clock for 6 chains.

For higher cadence or more chains, batch or upgrade the API tier — don't try
to game the rate limit with retries (you'll trip the 429 floor and lose all
in-flight data).

### Telegram sender outside node_modules — load env manually
Scripts under `/home/jojo/automation/<chain>/...` can `require('dotenv')` but
fleet-wide scripts at `/home/jojo/automation/arb-watch/...` may also need an env
loader. Cleanest: inline 5-line parser, reuse across all fleet-wide scripts:
```js
const env = fs.readFileSync('/home/jojo/automation/.env', 'utf8');
for (const line of env.split('\n')) {
  const m = line.match(/^([A-Z_][A-Z0-9_]*)\s*=\s*(.*)$/);
  if (m && !process.env[m[1]]) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
}
```
Then `require('/home/jojo/automation/mibera/sweeper/telegram').send` works
because the shared Telegram helper reads `TELEGRAM_BOT_TOKEN` + `TELEGRAM_CHAT_ID`
from `process.env`.

### Systemd oneshot timer pattern for periodic checks
For periodic-not-continuous monitors (cliff scope, arb watch, missed-liq audit):
```ini
# arb-watch.service
[Service]
Type=oneshot
ExecStart=/usr/bin/node /path/to/check.js

# arb-watch.timer
[Timer]
OnBootSec=5min
OnUnitActiveSec=4h
Persistent=true
```
- `OnBootSec=5min` delays first run until after boot settles
- `OnUnitActiveSec=4h` schedules next run 4h after the previous run STARTED
- `Persistent=true` catches up missed runs after sleep/resume
- `systemctl --user enable --now arb-watch.timer` enables + starts in one shot

Logs go to `--StandardOutput=append:/.../logs/arb-watch.log`. Don't use journal
for high-cadence repeating runs — gets noisy fast.

### Change-based dedup with 24h backstop is the right default
For "alert me when X happens, but don't spam if it persists":
- Persist state as `{ alertKey: lastAlertedAtMs }`
- Drop entries older than 24h on load (genuine re-emergence after pause = re-alert)
- Set entry once the alert fires; never re-fire until the entry ages out

Avoids both: (a) re-alerting every check while condition persists, and (b) silent
loss of an obviously-re-emerging condition. The 24h window is the sweet spot for
operational signals — your sleep cycle won't miss anything, your daytime won't
get spam.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md` (under fleet-monitoring / DEX-data sections):

### CoinGecko on-chain endpoint = preferred multi-chain DEX data source
Demo API key works for `https://api.coingecko.com/api/v3/onchain/networks/{slug}/pools`.
Better than GeckoTerminal's `api.geckoterminal.com` for newer chains (Somnia returns
empty on GT public, populated on CoinGecko onchain).

### Always canonicalize token order before cross-pool price comparison
Sort token IDs and read the appropriate `base_token_price_quote_token` /
`quote_token_price_base_token` field based on which token is the canonical "A".
The base/quote orientation pitfall produces eye-watering false positives if not
handled.

### Verify detection by lowering threshold pre-deploy
Drop tripwire thresholds way below production for a single dry pass. If real
same-pair matches appear, the detection logic works. If nonsense appears, fix
before deploying. Cheap, repeatable, catches structural bugs.

### Persistent change-based dedup + 24h backstop
The right default for any operational tripwire. Persists across process restarts
(unlike pure in-memory Map dedup). Re-fires after 24h if condition still active —
sweet spot for sleep-cycle-friendly alerting.

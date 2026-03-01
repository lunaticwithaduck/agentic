#!/usr/bin/env python3
"""
bench/lib/dashboard.py — Full-terminal benchmark dashboard (zero deps, stdlib only).

Renders a multi-panel ASCII/Unicode dashboard using only ANSI escape codes and
block characters.  No matplotlib, no numpy, no nothing.

Panels:
  ┌─────────────────────────────────────────────────────────────────┐
  │  STAT BAR  — headline numbers across the top                    │
  ├──────────────────────┬──────────────────────────────────────────┤
  │  SUITE HISTORY       │  SKILL DETECTION P/R/F1                  │
  │  sparklines per run  │  bar chart per metric                    │
  ├──────────────────────┴──────────────────────────────────────────┤
  │  TASK QUALITY  — grouped bar: with-infra vs vanilla             │
  ├──────────────────────────────────────────────────────────────────┤
  │  DELTA WATERFALL  — quality delta per task                      │
  ├──────────────────────┬──────────────────────────────────────────┤
  │  WIN-RATE TREND      │  TOKEN COST per task                     │
  │  over runs           │  ratio bar + delta                       │
  └──────────────────────┴──────────────────────────────────────────┘

Usage:
    python3 bench/lib/dashboard.py
    python3 bench/lib/dashboard.py --no-color
"""

import json
import os
import sys
from pathlib import Path
from datetime import datetime, timezone

# ── Paths ─────────────────────────────────────────────────────────────────────
BENCH_DIR   = Path(__file__).parent.parent
METRICS_DIR = BENCH_DIR / "results" / "metrics"
E2E_DIR     = BENCH_DIR / "results" / "e2e"
TOKENS_DIR  = BENCH_DIR / "results" / "tokens"

# ── Terminal width ─────────────────────────────────────────────────────────────
try:
    TERM_W = os.get_terminal_size().columns
except OSError:
    TERM_W = 100
TERM_W = max(80, min(TERM_W, 160))

# ── ANSI ──────────────────────────────────────────────────────────────────────
NO_COLOR = "--no-color" in sys.argv or not sys.stdout.isatty()

def _c(*codes):
    return "" if NO_COLOR else f"\033[{';'.join(str(c) for c in codes)}m"

RST   = _c(0)
BOLD  = _c(1)
DIM   = _c(2)
RED   = _c(91)
GRN   = _c(92)
YLW   = _c(93)
BLU   = _c(94)
MAG   = _c(95)
CYN   = _c(96)
WHT   = _c(97)

# true-color helpers (fall back gracefully when NO_COLOR)
def fg(r, g, b):
    return "" if NO_COLOR else f"\033[38;2;{r};{g};{b}m"

INFRA_C   = fg(76,  155, 232)   # blue
VANILLA_C = fg(232, 120,  76)   # orange
WIN_C     = fg( 82, 201, 122)   # green
LOSE_C    = fg(232,  82,  82)   # red
ACCENT_C  = fg(247, 204,  80)   # yellow
DIM_C     = fg(139, 148, 158)   # grey

# ── Block chars ───────────────────────────────────────────────────────────────
FULL  = "█"
HALF  = "▌"
EMPTY = "░"
SHADE = "▓"
SPARK = ["▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"]  # sparkline levels

# ═══════════════════════════════════════════════════════════════════════════════
# Low-level rendering primitives
# ═══════════════════════════════════════════════════════════════════════════════

def bar(value: float, max_val: float, width: int, color: str = "", bg: str = DIM_C) -> str:
    """Horizontal filled bar."""
    ratio  = max(0.0, min(value / max_val, 1.0)) if max_val else 0.0
    filled = round(ratio * width)
    empty  = width - filled
    return f"{color}{FULL * filled}{RST}{bg}{EMPTY * empty}{RST}"


def signed_bar(value: float, max_abs: float, half_width: int,
               pos_color: str = WIN_C, neg_color: str = LOSE_C) -> str:
    """Centred bar for +/- values. half_width = chars on each side of zero."""
    ratio  = abs(value) / max_abs if max_abs else 0.0
    blocks = round(ratio * half_width)
    pad    = half_width - blocks
    if value >= 0:
        left  = DIM_C + EMPTY * half_width + RST
        right = pos_color + FULL * blocks + RST + DIM_C + EMPTY * pad + RST
    else:
        left  = DIM_C + EMPTY * pad + RST + neg_color + FULL * blocks + RST
        right = DIM_C + EMPTY * half_width + RST
    return left + DIM_C + "┃" + RST + right


def sparkline(values: list[float]) -> str:
    """Single-line sparkline string."""
    if not values:
        return ""
    lo, hi = min(values), max(values)
    span   = hi - lo or 1
    chars  = []
    for v in values:
        idx = round((v - lo) / span * (len(SPARK) - 1))
        chars.append(SPARK[idx])
    return "".join(chars)


def hline(width: int, char: str = "─") -> str:
    return DIM_C + char * width + RST


def section(title: str, width: int) -> str:
    pad   = width - len(title) - 4
    left  = pad // 2
    right = pad - left
    return (DIM_C + "─" * left + RST + "  " +
            BOLD + CYN + title + RST +
            "  " + DIM_C + "─" * right + RST)


def color_score(s: float) -> str:
    if s >= 1.0: return WIN_C
    if s >= 0.7: return YLW
    return LOSE_C

# ═══════════════════════════════════════════════════════════════════════════════
# Data loading
# ═══════════════════════════════════════════════════════════════════════════════

def load_metrics() -> list[dict]:
    records = []
    for p in sorted(METRICS_DIR.glob("*.json")):
        try:
            d = json.loads(p.read_text())
            if "timestamp" in d:
                records.append(d)
        except Exception:
            pass
    records.sort(key=lambda r: r["timestamp"])
    return records


def load_latest(directory: Path, pattern: str) -> dict | None:
    matches = sorted(directory.glob(pattern))
    if not matches:
        return None
    try:
        return json.loads(matches[-1].read_text())
    except Exception:
        return None


def short_ts(ts: str) -> str:
    try:
        dt = datetime.fromisoformat(ts.rstrip("Z").split("+")[0])
        return dt.strftime("%m-%d %H:%M")
    except Exception:
        return ts[:13]

# ═══════════════════════════════════════════════════════════════════════════════
# Panels
# ═══════════════════════════════════════════════════════════════════════════════

def panel_stat_bar(e2e: dict, tokens: dict | None) -> list[str]:
    """Top headline row of key numbers."""
    win_rate  = e2e.get("infra_win_rate", 0) * 100
    iw        = e2e.get("infra_wins", 0)
    tc        = e2e.get("task_count", 0)
    avg_delta = e2e.get("avg_delta", 0)
    avg_with  = e2e.get("avg_with_score", 0)
    avg_wo    = e2e.get("avg_without_score", 0)
    cost      = tokens.get("avg_cost_ratio") if tokens else None

    dc  = WIN_C if avg_delta > 0 else LOSE_C
    wrc = WIN_C if win_rate >= 60 else LOSE_C

    items = [
        (f"{wrc}{win_rate:.0f}%{RST}",          "infra win-rate"),
        (f"{WIN_C}{iw}{RST}{DIM_C}/{tc}{RST}",  "wins/tasks"),
        (f"{dc}{avg_delta:+.3f}{RST}",           "avg Δ quality"),
        (f"{INFRA_C}{avg_with:.3f}{RST}",        "avg with-infra /5"),
        (f"{VANILLA_C}{avg_wo:.3f}{RST}",        "avg vanilla /5"),
        (f"{ACCENT_C}{cost:.3f}×{RST}" if cost else f"{DIM_C}—{RST}", "avg cost ratio"),
    ]

    import re
    ansi_re = re.compile(r"\033\[[^m]*m")
    col_w = TERM_W // len(items)
    top_row = ""
    bot_row = ""
    for val, label in items:
        # val contains ANSI codes — pad by display width, not raw len
        visible_len = len(ansi_re.sub("", val))
        pad = max(0, col_w - visible_len)
        top_row += f"{BOLD}{val}{' ' * pad}{RST}"
        bot_row += f"{DIM_C}{label:<{col_w}}{RST}"

    lines = [
        section("BENCHMARK DASHBOARD", TERM_W),
        "",
        top_row,
        bot_row,
        "",
    ]
    return lines


def panel_suite_history(metrics: list[dict], width: int) -> list[str]:
    """Sparkline history table — one row per suite."""
    suite_defs = [
        ("01-infrastructure",  "01 Infra    ", WIN_C),
        ("02-skill-detection", "02 Skill Det", INFRA_C),
        ("03-hook-security",   "03 Hook Sec ", ACCENT_C),
        ("04-task-quality",    "04 Task Qual", VANILLA_C),
        ("05-keyword-overlap", "05 KW Overlap", MAG),
        ("06-token-cost",      "06 Token Cost", fg(232, 76, 160)),
    ]

    lines = [section("SUITE HISTORY  (oldest → newest)", width), ""]

    # timestamps header
    ts_labels = [short_ts(r["timestamp"]) for r in metrics]
    run_w     = max(len(t) for t in ts_labels) + 2
    header    = DIM_C + " " * 14 + RST
    for t in ts_labels:
        header += DIM_C + f"{t:^{run_w}}" + RST
    lines.append(header)
    lines.append(DIM_C + " " * 14 + "─" * (run_w * len(metrics)) + RST)

    for suite_id, label, color in suite_defs:
        row = f"{DIM_C}{label:<14}{RST}"
        values = []
        for rec in metrics:
            s = rec.get("suites", {}).get(suite_id, {})
            if s and "score" in s:
                v = s["score"]
                skipped = s.get("passed", 0) == 0 and s.get("failed", 0) == 0
                values.append((v, skipped))
            else:
                values.append(None)

        for entry in values:
            if entry is None:
                cell = DIM_C + f"{'—':^{run_w}}" + RST
            else:
                v, skipped = entry
                if skipped:
                    cell = YLW + f"{'skip':^{run_w}}" + RST
                else:
                    idx  = round(v * (len(SPARK) - 1))
                    ch   = SPARK[idx]
                    pct  = f"{v:.0%}"
                    c    = color_score(v)
                    cell = f"{c}{ch}{pct:^{run_w - 1}}{RST}"
            row += cell

        lines.append(row)

    lines.append("")
    return lines


def panel_skill_detection(metrics: list[dict], width: int) -> list[str]:
    """Horizontal bar chart for the latest P/R/F1 values."""
    # collect latest run that has suite 02 data
    latest = None
    for rec in reversed(metrics):
        s = rec.get("suites", {}).get("02-skill-detection", {})
        if "precision" in s:
            latest = s
            break

    lines = [section("SKILL DETECTION  (latest)", width), ""]

    if not latest:
        lines.append(f"  {DIM_C}no data{RST}")
        lines.append("")
        return lines

    bar_w   = width - 22
    metrics_list = [
        ("Precision", latest.get("precision", 0), INFRA_C),
        ("Recall   ", latest.get("recall",    0), ACCENT_C),
        ("F1       ", latest.get("f1",        0), WIN_C),
    ]

    tp = latest.get("tp", 0)
    fp = latest.get("fp", 0)
    fn = latest.get("fn", 0)

    for label, val, color in metrics_list:
        b   = bar(val, 1.0, bar_w, color)
        pct = f"{val:.1%}"
        lines.append(f"  {DIM_C}{label}{RST}  {b}  {color}{BOLD}{pct}{RST}")

    lines.append("")
    lines.append(f"  {DIM_C}TP:{RST}{WIN_C}{tp}{RST}  "
                 f"{DIM_C}FP:{RST}{LOSE_C}{fp}{RST}  "
                 f"{DIM_C}FN:{RST}{YLW}{fn}{RST}  "
                 f"{DIM_C}prompts:{RST} {latest.get('total_prompts','?')}")
    lines.append("")
    return lines


def panel_task_quality(e2e: dict, width: int) -> list[str]:
    """Side-by-side bars for with-infra vs vanilla score per task."""
    tasks  = e2e.get("tasks", [])
    lines  = [section("TASK QUALITY  (with-infra  vs  vanilla)", width), ""]

    max_score = 5.0
    bar_w     = (width - 28) // 2   # bars on each side

    # header
    lines.append(
        f"  {'Task':<6}  "
        f"{INFRA_C}{'with-infra':^{bar_w + 7}}{RST}  "
        f"{VANILLA_C}{'vanilla':^{bar_w + 7}}{RST}  "
        f"{'Δ':>6}"
    )
    lines.append("  " + DIM_C + "─" * (width - 4) + RST)

    for t in tasks:
        tid  = t["task_id"]
        ws   = t["with_score"]
        vs   = t["without_score"]
        d    = t["delta"]
        w    = t.get("winner", "")

        wb   = bar(ws, max_score, bar_w, INFRA_C)
        vb   = bar(vs, max_score, bar_w, VANILLA_C)
        dc   = WIN_C if d > 0 else (LOSE_C if d < 0 else DIM_C)
        win_marker = f"{WIN_C}◀{RST}" if w == "A" else (f"{LOSE_C}▶{RST}" if w == "B" else " ")

        lines.append(
            f"  {DIM_C}{tid}{RST}  "
            f"{wb} {INFRA_C}{ws:.2f}{RST} "
            f"{win_marker} "
            f"{vb} {VANILLA_C}{vs:.2f}{RST}  "
            f"{dc}{d:+.2f}{RST}"
        )

    lines.append("")
    return lines


def panel_delta_waterfall(e2e: dict, width: int) -> list[str]:
    """Centred waterfall of quality deltas."""
    tasks    = e2e.get("tasks", [])
    deltas   = [t["delta"] for t in tasks]
    max_abs  = max(abs(d) for d in deltas) if deltas else 1.0
    half_w   = (width - 18) // 2
    avg      = sum(deltas) / len(deltas) if deltas else 0

    lines = [section(f"QUALITY DELTA  (avg {avg:+.3f})", width), ""]

    # zero axis label
    lines.append(
        " " * 8 +
        DIM_C + f"{'−':>{half_w}}" + "┃" + f"{'+':<{half_w}}" + RST
    )
    lines.append(" " * 8 + DIM_C + "─" * (half_w * 2 + 1) + RST)

    for t, d in zip(tasks, deltas):
        tid = t["task_id"]
        b   = signed_bar(d, max_abs, half_w)
        dc  = WIN_C if d > 0 else (LOSE_C if d < 0 else DIM_C)
        lines.append(
            f"  {DIM_C}{tid}{RST}  {b}  {dc}{d:+.2f}{RST}"
        )

    # avg marker row
    avg_pos  = round((avg / max_abs) * half_w) if max_abs else 0
    marker   = " " * 8
    if avg >= 0:
        marker += " " * half_w + "┃" + " " * avg_pos + ACCENT_C + "▲" + RST
    else:
        marker += " " * (half_w + avg_pos) + ACCENT_C + "▲" + RST + "┃"
    lines.append(marker + f"  {ACCENT_C}avg {avg:+.3f}{RST}")
    lines.append("")
    return lines


def panel_winrate_trend(metrics: list[dict], width: int) -> list[str]:
    """Win-rate + avg Δ over all suite-04 runs as a sparkline chart."""
    runs = []
    for rec in metrics:
        s = rec.get("suites", {}).get("04-task-quality", {})
        if "infra_win_rate" in s and "avg_delta" in s:
            runs.append({
                "ts":  short_ts(rec["timestamp"]),
                "wr":  s["infra_win_rate"],
                "d":   s["avg_delta"],
                "iw":  s.get("infra_wins", "?"),
                "vw":  s.get("vanilla_wins", "?"),
            })

    lines = [section("SUITE 04  WIN-RATE TREND", width), ""]

    if not runs:
        lines.append(f"  {DIM_C}no data{RST}")
        lines.append("")
        return lines

    bar_w = width - 32

    # sparkline header
    wr_spark = sparkline([r["wr"] for r in runs])
    d_spark  = sparkline([r["d"]  for r in runs])
    lines.append(f"  {DIM_C}win-rate  {RST}{INFRA_C}{wr_spark}{RST}  "
                 f"{DIM_C}avg Δ  {RST}{ACCENT_C}{d_spark}{RST}")
    lines.append("")

    for r in runs:
        wr  = r["wr"]
        d   = r["d"]
        wrc = WIN_C if wr >= 0.6 else LOSE_C
        dc  = WIN_C if d > 0 else LOSE_C

        wr_bar = bar(wr, 1.0, bar_w // 2, wrc)
        lines.append(
            f"  {DIM_C}{r['ts']}{RST}  "
            f"{wr_bar}  {wrc}{wr:.0%}{RST}  "
            f"{dc}{d:+.3f}Δ{RST}  "
            f"{DIM_C}{r['iw']}W {r['vw']}L{RST}"
        )

    lines.append("")
    return lines


def panel_token_cost(tokens: dict, width: int) -> list[str]:
    """Token cost ratio bar per task."""
    tasks    = tokens.get("tasks", [])
    lines    = [section(f"TOKEN COST  (avg {tokens.get('avg_cost_ratio', 0):.3f}×)", width), ""]

    if not tasks:
        lines.append(f"  {DIM_C}no data{RST}")
        lines.append("")
        return lines

    max_ratio = max(t["cost_ratio"] for t in tasks)
    bar_w     = width - 34

    lines.append(
        f"  {'Task':<6}  {'Ratio bar':<{bar_w}}  {'×':>5}  {'Δ tokens':>10}"
    )
    lines.append("  " + DIM_C + "─" * (width - 4) + RST)

    for t in tasks:
        tid   = t["task_id"]
        ratio = t["cost_ratio"]
        delta = t["output_delta"]
        rc    = WIN_C if ratio <= 1.0 else VANILLA_C
        dc    = WIN_C if delta <= 0  else VANILLA_C

        b = bar(ratio, max(max_ratio, 1.0), bar_w, rc)
        lines.append(
            f"  {DIM_C}{tid}{RST}  "
            f"{b}  {rc}{ratio:.3f}×{RST}  "
            f"{dc}{delta:>+8}t{RST}"
        )

    lines.append("")
    return lines

# ═══════════════════════════════════════════════════════════════════════════════
# Layout
# ═══════════════════════════════════════════════════════════════════════════════

def two_col(left_lines: list[str], right_lines: list[str],
            left_w: int, right_w: int) -> list[str]:
    """Render two panels side-by-side, padded to equal height."""
    # strip ANSI to measure display width
    import re
    ansi_re = re.compile(r"\033\[[^m]*m")

    def display_len(s: str) -> int:
        return len(ansi_re.sub("", s))

    height = max(len(left_lines), len(right_lines))
    left_lines  = left_lines  + [""] * (height - len(left_lines))
    right_lines = right_lines + [""] * (height - len(right_lines))

    out = []
    for l, r in zip(left_lines, right_lines):
        # pad left side to exact display width
        pad = left_w - display_len(l)
        out.append(l + " " * max(0, pad) + DIM_C + "│" + RST + r)
    return out


def render(lines: list[str]) -> None:
    for l in lines:
        print(l)


# ═══════════════════════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════════════════════

def main() -> None:
    metrics = load_metrics()
    e2e     = load_latest(E2E_DIR,    "summary-*.json")
    tokens  = load_latest(TOKENS_DIR, "summary-*.json")

    if not metrics and not e2e:
        print(f"{RED}No benchmark data found. Run:  bash bench/run.sh{RST}")
        sys.exit(1)

    out: list[str] = []

    # ── Stat bar ──────────────────────────────────────────────────────────────
    if e2e:
        out += panel_stat_bar(e2e, tokens)

    # ── Suite history + Skill detection (two-col) ─────────────────────────────
    left_w  = (TERM_W * 3) // 5
    right_w = TERM_W - left_w - 1

    hist_lines  = panel_suite_history(metrics, left_w)
    skill_lines = panel_skill_detection(metrics, right_w)
    out += two_col(hist_lines, skill_lines, left_w, right_w)

    out.append(hline(TERM_W))
    out.append("")

    # ── Task quality ──────────────────────────────────────────────────────────
    if e2e:
        out += panel_task_quality(e2e, TERM_W)

    # ── Delta waterfall ───────────────────────────────────────────────────────
    if e2e:
        out += panel_delta_waterfall(e2e, TERM_W)

    # ── Win-rate trend + Token cost (two-col) ─────────────────────────────────
    left_w2 = TERM_W // 2

    wt_lines  = panel_winrate_trend(metrics, left_w2)
    tok_lines = panel_token_cost(tokens, TERM_W - left_w2 - 1) if tokens else \
                [section("TOKEN COST", TERM_W - left_w2 - 1), "", f"  {DIM_C}no data{RST}", ""]
    out += two_col(wt_lines, tok_lines, left_w2, TERM_W - left_w2 - 1)

    # ── Footer ────────────────────────────────────────────────────────────────
    out.append(hline(TERM_W))
    ts_now = datetime.now(tz=timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    out.append(f"  {DIM_C}generated {ts_now}  ·  bench/lib/dashboard.py{RST}")
    out.append("")

    render(out)


if __name__ == "__main__":
    main()

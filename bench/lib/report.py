#!/usr/bin/env python3
"""
bench/lib/report.py — Pretty terminal report of latest bench results.

Usage:
    python3 bench/lib/report.py            # latest run
    python3 bench/lib/report.py --all      # show last N runs (default: 5)
    python3 bench/lib/report.py --suite=02 # filter to one suite's detail
"""

import json
import sys
import os
from pathlib import Path
from datetime import datetime, timezone

# ── Paths ─────────────────────────────────────────────────────────────────────
BENCH_DIR   = Path(__file__).parent.parent
METRICS_DIR = BENCH_DIR / "results" / "metrics"

# ── ANSI ──────────────────────────────────────────────────────────────────────
IS_TTY = sys.stdout.isatty() or os.environ.get("FORCE_COLOR")

def _c(*codes):
    return f"\033[{';'.join(str(c) for c in codes)}m" if IS_TTY else ""

RESET  = _c(0)
BOLD   = _c(1)
DIM    = _c(2)
RED    = _c(91)
GREEN  = _c(92)
YELLOW = _c(93)
BLUE   = _c(94)
CYAN   = _c(96)
WHITE  = _c(97)

def color_score(score: float, skipped: bool = False) -> str:
    if skipped:
        return YELLOW
    if score >= 1.0:
        return GREEN
    if score >= 0.70:
        return YELLOW
    return RED


# ── Bar rendering ──────────────────────────────────────────────────────────────

def score_bar(score: float, width: int = 14, skipped: bool = False) -> str:
    if skipped:
        return DIM + "─" * width + RESET
    filled  = round(score * width)
    empty   = width - filled
    c       = color_score(score)
    fail_c  = RED if score < 0.70 else (YELLOW if score < 1.0 else GREEN)
    # filled portion in score color, empty in dim
    return f"{c}{'█' * filled}{RESET}{DIM}{'░' * empty}{RESET}"


def mini_bar(value: float, max_val: float = 1.0, width: int = 6) -> str:
    ratio  = min(value / max_val, 1.0) if max_val > 0 else 0
    filled = round(ratio * width)
    return "█" * filled + "░" * (width - filled)


# ── Delta formatting ───────────────────────────────────────────────────────────

def delta_str(cur: float, prev: float) -> str:
    d = cur - prev
    if abs(d) < 0.001:
        arrow = f"{DIM}→{RESET}"
        s = f"{DIM}±0{RESET}"
    elif d > 0:
        arrow = f"{GREEN}↑{RESET}"
        s = f"{GREEN}+{d:.1%}{RESET}"
    else:
        arrow = f"{RED}↓{RESET}"
        s = f"{RED}{d:.1%}{RESET}"
    return f"{s} {arrow}"


# ── Suite-specific extra info ──────────────────────────────────────────────────

SUITE_LABELS = {
    "01-infrastructure":  "01 · Infrastructure  ",
    "02-skill-detection": "02 · Skill Detection ",
    "03-hook-security":   "03 · Hook Security   ",
    "04-task-quality":    "04 · Task Quality    ",
    "05-keyword-overlap": "05 · Keyword Overlap ",
}

def suite_notes(name: str, suite: dict) -> str:
    skipped = suite.get("passed", 0) == 0 and suite.get("failed", 0) == 0

    if name == "02-skill-detection" and not skipped:
        p  = suite.get("precision", 0)
        r  = suite.get("recall", 0)
        f1 = suite.get("f1", 0)
        pc = color_score(p)
        rc = color_score(r)
        fc = color_score(f1)
        return (f"{DIM}P:{RESET}{pc}{p:.1%}{RESET}  "
                f"{DIM}R:{RESET}{rc}{r:.1%}{RESET}  "
                f"{DIM}F1:{RESET}{fc}{f1:.1%}{RESET}")

    if name == "03-hook-security" and not skipped:
        fpr = suite.get("false_positive_rate", 0)
        fnr = suite.get("false_negative_rate", 0)
        blk = suite.get("block_corpus_size", 0)
        alw = suite.get("allow_corpus_size", 0)
        fpr_c = GREEN if fpr == 0 else (YELLOW if fpr < 0.05 else RED)
        fnr_c = GREEN if fnr == 0 else (YELLOW if fnr < 0.05 else RED)
        return (f"{DIM}FPR:{RESET}{fpr_c}{fpr:.1%}{RESET}  "
                f"{DIM}FNR:{RESET}{fnr_c}{fnr:.1%}{RESET}  "
                f"{DIM}({blk}↓ {alw}✓){RESET}")

    if name == "04-task-quality":
        if skipped:
            return f"{YELLOW}skipped{RESET} {DIM}— run from a real terminal{RESET}"
        iw  = suite.get("infra_wins", 0)
        vw  = suite.get("vanilla_wins", 0)
        ti  = suite.get("ties", 0)
        wr  = suite.get("infra_win_rate", 0)
        d   = suite.get("avg_delta", 0)
        wrc = GREEN if wr >= 0.60 else RED
        dc  = GREEN if d > 0 else (YELLOW if d == 0 else RED)
        return (f"{DIM}infra {iw}W {vw}L {ti}T{RESET}  "
                f"{DIM}win-rate:{RESET}{wrc}{wr:.0%}{RESET}  "
                f"{DIM}Δ:{RESET}{dc}{d:+.2f}{RESET}")

    if name == "05-keyword-overlap" and not skipped:
        skills = suite.get("total_skills", 0)
        dup    = suite.get("duplication_rate", 0)
        noisy  = suite.get("noisy_skill_count", 0)
        dup_c  = GREEN if dup < 0.10 else (YELLOW if dup < 0.30 else RED)
        n_c    = GREEN if noisy == 0 else (YELLOW if noisy <= 3 else RED)
        return (f"{DIM}{skills} skills  dup:{RESET}{dup_c}{dup:.1%}{RESET}  "
                f"{DIM}noisy:{RESET}{n_c}{noisy}{RESET}")

    return ""


# ── Load helpers ──────────────────────────────────────────────────────────────

def sorted_jsons() -> list[Path]:
    return sorted(METRICS_DIR.glob("*.json"), key=lambda p: p.stat().st_mtime, reverse=True)


def load_json(path: Path) -> dict:
    return json.loads(path.read_text())


def fmt_ts(ts: str) -> str:
    """'2026-02-25T22:59:09Z' → '2026-02-25 22:59 UTC'"""
    try:
        dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
        return dt.strftime("%Y-%m-%d %H:%M UTC")
    except Exception:
        return ts


def fmt_ms(ms: int) -> str:
    if ms < 1000:
        return f"{ms}ms"
    return f"{ms / 1000:.1f}s"


# ── Single-run report ─────────────────────────────────────────────────────────

def render_run(data: dict, prev: dict | None = None) -> None:
    overall    = data.get("overall", {})
    suites     = data.get("suites", {})
    ts         = fmt_ts(data.get("timestamp", ""))
    sha        = data.get("git_sha", "?")
    total_p    = overall.get("passed", 0)
    total_f    = overall.get("failed", 0)
    total      = total_p + total_f
    o_score    = overall.get("score", 1.0)
    o_dur      = overall.get("duration_ms", 0)

    # Was anything actually skipped?
    any_skipped = any(
        s.get("passed", 0) == 0 and s.get("failed", 0) == 0
        for s in suites.values()
    )

    # Header
    w = 66
    print()
    print(f"{BOLD}{CYAN}╔{'═' * (w-2)}╗{RESET}")
    print(f"{BOLD}{CYAN}║{'  Agentic Bench — Latest Results':^{w-2}}║{RESET}")
    print(f"{BOLD}{CYAN}╚{'═' * (w-2)}╝{RESET}")
    print()

    # Meta line
    o_c = color_score(o_score)
    o_pct = f"{o_score:.1%}"
    print(f"  {DIM}{ts}  ·  sha: {sha}  ·  {fmt_ms(o_dur)}{RESET}")
    print(f"  {BOLD}Overall: {o_c}{o_pct}{RESET}{BOLD}  {score_bar(o_score)}  "
          f"{o_c}{total_p}/{total}{RESET}{BOLD} tests{RESET}")
    print()

    # Column header
    hdr = (f"  {'Suite':<23}  {'Score':>7}  {'Bar':<14}  "
           f"{'Tests':>7}  {'Time':>6}  Notes")
    sep = "  " + "─" * (w - 2)
    print(f"{DIM}{hdr}{RESET}")
    print(f"{DIM}{sep}{RESET}")

    # Suite rows
    for name, suite in suites.items():
        label   = SUITE_LABELS.get(name, name)
        passed  = suite.get("passed", 0)
        failed  = suite.get("failed", 0)
        score   = suite.get("score", 1.0)
        dur_ms  = suite.get("duration_ms", 0)
        skipped = passed == 0 and failed == 0
        total_t = passed + failed

        sc      = color_score(score, skipped)
        score_s  = "skip" if skipped else f"{score:.1%}"
        score_fc = f"{YELLOW}{score_s}{RESET}" if skipped else f"{sc}{score_s}{RESET}"
        tests_s  = "—" if skipped else f"{passed}/{total_t}"
        tests_fc = f"{DIM}{tests_s}{RESET}" if skipped else (
            f"{GREEN if failed == 0 else RED}{tests_s}{RESET}"
        )
        dur_s   = f"{DIM}{fmt_ms(dur_ms)}{RESET}"
        bar     = score_bar(score, skipped=skipped)
        notes   = suite_notes(name, suite)

        # Use raw string widths for alignment (ANSI codes are invisible)
        print(f"  {BOLD}{label}{RESET}  {score_fc}{' ' * (6 - len(score_s))}  {bar}  "
              f"{tests_fc}{' ' * max(0, 6 - len(tests_s))}  {dur_s:>6}  {notes}")

    # Footer separator
    print(f"{DIM}{sep}{RESET}")

    # Delta vs prev
    if prev and "overall" in prev:
        prev_score = prev["overall"].get("score", 0)
        prev_ts    = fmt_ts(prev.get("timestamp", ""))
        ds         = delta_str(o_score, prev_score)
        print(f"  {DIM}vs {prev_ts}:{RESET}  overall {ds}", end="")

        # Per-suite deltas
        deltas = []
        for name, suite in suites.items():
            if name in prev.get("suites", {}):
                ps = prev["suites"][name].get("score", 0)
                cs = suite.get("score", 1.0)
                sk = suite.get("passed", 0) == 0 and suite.get("failed", 0) == 0
                short = name.split("-")[0]  # "01", "02" etc.
                if sk:
                    deltas.append(f"{DIM}{short}:skip{RESET}")
                else:
                    deltas.append(f"{DIM}{short}:{RESET}{delta_str(cs, ps)}")
        if deltas:
            print(f"  {'  '.join(deltas)}", end="")
        print()

    print()


# ── Multi-run sparkline history ────────────────────────────────────────────────

def render_history(runs: list[dict], max_runs: int = 8) -> None:
    runs = runs[:max_runs]
    if len(runs) < 2:
        print(f"  {DIM}(only one run found — run the bench more to see history){RESET}")
        return

    print(f"  {BOLD}Score history (newest → oldest):{RESET}")
    print()

    # Overall sparkline
    scores = [r.get("overall", {}).get("score", 1.0) for r in runs]
    sparks = []
    for s in scores:
        c = color_score(s)
        bar = mini_bar(s)
        sparks.append(f"{c}{bar}{RESET}")
    print(f"  {DIM}Overall  {RESET}" + f"  {DIM}|{RESET}  ".join(sparks))

    # Per-suite sparklines
    all_suite_names = list(runs[0].get("suites", {}).keys())
    for name in all_suite_names:
        label = SUITE_LABELS.get(name, name).rstrip()
        suite_sparks = []
        for r in runs:
            suite = r.get("suites", {}).get(name, {})
            score = suite.get("score", 1.0)
            skip  = suite.get("passed", 0) == 0 and suite.get("failed", 0) == 0
            if skip:
                suite_sparks.append(f"{YELLOW}{'─' * 6}{RESET}")
            else:
                c = color_score(score)
                suite_sparks.append(f"{c}{mini_bar(score)}{RESET}")
        print(f"  {DIM}{label[:9]:<9}{RESET}" + f"  {DIM}|{RESET}  ".join(suite_sparks))

    # Timestamps row
    ts_row = [f"{DIM}{fmt_ts(r.get('timestamp',''))[:10]}{RESET}" for r in runs]
    print()
    print(f"  {' ' * 11}" + f"   ".join(ts_row))
    print()


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    if not METRICS_DIR.exists():
        print(f"{RED}No metrics directory found at {METRICS_DIR}{RESET}")
        print(f"{DIM}Run:  bash bench/run.sh{RESET}")
        sys.exit(1)

    jsons = sorted_jsons()
    if not jsons:
        print(f"{RED}No metrics files found.{RESET}")
        print(f"{DIM}Run:  bash bench/run.sh{RESET}")
        sys.exit(1)

    show_all    = "--all" in sys.argv
    suite_filt  = next((a.split("=")[1] for a in sys.argv if a.startswith("--suite=")), None)

    latest = load_json(jsons[0])
    prev   = load_json(jsons[1]) if len(jsons) > 1 else None

    render_run(latest, prev)

    if show_all and len(jsons) > 1:
        all_data = [load_json(p) for p in jsons]
        render_history(all_data)


if __name__ == "__main__":
    main()

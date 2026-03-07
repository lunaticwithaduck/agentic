#!/usr/bin/env python3
"""
bench/e2e/token_compare.py — Token Cost Measurement

Measures the token overhead of agentic infrastructure vs vanilla Claude.
Uses subprocess mode (real `claude -p` runs) — same approach as suite 04.

Token counts come from the `usage` field in claude's --output-format=json response.

For each task in tasks.json, runs both conditions:
  with-infra:    temp dir with .claude/ copied in (hooks fire, skills load)
  without-infra: fresh temp dir (vanilla Claude, no .claude/)

Metrics reported:
  input_overhead   — extra input tokens consumed by infra condition
  output_delta     — difference in output tokens (positive = infra generates more)
  cost_ratio       — total tokens (with-infra) / total tokens (without-infra)

Usage:
    python3 bench/e2e/token_compare.py [options]

Options:
    --task=eq01       Run only this task (default: all)
    --no-cache        Re-run even if cached results exist
    --json            Output JSON instead of human-readable
    --dry-run         Show what would run without making subprocess calls
    --model=X         Model to use (default: claude-sonnet-4-6)
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Optional
from datetime import datetime, timezone

# ── Paths ─────────────────────────────────────────────────────────────────────
BENCH_DIR        = Path(__file__).parent.parent
ROOT_DIR         = BENCH_DIR.parent
E2E_DIR          = Path(__file__).parent
RESULTS          = BENCH_DIR / "results" / "tokens"
FIXTURES_DIR     = E2E_DIR / "fixtures"

# ── Defaults ──────────────────────────────────────────────────────────────────
DEFAULT_MODEL      = "claude-sonnet-4-6"
CLAUDE_BIN         = "claude"
SUBPROCESS_TIMEOUT = 300


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


# ── Retry helper ─────────────────────────────────────────────────────────────

def _run_with_retry(fn, *args, log=None, **kwargs):
    """Call fn(*args, **kwargs), retrying once on RuntimeError (transient timeouts)."""
    try:
        return fn(*args, **kwargs)
    except RuntimeError as e:
        if log:
            log(f" failed ({e}), retrying...")
        else:
            print(f"  [retry] {e}", file=sys.stderr)
        return fn(*args, **kwargs)


# ── Subprocess runner (returns full JSON data, not just text) ─────────────────

def run_claude_subprocess_raw(prompt: str, work_dir: str, model: str) -> dict:
    """Run `claude -p` and return the full parsed JSON response dict.

    The response includes `result`, `usage`, `total_cost_usd`, etc.
    usage = {"input_tokens": N, "output_tokens": M}
    """
    cmd = [
        CLAUDE_BIN, "-p", prompt,
        "--output-format", "json",
        "--no-session-persistence",
        "--model", model,
        "--disallowedTools", "Bash,Edit,Write,NotebookEdit",
    ]
    env = {k: v for k, v in os.environ.items() if k != "CLAUDECODE"}

    try:
        result = subprocess.run(
            cmd,
            cwd=work_dir,
            stdin=subprocess.DEVNULL,
            capture_output=True,
            text=True,
            timeout=SUBPROCESS_TIMEOUT,
            env=env,
        )
    except FileNotFoundError:
        raise RuntimeError(f"'{CLAUDE_BIN}' not found in PATH. Ensure Claude Code is installed.")
    except subprocess.TimeoutExpired:
        raise RuntimeError(f"claude subprocess timed out after {SUBPROCESS_TIMEOUT}s")

    if result.returncode != 0:
        stderr = (result.stderr or "")[:500]
        raise RuntimeError(f"claude subprocess exited {result.returncode}: {stderr}")

    try:
        data = json.loads(result.stdout)
        if isinstance(data, dict) and data.get("is_error"):
            raise RuntimeError(f"claude reported an error: {data.get('result', '')}")
        return data if isinstance(data, dict) else {"result": result.stdout.strip()}
    except json.JSONDecodeError:
        return {"result": result.stdout.strip()}


def extract_usage(data: dict) -> dict:
    """Extract token counts from a claude -p JSON response."""
    usage = data.get("usage") or {}
    return {
        "input_tokens":  usage.get("input_tokens", 0),
        "output_tokens": usage.get("output_tokens", 0),
    }


# ── Run helpers (mirrors compare.py) ─────────────────────────────────────────

def _copy_fixtures(fixture_dir: Path, dest: str) -> None:
    for item in fixture_dir.iterdir():
        target = Path(dest) / item.name
        if item.is_dir():
            shutil.copytree(str(item), str(target))
        else:
            shutil.copy2(str(item), str(target))


def run_with_infra(prompt: str, model: str, fixture_dir: Optional[Path] = None) -> dict:
    """Run in temp dir with .claude/ copied in. Returns raw JSON response."""
    claude_src = ROOT_DIR / ".claude"
    with tempfile.TemporaryDirectory(prefix="tok-infra-") as tmpdir:
        if claude_src.exists():
            shutil.copytree(
                str(claude_src),
                str(Path(tmpdir) / ".claude"),
                ignore=shutil.ignore_patterns("settings.local.json"),
            )
        if fixture_dir and fixture_dir.exists():
            _copy_fixtures(fixture_dir, tmpdir)
        return run_claude_subprocess_raw(prompt, tmpdir, model)


def run_without_infra(prompt: str, model: str, fixture_dir: Optional[Path] = None) -> dict:
    """Run in a fresh temp dir — vanilla Claude, no .claude/."""
    with tempfile.TemporaryDirectory(prefix="tok-ctrl-") as tmpdir:
        if fixture_dir and fixture_dir.exists():
            _copy_fixtures(fixture_dir, tmpdir)
        return run_claude_subprocess_raw(prompt, tmpdir, model)


# ── Caching ───────────────────────────────────────────────────────────────────

def task_dir(task_id: str) -> Path:
    d = RESULTS / task_id
    d.mkdir(parents=True, exist_ok=True)
    return d


def load_cache(task_id: str) -> Optional[dict]:
    path = task_dir(task_id) / "tokens.json"
    return json.loads(path.read_text()) if path.exists() else None


def save_cache(task_id: str, data: dict) -> None:
    (task_dir(task_id) / "tokens.json").write_text(json.dumps(data, indent=2))


# ── Main ──────────────────────────────────────────────────────────────────────

def run(args: argparse.Namespace) -> dict:
    tasks = json.loads((E2E_DIR / "tasks.json").read_text())
    if args.task:
        tasks = [t for t in tasks if t["id"] == args.task]
        if not tasks:
            print(f"Task '{args.task}' not found.", file=sys.stderr)
            sys.exit(1)

    model = args.model

    def log(msg: str, end: str = "\n") -> None:
        print(msg, end=end, flush=True, file=sys.stderr)

    if not args.json:
        print(f"\n  Token cost measurement  |  Tasks: {len(tasks)}  |  Model: {model}")
        print(f"  Results cached in bench/results/tokens/ — use --no-cache to force re-run")
        if args.dry_run:
            print("  \033[33mDRY RUN — no subprocess calls will be made\033[0m")
    print("", file=sys.stderr)

    results = []

    for i, task in enumerate(tasks, 1):
        task_id = task["id"]

        cached = load_cache(task_id)
        if cached and not args.no_cache:
            log(f"  [{i}/{len(tasks)}] {task['title']} — using cached token data")
            result = cached
        elif args.dry_run:
            result = {
                "task_id":        task_id,
                "title":          task["title"],
                "with_input":     0,
                "with_output":    0,
                "without_input":  0,
                "without_output": 0,
                "input_overhead": 0,
                "output_delta":   0,
                "cost_ratio":     0.0,
                "dry_run":        True,
            }
        else:
            fixture_dir: Optional[Path] = None
            if task.get("fixture_dir"):
                fixture_dir = FIXTURES_DIR / task["fixture_dir"]

            log(f"  [{i}/{len(tasks)}] {task['title']}")
            try:
                log(f"    → with-infra:    calling claude...", end=" ")
                with_data  = _run_with_retry(run_with_infra, task["prompt"], model, fixture_dir, log=log)
                with_usage = extract_usage(with_data)
                log("done")

                log(f"    → without-infra: calling claude...", end=" ")
                wo_data    = _run_with_retry(run_without_infra, task["prompt"], model, fixture_dir, log=log)
                wo_usage   = extract_usage(wo_data)
                log("done")
            except RuntimeError as e:
                log(f"FAILED")
                log(f"    → error: {e}")
                log(f"    → skipping task {task_id}")
                continue

            with_total = with_usage["input_tokens"]  + with_usage["output_tokens"]
            wo_total   = wo_usage["input_tokens"]    + wo_usage["output_tokens"]

            result = {
                "task_id":        task_id,
                "title":          task["title"],
                "with_input":     with_usage["input_tokens"],
                "with_output":    with_usage["output_tokens"],
                "without_input":  wo_usage["input_tokens"],
                "without_output": wo_usage["output_tokens"],
                "input_overhead": with_usage["input_tokens"] - wo_usage["input_tokens"],
                "output_delta":   with_usage["output_tokens"] - wo_usage["output_tokens"],
                "cost_ratio":     round(with_total / wo_total, 3) if wo_total else 0.0,
            }
            save_cache(task_id, result)

        results.append(result)

        if not args.json:
            r = result
            sign = "+" if r["output_delta"] >= 0 else ""
            if r.get("dry_run"):
                print(f"  [{task_id}] {task['title'][:45]:<45}  [dry run]")
            else:
                print(
                    f"  [{task_id}] {task['title'][:45]:<45}  "
                    f"in +{r['input_overhead']:>6,}  "
                    f"out {sign}{r['output_delta']:>5,}  "
                    f"ratio {r['cost_ratio']:.2f}x"
                )

    if not results:
        return {"error": "no results"}

    real = [r for r in results if not r.get("dry_run")]
    avg_overhead  = sum(r["input_overhead"] for r in real) / len(real) if real else 0
    avg_out_delta = sum(r["output_delta"]   for r in real) / len(real) if real else 0
    avg_ratio     = sum(r["cost_ratio"]     for r in real) / len(real) if real else 0

    summary = {
        "timestamp":          utcnow().isoformat() + "Z",
        "model":              model,
        "task_count":         len(results),
        "avg_input_overhead": round(avg_overhead, 1),
        "avg_output_delta":   round(avg_out_delta, 1),
        "avg_cost_ratio":     round(avg_ratio, 3),
        "tasks":              results,
    }

    if not args.json:
        print(f"\n  {'━'*50}")
        sign = "+" if avg_out_delta >= 0 else ""
        if real:
            print(f"  Avg input overhead:  +{avg_overhead:,.0f} tokens (infra system prompt)")
            print(f"  Avg output delta:    {sign}{avg_out_delta:,.0f} tokens")
            print(f"  Avg cost ratio:      {avg_ratio:.2f}x  (1.0 = no overhead)")
        else:
            print("  (dry run — no real token data)")
    else:
        print(json.dumps(summary, indent=2))

    RESULTS.mkdir(parents=True, exist_ok=True)
    out_path = RESULTS / f"summary-{utcnow().strftime('%Y-%m-%dT%H-%M-%S')}.json"
    out_path.write_text(json.dumps(summary, indent=2))

    return summary


def main():
    parser = argparse.ArgumentParser(description="Token cost measurement for agentic infrastructure")
    parser.add_argument("--task",     help="Run only this task ID")
    parser.add_argument("--no-cache", action="store_true", help="Re-run even if cached")
    parser.add_argument("--json",     action="store_true", help="Output JSON")
    parser.add_argument("--dry-run",  action="store_true", help="No subprocess calls")
    parser.add_argument("--model",    default=DEFAULT_MODEL)
    args = parser.parse_args()
    run(args)


if __name__ == "__main__":
    main()

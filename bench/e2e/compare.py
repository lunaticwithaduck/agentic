#!/usr/bin/env python3
"""
bench/e2e/compare.py — E2E Task Quality Comparison

Compares Claude responses WITH vs WITHOUT agentic infrastructure context.
Uses the Anthropic API directly via urllib (no dependencies).

Usage:
    python3 bench/e2e/compare.py [options]

Options:
    --task=eq01          Run only this task (default: all)
    --no-cache           Re-run API calls even if cached results exist
    --judge-only         Re-judge cached responses without re-running tasks
    --response-model=X  Model for generating responses (default: claude-sonnet-4-6)
    --judge-model=X     Model for judging responses (default: claude-haiku-4-5-20251001)
    --dry-run           Show what would run without calling API
    --json              Output JSON instead of human-readable

Requires:
    ANTHROPIC_API_KEY environment variable
"""

import argparse
import json
import os
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path
from datetime import datetime, timezone

def utcnow() -> datetime:
    return datetime.now(timezone.utc)

# ── Paths ─────────────────────────────────────────────────────────────────────
BENCH_DIR = Path(__file__).parent.parent
ROOT_DIR  = BENCH_DIR.parent
E2E_DIR   = Path(__file__).parent
RESULTS   = BENCH_DIR / "results" / "e2e"
SKILL_DIR = ROOT_DIR / ".claude" / "skills"
CLAUDE_MD = ROOT_DIR / "CLAUDE.md"

# ── Defaults ──────────────────────────────────────────────────────────────────
DEFAULT_RESPONSE_MODEL = "claude-sonnet-4-6"
DEFAULT_JUDGE_MODEL    = "claude-haiku-4-5-20251001"
ANTHROPIC_VERSION      = "2023-06-01"
API_URL                = "https://api.anthropic.com/v1/messages"


# ── API helpers ───────────────────────────────────────────────────────────────

def api_call(messages: list, system: str, model: str, max_tokens: int = 2048) -> str:
    """Call the Anthropic messages API via urllib."""
    api_key = os.environ.get("ANTHROPIC_API_KEY", "")
    if not api_key:
        raise RuntimeError("ANTHROPIC_API_KEY environment variable not set")

    payload = json.dumps({
        "model":      model,
        "max_tokens": max_tokens,
        "system":     system,
        "messages":   messages,
    }).encode()

    req = urllib.request.Request(
        API_URL,
        data=payload,
        headers={
            "x-api-key":         api_key,
            "anthropic-version": ANTHROPIC_VERSION,
            "content-type":      "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = json.loads(resp.read())
            return data["content"][0]["text"]
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        raise RuntimeError(f"API error {e.code}: {body}") from e


# ── Infrastructure context builders ───────────────────────────────────────────

def load_skill(skill_name: str) -> str:
    path = SKILL_DIR / f"{skill_name}.md"
    return path.read_text() if path.exists() else ""


def build_with_infra_system(expected_skills: list[str]) -> str:
    """System prompt simulating the agentic infrastructure context."""
    claude_md_content = CLAUDE_MD.read_text() if CLAUDE_MD.exists() else ""

    skill_sections = []
    for skill in expected_skills:
        content = load_skill(skill)
        if content:
            skill_sections.append(f"### Active Skill: `{skill}`\n{content}")

    skill_block = "\n\n".join(skill_sections) if skill_sections else "(no skills loaded)"

    return f"""You are Claude Code, Anthropic's official CLI for Claude, an expert AI for software engineering tasks.

## Infrastructure Context

{claude_md_content}

## Active Skills for This Task

The following specialized skills are active. Apply their frameworks and expertise:

{skill_block}

Use the domain expertise, checklists, and patterns from these skills in your response."""


def build_without_infra_system() -> str:
    """Bare system prompt — no infrastructure context."""
    return "You are a helpful AI assistant for software development tasks."


# ── Caching ───────────────────────────────────────────────────────────────────

def task_dir(task_id: str) -> Path:
    d = RESULTS / task_id
    d.mkdir(parents=True, exist_ok=True)
    return d


def load_cache(task_id: str, condition: str) -> str | None:
    path = task_dir(task_id) / f"{condition}.txt"
    return path.read_text() if path.exists() else None


def save_cache(task_id: str, condition: str, content: str) -> None:
    path = task_dir(task_id) / f"{condition}.txt"
    path.write_text(content)


def load_judgement(task_id: str) -> dict | None:
    path = task_dir(task_id) / "judgement.json"
    if path.exists():
        return json.loads(path.read_text())
    return None


def save_judgement(task_id: str, data: dict) -> None:
    path = task_dir(task_id) / "judgement.json"
    path.write_text(json.dumps(data, indent=2))


# ── Scoring ───────────────────────────────────────────────────────────────────

def judge_responses(
    task: dict,
    with_resp: str,
    without_resp: str,
    judge_model: str,
) -> dict:
    """Use an LLM judge to score both responses against the task rubric."""
    rubric_lines = "\n".join(
        f"- **{r['dimension']}** (weight {r['weight']}): {r['description']}"
        for r in task["rubric"]
    )

    judge_prompt = f"""You are an expert software engineering evaluator. Score two AI responses to a coding task.

## Task
{task['prompt']}

## Rubric dimensions (score each 1–5, where 1=poor and 5=excellent):
{rubric_lines}

## Response A (generated WITH specialized infrastructure context)
<response_a>
{with_resp}
</response_a>

## Response B (generated WITHOUT specialized infrastructure context)
<response_b>
{without_resp}
</response_b>

Score each response on EACH rubric dimension independently. Then pick a winner.

Respond ONLY with valid JSON matching this exact schema (no markdown fences):
{{
  "response_a": {{"<dimension_name>": <score_1_to_5>, ...}},
  "response_b": {{"<dimension_name>": <score_1_to_5>, ...}},
  "winner": "A" | "B" | "TIE",
  "reasoning": "<2-3 sentence explanation of the key difference>"
}}"""

    raw = api_call(
        messages=[{"role": "user", "content": judge_prompt}],
        system="You are an impartial evaluator. You must respond with valid JSON only.",
        model=judge_model,
        max_tokens=1024,
    )

    # Strip markdown fences if present
    text = raw.strip()
    if "```json" in text:
        text = text.split("```json")[1].split("```")[0].strip()
    elif "```" in text:
        text = text.split("```")[1].split("```")[0].strip()

    return json.loads(text)


def compute_weighted_score(rubric: list, scores: dict) -> float:
    """Compute weighted average score from rubric weights and per-dimension scores."""
    total_weight = sum(r["weight"] for r in rubric)
    if total_weight == 0:
        return 0.0
    weighted = sum(
        r["weight"] * scores.get(r["dimension"], 3)  # default 3 if dimension missing
        for r in rubric
    )
    return weighted / total_weight


# ── Display ───────────────────────────────────────────────────────────────────

def bar(score: float, max_score: float = 5.0, width: int = 10) -> str:
    filled = round((score / max_score) * width)
    return "█" * filled + "░" * (width - filled)


def print_task_result(task: dict, judgement: dict, with_score: float, without_score: float) -> None:
    winner = judgement.get("winner", "TIE")
    delta  = with_score - without_score
    delta_str = f"{'+' if delta >= 0 else ''}{delta:.2f}"
    winner_label = {
        "A":   "\033[32m✓ WITH INFRA wins\033[0m",
        "B":   "\033[31m✗ WITHOUT INFRA wins\033[0m",
        "TIE": "\033[33m~ TIE\033[0m",
    }.get(winner, winner)

    print(f"\n  [{task['id']}] {task['title']}")
    print(f"    With infra:    {with_score:.2f}/5.00  {bar(with_score)}")
    print(f"    Without infra: {without_score:.2f}/5.00  {bar(without_score)}")
    print(f"    {winner_label}  ({delta_str})")
    reasoning = judgement.get("reasoning", "")
    if reasoning:
        # Word-wrap at 72 chars
        words = reasoning.split()
        line, lines = "", []
        for w in words:
            if len(line) + len(w) + 1 > 72:
                lines.append(line)
                line = w
            else:
                line = (line + " " + w).strip()
        if line:
            lines.append(line)
        for l in lines:
            print(f"    \033[2m{l}\033[0m")


# ── Main ──────────────────────────────────────────────────────────────────────

def run(args: argparse.Namespace) -> dict:
    if not os.environ.get("ANTHROPIC_API_KEY"):
        print("\033[31mERROR: ANTHROPIC_API_KEY not set.\033[0m", file=sys.stderr)
        print("Export it and re-run:  export ANTHROPIC_API_KEY=sk-ant-...", file=sys.stderr)
        sys.exit(2)

    tasks = json.loads((E2E_DIR / "tasks.json").read_text())
    if args.task:
        tasks = [t for t in tasks if t["id"] == args.task]
        if not tasks:
            print(f"Task '{args.task}' not found.", file=sys.stderr)
            sys.exit(1)

    response_model = args.response_model
    judge_model    = args.judge_model

    if not args.json:
        print(f"\n  Tasks: {len(tasks)}  |  Response model: {response_model}  |  Judge: {judge_model}")
        if args.dry_run:
            print("  \033[33mDRY RUN — no API calls will be made\033[0m")

    results = []

    for task in tasks:
        task_id = task["id"]

        # ── Step 1: Get responses ─────────────────────────────────────────────
        if not args.judge_only:
            # WITH infrastructure
            with_cached = load_cache(task_id, "with-infra")
            if with_cached and not args.no_cache:
                with_resp = with_cached
                if not args.json:
                    print(f"\n  [{task_id}] {task['title']} — using cached responses")
            else:
                if not args.json:
                    print(f"\n  [{task_id}] {task['title']} — calling API...", end=" ", flush=True)
                if args.dry_run:
                    with_resp = "[DRY RUN — with infra response]"
                else:
                    with_resp = api_call(
                        messages=[{"role": "user", "content": task["prompt"]}],
                        system=build_with_infra_system(task.get("expected_skills", [])),
                        model=response_model,
                    )
                if not args.dry_run:
                    save_cache(task_id, "with-infra", with_resp)

                # WITHOUT infrastructure
                if args.dry_run:
                    without_resp = "[DRY RUN — without infra response]"
                else:
                    without_resp = api_call(
                        messages=[{"role": "user", "content": task["prompt"]}],
                        system=build_without_infra_system(),
                        model=response_model,
                    )
                    save_cache(task_id, "without-infra", without_resp)
                if not args.json:
                    print("done")
        else:
            # Judge-only mode: load from cache
            with_resp    = load_cache(task_id, "with-infra") or ""
            without_resp = load_cache(task_id, "without-infra") or ""
            if not with_resp or not without_resp:
                if not args.json:
                    print(f"\n  [{task_id}] {task['title']} — no cached responses, skipping")
                continue

        # ── Step 2: Judge ─────────────────────────────────────────────────────
        judgement_cached = load_judgement(task_id)
        if judgement_cached and not args.no_cache:
            judgement = judgement_cached
        else:
            if not args.json and not args.dry_run:
                print(f"  [{task_id}] judging...", end=" ", flush=True)
            if args.dry_run:
                judgement = {
                    "response_a": {r["dimension"]: 4 for r in task["rubric"]},
                    "response_b": {r["dimension"]: 3 for r in task["rubric"]},
                    "winner": "A",
                    "reasoning": "Dry run placeholder.",
                }
            else:
                judgement = judge_responses(task, with_resp, without_resp, judge_model)
            if not args.dry_run:
                save_judgement(task_id, judgement)
            if not args.json and not args.dry_run:
                print("done")

        # ── Step 3: Score ─────────────────────────────────────────────────────
        with_score    = compute_weighted_score(task["rubric"], judgement.get("response_a", {}))
        without_score = compute_weighted_score(task["rubric"], judgement.get("response_b", {}))

        result = {
            "task_id":       task_id,
            "title":         task["title"],
            "with_score":    round(with_score, 3),
            "without_score": round(without_score, 3),
            "delta":         round(with_score - without_score, 3),
            "winner":        judgement.get("winner", "TIE"),
            "reasoning":     judgement.get("reasoning", ""),
        }
        results.append(result)

        if not args.json:
            print_task_result(task, judgement, with_score, without_score)

    # ── Aggregate summary ─────────────────────────────────────────────────────
    if not results:
        return {"error": "no results"}

    infra_wins  = sum(1 for r in results if r["winner"] == "A")
    vanilla_wins = sum(1 for r in results if r["winner"] == "B")
    ties        = sum(1 for r in results if r["winner"] == "TIE")
    avg_delta   = sum(r["delta"] for r in results) / len(results)
    avg_with    = sum(r["with_score"] for r in results) / len(results)
    avg_without = sum(r["without_score"] for r in results) / len(results)
    infra_win_rate = (infra_wins + ties) / len(results)  # ties count as half-win

    summary = {
        "timestamp":      utcnow().isoformat() + "Z",
        "task_count":     len(results),
        "infra_wins":     infra_wins,
        "vanilla_wins":   vanilla_wins,
        "ties":           ties,
        "infra_win_rate": round(infra_win_rate, 3),
        "avg_with_score":    round(avg_with, 3),
        "avg_without_score": round(avg_without, 3),
        "avg_delta":         round(avg_delta, 3),
        "pass":           infra_win_rate >= 0.60,
        "tasks":          results,
        "response_model": response_model,
        "judge_model":    judge_model,
    }

    if not args.json:
        print(f"\n  {'━'*40}")
        print(f"  Infrastructure wins: {infra_wins}/{len(results)} tasks")
        print(f"  Vanilla wins:        {vanilla_wins}/{len(results)} tasks")
        print(f"  Ties:                {ties}/{len(results)} tasks")
        print(f"  Avg score (with):    {avg_with:.2f}/5.00  {bar(avg_with)}")
        print(f"  Avg score (without): {avg_without:.2f}/5.00  {bar(avg_without)}")
        sign = "+" if avg_delta >= 0 else ""
        print(f"  Avg improvement:     {sign}{avg_delta:.2f} points")
        verdict = "\033[32mPASS\033[0m" if summary["pass"] else "\033[31mFAIL\033[0m"
        print(f"  Verdict:             {verdict}  (threshold: ≥60% tasks won/tied by infra)")
    else:
        print(json.dumps(summary, indent=2))

    # Save aggregate result
    out_path = RESULTS / f"summary-{utcnow().strftime('%Y-%m-%dT%H-%M-%S')}.json"
    out_path.write_text(json.dumps(summary, indent=2))

    return summary


def main():
    parser = argparse.ArgumentParser(description="E2E task quality comparison")
    parser.add_argument("--task", help="Run only this task ID")
    parser.add_argument("--no-cache", action="store_true", help="Re-run even if cached")
    parser.add_argument("--judge-only", action="store_true", help="Re-judge cached responses")
    parser.add_argument("--response-model", default=DEFAULT_RESPONSE_MODEL)
    parser.add_argument("--judge-model", default=DEFAULT_JUDGE_MODEL)
    parser.add_argument("--dry-run", action="store_true", help="No API calls")
    parser.add_argument("--json", action="store_true", help="Output JSON")
    args = parser.parse_args()

    result = run(args)
    if isinstance(result, dict) and not result.get("pass", True):
        sys.exit(1)


if __name__ == "__main__":
    main()

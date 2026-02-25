# bench/suites/05-keyword-overlap.sh
# Suite: Keyword Overlap Analysis
# Identifies keywords shared across multiple skills (primary cause of false positives
# in skill detection), computes per-skill exclusivity scores, and flags noisy skills.
# Sourced by bench/run.sh — has access to ROOT_DIR, print_*, SUITE_PASSED, SUITE_FAILED.

print_suite "05 · Keyword Overlap"

SUITE_PASSED=0
SUITE_FAILED=0
SUITE_JSON="{}"

RULES="$ROOT_DIR/.claude/skills/skill-rules.json"

if [ ! -f "$RULES" ]; then
  print_fail "skill-rules.json not found"
  return 0
fi

# ── Run analysis via python3 ──────────────────────────────────────────────────
ANALYSIS=$(python3 - "$RULES" <<'PYEOF'
import json, sys
from collections import defaultdict

rules_path = sys.argv[1]
with open(rules_path) as f:
    rules = json.load(f)

# Build keyword → [skills] mapping
keyword_to_skills = defaultdict(list)
for skill, data in rules.items():
    for kw in data.get("keywords", []):
        keyword_to_skills[kw.lower()].append(skill)

# Skills with the most keywords
skill_keyword_counts = {
    skill: len(data.get("keywords", []))
    for skill, data in rules.items()
}

# Per-keyword: how many skills share it
shared_keywords = {
    kw: skills for kw, skills in keyword_to_skills.items() if len(skills) > 1
}

# Top shared keywords (most skills)
top_shared = sorted(shared_keywords.items(), key=lambda x: len(x[1]), reverse=True)

# Per-skill exclusivity score:
# For each skill, avg(1/share_count) across all its keywords.
# Score = 1.0 → all keywords unique to that skill.
# Score near 0 → all keywords shared with many other skills.
skill_exclusivity = {}
for skill, data in rules.items():
    keywords = data.get("keywords", [])
    if not keywords:
        skill_exclusivity[skill] = 1.0
        continue
    scores = [1.0 / len(keyword_to_skills[kw.lower()]) for kw in keywords]
    skill_exclusivity[skill] = round(sum(scores) / len(scores), 4)

# Noisy skills: exclusivity < 0.6
noisy_skills = sorted(
    [(s, e) for s, e in skill_exclusivity.items() if e < 0.60],
    key=lambda x: x[1]
)

# Clean skills: exclusivity >= 0.9
clean_skills = sorted(
    [(s, e) for s, e in skill_exclusivity.items() if e >= 0.90],
    key=lambda x: x[1], reverse=True
)

# Total unique keywords vs total keyword entries
total_entries = sum(len(d.get("keywords", [])) for d in rules.values())
total_unique  = len(keyword_to_skills)
duplication_rate = round(1 - total_unique / total_entries, 4) if total_entries > 0 else 0

print(json.dumps({
    "total_skills": len(rules),
    "total_keyword_entries": total_entries,
    "total_unique_keywords": total_unique,
    "duplication_rate": duplication_rate,
    "shared_keyword_count": len(shared_keywords),
    "top_shared": [[kw, skills] for kw, skills in top_shared[:20]],
    "noisy_skills": noisy_skills,
    "clean_skills": clean_skills,
    "skill_exclusivity": dict(sorted(skill_exclusivity.items(), key=lambda x: x[1])),
    "skill_keyword_counts": skill_keyword_counts,
}))
PYEOF
)

if [ -z "$ANALYSIS" ]; then
  print_fail "Analysis script returned no output"
  return 0
fi

# ── Display results ───────────────────────────────────────────────────────────
python3 - "$ANALYSIS" <<'PYEOF'
import json, sys

data = json.loads(sys.argv[1])

total_skills     = data["total_skills"]
total_entries    = data["total_keyword_entries"]
total_unique     = data["total_unique_keywords"]
dup_rate         = data["duplication_rate"]
shared_count     = data["shared_keyword_count"]
top_shared       = data["top_shared"]
noisy            = data["noisy_skills"]
clean            = data["clean_skills"]
exclusivity      = data["skill_exclusivity"]

print(f"  Skills: {total_skills}  |  Total keyword entries: {total_entries}  |  Unique keywords: {total_unique}")
print(f"  Shared keywords (appear in >1 skill): {shared_count}/{total_unique} ({shared_count/total_unique:.1%})")
print(f"  Overall duplication rate: {dup_rate:.1%}")
print()

# Top shared keywords
print("  Most-shared keywords (cross-contamination sources):")
print(f"  {'Keyword':<35} {'Shared by':>4}  Skills")
print(f"  {'-'*35} {'----':>4}  ------")
for kw, skills in top_shared[:12]:
    skills_str = ", ".join(skills[:5])
    if len(skills) > 5:
        skills_str += f" (+{len(skills)-5} more)"
    print(f"  {kw:<35} {len(skills):>4}  {skills_str}")

print()
print("  Per-skill exclusivity (0=all keywords shared, 1=all unique):")
print(f"  {'Skill':<30} {'Score':>6}  {'Keywords':>8}  Status")
print(f"  {'-'*30} {'------':>6}  {'--------':>8}  ------")
kw_counts = data["skill_keyword_counts"]
for skill, score in exclusivity.items():
    status = "✓ clean" if score >= 0.90 else ("△ ok" if score >= 0.60 else "✗ noisy")
    kw_count = kw_counts.get(skill, 0)
    print(f"  {skill:<30} {score:>6.3f}  {kw_count:>8}  {status}")
PYEOF

echo ""

# ── Extract metrics for thresholds ───────────────────────────────────────────
DUP_RATE=$(echo "$ANALYSIS"     | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['duplication_rate'])")
NOISY_COUNT=$(echo "$ANALYSIS"  | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['noisy_skills']))")
TOTAL_SKILLS=$(echo "$ANALYSIS" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['total_skills'])")

# Threshold 1: duplication rate < 30% (manageable keyword sharing)
python3 -c "import sys; sys.exit(0 if $DUP_RATE < 0.30 else 1)" 2>/dev/null \
  && print_pass "Keyword duplication rate $(python3 -c "print(f'{$DUP_RATE:.1%}')" 2>/dev/null) < 30%" \
  || print_fail "Keyword duplication rate $(python3 -c "print(f'{$DUP_RATE:.1%}')" 2>/dev/null) >= 30% — too much cross-contamination"

# Threshold 2: noisy skills (exclusivity < 0.6) <= 20% of all skills
NOISY_THRESHOLD=$(python3 -c "print(int($TOTAL_SKILLS * 0.20))")
python3 -c "import sys; sys.exit(0 if $NOISY_COUNT <= $NOISY_THRESHOLD else 1)" 2>/dev/null \
  && print_pass "Noisy skills ($NOISY_COUNT) within 20% threshold (≤$NOISY_THRESHOLD)" \
  || print_fail "Too many noisy skills ($NOISY_COUNT > $NOISY_THRESHOLD) — review shared keywords"

# Threshold 3: every skill has at least 3 keywords
LOW_KW=$(echo "$ANALYSIS" | python3 -c "
import json, sys
d = json.load(sys.stdin)
low = [s for s, c in d['skill_keyword_counts'].items() if c < 3]
print(len(low))
")
if [ "$LOW_KW" -eq 0 ]; then
  print_pass "All skills have >= 3 keywords"
else
  print_fail "$LOW_KW skill(s) have fewer than 3 keywords (insufficient coverage)"
fi

# Threshold 4: no single keyword shared by more than 5 skills
MAX_SHARED=$(echo "$ANALYSIS" | python3 -c "
import json, sys
d = json.load(sys.stdin)
top = d['top_shared']
print(len(top[0][1]) if top else 0)
")
python3 -c "import sys; sys.exit(0 if $MAX_SHARED <= 5 else 1)" 2>/dev/null \
  && print_pass "No keyword shared by more than 5 skills (max: $MAX_SHARED)" \
  || print_fail "A keyword is shared by $MAX_SHARED skills — consider specializing it"

# ── Emit SUITE_JSON ───────────────────────────────────────────────────────────
SUITE_JSON=$(echo "$ANALYSIS" | python3 -c "import json,sys; d=json.load(sys.stdin); print(json.dumps({'total_skills':d['total_skills'],'total_unique_keywords':d['total_unique_keywords'],'duplication_rate':d['duplication_rate'],'shared_keyword_count':d['shared_keyword_count'],'noisy_skill_count':len(d['noisy_skills'])}))")

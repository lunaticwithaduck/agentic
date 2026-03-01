# Problem: Keyword False Positives

**Date identified:** 2026-03-01
**Status:** Open

---

## The Problem

Skill detection uses substring matching against the user's prompt. Common domain words like
"infrastructure", "cache", "docker", "security", "deployment" appear in conversational context
(e.g., "I'm going to implement our infrastructure in a test project") and trigger skill injection
when no domain expertise was requested.

## Why It Matters

False positives waste tokens injecting irrelevant skill content and erode user trust. If the
system visibly activates a Terraform guide when the user is having a project planning conversation,
it looks broken. In a demo or enterprise evaluation context, this is a credibility issue.

The current substring approach trades precision for recall — it catches every genuine request
but also fires on casual mentions of domain terms.

## Dimensions of the Problem

1. **Single-word keywords are the worst offenders** — "cache", "cloud", "docker", "security",
   "deployment", "infrastructure" all appear in normal conversation
2. **No intent signal** — the matcher can't distinguish "help me write a Dockerfile" (intent)
   from "we use Docker in production" (mention)
3. **Multi-word keywords help but don't solve it** — "sql optimization" is better than "sql",
   but "I was thinking about sql optimization" is still a false positive
4. **Compounding cost** — multiple skills can fire on one prompt, amplifying the waste

## Approaches Worth Exploring

- Require 2+ keyword matches (not just one) before activating a skill
- Weight multi-word phrases higher than single words; require a threshold score
- Add negative keywords or exclusion patterns (e.g., don't fire if prompt is a question about
  the project itself rather than a technical request)
- Use prompt length or structure as a signal — very short conversational messages are unlikely
  to be technical requests
- Accept some false positives as the cost of zero-miss recall and focus on minimizing token
  waste per false activation (e.g., inject a summary instead of the full skill)

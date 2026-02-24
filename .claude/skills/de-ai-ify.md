---
name: de-ai-ify
description: Remove AI-generated phrasing and replace with plain, natural language
activation:
  keywords: ["de-ai", "deai", "remove ai", "plain language", "simplify writing", "ai phrasing", "sound human"]
  file_patterns: []
---

# De-AI-ify Text

## Purpose
Scan text for common AI-generated phrasing patterns and rewrite with plain, natural language while preserving the original meaning and tone.

## Instructions

1. **Scan for AI tells** -- flag these words and phrases:
   - **Overused words**: delve, tapestry, landscape, leverage, utilize, pivotal, multifaceted, streamline, robust, cutting-edge, game-changer, holistic, synergy, paradigm, revolutionize, unprecedented, foster, empower, harness, spearhead, cornerstone, underscore, facilitate
   - **Filler phrases**: "It's important to note that", "In today's rapidly evolving", "At its core", "In the realm of", "A testament to", "Serves as a", "It's worth noting", "When it comes to"
   - **Hedging**: "It could potentially", "This might arguably", "In many ways", "To some extent"
   - **Overly formal connectors**: "Furthermore", "Moreover", "Additionally" (when overused), "In conclusion"
   - **Empty superlatives**: "truly remarkable", "incredibly powerful", "absolutely essential"

2. **Replace with plain equivalents**:
   - "leverage" -> "use"
   - "utilize" -> "use"
   - "facilitate" -> "help" or "enable"
   - "streamline" -> "simplify" or describe what specifically improves
   - "robust" -> "reliable" or "strong" or describe what makes it so
   - "cutting-edge" -> "new" or "modern" or describe what is novel
   - "delve" -> "look at" or "explore" or "examine"
   - "landscape" -> "field" or "area" or be specific
   - "holistic" -> "complete" or "overall" or describe the scope
   - "paradigm" -> "model" or "approach" or be specific

3. **Simplify sentence structure**:
   - Break long compound sentences into shorter ones
   - Remove unnecessary subordinate clauses
   - Put the subject and verb close together
   - Prefer concrete subjects over abstract ones
   - Remove phrases that add no information

4. **Preserve**:
   - The original meaning and all factual content
   - The intended tone (formal/informal/technical)
   - Technical terms that are accurate and necessary
   - The author's voice where it comes through

5. **Do not**:
   - Make the text too casual if the original is professional
   - Remove technical accuracy for the sake of simplicity
   - Change the structure beyond what is needed
   - Add new information or opinions

## Output Format

Return the full rewritten text. Optionally, follow with a brief list of the most significant changes made if the user wants to review them.

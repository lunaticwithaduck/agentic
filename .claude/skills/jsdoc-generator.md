---
name: jsdoc-generator
description: Generate JSDoc, docstrings, or GoDoc documentation for functions and classes
activation:
  keywords: ["jsdoc", "docstring", "godoc", "document functions", "generate docs", "type docs"]
  file_patterns: ["**/*.js", "**/*.ts", "**/*.py", "**/*.go", "**/*.java"]
---

# JSDoc/Docstring Generator

## Purpose
Analyze function signatures, class definitions, and method bodies to generate accurate documentation comments in the appropriate format for the language.

## Instructions

1. **Detect the language and convention**:
   - JavaScript/TypeScript: JSDoc (`/** ... */`)
   - Python: Docstrings (Google, NumPy, or Sphinx style -- match existing project style)
   - Go: GoDoc (comment block above the declaration)
   - Java/Kotlin: Javadoc (`/** ... */`)
   - Rust: Doc comments (`///` or `//!`)
   - If the project already has doc comments, match that exact style

2. **Analyze each function/method**:
   - Read the function name, parameters, and return type
   - Infer types from usage if not explicitly typed
   - Read the function body to understand behavior, side effects, and error conditions
   - Check call sites for additional context on expected usage

3. **Write the documentation**:
   - First line: concise summary of what the function does (imperative mood: "Create", "Return", "Validate")
   - Extended description if behavior is non-obvious
   - `@param` / `:param:` for each parameter with type and description
   - `@returns` / `:returns:` with type and description
   - `@throws` / `:raises:` for each exception with condition
   - `@example` with a realistic, runnable code snippet
   - `@deprecated` if applicable, with migration path
   - `@see` for related functions or documentation

4. **Handle complex types**:
   - Document generic type parameters (`@template T`)
   - Document callback signatures inline
   - Document object shapes for config/options parameters
   - Use union types and nullable annotations accurately

5. **For classes**:
   - Document the class purpose and usage pattern
   - Document constructor parameters
   - Document public properties
   - Group methods logically in the documentation

## Output Format

Return the code with doc comments inserted directly above each function, class, or method. Preserve all existing code and formatting. Do not modify function bodies.

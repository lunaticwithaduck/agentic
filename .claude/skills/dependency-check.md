---
name: dependency-check
description: Comprehensive dependency health check for outdated, vulnerable, or unnecessary packages
activation:
  keywords: ["dependency check", "outdated packages", "dependency audit", "package health", "dependency review"]
  file_patterns: ["**/package.json", "**/requirements.txt", "**/Cargo.toml", "**/go.mod", "**/Gemfile", "**/pom.xml"]
---

# Dependency Health Check

## Purpose
Analyze all project dependencies for security, maintenance, licensing,
and overall health. Recommend updates, removals, and replacements.

## Instructions

1. **List All Dependencies**
   - Identify all dependency manifests in the project
   - Distinguish between production and development dependencies
   - Note pinned versions vs ranges

2. **Check for Outdated Versions**
   - Run the appropriate tool (`npm outdated`, `pip list --outdated`, etc.)
   - Categorize: patch updates, minor updates, major updates
   - Highlight packages more than 1 major version behind

3. **Check for Known Vulnerabilities**
   - Run security audit tools for the ecosystem
   - Flag any dependency with a known CVE
   - Note severity and whether a fix version exists

4. **Identify Unmaintained Packages**
   - Check last publish/release date for each dependency
   - Flag packages with no updates in over 1 year
   - Check for archived or deprecated repositories
   - Look for "looking for maintainer" notices

5. **License Compatibility**
   - List the license of each dependency
   - Flag copyleft licenses (GPL, AGPL) that may conflict
   - Identify packages with no license (legally risky)
   - Verify license compatibility with the project's license

6. **Detect Duplicates and Unnecessary Dependencies**
   - Find duplicate packages at different versions
   - Identify dependencies that overlap in functionality
   - Flag dependencies that could be replaced with built-in features
   - Check for packages used only once (consider inlining)

7. **Recommend Actions**
   - Prioritize updates by: security fixes > major updates > minor > patch
   - Suggest drop-in replacements for unmaintained packages
   - Recommend removal of unused dependencies
   - Provide upgrade commands

## Output Format

```
# Dependency Health Report

## Summary
- Total dependencies: X (prod: X, dev: X)
- Outdated: X | Vulnerable: X | Unmaintained: X

## Action Required

### Critical (fix immediately)
- package@version: reason and fix command

### Recommended Updates
| Package | Current | Latest | Type | Notes |
|---------|---------|--------|------|-------|

### Unmaintained Packages
| Package | Last Updated | Alternative |
|---------|-------------|-------------|

### License Issues
| Package | License | Issue |
|---------|---------|-------|

### Recommended Removals
- package: reason for removal
```

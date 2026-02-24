---
name: changelog
description: Generate changelogs from git history in Keep a Changelog format
activation:
  keywords: ["changelog", "release notes", "what changed", "generate changelog"]
  file_patterns: ["CHANGELOG.md", "CHANGELOG*"]
---

# Changelog

## Purpose
Generate clear, categorized changelogs from git history following the Keep a Changelog format.

## Instructions

### 1. Determine the Range
- Ask for or identify the two refs to compare (e.g., v1.0.0..v1.1.0)
- If no range given, use the last tag to HEAD: `git describe --tags --abbrev=0`
- Run `git log <from>..<to> --oneline --no-merges` to get commits

### 2. Categorize Changes
Group each commit into one of these categories:
- **Added**: New features or capabilities
- **Changed**: Modifications to existing functionality
- **Fixed**: Bug fixes
- **Removed**: Removed features or deprecated items
- **Security**: Vulnerability fixes or security improvements
- **Deprecated**: Features marked for future removal

### 3. Group by Component
If the project has clear modules or components, group entries:
```
### Added
- **auth**: Add OAuth2 support for GitHub login
- **api**: Add pagination to list endpoints
```

### 4. Write Entries
- Start each entry with a verb in past tense ("Added", "Fixed")
- Be specific enough to be useful, brief enough to scan
- Reference issue/PR numbers where applicable
- Mention breaking changes prominently

### 5. Format
Follow Keep a Changelog (https://keepachangelog.com):
- Newest version at top
- Include release date
- Use semantic versioning

## Output Format
```markdown
# Changelog

## [version] - YYYY-MM-DD

### Added
- [description] (#PR)

### Changed
- [description] (#PR)

### Fixed
- [description] (#PR)

### Removed
- [description] (#PR)

### Security
- [description] (#PR)
```

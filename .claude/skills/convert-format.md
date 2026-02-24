---
name: convert-format
description: Convert data between formats (JSON, YAML, TOML, CSV, XML, INI)
activation:
  keywords: ["convert format", "json to yaml", "yaml to json", "csv to json", "toml to json", "format conversion", "xml to json"]
  file_patterns: ["**/*.json", "**/*.yaml", "**/*.yml", "**/*.toml", "**/*.csv", "**/*.xml", "**/*.ini"]
---

# Format Conversion

## Purpose
Convert data between common formats while preserving structure, types,
and comments where possible. Support bidirectional conversion.

## Instructions

1. **Detect Source Format**
   - Identify the input format from content or file extension
   - Supported formats: JSON, YAML, TOML, CSV, XML, INI, ENV
   - Validate the source is well-formed before converting
   - Note any format-specific features (YAML anchors, TOML dates, XML attributes)

2. **Parse Source Content**
   - Parse the full document structure
   - Preserve data types (strings, numbers, booleans, null, dates)
   - Capture comments where the source format supports them
   - Handle nested structures and arrays
   - Note any features that cannot be represented in the target format

3. **Convert to Target Format**
   - Map data types to the closest equivalent in the target format
   - Preserve key ordering where possible
   - Handle format-specific constraints:
     - JSON: no comments, no date type, no multiline strings
     - YAML: supports comments, anchors, multiline strings
     - TOML: supports comments, native dates, typed arrays
     - CSV: flat structure only, needs flattening strategy for nested data
     - XML: attributes vs elements decision, text content handling
     - INI: two levels deep maximum, string values only

4. **Handle Type Conversions**
   - Dates: ISO 8601 string in JSON, native in TOML, string in CSV
   - Booleans: true/false in JSON/YAML, true/false in TOML, "true"/"false" in CSV
   - Null: null in JSON/YAML, omit in TOML/INI, empty in CSV
   - Numbers: preserve precision, note integer vs float differences
   - Nested objects: flatten for CSV (dot notation), expand from flat formats

5. **Preserve Comments** (where possible)
   - YAML to TOML: transfer comments to equivalent positions
   - Any to JSON: note that comments will be lost
   - Include original comments as documentation in output when format lacks comment support

6. **Validate Output**
   - Ensure output is valid in the target format
   - Round-trip test: converting back should produce equivalent data
   - Note any data loss or transformation that occurred
   - Format output with consistent indentation

## Output Format

```
# Format Conversion: [source format] -> [target format]

## Converted Output
\`\`\`target-format
[converted content]
\`\`\`

## Conversion Notes
- Any data transformations applied
- Features lost in conversion
- Type mapping decisions made

## Validation
- Output format: valid/invalid
- Data preserved: yes/partial (list what changed)
```

If the conversion involves data loss, warn prominently before providing output.

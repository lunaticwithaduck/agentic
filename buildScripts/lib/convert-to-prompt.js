#!/usr/bin/env node
'use strict';
// convert-to-prompt.js — converts a .claude/commands/*.md file to a Copilot .prompt.md file
//
// Usage: node convert-to-prompt.js <input-path> <output-path>
//
// Input: .claude/commands/foo.md
//   First line = description text
//   Rest = body content
//
// Output: .github/prompts/foo.prompt.md
//   ---
//   description: [first line of input file]
//   mode: agent
//   ---
//
//   [rest of file, unchanged]

const fs = require('fs');
const path = require('path');

const [,, inputPath, outputPath] = process.argv;

if (!inputPath || !outputPath) {
  console.error('Usage: node convert-to-prompt.js <input-path> <output-path>');
  process.exit(1);
}

const content = fs.readFileSync(inputPath, 'utf8');
const lines = content.split('\n');

// First line is the description
const description = lines[0].trim();

// Rest of the file is the body (skip the first line)
const body = lines.slice(1).join('\n');

// Build output with frontmatter
const frontmatter = [
  '---',
  `description: ${description}`,
  'mode: agent',
  '---',
].join('\n');

const output = frontmatter + '\n' + body;

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, output);
console.log(`Converted: ${path.basename(inputPath)} -> ${outputPath}`);

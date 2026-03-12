#!/usr/bin/env node
'use strict';
// generate-skill-index.js — generates the always-on skill index Cursor rule
//
// Usage: node generate-skill-index.js <skill-rules-path> <output-path>
//
// Reads skill-rules.json and writes a Cursor rule file with alwaysApply: true
// that lists all available skills and their trigger keywords.

const fs = require('fs');
const path = require('path');

const [,, rulesPath, outputPath] = process.argv;

if (!rulesPath || !outputPath) {
  console.error('Usage: node generate-skill-index.js <skill-rules-path> <output-path>');
  process.exit(1);
}

let rules;
try {
  rules = JSON.parse(fs.readFileSync(rulesPath, 'utf8'));
} catch (e) {
  console.error(`Failed to read skill-rules.json: ${e.message}`);
  process.exit(1);
}

const MAX_KEYWORDS = 5;

// Build the table rows
const rows = Object.entries(rules).map(([skillName, rule]) => {
  const keywords = (rule.keywords || []).slice(0, MAX_KEYWORDS);
  const keywordsStr = keywords.join(', ');
  return `| ${skillName} | ${keywordsStr} |`;
});

const tableHeader = [
  '| Skill | Trigger keywords |',
  '|---|---|',
];

const content = [
  '---',
  'description: Skill index — always active',
  'alwaysApply: true',
  '---',
  '',
  '# Available Skills',
  '',
  'When any of the following topics appear in the conversation, you have expert domain knowledge available as a Cursor rule. Request the relevant skill and apply its guidance.',
  '',
  ...tableHeader,
  ...rows,
  '',
  'To use a skill: type `@[skill-name]` to attach it, or the model will request it automatically based on context.',
].join('\n');

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, content);
console.log(`Generated skill index: ${outputPath} (${Object.keys(rules).length} skills)`);

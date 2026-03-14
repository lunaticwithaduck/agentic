#!/usr/bin/env node
'use strict';
// generate-hooks-json-copilot.js — generates .github/hooks/hooks.json for the Copilot ship target
//
// Usage: node generate-hooks-json-copilot.js <output-path>

const fs = require('fs');
const path = require('path');

const [,, outputPath] = process.argv;

if (!outputPath) {
  console.error('Usage: node generate-hooks-json-copilot.js <output-path>');
  process.exit(1);
}

const hooksConfig = {
  hooks: {
    UserPromptSubmit: [
      {
        type: 'command',
        command: 'node .github/hooks/skill-detector.cjs'
      }
    ],
    PreToolUse: [
      {
        type: 'command',
        command: 'node .github/hooks/block-secrets.cjs'
      }
    ],
    PostToolUse: [
      {
        type: 'command',
        command: 'node .github/hooks/post-write.cjs'
      }
    ],
    Stop: [
      {
        type: 'command',
        command: 'node .github/hooks/post-stop.cjs'
      }
    ]
  }
};

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, JSON.stringify(hooksConfig, null, 2));
console.log(`Generated hooks.json: ${outputPath}`);

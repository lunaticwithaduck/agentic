#!/usr/bin/env node
'use strict';
// generate-hooks-json.js — generates .cursor/hooks.json for the Cursor ship target
//
// Usage: node generate-hooks-json.js <output-path>

const fs = require('fs');
const path = require('path');

const [,, outputPath] = process.argv;

if (!outputPath) {
  console.error('Usage: node generate-hooks-json.js <output-path>');
  process.exit(1);
}

const hooksConfig = {
  version: 1,
  hooks: {
    sessionStart: [
      {
        command: 'node .cursor/hooks/cursor-session-start.cjs'
      }
    ],
    afterFileEdit: [
      {
        command: 'node .cursor/hooks/post-write.cjs'
      },
      {
        command: 'node .cursor/hooks/cursor-skill-injector.cjs'
      }
    ],
    beforeShellExecution: [
      {
        command: 'node .cursor/hooks/block-secrets.cjs'
      }
    ],
    stop: [
      {
        command: 'node .cursor/hooks/post-stop.cjs'
      }
    ]
  }
};

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, JSON.stringify(hooksConfig, null, 2));
console.log(`Generated hooks.json: ${outputPath}`);

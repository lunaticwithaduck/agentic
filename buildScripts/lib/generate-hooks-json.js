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
        command: 'node .cursor/hooks/cursor-session-start.js'
      }
    ],
    afterFileEdit: [
      {
        command: 'node .cursor/hooks/post-write.js'
      },
      {
        command: 'node .cursor/hooks/cursor-skill-injector.js'
      }
    ],
    beforeShellExecution: [
      {
        command: 'node .cursor/hooks/block-secrets.js'
      }
    ],
    stop: [
      {
        command: 'node .cursor/hooks/post-stop.js'
      }
    ]
  }
};

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, JSON.stringify(hooksConfig, null, 2));
console.log(`Generated hooks.json: ${outputPath}`);

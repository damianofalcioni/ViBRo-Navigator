#!/usr/bin/env node

import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { transform } from 'esbuild';

const toolsDir = path.dirname(fileURLToPath(import.meta.url));
const docsDir = path.resolve(toolsDir, '../docs');
const sourcePath = path.join(docsDir, 'index.js');
const targetPath = path.join(docsDir, 'index.min.js');

// Minify the existing classic script without bundling or adding runtime code.
// The ES5 target prevents minification from introducing newer syntax.
export async function minifyJs(source) {
  const result = await transform(source, {
    loader: 'js',
    target: 'es5',
    minify: true,
    legalComments: 'none',
    charset: 'ascii',
    sourcefile: 'index.js'
  });
  return result.code;
}

async function main() {
  const generated = await minifyJs(await readFile(sourcePath, 'utf8'));

  if (process.argv.includes('--check')) {
    let current = '';
    try {
      current = await readFile(targetPath, 'utf8');
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }

    if (current !== generated) {
      console.error('index.min.js is out of date; run: npm run build:js');
      process.exitCode = 1;
    } else {
      console.log('index.min.js is in sync with index.js');
    }
  } else {
    await writeFile(targetPath, generated, 'utf8');
    console.log(`wrote ${path.relative(docsDir, targetPath)} (${Buffer.byteLength(generated)} bytes)`);
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main();
}

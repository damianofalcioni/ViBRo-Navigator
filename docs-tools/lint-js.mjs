#!/usr/bin/env node

import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { ESLint } from 'eslint';
import siteConfig from './eslint.config.mjs';

const toolsDir = path.dirname(fileURLToPath(import.meta.url));
const repoDir = path.resolve(toolsDir, '..');

async function walk(directory, extension) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await walk(fullPath, extension));
    else if (entry.isFile() && entry.name.endsWith(extension)) files.push(fullPath);
  }
  return files.sort();
}

function inlineScripts(html) {
  const scripts = [];
  const pattern = /<script\b(?![^>]*\bsrc\s*=)[^>]*>([\s\S]*?)<\/script>/giu;
  for (const match of html.matchAll(pattern)) {
    if (!match[1].trim()) continue;
    const codeOffset = match.index + match[0].indexOf('>') + 1;
    scripts.push({ code: match[1], startLine: html.slice(0, codeOffset).split('\n').length });
  }
  return scripts;
}

function printInlineMessages(rootDir, htmlPath, startLine, messages) {
  for (const message of messages) {
    const line = startLine + (message.line ?? 1) - 1;
    const column = message.column ?? 1;
    const rule = message.ruleId ? `  ${message.ruleId}` : '';
    const severity = message.severity === 2 ? 'error' : 'warning';
    console.error(`${path.relative(rootDir, htmlPath)}:${line}:${column}  ${severity}  ${message.message}${rule}`);
  }
}

export async function lintSite({ rootDir = repoDir, config = siteConfig, quiet = false } = {}) {
  const docsDir = path.join(rootDir, 'docs');
  const eslint = new ESLint({
    cwd: rootDir,
    overrideConfigFile: true,
    overrideConfig: config
  });
  const jsFiles = await walk(docsDir, '.js');
  const results = jsFiles.length ? await eslint.lintFiles(jsFiles) : [];
  if (!quiet) {
    const formatter = await eslint.loadFormatter('stylish');
    const formatted = await formatter.format(results);
    if (formatted) process.stdout.write(formatted);
  }

  const htmlFiles = await walk(docsDir, '.html');
  let inlineCount = 0;
  for (const htmlPath of htmlFiles) {
    const scripts = inlineScripts(await readFile(htmlPath, 'utf8'));
    for (const [index, script] of scripts.entries()) {
      const [result] = await eslint.lintText(script.code, {
        filePath: `${htmlPath}.inline-${index + 1}.js`
      });
      results.push(result);
      inlineCount += 1;
      if (!quiet) printInlineMessages(rootDir, htmlPath, script.startLine, result.messages);
    }
  }

  return {
    files: jsFiles.length,
    inlineScripts: inlineCount,
    errorCount: results.reduce((total, result) => total + result.errorCount, 0),
    warningCount: results.reduce((total, result) => total + result.warningCount, 0)
  };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const result = await lintSite();
  console.log(`Checked ${result.files} JavaScript file(s) and ${result.inlineScripts} inline script(s).`);
  // Warnings include ignored files: skipped code must never count as success.
  if (result.errorCount > 0 || result.warningCount > 0) process.exitCode = 1;
}

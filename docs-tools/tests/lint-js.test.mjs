import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { lintSite } from '../lint-js.mjs';
import config from '../eslint.config.mjs';

async function fixture(t, { js, html, generated }) {
  const rootDir = await mkdtemp(path.join(os.tmpdir(), 'vibro-lint-'));
  assert.equal(path.dirname(rootDir), path.resolve(os.tmpdir()));
  assert.ok(path.basename(rootDir).startsWith('vibro-lint-'));
  t.after(() => rm(rootDir, { recursive: true, force: true }));
  const docsDir = path.join(rootDir, 'docs');
  await mkdir(docsDir);
  if (js !== undefined) await writeFile(path.join(docsDir, 'index.js'), js);
  if (generated !== undefined) await writeFile(path.join(docsDir, 'index.min.js'), generated);
  await writeFile(path.join(docsDir, 'index.html'), html);
  return rootDir;
}

test('lint checks external and inline scripts under the same site root', async t => {
  const rootDir = await fixture(t, { js: 'document.title = "OK";', html: '<script>document.title = "Inline";</script>' });
  assert.deepEqual(await lintSite({ rootDir, quiet: true }), {
    files: 1, inlineScripts: 1, errorCount: 0, warningCount: 0
  });
});

test('invalid external JavaScript is reported', async t => {
  const rootDir = await fixture(t, { js: 'undefinedExternal();', html: '<script src="index.js"></script>' });
  const result = await lintSite({ rootDir, quiet: true });
  assert.equal(result.errorCount, 1);
  assert.equal(result.warningCount, 0);
  assert.equal(result.inlineScripts, 0);
});

test('invalid inline JavaScript is reported', async t => {
  const rootDir = await fixture(t, { html: '<script\n>undefinedInline();</script>' });
  const result = await lintSite({ rootDir, quiet: true });
  assert.equal(result.errorCount, 1);
  assert.equal(result.warningCount, 0);
  assert.equal(result.inlineScripts, 1);
});

test('published scripts reject syntax that requires newer JavaScript parsers', async t => {
  const rootDir = await fixture(t, { js: '(() => {})();', html: '<script>try {} catch {}</script>' });
  const result = await lintSite({ rootDir, quiet: true });
  assert.equal(result.errorCount, 2);
  assert.equal(result.warningCount, 0);
});

test('generated delivery scripts retain ES5 syntax validation', async t => {
  const rootDir = await fixture(t, { js: 'document.title = "OK";', generated: '(() => {})();', html: '' });
  const result = await lintSite({ rootDir, quiet: true });
  assert.equal(result.files, 2);
  assert.equal(result.errorCount, 1);
  assert.equal(result.warningCount, 0);
});

test('generated scripts skip source maintainability limits but retain correctness rules', async t => {
  const rootDir = await fixture(t, {
    generated: 'function pick(a,b,c){if(a)return a;if(b)return b;return c;}try{document.title=pick(1,2,3);}catch(e){}',
    html: ''
  });
  assert.equal((await lintSite({ rootDir, quiet: true })).errorCount, 0);
  await writeFile(path.join(rootDir, 'docs', 'index.min.js'), 'undefinedGenerated();');
  assert.equal((await lintSite({ rootDir, quiet: true })).errorCount, 1);
});

test('ignored inline scripts produce a failing warning count', async t => {
  const rootDir = await fixture(t, { html: '<script>undefinedInline();</script>' });
  const result = await lintSite({ rootDir, quiet: true, config: [{ ignores: ['**/*.js'] }, ...config] });
  assert.equal(result.warningCount, 1);
});

test('ignoring all external scripts cannot silently pass', async t => {
  const rootDir = await fixture(t, { js: 'undefinedExternal();', html: '' });
  const result = await lintSite({ rootDir, quiet: true, config: [{ ignores: ['**/*.js'] }, ...config] });
  assert.equal(result.warningCount, 1);
});

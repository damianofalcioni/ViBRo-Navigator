import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';
import test from 'node:test';
import { ESLint } from 'eslint';
import { minifyJs } from '../minify-js.mjs';

test('minification preserves classic-script scope, strict mode and side effects', async () => {
  const source = `/* remove this comment */
    (function () {
      'use strict';
      var message = 'GPS → café';
      window.result = [(function () { return this; })() === undefined, message];
      window.calls += 1;
    })();`;
  const delivered = await minifyJs(source);
  const page = { window: { calls: 0 } };
  vm.runInNewContext(delivered, page);
  assert.deepEqual(Array.from(page.window.result), [true, 'GPS → café']);
  assert.equal(page.window.calls, 1);
  assert.deepEqual(Object.keys(page), ['window']);
  assert.equal(delivered.includes('remove this comment'), false);
  assert.ok(delivered.length < source.length);
  const eslint = new ESLint({
    overrideConfigFile: true,
    overrideConfig: [{ languageOptions: { ecmaVersion: 5, sourceType: 'script' } }]
  });
  const [result] = await eslint.lintText(delivered);
  assert.equal(result.errorCount, 0);
  assert.equal(result.warningCount, 0);
});

test('invalid JavaScript fails minification', async () => {
  await assert.rejects(minifyJs('function broken( {'), /Transform failed/);
});

test('delivered JavaScript matches the current source and pinned esbuild build', async () => {
  const [source, delivered] = await Promise.all([
    readFile(new URL('../../docs/index.js', import.meta.url), 'utf8'),
    readFile(new URL('../../docs/index.min.js', import.meta.url), 'utf8')
  ]);
  assert.equal(delivered, await minifyJs(source));
  assert.ok(Buffer.byteLength(delivered) < Buffer.byteLength(source));
});

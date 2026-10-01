import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { parse, toPlainObject } from 'css-tree';
import { minifyCss } from '../minify-css.mjs';

function syntax(css) {
  return toPlainObject(parse(css, { parseCustomProperty: true }));
}

test('descendant pseudo selectors retain their combinators', () => {
  const source = '.parent :hover { color: red; } .parent ::before { content: "x"; }';
  const result = minifyCss(source);
  assert.match(result, /\.parent :hover/);
  assert.match(result, /\.parent ::before/);
  assert.deepEqual(syntax(result), syntax(source));
});

test('CSS syntax survives comments, strings, URLs, custom properties and math', () => {
  const source = `/* remove this */
    @media (min-width: 700px) {
      .card > :first-child, [data-label="a  b"]::before {
        --empty: ;
        --color: rgba(1, 2, 3, .5);
        width: calc(100% - 20px);
        content: "x ; : /* literal */ \\\" y";
        background-image: url("https://example.test/a:b;c.png");
        transition: all 250ms !important;
      }
    }`;
  const result = minifyCss(source);
  assert.equal(result.includes('remove this'), false);
  assert.match(result, /calc\(100% - 20px\)/);
  assert.deepEqual(syntax(result), syntax(source));
});

test('unparseable CSS fails rather than silently changing the output', () => {
  assert.throws(() => minifyCss('.a { color: ); }'));
});

test('delivered stylesheet preserves source rules, selectors and declarations', async () => {
  const [source, delivered] = await Promise.all([
    readFile(new URL('../../docs/styles.css', import.meta.url), 'utf8'),
    readFile(new URL('../../docs/styles.min.css', import.meta.url), 'utf8')
  ]);
  assert.deepEqual(syntax(delivered), syntax(source));
  assert.equal(delivered, minifyCss(source));
});

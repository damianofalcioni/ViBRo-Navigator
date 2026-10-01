# ViBRo Navigator documentation tooling

Development-only tools for the static website in `../docs/`. Nothing here is required by public pages. Agent guidance lives in `../docs/AGENT.MD`; use Git history for superseded revisions.

## Setup

Requires Node.js 20.19 or newer, as declared in `package.json`. Install the pinned development dependency tree from the repository root:

```sh
cd docs-tools
npm ci
```

`node_modules/`, `.npm-cache/`, and `*.log` are ignored and must not be committed or published. If the default npm cache is inaccessible, use `npm ci --cache .npm-cache`; keep development dependencies and caches outside `docs/`.

## Commands

Run these from `docs-tools/`:

| Command | Purpose |
| --- | --- |
| `npm run build` | Regenerate both CSS and JavaScript delivery files |
| `npm run build:css` | Regenerate `../docs/styles.min.css` from authored CSS |
| `npm run check:css-build` | Verify generated CSS is current |
| `npm run build:js` | Minify `../docs/index.js` into `../docs/index.min.js` with esbuild |
| `npm run check:js-build` | Verify generated JavaScript is current |
| `npm run lint:css` | Stylelint and Project Wallace complexity limits |
| `npm run lint:js` | ESLint for external and inline site scripts |
| `npm run lint` | Both lint checks |
| `npm test` | Tooling and site behavior regressions |
| `npm run check` | Both lint checks, CSS/JavaScript synchronization, and tests |

After authored CSS changes, run `npm run build:css` followed by `npm run check`. After authored JavaScript changes, run `npm run build:js` followed by `npm run check`. When both change, run `npm run build` followed by `npm run check`. After tooling changes, run the applicable build if output changes, then `npm run check`. Never edit generated assets by hand. When delivered CSS or JavaScript changes, bump that asset's cache query consistently across all six public HTML pages.

## Tool ownership

- `minify-css.mjs` uses pinned development-only `css-tree` parsing/generation to remove comments and formatting without restructuring selectors, declarations, or rules. Parse errors fail the build; regression fixtures preserve descendant combinators, strings, URLs, custom properties, and CSS math.
- `minify-js.mjs` uses pinned development-only esbuild to generate `index.min.js` from readable `index.js`. It minifies the classic script with an ES5 target, without bundling, source maps, or runtime dependencies. Public pages load only `index.min.js`, synchronously in the head to preserve early theme restoration.
- Repository `.gitattributes` keeps generated `docs/styles.min.css` and `docs/index.min.js` at LF line endings so exact build checks work on Windows checkouts as well as other platforms.
- `lint-js.mjs` applies `eslint.config.mjs` with the repository as its base. It recursively checks external `.js` files and inline `<script>` blocks in HTML, reports coverage, and fails on errors or warnings, including ignored-file warnings. The current tree contains one authored script and its generated delivery file, with no inline scripts. Both scripts retain ES5 syntax and correctness checks; only `docs/index.min.js` is exempt from source maintainability limits.
- `stylelint.config.mjs` owns CSS lint and complexity limits. It preserves the site's legacy `rgba()` notation and required vendor properties. Selector-order exceptions remain explained beside the affected source rules, without a blanket disable.
- `tests/lint-js.test.mjs` covers valid, invalid, ignored, and non-ES5 scripts.
- `tests/minify-css.test.mjs` covers CSS syntax preservation, parse failures, and source/delivery equivalence.
- `tests/minify-js.test.mjs` covers classic-script scope, strict mode, side effects, ES5 output, parse failures, and source/delivery synchronization.
- `tests/site.test.mjs` runs theme, cookie, DOM-readiness, navigation/focus, and media-API regressions against both authored and minified scripts. It also checks that all pages load the minified script with consistent asset versions.

## Enforced limits

These values match the checked-in configs. ESLint uses core maintainability rules rather than a separate complexity plugin.

| CSS rule | Limit |
| --- | --- |
| Nesting depth | 1 |
| Selector specificity | `0,3,0` |
| Compound selectors per selector | 3 |
| Combinators per selector | 2 |
| Selector complexity | 5 |
| Average selector complexity | 2 |
| Declarations per rule | 30 |
| Selectors per rule | 6 |
| Important declaration ratio | 0.01 (1%) |

Stylelint's nesting rule ignores root-level block at-rules such as `@media`. Specificity uses lexicographic comparison of IDs, classes/attributes/pseudo-classes, and elements/pseudo-elements. The intentional `!important` declarations support reduced motion and the global theme transition.

| JavaScript rule | Limit |
| --- | --- |
| Cyclomatic complexity (classic) | 4 |
| Block nesting depth | 2 |
| Nonblank/noncomment lines per function | 60 |
| Nested callbacks | 1 |
| Parameters per function | 2 |
| Statements per function | 25 |

Published scripts use ES5 syntax; tools and tests use modern Node.js. Function length includes nested function bodies, excludes blank lines/comments, and excludes IIFEs. The other complexity limits still apply to IIFE wrappers. Keep meaningful responsibility boundaries rather than packing declarations or splitting cohesive functions only to satisfy a rule.

## Verification scope

Runtime tests use DOM stubs; passing them does not establish browser rendering, old-device compatibility, animation smoothness, or Lighthouse performance. Check those separately when a change affects appearance or interaction.

Keep this guide and `../docs/AGENT.MD` focused on current behavior. Dependency versions belong in `package.json`/`package-lock.json`, limits in the lint configs, and asset cache queries in public HTML.

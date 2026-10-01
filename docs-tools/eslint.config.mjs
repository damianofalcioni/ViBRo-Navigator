export default [
  {
    languageOptions: {
      // Published scripts use ES5 syntax; development tooling may use modern JS.
      ecmaVersion: 5,
      sourceType: 'script',
      globals: {
        document: 'readonly',
        location: 'readonly',
        window: 'readonly'
      }
    },
    linterOptions: {
      reportUnusedDisableDirectives: 'error'
    },
    rules: {
      'no-undef': 'error',
      'no-unused-vars': ['error', { args: 'after-used', caughtErrorsIgnorePattern: '^_', ignoreRestSiblings: true }],
      'no-unreachable': 'error',
      'no-dupe-args': 'error',
      'no-dupe-keys': 'error',
      'no-func-assign': 'error',
      'no-class-assign': 'error',
      'no-import-assign': 'error',
      'no-unsafe-finally': 'error',
      'no-unsafe-negation': 'error',
      'no-unsafe-optional-chaining': 'error',
      'use-isnan': 'error',
      'valid-typeof': 'error',

      // Measured against external and inline site scripts; see README.md.
      'complexity': ['error', { max: 4, variant: 'classic' }],
      'max-depth': ['error', 2],
      'max-lines-per-function': [
        'error',
        {
          max: 60,
          skipBlankLines: true,
          skipComments: true,
          IIFEs: false
        }
      ],
      'max-nested-callbacks': ['error', { max: 1 }],
      'max-params': ['error', 2],
      'max-statements': ['error', 25]
    }
  },
  {
    // Generated delivery code still needs ES5 syntax and correctness checks;
    // maintainability limits apply to its readable source, not esbuild output.
    files: ['docs/index.min.js'],
    rules: {
      // esbuild renames the source's intentionally unused _error catch binding.
      'no-unused-vars': ['error', { args: 'after-used', caughtErrors: 'none', ignoreRestSiblings: true }],
      'complexity': 'off',
      'max-depth': 'off',
      'max-lines-per-function': 'off',
      'max-nested-callbacks': 'off',
      'max-params': 'off',
      'max-statements': 'off'
    }
  }
];

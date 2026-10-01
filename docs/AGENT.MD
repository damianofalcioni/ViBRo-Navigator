# ViBRo Navigator Website – Agent Notes

This file defines the current constraints for the static ViBRo Navigator website in `docs/`, the directory containing this file. Development tooling lives in the repository-level sibling `docs-tools/`.
Treat them as the source of truth unless the user explicitly changes a decision.

Last packaged revision: **v173**. Routine workspace edits do not require a new package revision.

Current shared asset queries: CSS **v=182**, JavaScript **v=178** across all six public HTML pages. Public pages load `styles.min.css` and `index.min.js`. Keep current guidance here; use Git history for superseded revision notes.

## Project

- App: ViBRo Navigator
- Repository: `https://github.com/damianofalcioni/ViBRo-Navigator`
- Google Play: `https://play.google.com/store/apps/details?id=vibro.navigator`
- F-Droid: `https://f-droid.org/packages/vibro.navigator/`
- Latest release: `https://github.com/damianofalcioni/ViBRo-Navigator/releases/latest`
- GitHub Sponsors: `https://github.com/sponsors/damianofalcioni`

## Architecture

- Static HTML/CSS site rooted in this directory.
- Keep `.nojekyll`.
- Do not add Jekyll, `_config.yml`, a Markdown runtime renderer, or a build generator unless explicitly requested.
- Published legal and changelog content is maintained directly in HTML.
- Shared CSS: `styles.css`.
- Shared theme/cookie/navigation source: `index.js`; public pages load generated `index.min.js` synchronously in the head.
- Keep published JavaScript small and dependency-free. Development-only lint/build tooling lives in the sibling `../docs-tools/` directory and must not be referenced by public pages. The published JavaScript currently handles:
  - immediate manual light/dark CSS interpolation
  - theme preference persistence
  - cookie consent
  - the screenshot light/dark hint word
  - homepage mobile navigation
- Shared `index.js` also owns the homepage mobile-navigation behavior, guarded on the presence of its controls.
- Preserve accessibility, responsive behavior, and Lighthouse-oriented performance.

## Homepage structure

`index.html` uses this order:

1. Top navigation
2. Hero
3. Screenshots
4. Quick facts
5. Features
6. Navigation modes
7. Innovations
8. Support the development
9. Legal/store links
10. Footer

## Navigation

Desktop links:
- Screenshots
- Features
- Navigation
- Download
- Changelog
- GitHub

Mobile:
- collapse navigation into a hamburger menu
- menu closes on link selection, outside click, and Escape
- avoid horizontal overflow

Theme switch:
- visible only on the homepage
- the visible `.theme-switch` is a native `<button type="button">` with an accessible label and `aria-pressed`; keep it keyboard-focusable on the homepage
- `index.js` owns the manual theme state by updating `<html data-theme>` and persists the preference only when cookie consent allows it
- keep the top-bar action layer above adjacent content and the visible switch explicitly hit-testable (`pointer-events: auto`, `touch-action: manipulation`)
- when shared CSS/JS behavior changes, update the cache-busting query used by public HTML pages so new markup cannot run against stale cached assets
- sticky top bar uses a fully opaque theme background with no backdrop blur/translucency, so scrolling content cannot show through behind the navigation
- on desktop, the primary navigation links share the top-bar surface directly; do not give `.toplinks` a separate card/gradient background or shadow
- the visible theme switch uses directly interpolated `background-color`, border, shadow, and color states; do not theme it with background gradients or component-specific transition durations
- the homepage document root provides the `#top` fragment target used by the screenshot theme hint; keep that target separate from the sticky navigation element
- legal pages and changelog have no visible switch; they still inherit the effective theme from `<html data-theme>`

Self-hosted badges:
- keep badge images local under `assets/`
- current files: `google-play-badge.png`, `fdroid-badge.png`, `github-apk-badge.png`, `github-sponsor-badge.svg`
- preserve the supplied GitHub APK and Sponsors badge assets verbatim rather than regenerating approximations
- use the single Play and F-Droid assets everywhere; do not reintroduce `-400.png` derivatives or badge `srcset`/`sizes` variants
- links may remain external, but badge image `src` values must stay local
- do not reintroduce remote `img.shields.io`, `play.google.com` badge-image, or `fdroid.gitlab.io` badge-image URLs

## Theme system

- Support dark and light themes.
- Default to `prefers-color-scheme`.
- A manual homepage switch overrides the effective theme.
- Respect `prefers-reduced-motion`.
- Do not use the browser View Transition API for theme switching; its root snapshot/raster preparation caused a visible startup delay on this long page.
- The manual theme switch follows the user-provided Moonwalk v3 demo directly: the effective theme is stored on `<html data-theme>`, clicking adds the temporary `transition` class to `<html>`, changes the theme state immediately in the same task, and removes the class after 1 second. Under `prefers-reduced-motion: no-preference`, `html.transition`, every descendant, and their pseudo-elements receive `transition: all 250ms !important` with zero delay. Do not add special midpoint text swaps, foreground delays, forced layout/style flushes, or a separate page-background opacity layer. The page and framed surfaces use directly interpolated `background-color`; theme-dependent hero and screenshot artwork remains layered and crossfades through opacity on the same 250 ms window. Reduced-motion users receive an immediate switch.

### Theme persistence

Cookies:
- `vibro_cookie_consent`
- `vibro_theme_override`

Rules:
- no saved manual override until the user chooses `Accept all`
- `Reject all` deletes the theme-preference cookie
- rejection does not disable manual switching for the current page
- use `Path=/`, `SameSite=Lax`, and `Secure` on HTTPS
- retention is one year
- no analytics or advertising cookies are used

### Cookie panel

Show the bottom consent panel until a consent choice exists.

It must:
- offer exactly `Reject all` and `Accept all`
- explain that the optional cookie persists the theme choice
- explain that the consent cookie remembers the consent decision
- state that no analytics or advertising cookies are used
- adapt to both themes and narrow screens

## Visual direction

The design should feel:
- cyberpunk navigation
- precise
- technical
- clean
- lightweight rather than decorative

Avoid:
- excessive glow
- heavy text shadows
- obvious borders around glass elements
- large opaque hero panels
- unnecessary decorative UI

Core dark palette is based around:
- `#05070b`
- `#090d14`
- `#0c121b`
- `#101824`
- `#141d29`
- purple `#b147ff`
- blue `#36a7ff`
- cyan `#23d9ff`
- green `#31e77d`
- red `#ff4b5c`
- orange `#ff8a36`

The light theme must remain recognizably the same design system.

Light-mode feature, navigation-mode, and innovation cards use a broad, soft outer shadow so the transition into the page background is gradual rather than looking clipped; their dark-mode counterparts intentionally have no outer shadow. Keep the blur radius generous and the opacity restrained. Do not apply `content-visibility`/paint containment to these card sections, including on mobile, because it clips their outer shadows; the current mobile optimization is limited to the legal/footer area.

## Hero

Live banner assets:
- dark: `assets/hero-cyberpunk-background.webp`
- light: `assets/hero-cyberpunk-background-light.webp`

Reference/source PNGs:
- `assets/hero-cyberpunk-background.png`
- `assets/hero-cyberpunk-background-light.png`

Rules:
- use the same wide banner asset on desktop and mobile; do not create a separate mobile crop
- deliver the self-hosted full-resolution WebP variants; there are no JPEG hero derivatives in the current package
- keep the PNGs as source/reference assets
- regenerate WebP delivery assets from the PNG sources, not from older compressed files
- full-width cover image with centered `object-position`
- clip hero artwork with `overflow: clip` so the Download fragment scrolls the page without scrolling inside the banner and separating its artwork from its overlays
- desktop/tablet dark and light themes use the intended synthetic hero overlay for text legibility and visual treatment; on mobile, dark mode intentionally shows the banner without that overlay while light mode keeps it
- no baked-in text
- hero title, subtitle, tagline, and download buttons stay vertically centered within the banner

Logo asset:
`assets/icon.png`

Use this self-hosted image for the favicon, top-navigation logo, hero logo, and homepage Open Graph image. Do not reintroduce a remote GitHub icon URL.

Both the top-navigation logo and hero logo:
- have no visible outer box
- have no border, glass background, or box shadow
- keep their existing size/alignment

### Hero copy

Title:
`ViBRo Navigator`

`ViBRo` wordmark:
- dark: bright cyan/purple/magenta gradient
- light: darker blue/indigo/violet/magenta gradient
- no text shadow

Subtitle:
`Offline and truly lightweight GPS navigator with minimal on-screen, voice, and vibration guidance`

Subtitle:
- centered
- desktop max width around 540 px
- visually narrower than the main title
- keep `line-height: 1.55` and all layout metrics identical between themes
- light-mode text uses `#0d1e35`; dark-mode text uses `#d6dfef`
- light mode uses subtle translucent blur
- no visible border or shadow

Tagline:
`ROADS • TRAILS • CITIES • BEYOND`

Tagline:
- centered and readable
- light mode may use subtle translucent blur
- no visible border or shadow
- use color contrast instead of text shadow
- keep typography, padding, and layout metrics identical between themes
- desktop uses `.80rem` font size and `1.24rem` line-height; mobile uses `10px` and `16px`
- keep the centered text-hugging `width: fit-content`, `max-width: calc(100% - 28px)` container on mobile
- render the light pill with `::before` and `inset: -6px -12px -7px` at both desktop and mobile widths, so its paint does not change layout

### Hero downloads

Links:
- Google Play
- F-Droid
- GitHub Releases

Badges:
- Play: `assets/google-play-badge.png`
- F-Droid: `assets/fdroid-badge.png`
- GitHub Releases: `assets/github-apk-badge.png` (alt text: `Get APK from GitHub`)

Do not put visible outer boxes around these badges. Keep only clean spacing and subtle hover movement.
All three download images have intrinsic dimensions `646×250` and share the `--store-badge-width: 160px` display width with automatic height. Their slots use zero padding. Desktop uses three columns with space between; mobile uses one centered column with a `14px` gap. Keep the GitHub badge geometry aligned with the store badges rather than restoring the removed SVG shield's special sizing/padding.

## Screenshots

Section:
- kicker: `Real app. Real navigation.`
- title: `Screenshots`
- four compact screenshot slots
- 4 columns on desktop, 2 on mobile
- keep dark and light screenshot images layered inside `.screenshot-theme-picture`; the two layers crossfade with opacity on the same 250 ms timeline as the page background and framed surfaces

Current placeholder assets:
- `assets/screenshot-1-dark-placeholder.webp`
- `assets/screenshot-1-light-placeholder.webp`
- `assets/screenshot-2-dark-placeholder.webp`
- `assets/screenshot-2-light-placeholder.webp`
- `assets/screenshot-3-dark-placeholder.webp`
- `assets/screenshot-3-light-placeholder.webp`
- `assets/screenshot-4-dark-placeholder.webp`
- `assets/screenshot-4-light-placeholder.webp`

These are temporary 360×640 WebPs. Each screenshot/theme currently uses one asset for both desktop and mobile; there are no responsive placeholder variants or `srcset`/`sizes` candidates. Both theme images are present as layered `<img>` elements so a manual theme change can crossfade the artwork with CSS opacity on exactly the same 250 ms timeline as the page background and screenshot frame. Real screenshots may replace these assets later without changing the four-slot structure.
Do not keep obsolete screenshot assets once they are no longer referenced by the gallery.

### Screenshot theme hint

The hint:
- points users toward the homepage theme switch
- says the screenshots can be viewed in the opposite theme
- changes only the single word in `#screenshot-theme-word`
- shows `light` when the current effective theme is dark
- shows `dark` when the current effective theme is light
- does not mention the hero/banner change

## Homepage product content

### Quick facts

- keep the `.quick-facts` grid background transparent; the translucent `.fact` cells must composite directly over the page so their theme fade follows the same visual path as the other cards
- do not add per-cell theme shadows or a changing parent fill behind the four cells
- Offline routing / BRouter-powered
- ~400 KB / F-Droid APK
- Android 6+ / min SDK 23
- Pure Java / minimal dependencies

### Positioning

Section title:
`Navigation without living inside a map.`

Core positioning:
`It is designed for map-free navigation, delivering directions through vibrations and minimal on-screen and voice guidance, enabling distraction-free and screen-off usage.`

### Guidance

Vibration/voice copy should communicate:
- recognizable vibration patterns for instruction categories
- smartwatch/smartband-friendly notifications
- minimal voice guidance aligned with navigation notifications
- distraction-conscious guidance

### Battery

Battery-conscious copy must cover:
- dynamic GPS fix acquisition based on navigation need
- screen-off optimization
- avoidance of unnecessary rendering/processing when the display is off

### Navigation modes

1. Route / Turn-by-turn
2. Round Trip / Circular routes
3. Straight Line / Point and go

### Innovations

1. Dynamic GPS fix acquisition
2. Time-based turn timing
3. BRouter-segment street rendering
4. Smartband-friendly notifications
5. Recognizable vibration categories
6. Initial turn orientation

## Support section

Keep `Support the development` above the legal/footer area.

GitHub Sponsors:
- destination: `https://github.com/sponsors/damianofalcioni`
- badge: `assets/github-sponsor-badge.svg`

Google Play:
- destination: `https://play.google.com/store/apps/details?id=vibro.navigator`
- use the official Play badge
- explain that the Play version supports development and additionally includes:
  - automatic updates through Google Play
  - Google Fused Location Provider
  - Android Auto integration
  - optional Google services for POI search when the user supplies an API key

Do not imply these Google-integrated features are available in the F-Droid build unless the project changes.

Both support badges:
- no visible outer button box
- no outer border, opaque background, glass panel, or shadow
- small hover movement is fine

## Legal pages

Routes:
- `privacy-policy/index.html`
- `terms-of-service/index.html`
- `data-deletion/index.html`
- `store-disclosures/index.html`

Rules:
- preserve legal content unless explicitly asked to edit it
- use the shared stylesheet/theme system
- do not show a visible theme switch
- visible `http://` / `https://` URLs must be clickable links

Store Disclosure Notes:
- internal Privacy / Terms / Data Deletion links use local relative URLs
- do not display a deployment hostname for those links
- do not mention GitHub Pages on that page

## Changelog

Route:
`CHANGELOG/index.html`

- static HTML
- newest entries go first
- keep this insertion marker directly below the heading:
  `<!-- ADD NEW CHANGELOG ENTRIES HERE, newest release first. -->`
- no separate changelog footer is required

## Responsive behavior

Keep:
- `-webkit-text-size-adjust: 100%`
- `text-size-adjust: 100%`

These prevent Android Chrome Desktop-site font boosting.

Also ensure:
- hero/title/subtitle/tagline/badges do not clip
- screenshots remain compact
- support cards stack on narrow screens
- legal/footer content can stack
- no horizontal overflow
- final `o` in `ViBRo` is never clipped

## Performance / Lighthouse

Keep:
- `styles.css` is the readable source; public pages load the generated, actually minified `styles.min.css` delivery file. Keep `styles.min.css` comment-free and whitespace-minified rather than copying the readable source verbatim.
- CSS delivery parity is maintained by `../docs-tools/minify-css.mjs` using pinned development-only CSS parsing/generation: from `docs-tools/`, run `npm run build:css` after editing `styles.css`, and use `npm run check:css-build` to verify that `styles.min.css` is current. No build dependency is loaded by public pages.
- JavaScript delivery parity is maintained by `../docs-tools/minify-js.mjs` using pinned development-only esbuild: run `npm run build:js` after editing `index.js`, and `npm run check:js-build` to verify `index.min.js`. Preserve the ES5 target and synchronous head loading for early theme restoration.
- the homepage loads `styles.min.css` as a normal render-blocking stylesheet. This avoids post-first-paint restyling/CLS and the associated main-thread layout task.
- unique `<meta name="description">` on every public HTML page
- homepage `<meta name="description">` and `og:description` both use exactly: `Offline and truly lightweight Android GPS navigator with minimal on-screen, voice, and vibration guidance.`
- media-qualified, `fetchpriority="high"` preloads for the system-matching full-width hero WebP
- use the same wide hero image on desktop and mobile
- dark/light hero and screenshot artwork layers crossfade with CSS opacity on the same 250 ms manual-theme timeline as the page background; no JavaScript artwork timer is used
- screenshots remain lazy-loaded; the two hero theme assets are present in the DOM and the system-matching hero asset is promoted by the media-qualified preload so the initial LCP path stays prioritized
- one 360×640 WebP per screenshot/theme placeholder, with no responsive placeholder variants
- lazy loading, async decoding, and low fetch priority for screenshots
- explicit image dimensions for store/support badges
- intrinsic screenshot dimensions / 9:16 reserved ratio
- on mobile only, `content-visibility: auto` is limited to the legal/footer area; do not apply it to card sections because paint containment clips their broad shadows
- below-the-fold support badges stay lazy-loaded
- hero/store image requests below the LCP hero use low fetch priority where appropriate.

For the hero and screenshot gallery, keep the paired dark/light `<img>` layers because synchronized opacity interpolation is required for the manual theme transition. Do not add further duplicate theme layers elsewhere without a specific transition need.

All badge images are self-hosted locally, so badge requests should not trigger third-party-cookie findings.
GitHub Pages controls cache response headers, so its short cache TTL cannot be fixed from files in this directory.

A real Lighthouse score depends on the deployed site, external asset origins, network/browser conditions, and Lighthouse version.

## Delivery

For each requested site change:
- edit the existing workspace files directly
- do not automatically generate ZIP archives, patch files, checksums, or release notes
- do not create or maintain a `deliverables/` folder
- create packages or release artifacts and increment the package revision only when the user explicitly requests them
- preserve unrelated files and content
- bump the public HTML CSS/JS cache-busting query only when delivered `styles.min.css` or `index.min.js` actually changes

Current implementation details to preserve:
- on phone widths (`<=700px`), the duplicate hero app icon beside `ViBRo Navigator` is hidden because the navigation bar already shows the app icon; the heading remains centered across the full hero-content width
- desktop (`>900px`) uses a 515px visible hero with a centered 610px-tall `.hero-bg-image`; tablet (`701–900px`) uses a 510px visible hero with a centered 590px-tall image; mobile hero height is content-driven
- mobile dark mode shows the hero banner without the synthetic `::before`/`::after` overlay; mobile light mode keeps the light overlay; desktop/tablet keep overlays for both themes
- screenshot placeholders are one self-hosted 360×640 WebP per screenshot/theme, used for both desktop and mobile, with no responsive placeholder variants
- hero delivery uses the dark/light WebPs; PNG originals remain only as source/reference assets
- feature cards use custom inline SVG line icons; standalone source copies live under `assets/feature-icons/`; filled details use `currentColor`, so previews must set CSS `color` as well as stroke styling
- feature-card headings place the SVG icon and title on one row, with the descriptive paragraph below
- `robots.txt` explicitly allows public crawling and `llms.txt` provides the concise machine-readable project/site index

## CSS ownership and maintenance

Keep the stylesheet's section map and token-ownership comments current. Update canonical component rules in place; do not append revision-stamped overrides or a final catch-all text-color block.

- `:root` owns foundational page values (`--bg`, `--text`, `--muted`, `--line`), shared primitives, artwork opacity, and theme-switch knob position. `html[data-theme="light"]` supplies their light overrides.
- `.site-root` owns shared semantic surfaces, controls, navigation/document text, borders, shadows, and badge filters. Keep dark/default values in its base rule and only light differences in its light override. Do not duplicate foundational page values on shells.
- `.doc-site-root` owns document-specific coordinated state. Components own their local `--doc-*`, `--theme-hint-*`, `--cookie-*`, `--mobile-nav-*`, hero, menu-button, and tagline state as applicable.
- Use direct `html[data-theme="light"]` component qualifiers without a redundant `.site-root` ancestor. Keep dark/default presentation in base rules and only genuine light differences in theme rules. Preserve specificity-neutral `:where(...)` scopes and higher-specificity interaction rules where the cascade needs them.
- Inherit text colors where appropriate. Mode headings explicitly use `--text` and mode copy uses `--muted` because the mode's accent color also drives `currentColor` rails and labels.
- Keep screenshot frame surface, border, and shadow on `.screenshot-theme-picture`, rather than its images. Scope badge-link styling to hero/support contexts; do not restore a generic glass button layer.
- Shared primitives represent stable ownership: `--font-mono`, `--label-accent`, `--page-background-images`, `--homepage-content-max-width` (1180px), and `--store-badge-width` (160px). The page backdrop is theme-independent while `--bg` supplies its theme-dependent base.
- Group structural rules only for intentionally coupled components. Hero/frame minimum heights, collection-grid gutters (15px), and card/mode radii (17px) share geometry; component-specific exceptions stay local. Do not create a generic spacing/radius system from repeated numbers or alias variables that add no meaning.
- Keep shared responsive overrides in the existing broad-to-narrow 1120/900/700/450/360px blocks. Hero crop queries stay with the hero. Avoid duplicate breakpoint restatements.
- Preserve intentional WebKit compatibility declarations. Keep `!important` limited to reduced-motion enforcement and the global theme transition. Selector-order lint exceptions must retain local explanations; do not replace them with a blanket disable.

The theme-switch knob consumes `--theme-switch-knob-x` through `translateX`: dark/default is `0px` toward the moon; light is `18px` toward the sun. Its movement follows the existing global transition and immediate reduced-motion behavior.

Paired hero/screenshot artwork uses `cubic-bezier(.35, 0, .25, 1)` opacity interpolation within the same 250ms, zero-delay theme window. Keep this timing compensation without adding artwork timers or a second transition mechanism.

## JavaScript ownership and compatibility

All published interaction is authored in dependency-free `index.js` and delivered through generated `index.min.js`; the homepage has no inline script. `initThemeControls`, `initCookieConsent`, and guarded `initNavigation` own separate responsibilities.

- Restore the effective theme synchronously in the head, then initialize DOM controls at DOM readiness (or immediately if ready).
- Treat malformed cookie encoding and unknown consent values as absent. Restore saved themes only with accepted consent. Remembered consent avoids creating an unused panel.
- Use ES5 syntax for published scripts; development tools/tests use modern Node.js.
- Support modern and legacy media-query listeners. Navigation uses resize only when media listeners are unavailable.
- Keep navigation ARIA/class state in one writer, use parent-walking link detection, and preserve the legacy Escape-key fallback. Escape restores focus only when closing an open menu.
- JavaScript owns interaction state; CSS owns layout and paint. Do not add browser-side CSS processing.

## Development tooling and required validation

Development tooling stays in the sibling `../docs-tools/`, outside published `docs/`. Keep its pinned development dependencies and `package-lock.json`; install with `npm ci`. `node_modules/`, `.npm-cache/`, and logs are ignored. The public site remains runtime-dependency-free.

`minify-css.mjs` uses pinned development-only `css-tree` parsing/generation without restructuring rules or optimizing declaration values. Preserve selector whitespace, quoted strings, custom properties, and CSS math; parse errors fail the build. Do not restore the removed Python minifier.

`minify-js.mjs` uses pinned development-only esbuild to minify the classic script with an ES5 target, without bundling, source maps, or runtime dependencies. Public pages load only `index.min.js`; keep `index.js` readable as its source.

Repository `.gitattributes` keeps generated `docs/styles.min.css` and `docs/index.min.js` at LF line endings for exact source/build checks across operating systems. Preserve these rules; newline normalization alone does not need an asset cache-version bump.

`lint-js.mjs` uses the repository as ESLint's base, checks all external scripts and any inline HTML scripts, and fails on errors or warnings, including ignored-file warnings. The current tree has one authored script and its generated delivery file, with zero inline scripts. Core ESLint rules enforce ES5 syntax and correctness on both scripts; only generated `docs/index.min.js` is exempt from the six source maintainability limits.

`../docs-tools/README.md` documents the commands, current enforced limits, and regression coverage. Keep that guide aligned with the configs. Preserve cohesive responsibilities rather than packing declarations or fragmenting functions to pass limits.

Future agents must run the applicable tooling after authored CSS or JavaScript changes, including small edits:

- After CSS changes, run `npm run build:css`, then `npm run lint:css` and `npm run check:css-build` from `docs-tools/`. Prefer `npm run build:css` followed by `npm run check`.
- After `index.js` changes, run `npm run build:js` followed by `npm run check`. Inline script changes require `npm run lint:js`; prefer `npm run check`.
- After both CSS and JavaScript changes, run `npm run build` followed by `npm run check`.
- After tooling/config/test changes, run the applicable build if output changes, then `npm run check`.
- Never edit generated `docs/styles.min.css` or `docs/index.min.js` by hand. Bump each shared asset's cache query consistently only when its delivered content changes.
- If dependencies are absent, run `npm ci` first; keep any writable npm cache outside `docs/`.
- If required checks fail to execute, report the environment limitation and do not claim they passed.

The regression suite covers lint coverage, CSS syntax equivalence, JavaScript minification and source/build synchronization, shared asset versions, and theme/cookie/navigation behavior against both authored and delivered scripts. Preserve DOM-readiness and legacy media-API coverage. DOM stubs do not verify browser/device rendering or Lighthouse results; those require separate checks.

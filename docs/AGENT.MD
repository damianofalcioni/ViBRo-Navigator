# ViBRo Navigator Website – Agent Notes

This file defines the current constraints for the static ViBRo Navigator website under `docs/`.
Treat them as the source of truth unless the user explicitly changes a decision.

## Project

- App: ViBRo Navigator
- Repository: `https://github.com/damianofalcioni/ViBRo-Navigator`
- Google Play: `https://play.google.com/store/apps/details?id=vibro.navigator`
- F-Droid: `https://f-droid.org/packages/vibro.navigator/`
- Latest release: `https://github.com/damianofalcioni/ViBRo-Navigator/releases/latest`
- GitHub Sponsors: `https://github.com/sponsors/damianofalcioni`

## Architecture

- Static HTML/CSS site under `docs/`.
- Keep `docs/.nojekyll`.
- Do not add Jekyll, `_config.yml`, a Markdown runtime renderer, or a build generator unless explicitly requested.
- Published legal and changelog content is maintained directly in HTML.
- Shared CSS: `docs/styles.css`.
- Shared theme/cookie logic: `docs/index.js`.
- Keep JavaScript small and dependency-free. It currently handles:
  - sunrise/sunset theme transition
  - theme preference persistence
  - cookie consent
  - themed `<picture>` source synchronization
  - the screenshot light/dark hint word
- The homepage also contains the minimal mobile-navigation behavior.
- Preserve accessibility, responsive behavior, and Lighthouse-oriented performance.

## Homepage structure

`docs/index.html` uses this order:

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
- legal pages and changelog still follow the system theme or saved preference without showing a switch

## Theme system

- Support dark and light themes.
- Default to `prefers-color-scheme`.
- A manual homepage switch overrides the effective theme.
- Respect `prefers-reduced-motion`.
- Avoid many per-element theme animations; use the single page-level solar transition.

Transition direction:
- light → dark: warm yellow → dark orange → dark blue → dark theme
- dark → light: dark blue → dark orange → warm yellow → light theme
- transition duration is about 1.65 seconds
- overlay is viewport-fixed and localized near the right side of the hero logo

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

## Hero

Live banner assets:
- dark: `docs/assets/hero-cyberpunk-background.jpg`
- light: `docs/assets/hero-cyberpunk-background-light.jpg`

Reference/source PNGs:
- `docs/assets/hero-cyberpunk-background.png`
- `docs/assets/hero-cyberpunk-background-light.png`

Rules:
- site loads the optimized JPEGs
- keep the PNGs as source/reference assets
- regenerate JPEGs from PNGs, not from older JPEGs
- full-width cover image
- no baked-in text

Hero logo:
`https://github.com/damianofalcioni/ViBRo-Navigator/raw/main/fastlane/metadata/android/en-US/images/icon.png`

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
- light mode uses subtle translucent blur
- no visible border or shadow

Tagline:
`ROADS • TRAILS • CITIES • BEYOND`

Tagline:
- centered and readable
- light mode may use subtle translucent blur
- no visible border or shadow
- use color contrast instead of text shadow

### Hero downloads

Links:
- Google Play
- F-Droid
- GitHub Releases

Badges:
- Play: `https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png`
- F-Droid: `https://fdroid.gitlab.io/artwork/badge/get-it-on.png`
- GitHub: `https://img.shields.io/badge/GitHub-Releases-181717?style=for-the-badge&logo=github&logoColor=white`

Do not put visible outer boxes around these badges. Keep only clean spacing and subtle hover movement.
The GitHub Releases shield is visually denser than the store badges, so keep it smaller (`32px` image height, max-width about `82%`) to balance the row.
On desktop, its slot uses `16px` left padding so the visible gap between F-Droid and GitHub matches the Google Play/F-Droid spacing more closely.
On the single-column mobile layout, keep Play and F-Droid unshifted and use `16px` top plus `48px` left/right padding on the GitHub Releases slot. Do not use percentage side padding here because it makes the GitHub badge too small on phones.

## Screenshots

Section:
- kicker: `Real app. Real navigation.`
- title: `Screenshots`
- four compact screenshot slots
- 4 columns on desktop, 2 on mobile
- use `<picture>` with dark `<img>` fallback and light `<source data-theme-source>`

Current placeholder assets:
- `docs/assets/screenshot-1-dark-placeholder.jpg`
- `docs/assets/screenshot-1-light-placeholder.jpg`
- `docs/assets/screenshot-2-dark-placeholder.jpg`
- `docs/assets/screenshot-2-light-placeholder.jpg`
- `docs/assets/screenshot-3-dark-placeholder.jpg`
- `docs/assets/screenshot-3-light-placeholder.jpg`
- `docs/assets/screenshot-4-dark-placeholder.jpg`
- `docs/assets/screenshot-4-light-placeholder.jpg`

These are temporary JPEGs. Real screenshots may replace them later without changing the four-slot/themed-`<picture>` structure.
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
- badge: `https://img.shields.io/badge/Sponsor-30363D?style=for-the-badge&logo=GitHub-Sponsors&logoColor=EA4AAA`

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
- `docs/privacy-policy/index.html`
- `docs/terms-of-service/index.html`
- `docs/data-deletion/index.html`
- `docs/store-disclosures/index.html`

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
`docs/CHANGELOG/index.html`

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
- unique `<meta name="description">` on every public HTML page
- media-qualified preload for the system-matching hero JPEG
- `<picture>` for light/dark hero and screenshot variants
- only one initial themed image selected per visual
- `source[data-theme-source]` synchronized by `index.js`
- lazy loading and async decoding for screenshots
- intrinsic screenshot dimensions / 9:16 reserved ratio
- hero intrinsic dimensions and `fetchpriority="high"`
- below-the-fold support badges lazy-loaded

Do not use two simultaneously present hidden `<img>` elements for light/dark variants.

A real Lighthouse score depends on the deployed site, external asset origins, network/browser conditions, and Lighthouse version.

## Delivery

For each requested site revision:
- increment the version
- provide the full ZIP
- provide a patch from the previous version
- preserve unrelated files and content

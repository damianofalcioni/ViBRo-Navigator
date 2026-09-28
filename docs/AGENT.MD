# ViBRo Navigator Website – Agent Notes

This file defines the current constraints for the static ViBRo Navigator website.
Treat them as the source of truth unless the user explicitly changes a decision.

## Project

- App: ViBRo Navigator
- Repository: `https://github.com/damianofalcioni/ViBRo-Navigator`
- Google Play: `https://play.google.com/store/apps/details?id=vibro.navigator`
- F-Droid: `https://f-droid.org/packages/vibro.navigator/`
- Latest release: `https://github.com/damianofalcioni/ViBRo-Navigator/releases/latest`
- GitHub Sponsors: `https://github.com/sponsors/damianofalcioni`

## Architecture

- Static HTML/CSS site.
- Keep `.nojekyll`.
- Do not add Jekyll, `_config.yml`, a Markdown runtime renderer, or a build generator unless explicitly requested.
- Published legal and changelog content is maintained directly in HTML.
- Shared CSS: `styles.css`.
- Shared theme/cookie logic: `index.js`.
- Keep JavaScript small and dependency-free. It currently handles:
  - sunrise/sunset theme transition
  - theme preference persistence
  - cookie consent
  - themed `<picture>` source synchronization
  - the screenshot light/dark hint word
- The homepage also contains the minimal mobile-navigation behavior.
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
- the actual semantic control is `#theme-toggle`, a checkbox with an accessible label; keep it keyboard-focusable on the homepage
- the visible switch is a `<label for="theme-toggle">` with no ARIA switch role; this preserves native click/tap behavior even if JavaScript is unavailable or stale
- `index.js` enhances the native control with the sunrise/sunset transition, image synchronization, and preference persistence, but basic theme switching must not depend on JavaScript
- keep the top-bar action layer above adjacent content and the visible switch explicitly hit-testable (`pointer-events: auto`, `touch-action: manipulation`)
- when shared CSS/JS behavior changes, update the cache-busting query used by public HTML pages so new markup cannot run against stale cached assets
- sticky top bar uses a fully opaque theme background with no backdrop blur/translucency, so scrolling content cannot show through behind the navigation
- on desktop, the primary navigation links share the top-bar surface directly; do not give `.toplinks` a separate card/gradient background or shadow
- the homepage document root provides the `#top` fragment target used by the screenshot theme hint; keep that target separate from the sticky navigation element
- legal pages and changelog have no visible switch, so their hidden theme-state checkbox remains `aria-hidden` / out of the tab order

Self-hosted badges:
- keep badge images local under `assets/`
- current files: `google-play-badge.png`, `fdroid-badge.png`, `github-releases-badge.svg`, `github-sponsor-badge.svg`
- `github-releases-badge.svg` and `github-sponsor-badge.svg` are user-supplied badge assets; preserve these files verbatim rather than regenerating approximations.
- links may remain external, but badge image `src` values must stay local
- do not reintroduce remote `img.shields.io`, `play.google.com` badge-image, or `fdroid.gitlab.io` badge-image URLs

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

Light-mode feature, navigation-mode, innovation, and support cards use a broad, soft outer shadow so the transition into the page background is gradual rather than looking clipped. Keep the blur radius generous and the opacity restrained. Do not apply paint containment to these desktop sections because it clips the outer shadow; the `content-visibility` Lighthouse optimization is mobile-only.

## Hero

Live banner assets:
- primary dark: `assets/hero-cyberpunk-background.webp`
- primary light: `assets/hero-cyberpunk-background-light.webp`

Reference/source PNGs:
- `assets/hero-cyberpunk-background.png`
- `assets/hero-cyberpunk-background-light.png`

Rules:
- use the same wide banner asset on desktop and mobile; do not create a separate mobile crop
- site loads high-quality, full-resolution self-hosted WebP variants first, with the wide JPEGs as fallbacks
- keep the PNGs as source/reference assets
- regenerate WebP/JPEG delivery assets from the PNG sources, not from older compressed files
- full-width cover image with centered `object-position`
- dark and light themes both use the intended synthetic hero overlay for text legibility and visual treatment
- no baked-in text
- hero title, subtitle, tagline, and download buttons stay vertically centered within the banner

Hero logo:
`assets/icon.png`

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
- Play: `assets/google-play-badge.png`
- F-Droid: `assets/fdroid-badge.png`
- GitHub Releases: `assets/github-releases-badge.svg`

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
- `assets/screenshot-1-dark-placeholder.jpg`
- `assets/screenshot-1-light-placeholder.jpg`
- `assets/screenshot-2-dark-placeholder.jpg`
- `assets/screenshot-2-light-placeholder.jpg`
- `assets/screenshot-3-dark-placeholder.jpg`
- `assets/screenshot-3-light-placeholder.jpg`
- `assets/screenshot-4-dark-placeholder.jpg`
- `assets/screenshot-4-light-placeholder.jpg`

These are temporary JPEGs. Real screenshots may replace them later without changing the four-slot/themed-`<picture>` structure; keep an appropriately sized mobile candidate when real screenshots are added.
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
- `styles.css` is the readable source; public pages load the generated `styles.min.css` delivery file.
- the homepage loads `styles.min.css` as a normal render-blocking stylesheet. This avoids post-first-paint restyling/CLS and the associated main-thread layout task.
- unique `<meta name="description">` on every public HTML page
- media-qualified, `fetchpriority="high"` preloads for the system-matching full-width hero WebP
- use the same wide hero image on desktop and mobile
- `<picture>` for light/dark hero and screenshot variants
- only one initial themed image selected per visual
- `source[data-theme-source]` synchronized by `index.js`
- responsive `320w`, `432w`, and `1080w` JPEG candidates for screenshot placeholders
- lazy loading, async decoding, and low fetch priority for screenshots
- explicit image dimensions for store/support badges
- intrinsic screenshot dimensions / 9:16 reserved ratio
- below-the-fold homepage sections use `content-visibility: auto` with intrinsic-size estimates
- below-the-fold support badges stay lazy-loaded
- hero/store image requests below the LCP hero use low fetch priority where appropriate.

Do not use two simultaneously present hidden `<img>` elements for light/dark variants.

All badge images are self-hosted locally, so badge requests should not trigger third-party-cookie findings.
GitHub Pages controls cache response headers, so its short cache TTL cannot be fixed from files under `docs/`.

A real Lighthouse score depends on the deployed site, external asset origins, network/browser conditions, and Lighthouse version.

## Delivery

For each requested site revision:
- increment the version
- provide the full ZIP
- provide a patch from the previous version
- preserve unrelated files and content
- On phone widths, the hero app icon is positioned independently from the heading so it does not shift the `ViBRo Navigator` title off center. The heading occupies the full hero-content width, is center-aligned, and uses symmetric side padding to avoid colliding with the icon.
- Desktop hero is intentionally shorter than before (`560px` above 900px and `545px` from 701–900px), cropping a little more from the top and bottom of the shared wide banner while keeping the hero content vertically centered. Mobile remains content-height driven.
- Desktop/tablet hero is intentionally much shorter to crop substantially more of the wide banner at the top and bottom while keeping the image centered: 420px above 900px and 430px from 701–900px. Mobile hero sizing remains content-driven.
- Desktop/tablet hero cropping is implemented by clipping a v81-sized image box, not by rescaling the banner: the visible hero stays short, while `.hero-bg-image` keeps a 610px virtual height above 900px and 590px at 701–900px, centered vertically. This preserves the v81 image zoom and removes only top/bottom content. Mobile continues to use its content-driven sizing.
- Desktop/tablet hero keeps the v81 image zoom (610px image box above 900px, 590px at 701–900px) but now restores half of the previous vertical crop: the visible hero window is 515px on desktop and 510px on 701–900px widths. Mobile remains content-driven.
- Screenshot placeholders are intentionally simplified to one self-hosted 360px-wide WebP per screenshot/theme (dark and light), used for both desktop and mobile. There are no responsive screenshot variants and no `srcset`/`sizes` complexity for placeholders.
- Hero banners are served in WebP on the homepage (dark and light). PNG originals remain in `assets/` as reference assets; JPG banner derivatives are no longer part of the delivered package.
- On mobile (`max-width: 700px`), the dark hero theme intentionally has no synthetic `::before`/`::after` banner overlay. Mobile light mode keeps the existing overlay, and desktop/tablet keep overlays in both themes.
- Hero overlays are enabled in both dark and light themes on desktop and mobile. Compared with the previous stronger treatment, the dark overlay is intentionally lighter (10%/20%/56% vertical black opacity with subtler side/radial color) so more banner detail remains visible; the light overlay is also slightly reduced.
- Feature cards use custom inline SVG line icons instead of Unicode/emoji. The six icons are vibration, compass, Wi-Fi-off/offline, battery, destination target, and trip controls. Artwork is intentionally sparse: 24×24 viewBox, 1.65px rounded strokes, simple geometry, and very few intersections. Vibration uses an unmistakable phone outline, the compass uses a circle around the original NE↔SW directional rhomboid, offline uses a small filled circle beneath the Wi-Fi arcs plus slash, and the battery is a solid filled green silhouette with no internal lightning mark. The 42px icon tile has clipped corners for a restrained cyberpunk look. Standalone source copies live in `assets/feature-icons/`.
- Feature-icon previews must set CSS `color` to the same accent as the stroke, because filled details such as the Offline Wi-Fi bottom circle use `fill="currentColor"`. Setting only `stroke` makes those fills render with the default text color instead.
- Feature-card headings place the custom SVG icon and the feature title on the same horizontal row; the descriptive paragraph remains below.
- On phone widths (<=700px), the duplicate hero app icon beside the `ViBRo Navigator` heading is hidden because the navigation bar already shows the app icon. The navigation-bar icon remains visible; desktop/tablet hero icon behavior is unchanged.

- v98 Lighthouse cleanup: homepage CSS is render-blocking to eliminate the reported screenshot-section CLS and post-FCP restyle cost; each screenshot/theme remains a single 360×640 WebP asset with matching intrinsic dimensions, with no responsive screenshot variants added.
- v103 crawler metadata: `robots.txt` explicitly allows public crawling, and `llms.txt` provides a concise machine-readable project/site index using portable relative links for site pages plus canonical external project/download links.

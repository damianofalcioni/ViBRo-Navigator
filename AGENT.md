# VibeNavigator Agent Guide

Primary product requirements live in `SPECIFICATION.md` at the repository root. Use this guide together with that specification, and treat `SPECIFICATION.md` as the source of truth when implementation details or feature expectations are unclear.

## Project intent

VibeNavigator is a lightweight Android navigation app built around the installed BRouter app. The product goal is minimal, battery-efficient, offline-first navigation with vibration-led turn guidance and a dark visual theme.

## Stack and build

- Single-module Android app at `app/`
- Language: Java only
- UI base: AppCompat
- Gradle: `8.2`
- Android Gradle Plugin: `8.2.2`
- Java toolchain: `17`
- SDKs in repo today: `compileSdk 34`, `targetSdk 34`, `minSdk 21`
- Runtime dependencies are intentionally minimal: `androidx.appcompat` and `androidx.core`
- Do not add Google Play Services or other heavy dependencies unless explicitly requested

Verified commands from the repository root:

- `.\gradlew.bat test`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat assembleRelease`
- `.\gradlew.bat lint`

CI lives in `.github/workflows/build-apk.yml` and runs tests plus debug/release APK builds.

## Architecture

Main flow:

- `app/src/main/java/com/vibenavigator/MainActivity.java`
  - Main form for vehicle profile selection, destination input, optional stops, and start navigation
  - Accepts shared `geo:` and text intents via `IntentLocationParser`
- `app/src/main/java/com/vibenavigator/NavigationActivity.java`
  - Navigation screen
  - Checks runtime permissions, location enabled state, notifications, and battery optimization exemption before starting the foreground service
- `app/src/main/java/com/vibenavigator/nav/NavigationService.java`
  - Foreground navigation engine
  - Owns location updates, Kalman filtering, reroute logic, blocked-waypoint handling, notifications, and UI state emission
  - Uses `NavigationLifecyclePolicy` for extracted plain-Java lifecycle decisions

Supporting packages:

- `app/src/main/java/com/vibenavigator/brouter/`
  - BRouter AIDL/service bridge, params, profile discovery, and GeoJSON route requests
  - Route requests currently use BRouter native `timode=9` so GeoJSON voice hints preserve beeline and distinct exit-left/exit-right commands
- `app/src/main/java/com/vibenavigator/poi/`
  - POI model, coordinate parsing, search history
- `app/src/main/java/com/vibenavigator/poi/search/`
  - Google geocoding client when `GOOGLE_MAPS_API_KEY` is configured, otherwise OSM Nominatim fallback
- `app/src/main/java/com/vibenavigator/poi/ui/`
  - Input controller and popup suggestion UI shared by destination and stop fields
- `app/src/main/java/com/vibenavigator/nav/route/`
  - GeoJSON parsing, track model, polyline matching
- `app/src/main/java/com/vibenavigator/nav/directions/`
  - Voice-hint mapping from BRouter codes to in-app direction semantics
- `app/src/main/java/com/vibenavigator/nav/kalman/`
  - Lightweight filtering for location smoothing
- `app/src/main/java/com/vibenavigator/util/AppLogger.java`
  - File-based app logging used across app startup, routing, search, and navigation

Tests currently live in:

- `app/src/test/java/com/vibenavigator/geo/`
- `app/src/test/java/com/vibenavigator/`
- `app/src/test/java/com/vibenavigator/nav/directions/`
  - Includes voice-hint mapping coverage for the current BRouter mode-9 command table and user-visible symbols
- `app/src/test/java/com/vibenavigator/nav/kalman/`

Current test strategy:

- Prefer JVM tests only
- Lifecycle coverage that would normally require instrumentation should be implemented with Robolectric where practical
- Do not add tests that require a real device or emulator unless explicitly requested
- Current lifecycle coverage includes host-side tests for navigation back-button behavior, foreground re-promotion, and `onTaskRemoved()` shutdown
- Keep pure lifecycle rules in `NavigationLifecyclePolicy` when practical so they can also be covered by plain JUnit tests

## Project rules

- Keep the implementation lightweight. Prefer platform APIs and small local helpers over new libraries.
- Keep code in Java.
- Preserve the dark/black theme and support both portrait and landscape layouts.
- Do not hardcode user-facing text in Java or XML layouts. Add or update strings in `app/src/main/res/values/strings.xml`.
- Keep README/about content aligned. `README.md` and `about_body` in `strings.xml` intentionally describe the same product.
- Keep the app usable without Google APIs. Google search is optional and gated by `GOOGLE_MAPS_API_KEY`.
- Preserve support for shared coordinates/addresses and `geo:` deep links.
- Preserve background and screen-off navigation behavior. Changes affecting the foreground service, wake lock, notifications, or battery optimization flow need extra scrutiny.
- Preserve BRouter compatibility. Route requests should continue using the local BRouter service and GeoJSON output.
- Preserve the current BRouter voice-hint contract unless there is a deliberate product change: mode `9`, distinct `beeline`, `exit left`, and `exit right`, and a neutral unknown fallback.
- Keep profile handling compatible with both bundled BRouter profiles and user-selected external `profiles2` folders.
- Keep notification behavior tied to turn timing and left/right vibration patterns.
- Prefer extending the existing logging with `AppLogger` when touching startup, permissions, routing, background execution, or network search behavior.

## Editing guidance

- If you change navigation state, rerouting, route parsing, voice-hint mapping, or geometry helpers, add or update unit tests.
- If you change BRouter voice-hint mapping, keep the mode-9 command coverage and symbol assertions aligned in `VoiceHintMapperTest`.
- If you change navigation/task/foreground-service lifecycle behavior, prefer updating the Robolectric JVM tests under `app/src/test/java/com/vibenavigator/`.
- If you change manifest-declared components or permissions, verify the corresponding runtime checks in `NavigationActivity`.
- If you change BRouter request parameters or response parsing, inspect both `brouter/` and `nav/route/` code paths together.
- If you change POI search behavior, keep direct coordinate entry working and keep history suggestions available when the field is focused and empty.
- If you change intent/deep-link destination handling or POI binding, keep externally opened/shared locations flowing through the same history behavior as manual destination selection.
- If you change icon/theme/about assets, preserve the app identity: minimal, black-theme, vibration-first navigation.

## Local config

- Optional Google key: define `GOOGLE_MAPS_API_KEY` in `local.properties` or the environment
- BRouter must be installed on the device as `btools.routingapp`
- The app may use a user-selected document tree for external custom `.brf` profiles

## Practical review checklist

- Does the change keep the app dependency-light?
- Are new strings localized through `strings.xml`?
- Does the change preserve offline-first behavior when no Google key is present?
- Does navigation still work in background and with screen off?
- Are permissions/settings prompts still reachable for location, notifications, and battery optimization?
- Are BRouter profile selection and route calculation still intact?

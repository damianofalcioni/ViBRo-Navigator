# VibeNavigator Agent Guide

Primary product requirements live in `SPECIFICATION.md` at the repository root. On the first implementation-oriented interaction in this repository, read `SPECIFICATION.md` before making changes. Use this guide together with that specification, and treat `SPECIFICATION.md` as the source of truth when implementation details or feature expectations are unclear.

## Stack and build

- Single-module Android app at `app/`
- Language: Java only
- UI base: platform `Activity` + platform widgets/dialogs, with `androidx.core` kept for compatibility helpers
- Gradle: `8.2`
- Android Gradle Plugin: `8.2.2`
- Java toolchain: `17`
- SDKs in repo today: `compileSdk 34`, `targetSdk 34`, `minSdk 21`
- Runtime dependencies are intentionally minimal: `androidx.core`
- Do not add Google Play Services or other heavy dependencies unless explicitly requested

Verified commands from the repository root:

- `.\gradlew.bat test`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat assembleRelease`
- `.\gradlew.bat lint`
- `.\gradlew.bat lintDebug`

CI lives in `.github/workflows/build-apk.yml` and runs tests plus debug/release APK builds.

## Architecture

- `MainActivity` should stay thin and delegate profile selection, stop rows, incoming intents, and navigation input validation.
- `MapPickerActivity` owns manual map-based point picking for destination and stop fields. Keep it dependency-light: use the existing local WebView asset approach for OpenStreetMap raster tiles instead of introducing a native map SDK unless explicitly requested.
- `NavigationActivity` should stay focused on rendering, service binding, and task/back-button behavior. Startup checks belong in `NavigationStartupCoordinator`.
- The navigation screen's center visualization is a custom `NavigationCompassView` fed by lightweight navigation state. Keep route geometry preparation out of the view and keep Android drawing concerns out of the service/session logic.
- Display-relative compass heading preparation belongs outside `NavigationCompassView`. Keep screen-rotation compensation in the heading/state pipeline, and keep raw geomagnetic heading available for non-UI orientation logic such as stationary turn-to-face-route advice.
- `NavigationService` is the Android lifecycle shell. Keep notification handling, location subscriptions, wake locks, route execution, listener broadcasting, and turn notification fan-out delegated to focused collaborators.
- `NavigationSession` is the session-level coordinator. Keep filtered location, route progress, blocked-road state, turn progression, and route-request lifecycle split across dedicated state/policy classes instead of collapsing them back together.
- Keep reroute heuristics split between bearing-source trust, route-deviation policy, and route-state progress confirmation. Wrong-direction detection should continue to use forward-looking route bearing plus along-track direction-of-progress evidence instead of relying on a raw matched-segment bearing alone.
- `NavigationRequest` owns the shared navigation extras contract used by activities, the service, and resume notifications.
- `NavigationTextFormatter` owns shared user-visible navigation/notification formatting.
- `brouter/` owns BRouter service integration, routing params, profile discovery, and GeoJSON route requests. Preserve the current `timode=9` voice-hint contract unless there is a deliberate product change.
- `poi/`, `poi/search/`, and `poi/ui/` own POI parsing, history, provider-backed search, and shared suggestion UI. Keep search execution shared across inputs.
- `app/src/main/assets/map_picker.html` is the current map-rendering implementation for destination/stop selection. Preserve tap-to-select, drag-to-pan, pinch-to-zoom, button-based zoom/current-location controls, and safe gesture handling so pinch release does not mutate the selected point.
- `nav/route/`, `nav/directions/`, and `nav/kalman/` hold route parsing/matching, voice-hint mapping, and location smoothing.
- `util/AppLogger` is the shared file logger. Single-line and multiline writes should continue to use the same formatting and append path.

## Test strategy

- Prefer JVM tests. Use Robolectric when Android lifecycle coverage is needed.
- Do not add emulator/device requirements to the core automated suite unless explicitly requested.
- Keep lifecycle rules, heuristics, planners, and policy thresholds in small helpers when practical so they stay directly unit-testable.
- Keep focused coverage around navigation startup/preflight, request serialization, reroute heuristics, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route callback handoff, turn-event dispatch, and state broadcasting.
- Refactors that only move unchanged wiring into thin helpers do not need new tests by default. Behavior changes do.

## Project rules

- Keep repository documentation aligned with the code when relevant changes are made. Update `SPECIFICATION.md` when product behavior, requirements, or user-visible flows change. Update `AGENT.md` when architecture, guardrails, workflows, or coding expectations change. Do not make doc-only churn for code changes that do not affect those areas.
- Keep README/about content aligned at the product-description level. `README.md` and `about_body` in `strings.xml` should stay consistent about the app's purpose and core behavior, but they do not need to be literal copies of each other.

## Editing guidance

- Add or update tests when you change navigation state, rerouting, route parsing, voice-hint mapping, geometry helpers, or user-visible behavior.
- Keep background route computation separated from main-thread state mutation. Preserve `NavigationRouteExecutor` rather than inlining thread management back into `NavigationService`.
- Keep Android service concerns delegated through the existing foreground/location/wakelock/dispatch/broadcast helpers, and keep `NavigationSession` as a coordinator over focused session collaborators.
- If you change reroute thresholds, polling cadence, turn-alert timing, blocked-road escalation, or route-request lifecycle behavior, update the corresponding `nav/` tests.
- If you change guidance confidence rules, keep map-free use in mind and update tests around bearing trust, forward-look route bearing, direction-of-progress, turn suppression, and duplicate/imminent alert behavior.
- If you change navigation intent extras, update `NavigationRequest` first and keep resume/start flows serialized through it instead of hand-copying extras.
- If you change startup permission/settings/battery-optimization flow, keep `NavigationActivity` thin and update the startup/lifecycle tests.
- If you change BRouter request parameters, response parsing, or voice-hint mapping, inspect both `brouter/` and `nav/route/` paths together and keep mode-9 coverage aligned.
- If you change POI search or incoming intent handling, preserve coordinate entry, empty-field history suggestions, shared search dispatch, and history behavior for externally opened locations.
- If you change the map picker, preserve the no-external-library constraint, OSM raster tile rendering, current-location fallback when a field has no coordinates yet, restored-selection behavior across rotation, and the icon-only control layout.
- If you change logging, keep the shared `buildLogPrefix`/`appendBlock` style intact so formatting and file-rotation behavior stay consistent.
- If you change icon/theme/about assets, preserve the app identity: minimal, black-theme, vibration-first navigation.
- After any code update, always run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat lintDebug` before closing the task.
- At the end of implementation work, always ask whether to do a fresh recompile and install on a connected phone if one is available, and if there are next-step suggestions, propose those as well.

## Local config

- Optional Google key: define `GOOGLE_MAPS_API_KEY` in `local.properties` or the environment
- BRouter must be installed on the device as `btools.routingapp`
- The app may use a user-selected document tree for external custom `.brf` profiles
- Destination/stop map picking currently requires only platform WebView plus network access to `tile.openstreetmap.org`; do not replace it with an external map dependency unless explicitly requested

## Practical review checklist

- Does the change keep the app dependency-light?
- Are new strings localized through `strings.xml`?
- Does the change preserve offline-first behavior when no Google key is present?
- Does the change keep the destination/stop map picker dependency-free and working in both portrait and landscape?
- Does navigation still work in background and with screen off?
- Are permissions/settings prompts still reachable for location, notifications, and battery optimization?
- Are BRouter profile selection and route calculation still intact?
- Would the resulting turn guidance still be safe to trust without looking at a map?

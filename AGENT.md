# ViBRo Navigator Agent Guide

Primary product requirements live in `SPECIFICATION.md` at the repository root. On the first implementation-oriented interaction in this repository, read `SPECIFICATION.md` before making changes. Use this guide together with that specification, and treat `SPECIFICATION.md` as the source of truth when implementation details or feature expectations are unclear.

## Stack and build

- Single-module Android app at `app/`
- Language: Java only
- UI base: platform `Activity` + platform widgets/dialogs, with `androidx.core` kept for compatibility helpers
- Keep navigation back handling compatible with predictive back using platform callbacks; do not migrate the app shell to `ComponentActivity` unless explicitly requested
- Gradle: `9.4.1`
- Android Gradle Plugin: `9.1.1`
- Java toolchain: `17`
- SDKs in repo today: `compileSdk 36`, `targetSdk 36`, `minSdk 21`
- Runtime dependencies are intentionally minimal: `androidx.core`
- Do not add Google Play Services or other heavy dependencies unless explicitly requested

Verified commands from the repository root:

- `.\gradlew.bat test`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat assembleRelease`
- `.\gradlew.bat complexityCheck`
- `.\gradlew.bat aiMaintainabilitySweep`
- `.\gradlew.bat lint`
- `.\gradlew.bat lintDebug`

CI lives in `.github/workflows/build-apk.yml` and runs tests plus debug/release APK builds.

Distribution-related workflows:

- `.github/workflows/fdroid-ready.yml` validates upstream F-Droid readiness: fastlane metadata presence, version/tag consistency, lint/complexity/tests, and unsigned release APK generation.
- `.github/workflows/fdroid-submit.yml` is a maintainer-operated workflow that renders `fdroid/vibro.navigator.yml`, pushes it to a GitLab `fdroiddata` fork, and opens or reuses a merge request. It does not complete official publication by itself.
- `fdroid/SUBMISSION.md` is maintainer-facing runbook documentation for the official F-Droid submission flow. Treat it as operator documentation, not as an agent-only instruction file.
- `fdroid/vibro.navigator.yml` is a draft metadata template for `fdroiddata`; keep its placeholders and release fields aligned with the real upstream repo, tag, and versioning strategy.

## Architecture

- `MainActivity` should stay thin and delegate profile selection, stop rows, incoming intents, and navigation input validation. Keep routing-profile spinner option construction and restore-position logic in `ProfileSpinnerOptions`/`ProfileSpinnerOption` rather than folding that state machine back into the activity or spinner controller.
- `MapPickerActivity` owns manual map-based point picking for destination and stop fields. Keep it dependency-light: use the existing local WebView asset approach for OpenStreetMap raster tiles instead of introducing a native map SDK unless explicitly requested. Keep current-location permission/provider/fresh-fix handling in `MapPickerLocationController` so the activity stays focused on WebView map state and result selection.
- `NavigationActivity` should stay focused on service binding, startup coordination, and task/back-button behavior. Navigation-screen view binding, text scaling, compass mode rendering, GPS status rendering, and navigation-control click wiring belong in `NavigationActivityRenderer`; startup checks belong in `NavigationStartupCoordinator`.
- Treat pause/resume as real navigation-session state, not as a UI-only toggle. `NavigationActivity` and `NavigationActivityRenderer` should only render/invoke binder actions; the paused/running behavior must stay owned by `NavigationService` and `NavigationSession`.
- `NavigationActivity` must remain a platform `Activity`. If back behavior changes, preserve the current combination of legacy `onBackPressed()` handling plus platform predictive-back registration for API 33+ instead of switching the screen to `ComponentActivity`.
- The navigation screen's center visualization is a custom `NavigationCompassView` fed by lightweight navigation state. Keep route geometry preparation out of the view and keep Android drawing concerns out of the service/session logic. `NavigationCompassView` should compose compass chrome and delegate route drawing to `NavigationCompassRouteRenderer`, route start/destination/hint markers to `NavigationCompassRouteMarkerRenderer`, route projection math to `NavigationCompassRouteProjector`, and distance/time legend drawing to `NavigationCompassLegendRenderer`. Compass route/hint sampling belongs in `CompassRouteGeometryFactory`; automatic compass zoom/radius reuse/smoothing belongs in `CompassRadiusResolver`.
- Display-relative compass heading preparation belongs outside `NavigationCompassView`. Keep screen-rotation compensation and heading-accuracy mapping in `NavigationDisplayHeading`; keep heading-sensor lifecycle, stationary-orientation notification wiring, and compass UI refresh throttling in `NavigationOrientationController`; and keep raw heading-sensor output available for non-UI orientation logic such as stationary turn-to-face-route advice. Prefer geomagnetic rotation vector when the device exposes it, but support rotation-vector fallback devices too.
- `NavigationService` is the Android lifecycle shell. Keep notification handling, location subscriptions, wake locks, route execution, listener broadcasting, screen-interactivity monitoring, orientation/display-heading preparation, and turn notification fan-out delegated to focused collaborators.
- Stationary turn-to-face-route notification episode state belongs in `StationaryOrientationNotifier`; keep `NavigationOrientationController` responsible for wiring lifecycle inputs and foreground notification sinks rather than moving the dwell/notify/reset policy into `NavigationService`.
- If pause/resume behavior changes, preserve the current contract that pausing retains the active request/route while suspending live location, reroute, and turn-notification processing until resume.
- Long-lived navigation reliability should come from the location foreground service and ongoing location callbacks, not from a session-long CPU wake lock.
- Any partial wake lock used by navigation must stay short, explicit, and owned by the collaborator performing the critical burst of work, such as route or reroute calculation.
- `NavigationSession` is the session-level coordinator. Keep filtered location, route progress, blocked-road state, turn progression, route display state, and route-request lifecycle split across dedicated state/policy classes instead of collapsing them back together. Rolling route-progress samples for smoothed accuracy, ETA speed, and direction-of-progress evidence belong in `NavigationRouteProgressTracker`.
- `NavigationSessionRouteState` owns active-route evaluation: matching, arrival, deviation, blocked-road point selection, and turn-event handoff. Keep compass geometry, visible-radius memory, GPS status wrapping, target progress labels, and `NavState` construction in `NavigationSessionRouteDisplayState` so route safety decisions and route presentation stay independently understandable.
- Keep reroute heuristics split between bearing-source trust, route-deviation policy, deviation confirmation, and route-state progress handling. Wrong-direction detection should continue to use forward-looking route bearing plus along-track direction-of-progress evidence instead of relying on a raw matched-segment bearing alone. Consecutive-sample confirmation and speed-sensitive immediate off-track margins belong in `NavigationDeviationConfirmation`.
- Startup last-known-location freshness and quality selection belongs in `NavigationStartupLocationSelector`; keep `NavigationLocationController` focused on orchestrating Android location tracking state and active-subscription reuse. Provider permission checks, enabled-provider selection, last-known reads, and continuous update requests belong in `NavigationLocationProviderAccess`. Android 11+ one-shot current-location seed requests and their cancellation state belong in `NavigationCurrentLocationSeeder`. Foreground navigation GNSS satellite-count callback state belongs in `NavigationGnssStatusTracker`.
- `NavigationRequest` owns the shared navigation extras contract used by activities, the service, and resume notifications.
- `NavState` is the immutable navigation display snapshot. Keep route direction/progress line assembly in `NavStateTextFactory`, compass-state assembly in `NavCompassStateFactory`, automatic compass radius policy in `CompassRadiusResolver`, and primitive user-visible navigation/notification formatting in `NavigationTextFormatter`. Keep GPS status field formatting and validity checks in `NavigationGpsTextFormatter` so turn/off-route copy and GPS telemetry copy remain separate.
- `brouter/` owns BRouter service integration, routing params, profile discovery, and GeoJSON route requests. Keep external `profiles2` document-tree probing in `BRouterProfileDirectories` and document-ID candidate ordering in `BRouterProfileDirectoryCandidates`, rather than folding Android storage probing back into `BRouterProfilesRepository`. Preserve the current `timode=9` voice-hint contract unless there is a deliberate product change.
- `poi/`, `poi/search/`, and `poi/ui/` own POI parsing, history, provider-backed search, and shared suggestion UI. Keep search execution shared across inputs. Keep `PoiInputController` focused on the text field, selection, and history-row edit/delete UI; keep popup window safety/presentation in `PoiSuggestionPopupController`; keep query precedence, debounce timing, direct-coordinate suggestions, history-match priority, and async provider search in `PoiSuggestionSearchController`.
- Incoming shared/opened location parsing is split under `util/`: `IntentLocationParser` handles intent/shared-text orchestration, `IntentLocationUriParser` handles URI scheme dispatch, `IntentWebMapUriParser` handles Google Maps/OpenStreetMap web URLs, and the coordinate/query/decode helpers keep the parsing mechanics isolated.
- `app/src/main/assets/map_picker.html` is the current map-rendering implementation for destination/stop selection. Preserve tap-to-select, drag-to-pan, pinch-to-zoom, button-based zoom/current-location controls, and safe gesture handling so pinch release does not mutate the selected point.
- `nav/route/`, `nav/directions/`, and `nav/kalman/` hold route parsing/matching, voice-hint mapping, and location smoothing.
- About-page developer diagnostics are split between `AboutSensorStatusFormatter` for status orchestration, `AboutGnssStatusTracker` for GNSS satellite tracking, and `AboutSensorValueFormatter` for pure diagnostic value formatting.
- `util/AppLogger` is the shared logging facade. Single-line and multiline writes should continue to use the same `buildLogPrefix` formatting and `AppLogFiles` append path; keep log storage selection in `AppLogStorage` and trimming/migration/recreation in `AppLogFileMaintenance`.

## Test strategy

- Prefer JVM tests. Use Robolectric when Android lifecycle coverage is needed.
- Do not add emulator/device requirements to the core automated suite unless explicitly requested.
- Keep lifecycle rules, heuristics, planners, and policy thresholds in small helpers when practical so they stay directly unit-testable.
- Keep focused coverage around navigation startup/preflight, request serialization, reroute heuristics, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route callback handoff, turn-event dispatch, and state broadcasting.
- Changes to pause/resume behavior should add or update focused JVM coverage for session state and any service-policy decisions that depend on paused navigation.
- `.\gradlew.bat complexityCheck` runs PMD cyclomatic and cognitive complexity thresholds over production Java sources. The task has a zero-violation baseline and should fail on any reported violation; treat violations as refactor candidates, with priority for navigation/routing safety logic and frequently edited classes.
- `.\gradlew.bat aiMaintainabilitySweep` runs a stricter non-gating PMD sweep over production Java sources using AI-agent-friendly maintainability rules. It is allowed to report existing violations and should be used as a refactor backlog, not as a release gate.
- Refactors that only move unchanged wiring into thin helpers do not need new tests by default. Behavior changes do.

## Project rules

- Keep repository documentation aligned with the code when relevant changes are made. Update `SPECIFICATION.md` when product behavior, requirements, or user-visible flows change. Update `AGENT.md` when architecture, guardrails, workflows, or coding expectations change. Do not make doc-only churn for code changes that do not affect those areas.
- Keep README/about content aligned at the product-description level. `README.md` and `about_body` in `strings.xml` should stay consistent about the app's purpose and core behavior, but they do not need to be literal copies of each other.
- When release/distribution mechanics change, keep `.github/workflows/fdroid-ready.yml`, `.github/workflows/fdroid-submit.yml`, `fastlane/metadata/android/en-US/...`, `fdroid/vibro.navigator.yml`, and `fdroid/SUBMISSION.md` aligned so the maintainer-facing F-Droid process remains accurate.

## Editing guidance

- Add or update tests when you change navigation state, rerouting, route parsing, voice-hint mapping, geometry helpers, or user-visible behavior.
- Keep background route computation separated from main-thread state mutation. Preserve `NavigationRouteExecutor` rather than inlining thread management back into `NavigationService`.
- Keep BRouter transient-failure classification in `BRouterTransientFailureClassifier` so `NavigationRouteExecutor` stays focused on route execution, retry timing, callback handoff, and wake-lock scoping.
- Keep Android service concerns delegated through the existing foreground/location/wakelock/dispatch/broadcast helpers, and keep `NavigationSession` as a coordinator over focused session collaborators.
- Do not add a wake-lock renewal loop or reintroduce a session-lifetime wake lock to keep navigation alive; if a path needs a wake lock, scope it to the shortest critical section that actually needs CPU residency.
- If you change reroute thresholds, polling cadence, turn-alert timing, blocked-road escalation, or route-request lifecycle behavior, update the corresponding `nav/` tests.
- If you change guidance confidence rules, keep map-free use in mind and update tests around bearing trust, forward-look route bearing, direction-of-progress, turn suppression, and duplicate/imminent alert behavior.
- If you change navigation intent extras, update `NavigationRequest` first and keep resume/start flows serialized through it instead of hand-copying extras.
- If you change startup permission/settings/battery-optimization flow, keep `NavigationActivity` thin and update the startup/lifecycle tests.
- If you change BRouter request parameters, response parsing, or voice-hint mapping, inspect both `brouter/` and `nav/route/` paths together and keep mode-9 coverage aligned.
- If you change POI search or incoming intent handling, preserve coordinate entry, empty-field history suggestions, history-before-online precedence from the first typed character, shared search dispatch, and history behavior for externally opened locations.
- If you change the map picker, preserve the no-external-library constraint, OSM raster tile rendering, current-location fallback when a field has no coordinates yet, restored-selection behavior across rotation, and the icon-only control layout.
- If you change logging, keep the shared `buildLogPrefix` plus `AppLogFiles.appendBlock` path intact so formatting, developer-mode gating, file-session selection, trimming, and legacy migration behavior stay consistent.
- When extracting helpers around Android APIs, preserve lint-visible SDK guards with `@RequiresApi`, guarded callers, or min-SDK-safe overloads. In particular, avoid newer Java/Android overloads such as `URLEncoder.encode(String, Charset)` unless desugaring/minSdk support is already verified by `lintDebug`.
- If you change icon/theme/about assets, preserve the app identity: minimal, black-theme, vibration-first navigation.
- After any code update, always run `.\gradlew.bat testDebugUnitTest`, `.\gradlew.bat complexityCheck`, and `.\gradlew.bat lintDebug` before closing the task.
- At the end of implementation work, always ask whether to do a fresh recompile and install on a connected phone if one is available, and if there are next-step suggestions, propose those as well.

## Local config

- Optional Google key: define `GOOGLE_MAPS_API_KEY` in `local.properties` or the environment
- BRouter must be installed on the device as `btools.routingapp`
- The app may use a user-selected document tree for external custom `.brf` profiles
- Destination/stop map picking currently requires only platform WebView plus network access to `tile.openstreetmap.org`; do not replace it with an external map dependency unless explicitly requested
- `local.properties` is developer-local configuration. Do not make release or F-Droid flows depend on committed machine-specific values.

## Practical review checklist

- Does the change keep the app dependency-light?
- Are new strings localized through `strings.xml`?
- Does the change preserve offline-first behavior when no Google key is present?
- Does the change keep the destination/stop map picker dependency-free and working in both portrait and landscape?
- Does navigation still work in background and with screen off?
- Are permissions/settings prompts still reachable for location, notifications, and battery optimization?
- Are BRouter profile selection and route calculation still intact?
- Would the resulting turn guidance still be safe to trust without looking at a map?

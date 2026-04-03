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
  - Should stay a thin UI/activity layer that delegates profile selection and navigation-input resolution
  - Uses `ProfileSpinnerController` for profile spinner state and document-picker related selection flow
  - Uses `NavigationInputResolver` for destination/stop validation and history persistence before launch
  - Accepts shared `geo:` and text intents via `IntentLocationParser`
- `app/src/main/java/com/vibenavigator/NavigationActivity.java`
  - Navigation screen
  - Should stay focused on screen rendering, service binding, and task/back-button behavior
  - Delegates permission/settings/battery-optimization checks and service-start orchestration to `NavigationStartupCoordinator`
- `app/src/main/java/com/vibenavigator/nav/NavigationStartupCoordinator.java`
  - Owns navigation startup/preflight orchestration before foreground-service launch
  - Uses `NavigationPreflight` for inspection and a small host interface for permission requests, dialogs, and service startup
- `app/src/main/java/com/vibenavigator/nav/NavigationService.java`
  - Foreground-service shell for binding, lifecycle callbacks, navigation start/stop, and delegation to focused collaborators
  - Delegates Android notification rendering/promotion to `NavigationForegroundController`
  - Delegates foreground-notification monitoring and task-removal lifecycle policy to `NavigationForegroundCoordinator`
  - Delegates Android location-provider subscription, last-known/current-location seeding, and provider bookkeeping to `NavigationLocationController`
  - Delegates wake-lock ownership to `NavigationWakeLockController`
  - Delegates navigation session orchestration, reroute requests, blocked-waypoint handling, and route-result application to `NavigationSession`
  - Delegates async route calculation and callback handoff to `NavigationRouteExecutor`
  - Delegates listener registration and safe state fan-out to `NavigationStateBroadcaster`
  - Delegates initial/imminent/passed turn notification fan-out to `NavigationTurnEventDispatcher`
  - Shared navigation state must only be committed back on the main thread after background route execution completes
- `app/src/main/java/com/vibenavigator/nav/NavigationSession.java`
  - Thin coordinator for session-level workflow and the public API used by `NavigationService`
  - Delegates filtered-location ownership and live-location arbitration to `NavigationSessionLocationState`
  - Delegates active-route progress, blocked-road escalation, turn-event generation, and `NavState` construction to `NavigationSessionRouteState`
  - Delegates reroute throttling, request tokens, and route failure state to `NavigationRouteRequestManager`
- `app/src/main/java/com/vibenavigator/nav/NavigationSessionLocationState.java`
  - Owns live-location arbitration, Kalman filtering, and derived speed/bearing/accuracy calculations for the current session
- `app/src/main/java/com/vibenavigator/nav/NavigationSessionRouteState.java`
  - Owns active-route progress, blocked-road no-go memory/escalation, initial/imminent/passed turn events, and `NavState` rendering inputs
  - Delegates route-deviation thresholds to `RouteDeviationPolicy`
  - Delegates adaptive polling cadence to `NavigationUpdateScheduler`
  - Delegates upcoming-turn alert progression to `TurnEventPlanner`
- `app/src/main/java/com/vibenavigator/nav/NavigationRouteRequestManager.java`
  - Owns reroute throttling, stale-request rejection, in-flight route-calculation state, and route-failure summarization
- `app/src/main/java/com/vibenavigator/nav/NavigationRequest.java`
  - Owns the shared extras contract plus parsing/serialization for navigation intents passed between activities and the service
  - Also defines the resume-notification request contract so new navigation extras are not rebuilt manually in multiple places
- `app/src/main/java/com/vibenavigator/nav/NavigationTextFormatter.java`
  - Shared user-visible formatting for turn notifications and navigation-state text
  - Keeps distance/time/ETA rendering aligned across notification and on-screen surfaces

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
  - `PoiSearchDispatcher` provides shared background execution for POI lookups so each field does not own its own executor
- `app/src/main/java/com/vibenavigator/nav/route/`
  - GeoJSON parsing, track model, polyline matching
- `app/src/main/java/com/vibenavigator/nav/directions/`
  - Voice-hint mapping from BRouter codes to in-app direction semantics
- `app/src/main/java/com/vibenavigator/nav/kalman/`
  - Lightweight filtering for location smoothing
- `app/src/main/java/com/vibenavigator/nav/NavigationPreflight.java`
  - Shared inspection of runtime permissions, settings prerequisites, and battery-optimization state
  - Exposes a testable `Status` value used by `NavigationStartupCoordinator`
- `app/src/main/java/com/vibenavigator/nav/LiveLocationCoordinator.java`
  - Isolated GPS/network arbitration and stale/duplicate location suppression
- `app/src/main/java/com/vibenavigator/nav/NavigationForegroundController.java`
  - Encapsulates notification channels, foreground promotion, ongoing-notification visibility checks, and turn-notification dispatch
- `app/src/main/java/com/vibenavigator/nav/NavigationForegroundCoordinator.java`
  - Encapsulates ongoing-notification monitoring, binder-triggered foreground restoration, and task-removal stop policy around `NavigationLifecyclePolicy`
- `app/src/main/java/com/vibenavigator/nav/NavigationLocationController.java`
  - Encapsulates `LocationManager` subscriptions, last-known/current-location retrieval, provider enable/disable response, and deadline tracking
- `app/src/main/java/com/vibenavigator/nav/NavigationRouteExecutor.java`
  - Encapsulates background route calculation, executor ownership, empty-route rejection, and main-thread callback handoff
- `app/src/main/java/com/vibenavigator/nav/NavigationTurnEventDispatcher.java`
  - Encapsulates initial/imminent/passed turn-event logging plus notification fan-out
- `app/src/main/java/com/vibenavigator/nav/NavigationStateBroadcaster.java`
  - Encapsulates listener registration, removal, clearing, and exception-safe state broadcasting
- `app/src/main/java/com/vibenavigator/nav/NavigationWakeLockController.java`
  - Encapsulates partial wake-lock acquisition/release for active navigation
- `app/src/main/java/com/vibenavigator/nav/RouteDeviationPolicy.java`
  - Plain-Java reroute-threshold policy for off-track and wrong-direction decisions
- `app/src/main/java/com/vibenavigator/nav/NavigationUpdateScheduler.java`
  - Plain-Java policy for adaptive location polling intervals near the next hint
- `app/src/main/java/com/vibenavigator/nav/TurnEventPlanner.java`
  - Plain-Java planner for initial/imminent/passed turn-event generation
- `app/src/main/java/com/vibenavigator/util/AppLogger.java`
  - File-based app logging used across app startup, routing, search, and navigation
  - Single-line and multiline writes must continue to share the same formatting and append path

Tests currently live in:

- `app/src/test/java/com/vibenavigator/geo/`
- `app/src/test/java/com/vibenavigator/`
- `app/src/test/java/com/vibenavigator/nav/directions/`
  - Includes voice-hint mapping coverage for the current BRouter mode-9 command table and user-visible symbols
- `app/src/test/java/com/vibenavigator/nav/kalman/`
- `app/src/test/java/com/vibenavigator/nav/`
  - Includes focused JVM coverage for `NavigationRequest`, `NavigationStartupCoordinator`, `LiveLocationCoordinator`, `RouteDeviationPolicy`, `NavigationUpdateScheduler`, `TurnEventPlanner`, `NavigationRouteRequestManager`, and `NavigationSessionRouteState`
  - Includes focused JVM coverage for `NavigationForegroundCoordinator`, `NavigationRouteExecutor`, and `NavigationTurnEventDispatcher`

Current test strategy:

- Prefer JVM tests only
- Lifecycle coverage that would normally require instrumentation should be implemented with Robolectric where practical
- Do not add tests that require a real device or emulator unless explicitly requested
- Current lifecycle coverage includes host-side tests for navigation back-button behavior, foreground re-promotion, and `onTaskRemoved()` shutdown
- Notification-resume intent serialization is covered with Robolectric and should stay aligned with `NavigationRequest`
- Navigation startup/preflight branching is covered through `NavigationStartupCoordinatorTest` and should stay off the activity itself unless UI behavior truly requires it
- Keep pure lifecycle rules in `NavigationLifecyclePolicy` when practical so they can also be covered by plain JUnit tests
- Keep navigation heuristics in plain-Java policy/planner helpers when practical so threshold changes stay directly unit-testable
- Keep service-side orchestration seams in extracted helpers when practical so notification monitoring, route callback handoff, turn-event fan-out, and listener broadcasting stay directly unit-testable without inflating `NavigationService`

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
- Keep `AppLogger` on one shared file-append path. If logging behavior changes, do not reintroduce separate write flows for single-line versus multiline entries.

## Editing guidance

- If you change navigation state, rerouting, route parsing, voice-hint mapping, or geometry helpers, add or update unit tests.
- If you change `NavigationService` or `NavigationSession` route-execution flow, keep background route computation separated from main-thread state mutation and preserve the `NavigationRouteExecutor` seam instead of inlining thread management back into the service.
- If you change navigation-state ownership, keep Android service concerns delegated through `NavigationForegroundController`, `NavigationForegroundCoordinator`, `NavigationLocationController`, `NavigationTurnEventDispatcher`, `NavigationStateBroadcaster`, and `NavigationWakeLockController`, while `NavigationSession` remains a coordinator over focused session collaborators instead of reabsorbing location, route-state, or request-lifecycle details.
- If you change reroute thresholds, dynamic polling cadence, or turn-alert timing, update the corresponding policy/planner tests under `app/src/test/java/com/vibenavigator/nav/`.
- If you change filtered-location ownership, blocked-road escalation, or reroute-throttling behavior, update the corresponding tests for `NavigationSessionLocationState`, `NavigationSessionRouteState`, or `NavigationRouteRequestManager`.
- If you change BRouter voice-hint mapping, keep the mode-9 command coverage and symbol assertions aligned in `VoiceHintMapperTest`.
- If you change navigation/task/foreground-service lifecycle behavior, prefer updating the Robolectric JVM tests under `app/src/test/java/com/vibenavigator/` and the focused collaborator tests under `app/src/test/java/com/vibenavigator/nav/`.
- If you change navigation intent extras, update `NavigationRequest` first and keep notification resume/start flows serialized through it instead of hand-copying extras.
- If you change startup permission/settings/battery-optimization flow, keep `NavigationActivity` thin and update `NavigationStartupCoordinatorTest` plus any affected Robolectric lifecycle coverage.
- If you change manifest-declared components or permissions, verify the corresponding runtime checks in `NavigationPreflight` and `NavigationStartupCoordinator`.
- If you change logging, keep `buildLogPrefix`/`appendBlock` style sharing intact so formatting and file-rotation behavior stay consistent across entry types.
- If you change BRouter request parameters or response parsing, inspect both `brouter/` and `nav/route/` code paths together.
- If you change POI search behavior, keep direct coordinate entry working, keep history suggestions available when the field is focused and empty, and preserve shared search dispatch instead of reintroducing per-field executor ownership.
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

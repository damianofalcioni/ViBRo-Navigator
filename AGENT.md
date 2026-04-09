# VibeNavigator Agent Guide

Primary product requirements live in `SPECIFICATION.md` at the repository root. On the first implementation-oriented interaction in this repository, read `SPECIFICATION.md` before making changes. Use this guide together with that specification, and treat `SPECIFICATION.md` as the source of truth when implementation details or feature expectations are unclear.

## Project intent

VibeNavigator is a lightweight Android navigation app built around the installed BRouter app. The product goal is minimal, battery-efficient, offline-first navigation with vibration-led turn guidance and a dark visual theme.

Map-free trust is a primary product constraint. Assume the user may not see the map at all and may need to trust the next direction blindly. When guidance confidence is weak, conservative behavior is preferred: suppress, delay, or simplify instructions rather than surfacing a misleading maneuver.

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
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat assembleRelease`
- `.\gradlew.bat lint`

CI lives in `.github/workflows/build-apk.yml` and runs tests plus debug/release APK builds.

## Architecture

- `MainActivity` should stay thin and delegate profile selection, stop rows, incoming intents, and navigation input validation.
- `NavigationActivity` should stay focused on rendering, service binding, and task/back-button behavior. Startup checks belong in `NavigationStartupCoordinator`.
- The navigation screen's center visualization is a custom `NavigationCompassView` fed by lightweight navigation state. Keep route geometry preparation out of the view and keep Android drawing concerns out of the service/session logic.
- `NavigationService` is the Android lifecycle shell. Keep notification handling, location subscriptions, wake locks, route execution, listener broadcasting, and turn notification fan-out delegated to focused collaborators.
- `NavigationSession` is the session-level coordinator. Keep filtered location, route progress, blocked-road state, turn progression, and route-request lifecycle split across dedicated state/policy classes instead of collapsing them back together.
- `NavigationRequest` owns the shared navigation extras contract used by activities, the service, and resume notifications.
- `NavigationTextFormatter` owns shared user-visible navigation/notification formatting.
- `brouter/` owns BRouter service integration, routing params, profile discovery, and GeoJSON route requests. Preserve the current `timode=9` voice-hint contract unless there is a deliberate product change.
- `poi/`, `poi/search/`, and `poi/ui/` own POI parsing, history, provider-backed search, and shared suggestion UI. Keep search execution shared across inputs.
- `nav/route/`, `nav/directions/`, and `nav/kalman/` hold route parsing/matching, voice-hint mapping, and location smoothing.
- `util/AppLogger` is the shared file logger. Single-line and multiline writes should continue to use the same formatting and append path.

## Test strategy

- Prefer JVM tests. Use Robolectric when Android lifecycle coverage is needed.
- Do not add emulator/device requirements to the core automated suite unless explicitly requested.
- Keep lifecycle rules, heuristics, planners, and policy thresholds in small helpers when practical so they stay directly unit-testable.
- Keep focused coverage around navigation startup/preflight, request serialization, reroute heuristics, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route callback handoff, turn-event dispatch, and state broadcasting.
- Refactors that only move unchanged wiring into thin helpers do not need new tests by default. Behavior changes do.

## Project rules

- Keep the implementation lightweight. Prefer platform APIs and small local helpers over new libraries.
- Keep code in Java.
- Preserve the dark/black theme and support both portrait and landscape layouts.
- Preserve the map-free compass visualization on the navigation screen. Do not replace it with a map dependency or a full cartographic surface unless explicitly requested.
- Do not hardcode user-facing text in Java or XML layouts. Add or update strings in `app/src/main/res/values/strings.xml`.
- Keep repository documentation aligned with the code when relevant changes are made. Update `SPECIFICATION.md` when product behavior, requirements, or user-visible flows change. Update `AGENT.md` when architecture, guardrails, workflows, or coding expectations change. Do not make doc-only churn for code changes that do not affect those areas.
- Keep README/about content aligned at the product-description level. `README.md` and `about_body` in `strings.xml` should stay consistent about the app's purpose and core behavior, but they do not need to be literal copies of each other.
- Keep the app usable without Google APIs. Google search is optional and gated by `GOOGLE_MAPS_API_KEY`.
- Preserve support for shared coordinates/addresses and `geo:` deep links.
- Preserve background and screen-off navigation behavior. Changes affecting the foreground service, wake lock, notifications, or battery optimization flow need extra scrutiny.
- Preserve startup seed quality. Cached last-known location may only accelerate navigation startup when it is fresh and accurate enough; stale or low-quality cached fixes must not trigger the initial route calculation.
- Preserve the bucketed navigation polling cadence. Small ETA fluctuations should not continuously change the requested location interval, and identical interval/provider requests should reuse the active listener registration instead of forcing a remove-and-readd cycle.
- Preserve conservative guidance under uncertainty. Bearing-only reroutes, sub-accuracy maneuver prompts, and other low-confidence direction changes must not be surfaced as trustworthy instructions.
- Keep sensor-based orientation hints separated from reroute trust. Geomagnetic heading may support advisory stationary notifications when confidence is high enough, but wrong-direction reroutes must remain movement-derived.
- Keep stationary-orientation prompts conservative. Do not treat a brief low-speed GPS sample as a stop when recent filtered displacement still shows walking movement, and suppress those prompts while route recalculation is in progress.
- Keep live compass rotation display separated from reroute trust. The navigation UI may rotate from geomagnetic heading for user feedback, but route-decision logic must continue to rely on the existing confidence gates.
- Treat OEM settings quirks as part of the product surface. For battery-optimization redirects, prefer a settings destination that reliably stays open on real devices over a nominally more direct intent that immediately closes.
- Preserve BRouter compatibility. Route requests should continue using the local BRouter service and GeoJSON output.
- Preserve the current BRouter voice-hint contract unless there is a deliberate product change: mode `9`, distinct `beeline`, `exit left`, and `exit right`, and a neutral unknown fallback.
- Keep profile handling compatible with both bundled BRouter profiles and user-selected external `profiles2` folders.
- Keep notification behavior tied to turn timing and left/right vibration patterns.
- Prefer extending the existing logging with `AppLogger` when touching startup, permissions, routing, background execution, or network search behavior.
- Keep `AppLogger` on one shared file-append path. If logging behavior changes, do not reintroduce separate write flows for single-line versus multiline entries.

## Editing guidance

- Add or update tests when you change navigation state, rerouting, route parsing, voice-hint mapping, geometry helpers, or user-visible behavior.
- Keep background route computation separated from main-thread state mutation. Preserve `NavigationRouteExecutor` rather than inlining thread management back into `NavigationService`.
- Keep Android service concerns delegated through the existing foreground/location/wakelock/dispatch/broadcast helpers, and keep `NavigationSession` as a coordinator over focused session collaborators.
- If you change reroute thresholds, polling cadence, turn-alert timing, blocked-road escalation, or route-request lifecycle behavior, update the corresponding `nav/` tests.
- If you change guidance confidence rules, keep map-free use in mind and update tests around bearing trust, turn suppression, and duplicate/imminent alert behavior.
- If you change navigation intent extras, update `NavigationRequest` first and keep resume/start flows serialized through it instead of hand-copying extras.
- If you change startup permission/settings/battery-optimization flow, keep `NavigationActivity` thin and update the startup/lifecycle tests.
- If you change BRouter request parameters, response parsing, or voice-hint mapping, inspect both `brouter/` and `nav/route/` paths together and keep mode-9 coverage aligned.
- If you change the navigation compass behavior or visuals, update both `SPECIFICATION.md` and the custom-view/state plumbing together so rendering expectations, live heading inputs, and route geometry assumptions stay aligned.
- If you change POI search or incoming intent handling, preserve coordinate entry, empty-field history suggestions, shared search dispatch, and history behavior for externally opened locations.
- If you change logging, keep the shared `buildLogPrefix`/`appendBlock` style intact so formatting and file-rotation behavior stay consistent.
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
- Would the resulting turn guidance still be safe to trust without looking at a map?

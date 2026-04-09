# VibeNavigator Specification

## Source

This specification is derived from the original project-generation prompt and captures the intended product requirements for VibeNavigator.

## Product summary

VibeNavigator is a lightweight Android navigation app based on BRouter.

Core product constraints:

- Use Java
- Avoid dependencies as much as possible, including Google Play Services
- Keep generated and maintained code minimal while still implementing the features
- Target the latest practical Android SDK while keeping `minSdk 21`
- Use a black theme
- Do not hardcode user-facing text in code
- Support both portrait and landscape orientations
- Check and request all required permissions before starting navigation, when they are needed
- When navigation startup depends on system settings, route the user to a reachable settings screen that stays open on supported OEM builds, even if the device requires a generic settings page instead of a per-app approval dialog
- Provide a README describing VibeNavigator as a lightweight, battery-efficient, offline vibe-coded GPS navigation app that only vibrates directions
- Provide a distinctive app logo suitable for use as the app icon
- Treat map-free use as a primary product mode: navigation guidance must be trustworthy enough that a user who does not see the map can rely on the next direction without visual confirmation
- When the current position or heading confidence is too weak, prefer delaying or suppressing a direction update over presenting a misleading one

## Functional specification

### 1. Main UI

The app must show a main UI implemented as an Android `Activity`.

The main UI must include a routing-profile selector at the top.

#### 1.1 Routing profiles

- The selector items must be the BRouter profile names
- Profiles are the filenames with `.brf` extension available in the BRouter `profiles2` folder
- The selector is profile-based, not a separate vehicle-type toggle
- Example legacy path:
  `/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`

### 2. Destination input

Below the routing-profile selector, the app must show an input field for searching a destination POI or coordinates.

#### 2.1 History dropdown before typing

- Before the user starts typing, a dropdown must appear below the input field
- The dropdown must show previously searched POIs
- History entries must be promoted when a destination or stop is selected or otherwise resolved to valid coordinates for navigation
- Each history row must include an `X` control on the right to delete that POI from history

#### 2.2 Search after 3+ characters

- On every typed character, but only once the query length is greater than 3, the app must retrieve matching POIs
- Each result must include the POI full name and coordinates
- The data source must be Google Maps REST APIs when a Google API key is defined
- If the Google API key is not defined, the app must use OpenStreetMap APIs

#### 2.3 Search results dropdown

- Search results must be shown in a dropdown below the input field
- The user must be able to select a result from the dropdown
- Selecting a result must bind the destination to the coordinates of that POI

### 3. Intermediate stops

Below the destination input, the app must show a centered plus button.

#### 3.1 Add stop field

- Pressing the plus button must add a new input field for an intermediate POI
- Each intermediate input must have the same behavior and capabilities as the destination field

#### 3.2 Remove stop field

- Each added stop row must include an `X` button on the right
- Pressing that button must remove both the stop input field and the button itself

### 4. Start navigation

At the bottom center of the main UI, the app must show a start navigation button.

Pressing the button must:

- Open a new navigation UI implemented as an Android `Activity`
- Access the current user location
- Use the installed BRouter app intent/service integration to calculate a path from the current location to the destination
- Include any intermediate stops in the route calculation
- A cached last-known location may only be used to accelerate startup when it is recent and accurate enough to represent the current user location; otherwise the first route calculation must wait for a one-shot current fix or a live location update

#### 4.3 BRouter integration

The implementation must use BRouter integration compatible with these references:

- `IBRouterService.aidl`
  - [https://raw.githubusercontent.com/osmandapp/OsmAnd/refs/heads/master/OsmAnd/src/btools/routingapp/IBRouterService.aidl](https://raw.githubusercontent.com/osmandapp/OsmAnd/refs/heads/master/OsmAnd/src/btools/routingapp/IBRouterService.aidl)
- `BRouterServiceConnection.java`
  - [https://raw.githubusercontent.com/osmandapp/OsmAnd/refs/heads/master/OsmAnd/src/btools/routingapp/BRouterServiceConnection.java](https://raw.githubusercontent.com/osmandapp/OsmAnd/refs/heads/master/OsmAnd/src/btools/routingapp/BRouterServiceConnection.java)
- OsmAnd sample usage in `RouteProvider.java`
  - [https://raw.githubusercontent.com/osmandapp/OsmAnd/094097cc7411aef722b9183e24e828e6f749ca59/OsmAnd/src/net/osmand/plus/routing/RouteProvider.java](https://raw.githubusercontent.com/osmandapp/OsmAnd/094097cc7411aef722b9183e24e828e6f749ca59/OsmAnd/src/net/osmand/plus/routing/RouteProvider.java)

#### 4.3.1 Profile selection

- Route calculations must send the selected BRouter `profile` explicitly
- The app must not force a separate `v` vehicle-mode parameter when an explicit profile is supplied
- The selected `.brf` file is the source of routing behavior for walk, bike, or car use cases

#### 4.3.4 GeoJSON output

- The app must request BRouter GeoJSON output using the Android-service parameters that produce a GeoJSON `FeatureCollection`
- The app must request BRouter native turn-instruction mode `9` so GeoJSON `voicehints` preserve distinct exit-left, exit-right, and beeline commands

#### 4.4 Navigation update loop

The app must monitor user position:

- Every 1 second while startup route lock is still stabilizing, for at most the first 60 seconds after navigation starts
- Startup fast polling may end earlier once the app has gathered 5 consecutive accurate on-route updates after a route is active
- An accurate warmup update means an on-route evaluation with location accuracy of 25 meters or better
- Later at a dynamic interval derived from the estimated time to the next direction, using the current speed and remaining route distance to that direction
- When the next direction is estimated to be 8 seconds away or less, the dynamic interval must be 1 second
- Otherwise the dynamic interval should scale to roughly one quarter of the estimated time remaining to the next direction
- The post-warmup dynamic interval must be snapped to a small fixed bucket set instead of continuously varying on every update
- The bucket set must currently be `1s`, `2s`, `3s`, `5s`, `8s`, `12s`, `20s`, `30s`, and `60s`
- The dynamic interval must never be lower than 1 second
- The dynamic interval must never exceed 60 seconds
- Re-requesting location updates must reuse the active listener registration when the requested interval bucket and enabled provider set are unchanged, so the app does not continuously tear down and rebuild subscriptions
- Position handling must use a Kalman filter
- Any asynchronous route calculation must apply its resulting shared navigation state in a single serialized path so stale background results cannot overwrite newer navigation state

#### 4.4.1 Off-track reroute

- The route must be recalculated whenever the user position differs by 10 meters plus the GPS error distance from the current track

#### 4.4.2 Wrong-direction reroute

- The route must also be recalculated when the user is still on the track but is moving in the wrong direction
- Wrong direction is defined as bearing difference greater than 60 degrees
- Bearing-based wrong-direction detection must only be trusted when the current fix is accurate enough and movement-derived heading is credible for the current speed/displacement
- Low-confidence bearing estimates must not trigger reroutes on their own

#### 4.4.3 Direction distance estimation

- The app must estimate the distance left to the next direction
- The live remaining distance to the next direction should be derived from the user's current matched position along the active route geometry
- Current speed should be used to estimate the time left to that direction
- BRouter voice-hint distance metadata may be parsed and retained, but it must not be treated as the primary source of the user's live remaining distance to the next direction
- The app must treat very short maneuver distances as unreliable whenever they fall inside the current location uncertainty radius
- A next-direction distance that is less than or equal to the current trusted uncertainty radius must not be presented as a trustworthy instruction

#### 4.4.4 Turn notifications

- The app must send notifications:
  - When navigation starts and the first route has been calculated, for the first upcoming direction even if the user is not moving yet
- When the user has remained stationary for several seconds during navigation, recent filtered fixes show only negligible displacement, and the app has a sufficiently trustworthy geomagnetic heading sample that shows the user is not already facing the route
  - When the previous direction has just been passed, only if advancing guidance requires surfacing a new actionable upcoming instruction rather than replaying the just-passed maneuver
  - When 10 seconds remain to the next direction
  - When 5 seconds remain to the next direction
- The app must suppress or delay turn notifications whose remaining distance is not trustworthy relative to current location accuracy
- The app must not emit a passed-turn notification whose displayed remaining distance or time would collapse to zero; in that case it should suppress the passed maneuver and move on to the next actionable instruction
- When the user is already inside the most urgent threshold, the app should emit only the single most urgent imminent-turn notification instead of stacking multiple near-identical alerts
- Turn notifications must reuse a single notification entry in the notification list so older direction notifications do not pile up
- Replacing a direction notification in the notification list must still be compatible with smart bands or similar devices that mirror notifications as they arrive
- A stationary orientation notification must be emitted only after a short stationary dwell, must require both low recent movement speed and negligible recent filtered displacement, must require a fresh geomagnetic heading sample with good coarse calibration, and when the sensor exposes a per-sample heading accuracy estimate it must suppress the notification unless the required turn still clearly exceeds that uncertainty margin
- Geomagnetic stationary-orientation monitoring must remain available during background and screen-off navigation so those advisory notifications still work without the navigation UI being open
- Stationary orientation notifications are advisory turn-to-face-the-route prompts and must not change wrong-direction reroute behavior, which remains gated by movement-derived heading confidence
- Stationary orientation notifications must be suppressed while a route recalculation is in progress so the app does not emit contradictory off-route and turn-yourself prompts at the same time
- Each notification message must contain:
  - A direction arrow emoji
  - The distance left
  - The time left
  - The direction text
  - The exit number for roundabouts when applicable
  - Hyphen (`-`) separators between fields instead of the bullet character

#### 4.4.4.1 Imminent turn vibration patterns

- The notification imminent to the next direction must use different vibration patterns for left and right directions

#### 4.4.4.2 Voice hints

- BRouter directions are returned in the GeoJSON property `voicehints`
- Voice-hint interpretation must follow:
  - `FormatJson.java`
    - [https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/FormatJson.java](https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/FormatJson.java)
  - `VoiceHint.java`
    - [https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/VoiceHint.java](https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/VoiceHint.java)
- The app must interpret the current BRouter mode-9 GeoJSON command set, including distinct mappings for:
  - `16` beeline
  - `17` exit left
  - `18` exit right
- Unknown or unsupported voice-hint commands must fall back to a neutral unknown-direction presentation instead of pretending to be a normal continue instruction

### 4.5 Navigation UI

The navigation UI must show the following in large text:

#### 4.5.1 Next two directions

- At the top: the next two directions
- Each must include emoji, text, distance left, and time left
- The navigation UI must only surface directions whose distance is outside the current minimum trusted maneuver radius; unreliable micro-maneuvers should be skipped in favor of the next trustworthy instruction
- In ambiguous low-confidence conditions, temporary absence of a next-turn line is preferable to presenting a wrong or misleading turn

#### 4.5.2 Compass route view

- In the center: a map-free compass canvas showing the active route relative to the current position
- The compass must not render a map background
- The route must rotate live with the latest trusted display heading so forward stays at the top of the view
- Live geomagnetic compass rotation is only required while the navigation UI is visible and the screen is interactive
- The compass outer ring must carry the rotating cardinal labels `N`, `O`, `S`, and `W`
- The inner circles must remain stable visual distance references for the route
- The current-position marker should be shown as a small center dot
- A transparent orange filled circle centered on the current-position dot must visualize the current GPS accuracy radius at the compass scale, using the same orange as the accent ticks on the outer compass ring
- A semi-transparent fixed vertical guide line must run from the center dot to the top border of the compass and end with an open arrowhead whose tip aligns with the guide line
- When the displayed heading comes from a geomagnetic sample that exposes a heading-accuracy estimate, the compass must show two semi-transparent white straight guide lines, using the same visual treatment as the fixed top heading guide, from the center to the outer distance ring at the negative and positive angular error bounds around the fixed top heading guide
- Each distance ring should show a semi-transparent distance label on the right side and a matching travel-time label on the left side
- When heading accuracy is zero or unavailable, those labels should stay aligned with the short vertical-guide tick at the top ring intersection
- When heading accuracy is non-zero, the top tick should be replaced by a semi-transparent arc spanning between the left and right heading-accuracy guides, and the right distance label plus left travel-time label should align with those guide intersections
- Small semi-transparent white point markers must be shown on the route at the visible start position, at each visible hint position, and at the final destination
- The route must be rendered as a continuous line, not as discrete dots
- The destination endpoint must be shown with a finish-line icon without an enclosing badge

#### 4.5.3 Remaining stop progress

- Below the compass, the UI must first show the final destination progress as a dedicated single line using a destination icon instead of the literal `Destination` label
- Below the compass, the UI must show the distance left, time left, and arrival time for every intermediate stop that is still ahead on the route
- The destination progress line sits above the remaining intermediate-stop progress block

#### 4.5.4 Blocked road button

- Below the progress section and centered: a `blocked road` button

##### 4.5.4.1 Blocked no-go memory

- Pressing the button must add route-based no-go points derived from the upcoming matched route geometry, not from the raw GPS position
- The first press in an area must create a single no-go point slightly ahead on the route
- The first blocked area must use a small street-scale radius of about 10 to 12 meters
- This internal no-go list must be reset when a new navigation is started

##### 4.5.4.2 Blocked reroute

- After blocking the upcoming route area, the app must recalculate the route
- The recalculation must pass the no-go point list, including per-point radii, to BRouter

##### 4.5.4.3 Repeated blocked-road escalation

- Repeated presses in the same nearby area, or repeated presses within a short time window in a nearby area, must escalate the blocked region
- Escalation must increase both:
  - the number of forward route points used as no-go points
  - the no-go radius applied to those points
- The blocked-road behavior should be tuned primarily for walking and cycling, with cars treated as a secondary use case

#### 4.5.5 Stop navigation button

- At the bottom: a button to stop navigation
- Pressing it must return to the previous UI
- Destination and intermediate stops must be kept

#### 4.5.6 Back button behavior during navigation

- Pressing the system back button while the navigation UI is open must move the whole app task to the background
- Pressing back during navigation must not reveal the main UI underneath the navigation UI
- Navigation must continue running after this backgrounding action as long as the foreground service remains active

### 4.6 Background behavior

- Navigation functionality must remain active in the background
- Navigation functionality must remain active when the screen is off
- Screen-off or background navigation may suspend compass UI updates, but geomagnetic monitoring needed for stationary-orientation notifications must continue

#### 4.6.1 Foreground service lifecycle

- Active navigation must run through a foreground service with an ongoing notification
- If the app task is removed from recents, navigation must stop and the foreground service must be terminated
- If the foreground notification is removed while navigation is still running, reopening the app from recents must restore the foreground notification immediately when the navigation UI reconnects to the running service
- The app should treat removal of its own ongoing navigation notification as a stop signal when the Android device delivers that removal event to the app
- Navigation request extras used by the main screen, navigation screen, foreground service, and resume notification should be serialized through one shared contract so those entry points stay behaviorally identical

### 5. About button and page

- A small button showing only the app logo must be displayed at the very top center of the main UI
- Pressing it must open an about page
- The about page must contain:
  - The app version
  - A concise in-app product summary aligned with the README's description of the app and its core behavior

#### 5.1 Hidden developer mode

- While on the about page, five fast taps anywhere on the page must enable a hidden developer mode
- Enabling developer mode must show a popup confirming that developer mode is now enabled
- The app must start with developer mode disabled on every app launch
- Developer mode must apply only after it is enabled from the about page during the current app run
- The app must write its log file only when developer mode is enabled
- When developer mode is enabled for the first time, the app must create a new log file named `vibe-navigator-log-yyyymmddhhmm.txt` using the current local date and time
- When developer mode is enabled again after a later app restart, the app must start a fresh log session by recreating that run's target log file before writing new entries
- Repeating the five-tap developer-mode gesture while developer mode is already enabled must not restart logging and must instead show a popup that developer mode is already enabled
- When developer mode is enabled, the app must log the full decoded BRouter response payload in addition to the existing route summaries
- The logging implementation should keep a single shared path for log-entry formatting and file appends so single-line and multiline records cannot silently diverge in behavior
- When developer mode is enabled, the about page must additionally show a developer-only diagnostics block below the normal about text
- That diagnostics block must currently list the app's used live inputs:
  - GPS provider
  - network provider
  - geomagnetic rotation vector
- The diagnostics block must refresh automatically every 1 second while the about page is visible
- Each listed item must show both its current status and its latest available value details
- Location-provider details should include the latest available fix data such as coordinates, accuracy, speed, bearing, and sample age when available
- Geomagnetic rotation-vector details should include the latest available heading/orientation-derived values and sample age when available

### 6. Shared/opened coordinates and addresses

- The app must support opening or sharing map coordinates or addresses into the app
- Shared/opened coordinates or addresses must be set as the destination
- Incoming locations that resolve to valid coordinates must be saved into the same destination history list used by manual POI selection
- The app must register as a target for at least these incoming Android formats:
  - `geo:` map intents
  - `google.navigation:` intents
  - shared `text/plain` payloads
  - common Google Maps and OpenStreetMap `http(s)` links that contain a destination or coordinates
- Incoming coordinate or address intents must open the app without crashing on any supported Android version
- Parsing of incoming locations must be compatible with the app's minimum supported Android API level
- Invalid or malformed incoming coordinate payloads must fail gracefully instead of crashing or silently redirecting to placeholder coordinates
- On devices where multiple apps can handle the same map/share intent, the system chooser may appear before the user selects VibeNavigator

## Non-functional expectations

- Battery-conscious background navigation
- Minimal UI and minimal code footprint
- Offline-first routing through BRouter
- Translation-friendly text resource usage
- Orientation-safe layouts
- Robust permission handling before navigation starts
- Robust OEM-compatible redirects for required system settings such as battery-optimization exemptions
- Compatibility with all supported Android versions for intent parsing and deep-link handling
- Robustness for map-free guidance: the product should favor conservative, high-confidence navigation prompts over aggressive but error-prone updates

## Implementation guidance

- Keep `MainActivity` and `NavigationActivity` thin. Input validation, incoming-intent handling, startup/preflight checks, and navigation startup orchestration should stay in dedicated helpers.
- Keep navigation text formatting shared between on-screen state and notifications.
- Keep `NavigationService` focused on Android lifecycle and orchestration, with notification handling, location subscriptions, wake locks, route execution, listener broadcasting, and turn-event fan-out isolated in focused collaborators.
- Keep background route computation asynchronous while all shared navigation-state mutation remains serialized on the main thread.
- Keep `NavigationSession` split across focused collaborators for filtered location, route progress, blocked-road state, turn progression, and route-request lifecycle handling rather than collapsing that logic into one class.
- Keep heuristics such as reroute thresholds, polling cadence, and turn-alert timing in small policy/planner helpers, and keep POI search execution shared across destination and stop fields.
- Keep the navigation-intent extras contract owned by `NavigationRequest` so activities, the foreground service, and resume notifications serialize the same request shape.

## Testing expectations

- Prefer JVM regression coverage, with Robolectric for Android lifecycle behavior where practical.
- The core automated suite should not require an emulator or real device, though some foreground-service and OEM notification behaviors may still need manual verification.
- Keep lifecycle decisions, heuristics, planners, and policy thresholds in small helpers when practical so they remain directly unit-testable.
- Maintain coverage for navigation-request serialization, startup/preflight flow, reroute heuristics, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route-execution callback handoff, turn-event dispatch, and safe listener broadcasting.
- Voice-hint mapping coverage should verify the current BRouter mode-9 command set, including user-visible direction symbols.
- Refactors that only move unchanged wiring into helpers do not require new tests by default. Behavior changes in helper-owned flows should add or update focused JVM or Robolectric coverage.

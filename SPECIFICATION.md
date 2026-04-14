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
- The current repository baseline is `compileSdk 36` and `targetSdk 36` while keeping `minSdk 21`
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
- Profiles may come from bundled BRouter internal profiles or from user-accessible `.brf` files in an external `profiles2` folder
- The selector is profile-based, not a separate vehicle-type toggle
- Profile handling must remain compatible with both bundled BRouter profiles and user-selected external `profiles2` folders
- If BRouter is not installed, the app must not immediately open a profile-file or profile-folder picker during main-screen startup
- If BRouter is not installed, any profile-source prompt must be suppressed until BRouter is available
- If no external `profiles2` folder is selected or accessible, the app must still list and use bundled BRouter internal profiles for normal routing
- When trying to open a custom profile source, the app should probe multiple plausible BRouter `profiles2` locations and prefer one that actually exists instead of assuming a single path from Android version alone
- Common example paths:
  `/storage/emulated/0/Android/media/btools.routingapp/brouter/profiles2`
  `/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`

### 2. Destination input

Below the routing-profile selector, the app must show an input field for searching a destination POI or coordinates.

#### 2.1 History dropdown before typing

- Before the user starts typing, a dropdown must appear below the input field
- The dropdown must show previously searched POIs
- History entries must be promoted when a destination or stop is selected or otherwise resolved to valid coordinates for navigation
- Each history row must include an edit control on the right that lets the user rename the stored display label without changing the saved coordinates
- Each history row must include an `X` control on the right to delete that POI from history
- Renaming a history row must preserve that entry's coordinate identity so later selection still resolves to the same saved destination

#### 2.2 Search after 3+ characters

- Typed destination and stop queries must first check the saved history entries from the first typed character and preserve their recency order
- When one or more history entries match the typed query, the dropdown must show those history matches instead of online provider results
- On every typed character, but only once the query length is greater than 3, the app must retrieve matching POIs
- Online provider search must only run when the typed query has no matching history entries and the query length is greater than 3
- Each result must include the POI full name and coordinates
- The data source must be Google Maps REST APIs when a Google API key is defined
- If the Google API key is not defined, the app must use OpenStreetMap APIs

#### 2.3 Search results dropdown

- Search results must be shown in a dropdown below the input field
- The user must be able to select a result from the dropdown
- Selecting a result must bind the destination to the coordinates of that POI
- Selecting a stored history entry must be treated as a final selection: the dropdown should close and the app must not immediately reopen search suggestions unless the user edits the text again
- After a portrait/landscape layout change or other activity recreation, restoring a previously selected destination or stop must keep that resolved selection and must not reopen suggestions unless the user edits the restored text

#### 2.4 Destination map picker

- Next to the destination text field, the app must show a map-picker icon button instead of a text-labelled map button
- Pressing that button must open a separate picker `Activity`
- The picker must remain dependency-light and must not use an external native map library
- The picker must render OpenStreetMap raster tiles through the app's own implementation
- If the destination field already resolves to coordinates, the picker must open centered on that destination and must apply a predefined zoom level
- If the destination field does not yet resolve to coordinates, the picker must open centered on the current device location when available, and otherwise fall back gracefully
- The picker must let the user select a point directly from the map and return that point as the destination
- The picker must support icon-only controls for confirm, cancel, current location, zoom in, and zoom out
- The picker must not show extra top or bottom banners; controls should remain overlaid directly on the map
- Rotating the device while the picker is open must preserve the currently selected point and keep it visible on the map after recreation

### 3. Intermediate stops

Below the destination input, the app must show a centered plus button.

#### 3.1 Add stop field

- Pressing the plus button must add a new input field for an intermediate POI
- Each intermediate input must have the same behavior and capabilities as the destination field
- Each intermediate row must also include a map-picker icon button with the same map-selection capabilities as the destination field

#### 3.2 Remove stop field

- Each added stop row must include an `X` button on the right
- Pressing that button must remove both the stop input field and the button itself

#### 3.3 Map-picker interaction parity

- Opening the picker for an intermediate stop that already has coordinates must center the map on that stop and apply the predefined zoom level
- Opening the picker for an empty intermediate stop must center on the current device location when available
- Selecting the current location from inside the picker must also apply the current-location zoom level
- The picker must preserve the selected stop location across portrait/landscape recreation in the same way as the destination picker

#### 3.4 Map gestures and controls

- The picker must support one-finger drag panning
- The picker must support one-finger tap selection
- The picker must support two-finger pinch zoom in and out
- Completing a pinch gesture must not accidentally change the currently selected point because of finger release being misinterpreted as a tap

### 4. Start navigation

At the bottom center of the main UI, the app must show a start navigation button.

Pressing the button must:

- Open a new navigation UI implemented as an Android `Activity`
- Access the current user location
- Use the installed BRouter app intent/service integration to calculate a path from the current location to the destination
- Include any intermediate stops in the route calculation
- A cached last-known location may only be used to accelerate startup when it is recent and accurate enough to represent the current user location; otherwise the first route calculation must wait for a one-shot current fix or a live location update

#### 4.1 Missing BRouter handling

- If BRouter is not installed, the main screen must clearly tell the user that BRouter is required instead of behaving as if profile files are merely missing
- On first main-screen open without BRouter installed, the app should offer direct install options for the BRouter app page, including Play Store and F-Droid targets when those intents are available
- If no install target can be opened on the device, the app must fail gracefully with a short user-visible message rather than crashing
- When BRouter is not installed, pressing start navigation must stop before profile resolution and must show a missing-BRouter message instead of opening the custom-profile picker or the profiles-folder picker

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
- Bundled internal BRouter profiles must remain usable even when no external profile folder has been selected
- Custom external profile browsing should target a real accessible `profiles2` folder when one can be found, but normal routing must not depend on that folder existing

#### 4.3.4 GeoJSON output

- The app must request BRouter GeoJSON output using the Android-service parameters that produce a GeoJSON `FeatureCollection`
- The app must request BRouter native turn-instruction mode `9` so GeoJSON `voicehints` preserve distinct exit-left, exit-right, and beeline commands
- When BRouter includes per-track GeoJSON `times`, the app must parse and retain them as route timing metadata that can be reused for maneuver-time estimation when live speed is not yet trustworthy or not yet available

#### 4.4 Navigation update loop

The app must monitor user position:

- Every 1 second while startup route lock is still stabilizing, for at most the first 60 seconds after navigation starts
- Startup fast polling may end earlier once the app has gathered 5 consecutive accurate on-route updates after a route is active
- An accurate warmup update means an on-route evaluation with location accuracy of 25 meters or better
- Later at a dynamic interval derived from the estimated time to the next direction, using the current speed and remaining route distance when the next maneuver still lies on the current matched route segment and live speed is available, or route timing metadata when the next maneuver lies beyond the current matched route segment
- When the next direction is estimated to be 8 seconds away or less, the dynamic interval must be 1 second
- Otherwise the dynamic interval should scale to roughly one quarter of the estimated time remaining to the next direction
- The post-warmup dynamic interval must be snapped to a small fixed bucket set instead of continuously varying on every update
- The bucket set must currently be `1s`, `2s`, `3s`, `5s`, `8s`, `12s`, `20s`, `30s`, and `60s`
- The dynamic interval must never be lower than 1 second
- The dynamic interval must never exceed 60 seconds
- Re-requesting location updates must reuse the active listener registration when the requested interval bucket and enabled provider set are unchanged, so the app does not continuously tear down and rebuild subscriptions
- Position handling must use a Kalman filter
- Any asynchronous route calculation must apply its resulting shared navigation state in a single serialized path so stale background results cannot overwrite newer navigation state
- The navigation session must support an explicit paused mode that preserves the current request and loaded route while temporarily suspending live guidance processing
- While paused, the app must stop live location/orientation-driven navigation updates, suppress turn and reroute handling, and resume from the same session state when the user continues navigation

#### 4.4.1 Off-track reroute

- The route must be recalculated whenever the user position differs from the current track by more than an off-track threshold derived from recent location confidence
- Off-track distance must be measured against the nearest matched point on the active route geometry
- The off-track threshold should use a short-window smoothed location-accuracy estimate rather than a single raw fix accuracy so one bad GPS sample does not immediately widen the tolerance
- The off-track threshold must currently be `max(smoothedAccuracy + 8 meters, 10 meters)`
- Off-track reroutes should require confirmation across consecutive samples when the miss only slightly exceeds the threshold, to avoid single-fix GPS spikes causing unnecessary reroutes
- Clearly large misses beyond the threshold may reroute immediately without waiting for a second confirmation sample
- The size of that immediate-reroute margin may shrink at higher travel speeds so driving-style use reacts faster without making low-speed walking reroutes twitchy

#### 4.4.2 Wrong-direction reroute

- The route must also be recalculated when the user is still on the track but is moving in the wrong direction
- Wrong direction is defined as bearing difference greater than 60 degrees
- Bearing-based wrong-direction detection must only be trusted when the current fix is accurate enough and the heading source is credible for the current speed and displacement
- When GPS bearing accuracy is available, the app should prefer GPS bearing only when that reported bearing accuracy is good enough for walking and cycling use cases; low-speed walking use must remain supported and must not be excluded by a cycling-only speed gate
- When GPS bearing is not trustworthy enough, wrong-direction detection and moving compass-heading selection should fall back to a movement-derived course computed from recent filtered route progress rather than from a single noisy fix pair
- Low-confidence bearing estimates must not trigger reroutes on their own
- The expected route bearing for wrong-direction checks should be forward-looking, derived from a short lookahead along the matched route geometry rather than only from the single currently matched segment
- Bearing mismatch alone should not be enough to reroute while the user is still making clear forward progress along the route
- Wrong-direction reroutes should be confirmed across consecutive samples, and should be supported by direction-of-progress evidence such as backward or stalled along-route progress over time

#### 4.4.3 Direction distance estimation

- The app must estimate the distance left to the next direction
- The live remaining distance to the next direction should be derived from the user's current matched position along the active route geometry
- A trustworthy live speed estimate for ETA purposes should come from recent smoothed forward movement rather than from a single raw instantaneous speed sample
- That smoothed live speed should remain usable for slow walking and hiking speeds when recent progress shows genuine forward motion, and should not require a cycling-style speed threshold
- Smoothed live speed should be used to estimate maneuver time only for the still-untraveled portion of the user's current matched route segment, and only when that smoothed movement estimate is trustworthy
- When trustworthy live speed is not available for the remaining current-segment portion, the app should estimate that remaining current-segment time from BRouter route timing data when possible
- For any maneuver beyond the current matched route segment, the app should add BRouter-derived time for all following route segments between the end of the current segment and that maneuver
- When BRouter GeoJSON per-track timing metadata such as the `times` array is available, it should be the preferred source for those BRouter-derived segment times
- When per-track timing metadata is unavailable, the app may fall back to another BRouter-derived route time model for those segments rather than inventing a placeholder live-speed estimate for the whole remaining route
- When neither trustworthy live speed nor any BRouter-derived timing model can produce a maneuver ETA, the app must show `--`
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
- Stationary orientation notifications are advisory turn-to-face-the-route prompts and must not change wrong-direction reroute behavior, which remains gated by trusted movement heading, route-progress confirmation, and reroute confidence rules
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

#### 4.5.0 Portrait vs landscape arrangement

- The navigation UI may use different layouts in portrait and landscape as long as the same navigation information and actions remain available
- On phone-sized screens in landscape orientation, the navigation UI must switch to a two-column layout
- In that landscape layout, the left column must contain all navigation text content and both action buttons
- In that landscape layout, the right column must contain only the compass route view
- In that landscape layout, the left column must keep the turn-instruction block near the top and the blocked-road, stop, and pause/resume actions together in the bottom action row instead of placing them under the compass

#### 4.5.1 Next two directions

- Near the top, below the GPS status line: the next two directions
- Each must include emoji, text, distance left, and time left
- The first upcoming direction must show distance and time from the user's current matched position
- If the first upcoming direction still lies on the current matched route segment, its displayed time left should use trustworthy smoothed live speed when available and otherwise fall back to BRouter-derived timing for the remaining current-segment portion when available
- If the first upcoming direction lies beyond the current matched route segment, its displayed time left should combine the remaining current-segment time with BRouter-derived time for all following route segments up to that maneuver
- The second upcoming direction must show distance left and time left relative to the first upcoming direction rather than relative to the current position
- The second upcoming direction's relative time should be derived from BRouter timing between the first and second maneuver points when available
- The navigation UI must only surface directions whose distance is outside the current minimum trusted maneuver radius; unreliable micro-maneuvers should be skipped in favor of the next trustworthy instruction
- In ambiguous low-confidence conditions, temporary absence of a next-turn line is preferable to presenting a wrong or misleading turn
- The first upcoming direction must keep the full available instruction row width
- Both direction lines must stay on a single line and should reduce text size as needed before falling back to end-ellipsis truncation

#### 4.5.2 Compass route view

- In the center: a map-free compass canvas showing the active route relative to the current position
- The compass must not render a map background
- The route must rotate live with the latest trusted display heading so forward stays at the top of the view
- While the user is moving, the displayed compass heading should prefer the trusted GPS/course heading to reduce jitter, and should fall back to a movement-derived course when GPS/course heading is unavailable or too inaccurate
- The displayed compass heading must be compensated for the current screen rotation so portrait and landscape show the same real-world forward direction at the top of the view instead of drifting by 90 or 180 degrees
- When the user is stationary, or when neither trusted GPS/course heading nor movement-derived course is available, the displayed compass heading may fall back to the live geomagnetic heading
- Live geomagnetic compass rotation is only required while the navigation UI is visible and the screen is interactive
- The compass outer ring must carry the rotating cardinal labels `N`, `O`, `S`, and `W`
- The inner circles must remain stable visual distance references for the route
- When the user is stationary, the compass should zoom out to fit the full active route overview inside the compass
- When the user is moving and the current native speed reading is reliable, the compass should zoom to a forward-looking radius representing about 60 seconds of travel
- That moving 60-second radius must not be capped to a smaller fixed maximum such as 600 meters
- When the user is moving but the current native speed reading is not yet reliable, the compass should prefer reusing the last reliable moving zoom radius if one exists instead of jumping back and forth between zoom modes
- When the user starts or resumes movement without a reliable moving-speed reading and no previous reliable moving zoom radius exists yet, the compass should fall back to the full-route overview until a reliable moving-speed reading becomes available
- When the user stops and the compass expands back to the full-route overview, the last reliable moving zoom radius should be preserved so it can be restored when movement resumes before speed confidence has recovered
- The compass route geometry and hint-marker geometry should be sampled once per active route and reused across UI updates instead of rebuilding full projected route lists on every heading or location refresh
- Compass rendering should avoid per-frame transient object allocation in its hot drawing path for route, hint, and destination projection
- Transitions between stationary overview and moving zoom should be smoothed instead of snapping abruptly, except that restoring a previously saved reliable moving zoom radius after a stationary pause may return directly to that saved scale to avoid intermediate zoom thrash
- The transition from moving zoom to the stationary full-route overview must complete in a fixed duration of about 2 seconds regardless of the total route length or overview radius delta
- The current-position marker should be shown as a small center dot
- A transparent orange filled circle centered on the current-position dot must visualize the current GPS accuracy radius at the compass scale, using the same orange as the accent ticks on the outer compass ring
- A semi-transparent fixed vertical guide line must run from the center dot to the top border of the compass and end with an open arrowhead whose tip aligns with the guide line
- When the displayed heading source exposes a heading-accuracy estimate, the compass must show two semi-transparent white straight guide lines, using the same visual treatment as the fixed top heading guide, from the center to the outer distance ring at the negative and positive angular error bounds around the fixed top heading guide
- Each distance ring should show a semi-transparent distance label on the right side and a matching travel-time label on the left side
- In moving mode, the top visible distance ring is the primary horizon reference for those labels; inner-ring distance and time labels must scale from that top visible ring rather than from the hidden compass edge
- When heading accuracy is zero or unavailable, those labels should stay aligned with the short vertical-guide tick at the top ring intersection
- When heading accuracy is non-zero, the top tick should be replaced by a semi-transparent arc spanning between the left and right heading-accuracy guides, and the right distance label plus left travel-time label should align with those guide intersections
- Small semi-transparent white point markers must be shown on the route at the visible start position and at each visible hint position
- The route must be rendered as a continuous line, not as discrete dots
- The route ahead of the current matched position must keep the normal route red styling
- In moving mode, the red route-ahead stroke width must visualize the current off-track threshold derived from recent smoothed location accuracy, so the red line acts as the allowed route corridor rather than a purely decorative fixed-width line
- That moving-mode route corridor width should reflect the full threshold span around the route centerline, not only a one-sided offset
- In the stationary full-route overview, the red route line must keep a fixed visual stroke width instead of scaling to the off-track threshold
- The already passed part of the currently active route must be shown as the same red with about 50 percent transparency
- When a reroute is applied, the passed-route overlay must be rebuilt from the new active route geometry and must not retain passed geometry from the previous route
- The destination endpoint must be shown as a slightly larger opaque white point without a finish-line icon or enclosing badge
- The destination endpoint must only be shown when it falls within the currently visible compass radius; if it lies outside the visible radius, it should not be clamped back onto the compass edge as a detached marker

#### 4.5.3 Shared status block

- Below the compass, the UI must use a single shared status text block instead of separate destination-progress and secondary-detail text areas
- When no higher-priority notice is active, that shared status block must show the final destination progress and, if an intermediate stop is still ahead, the next intermediate-stop progress in the same text area
- The destination progress portion should use the same segment-aware hybrid estimator as maneuver timing: trustworthy smoothed live speed only for the remaining current-segment portion, plus BRouter-derived time for later segments
- The next intermediate-stop portion should use the same segment-aware hybrid estimator as maneuver timing and destination progress
- When the current next intermediate stop is passed, the shared status block must switch to the following intermediate stop if one remains
- The UI must not list all remaining intermediate stops at once in that shared status block
- When a navigation detail or notice needs to be surfaced in that area, such as route-unavailable detail, blocked-road reroute feedback, or paused-state messaging, that detail must take precedence over progress content in the shared status block
- When the user explicitly triggers the blocked-road action and a reroute request starts, the shared status block should surface a specific blocked-road reroute-progress notice such as `Blocked road added. Recalculating route.` instead of only showing the generic route-calculation body text

#### 4.5.3.1 GPS status line

- The navigation UI must show a single GPS status line formatted as `<speed> ↑<elev> <bearing> • <accuracy> <bearing-accuracy> • (<sat>) • <countdown>`
- That line must show the current speed, current elevation, current horizontal accuracy, GPS bearing, GPS bearing accuracy, and the current number of GNSS satellites used in the fix, in that exact order
- The current horizontal accuracy and GPS bearing-accuracy values in that line must be emphasized in orange when they are available
- That GPS status line must stay on a single line and should reduce its text size as needed instead of wrapping onto a second line
- When any of those values is unavailable, the UI must show `--` in that field instead of omitting it

#### 4.5.4 Blocked road button

- Near the top of the screen: an icon-only circular blocked-road button
- The blocked-road icon should use a simple no-go / forbidden-sign style glyph that remains legible at button size
- The blocked-road button must live in the same bottom action row as the stop and pause/resume actions
- The blocked-road action must be unavailable while navigation is paused so the app does not queue reroute changes against a suspended guidance session

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

#### 4.5.5 Pause/resume navigation button

- At the bottom action row, the navigation UI must show an icon-only circular button that toggles between pause and resume for the current navigation session
- Pressing pause must keep the current route, destination, and intermediate-stop progress in memory while suspending live guidance updates
- While paused, the navigation UI must clearly indicate that the session is paused and the button icon must switch to resume/play
- Pressing resume must continue the existing navigation session instead of starting a fresh route-planning flow
- Portrait and landscape layouts must both expose the pause/resume action alongside the blocked-road and stop-navigation actions
- In the bottom action row, the action order must be blocked-road, stop, then pause/resume from left to right, with matching spacing around the three circular buttons

#### 4.5.6 Stop navigation button

- At the bottom action row: an icon-only circular button to stop navigation
- In that bottom action row, the stop button must sit between the blocked-road button and the pause/resume button
- Pressing it must return to the previous UI
- Destination and intermediate stops must be kept

#### 4.5.7 Back button behavior during navigation

- Pressing the system back button while the navigation UI is open must move the whole app task to the background
- Pressing back during navigation must not reveal the main UI underneath the navigation UI
- Navigation must continue running after this backgrounding action as long as the foreground service remains active
- On Android versions that use predictive back, the navigation screen must keep the same backgrounding behavior through the platform back-dispatch path instead of relying only on legacy `onBackPressed()` callbacks

### 4.6 Background behavior

- Navigation functionality must remain active in the background
- Navigation functionality must remain active when the screen is off
- Background and screen-off navigation reliability must be provided primarily by the location foreground service and ongoing location callbacks rather than by holding a session-long CPU wake lock
- Partial wake locks may be used only as short, focused guards around critical work such as startup bootstrap, route calculation, or reroute calculation, and each acquisition must use an explicit timeout and be released by the same flow that acquired it
- The app must not rely on continuously renewing or indefinitely holding a partial wake lock for the full navigation session
- Screen-off or background navigation may suspend compass UI updates, but geomagnetic monitoring needed for stationary-orientation notifications must continue
- If the platform cannot deliver the required geomagnetic samples while the device is asleep without a session-long wake lock, stationary-orientation notifications may degrade to best-effort while core location tracking and route guidance continue

#### 4.6.1 Foreground service lifecycle

- Active navigation must run through a foreground service with an ongoing notification
- When navigation is paused but not stopped, the foreground service must remain alive and its ongoing notification must reflect that the session is paused
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
  - `http(s)` map links for:
    - `maps.google.com`
    - `google.com/maps`
    - `www.google.com/maps`
    - `openstreetmap.org`
    - `www.openstreetmap.org`
- The app must not register itself as a generic handler for arbitrary web URLs or for non-map `google.com` and `www.google.com` pages such as search results or article links
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
- Keep wake-lock ownership narrow and task-scoped. Long-lived navigation reliability should come from the foreground location service, while any partial wake lock should be acquired only by the collaborator performing the short critical section that needs it.
- Keep background route computation asynchronous while all shared navigation-state mutation remains serialized on the main thread.
- Keep `NavigationSession` split across focused collaborators for filtered location, route progress, blocked-road state, turn progression, and route-request lifecycle handling rather than collapsing that logic into one class.
- Keep heuristics such as reroute thresholds, bearing trust rules, forward-look route bearing, direction-of-progress checks, polling cadence, and turn-alert timing in small policy/planner helpers, and keep POI search execution shared across destination and stop fields.
- Keep the navigation-intent extras contract owned by `NavigationRequest` so activities, the foreground service, and resume notifications serialize the same request shape.
- Prefer extending the existing `AppLogger` coverage when touching startup, permissions, routing, background execution, or network search behavior.

## Testing expectations

- Prefer JVM regression coverage, with Robolectric for Android lifecycle behavior where practical.
- The core automated suite should not require an emulator or real device, though some foreground-service and OEM notification behaviors may still need manual verification.
- Keep lifecycle decisions, heuristics, planners, and policy thresholds in small helpers when practical so they remain directly unit-testable.
- Maintain coverage for navigation-request serialization, startup/preflight flow, reroute heuristics, bearing trust, route-progress confirmation, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route-execution callback handoff, turn-event dispatch, and safe listener broadcasting.
- Voice-hint mapping coverage should verify the current BRouter mode-9 command set, including user-visible direction symbols.
- Refactors that only move unchanged wiring into helpers do not require new tests by default. Behavior changes in helper-owned flows should add or update focused JVM or Robolectric coverage.

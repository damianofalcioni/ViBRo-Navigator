# ViBRo Navigator Specification

## Source

This specification is derived from the original project-generation prompt and captures the intended product requirements for ViBRo Navigator.

## Product summary

ViBRo Navigator is a lightweight Android navigation app based on BRouter.

Core product constraints:

- Use Java
- Avoid dependencies as much as possible. Google Play Services may be used only in the Google Play distribution flavor for explicitly requested Google features; the F-Droid flavor and common source set must remain free of Google Play Services dependencies.
- Keep generated and maintained code minimal while still implementing the features
- Target the latest practical Android SDK while keeping `minSdk 21`
- The current repository baseline is `compileSdk 36` and `targetSdk 36` while keeping `minSdk 21`
- Use a black theme
- Do not hardcode user-facing text in code
- Support both portrait and landscape orientations
- Check and request all required permissions before starting navigation, when they are needed
- When navigation startup depends on system settings, route the user to a reachable settings screen that stays open on supported OEM builds, even if the device requires a generic settings page instead of a per-app approval dialog
- If a startup settings dialog is dismissed after the required setting was changed elsewhere, navigation startup must re-check preflight and continue; if the blocker remains unresolved, startup must abort cleanly instead of leaving the navigation screen waiting
- Provide a README describing ViBRo Navigator as a lightweight, battery-efficient, offline vibe-coded GPS navigation app based on BRouter, that vibrates directions
- Provide a distinctive app logo suitable for use as the app icon
- Treat map-free use as a primary product mode: navigation guidance must be trustworthy enough that a user who does not see the map can rely on the next direction without visual confirmation
- When the current position or heading confidence is too weak, prefer delaying or suppressing a direction update over presenting a misleading one
- Android Auto support is Google Play flavor only. It must use the Android for Cars App Library template model, because Android Auto does not host the phone `Activity` layout directly.

## Functional specification

### 1. Main UI

The app must show a main UI implemented as an Android `Activity`.

The main UI must include a routing-profile selector at the top.

#### 1.1 Routing profiles

- The selector items must be the BRouter profile names
- Profiles may come from bundled BRouter internal profiles or from user-accessible `.brf` files in autodiscovered external `profiles2` folders
- The selector is profile-based, not a separate vehicle-type toggle
- Profile handling must remain compatible with both bundled BRouter profiles and autodiscovered external `profiles2` folders
- If BRouter is not installed, the app must not immediately open a profile-file picker during main-screen startup
- If no autodiscovered external `profiles2` folder is accessible, the app must still list and use bundled BRouter internal profiles for normal routing
- The routing-profile selector must use the same external `profiles2` discovery logic as the custom-profile picker
- When one or more external `profiles2` folders are discoverable, the selector should list those external `.brf` profiles alongside bundled BRouter profiles
- When no external `profiles2` folder is discoverable, the selector must fall back to bundled BRouter profiles if bundled profiles are available
- When neither discoverable external `profiles2` folders nor bundled BRouter profiles are available, the selector must still show a single custom-profile entry so the user can pick a `.brf` file manually
- The selector must continue to behave like a normal dropdown even when a custom profile is currently selected
- The selector must include a single custom-profile entry; when the user chooses that custom entry from the opened dropdown, the app must open the custom `.brf` picker even if that same custom entry is already the current selection
- The first time the user chooses the custom-profile entry and no persisted directory access exists yet, the app must first request Storage Access Framework directory access to the BRouter `profiles2` folder, then continue to the `.brf` file picker
- After the app has a persisted readable `profiles2` tree grant, subsequent custom-profile selections should continue directly to the `.brf` file picker while reusing that granted tree for external-profile discovery and picker startup
- The one-time directory-access step is additive only: it must not change normal spinner behavior, selected-profile persistence, or the way routing uses the chosen BRouter profile name
- When trying to open a custom profile source, the app should probe multiple plausible BRouter `profiles2` locations across both internal and removable storage, and across both `Android/media/...` and legacy `Android/data/...` layouts, instead of assuming a single path from Android version alone
- On Android 11 and later, the app should prefer `Android/media/.../profiles2` for user-granted directory access because SAF tree access to `Android/data/...` is restricted, while legacy `Android/data/...` probing may remain as a non-granted fallback hint only
- If no candidate path can be verified, the picker-startup fallback should prefer the primary internal `Android/media/.../profiles2` location before removable-storage candidates
- Common example paths:
  `/storage/emulated/0/Android/media/btools.routingapp/brouter/profiles2`
  `/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`
  `/storage/<sdcard-uuid>/Android/media/btools.routingapp/brouter/profiles2`
  `/storage/<sdcard-uuid>/Android/data/btools.routingapp/files/brouter/profiles2`

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
- In the Google Play flavor, the data source must be Google Maps REST APIs when a Google API key is saved in the app settings
- If the Google API key is not defined, the app must use OpenStreetMap APIs
- In the F-Droid flavor, POI search must always use OpenStreetMap APIs and must not include Google search code or require a Google API key

#### 2.3 Search results dropdown

- Search results must be shown in a dropdown below the input field
- The user must be able to select a result from the dropdown
- Selecting a result must bind the destination to the coordinates of that POI
- Selecting a stored history entry must be treated as a final selection: the dropdown should close and the app must not immediately reopen search suggestions unless the user edits the text again
- Selecting a destination or intermediate stop must clear focus from POI text inputs and hide the soft keyboard, including when Android tries to restore focus to another POI input after the selection popup closes
- After a portrait/landscape layout change or other activity recreation, restoring a previously selected destination or stop must keep that resolved selection and must not reopen suggestions unless the user edits the restored text

#### 2.4 Destination map picker

- Next to the destination text field, the app must show a map-picker icon button instead of a text-labelled map button
- Pressing that button must open a separate picker `Activity`
- The picker must remain dependency-light and must not use an external native map library
- The picker must render OpenStreetMap raster tiles through the app's own implementation
- The picker must show a lower-right attribution overlay reading `Map data from OpenStreetMap`
- In that overlay, `OpenStreetMap` must link to `https://www.openstreetmap.org/copyright`
- If the destination field already resolves to coordinates, the picker must open centered on that destination and must apply a predefined zoom level
- If the destination field does not yet resolve to coordinates, the picker must open centered on the current device location when available, and otherwise fall back gracefully
- The picker must let the user select a point directly from the map and return that point as the destination
- The picker must support icon-only controls for confirm, cancel, current location, zoom in, and zoom out
- The picker must support an icon-only POI category control overlaid on the map. Opening the control must show POI categories dynamically discovered from OpenStreetMap/Overpass tags in the current map view, sorted alphabetically, with each row showing the number of discovered items such as `Fuel (15)`. Category rows must be text-only, support a single active category, highlight the active category, and toggle that category off when tapped again. Category discovery should be initiated by opening the POI control rather than by initial map load. Returned POIs must be drawn with one shared POI pin style, and POI names must appear automatically when the map is zoomed in enough.
- When the POI category filter setting is enabled, opening the POI category control must show only the configured and enabled category names and must query Overpass only for selectors derived from those configured names instead of running broad category discovery.
- Map-picker POI requests should be minimized: the category-discovery Overpass response should seed the visible POI marker cache, selected-category rendering should reuse cached markers immediately, and later map movement should query only viewport areas not already covered by cached data for the selected category.
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

At the bottom center of the main UI, the app must show a large icon-only circular start navigation button using the same green play symbol as the navigation resume/play button.

Pressing the button must:

- Open a new navigation UI implemented as an Android `Activity`
- Access the current user location
- Use the installed BRouter app intent/service integration to calculate a path from the current location to the destination
- Include any intermediate stops in the route calculation
- A cached last-known location may only be used to accelerate startup when it is recent and accurate enough to represent the current user location; otherwise the first route calculation must wait for a one-shot current fix or a live location update
- The first BRouter route calculation must only use a startup location fix that is recent and has location accuracy of 25 meters or better, whether that fix came from a cached seed, one-shot current-location request, or live location update
- When a fresher startup fix arrives while the first no-active-route calculation is still running, the app should only queue a replacement BRouter request if the new fix materially changes the route start or meaningfully improves start accuracy; small startup jitter around the cached seed should not force a duplicate route calculation

#### 4.1 Missing BRouter handling

- If BRouter is not installed, the main screen must clearly tell the user that BRouter is required instead of behaving as if profile files are merely missing
- On first main-screen open without BRouter installed, the app should offer direct install options for the BRouter app page, including Play Store and F-Droid targets when those intents are available
- If no install target can be opened on the device, the app must fail gracefully with a short user-visible message rather than crashing
- When BRouter is not installed, pressing start navigation must stop before profile resolution and must show a missing-BRouter message instead of opening the custom-profile picker

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
- Bundled internal BRouter profiles must remain usable even when no autodiscovered external profile folder is accessible
- Custom external profile browsing should target a real accessible `profiles2` folder when one can be found, but normal routing must not depend on that folder existing
- The picker-initial-location logic for custom external profiles must use the same version-agnostic multi-path probing strategy as the profile-discovery logic, rather than switching candidate path sets solely by Android version
- External-profile discovery for selector population and picker startup must share the same version-agnostic candidate set and the same internal-versus-removable storage coverage
- A persisted SAF tree grant for `profiles2` must be treated as the highest-priority source for external-profile discovery and picker initial location, ahead of unguided path probing
- A single-file SAF grant obtained from picking one `.brf` file must not be assumed to provide sibling-folder enumeration rights; folder enumeration must rely on a tree grant or on separately accessible autodiscovered paths

#### 4.3.4 GeoJSON output

- The app must request BRouter GeoJSON output using the Android-service parameters that produce a GeoJSON `FeatureCollection`
- The app must request BRouter native turn-instruction mode `9` so GeoJSON `voicehints` preserve distinct exit-left, exit-right, and beeline commands
- When BRouter includes per-track GeoJSON `times`, the app must parse and retain them as route timing metadata that can be reused for maneuver-time estimation when live speed is not yet trustworthy or not yet available
- When BRouter includes GeoJSON `messages` rows with a `maxspeed` value in `WayTags`, the app should parse those rows as route speed-limit sections and display the current section's speed limit during active navigation
- When BRouter snaps the requested start to a routable network point outside the current off-track threshold, the app should keep BRouter's original route geometry for route matching and treat the snapped route start as a beeline approach target using command `16`; while that approach target is active, off-track rerouting must remain suppressed so the user may reach the original route corridor by any path, and normal route-following guidance should begin only after the user is inside the original route threshold

#### 4.4 Navigation update loop

The app must monitor user position:

- Every 1 second while startup route lock is still stabilizing, for at most the first 60 seconds after navigation starts
- Startup fast polling may end earlier once the app has gathered 5 consecutive accurate on-route updates after a route is active
- An accurate warmup update means an on-route evaluation with location accuracy of 25 meters or better
- After startup fast polling has ended, a long gap between accepted location evaluations must temporarily resume 1-second checks so the app can restabilize position accuracy before continuing with long dynamic intervals
- The first accepted fix after such a long gap must be treated as location reacquisition: reset stale Kalman velocity and motion/progress evidence, use trusted on-route matches to catch up route/turn state, but suppress immediate off-route or wrong-direction reroutes until follow-up samples confirm the deviation
- Later at a dynamic interval derived from the estimated time to the next direction, using the current speed and remaining route distance when the next maneuver still lies on the current matched route segment and live speed is available, or route timing metadata when the next maneuver lies beyond the current matched route segment
- When the next direction is estimated to be 8 seconds away or less, the dynamic interval must be 1 second
- Otherwise the dynamic interval should scale to roughly one quarter of the estimated time remaining to the next direction
- When the next maneuver or arrival is estimated within about 3 minutes, the dynamic interval should be capped at 20 seconds so speed changes cannot leave the app waiting through a long quiet window near guidance-critical points
- The post-warmup dynamic interval must be snapped to a small fixed bucket set instead of continuously varying on every update
- The bucket set must currently be `1s`, `2s`, `3s`, `5s`, `8s`, `12s`, `20s`, `30s`, and `60s`
- The dynamic interval must never be lower than 1 second
- The dynamic interval must never exceed 60 seconds
- Re-requesting location updates must reuse the active listener registration when the requested interval bucket and enabled provider set are unchanged, so the app does not continuously tear down and rebuild subscriptions
- The Google Play flavor may use Google fused location when Google Play Services is available and the user has enabled the fused-location setting
- When fused location is unavailable or disabled, the app must fall back to the legacy platform GPS/network provider path
- The F-Droid flavor must use the legacy platform GPS/network provider path and must not include Google fused-location code or Play Services dependencies
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
- When a route recalculation starts after one or more intermediate stops have been reached, the recalculation must pass only the remaining unreached intermediate stops to BRouter; passed stops must not be reintroduced into the new route request, compass targets, or intermediate-stop progress state

#### 4.4.2 Wrong-direction reroute

- The route must also be recalculated when the user is still on the track but is moving in the wrong direction
- Wrong direction is defined as bearing difference greater than 60 degrees
- Bearing-based wrong-direction detection must only be trusted when the current fix is accurate enough and the heading source is credible for the current speed and displacement
- When numeric GPS bearing accuracy is available, the app should trust GPS bearing for wrong-direction evidence only when the reported bearing accuracy is good enough for walking and cycling use cases and the user is moving at least 0.8 m/s; low-speed walking use must remain supported and must not be excluded by a cycling-only speed gate
- When numeric GPS bearing accuracy is not available, the app should trust GPS bearing for wrong-direction evidence only at course-style speeds of at least 2.5 m/s
- When GPS bearing is not trustworthy enough, wrong-direction detection should fall back to a movement-derived course computed from recent filtered route progress rather than from a single noisy fix pair
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
- When the user has remained stationary for several seconds during navigation, recent filtered fixes show only negligible displacement, and the app has a sufficiently trustworthy live heading sample from the preferred heading sensor path that shows the user is not already facing the route
  - When the previous direction has just been passed, only if advancing guidance requires surfacing a new actionable upcoming instruction rather than replaying the just-passed maneuver; if route-matched progress is stable, the app may surface that next actionable instruction immediately
  - When 20 seconds remain to the next direction, if the next maneuver is actionable and route progress is trustworthy
  - When 5 seconds remain to the next direction, if the next maneuver is actionable and route progress is trustworthy
- The app must suppress or delay turn notifications when the user's route progress is not trustworthy enough to identify the next actionable maneuver
- When the 5-second imminent notification is emitted for a real BRouter voice hint, the visible navigation compass must show that hint's signed maneuver angle using the same red partial arc and target marker as the stationary orientation cue, resolved from the incoming route bearing into an absolute target heading so the marker and arc rotate with subsequent compass movement, and must hide that cue once the hint is passed
- For the initial startup notification, remaining maneuver distance must be trustworthy relative to current location accuracy before the notification is emitted
- For in-route imminent maneuver notifications, the app may rely primarily on stable route-matched progress rather than raw horizontal accuracy alone, so coarse GPS accuracy does not by itself suppress a 10-second or 5-second alert
- In-route imminent maneuver notifications must still be suppressed when the remaining maneuver distance is already too small to be actionable or when route matching is unstable
- For slow walking or hiking speeds, the in-route actionable-distance floor for the 5-second imminent notification may shrink below the normal 5-meter floor when recent along-route progress provides a trustworthy ETA, so the 5-second notification and matching compass target are still reachable before the maneuver is passed
- The app must not emit a passed-turn notification whose displayed remaining distance or time would collapse to zero; in that case it should suppress the passed maneuver and move on to the next actionable instruction
- When the user is already inside the most urgent threshold, the app should emit only the single most urgent imminent-turn notification instead of stacking multiple near-identical alerts
- Synthetic intermediate-arrival instructions must participate in normal approaching-turn notification timing before the intermediate stop is reached, ordered by their along-route position relative to real route voice hints
- When the user's current filtered position enters the destination-reached radius around an intermediate stop, the app must emit an intermediate-destination-reached guidance notification with the mapped arrival symbol, then continue guidance toward the following stop or final destination
- When the user's current filtered position enters the destination-reached radius around the final destination point, the app must emit a destination-reached guidance notification instead of silently ending maneuver alerts
- That destination-reached radius must be based on the final destination point and use the same trusted-accuracy threshold policy as red route deviation display, currently `max(smoothedAccuracy + 8 meters, 10 meters)`; the compass should draw the visible destination/stop radius as that threshold minus the current trusted accuracy radius, so overlap with the current-position accuracy circle matches the arrival check
- Turn notifications must reuse a single notification entry in the notification list so older direction notifications do not pile up
- Replacing a direction notification in the notification list must still be compatible with smart bands or similar devices that mirror notifications as they arrive
- A stationary orientation notification must be emitted only after a short stationary dwell, must require both low recent movement speed and negligible recent filtered displacement, must require a fresh heading sample from the preferred heading sensor path with good coarse calibration when that concept exists for the selected sensor, must treat a fresh deprecated-orientation-sensor medium, low, or unreliable accuracy status as a calibration veto when that sensor is available, and when the selected sensor exposes a per-sample heading accuracy estimate it must suppress the notification unless the required turn still clearly exceeds that uncertainty margin
- Stationary-orientation monitoring via the preferred heading sensor path must remain available during background and screen-off navigation so those advisory notifications still work without the navigation UI being open
- Stationary orientation notifications are advisory turn-to-face-the-route prompts and must not change wrong-direction reroute behavior, which remains gated by trusted movement heading, route-progress confirmation, and reroute confidence rules
- Stationary orientation notifications must be suppressed while a route recalculation is in progress so the app does not emit contradictory off-route and turn-yourself prompts at the same time
- When a stationary orientation notification is emitted and the navigation UI is visible, the compass must show a matching red turn-to-face-route cue: a very thin red partial arc just outside and close to the compass border from the current heading through the target heading, plus a red target triangle inside the compass at the target heading with only its vertex attached to the compass border
- The stationary orientation target triangle must keep a contrasting compass-surface-colored outline so it remains readable when the selected-heading-source calibration background is red
- That stationary orientation cue must remain tied to the notification episode and disappear as soon as the user starts moving, or when the route/notification episode resets
- Each notification message must contain:
  - A direction/status symbol
  - The distance left
  - The time left
  - The direction text
  - The exit number for roundabouts when applicable, including alongside the roundabout symbol as well as in the direction text
  - Hyphen (`-`) separators between fields instead of the bullet character
- When a turn voice is selected in settings, the app must use Android's built-in TextToSpeech service to speak maneuver notifications with the time left first and the direction second, such as `20s turn left`
- When turn voice is disabled, maneuver notifications must remain vibration/visual-only

#### 4.4.4.1 Guidance vibration patterns

- The notification imminent to the next direction must use different vibration patterns for left and right directions
- Guidance notifications that are not classified as left or right, such as straight-ahead or other neutral alerts, must use a third vibration pattern that is distinct from both the left and right patterns
- Off-route or reroute alert notifications that use the generic guidance alert path must use that same generic third vibration pattern rather than reusing the left or right directional patterns

#### 4.4.4.2 Voice hints

- BRouter directions are returned in the GeoJSON property `voicehints`
- Each parsed mode-9 BRouter voice hint must retain the maneuver angle field so compass guidance can display the signed turn angle when the maneuver becomes imminent
- Voice-hint interpretation must follow:
  - `FormatJson.java`
    - [https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/FormatJson.java](https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/FormatJson.java)
  - `VoiceHint.java`
    - [https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/VoiceHint.java](https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-core/src/main/java/btools/router/VoiceHint.java)
- The app must interpret the current BRouter mode-9 GeoJSON command set, including distinct mappings for:
  - `16` beeline
  - `17` exit left
  - `18` exit right
- The app must also support the arrival command `100` and map it to a distinct destination-reached presentation rather than treating it as a normal turn maneuver
- The app must support its own synthetic intermediate-arrival command `101` and map it to a distinct intermediate-destination-reached presentation using the same smartband-safe arrival symbol as command `100`
- User-visible maneuver and notification symbols should favor simple smartband-safe glyphs over ornate emoji presentation so mirrored wearables can render them consistently
- Unknown or unsupported voice-hint commands must fall back to a neutral unknown-direction presentation instead of pretending to be a normal continue instruction

### 4.5 Navigation UI

The navigation UI must show the following in large text:

#### 4.5.0 Portrait vs landscape arrangement

- The navigation UI may use different layouts in portrait and landscape as long as the same navigation information and actions remain available
- While the dedicated navigation UI is visible, the app must keep the display awake so the screen does not time out during active on-screen guidance
- On phone-sized screens in landscape orientation, the navigation UI must switch to a two-column layout
- In that landscape layout, the left column must contain all navigation text content and both action buttons
- In that landscape layout, the right column must contain only the compass route view plus the overlaid GPX export control
- In that landscape layout, the left column must keep the turn-instruction block near the top and the blocked-road, stop, and pause/resume actions together in the bottom action row instead of placing them under the compass

#### 4.5.1 Next two directions

- Near the top, below the GPS status line: the next two directions
- Each must include the mapped direction symbol, text, distance left, and time left
- For roundabouts, the mapped direction symbol should also include the exit number while the text continues to spell out the exit number
- The first upcoming direction must show distance and time from the user's current matched position
- If the first upcoming direction still lies on the current matched route segment, its displayed time left should use trustworthy smoothed live speed when available and otherwise fall back to BRouter-derived timing for the remaining current-segment portion when available
- If the first upcoming direction lies beyond the current matched route segment, its displayed time left should combine the remaining current-segment time with BRouter-derived time for all following route segments up to that maneuver
- The second upcoming direction must show distance left and time left relative to the first upcoming direction rather than relative to the current position
- The second upcoming direction's relative time should be derived from BRouter timing between the first and second maneuver points when available
- The navigation UI must only surface directions whose distance is outside the current minimum trusted maneuver radius; unreliable micro-maneuvers should be skipped in favor of the next trustworthy instruction
- In ambiguous low-confidence conditions, temporary absence of a next-turn line is preferable to presenting a wrong or misleading turn
- When BRouter reports command `100`, the navigation UI must treat it as the authoritative destination-reached arrival instruction
- When no further actionable maneuver follows the final maneuver and BRouter has not reported command `100`, the navigation UI must synthesize a destination-reached arrival instruction at the final route point
- Before the user enters the destination-reached radius, destination-reached instructions must behave like an upcoming direction and include the remaining distance and time, including relative distance and time when shown as the second line after the final maneuver
- Before the user enters the destination-reached radius for an intermediate stop, synthetic intermediate-destination-reached instructions must behave like upcoming directions and be ordered between surrounding real maneuvers by along-route position
- Once the user is inside the destination-reached radius for an intermediate stop, the intermediate-destination-reached instruction must be emitted as a guidance notification, then the primary next-direction line must advance to the following actionable maneuver while the secondary line shows the next instruction after that when one exists
- Once the user is inside the destination-reached radius, the primary next-direction line must switch to a destination-reached message using the mapped arrival symbol instead of remaining blank
- That terminal destination-reached presentation should omit misleading `0 m` or `0 s` countdown fields and behave as a terminal guidance state rather than as another ordinary turn
- The first upcoming direction must keep the full available instruction row width
- Both direction lines must stay on a single line and should reduce text size as needed before falling back to end-ellipsis truncation

#### 4.5.2 Compass route view

- In the center: a map-free compass canvas showing the active route relative to the current position
- The compass must not render a map background
- The route must rotate live with the latest trusted display heading so forward stays at the top of the view
- The compass must draw the destination-reached radius around the destination marker using the same transparent red treatment as the route-threshold overlay
- While a route-start beeline approach target is active, the compass should show a live target marker and dotted red bearing line from the current position toward the snapped original route start, without drawing that approach as part of the off-track route corridor
- While the user is moving at course-style speeds of at least 2.5 m/s, the displayed compass heading should prefer trusted GPS/course heading to reduce jitter; at walking speeds below that threshold, the displayed compass should prefer the live heading sensor when available
- At course-style speeds, the displayed compass heading should fall back to a movement-derived course when GPS/course heading is unavailable or too inaccurate
- The displayed compass heading must be compensated for the current screen rotation so portrait and landscape show the same real-world forward direction at the top of the view instead of drifting by 90 or 180 degrees
- When the user is stationary, moving below the course-heading display threshold, or when neither trusted GPS/course heading nor movement-derived course is available, the displayed compass heading may fall back to the live heading from the preferred heading sensor path
- The preferred heading sensor path must use the standard rotation vector when the platform exposes it and may fall back to the geomagnetic rotation vector when that is the only available fused heading sensor
- When the deprecated orientation sensor is available, navigation may register it only as a calibration cross-check. A fresh medium, low, or unreliable deprecated-orientation accuracy status should conservatively downgrade the displayed live heading-sensor accuracy, but its azimuth must not replace the rotation-vector or geomagnetic-rotation-vector heading.
- Live heading-sensor-driven compass rotation is only required while the navigation UI is visible and the screen is interactive
- The compass outer ring must carry the rotating cardinal labels `N`, `O`, `S`, and `W`
- The inner circles must remain stable visual distance references for the route
- When the user is stationary, the compass should zoom out to fit the full active route overview inside the compass
- When the user is moving and the current native speed reading is reliable, the compass should zoom to a forward-looking radius representing about 60 seconds of travel
- A single tap on the compass route view must toggle the currently displayed zoom mode between the stationary full-route overview and the moving 60-second view
- Tap-driven zoom changes must use the same smooth radius transition used when the compass switches from stationary overview to moving 60-second view
- That moving 60-second radius must not be capped to a smaller fixed maximum such as 600 meters
- When the user is moving but the current native speed reading is not yet reliable, the compass should prefer reusing the last reliable moving zoom radius if one exists instead of jumping back and forth between zoom modes
- When the user starts or resumes movement without a reliable moving-speed reading and no previous reliable moving zoom radius exists yet, the compass should fall back to the full-route overview until a reliable moving-speed reading becomes available
- When the user stops and the compass expands back to the full-route overview, the last reliable moving zoom radius should be preserved so it can be restored when movement resumes before speed confidence has recovered
- Automatic compass zoom/radius policy is a navigation-state responsibility and must remain separate from compass drawing and activity/service lifecycle wiring
- That tap-driven zoom toggle must be only a UI override layered on top of the existing automatic behavior, so stationary navigation still defaults to the full-route overview and moving navigation still defaults to the 60-second view whenever no temporary override is active
- If the user taps the compass while moving and the currently displayed 60-second view is active, the compass must switch to the full-route overview temporarily and then automatically restore the moving 60-second view after about 5 seconds
- The compass route geometry and hint-marker geometry should be sampled once per active route and reused across UI updates instead of rebuilding full projected route lists on every heading or location refresh
- Compass rendering should avoid per-frame transient object allocation in its hot drawing path for route, hint, and destination projection
- In the moving 60-second view, including after a tap from the full-route overview, the red route centerline and wider threshold overlay must remain continuously visible for the route portion crossing the compass instead of flickering or disappearing while off-screen route geometry is clipped
- Transitions between stationary overview and moving zoom should be smoothed instead of snapping abruptly, except that restoring a previously saved reliable moving zoom radius after a stationary pause may return directly to that saved scale to avoid intermediate zoom thrash
- Transitions between the full-route overview and the moving 60-second view should use the same fast timing in both directions, reaching the target scale in about 1 second regardless of the total route length or overview radius delta
- The current-position marker should be shown as a small center dot
- A transparent orange filled circle centered on the current-position dot must visualize the current GPS accuracy radius at the compass scale, using the same orange as the accent ticks on the outer compass ring
- A semi-transparent fixed vertical guide line must run from the center dot to the top border of the compass and end with an open arrowhead whose tip aligns with the guide line
- When the displayed heading source exposes a heading-accuracy estimate, the compass must show two semi-transparent white straight guide lines, using the same visual treatment as the fixed top heading guide, from the center to the outer distance ring at the negative and positive angular error bounds around the fixed top heading guide
- The compass must show a transient calibration background in the outer compass layer when the selected displayed heading source becomes explicitly inaccurate enough to need recalibration or recovery: translucent red while that selected source is inaccurate, translucent green when it returns to acceptable accuracy, and the green background must disappear automatically after about 2 seconds
- That calibration background must span from the outer visible distance ring to the outer border of the compass, sit behind the outer compass ticks and cardinal labels, and remain visually separate from the stationary orientation cue drawn around the compass border
- That calibration background must follow the same selected heading source that drives the rendered compass heading, such as trusted GPS/course while moving, movement-derived course when GPS/course bearing is not trustworthy, or live heading-sensor heading when stationary or otherwise needed; it must not show a raw heading-sensor calibration warning while the compass is currently rendered from a different trusted source
- Missing numeric heading-accuracy data alone must not force the calibration background red; the warning should require explicit poor heading accuracy from the selected displayed heading source, including a fresh deprecated-orientation-sensor low or unreliable accuracy status when live heading-sensor heading is the selected display source
- While navigation is paused, the compass must show a light gray translucent paused-state background in the outer compass layer, using the same layer geometry and transparency as the calibration background, in addition to the paused-state status text and resume/play button
- Each distance ring should show a semi-transparent distance label on the right side and a matching travel-time label on the left side
- In moving mode, the top visible distance ring is the primary horizon reference for those labels; inner-ring distance and time labels must scale from that top visible ring rather than from the hidden compass edge
- When heading accuracy is zero or unavailable, those labels should stay aligned with the short vertical-guide tick at the top ring intersection
- When heading accuracy is non-zero, the top tick should be replaced by a semi-transparent arc spanning between the left and right heading-accuracy guides, and the right distance label plus left travel-time label should align with those guide intersections
- Small semi-transparent white point markers must be shown on the route at the visible start position and at each visible hint position
- The route must be rendered as a continuous line, not as discrete dots
- The route ahead of the current matched position must keep the normal route red styling
- In moving mode and in the stationary full-route overview, the route ahead must be rendered in two red layers whenever the current off-track threshold extends beyond the current GPS accuracy radius: the original opaque route centerline plus a wider semi-transparent threshold overlay behind it
- That wider threshold overlay must visualize the current off-track threshold derived from recent smoothed location accuracy, so it acts as the allowed route corridor rather than a purely decorative fixed-width line
- That threshold overlay width should reflect the full threshold span around the route centerline excluding the GPS accuracy radius, not only a one-sided offset. This way, when the orange accuracy circle overlaps the red corridor, the user is still on track; when they no longer overlap, the user is off track
- In the stationary full-route overview, the red route centerline itself must keep a fixed visual stroke width instead of scaling to the off-track threshold, even though the threshold overlay remains visible
- The already passed part of the currently active route must be shown as the same red with about 50 percent transparency
- When a reroute is applied, the passed-route overlay must be rebuilt from the new active route geometry and must not retain passed geometry from the previous route
- The destination endpoint must be shown as a slightly larger opaque white point without a finish-line icon or enclosing badge
- The destination endpoint must only be shown when it falls within the currently visible compass radius; if it lies outside the visible radius, it should not be clamped back onto the compass edge as a detached marker
- Each remaining intermediate stop must be shown on the compass route with the same opaque white point radius as the destination endpoint and the same transparent destination-reached radius overlay

#### 4.5.3 Shared status block

- Below the compass, the UI must use a single shared status text block instead of separate destination-progress and secondary-detail text areas
- When no higher-priority notice is active, that shared status block must show the final destination progress and, if an intermediate stop is still ahead, the next intermediate-stop progress in the same text area
- The destination progress portion should use the same segment-aware hybrid estimator as maneuver timing: trustworthy smoothed live speed only for the remaining current-segment portion, plus BRouter-derived time for later segments
- The next intermediate-stop portion should use the same segment-aware hybrid estimator as maneuver timing and destination progress
- When the current next intermediate stop is passed, the shared status block must switch to the following intermediate stop if one remains
- Reaching an intermediate stop must not switch the shared destination progress line to the terminal destination-reached state; destination progress should remain active until the final destination is reached
- The UI must not list all remaining intermediate stops at once in that shared status block
- When a navigation detail or notice needs to be surfaced in that area, such as route-unavailable detail, blocked-road reroute feedback, or paused-state messaging, that detail must take precedence over progress content in the shared status block
- When the user explicitly triggers the blocked-road action and a reroute request starts, the shared status block should surface a specific blocked-road reroute-progress notice such as `Blocked road added. Recalculating route.` instead of only showing the generic route-calculation body text
- After the user enters the destination-reached radius, the shared status block should switch from live destination progress to a destination-reached message unless a higher-priority detail notice is active

#### 4.5.3.1 GPS status line

- The navigation UI must show a single GPS status line formatted as `<speed> ↑<elev> <bearing> • <accuracy> <bearing-accuracy> • (<sat>) #<fix-count> • <countdown>`
- That line must show the current speed, current elevation, current horizontal accuracy, GPS bearing, GPS bearing accuracy, the current number of GNSS satellites used in the fix, and the total number of accepted location fixes acquired during the current navigation session, in that exact order
- The current horizontal accuracy and GPS bearing-accuracy values in that line must be emphasized in orange when they are available
- That GPS status line must stay on a single line and should reduce its text size as needed instead of wrapping onto a second line
- When any of those values is unavailable, the UI must show `--` in that field instead of omitting it
- The `<countdown>` field shows the time remaining until the next scheduled navigation position evaluation based on the current active update interval; it is a scheduling countdown and must not be interpreted as a guarantee that Android or fused location cannot deliver an earlier usable fix
- When tracking is active and an accepted location fix is processed, the `<countdown>` field should refresh from the active update interval, including when the existing location listener registration is reused rather than torn down and recreated

#### 4.5.3.2 GPX export button

- The navigation UI must show an icon-only export button in the top-right corner of the measured compass square
- The export button must overlay the compass area so it does not shrink, reflow, or otherwise change the compass route view
- The export button must sit in the square corner outside the compass circle rather than covering the circular compass surface
- Pressing the export button must transform the current active route into GPX and open it through an Android chooser using the GPX MIME type `application/gpx+xml`, with the GPX file provided as a stream and GPX viewer apps offered as explicit targets, so the user can select which installed GPX-capable app should receive the export instead of Android auto-opening a saved default viewer
- The exported GPX route, track, and metadata name must use `ViBRo-Navigator Export <current datetime>` rather than the destination label
- The exported GPX must include the active route geometry as GPX route/track geometry, must include turn-instruction waypoints derived from the current route voice hints using the same user-visible instruction text mapping as the navigation UI and turn notifications, and must include explicit waypoint entries for remaining intermediate destinations
- When the active route has no explicit destination-arrival voice hint, the exported GPX should include a synthetic destination-reached waypoint at the final route point
- The export flow must write the generated GPX XML into the application log before launching the chooser
- The export flow must use an app-private cache file exposed through a read-only `FileProvider` content URI with a temporary read grant, not broad storage permissions
- If no active route exists, the navigation UI must show a short failure message instead of opening an empty GPX file
- If no installed app can open GPX routes, the navigation UI must show a short failure message instead of crashing

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
- The resume/play icon must use the same green play symbol as the main start navigation button
- Pressing resume must continue the existing navigation session instead of starting a fresh route-planning flow
- Portrait and landscape layouts must both expose the pause/resume action alongside the blocked-road and stop-navigation actions
- In the bottom action row, the action order must be blocked-road, stop, then pause/resume from left to right, with matching spacing around the three circular buttons

#### 4.5.6 Stop navigation button

- At the bottom action row: an icon-only circular button to stop navigation
- In that bottom action row, the stop button must sit between the blocked-road button and the pause/resume button
- Pressing it must first show a confirmation dialog before stopping the active navigation session
- Confirming the dialog must stop navigation and return to the previous UI
- Canceling the dialog must leave the current navigation session running and keep the navigation UI open
- Destination and intermediate stops must be kept

#### 4.5.7 Back button behavior during navigation

- Pressing the system back button while the navigation UI is open must move the whole app task to the background
- Pressing back during navigation must not reveal the main UI underneath the navigation UI
- Navigation must continue running after this backgrounding action as long as the foreground service remains active
- On Android versions that use predictive back, the navigation screen must keep the same backgrounding behavior through the platform back-dispatch path instead of relying only on legacy `onBackPressed()` callbacks

#### 4.5.8 Android Auto view

- Android Auto support must exist only in the Google Play flavor.
- The F-Droid flavor and common source set must not include Android for Cars App Library dependencies, Android Auto manifest entries, or Auto-specific runtime classes.
- Android Auto must expose a `CarAppService` using `androidx.car.app.CarAppService` and declare the `androidx.car.app.category.NAVIGATION` car app category.
- The Google Play flavor must declare the `template` capability through `automotive_app_desc.xml` so Android Auto can discover the app.
- The Google Play flavor must declare the Android for Cars surface permission needed to draw the custom navigation surface.
- The Android Auto service should use the lowest practical `androidx.car.app.minCarApiLevel`, currently `1`, and prefer broadly supported templates and APIs so it remains compatible with as many Android Auto host versions as possible.
- Android Auto must not try to launch or render the phone `NavigationActivity` directly on the car display. Android Auto hosts a driver-optimized template surface, not arbitrary phone `Activity` layouts.
- The active Android Auto screen must use an Android for Cars navigation surface to draw a landscape-style navigation view: navigation text and the three-button action row on the left, and the compass route view on the right.
- The Android Auto surface must mirror the same active navigation state shown by the phone landscape navigation layout, including at least the GPS status, next direction, second direction when available, destination/progress/status text, blocked-road, stop navigation, pause/resume, and the compass route view.
- The Android Auto compass should reuse the existing `NavigationCompassView` rendering path so compass route geometry, radius behavior, paused-state chrome, orientation cues, and destination/intermediate markers stay consistent with the phone navigation screen.
- Android Auto should expose the blocked-road, stop, and pause/resume controls both through the drawn surface layout and through the Android for Cars template action strip when required by the host template.
- Tapping the compass area on the Android Auto surface should preserve the same temporary compass zoom toggle behavior as tapping the phone compass.
- When no active navigation is available, the Android Auto screen must show a concise no-active-navigation state and provide a way to open the phone app.
- Android Auto UI state should bind to the existing `NavigationService`/`NavState` listener path rather than duplicating route calculation, location tracking, or guidance logic.
- Android Auto controls must use the existing navigation binder/service actions for blocked-road, pause/resume, and stop, so phone and car surfaces stay consistent.
- Android Auto text must come from flavor resources and must not be hardcoded in Java.

### 4.6 Background behavior

- Navigation functionality must remain active in the background
- Navigation functionality must remain active when the screen is off
- Background and screen-off navigation reliability must be provided primarily by the location foreground service and ongoing location callbacks rather than by holding a session-long CPU wake lock
- Partial wake locks may be used only as short, focused guards around critical work such as startup bootstrap, route calculation, or reroute calculation, and each acquisition must use an explicit timeout and be released by the same flow that acquired it
- The app must not rely on continuously renewing or indefinitely holding a partial wake lock for the full navigation session
- Screen-off or background navigation may suspend compass UI updates, but heading-sensor monitoring needed for stationary-orientation notifications must continue
- If the platform cannot deliver the required heading-sensor samples while the device is asleep without a session-long wake lock, stationary-orientation notifications may degrade to best-effort while core location tracking and route guidance continue

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
  - Links immediately after the summary to the project source code and the GitHub new-issue page
  - Copyright and license text
  - API/data-source attribution stating the active POI search data source and that map tiles and geodata are by OpenStreetMap contributors, including the `https://www.openstreetmap.org/copyright` URL
  - A Settings section below the about text
  - A Diagnostic section at the end

#### 5.1 Logging and diagnostics

- The about page Settings section must show a Log enabled switch
- The about page Settings section must show a Use fused location switch
- The about page Settings section must show a Use imperial units switch for distance, speed, elevation, and accuracy display values
- The about page Settings section must show a single-row POI category filter setting with a `POI categories filter` label, an icon-only list button for editing category names, and a switch that enables or disables the map POI category filter
- The POI categories filter editor must let the user manage multiple category-name fields, each with the placeholder `POI Category Name`, an item switch between the field and an `X` remove button, plus a centered `+` button that adds another field
- Fresh installs must prefill the POI categories filter editor with commonly needed categories for driving, walking/running, and cycling: `Bicycle Repair Station`, `Drinking Water`, `Fuel`, `Hospital`, `Parking`, `Pharmacy`, `Police`, `Public Transport Stop Position`, `Supermarket Shop`, `Taxi`, and `Toilets`
- Fresh installs must enable the POI category filter by default
- The about page Settings section must show a Speech directions voice spinner that can disable spoken maneuver notifications, use the system default TextToSpeech voice, or select one of the downloaded/offline Android TextToSpeech voices available on the device
- Downloaded/offline Speech directions voice options should show user-friendly labels derived from the voice locale and readable voice variant when available, instead of exposing raw TextToSpeech engine identifiers in the spinner label
- The Speech directions voice spinner dropdown should visually highlight the currently selected voice option
- The Speech directions voice spinner row must include an icon-only settings button that opens the device's built-in Android Text-to-speech settings page, falling back to Android's TTS data installer when the settings page is unavailable
- The Google Play flavor must let the user save an optional Google Maps API key for POI search; when this key is present, POI search must use Google Maps Geocoding instead of OpenStreetMap Nominatim
- The F-Droid flavor must not enable the Google Maps API key setting
- The about page Settings section must show an Export database button that lets the user save a JSON backup of all app-managed stored data, including POI history, app settings, logging preference, and BRouter profile selections
- The about page Settings section must show an Import database button that lets the user select a JSON backup and restore those same app-managed stored data stores
- Database export and import must use Android's document picker flows so the user chooses the backup file location without requiring broad storage permissions
- The Use fused location switch must be enabled only in builds that support Google fused location
- In the F-Droid flavor, the Use fused location switch must be disabled and must not enable Google functionality
- In the Google Play flavor, disabling Use fused location must force the legacy platform GPS/network provider path even when Google Play Services is available
- The app must write its log file only when the Log enabled setting is switched on
- The Log enabled setting must persist across app launches
- When Log enabled is already on at app startup, the app must create a fresh log file for that app session before startup logging begins
- When Log enabled is switched on during an app session, the app must create a fresh log file for the remaining logs in that session
- A single log file must contain all logs written from the app session's log-file creation until termination or until logging is switched off
- Log files must use the `vibro-navigator-log-yyyymmddhhmmss.txt` naming pattern, with a collision suffix when needed so app sessions opened close together do not overwrite each other
- When logging is enabled, the app must log the full decoded BRouter response payload in addition to the existing route summaries
- The logging implementation should keep a single shared path for log-entry formatting and file appends so single-line and multiline records cannot silently diverge in behavior
- The about page Diagnostic section must currently list the app's used live inputs:
  - GPS provider
  - network provider
  - rotation vector heading sensor
  - geomagnetic rotation vector heading sensor
  - deprecated orientation sensor, used by navigation only as a calibration cross-check when available
- The diagnostics block must refresh automatically every 1 second while the about page is visible
- Each listed item must show both its current status and its latest available value details
- Location-provider details should include the latest available fix data such as coordinates, accuracy, speed, bearing, bearing accuracy, satellite count, and sample age when available
- Heading-sensor details should include the selected sensor type plus the latest available heading/orientation-derived values and sample age when available
- The about page Diagnostic section must also show actions to send notification-symbol tests for left, other, and right guidance notifications
- Triggering any of those actions must post a fresh notification entry, not only update an existing one, so mirrored smart bands or similar devices can treat each test run as a new notification
- Those test notifications must contain the full set of distinct user-visible symbols currently used by the app's notification text formatting, including all direction/status symbols used in guidance notifications and the degree sign used by stationary-orientation notifications
- The test notification titles must identify the tested group as `test all lefts notifications`, `test all others notifications`, or `test all rights notifications`
- Those test symbols should remain simple enough to render on generic smart bands rather than assuming full emoji support

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
- On devices where multiple apps can handle the same map/share intent, the system chooser may appear before the user selects ViBRo Navigator

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

## Distribution and release expectations

- The repository should remain suitable for official F-Droid inclusion.
- The Android app must be built with explicit `fdroid` and `gplay` product flavors.
- The F-Droid flavor must not include Google Play Services dependencies, Google fused-location code, Google POI search code, or any runtime requirement for a Google API key.
- Google-specific implementation code and Android Auto implementation code must live in the `gplay` source set, with F-Droid-safe stubs in the `fdroid` source set and flavor-neutral interfaces in the common source set where needed.
- Android for Cars App Library dependencies and Android Auto manifest/resource declarations must be scoped to the `gplay` flavor only.
- GitHub Actions and F-Droid metadata must build the `fdroid` flavor for F-Droid readiness and submission paths.
- Upstream release automation may build both `fdroid` and `gplay` release APKs, with artifacts kept under their flavor-specific Gradle output paths.
- Upstream app-store metadata should be maintained in the source repository using the `fastlane/metadata/android/en-US/...` layout so F-Droid can reuse the app description, changelog, icon, and screenshots directly from upstream.
- The repository should provide maintainer-facing submission documentation for official F-Droid inclusion. That documentation is an operator runbook for project maintainers and should not be treated as end-user product documentation.
- GitHub Actions may automate F-Droid readiness checks and preparation of a `fdroiddata` merge request, but the specification should assume that official publication still requires F-Droid maintainer review and F-Droid-side rebuild/sign/publish steps.
- Release builds intended for upstream verification and F-Droid consumption must be unsigned by default in CI unless an explicit maintainer-controlled signing flow is being used outside the F-Droid path.
- Machine-specific local development configuration, such as SDK paths or local API keys in `local.properties`, must not be required for release validation or F-Droid builds.

## Implementation guidance

- Keep UI entry-point packages separated by purpose: `main` for destination/profile/stop setup, `map` for manual map picking, and `nav/ui` for the active navigation screen. `main/MainActivity` and `nav/ui/NavigationActivity` should stay thin. Input validation, incoming-intent handling, main-screen widget binding, destination field state persistence, startup/preflight checks, and navigation startup orchestration should stay in dedicated helpers.
- Keep navigation display state separated from text assembly: `nav/model/NavState` should remain the immutable display snapshot, with route/guidance/progress, GPS, and pause state exposed through focused value objects rather than duplicated top-level scalar aliases. Android/resource-aware state assembly should stay in `nav/presentation/NavStateComposer` and use `NavStateBuildInput` plus `nav/session/NavigationDisplaySnapshot` for route-display handoffs. Route direction/progress line assembly should stay in `nav/format`, compass-state assembly should stay in `nav/compass` through `NavCompassStateFactory`/`NavCompassStateInput`, compass rendering should stay in `nav/compass/ui`, route GPX export should stay in `nav/export`, and primitive navigation text formatting should remain shared between on-screen state, GPX instruction waypoints, and notifications.
- Keep `nav/service/NavigationService` focused on Android lifecycle and orchestration, with dependency construction/attachment grouped by foreground, tracking, and routing runtime contracts, start/stop command handling, notification callbacks, location/provider event handling, location subscriptions, route execution callbacks, listener broadcasting, UI-visibility/compass gating, orientation/display-heading preparation, paused-state turn-event gating, and turn-event fan-out isolated in focused collaborators such as `NavigationServiceDependencies`, `NavigationServiceCommandHandler`, `NavigationServiceLocationHandler`, `NavigationServiceRouteCallback`, and `NavigationServiceTurnEvents`.
- Keep wake-lock ownership narrow and task-scoped in `nav/power`. Long-lived navigation reliability should come from the foreground location service, while any partial wake lock should be acquired only by the collaborator performing the short critical section that needs it.
- Keep background route computation asynchronous in `nav/routing` while all shared navigation-state mutation remains serialized on the main thread. Route executor threading/callback handoff, transient-failure retry policy, and the BRouter adapter should remain separate collaborators.
- Keep `nav/session/NavigationSession` split across focused collaborators for filtered location, route progress, blocked-road state, turn progression, route-request lifecycle handling, active-route export handoffs, display-state handoffs, and explicit handoff value types rather than collapsing that logic into one class.
- Keep active route/polyline geometry ownership in `nav/route`, route-location evaluation in `NavigationRouteEvaluator`, final-arrival checks in `NavigationArrivalDetector`, intermediate-arrival tracking in `NavigationIntermediateArrivalTracker`, blocked-road point selection in `NavigationBlockedPointSelector`, route-result application in `NavigationRouteResultApplier`, route-deviation policy, confirmation, direction-of-progress evidence, and reroute-notice selection in `nav/guidance`, route display branching in `NavigationSessionRouteDisplayState`, compass display memory in `CompassDisplayMemory`, and route display assembly in `nav/presentation`/`nav/format`/`nav/compass` so safety decisions stay easy to review independently. Display memory updates should be explicit display-advance steps rather than hidden side effects of pure state construction.
- Keep compass display state grouped by display mode, radius state, progress labels, orientation cue, and route points. `NavCompassState` should remain the top-level immutable compass snapshot, while rendering code consumes `CompassDisplayMode`, `CompassRadiusState`, `CompassProgressLabels`, `CompassOrientationCue`, and `CompassRoutePoint` instead of relying on duplicate scalar aliases. Construction should use named factories for projected-point snapshots and route-geometry-backed snapshots, with grouped construction inputs for display metrics, radius metrics, destination projection, and optional orientation cue rather than direct public constructors or long primitive constructor chains.
- Keep heuristics such as reroute thresholds, bearing trust rules, forward-look route bearing, direction-of-progress checks, polling cadence, synthetic intermediate-arrival sequencing, and turn-alert timing in small policy/planner helpers. Keep POI query/search state shared across destination and stop fields, with text-field selection state, history rename/delete actions, popup-window presentation, and query precedence/debounce/provider search kept in separate collaborators.
- Keep flavor-specific services behind a small distribution bridge. Common code may call flavor-neutral interfaces, but Google Play Services imports, Google fused-location implementation, Google POI search, Android Auto service/template code, and Google parser tests must remain under `app/src/gplay` or `app/src/testGplay`. The `fdroid` source set must provide no-op or OpenStreetMap-only behavior for the same bridge contracts.
- Keep the Android Auto entry point in `app/src/gplay/java/vibro/navigator/auto`. Auto screens should consume `nav/model/NavState` through the existing `NavigationService` listener/binder API and translate that state into Android for Cars templates without owning navigation-domain decisions.
- Keep `nav/model/NavigationRequest` as a pure domain request. Keep the navigation-intent extras contract owned by `nav/intent/NavigationRequestIntentContract` so activities, the foreground service, and resume notifications serialize the same request shape without hand-copying extras. Keep app-wide incoming map/share URI parsing under `intent/`, separate from navigation-start extras.
- Prefer extending the existing `logging/AppLogger` coverage when touching startup, permissions, routing, background execution, or network search behavior.

## Testing expectations

- Prefer JVM regression coverage, with Robolectric for Android lifecycle behavior where practical.
- The core automated suite should not require an emulator or real device, though some foreground-service and OEM notification behaviors may still need manual verification.
- Keep lifecycle decisions, heuristics, planners, and policy thresholds in small helpers when practical so they remain directly unit-testable.
- Maintain coverage for navigation-request serialization, startup/preflight flow, reroute heuristics, bearing trust, route-progress confirmation, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route-execution callback handoff, turn-event dispatch, and safe listener broadcasting.
- Voice-hint mapping coverage should verify the current BRouter mode-9 command set, including user-visible direction symbols.
- Maintain a zero-violation PMD maintainability gate for production and JVM test Java sources, including flavor-specific source sets, covering complexity, size, coupling, nested-flow, dead-code, duplicate-literal, and related rules.
- Distribution-sensitive changes should run explicit flavor checks, including `testFdroidDebugUnitTest`, `testGplayDebugUnitTest`, `lintFdroidDebug`, `lintGplayDebug`, `assembleFdroidRelease`, and `assembleGplayRelease`.
- Refactors that only move unchanged wiring into helpers or package-level value contracts do not require new tests by default. Behavior changes in helper-owned flows should add or update focused JVM or Robolectric coverage.

# ViBRo Navigator Specification

## Source

This specification is derived from the original project-generation prompt and captures the intended product requirements for ViBRo Navigator.

## Product summary

ViBRo Navigator is a lightweight Android navigation app based on BRouter.

Core product constraints:

- Use Java
- Avoid dependencies as much as possible. Google Play Services may be used only in the Google Play distribution flavor for explicitly requested Google features; the F-Droid flavor and common source set must remain free of Google Play Services dependencies.
- Keep generated and maintained code minimal while still implementing the features
- Target the latest practical Android SDK while keeping `minSdk 23`
- The current repository baseline is `compileSdk 37` and `targetSdk 37` while keeping `minSdk 23`
- Use a black theme by default and provide a settings switch for an optional light theme
- Do not hardcode user-facing text in code
- Support both portrait and landscape orientations
- Check and request all required permissions before starting navigation, when they are needed
- When navigation startup depends on system settings, route the user to a reachable settings screen that stays open on supported OEM builds, even if the device requires a generic settings page instead of a per-app approval dialog
- If a startup settings dialog is dismissed after the required setting was changed elsewhere, navigation startup must re-check preflight and continue; if the blocker remains unresolved, startup must abort cleanly instead of leaving the navigation screen waiting
- Battery optimization exemption is recommended for reliable background navigation. When absent, navigation startup must show the user an advisory prompt to open the exemption request, but opening, cancelling, or dismissing that prompt must continue navigation startup instead of blocking it.
- Provide a README describing ViBRo Navigator as a lightweight, battery-efficient, offline vibe-coded GPS navigation app based on BRouter, that vibrates directions
- Provide a distinctive app logo suitable for use as the app icon
- Treat map-free use as a primary product mode: navigation guidance must be trustworthy enough that a user who does not see the map can rely on the next direction without visual confirmation
- When the current position or heading confidence is too weak, prefer delaying or suppressing a direction update over presenting a misleading one
- Android Auto support is Google Play flavor only. It must use the Android for Cars App Library template model, because Android Auto does not host the phone `Activity` layout directly.

## Functional specification

### 1. Main UI

The app must show a main UI implemented as an Android `Activity`.

The main UI must include a navigation-mode selector at the top, with the BRouter profile selector directly below it when a BRouter-backed mode is active.

#### 1.0 First-open welcome

- Before setup prompts or incoming route actions, show a full-screen welcome with the welcome title, a one-sentence TL;DR, one sentence introducing the app, an Initial setup heading, and short setup bullet points in that order.
- Focus on BRouter installation and regional routing-data downloads, `profiles2` folder access or legacy storage permission, location and notification access at navigation startup, the optional battery optimization exemption, and conditional microphone access for voice search.
- Keep the welcome concise and focused on initial setup; leave navigation instructions and mode explanations to their existing in-app help.
- Keep the text scrollable and Continue reachable in portrait, landscape, and with larger system fonts; follow the selected dark/light theme.
- Persist completion only after Continue. Closing before completion must show the welcome on the next open, and activity recreation must retain the welcome and its scroll position. After completion, resume normal setup and preserve any incoming destination or GPX intent. Subsequent opens skip the welcome.
- Keep welcome completion in installation-local storage excluded from Android and in-app backups. A restored settings backup or an updated package timestamp must not skip an uncompleted welcome. Ordinary updates retain completion; reinstalls or clearing app data show the welcome again. Existing installations with only the legacy preference show the welcome once when migrating to this storage.
- Notification taps must still resume an existing navigation session immediately while the welcome is open.

#### 1.1 Routing profiles

- The BRouter profile selector items must include BRouter profile names plus a single custom-profile entry
- Profiles may come from bundled BRouter internal profiles or from user-accessible `.brf` files in autodiscovered external `profiles2` folders
- The selector is profile-based, not a separate vehicle-type toggle
- Profile handling must remain compatible with both bundled BRouter profiles and autodiscovered external `profiles2` folders
- If BRouter is not installed, the app must not immediately open a profile-file picker during main-screen startup
- If no autodiscovered external `profiles2` folder is accessible, the app must still list and use bundled BRouter internal profiles for normal routing
- The routing-profile selector must use the same external `profiles2` discovery logic as the custom-profile picker
- When one or more external `profiles2` folders are discoverable, the selector should list those external `.brf` profiles alongside bundled BRouter profiles
- When no external `profiles2` folder is discoverable, the selector must fall back to bundled BRouter profiles if bundled profiles are available
- When neither discoverable external `profiles2` folders nor bundled BRouter profiles are available, the selector must still show a single custom-profile entry so the user can pick a `.brf` file manually
- Straight-line guidance must be selected through the navigation-mode selector, not as a BRouter profile row, and must remain visible and usable when BRouter is not installed
- The selector must continue to behave like a normal dropdown even when a custom profile is currently selected
- Bundled BRouter profile rows in the opened selector must include an info control that opens a UI showing the profile title, strengths, weaknesses, and distinctive usage guidance; custom or unknown external profiles must not get bundled-profile descriptions
- Experimental or debug bundled profiles such as `car-eco-de`, `moped`, `dummy`, `rail`, and `river` must be visually marked with a red experimental/debug indicator in the opened selector
- A profile-settings icon button must be shown next to the routing-profile selector in BRouter-backed modes and hidden when Straight Line mode is selected
- For a selected BRouter profile, the profile-settings UI must parse editable parameters declared in the selected `.brf` profile using BRouter's `%parameter% | description | type` comment convention
- The profile-settings UI must show each parameter name and a type-appropriate input: switch for boolean parameters, numeric input for number/integer parameters, spinner for bracketed option lists, and text input when no supported type can be inferred
- When a profile parameter has a description, the UI must show an information button for that parameter
- Saved profile-parameter values must be stored separately by normalized profile name, so switching from profile A to profile B and back to profile A restores profile A's edited values
- Custom profiles use the same profile-name key as bundled profiles, allowing a custom profile with the same name as a bundled profile to reuse the bundled profile's saved values and, when needed, parameter metadata fallback
- The profile-settings UI must include `Reset defaults` next to `Cancel` and `OK`; after confirmation, reset must restore the values defined by the profile and clear saved overrides for that profile
- Route calculation requests sent to the BRouter Android service must pass saved non-default profile-parameter overrides through the `extraParams` service parameter
- Profile-parameter names and values in `extraParams` must use UTF-8 URL encoding so BRouter preserves literal `+` and `%` characters. Text inputs must reject `?`, `&`, and `=` because BRouter splits parameters on those separators after decoding.
- The selector must include a single custom-profile entry; when the user chooses that custom entry from the opened dropdown, the app must open the custom `.brf` picker even if that same custom entry is already the current selection
- On Android 6–9, BRouter profile setup must request the same runtime external-storage read permission used for surrounding streets, without requiring a folder selection. An existing read permission granted for either feature must satisfy both storage status rows and skip the profile setup prompt.
- On Android 10 and later, BRouter profile setup must request persistent Storage Access Framework directory access to `profiles2`. Folder instructions must show the picker target path, including legacy `Android/data` or removable-storage layouts when detected, and allow the user to browse elsewhere if the suggested location is unavailable.
- Setup must wait until the welcome is completed. Granting legacy storage access must refresh external profiles immediately; denial or cancellation must keep bundled profiles and Straight Line mode usable. Returning from settings must refresh profiles to reflect changed storage access.
- Custom-profile selections must open the `.brf` file picker while reusing available directory access and the shared version-agnostic discovery logic for picker startup.
- The one-time directory-access step is additive only: it must not change normal spinner behavior, selected-profile persistence, or the way routing uses the chosen BRouter profile name
- When the custom-profile entry is selected, route calculations must send the selected `.brf` file contents to BRouter through its `remoteProfile` service parameter, so manually chosen files outside BRouter's `profiles2` folder are usable without requiring BRouter to find the file by bare profile name
- When trying to open a custom profile source, the app should probe multiple plausible BRouter `profiles2` locations across both internal and removable storage, and across both `Android/media/...` and legacy `Android/data/...` layouts, instead of assuming a single path from Android version alone
- On Android 11 and later, the app should prefer `Android/media/.../profiles2` for user-granted directory access because SAF tree access to `Android/data/...` is restricted, while legacy `Android/data/...` probing may remain as a non-granted fallback hint only
- If no candidate path can be verified, the picker-startup fallback should prefer the primary internal `Android/media/.../profiles2` location before removable-storage candidates
- Common example paths:
  `/storage/emulated/0/Android/media/btools.routingapp/brouter/profiles2`
  `/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`
  `/storage/<sdcard-uuid>/Android/media/btools.routingapp/brouter/profiles2`
  `/storage/<sdcard-uuid>/Android/data/btools.routingapp/files/brouter/profiles2`

#### 1.2 Navigation mode selector

- The main UI must show a navigation-mode spinner with `Route mode`, `Round Trip mode`, and `Straight Line mode`
- Each row in the opened navigation-mode selector must include an info control that briefly explains the selected mode
- On startup, the main UI must restore the last selected navigation mode; if BRouter is unavailable and the stored mode is BRouter-backed, the UI must show `Straight Line mode` while keeping the BRouter-backed rows visible but disabled
- If BRouter is not installed, `Route mode` and `Round Trip mode` must remain visible in the navigation-mode spinner but be disabled, while `Straight Line mode` remains selectable
- In `Route` mode, the main UI must show the destination input, destination map picker, intermediate stop inputs, red route-direction rail, saved-route controls, plus button, and start navigation button
- In `Round trip` mode, the main UI must hide the destination input, destination map picker, intermediate stop inputs, red route-direction rail, saved-route controls, and plus button
- In `Round trip` mode, the main UI must show an average roundtrip-distance field, a roundtrip-direction field, a small live compass next to that direction field, and the start navigation button
- The average roundtrip-distance field must accept kilometers when metric units are active, and miles when the imperial-units setting is active
- The roundtrip-direction field must accept compass bearing degrees from `0` through `359`, and while Round Trip mode is visible it must be filled live from the app compass orientation
- The small roundtrip-direction compass must rotate live with the compass orientation and keep a visible green or red outer status ring based on the current heading accuracy
- Round trip mode must use the selected BRouter profile and must not be startable without a selected BRouter profile
- In `Straight Line mode`, the main UI must show the same destination, stop, saved-route, and start controls as Route mode, but hide the BRouter profile selector and profile-settings button because BRouter is not used

### 2. Destination input

In `Route` mode, below the route-mode selector, the app must show an input field for searching a destination POI or coordinates.

- The destination field must include an icon-only speech button inside the right edge of the text field
- Pressing the speech button must first try Android's built-in speech recognition UI for dictated destination search text. If no recognizer UI activity is available but an Android speech recognition service is installed and has microphone access, the app must fall back to that service with an in-app listening prompt and request the app's microphone permission only when that fallback needs it.
- A recognized speech result must populate the destination field as editable text and use the normal POI history/search suggestion flow rather than binding coordinates by itself
- If speech recognition is unavailable, cancelled, fails to become ready, or returns no usable text, the current destination field value must remain unchanged and the app should show a short message when appropriate. When no recognizer activity or service is exposed by Android, or when a recognizer-looking service does not become ready after launch, the message must guide the user to enable a real Android voice-recognition provider such as the Google app.

#### 2.1 History dropdown before typing

- Before the user starts typing, a compact dropdown must appear adjacent to the input field without covering it; the dropdown may open above the field when keyboard or screen space makes that clearer
- The dropdown must show previously searched POIs
- History entries must be promoted when a destination or stop is selected or otherwise resolved to valid coordinates for navigation
- Each history row must include an edit control on the right that lets the user rename the stored display label without changing the saved coordinates
- Each history row must include an `X` control on the right to delete that POI from history
- Renaming a history row must preserve that entry's coordinate identity so later selection still resolves to the same saved destination

#### 2.2 Search after 3+ characters

- Typed destination and stop queries must first check the saved history entries from the first typed character and preserve their recency order
- When one or more history entries match the typed query, the dropdown must show those history matches first
- On every typed character, but only once the query length is at least 3, the app must retrieve matching POIs
- Online provider results must appear in the same dropdown after the matching history rows, skipping provider results whose coordinate identity already exists in the shown history rows
- Typed-query dropdowns must append a final `Search in Google Maps` action row after history and online provider rows. That row must show a search icon on the left, open Google Maps app or the Google Maps website with the typed query text, and must not bind a destination or add history by itself.
- Each result must include a concise POI/address label and coordinates. When structured address data is available, the visible label should keep the meaningful place name, street address, and city/locality while omitting lower-value administrative tails such as postcode, county, region/state, and country.
- OpenStreetMap Nominatim POI search must request address details, extra tags, and entrances. Results with extra tags or entrance details must show an info control on the right side of the dropdown row that opens a details UI including the available extra tags, address details, and entrances, followed by a sentence reminding the user to use the map to double-check the intended location. When Nominatim returns entrances for a result, those entrances must also appear as separate selectable dropdown rows in addition to the original result, except when the only returned entrance has the same coordinate identity as the original result.
- The `Search in Google Maps` action row must include an info control explaining that it searches externally and that a chosen Google Maps place can be brought back into ViBRo Navigator by sharing the place and selecting ViBRo Navigator.
- In the Google Play flavor, the data source must be Google Maps REST APIs when Google search is enabled and a valid Google API key is saved in the app settings
- If the Google API key is not defined or Google search is disabled, the app must use OpenStreetMap APIs
- In the F-Droid flavor, POI search must always use OpenStreetMap APIs and must not include Google search code or require a Google API key

#### 2.3 Search results dropdown

- Search results must be shown in a compact dropdown adjacent to the input field without covering it; the dropdown may open above the field when keyboard or screen space makes that clearer
- Suggestion dropdowns may use a reduced height for short one- or two-row lists, but if displayed rows would scroll because of wrapped long labels or addresses, the dropdown must expand only as much as the measured rows need, capped by the same maximum visible height used for larger result sets and constrained by available screen space.
- The user must be able to select a result from the dropdown
- Selecting a result must bind the destination to the coordinates of that POI
- Selecting a stored history entry must be treated as a final selection: the dropdown should close and the app must not immediately reopen search suggestions unless the user edits the text again
- When a saved history POI is currently selected and the user clicks its text field again, the dropdown must reopen as a single saved-history row for that POI with its edit and delete controls; losing focus must dismiss that dropdown. Deleting that selected-history row must remove the POI from history and clear the text field and selected POI.
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
- When a map-selected destination or stop is returned as raw coordinates, the main input field should show the coordinates immediately, then replace the visible display label with a concise reverse-geocoded address when an internet lookup succeeds. The selected internal latitude/longitude must remain the original coordinates returned by the picker.
- The picker must support icon-only controls for confirm, cancel, current location, zoom in, and zoom out
- The picker must support an icon-only POI category control overlaid on the map. Opening the control must show POI categories dynamically discovered from OpenStreetMap/Overpass tags in the current map view, sorted alphabetically, with each row showing the number of discovered items such as `Fuel (15)`. Category rows must be text-only, support a single active category, highlight the active category, and toggle that category off when tapped again. Category discovery should be initiated by opening the POI control rather than by initial map load. Returned POIs must be drawn with one shared POI pin style, and POI names must appear automatically when the map is zoomed in enough.
- When the POI category filter setting is enabled, opening the POI category control must show only the configured and enabled category names and must query Overpass only for selectors derived from those configured names instead of running broad category discovery.
- Map-picker POI requests should be minimized: the category-discovery Overpass response should seed the visible POI marker cache, selected-category rendering should reuse cached markers immediately, and later map movement should query only viewport areas not already covered by cached data for the selected category.
- The picker must not show extra top or bottom banners; controls should remain overlaid directly on the map
- Rotating the device while the picker is open must preserve the currently selected point and keep it visible on the map after recreation

### 3. Intermediate stops and saved routes

Below the destination input, the app must show a centered plus button. An icon-only save-route button must be shown immediately to the left of the plus button, and an icon-only restore-route button must be shown immediately to the right.

The destination, intermediate stops, and start-navigation area must include a non-interactive route-direction rail on the left side. The rail must use the app's red route color, start at the destination field, continue toward the start button/current-position marker, show bullets for the final destination and each intermediate stop, and show a directional arrow pointing from the current position toward the destination. The arrow must stay centered on the rail and keep a fixed distance from the current-position/bottom end of the rail as intermediate stops change rail height in both Route and Straight Line modes. In straight-line guidance, the vertical rail line must be dotted. Intermediate stop inputs must stay in entry order so pressing the plus button adds the next editable field near the plus button. In Route and Straight Line modes, those stop inputs are consumed from the current-position side upward so the bottom visible stop is reached first, then the stops above it, then the final destination.

#### 3.1 Add stop field

- Pressing the plus button must add a new input field for an intermediate POI
- Each intermediate input must have the same behavior and capabilities as the destination field
- Each intermediate input must include the same inline speech button as the destination field, including the service fallback when no recognizer UI activity is available; recognized speech text must populate that stop field as a normal POI search query
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

#### 3.5 Saved routes

- Pressing the save-route button must prompt for a route name, prefilled as `Route <timestamp>` using the current timestamp
- Confirming that prompt must save the currently specified destination and any non-empty, valid intermediate stops
- Saved routes must store resolved coordinates and display names for the destination and intermediate stops so restoring a route makes the form immediately usable for navigation
- Pressing the restore-route button must show a dialog listing all saved routes
- Each saved-route row must support selecting that route, editing its name, and deleting the entry
- Confirming the restore dialog must ask for confirmation before replacing the current route form
- After confirmation, restoring a route must fill the destination and recreate the needed intermediate stop rows in saved order
- Saved routes must be stored in app-managed preferences and included in database export/import

### 4. Start navigation

At the bottom center of the main UI, the app must show a large icon-only circular start navigation button using the same green play symbol as the navigation resume/play button.

Pressing the button must:

- Open a new navigation UI implemented as an Android `Activity`
- Access the current user location
- For BRouter profiles, use the installed BRouter app intent/service integration to calculate a path from the current location to the destination
- For Straight Line mode, skip BRouter route calculation and navigate directly toward the next destination point
- In round trip mode, use the installed BRouter app intent/service integration to calculate a circular route from the current location using the selected BRouter profile; the user enters an intended average roundtrip distance and the app converts that distance to a circle radius for BRouter, while the roundtrip direction sent to BRouter comes from the direction field
- Include any intermediate stops in BRouter route calculations in current-position-side order, and in straight-line mode treat them as ordered direct legs toward the final destination
- A cached last-known location may only be used to accelerate startup when it is recent and accurate enough to represent the current user location; otherwise the first route calculation must wait for a one-shot current fix or a live location update
- The first BRouter route calculation must only use a startup location fix that is recent and has location accuracy of 25 meters or better, whether that fix came from a cached seed, one-shot current-location request, or live location update
- Until the first BRouter route calculation has a route-start-quality fix, startup live-location updates should be requested about once per second so indoor or cold-start acquisition settles as quickly as practical
- While the first BRouter route calculation is still waiting for an accurate startup location, the navigation UI must continue to present the state as waiting for location rather than as an active BRouter route calculation
- Startup GPS status must suppress speed values derived only from inaccurate or same-timestamp provider jumps; when movement evidence is weak, show stationary/unknown speed instead of high jitter-derived speeds
- When a fresher startup fix arrives while the first no-active-route calculation is still running, the app should only queue a replacement BRouter request if the new fix materially changes the route start or meaningfully improves start accuracy; small startup jitter around the cached seed should not force a duplicate route calculation
- When the first BRouter route has already returned with a route-start beeline and a materially stronger startup fix settles before the user reaches the returned route corridor, the app should refresh the startup route from the settled location instead of keeping a stale long beeline

#### 4.1 Missing BRouter handling

- If BRouter is not installed, the main screen must clearly tell the user that BRouter is required for BRouter profiles instead of behaving as if profile files are merely missing
- On first main-screen open without BRouter installed, the app should offer direct install options for the BRouter app page, including Play Store and F-Droid targets when those intents are available
- If no install target can be opened on the device, the app must fail gracefully with a short user-visible message rather than crashing
- When BRouter is not installed and a BRouter-backed profile or custom profile is selected, pressing start navigation must stop before profile resolution and must show a missing-BRouter message instead of opening the custom-profile picker
- When BRouter is not installed and Straight Line mode is selected, pressing start navigation must continue without a missing-BRouter message
- Round trip mode must require a selected BRouter profile; pressing start navigation without an applicable BRouter profile must show a short message and must not start navigation

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
- Round trip route calculations must send BRouter `engineMode=4`, the current location as the only explicit route point, `roundTripDistance` as the calculated circle radius in meters, and `direction` from the roundtrip-direction field; they must omit `roundTripPoints` so BRouter uses its default helper-point count
- The BRouter roundtrip radius must be calculated from the user-entered intended average roundtrip distance as `distance / (2 * pi)`, after converting imperial input to meters when needed
- Round trip route calculations must still send saved non-default profile parameter overrides through `extraParams`
- Bundled internal BRouter profiles must remain usable even when no autodiscovered external profile folder is accessible
- Custom external profile browsing should target a real accessible `profiles2` folder when one can be found, but normal routing must not depend on that folder existing
- The picker-initial-location logic for custom external profiles must use the same version-agnostic multi-path probing strategy as the profile-discovery logic, rather than switching candidate path sets solely by Android version
- External-profile discovery for selector population and picker startup must share the same version-agnostic candidate set and the same internal-versus-removable storage coverage
- A persisted SAF tree grant for `profiles2` must be treated as the highest-priority source for external-profile discovery and picker initial location, ahead of unguided path probing
- A single-file SAF grant obtained from picking one `.brf` file must not be assumed to provide sibling-folder enumeration rights; folder enumeration must rely on a tree grant or on separately accessible autodiscovered paths

#### 4.3.2 Local segment access

- When surrounding-street display is enabled, the app must read nearby street geometry only from BRouter's already downloaded local `segments4` `.rd5` files
- Surrounding-street display must not download additional maps and must not add an external map-rendering or map-parsing library
- The surrounding-street compass overlay must be shown for moving-scale compass viewports with a valid positive radius, including high-speed expanded radii, but not for the full-route overview; drawing should remain clipped to the current compass viewport, while extraction should use a bounded in-memory session cache of local spatial chunks selected from a capped local area around the current position and a lateral route-ahead corridor, without expanding extraction to a full-route or transition-radius overview
- Surrounding-street extraction must decode the BRouter lookup-version-11 `highway`, `railway`, `waterway`, and `route` way-tag values. Any present `railway` or `waterway` value must take precedence and be classified as special routing geometry. `route=ferry`, `ski/piste`, `canoe`, and `bus` must be special routing geometry; `route=hiking/foot`, `bicycle`, and `mtb` must be walking/cycling geometry; these selected route values must take precedence over `highway`. `route=road` must defer to a recognized `highway` and otherwise use the unknown-geometry fallback
- After higher-priority railway, waterway, and selected-route classification, surrounding-street extraction must hard-exclude non-useful inactive `highway` context types before they enter the session cache: `construction`, `proposed`, `planned`, `virtual`, `abandoned`, `disused`, `razed`, `demolished`, `dismantled`, `no`, and `bus_stop`
- The visible surrounding-street overlay must filter cached street segments by the moving compass reference speed with three user-facing buckets: low speed `0-40 km/h`, medium speed `40-80 km/h`, and high speed `80+ km/h`; threshold hysteresis should keep the active bucket stable when speed jitters near `40 km/h` or `80 km/h`
- Low speed must keep every cacheable surrounding-street category. Medium speed must hide only walking/cycling geometry. High speed must keep only major/highway and special routing geometry. Special routing geometry must therefore remain visible in all three speed buckets
- The major/highway category must contain `motorway`, `motorway_link`, `trunk`, `trunk_link`, `primary`, `primary_link`, `secondary`, `secondary_link`, `rest_area`, and `services`. The normal-street category must contain `tertiary`, `tertiary_link`, `unclassified`, `residential`, `service`, and `road/yes`. The walking/cycling category must contain `living_street`, `track`, `pedestrian`, `footway`, `path`, `cycleway`, `steps`, `platform`, `corridor`, `elevator`, plus `route=hiking/foot`, `bicycle`, and `mtb`
- The speed-independent special-routing category must contain every railway and waterway value, `route=ferry`, `ski/piste`, `canoe`, and `bus`, `highway=busway`, `bridleway`, `raceway`, and `via_ferrata`, and routing geometry without a recognized base type
- A settings button immediately left of the Surrounding streets master switch must open a scrollable type selector grouped by the four overlay colors. Its distinct category headings are “Major roads”, “Normal streets”, “Walking / Cycling”, and “Special paths”, each with a theme-aware color swatch after the name and an independent on/off master. Each selectable type must show a plain-language, translatable name from string resources and a nearby info button opening a short description that includes its original OSM/BRouter tag value or fallback meaning. The OSM/BRouter tag must follow a blank line after the plain-language description. Opening help must preserve the unsaved selector state. In the walking/cycling and special groups, `route=*` entries must precede `highway=*` entries; the special group's unknown-geometry fallback must be last. Each supported `highway` type and each selected `route` value has its own switch. All `railway=*` values share one railway switch and all `waterway=*` values share a separate waterway switch. Inactive highway values hard-excluded during extraction must not be offered. Fresh installs enable every category and type. Turning off a category hides its members without erasing their individual selections; restoring it restores those selections. The saved selection must filter geometry before spatial sampling and before it enters the bounded session overlay cache, so disabled types do not occupy its segment or point budget. Changing the selection must invalidate those filtered chunks, cancel/reject old in-flight results, and reload from local rd5 data or reusable complete decoded cells. The speed-bucket filter remains display-side, without altering routing, extraction limits, or the master Surrounding streets setting.
- Major/highway streets must use blue, normal streets purple, walking/cycling streets green, and special routing geometry gray. Dark-theme colors must be `#CC4EA5FF`, `#CCCE93D8`, `#CC53D78C`, and `#CCBDBDBD` respectively; light-theme colors must use the same hue families with stronger contrast and full opacity as `#FF0069C2`, `#FF7B1FA2`, `#FF087A3C`, and `#FF616161`
- The Surrounding streets setting information dialog must include theme-aware colored swatches using the actual overlay colors: blue for major roads and highways, purple for normal streets, green for walking/cycling streets and routes, and gray for railways, waterways, and special routing geometry; the legend must show the swatches instead of color-name text
- `segments4` discovery must use the same version-agnostic storage probing strategy as external `profiles2` discovery, including primary and removable storage and both `Android/media/.../brouter/segments4` and legacy `Android/data/.../brouter/segments4` layouts
- On Android versions where BRouter's legacy external `Android/data/.../segments4` files are readable only with runtime external-storage access, enabling surrounding-street display must request that storage access before persisting the setting
- If surrounding-street display is already enabled but legacy external-storage access is missing when navigation starts, startup must request that access before launching navigation; if denied, navigation must continue without the street overlay and leave the setting disabled
- Missing, inaccessible, corrupt, unsupported, or out-of-area BRouter segment files must fail quietly by omitting the surrounding-street overlay rather than blocking navigation or route guidance
- BRouter segment reading for compass context must stay bounded to the current surrounding area, route-ahead corridor, session cache caps, and a fixed maximum number of drawable street segments
- Visible-area chunk selection must remain centered on the current accepted location; route-ahead loading is supplemental prefetch and must not shift that center. Street overlays must remain display-only and must not affect route matching, rerouting, guidance, or GPX export
- Extraction must retain at most 1,000 street segments per spatial chunk and display at most 2,000 segments after filtering and deduplication. When extraction reaches its limit, selection must preserve a spatially distributed sample within the requested chunk, favoring nearer geometry within populated areas; it must consider all intersecting rd5 cells instead of stopping at the first cells in file order. Dense areas may omit some streets under these limits
- The overlay session cache must remain capped at 240 chunks, 40,000 segments, and 160,000 points. Retention must prioritize nearby display chunks over prefetched and farther chunks and avoid repeatedly reloading chunks evicted for exceeding the current viewport's cache budget
- Nearby chunk requests should reuse complete decoded rd5 cells through a bounded cache of at most eight cells and 4 MiB of packed coordinate payload, separate from the overlay session cache. Reuse must distinguish storage sources and map revisions; corrupt or partially decoded cells must not enter the cache. A cell too large to cache must still supply the current query's full eligible geometry to the bounded sampler
- Missing visible chunks must load before route-ahead prefetch, in background batches of at most 16 chunks. Initial prefetch may run immediately; subsequent prefetch requires both 1 km of movement and 60 seconds since the previous prefetch request, or 2 km of movement regardless of elapsed time. Route-ahead corridor planning should run only when prefetch is due
- Closing or disabling the compass street viewport, resetting the overlay, or shutting down navigation must cancel outstanding extraction cooperatively and reject stale results. Reopening the viewport must allow missing visible chunks to load again
- Unchanged chunk selection, cached contents, street filter, and segment limit should reuse the same immutable display overlay. Heading-only redraws must reuse the three prepared category paths, rotate them with the canvas, and draw each non-empty category path once; changes to position, geometry, radius, or drawing scale must rebuild them. Clipping must preserve segments crossing the viewport even when both endpoints are outside it

#### 4.3.4 GeoJSON output

- The app must request BRouter GeoJSON output using the Android-service parameters that produce a GeoJSON `FeatureCollection`
- The app must request BRouter native turn-instruction mode `9` so GeoJSON `voicehints` preserve distinct exit-left, exit-right, and beeline commands
- When BRouter includes per-track GeoJSON `times`, the app must parse and retain them as route timing metadata that can be reused for maneuver-time estimation when live speed is not yet trustworthy or not yet available
- When BRouter includes GeoJSON `messages` rows with a `maxspeed` value in `WayTags`, the app should parse those rows as route speed-limit sections and display the current section's speed limit during active navigation
- When BRouter snaps the requested start to a routable network point outside the current off-track threshold, the app should keep BRouter's original route geometry for route matching and treat the snapped route start as a beeline approach target using command `16`; while that approach target is active, off-track rerouting must remain suppressed so the user may reach the original route corridor by any path, stationary orientation advice should point toward the beeline approach target, and normal route-following guidance should begin only after the user is inside the original route threshold
- In Route mode, route-start approaches and native/synthetic command-16 intermediate/destination legs may attempt speculative recovery only after at least 20 m distance growth beyond the closest and current fixes' accuracy allowances, sustained for 10 seconds with moving fixes of at most 50 m accuracy. An actual submitted attempt starts a 30-second retry cooldown; retries additionally require 20 m movement beyond the previous attempt's and latest fix's accuracy allowances. Stationarity, pauses, target changes and sample gaps over 10 seconds clear divergence evidence. Straight-line and Round Trip modes retain their no-reroute behaviour.
- In Route, Round Trip, and Straight Line modes, every active route-start, intermediate, final, or straight-line beeline must establish a direct-distance baseline for its current target and recheck that distance every 10 seconds. When a check finds that the direct distance exceeds the distance in the last beeline notification, or the leg's initial baseline when no repeat has yet been sent, the app must resend the normal command-16 beeline notification with the current distance and time estimate. A new target or pause/resume transition must establish a new baseline. Straight-line polling must be capped at 10 seconds while a direct target is active so long dynamic intervals cannot skip these checks. This notification policy must not disable or replace Route mode's simultaneous speculative recovery evaluation for route-start, intermediate, and final beelines.
- During speculative beeline recovery, active guidance, compass rendering and fix-by-fix GPX history must continue without a calculating screen or an offroute/error notification. Failure or an unsuitable result leaves the current beeline intact. Apply a candidate only if the same beeline context and request plan remain active, the latest evaluated fix is no more than 10 seconds old with usable accuracy, and it matches road geometry within the normal route tolerance before the first remaining stop. Reject a candidate requiring another initial approach from its requested start, consisting only of a beeline, omitting/reordering remaining stops, or changing the exact final destination. A usable road route may retain necessary final/intermediate beelines. Pauses, target completion/change, superseding requests and plan changes must prevent late results from replacing current guidance; rejected alternatives must never enter GPX history.
- When BRouter snaps an intermediate stop or final destination away from the requested coordinates, the app should append synthetic command `16` beeline geometry so guidance reaches the requested point. Intermediate stops must be represented as a two-leg spur from the route to the requested stop and back to the route before continuing; the final destination must be represented as a one-way beeline from BRouter's snapped endpoint to the requested final destination. Native BRouter and synthetic command `16` legs must behave like the route-start beeline: guide directly toward the current beeline target, allow the user to take any path without off-track or wrong-direction rerouting, and resume normal route matching only after the target is reached.

#### 4.4 Navigation update loop

The app must monitor user position:

- Every 3 seconds while startup route lock is still stabilizing, for at most the first 60 seconds after navigation starts
- Startup fast polling may end earlier once the app has gathered 5 consecutive accurate on-route updates after a route is active
- Those 5 stable updates must represent separate fast-polling intervals; clustered provider callbacks that arrive before the 3-second fast interval has elapsed must not end fast polling early
- An accurate warmup update means an on-route evaluation with location accuracy of 25 meters or better
- After a new route is applied, including after an off-track recalculation, the app must immediately re-enter 3-second location polling before returning to dynamic intervals after stable on-route fixes
- After startup fast polling has ended, an unexpected long gap between accepted location evaluations, beyond the currently requested active interval plus reasonable scheduling slack, must temporarily resume 3-second checks so the app can restabilize position accuracy before continuing with long dynamic intervals; a normal callback at a requested long dynamic bucket must not be treated as reacquisition
- The first accepted fix after such a long gap must be treated as location reacquisition: reset stale Kalman velocity and motion/progress evidence, use trusted on-route matches to catch up route/turn state, but suppress immediate off-route or wrong-direction reroutes until follow-up samples confirm the deviation
- Later at a dynamic interval derived from the estimated time to the next direction, using the current speed, bounded recent acceleration/deceleration, and remaining route distance when the next maneuver still lies on the current matched route segment and live speed is available, or route timing metadata when the next maneuver lies beyond the current matched route segment
- When the next direction is estimated to be 8 seconds away or less, the dynamic interval must be 3 seconds
- Otherwise the dynamic interval should scale to roughly one quarter of the estimated time remaining to the next direction
- When the next maneuver or arrival is estimated within about 3 minutes, the dynamic interval should be capped at 20 seconds so speed changes cannot leave the app waiting through a long quiet window near guidance-critical points
- After a maneuver instruction has just been passed, the dynamic interval must ramp upward through the fixed bucket set one accepted route evaluation at a time instead of jumping directly from the minimum interval to the larger interval for the following direction
- During that post-maneuver ramp, an accepted provider callback that arrives before the current ramp bucket has elapsed must keep the current bucket instead of advancing to the next bucket early
- The post-warmup dynamic interval must be snapped to a small fixed bucket set instead of continuously varying on every update
- The bucket set must currently be `3s`, `5s`, `8s`, `12s`, `20s`, `30s`, and `60s`
- When the dynamic GPS fix interval setting is enabled, the dynamic interval must never be lower than 3 seconds
- The dynamic interval must never exceed 60 seconds
- When the dynamic GPS fix interval setting is disabled, active navigation must request ongoing location fixes every 1 second instead of using the dynamic interval buckets
- Re-requesting location updates must reuse the active listener registration when the requested interval bucket and enabled provider set are unchanged, so the app does not continuously tear down and rebuild subscriptions
- In the Google Play flavor, when fused location is enabled and available, Google fused location must be the only continuous primary location subscription; legacy platform GPS/network listeners must remain dormant to avoid duplicate independently phased streams and unnecessary battery use
- If a fused update request fails synchronously or asynchronously, or if the wake-capable stale-location check finds that fused callbacks stopped, the app must activate the legacy GPS/network path at the same requested interval and keep that reliable fallback active for the remainder of the navigation session
- During provider transitions, a callback from a different provider that arrives inside the current requested interval must not create an extra accepted fix unless it materially improves location accuracy
- One-shot current-location fix acquisition must be skipped while the screen is off; screen-off navigation should rely on the foreground service's ongoing location updates instead
- While foreground navigation is active, the app must schedule a wake-capable stale-location check beyond the requested interval plus scheduling slack. If no callback arrives by that check, it must activate the legacy fallback when fused was primary or restart the active legacy subscriptions at the same interval, without introducing a session-lifetime wake lock; accepted fixes and explicit pause/stop lifecycle events must reschedule or cancel that recovery check
- The Google Play flavor may use Google fused location when Google Play Services is available and the user has enabled the fused-location setting
- When fused location is unavailable or disabled, the app must fall back to the legacy platform GPS/network provider path
- The F-Droid flavor must use the legacy platform GPS/network provider path and must not include Google fused-location code or Play Services dependencies
- Position handling must use a Kalman filter
- Position corrections must not create a travel velocity. Prediction may use a trusted measured GPS speed and course; stops, trusted course changes, and moving or stopping fixes separated by at least 15 seconds must discard stale prediction. Scheduled 60-second fixes must remain supported without forcing faster acquisition or treating the expected interval as loss of location.
- Sparse observations with zero endpoint speeds must still allow movement and a stop between fixes. For intervals of at least 15 seconds, jump detection must use a conservative travel allowance rather than extrapolating endpoint speeds; re-anchor when displacement exceeds the sum of the position accuracy radii. Preserve stationary jitter smoothing within that uncertainty. Modest position errors across a sparse gap cannot reliably be distinguished from genuine intervening movement using two fixes alone.
- Initialize position covariance from the incoming horizontal accuracy. Reject invalid coordinates/accuracy, older fixes, repeated observations at the same fix time, and isolated jumps inconsistent with elapsed time, reported speed, and positional uncertainty. A more accurate observation at the same time may replace the earlier one after the jump check; a second coherent newer observation must permit recovery from a real relocation. Longitude smoothing must use the short difference across the date line and remain finite at the poles.
- Limit smoothing displacement to the incoming accuracy radius. The displayed position accuracy must conservatively include the distance between the raw and filtered coordinates; preserve the original provider accuracy separately for diagnostics and GPX export. Unknown accuracy must remain unknown.
- Any asynchronous route calculation must apply its resulting shared navigation state in a single serialized path so stale background results cannot overwrite newer navigation state
- The navigation session must support an explicit paused mode that preserves the current request and loaded route while temporarily suspending live guidance processing
- While paused, the app must stop live location/orientation-driven navigation updates, suppress turn and reroute handling, and resume from the same session state when the user continues navigation
- In straight-line mode, the navigation session must not request BRouter routes or display turn directions
- In straight-line mode, the navigation UI must not draw a BRouter route polyline or turn-by-turn route corridor; it must show the dotted direct beeline to the next destination/intermediate target point, the destination/intermediate target points, and the compass target/arc for the next destination point
- In straight-line mode, the compass must also show the passed beeline path connecting accepted GPS fixes from the current navigation session as red dotted segments using the same passed-route color used for passed route geometry in route mode
- In straight-line mode with intermediate stops, the next compass target must be the next unreached intermediate stop; after each stop is reached, the compass target must advance to the following stop or final destination
- In straight-line mode, entering the destination-reached radius around an intermediate stop or the final destination must emit the same intermediate/final arrival guidance notifications as routed navigation
- In straight-line mode, the progress line for the next stop must use the direct distance from the current location to that stop
- In straight-line mode, the progress line for the final destination must sum the direct remaining legs through all unreached intermediate stops, such as current location to next stop plus stop to stop plus last stop to final destination, instead of using a single shortcut from the current location to the final destination
- In straight-line mode, displayed arrival times must use the same straight-line remaining distances and the current speed; if current speed is unavailable or stationary, the ETA must be unavailable
- In round trip mode, the route end is allowed to be near the route start; the navigation session must not emit final-arrival guidance at route load or while matched near the start of the loop unless progress is also near the end of the loaded route
- In round trip mode, the compass must keep showing the next real BRouter maneuver cue whenever that maneuver is still ahead, even before the maneuver reaches notification timing thresholds; synthetic arrival hints must not replace the maneuver cue
- In round trip mode, the blocked-road action must be disabled because blocked-road no-go recalculation is not applicable to a circular route request

#### 4.4.1 Off-track reroute

- In normal Route mode, the route must be recalculated whenever the user position differs from the current track by more than an off-track threshold derived from recent location confidence
- Off-track distance must be measured against the nearest matched point on the active route geometry
- The off-track threshold should use a short-window smoothed location-accuracy estimate rather than a single raw fix accuracy so one bad GPS sample does not immediately widen the tolerance
- The off-track threshold must currently be `max(smoothedAccuracy + 8 meters, 10 meters)`
- Off-track and bearing-mismatch reroutes must require two time-separated deviation samples at every travel speed so a single GPS spike or clustered provider burst cannot cause an unnecessary reroute
- Off-track confirmation must additionally distinguish independent movement from repeated uncertain positions: retain fast confirmation when displacement exceeds the current accuracy (and at least 4 m) and separation beyond the route corridor exceeds that accuracy; otherwise require 6 seconds of sustained off-track evidence. Returning to the corridor or a gap over 10 seconds clears the off-track movement evidence. Tentative projections must not advance remembered route segments or travelled history.
- After the first suspected deviation, the app must temporarily request 1-second location updates until an independently timed follow-up fix either confirms the reroute or clears the deviation; callbacks less than 750 milliseconds apart must not count as separate confirmation samples
- In normal Route mode, the app may start a speculative BRouter recalculation after the first suspected off-track sample, but the active route must remain unchanged until the independently timed follow-up fix confirms the off-track reroute. If the speculative route result is already available when confirmation arrives, it should be applied immediately.
- When a route recalculation starts after one or more intermediate stops have been reached, the recalculation must pass only the remaining unreached intermediate stops to BRouter; passed stops must not be reintroduced into the new route request, compass targets, or intermediate-stop progress state
- In Round Trip mode, confirmed off-track detection must send an off-route notification without requesting a route recalculation
- Startup and route-unavailable calculations in Round Trip mode must keep the active roundtrip mode, selected profile, profile parameter overrides, and calculated BRouter radius captured when navigation started

#### 4.4.2 Wrong-direction reroute

- The route must also be recalculated when the user is still on the track but is moving in the wrong direction
- Wrong direction requires a trusted bearing difference greater than 60 degrees combined with backward along-route progress
- Bearing-based wrong-direction detection must only be trusted when the current fix is accurate enough and the heading source is credible for the current speed and displacement
- When numeric GPS bearing accuracy is available, the app should trust GPS bearing for wrong-direction evidence only when the reported bearing accuracy is good enough for walking and cycling use cases and the user is moving at least 0.8 m/s; low-speed walking use must remain supported and must not be excluded by a cycling-only speed gate
- When numeric GPS bearing accuracy is not available, the app should trust GPS bearing for wrong-direction evidence only at course-style speeds of at least 2.5 m/s
- When GPS bearing is not trustworthy enough, wrong-direction detection should fall back to a movement-derived course computed from recent filtered route progress rather than from a single noisy fix pair
- Low-confidence bearing estimates must not trigger reroutes on their own
- The expected route bearing for wrong-direction checks should be forward-looking, derived from a short lookahead along the matched route geometry rather than only from the single currently matched segment
- Bearing mismatch alone should not be enough to reroute while the user is still making clear forward progress along the route
- Wrong-direction reroutes require two time-separated trusted bearing mismatches, each supported by `BACKWARD` along-route progress. `STALLED` and `UNKNOWN` progress must hold the current route and clear pending bearing-mismatch evidence; clear `FORWARD` progress suppresses the mismatch. Held mismatches must not trigger deviation-confirmation polling or speculative recalculation. Distance-based off-track detection and confirmation remain independent of this direction gate.

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
- A next-direction distance less than or equal to the current filtered-position accuracy radius must remain visible with its entire instruction line colored in the same orange as the GPS accuracy circle/value. Do not add uncertainty text or promote the following maneuver because of accuracy. Apply the same styling to the following line when that maneuver's absolute distance from the current position is inside the radius; missing accuracy also marks the instruction uncertain. Phone portrait/landscape and Android Auto must share these confidence flags and theme-aware normal colors.
- Accuracy changes must only change the instruction color: recovering accuracy restores normal color without advancing guidance or replaying alerts. Keep the current maneuver visible even inside the alert's actionable-distance floor, until trusted route progress reaches/passes it. A previously alerted maneuver may retire at its route point, but must not retire early solely because its ETA falls below 2 seconds. Hold guidance progression and unsent alerts while route progress is uncertain, including location reacquisition; confidence recovery emits only the currently most urgent due alert.

- A native beeline start marker at or behind the current route progress must not duplicate the active direct-guidance instruction in the following-instruction slot; upcoming beeline markers remain visible until their start point is reached.

#### 4.4.4 Turn notifications

- The app must send notifications:
  - When navigation starts and the first route has been calculated, for the first upcoming direction even if the user is not moving yet
- When the user has remained stationary for several seconds during navigation, recent filtered fixes show only negligible displacement, and the app has a sufficiently trustworthy live heading sample from the preferred heading sensor path that shows the user is not already facing the route
  - When the previous direction has just been passed, only if advancing guidance requires surfacing a new actionable upcoming instruction rather than replaying the just-passed maneuver; if route-matched progress is stable, the app may surface that next actionable instruction immediately
  - When 20 seconds remain to the next direction, if the next maneuver is actionable and route progress is trustworthy
  - When 5 seconds remain to the next direction, if the next maneuver is actionable and route progress is trustworthy
- When Single instruction mode is enabled, the app must suppress the route-start, 20-second, and 5-second approaching-maneuver notifications and instead emit only one approaching-maneuver notification when 10 seconds remain, if the next maneuver is actionable and route progress is trustworthy
- The app must suppress or delay turn notifications when the user's route progress is not trustworthy enough to identify the next actionable maneuver
- When the 5-second imminent notification is emitted for a real BRouter voice hint, the visible navigation compass must show that hint's signed maneuver angle using the same red partial arc and target marker as the stationary orientation cue, resolved from the incoming route bearing into an absolute target heading so the marker and arc rotate with subsequent compass movement, and must hide that cue once the hint is passed
- For the initial startup notification, remaining maneuver distance must be trustworthy relative to current location accuracy before the notification is emitted
- In-route imminent maneuver notifications may rely on stable route-matched progress even when the visible instruction is orange because of coarse position accuracy. The notification and speech must use that same current maneuver; an orange display warning alone does not suppress useful 10-second or 5-second alerts. Unstable progress must delay both notifications and speech without consuming their pending flags. Intermediate and final arrival instructions retain their separate reached-radius policy; a confirmed reached instruction uses the normal text color.
- In-route imminent maneuver notifications must still be suppressed when the remaining maneuver distance is already too small to be actionable or when route matching is unstable
- For slow walking or hiking speeds, the in-route actionable-distance floor for the 5-second imminent notification may shrink below the normal 5-meter floor when recent along-route progress provides a trustworthy ETA, so the 5-second notification and matching compass target are still reachable before the maneuver is passed
- The app must not emit a passed-turn notification whose displayed remaining distance or time would collapse to zero; in that case it should suppress the passed maneuver and move on to the next actionable instruction
- When the user is already inside the most urgent threshold, the app should emit only the single most urgent imminent-turn notification instead of stacking multiple near-identical alerts
- Synthetic intermediate-arrival instructions must participate in normal approaching-turn notification timing before the intermediate stop is reached, ordered by their along-route position relative to real route voice hints
- When the user's current filtered position enters the destination-reached radius around an intermediate stop, the app must emit an intermediate-destination-reached guidance notification with the mapped arrival symbol, then continue guidance toward the following stop or final destination
- When the user's current filtered position enters the destination-reached radius around the final destination point, the app must emit a destination-reached guidance notification instead of silently ending maneuver alerts
- That destination-reached radius must be based on the final destination point and use the same trusted-accuracy threshold policy as red route deviation display, currently `max(smoothedAccuracy + 8 meters, 10 meters)`; the compass should draw the visible destination/stop radius as that threshold minus the current trusted accuracy radius, so overlap with the current-position accuracy circle matches the arrival check
- Turn notifications must reuse a single notification entry in the notification list so older direction notifications do not pile up
- The current transient navigation instruction notification should be removed automatically after 5 seconds, unless a newer transient instruction has already replaced it
- Replacing a direction notification in the notification list must still be compatible with smart bands or similar devices that mirror notifications as they arrive
- A stationary orientation notification must be emitted only after a short stationary dwell, must require both low recent movement speed and negligible recent filtered displacement, must require a fresh heading sample from the preferred heading sensor path with good coarse calibration when that concept exists for the selected sensor, must treat a fresh deprecated-orientation-sensor low or unreliable accuracy status as a calibration veto when that sensor is available, must treat fresh medium deprecated-orientation-sensor accuracy as added uncertainty, and when the selected sensor exposes a per-sample heading accuracy estimate it must suppress the notification unless the required turn still clearly exceeds that uncertainty margin
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
- While the dedicated navigation UI is visible, Android should be allowed to show that navigation screen above the keyguard and turn the screen on when the activity resumes, so pressing the power button off and on during active on-screen guidance returns to navigation instead of requiring an immediate PIN entry. The app must not silently dismiss a secure lock screen or unlock the device.
- On phone-sized screens in landscape orientation, the navigation UI must switch to a two-column layout
- In that landscape layout, the left column must contain all navigation text content and both action buttons
- In that landscape layout, the right column must contain only the compass route view plus the overlaid GPX export and settings controls
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
- While a route-start or command `16` beeline is active, the primary direction line must show the live beeline instruction and the secondary line must show the next routed instruction when one is available
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
- Switching between compact compass and fullscreen route views must preserve the text size of the GPS status and upcoming-direction lines with the same content. Portrait GPS and direction panels must use the same padding as the arrival-statistics panel and reserve that inset in both modes so fullscreen backgrounds cannot change the available text bounds.
- Tapping either of the two direction lines, or the surrounding direction block, must open a compact details UI styled like the GPS details UI
- The directions details UI must list the remaining upcoming directions using live distance and time values from the current navigation state; the first row must be relative to the user's current matched route position, and each following row must be relative to the previous direction in the list
- The app should build the full directions details list only while that details UI is open so long routes do not add work to normal navigation rendering
- The directions details UI must refresh while it remains open and must scroll when the remaining instruction list is longer than the visible panel

#### 4.5.2 Compass route view

- Full navigation snapshots and heading-only refreshes must use the same monotonic time domain for GPS freshness, stationary heading confirmation and compass transitions. Calendar-clock corrections must not change those policies; displayed arrival times must continue to use wall-clock time.

- In the center: a map-free compass canvas showing the active route relative to the current position
- The compass must not render a map background
- The route must rotate live with the latest trusted display heading so forward stays at the top of the view
- Blocked-road no-go points must be shown on the compass as darker transparent red filled circles centered on each blocked point, using each point's configured no-go radius at the active compass scale
- The compass must draw the destination-reached radius around the destination marker using the same transparent red treatment as the route-threshold overlay
- While a route-start beeline approach target is active, the compass should show a live target marker and dotted red bearing line from the current position toward the snapped original route start, without drawing that approach as part of the off-track route corridor
- Activate an outgoing native or synthetic beeline in the same accepted location evaluation that finishes the route-start approach. On a normal road approach, entering the beeline start's reached radius may activate it when matched progress is at most that radius before the start vertex; a nearby future loop with much earlier progress must not activate early. Do not expose an intervening road-turn cue or initial road-turn notification before the outgoing beeline target is selected.
- During an active route-start, native BRouter, or synthetic command `16` beeline, the red direction cue must continuously point from the current position to the active beeline target, including intermediate destinations and return legs to the road. This target bearing takes precedence over road-maneuver and stationary-orientation cues, independently of the selected display heading or motion state. On reaching or skipping a target, update to the next active target immediately; when beeline guidance ends, restore the normal road-cue rules without reusing a stale maneuver cue. Phone and Android Auto must share this behavior.
- Outside an active beeline, suppress road-maneuver and stationary-orientation cues as soon as the current position is outside the route corridor (`max(smoothedAccuracy + 8 meters, 10 meters)`), including while deviation confirmation or speculative rerouting is pending. Clear cached maneuver display cues while outside the corridor; returning to it may restore a still-pending road cue. Keep the active route visible until a replacement is confirmed and applied.
- Native BRouter and synthetic command `16` route legs must use the same dotted red styling as the route-start beeline and must not receive the wider route-threshold overlay; while one is active, its dotted bearing line and target marker must follow the target directly from the user's live position
- While the user is moving in Route or Round Trip mode outside an active beeline, the displayed compass heading normally follows the current matched route direction. Display-only smoothing may use a window up to 15 meters behind and 35 meters ahead to suppress small roadside shifts. Disable this wider smoothing when a non-continue maneuver point falls inside that window, including at the maneuver itself. Accept the overall window direction only for windows at least 20 meters long whose first and last 3-meter baselines remain within 20 degrees of that direction and whose interior points stay within a 6-meter lateral corridor. When wider smoothing is disabled or rejected, use the original 20-meter forward-look bearing and its segment-bearing fallback; never clip that forward lookahead at a maneuver point. Preserve gradual rotation approaching and traversing turns and curves instead of holding the incoming heading until the maneuver and abruptly switching to the outgoing heading. Leave guidance/matching/export geometry unchanged, and expose no sensor accuracy cone while route geometry is selected.
- Routed moving display may temporarily use location-derived heading after disagreement greater than 120 degrees, and greater than its numeric heading uncertainty plus 90 degrees, is confirmed by two distinct fixes at least 750 milliseconds apart with headings agreeing within 30 degrees. Location fixes must be fresh within 10 seconds; repeated heading-only refreshes must not count as new evidence. Poor GPS bearing accuracy above 25 and up to 90 degrees additionally requires displacement beyond both 3 meters and the sum of the confirmation fixes' position-accuracy radii, with displacement direction within 30 degrees of the selected heading. Retain the location source through intermediate disagreement and return to route heading only after repeated disagreement below 60 degrees with uncertainty plus disagreement below 90 degrees, using the same confirmation rules. Preserve the selected location source's accuracy estimate. Unknown/invalid bearing accuracy must not enable this exception. Stationarity, pause, beeline transitions, route replacement, motion reacquisition/reset, or gaps between evidence fixes over 90 seconds clear disagreement confirmation. Stale fixes display route heading without counting toward confirmation; the evidence window accommodates ordinary sparse acquisitions and must not request extra fixes.
- During Straight Line mode or an active route-start, native BRouter, or synthetic command `16` beeline, GPS display bearing requires known finite numeric accuracy from 0 through 25 degrees and speed of at least 0.35 m/s; unknown bearing accuracy must fall back to the compass even at higher speeds. All display-heading rules, including the routed disagreement exception, must preserve the separate wrong-direction reroute gates and normal location cadence.
- Once stationarity is established, ambiguous accepted fixes must preserve it across normal long acquisition intervals so small noisy speeds do not reset stationary compass tracking. Retain a stop reference independently of the 3-second motion history, replacing it only with a more precise fix while movement remains unconfirmed. Resume movement when a later fix with finite position accuracy of at most 25 meters is displaced beyond both 1.5 meters and the sum of its and the reference's accuracy radii, or when finite reported speed exceeds 0.35 m/s and speed minus twice its finite nonnegative speed uncertainty exceeds zero. The uncertainty bound must confirm motion rather than require the travel-speed threshold a second time, so reliable slow departures restore fresh GPS heading without waiting for several meters of displacement. Position accuracy and bearing accuracy must not substitute for speed accuracy. An initial fix with position accuracy worse than 25 meters or unknown accuracy and unconfirmed speed at most 2.5 m/s must allow startup compass tracking. Stationary fixes must report zero resolved speed for navigation and display. Duplicate/older samples must not change the stationarity latch, and session or position-reacquisition resets must clear it. This policy must not request additional location updates.
- During beeline guidance, when GPS bearing is unavailable or too inaccurate, display course may be derived from a separate 90-second filtered-fix history that retains positions across the maximum 60-second dynamic interval and acquisition slack. Prefer the most recent course whose positional uncertainty permits at most 25 degrees of angular error; require at least 2 seconds, 3 meters, and displacement exceeding the sum of both position accuracy radii. Spans over 15 seconds represent a historical chord, not a reliable current direction, and must be treated as uncertain. Clear this display-only history on entering a confirmed stop and retain the stop fix as the new origin, so the approach cannot yield an incoming-direction chord during the first reversed departure steps. These display estimates must not change the recent-fix course used for rerouting.
- When a moving beeline has no reliable location heading, including when the moving fix is older than 10 seconds, the display must follow a fresh usable live compass with known finite accuracy from 0 through 25 degrees. If the current matched route direction is unavailable outside beeline guidance, use that same compass fallback rather than GPS bearing. If neither the applicable travel heading nor a usable live compass is available, retain the last travel heading, or the last displayed heading before any travel heading exists, with conservative 90-degree uncertainty. A fresh reliable heading restores normal confidence. Missing, inaccurate, or stale display heading must not shorten the location acquisition interval; preserve the normal navigation cadence and its existing maneuver, deviation, and position-recovery rules.
- Live heading-sensor input must be compensated for the current screen rotation so portrait and landscape show the same real-world forward direction at the top of the view instead of drifting by 90 or 180 degrees. Route geometry and location-derived travel bearings remain geographic headings.
- Startup before movement, including while waiting for location, must use a fresh usable live compass immediately rather than a fixed north-up orientation. Starting movement switches to the route/beeline heading selection described above.
- Entering stationarity after movement must retain the last displayed heading and establish a fresh compass reference for that stop; a compass/course mismatch alone must not rotate the view. Activate stationary compass tracking only after a turn of at least 30 degrees relative to that reference, increased to twice the numeric compass uncertainty when larger, remains within 10 degrees for at least 250 milliseconds. Once activated, track every usable live compass sample continuously, including small turns, until movement resumes. Compass accuracy must be available and at most 25 degrees. Missing/unusable samples before activation reset the reference, and sample gaps over 1.5 seconds reset turn confirmation. After activation, unusable samples retain orientation with reduced confidence; usable samples resume tracking without another turn check. Resuming movement restores moving route/beeline heading selection and resets the activation gate for the next stop. Session reset clears heading and activation memory.
- After a route-start, native BRouter, or synthetic command-16 beeline ends, keep the moving location-heading selection until a later accepted, stable on-route fix confirms FORWARD direction using the existing along-track progress tracker. BACKWARD, STALLED, UNKNOWN, stationary and tentative off-route evaluations must not complete the handoff. The completion fix must not count incoming beeline displacement as forward road progress. Continue to use the same GPS accuracy/freshness, movement-derived course, live compass fallback and held-heading uncertainty rules while waiting; stationary compass tracking retains its existing activation behavior. Heading-only refreshes and elapsed animation time cannot complete the handoff. Remove the automatic one-second rotation on reaching the road; once forward progress is confirmed, restore normal route-heading selection. Route replacement clears an old pending road handoff, but replacing an active beeline with road recovery must retain location heading until forward movement is confirmed. Pause/motion resets clear progress evidence while preserving a pending handoff, and session reset clears it. Share this policy between phone and Android Auto without changing direction cues, reroute detection or location cadence.
- Whenever the selected displayed heading source changes between matched route geometry, location-derived course and live compass, rotate from the last displayed heading to the new heading over 500 milliseconds using the shortest arc with smooth acceleration/deceleration. Include confirmed routed reverse-heading fallback, recovery to route heading, beeline/straight-line GPS-to-compass fallback and recovery, and stationary compass activation/movement resumption. Carry the actual selected source explicitly through full snapshots, heading-only refreshes and viewport/cache copies; held headings retain their original source. A beeline/road guidance-mode change without a source change must not start rotation. Live updates within one source remain immediate outside the transition; target updates during rotation must not restart its duration or reverse the chosen arc at the opposite bearing. A further source change mid-transition must start from the current displayed heading. First/unknown heading, pause or missing geometry cancels rotation; delayed frames settle it. Use existing phone and Android Auto animation-frame scheduling without requesting additional GPS/sensor fixes, and preserve guidance, geometry and selected-source accuracy.
- Full navigation-state assembly and heading-only refreshes must use the same heading-source selection for phone and Android Auto, including transitions into and out of beeline guidance.
- The preferred heading sensor path must use the standard rotation vector when the platform exposes it and may fall back to the geomagnetic rotation vector when that is the only available fused heading sensor
- When the deprecated orientation sensor is available, navigation may register it only as a calibration cross-check. A fresh medium, low, or unreliable deprecated-orientation accuracy status should conservatively downgrade the displayed live heading-sensor accuracy, but its azimuth must not replace the rotation-vector or geomagnetic-rotation-vector heading.
- Live heading-sensor-driven compass rotation is only required while the navigation UI is visible and the screen is interactive
- The compass outer ring must carry the rotating cardinal labels `N`, `O`, `S`, and `W`
- The compass outer status/calibration layer should stay slim so route and surrounding-street drawing remain dominant, with cardinal labels sized to fit inside that layer
- When Distance circles is enabled, the compact compass must show two stable visual distance references: the existing outer distance ring and one inner ring at half the outer radius and represented distance.
- When the user is stationary and Zoom out when stationary is enabled in settings, the compass should zoom out to fit the full active route overview inside the compass
- When Zoom out when stationary is disabled and a previous reliable moving-scale compass radius exists, becoming stationary should keep that moving-scale route view instead of automatically zooming out to the full route
- When the user is moving and the current native speed reading is reliable, the compass should zoom to a forward-looking moving-scale radius based on the same low, medium, and high speed buckets with hysteresis used by surrounding-street filtering
- Low-speed moving-scale 2D compass zoom must show about 30 seconds of travel at the outer ring and about 15 seconds at the inner ring.
- The moving-scale radius has a 90-meter minimum for visibility. Distance-ring travel times must use the resolved display speed rather than infer speed from that minimum radius and the bucket horizon; at slow walking speeds the outer ring therefore represents more than 30 seconds. Preserve usable speeds below 1 m/s down to the 0.2 m/s timing floor. Apply the same speed to compact/fullscreen, 2D/3D, manual zoom, and zoom-transition labels; projection and zoom may change represented distance but must not imply faster travel. Keep the existing 1 m/s fallback when no usable display speed is available. Ring labels estimate travel at the display speed; instruction/arrival estimates may also use route timing metadata for later segments.
- Medium-speed moving-scale 2D compass zoom must show about 45 seconds of travel at the outer ring and about 22.5 seconds at the inner ring, rounded by the normal time formatter.
- High-speed moving-scale 2D compass zoom must show about 60 seconds of travel at the outer ring and about 30 seconds at the inner ring. Meter values must continue adapting to the resolved speed; reducing the ring count must not change the speed buckets, zoom policy, or full-route overview scale.
- Moving-scale speed buckets must use nominal boundaries of 0-40 km/h, 40-80 km/h, and 80+ km/h, with the same hysteresis as surrounding-street filtering: low changes to medium at 43 km/h, medium changes to low below 36 km/h, medium changes to high at 84 km/h, and high changes to medium below 72 km/h
- The moving compass zoom must use the same resolved trustworthy speed as the GPS status line, so raw native speed alone must not zoom the compass when stationary or noisy movement checks suppress the displayed speed
- When the phone navigation UI opens and Show hint panel is enabled, show a simple theme-aware card with three gesture illustrations and these descriptions: "Double tap or swipe left / right to move from full route, 2D, and 3D views", "Pinch or spread the 2D and 3D view to ±2 zoom levels", and "Swipe up / down with two fingers to tilt the 3D view". Center it over the compact compass, or in the navigation screen when fullscreen route is enabled, in both orientations. Dismiss on the first touch anywhere without consuming that touch, or automatically after five seconds; the footer must show the remaining seconds. Preserve dismissal and the original timeout across rotation, theme recreation, and background/resume; a fresh navigation UI or a new navigation request shows the hints again with a fresh five-second timeout when enabled. Cancel pending countdown callbacks on dismissal or activity destruction. Keep the card readable with larger system fonts and expose an accessible dismiss action. Phone-specific hints must not be reused on Android Auto, whose host click/double-tap semantics differ.
- On the phone, each double tap on the compass route view must cycle through the full-route overview, the adaptive moving-scale 2D view, a heading-up 3D tilted orthographic view, the moving-scale 2D view again, then return to the full-route overview. Require two short, nearby taps within the platform double-tap timeout and switch only on the second release. Single taps, non-swipe drags, long presses, cancellations, and multi-touch streams must not switch views or complete a pending pair. The cycle must work in compact and fullscreen compass layouts, portrait and landscape. Android Auto keeps the same cycle through its host-supported click control.
- On the phone compass route view, a one-finger left swipe must advance the same view cycle as double tap; a right swipe must traverse it in reverse, including wraparound and both 2D steps. Require at least 48 dp of horizontal travel (or twice the platform touch slop when larger), with horizontal travel at least twice vertical travel, and change exactly once on release. A deliberate vertical drag cancels swipe recognition for that stream. Swipes must clear any pending tap pair; cancellations, detached views, and any multi-touch stream must not switch views. Support compact/fullscreen and portrait/landscape, with the same animations, automatic-scale overrides, and five-second moving overview restore as double tap. Android Auto uses the same direction and threshold policy through host scroll callbacks, with the host-specific stream boundaries described in section 4.5.8.
- On the phone navigation compass, two-finger pinch gestures must select exactly five zoom levels relative to the current adaptive scale: the default, two steps in, and two steps out, with each step doubling or halving the visible radius. Each separate pinch must change at most one level, regardless of continued finger travel or reversal; lift both fingers and start another pinch for another step. Support compact/fullscreen, portrait/landscape, and 2D/3D moving-scale views; full-route overview views must ignore pinch zoom. Keep the adjustment when switching phone compass surfaces and moving-scale views, bypass it during full-route overview, and reset it when the navigation display resets.
- In phone 3D compass views, a parallel two-finger vertical slide must continuously adjust inclination from a shallow tilt (25% of the default tilt) to 125% of the default tilt. Keep the original inclination as the default, with room to tilt in both directions. Sliding up increases tilt and sliding down reduces it. Prepare one fixed viewport covering the strongest inclination while preserving the default projection and readable flat labels. Two-finger gesture streams, including finger release, cancellation, or a third finger, must never trigger the double-tap view cycle; 2D views must ignore tilt gestures.
- Phone and Android Auto 3D views must use central perspective by default and retain the original tilted orthographic projection as a selectable alternative, with its existing default vertical scale of 0.58. With Perspective 3D view disabled, apply uniform vertical compression about the current position, with no distance-dependent shrinking or enlargement: equal ground widths must have equal screen widths at every forward/backward distance, and parallel ground lines must remain parallel. Derive the expanded viewport and distance references from the reciprocal vertical scale so route and street coverage remains complete throughout inclination changes, with the local route scale preserved.
- Enabling Perspective 3D view must use a planar central projection (homography) in the existing 3D cycle step: forward ground geometry shrinks with distance and nearby geometry enlarges. Apply it to route, streets, markers, GPS accuracy and distance/heading references through the shared compact/fullscreen phone and Android Auto renderer. Preserve the current-position anchor, gesture inclination limits, 2D views and navigation guidance. Keep distance text and orientation cues upright. Prepare a fixed expanded source viewport for the strongest tilt, clip the ground plane before camera depth becomes zero, and derive distance/time references from the same projection. Changing inclination must reuse route and street geometry; switching the setting may replace the prepared viewport without resetting the selected view or zoom.
- The 3D orthographic view must tilt the existing route, surrounding streets, markers, GPS accuracy, heading arrow, and heading accuracy lines and arcs in both compact and fullscreen views around the current position. Its prepared source viewport must cover the full tilt so available route and street geometry can reach the visible compass or fullscreen edge while preserving the local route scale; the viewport geometry should be reused across animation frames. Distance text and orientation cues must remain readable above the tilted geometry; it must not require map tiles or change guidance calculations.
- In the compact 3D compass, the current-position center must move slightly down during the tilt so the expanded outer ring's forward arc stays visible inside the dial. Distance rings must use the expanded orthographic horizon, about 1.72 times the 2D travel times at the default tilt. Reference dashes, heading-accuracy lines, and accuracy arcs must use the same layout as 2D and be projected together with the rings. Distance/time label anchor positions must follow the projected 2D layout, while the text remains upright at the normal 2D font size. Draw the labels above the compass outer ring without the circular geometry clip, and keep their text within the view bounds so the outer time/distance pair remains visible.
- When Distance circles is enabled, fullscreen route views must show one circle using the same color, opacity, and stroke as the compact compass distance rings, centered on the current position and aligned with the central heading guide's farthest distance/time reference. Distance/time text must sit above its reference with a gap so it cannot overlap the circle. In the 3D orthographic view, project this circle, the heading arrow, heading accuracy lines, and accuracy arc together; the single distance/time label pair must follow the projected central reference while staying upright and readable in both portrait and landscape.
- Gesture-driven zoom changes between full-route and moving-scale views must use the same smooth radius transition used when the compass switches from stationary overview to the moving-scale view. Switching between 2D and 3D at the same moving radius must not restart the radius transition, and the tilt must animate over roughly a third of a second using display-aligned frames without rebuilding route and street geometry on each tilt frame.
- When Instant compass zoom is enabled in settings, automatic and gesture-driven compass zoom changes must jump immediately to the target radius instead of animating
- The moving-scale radius must not be capped to a smaller fixed maximum such as 600 meters
- When the user is moving but the current native speed reading is not yet reliable, the compass should prefer reusing the last reliable moving zoom radius if one exists instead of jumping back and forth between zoom modes
- When the user starts or resumes movement without a reliable moving-speed reading and no previous reliable moving zoom radius exists yet, the compass should fall back to the full-route overview until a reliable moving-speed reading becomes available
- When the user stops and the compass expands back to the full-route overview, the last reliable moving zoom radius should be preserved so it can be restored when movement resumes before speed confidence has recovered
- Automatic compass zoom/radius policy is a navigation-state responsibility and must remain separate from compass drawing and activity/service lifecycle wiring
- The gesture-driven view cycle must be only a UI override layered on top of the existing automatic behavior, so stationary navigation still defaults to the full-route overview when Zoom out when stationary is enabled, and moving navigation still defaults to the 2D moving-scale view whenever no temporary override is active. The selected 3D view remains selected until another view-switch gesture or the navigation display resets.
- Temporary absence of compass geometry during route calculation or route-unavailable states must preserve the selected view-cycle step, relative zoom and 3D inclination on phone and Android Auto. Clear geometry caches and cancel route-heading/radius transitions during that gap; keep input disabled until geometry returns, then apply the retained adjustments to the new route. The temporary moving overview's five-second expiry must remain unchanged. A new phone navigation screen or an explicit Auto navigation/surface reset clears the view and gesture adjustments.
- While moving, any view-switch gesture selecting the full-route overview must show the full route temporarily and then automatically restore the 2D moving-scale view after about 5 seconds
- The compass must retain the complete active BRouter track once per route for close moving-scale rendering, while the full-route overview and hint-marker geometry remain bounded and sampled. Moving-scale rendering must use the original points from only the contiguous route ranges intersecting the compass viewport plus drawing padding, keep disjoint re-entering ranges separate, and use a route-built spatial block index so heading and location updates do not project or scan the full long route
- Compass rendering should avoid per-frame transient object allocation in its hot drawing path for route, hint, and destination projection
- Heading-only updates must reuse cached north-up route paths and rotate them with the canvas in compact and fullscreen 2D/3D views. Position, route progress, geometry, radius, scale, and drawing-padding changes must rebuild the affected paths. Preserve crossing segments and heading-up viewport clipping at every rotation, including fullscreen edges during tilt.
- Surrounding streets must use cached north-up line batches per category in compact/fullscreen and 2D/3D views so heading and tilt updates avoid rasterizing large street paths on older Android devices. Preserve category order, colors, continuous street geometry, viewport clipping, and round stroke caps without retaining duplicate path geometry.
- In the moving-scale view, including after a tap from the full-route overview, the red route centerline and wider threshold overlay must remain continuously visible for the route portion crossing the compass instead of flickering or disappearing while off-screen route geometry is clipped
- Compass distance and time labels must continue to update live during smooth zoom transitions, using the rendered distance and resolved display speed while the target zoom follows the moving-scale bucket horizon
- When enabled in settings, the compass must draw surrounding BRouter street geometry as stroke-only blue major/highway, purple normal-street, green walking/cycling, and gray special-routing context lines behind the active route, without street labels, symbols, fills, or map-tile backgrounds
- Surrounding-street extraction must run off the UI thread, reuse cached segment-file lookups, avoid per-frame decoding, skip work while a previous extraction is still running, and refresh only after a meaningful time or distance change around the current position
- Surrounding-street context is supplemental only: it must not change route matching, reroute decisions, turn notifications, straight-line guidance, destination arrival, GPX export, or BRouter route requests
- Transitions between stationary overview and moving zoom should be smoothed instead of snapping abruptly, except that restoring a previously saved reliable moving zoom radius after a stationary pause may return directly to that saved scale to avoid intermediate zoom thrash
- Transitions between the full-route overview and the moving-scale view should use the same fast timing in both directions, reaching the target scale in about 1 second regardless of the total route length or overview radius delta
- The current-position marker should be shown as a small center dot
- A transparent orange filled circle centered on the current-position dot must visualize the current GPS accuracy radius at the compass scale, using the same orange as the accent ticks on the outer compass ring
- When Distance circles is enabled, a fixed vertical guide line must run from the center dot to the top border of the compass and end with an open arrowhead whose tip aligns with the guide line. The heading arrow, heading accuracy V lines, and accuracy arcs must use the same theme color and opacity as the distance circles in compact and fullscreen views, in both dark and light themes.
- The fixed top heading guide's open arrowhead must sit inside the outer compass layer and use the same short, wide top-marker geometry as the red target-direction cue so the two align when the cue points straight ahead
- When Distance circles is enabled and the displayed heading source exposes a heading-accuracy estimate, compact and fullscreen views must show two straight guide lines, using the same visual treatment as the fixed top heading guide, from the center to the outer distance ring at the negative and positive angular error bounds around the fixed top heading guide.
- The red target-direction cue must stay behind the heading guide and distance/time labels so those foreground references remain legible when the cue is visible.
- The compass must show a transient calibration background in the outer compass layer when the selected displayed heading source becomes explicitly inaccurate enough to need recalibration or recovery: translucent red while that selected source is inaccurate, translucent green when it returns to acceptable accuracy, and the green background must disappear automatically after about 2 seconds
- That calibration background must span from the outer visible distance ring to the outer border of the compass, sit behind the outer compass ticks and cardinal labels, and remain visually separate from the stationary orientation cue drawn around the compass border
- That calibration background must follow the same selected heading source that drives the rendered compass heading, such as matched route direction while moving on a route, trusted GPS or movement-derived course during beelines, live compass fallback, or confirmed heading-sensor heading after a substantial stationary turn; route geometry must not trigger a sensor calibration warning, and the display must not show a raw heading-sensor calibration warning while rendered from a different trusted source
- Missing numeric heading-accuracy data alone must not force the calibration background red; the warning should require explicit poor heading accuracy from the selected displayed heading source, including a fresh deprecated-orientation-sensor low or unreliable accuracy status when live heading-sensor heading is the selected display source
- While navigation is paused, the compass must show a light gray translucent paused-state background in the outer compass layer, using the same layer geometry and transparency as the calibration background, in addition to the paused-state status text and resume/play button
- When Distance circles is enabled, each compact distance ring must show a semi-transparent distance label to the right of the central heading arrow and a matching travel-time label to its left. Both labels must sit above the ring or accuracy arc with the same gap used in fullscreen views, including during 3D tilt.
- Compact compass views must show two distance/travel-time label pairs. Portrait fullscreen route mode and fullscreen 3D views must show only the farthest pair; landscape fullscreen 2D views must keep their existing three scale label pairs.
- In moving mode, the top visible distance ring is the primary horizon reference for those labels; inner-ring distance and time labels must scale from that top visible ring rather than from the hidden compass edge
- When heading accuracy is zero or unavailable, those labels must stay aligned above the short central-guide tick at the top ring intersection.
- When heading accuracy is non-zero, the top tick must be replaced by an arc spanning between the left and right heading-accuracy guides. Distance and time labels must remain anchored above the central arrow's ring intersection rather than moving to the V intersections; changing heading accuracy must not move the labels.
- Turning Distance circles off must hide distance circles, distance/time labels and reference ticks, the central heading line and arrowhead, and heading accuracy V lines and arcs in compact and fullscreen views, including full-route overview, 2D, 3D, and transition frames. Route and street rendering, current-position and destination/intermediate-stop markers, GPS accuracy, outer compass chrome, calibration/paused state, and the separate target-direction cue must remain available. The setting is display-only and must not change navigation guidance or zoom policy; Android Auto must inherit it through the shared compass renderer.
- Small semi-transparent white point markers must be shown on the route at the visible start position and at each visible hint position
- The route must be rendered as a continuous line, not as discrete dots
- The route ahead of the current matched position must keep the normal route red styling
- In moving mode and in the stationary full-route overview, the route ahead must be rendered in two red layers whenever the current off-track threshold extends beyond the current GPS accuracy radius: the original opaque route centerline plus a wider semi-transparent threshold overlay behind it
- That wider threshold overlay must visualize the current off-track threshold derived from recent smoothed location accuracy, so it acts as the allowed route corridor rather than a purely decorative fixed-width line
- That threshold overlay width should reflect the full threshold span around the route centerline excluding the GPS accuracy radius, not only a one-sided offset. This way, when the orange accuracy circle overlaps the red corridor, the user is still on track; when they no longer overlap, the user is off track
- In the stationary full-route overview, the red route centerline itself must keep a fixed visual stroke width instead of scaling to the off-track threshold, even though the threshold overlay remains visible
- The already passed part of the route must be shown as the same red with about 50 percent transparency, including passed segments archived from earlier active routes after recalculation
- When a reroute is applied, the compass passed-route overlay must retain already passed segments from the previous active route and append only the newly passed segment from each subsequent active route, without duplicating earlier archives. For an off-track reroute, the dotted red connector must follow accepted fixes from the last stable on-route fix, through each accepted off-route fix, to the first stable fix on the final route after any queued recalculations finish; it must not project an off-route fix farther along the old route. Other route-replacement gaps may use a straight dotted red connector.
- Compass and GPX connectors must share departure/rejoin endpoints and the ordered intervening fixes, including the current unfinished connector. A downstream rejoin begins a new travelled road section at the actual rejoin rather than marking the bypassed route prefix as travelled. A brief backward connector displacement within the two fixes' combined accuracy, capped at 30 m and within 5 seconds, may be omitted only when the next fix resumes the previous direction; unresolved reversals and confirmed backtracking remain visible, and diagnostic GPS waypoints are retained.
- The destination endpoint must be shown as a slightly larger opaque white point without a finish-line icon or enclosing badge
- The destination endpoint must only be shown when it falls within the currently visible compass radius; if it lies outside the visible radius, it should not be clamped back onto the compass edge as a detached marker
- Each remaining intermediate stop must be shown on the compass route with the same opaque white point radius as the destination endpoint and the same transparent destination-reached radius overlay

#### 4.5.3 Shared status block

- Below the compass, the UI must use a single shared status text block instead of separate destination-progress and secondary-detail text areas
- When no higher-priority notice is active, that shared status block must show the final destination progress and, if an intermediate stop is still ahead, the next intermediate-stop progress in the same text area
- Tapping the shared status block must show a live trip-stats UI styled like the GPS details UI, without duplicating GPS quality fields. It must include elapsed time, travelled distance, moving time, stationary time, average speed, moving average speed, max speed, screen-on time, screen-off time, estimated battery used in mAh, and battery percentage drop for the active navigation session. Battery fields must show `--` while charging or when the relevant battery reading is unavailable.
- The destination progress portion should use the same segment-aware hybrid estimator as maneuver timing: trustworthy smoothed live speed only for the remaining current-segment portion, plus BRouter-derived time for later segments
- The next intermediate-stop portion should use the same segment-aware hybrid estimator as maneuver timing and destination progress
- When the current next intermediate stop is passed, the shared status block must switch to the following intermediate stop if one remains
- Reaching an intermediate stop must not switch the shared destination progress line to the terminal destination-reached state; destination progress should remain active until the final destination is reached
- The UI must not list all remaining intermediate stops at once in that shared status block
- When a navigation detail or notice needs to be surfaced in that area, such as route-unavailable detail, blocked-road reroute feedback, or paused-state messaging, that detail must take precedence over progress content in the shared status block
- When the user explicitly triggers the blocked-road action and a reroute request starts, the shared status block should surface a specific blocked-road reroute-progress notice such as `Blocked road added. Recalculating route.` instead of only showing the generic route-calculation body text
- After the user enters the destination-reached radius, the shared status block should switch from live destination progress to a destination-reached message unless a higher-priority detail notice is active

#### 4.5.3.1 GPS status line

- The navigation UI must show a single compact GPS status line formatted as `<speed> ↑<elev> • <accuracy-meters> • (<sat>) • <countdown>`
- That compact line must show only the current speed, current elevation, horizontal accuracy of the displayed position in meters, the current number of GNSS satellites used in the fix, and the countdown until the next scheduled navigation position evaluation. Keep the countdown visible in its existing format; fix age must appear only in GPS details, never in the compact status line. Phone and Android Auto GPS details must have a permanent Last fix age row immediately after Interval, separate from GPS time. Show its age in seconds from 0 s upward, updating from the monotonic fix timestamp without requesting new GPS fixes; keep the row present with -- when the age is unavailable. New accepted fixes reset the value without hiding the row.
- The compact GPS status line text must be sized like the secondary upcoming-direction line, and its accuracy value must be orange
- When route speed-limit data is available and the current displayed speed is above that limit, the speed value in the compact GPS status line must be bold
- Tapping the compact GPS status line must show a live GPS details UI with the compact-line fields plus the GPS obtained time, accepted GPS fix count, GPS bearing, and GPS bearing accuracy
- The phone GPS details UI should use a compact centered dialog panel with larger readable details text instead of a wide default alert layout
- GNSS satellite-status callback tracking may be suspended while the phone navigation UI is not visible or the screen is non-interactive; normal navigation location updates must continue, and the last known satellite count should be retained until satellite-status tracking resumes
- The displayed speed must suppress raw provider speed when recent filtered fixes show only stationary jitter
- That GPS status line must stay on a single line and should reduce its text size as needed instead of wrapping onto a second line
- When any of those values is unavailable, the UI must show `--` in that field instead of omitting it
- The `<countdown>` field shows the time remaining until the next scheduled navigation position evaluation based on the current active update interval; it is a scheduling countdown and must not be interpreted as a guarantee that Android or fused location cannot deliver an earlier usable fix
- When tracking is active and an accepted location fix is processed, the `<countdown>` field should refresh from the active update interval, including when the existing location listener registration is reused rather than torn down and recreated

#### 4.5.3.2 GPX export button

- The navigation UI must show an icon-only export button in the bottom-right corner of the measured compass square
- The export button must overlay the compass area so it does not shrink, reflow, or otherwise change the compass route view
- The export button must sit in the square corner outside the compass circle rather than covering the circular compass surface
- Pressing the export button must transform the current active route into GPX and open it through an Android chooser using the GPX MIME type `application/gpx+xml`, with the GPX file provided as a stream and GPX viewer apps offered as explicit targets, so the user can select which installed GPX-capable app should receive the export instead of Android auto-opening a saved default viewer
- The chooser-backed GPX stream file must use the same selected Downloads destination as Auto-save GPX, requesting missing access before exporting and never redirecting storage. Preserve the timestamped vibro-navigator-route-yyyymmddhhmmss.gpx pattern and collision handling.
- The exported GPX route, track, and metadata name must use `ViBRo-Navigator Export <current datetime>` rather than the destination label
- The exported GPX must describe the chronological path followed plus the remaining planned route. Its route element combines travelled road sections, the same fix-by-fix connectors displayed by the compass, and the remaining plan; track elements contain the travelled sections and remaining plan without bypassed route prefixes or alternative streets. Consecutive command-16 legs at an intermediate stop must keep one chronological fix path rather than inserting the planned stop coordinate and replaying an accepted fix between the outbound and return legs. A command-16 leg that is activated and completed by the same accepted fix only because its target is already inside the reached-radius uncertainty must be skipped in travelled history; when the paired return target is inside that same radius, both legs must be skipped without a connector, and otherwise the return connector must begin at that accepted fix rather than at an earlier fix or planned anchor. The same accuracy-only skip applies to a final command-16 leg. Turn waypoints come only from navigation voice hints/destination instructions. Existing instructions reached within a departure boundary's positional uncertainty (capped at 25 m) must survive route replacement even when the last passed projection is just before the instruction. Never infer new turns from GPS geometry, and do not duplicate the same instruction at a shared section boundary. Every requested intermediate destination, including an already reached stop, must have an explicit waypoint with its original stop number.
- The exported GPX must include accepted GPS fixes from the current navigation session as waypoint entries and should attach GPX timestamps and GPX elevation elements to those fix waypoints when wall-clock fix times and altitude are available
- Accepted-fix waypoints, and straight-line passed-track points that represent those same accepted fixes, must also carry available provider, horizontal accuracy, speed, bearing and bearing-accuracy measurements in GPX extensions; those measurements describe the provider fix while exported coordinates are filtered.
- The `accuracyMeters` extension must retain the original provider accuracy, and `positionAccuracyMeters` must carry the conservative accuracy radius around the exported filtered coordinates.
- For straight-line navigation, the exported GPX must also include the passed beeline path connecting accepted GPS fixes as an additional passed-route track segment when at least two accepted fixes are available
- When the active route has no explicit destination-arrival voice hint, the exported GPX should include a synthetic destination-reached waypoint at the final route point
- The export flow must write the generated GPX XML into the application log before launching the chooser
- When Auto-save GPX on stop is enabled, confirming Stop captures the current exportable GPX before stopping navigation and queues the write off the UI thread. Manual exports use the same worker and destination. Missing access or a failed write disables automatic saving and reports export failure; remove partial files and pending MediaStore rows after failed writes. Manual permission grants do not enable automatic saving.
- With Use external storage off, GPX exports use phone Download/ViBRo/gpx. With it on, they use the same layout on the selected SD card. Logs use the sibling logs folder. No custom output-folder setting is exposed.
- Expose saved GPX through a content URI with a temporary read grant: MediaStore on Android 10+, SAF on legacy SD cards, and the read-only file provider for legacy phone Downloads. Limit new public-directory provider roots to ViBRo's gpx and logs folders. Request WRITE_EXTERNAL_STORAGE only on Android 6–9 when phone Downloads access is needed.
- If no active route exists, the navigation UI must show a short failure message instead of opening an empty GPX file
- If no installed app can open GPX routes, the navigation UI must show a short failure message instead of crashing

#### 4.5.3.3 Settings button

- The navigation UI must show an icon-only settings button in the bottom-left corner of the measured compass square
- The settings button must overlay the compass area so it does not shrink, reflow, or otherwise change the compass route view
- Pressing the settings button must open the existing About page and scroll directly to its Settings section while keeping the active foreground navigation session running
- Returning from the About page must refresh navigation UI theme state and active location-update settings so relevant setting changes apply during the ongoing navigation session

#### 4.5.3.4 Custom button

- The navigation UI may show an icon-only custom button in the top-right corner of the measured compass square when the Custom button setting is enabled
- The custom button must overlay the compass area so it does not shrink, reflow, or otherwise change the compass route view
- Pressing the custom button must toggle exactly one configured setting: Dynamic GPS interval, Light Theme, Surrounding streets, Fullscreen route, Notifications, or Speech directions
- The custom button icon must reflect both the configured setting and that setting's current enabled or disabled state

#### 4.5.4 Blocked road button

- Near the top of the screen: an icon-only circular blocked-road button
- The blocked-road icon should use a simple no-go / forbidden-sign style glyph that remains legible at button size
- The blocked-road button must live in the same bottom action row as the stop and pause/resume actions
- The blocked-road action must be unavailable while navigation is paused so the app does not queue reroute changes against a suspended guidance session
- In Route mode, while a route-start, native, or synthetic command `16` beeline is active, pressing the blocked-road button must skip that beeline target instead of adding a no-go point or recalculating for a blocked road. Guidance must advance immediately to the following beeline target or routed instruction.
- In Straight Line mode, the blocked-road button must be available while a direct target is active. Pressing it must skip the current intermediate stop or final destination and immediately advance to the next direct target; skipping the final target must leave navigation in its terminal state.
- A manually skipped target must not emit a false reached-arrival notification. A skipped intermediate stop must be removed from the remaining-stop plan used by later route recalculations, while remaining part of the original requested-stop history for export.
- While the button will skip a beeline target, its accessibility description must identify the action as `Skip beeline target`; outside beeline guidance it retains the normal blocked-road description and behavior.

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
- The Google Play flavor must let the user enable or disable Android Auto integration from the about page Settings section; enabling it must explicitly enable the Android Auto service component and disabling it must explicitly disable that component when no Android Auto host is connected, while the F-Droid flavor must not expose an enabled Android Auto setting.
- Android Auto must expose a `CarAppService` using `androidx.car.app.CarAppService` and declare the `androidx.car.app.category.NAVIGATION` car app category.
- The Google Play flavor must declare the `template` capability through `automotive_app_desc.xml` so Android Auto can discover the app.
- The Google Play flavor must declare the Android for Cars surface permission needed to draw the custom navigation surface.
- The Android Auto service should use the lowest practical `androidx.car.app.minCarApiLevel`, currently `1`, and prefer broadly supported templates and APIs so it remains compatible with as many Android Auto host versions as possible.
- Android Auto must not try to launch or render the phone `NavigationActivity` directly on the car display. Android Auto hosts a driver-optimized template surface, not arbitrary phone `Activity` layouts.
- The active Android Auto screen must use an Android for Cars navigation surface to draw a landscape-style navigation view: navigation text and the three-button action row on the left, and the compass route view on the right.
- The Android Auto surface is the car-display counterpart of the phone landscape navigation layout. Any change to the phone landscape navigation layout, navigation text/status presentation, compass overlays, fullscreen-route behavior, or navigation control set must include the corresponding Android Auto update in the same change unless the difference is explicitly documented as required by Android for Cars host constraints.
- The Android Auto surface must mirror the same active navigation state shown by the phone landscape navigation layout, including at least the GPS status, next direction, second direction when available, destination/progress/status text, blocked-road, stop navigation, pause/resume, and the compass route view.
- Android Auto must reflect phone landscape compass-overlay controls in driver-safe surface form where appropriate, including speed-limit display and the configured custom button when that phone setting is enabled. Settings and GPX export must stay phone-only and must not be exposed as Android Auto surface buttons.
- Android Auto must mirror the phone compass display mode on the car surface: compact compass mode stays compact in the right half, and fullscreen-route mode fills the right half. Landscape Android Auto must stay split at the center with navigation text/actions on the left and compass/route/street drawing clipped to the right, so route and surrounding-street geometry cannot overlap the left-side text or buttons.
- The Android Auto GPS status line must use the same compact fields, speed-over-limit emphasis, and tap-to-show live details behavior as the phone navigation layout.
- Android Auto must provide tap-to-show details for the same phone landscape text affordances: GPS details from the compact GPS status line, upcoming maneuver details from the direction block, and trip statistics from the shared status block. These details may use bounded driver-optimized surface panels rather than phone dialogs when Android for Cars host constraints make dialogs unsuitable.
- The Android Auto compass should reuse the existing `NavigationCompassView` rendering path so compass route geometry, radius behavior, paused-state chrome, orientation cues, and destination/intermediate markers stay consistent with the phone navigation screen.
- Android Auto must publish the resolved compass viewport it draws to the navigation service so the same surrounding-street overlay loading used by the phone navigation UI remains visible on the car surface while Auto is active, and it must clear that viewport when no active navigation/surface is present.
- Android Auto should expose the blocked-road, stop, and pause/resume controls both through the drawn surface layout and through the Android for Cars template action strip when required by the host template.
- A single host click on the Android Auto compass must advance the shared full-route / moving 2D / 3D / moving 2D view cycle, including the temporary five-second overview while moving and the Instant compass zoom preference.
- On hosts supporting Car App API 2 or later, the navigation template must include `Action.PAN` in its map action strip so the host delivers scroll and scale callbacks. Keep API 1 hosts compatible by omitting that strip, without raising the manifest minimum API level.
- Android Auto pinch and host double-tap scale callbacks must use the same five relative zoom levels as the phone, with one step per callback burst, preserving the adjustment across moving 2D/3D and compact/fullscreen views and bypassing zoom in full-route overview. Reject known scale focal points outside the compass or on its custom button; accept host-unavailable negative focal coordinates.
- Host horizontal scroll must advance the view cycle for leftward travel and reverse it for rightward travel, using the phone horizontal threshold and 2:1 dominance policy, at most once per burst. A predominantly vertical host drag must adjust inclination continuously in 3D with the same direction, limits, and height-relative sensitivity as the phone; ignore tilt in 2D and reuse the prepared viewport for tilt frames.
- Android for Cars exposes neither raw pointer counts, scroll origins, nor touch release/cancel events. It reports pinch and double tap through the same `onScale` callback, so double tap zooms in Auto while single click changes the view. Vertical tilt uses the host scroll stream rather than requiring two fingers, and scroll controls apply to the compass even when the host omits the scroll origin. Treat a quiet interval longer than the platform double-tap timeout as a new callback burst; pauses within a held gesture and rapidly repeated gestures cannot be distinguished. Ignore compass clicks following a recent scroll/scale burst so finger release cannot also change the view. Gesture availability depends on the host.
- Apply Auto gesture adjustments after resolving the shared compass mode and before publishing the displayed street viewport. Clear compass mode, zoom, inclination, and pending host gesture input when navigation is cleared or the car surface closes; ignore scale/scroll input without an active surface and compass.
- When no active navigation is available, the Android Auto screen must show a concise no-active-navigation state without a phone-launch action.
- Android Auto UI state should bind to the existing `NavigationService`/`NavState` listener path rather than duplicating route calculation, location tracking, or guidance logic. While the Android Auto screen is open, it should keep trying to attach to an already-running navigation service so navigation started later from the phone appears on the car display without reconnecting Android Auto.
- Android Auto controls must use the existing navigation binder/service actions for blocked-road, pause/resume, and stop, so phone and car surfaces stay consistent.
- Turning Android Auto integration off from phone settings while a car session or system car mode is active must clear navigation from the Android Auto screen and stop rebinding to the navigation service. The component disable must be deferred until the active host session and car mode have ended to avoid Android Auto host error screens; turning the setting back on before then cancels the pending disable.
- Android Auto launcher visibility is host-controlled after service component state changes, so the settings help text should make clear that some hosts refresh the app list only after Android Auto reconnects.
- Android Auto text must come from flavor resources and must not be hardcoded in Java.

### 4.6 Background behavior

- Navigation functionality must remain active in the background
- Navigation functionality must remain active when the screen is off
- Background and screen-off navigation reliability must be provided primarily by the location foreground service and ongoing location callbacks rather than by holding a session-long CPU wake lock
- Partial wake locks may be used only as short, focused guards around critical work such as startup bootstrap, route calculation, or reroute calculation, and each acquisition must use an explicit timeout and be released by the same flow that acquired it
- The app must not rely on continuously renewing or indefinitely holding a partial wake lock for the full navigation session
- Screen-off or background navigation must suspend compass UI state dispatch, compass rendering updates, and surrounding-street viewport/extraction work, but heading-sensor monitoring needed for stationary-orientation notifications must continue
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
  - Links immediately after the summary, starting with the project homepage and a simple home icon before the Google Play rating link (when available) and project source code, followed by the GitHub new-issue page, the changelog for the installed app version, the public Privacy Policy, and the public Terms of Service
  - The Changelog link must follow Report an issue, reuse the Terms of Service list icon, and open `https://damianofalcioni.github.io/ViBRo-Navigator/CHANGELOG/#v<version>` using the app's version name
  - Copyright and license text
  - API/data-source attribution stating the active POI search data source and that map tiles and geodata are by OpenStreetMap contributors, including the `https://www.openstreetmap.org/copyright` URL
  - A Settings section below the about text
  - A Diagnostic section at the end

#### 5.1 Logging and diagnostics

- Each entry in the about page Settings section must include a right-side info button that opens a short UI explanation of that setting or settings action
- The about page Settings section must show a Log enabled switch
- The about page Settings section must show an Auto-save GPX on stop switch that is enabled by default
- Logs and Auto-save GPX retain their label, circular 44dp configuration button immediately left of the switch, and info button. Their dialogs show the actual current folder and compact right-aligned Open, red Delete, and OK actions. Remove custom output-folder selection and reset actions.
- Delete must show an explicit permanent-deletion confirmation. Delete only generated ViBRo filenames of that output type in the destination captured by the confirmation; retain unrelated files, other output types, subfolders and the folder. MediaStore deletion must filter by app ownership. Run deletion off the UI thread, serialize GPX deletion with exports, and close/restart logs under the logging lock with system details first. Cancelling deletes nothing, and deletion failure must never redirect into fallback storage.
- Open launches the system document browser in a separate ViBRo files task, keeping the main ViBRo task independently reachable in Recents. A non-exported transparent host activity with a distinct task affinity receives picker results; it owns no file-browser UI. Use ACTION_OPEN_DOCUMENT and the selected folder as the initial location where supported (Android 8+); Android 6–7 starts at the system picker default location. Reopening replaces the previous browser host instead of stacking hosts. Avoid directory ACTION_VIEW handlers, which can accept the intent and immediately close on OEM devices. Do not request a new folder grant just to browse. Prepare folders/provider access off the UI thread, but still launch the browser if preparation fails. Use the existing granted ancestor for SAF subfolders, handle unavailable pickers/viewers safely, open selected files in a viewer with temporary read access, finish the host after selection/cancellation, and leave storage preferences unchanged. Picker results and activity recreation must not relaunch duplicate pickers or affect the main ViBRo task.
- GPX exports and logs use only Download/ViBRo/gpx and Download/ViBRo/logs on the selected phone or SD storage. Android 10+ use MediaStore.Downloads on the chosen writable indexed volume; Android 6–9 use direct phone Downloads access with runtime WRITE_EXTERNAL_STORAGE or persistent SAF access to SD-card Downloads. Never fall back to another volume, Android/media, internal files, or Android/data. Missing or revoked access, unavailable storage, and write failures switch off the affected logging/automatic GPX setting. Enabling either setting requests missing access for its selected destination and verifies a write before enabling. Denial or cancellation leaves it off. Manual GPX export also requests missing access and resumes export only after a successful grant; this never enables automatic saving. Permission requests survive activity recreation. Older files stay in place, and old custom output-folder preferences are ignored.
- The first Advanced setting is Use external storage, off by default and enabled only when a supported mounted writable SD card is available. Exclude USB drives. On Android 6–9, enabling it reuses valid persisted read/write access to that card's Download folder (or its root/ViBRo ancestor), requesting access only when missing or revoked, even when both saving features are off. Disabling the switch retains the grant for subsequent enabling. Verify both fixed output folders before enabling the setting. Cancellation or failure leaves a previously disabled setting off. Storage access never enables logging or GPX saving. Card removal stops saving without changing the selected storage; insertion permits manual exports or explicitly enabling saving again. Recreate missing SAF directories only below an existing granted ancestor.
- Downloads survive uninstall. SD-card selection and URI grant preferences are device-local and excluded from Android and in-app backups. A reinstall needs legacy access again. Retain android:hasFragileUserData="true" for supported user-controlled app-data retention.
- The about page Settings section must show a Use fused location switch
- The about page Settings section must show a Use imperial units (ft/mi/mph) switch for distance, speed, elevation, and accuracy display values
- The about page Settings section must show a Light Theme switch that enables or disables the app's optional light theme while keeping the black theme as the default
- The about page Settings section must show a Show surrounding streets in compass switch that enables or disables the local-BRouter-segment street overlay in the navigation compass and is enabled by default
- The Surrounding streets row must also include an icon-only settings button for the per-category and per-type street visibility selector; its info button remains available.
- When legacy external-storage permission is required to read local BRouter street segment files, enabling the Show surrounding streets in compass switch must request that permission and leave the setting disabled if permission is denied
- The about page Settings section must show an Instant compass zoom switch that disables compass zoom animations so transitions between route overview and moving-scale views are immediate and is disabled by default
- The about page Settings section must show a Fullscreen route switch that expands the route view and is enabled by default
- The Compass settings category must show a Distance circles switch, enabled by default, that controls distance circles, their time/distance labels, the central heading arrow, and heading accuracy lines and arcs in both compact and fullscreen views. It must include a help button, save its value across launches, participate in settings backup/export, and apply when returning to active navigation.
- The Compass settings category must show a Perspective 3D view switch after Fullscreen route, enabled by default, choosing central perspective when enabled and the original orthographic tilt when disabled. Include a help button explaining the difference and the existing gestures for entering 3D. Persist the preference across launches, include it in database backup/export, refresh it after import, and apply it when returning to active navigation and on subsequent Android Auto draws.
- The Compass settings category must show a Show hint panel switch, enabled by default, with a help button. Turning it off must keep phone navigation gesture hints hidden across subsequent starts, new requests, and recreation, and hide any still-visible panel when returning to navigation. Persist the preference across launches, include it in database backup/export, and refresh it after import. Touch dismissal and the five-second timeout must not change this preference; reenabling takes effect on the next navigation start/request.
- The about page Settings section must show a disabled-by-default Zoom out when stationary switch under Compass that controls whether stationary navigation automatically changes from a remembered moving-scale route view to the full-route overview
- The Google Play flavor must show an Android Auto integration switch in the about page Settings section that enables or disables the Android Auto service component, deferring the package-manager disable while an Android Auto host is connected, and is enabled by default; the F-Droid flavor must not expose an enabled Android Auto integration switch
- The about page Settings section must show a single-row POI category filter setting with a `POI categories filter` label, an icon-only list button for editing category names, and a switch that enables or disables the map POI category filter
- The POI categories filter editor must let the user manage multiple category-name fields, each with the placeholder `POI Category Name`, an item switch between the field and a trash remove button, plus a centered bordered `+` button that adds another field
- Fresh installs must prefill the POI categories filter editor with commonly needed categories for driving, walking/running, and cycling: `Bicycle Repair Station`, `Drinking Water`, `Fuel`, `Hospital`, `Parking`, `Pharmacy`, `Police`, `Public Transport Stop Position`, `Supermarket Shop`, `Taxi`, and `Toilets`
- Fresh installs must enable the POI category filter by default
- The about page Settings section must show a Speech directions voice spinner that can disable spoken maneuver notifications, use the system default TextToSpeech voice, or select one of the downloaded/offline Android TextToSpeech voices available on the device
- Downloaded/offline Speech directions voice options should show user-friendly labels derived from the voice locale and readable voice variant when available, instead of exposing raw TextToSpeech engine identifiers in the spinner label
- The Speech directions voice spinner dropdown should visually highlight the currently selected voice option
- The Speech directions voice spinner row must include an icon-only settings button that opens the device's built-in Android Text-to-speech settings page, falling back to Android's TTS data installer when the settings page is unavailable
- The Speech directions settings dialog must show bordered controls for the voice preview play button and the Android Text-to-speech settings button
- Fresh installs must enable speech directions with the system default TextToSpeech voice by default
- The Google Play flavor must let the user save an optional Google Maps API key for POI search and enable or disable Google search; the app must validate a non-empty key through Google Maps Geocoding before marking it valid, and when Google search is enabled with a valid key, POI search and coordinate reverse geocoding must use Google Maps Geocoding instead of OpenStreetMap Nominatim
- The F-Droid flavor must not enable the Google Maps API key setting
- The about page Settings section must show an Export database button that lets the user save a JSON backup of all app-managed stored data, including POI history, saved routes, app settings, logging preference, BRouter profile selections, and BRouter profile parameter overrides
- The about page Settings section must show an Import database button that lets the user select a JSON backup and restore those same app-managed stored data stores
- Database export and import must use Android's document picker flows so the user chooses the backup file location without requiring broad storage permissions
- The Use fused location switch must be enabled only in builds that support Google fused location
- In the F-Droid flavor, the Use fused location switch must be disabled and must not enable Google functionality
- In the Google Play flavor, disabling Use fused location must force the legacy platform GPS/network provider path even when Google Play Services is available
- The about page Settings section must show a Dynamic GPS fix interval switch that is enabled by default; disabling it must force active navigation location fix requests to 1-second intervals instead of the dynamic buckets
- The app must write its detailed session log file only when the Log enabled setting is switched on
- The app must always record uncaught Java crashes regardless of the Log enabled setting. With logging enabled, append them to the current session log; with logging disabled, create a standard timestamped log file only when an anomaly occurs, with the usual system-details entry first
- When the app next starts, it must record a previously active navigation session that ended without a clean stop. On Android 11 and later it should include the system-reported process exit reason when available; otherwise it must label the cause as unknown rather than attribute it to battery optimization or another app
- On Android 11 and later, the app should also record system-reported crash, native crash, ANR, and other clearly abnormal process exits even when navigation was not active. Forced kills cannot be logged at the moment of termination
- The Auto-save GPX on stop, Instant compass zoom, Zoom out when stationary, and Distance circles settings must persist across app launches and be included in database backup/export with other app settings
- The Log enabled setting must persist across app launches
- When Log enabled is already on at app startup, the app must create a fresh log file for that app session before startup logging begins
- When Log enabled is switched on during an app session, the app must create a fresh log file for the remaining logs in that session
- One active selected log destination receives entries until termination, logging is switched off, the storage choice changes, or writing fails. A failure disables logging without creating a fallback file. Explicitly enabling logging again starts a new verified session in the selected folder. Write each entry once and never copy earlier history between destinations. Publish MediaStore logs after the initial system-details entry so Files can view the live file; preserve bounded trimming and UTF-8 line boundaries. Anomaly logs also require access to the selected folder and never use fallback storage.
- Log files must use the `vibro-navigator-log-yyyymmddhhmmss.txt` naming pattern, with a collision suffix when needed so app sessions opened close together do not overwrite each other
- The first entry in each newly created log file must report Android version/API level plus relevant app build, device, locale, screen, and log-file context before other app startup or setting-change logs
- Accepted-location diagnostics must include native speed and bearing accuracy, explicitly distinguish unavailable accuracy from numeric zero, and record the stationary decision and resolved motion speed alongside raw and filtered fixes. Phone render diagnostics must include display heading accuracy, active beeline mode and cue target bearing, including cue-only changes.
- Location diagnostics must include monotonic fix time, original provider accuracy, filtered position accuracy, and raw-to-filtered displacement. Filter rejection diagnostics must state the reason, including invalid data, duplicate/older samples, and unconfirmed jumps.
- When logging is enabled, the app must log the full decoded BRouter response payload in addition to the existing route summaries
- The logging implementation should keep a single shared path for log-entry formatting and file appends so single-line and multiline records cannot silently diverge in behavior
- The about page Settings section must show a Navigation notifications switch that enables or disables transient live-navigation alert notifications; the permanent foreground navigation notification and diagnostic direction test notifications must remain enabled
- The about page Settings section must show a Single instruction mode switch that keeps navigation alert notifications enabled but changes routed maneuver notifications to a single trustworthy 10-second approaching alert; reached-arrival and warning notifications must not be disabled by this mode
- The about page Settings section must show a Custom button setting with a switch to show or hide the navigation custom button and an icon-only configuration button whose UI uses a spinner to choose the setting toggled by that navigation button: Dynamic GPS interval, Light Theme, Surrounding streets, Fullscreen route, Notifications, or Speech directions; fresh installs must show the custom button by default and target Light Theme
- The about page Diagnostic section must currently list the app's used live inputs:
  - GPS provider
  - network provider
  - rotation vector heading sensor
  - geomagnetic rotation vector heading sensor
  - deprecated orientation sensor, used by navigation only as a calibration cross-check when available
  - linear acceleration sensor
  - accelerometer
- Diagnostics show GPX-folder and log-folder rows only on Android 6–9, even when saving is disabled. Hide these rows on Android 10+ where MediaStore.Downloads requires no folder grant, and skip their access checks. Show only each folder's name and green/red access status; do not show folder paths in these rows. Tapping a row needing access launches the legacy phone permission or SD folder-access flow without enabling saving. Reflect revoked grants and unavailable cards. Folder checks and recovery probes run off the UI thread.
- Before the sensor-status list, the Diagnostic section must show a Required access block for navigation access status:
  - location permission
  - device location services
  - app notifications
  - battery optimization exemption
- When Show surrounding streets in compass is enabled and legacy external-storage permission is relevant on the current Android version, the Required access block must also show a BRouter street storage row with OK/KO status; when the setting is disabled or the permission is not relevant, that row must be hidden
- When BRouter is installed, the Required access block must show BRouter profile storage status using the shared external-storage read permission on Android 6–9 and the persisted `profiles2` folder grant on Android 10 and later. Tapping the row must request the corresponding access and refresh status after the result.
- On Android 6–9, Logs and GPX must each have a Required access row showing the folder's name and green OK/red KO status even when saving is disabled, without displaying the path. Hide both rows on Android 10+. Tapping either visible row must request missing access for the selected storage without enabling saving. Provider checks and recovery probes must run off the UI thread; folder failures must not block navigation. After a write failure, verify a successful write before returning to green.
- Each Required access row must show a green OK or KO mark and a short status label. Missing hard startup requirements must show red KO. Missing battery optimization exemption must show orange KO and must not block navigation startup.
- Tapping a Required access row must open the most specific relevant Android settings page available, falling back to app details when needed in the same OEM-compatible style as navigation startup settings redirects. The battery optimization row must request the app-specific exemption when it is KO, and must open the generic battery optimization settings page when it is already OK.
- The diagnostics block must refresh automatically every 1 second while the about page is visible
- Each listed item must show both its current status and its latest available value details
- Location-provider details should include the latest available fix data such as coordinates, accuracy, speed, bearing, bearing accuracy, satellite count, and sample age when available
- Heading-sensor details should include the selected sensor type plus the latest available heading/orientation-derived values and sample age when available
- Acceleration-sensor details should include the latest available axis values, vector magnitude, accuracy, and sample age when available
- The about page Diagnostic section must also show actions to send notification-symbol tests for left, other, and right guidance notifications
- Triggering any of those actions must post a fresh notification entry, not only update an existing one, so mirrored smart bands or similar devices can treat each test run as a new notification
- Those test notifications must contain the full set of distinct user-visible symbols currently used by the app's notification text formatting, including all direction/status symbols used in guidance notifications and the degree sign used by stationary-orientation notifications
- The test notification titles must identify the tested group as `test all lefts notifications`, `test all others notifications`, or `test all rights notifications`
- Those test symbols should remain simple enough to render on generic smart bands rather than assuming full emoji support

### 6. Shared/opened coordinates and addresses

- The app must support opening or sharing map coordinates or addresses into the app
- Shared/opened coordinates or addresses must fill the destination only when its text is empty (ignoring whitespace). When the destination already contains text or a selected POI, preserve it and fill the first empty intermediate stop in displayed order; if every existing stop is filled, append and fill a new intermediate stop. Other filled stops must remain unchanged.
- The same placement rule must apply after a shared short map link finishes resolving, using the current form values at that time.
- Activity recreation must preserve the filled route without inserting an already applied incoming location again. A pending incoming location must be applied only after the destination and stops are restored.
- Shared/opened coordinates must show the coordinates immediately in the chosen destination or stop field, then replace its visible label with a concise reverse-geocoded address when an internet lookup succeeds. The stored coordinates must remain the original incoming coordinates.
- Incoming locations that resolve to valid coordinates must be saved into the same destination history list used by manual POI selection
- The app must register as a target for at least these incoming Android formats:
  - `geo:` map intents
  - `google.navigation:` intents
  - shared `text/plain` payloads
  - `http(s)` map links for:
    - `maps.google.com`
    - `maps.app.goo.gl`
    - `google.com/maps`
    - `www.google.com/maps`
    - regional Google `/maps` domains such as `google.it/maps`, `www.google.it/maps`, and `www.google.co.uk/maps`
    - `openstreetmap.org`
    - `www.openstreetmap.org`
- Incoming `maps.app.goo.gl` short links must be expanded by following HTTP redirects before applying normal map-link parsing, including when Google returns a regional `/maps` domain such as `google.it`.
- The app must not register itself as a generic handler for arbitrary web URLs or for non-map `google.com` and `www.google.com` pages such as search results or article links
- Incoming coordinate or address intents must open the app without crashing on any supported Android version
- Parsing of incoming locations must be compatible with the app's minimum supported Android API level
- Invalid or malformed incoming coordinate payloads must fail gracefully instead of crashing or silently redirecting to placeholder coordinates
- On devices where multiple apps can handle the same map/share intent, the system chooser may appear before the user selects ViBRo Navigator
- The app must also open or receive GPX documents advertised as `application/gpx+xml`, common GPX MIME aliases, or phone file providers that expose `.gpx` documents with generic XML/binary MIME types. GPX document reads must stay off the main UI thread.
- A GPX import must prefer valid explicit route-form `<wpt>` entries in document order: the last waypoint becomes the destination and every preceding waypoint becomes an intermediate stop in that same order. Waypoint names should be used as the visible POI labels, falling back to coordinates when a name is absent; unnamed imported coordinates should then use the same best-effort reverse-geocoding behavior as opened/shared coordinates, replacing the visible label with an address when an internet lookup succeeds while keeping the original coordinates. ViBRo-exported annotation waypoints such as turn instructions and accepted GPS fixes must not be imported as stops or destinations.
- When a GPX document has no usable route-form waypoints, import its valid `<rtept>` entries in document order. When a GPX document is track-only, import the final valid `<trkpt>` as the destination without treating dense track geometry samples as intermediate stops.
- Applying a GPX route must switch out of Round Trip mode, replace the existing destination and intermediate-stop form values, and promote all imported points into the normal POI history without triggering POI-search popups.
- A malformed GPX document, unreadable URI, or GPX document with no valid waypoints must leave the existing route form unchanged and show a short failure message.
- Opening a GPX document while navigation is already running must not replace the active navigation screen with the main route form; the import should be rejected and the existing navigation UI resumed.

## Non-functional expectations

- Battery-conscious background navigation
- Minimal UI and minimal code footprint
- Offline-first routing through BRouter
- Translation-friendly text resource usage
- Orientation-safe layouts
- Robust permission handling before navigation starts
- Robust OEM-compatible redirects for required system settings and battery-optimization exemption requests
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
- A maintainer may opt into Gradle Google Play publishing for `gplayRelease`, using external service account credentials and the existing signing environment. Default uploads must create internal-track drafts, and normal/F-Droid builds must not require the publisher plugin or credentials.
- Published stable GitHub Releases must be the single automatic upstream trigger for Google Play upload and F-Droid metadata submission. CI must build signed APKs and the Google Play AAB from the release tag, attach them to the release, and then run the two store jobs independently. It must submit the already-built AAB to the production track with a completed full rollout through a dedicated GitHub Action, retaining the Gradle publisher for manual fallback. Google review and Play Console managed publishing controls still apply. Prereleases, ordinary pushes, pull requests and manual build dispatches must not submit to the stores.
- Official F-Droid metadata must request upstream-signed `fdroid` APKs using a versioned public GitHub Release URL and an allowed signing certificate fingerprint. F-Droid must reproduce the unsigned APK before publishing the upstream-signed binary; an APK upload alone does not establish reproducibility.
- The repository should provide a maintainer-local release-prep Gradle task
  that updates Android/F-Droid version metadata and changelog files for a
  requested semantic version without creating a commit or tag. The task should
  summarize generated changelog additions in the console so maintainers can
  review them before committing.
- Upstream app-store metadata should be maintained in the source repository using the `fastlane/metadata/android/en-US/...` layout so F-Droid can reuse the app description, changelog, icon, and screenshots directly from upstream.
- Public store-facing legal and disclosure documents must live under `docs/` so GitHub Pages can serve stable public URLs for the Privacy Policy, Terms of Service, local data-deletion instructions, and maintainer store-disclosure notes.
- The repository should provide maintainer-facing submission documentation for official F-Droid inclusion. That documentation is an operator runbook for project maintainers and should not be treated as end-user product documentation.
- GitHub Actions should run the APK build workflow for normal commit and pull-request CI. F-Droid readiness checks should be maintainer-operated manually or run automatically as a required pre-submit gate after release artifacts have been published. Release calls should reuse the tested signed APK from the parent build; manual runs may rebuild the release source. Submission must verify the public release APK's signature, package and version against the requested release. Official publication still requires F-Droid maintainer review and F-Droid-side reproducible rebuild/publish steps.
- Release builds must remain unsigned without the explicit signing environment, including F-Droid's source rebuilds. Maintainer-controlled upstream CI may sign release APKs for F-Droid's binary verification and distribution.
- Machine-specific local development configuration, such as SDK paths or local API keys in `local.properties`, must not be required for release validation or F-Droid builds.

## Implementation guidance

- Keep UI entry-point packages separated by purpose: `main` for destination/profile/stop setup, `map` for manual map picking, and `nav/ui` for the active navigation screen. `main/MainActivity` and `nav/ui/NavigationActivity` should stay thin. Input validation, incoming-intent handling, main-screen widget binding, destination field state persistence, startup/preflight checks, and navigation startup orchestration should stay in dedicated helpers.
- Keep navigation display state separated from text assembly: `nav/model/NavState` should remain the immutable display snapshot, with route/guidance/progress, GPS, and pause state exposed through focused value objects rather than duplicated top-level scalar aliases. Android/resource-aware state assembly should stay in `nav/presentation/NavStateComposer` and use `NavStateBuildInput` plus `nav/session/NavigationDisplaySnapshot` for route-display handoffs. Route direction/progress line assembly should stay in `nav/format`, compass-state assembly should stay in `nav/compass` through `NavCompassStateFactory`/`NavCompassStateInput`, compass rendering should stay in `nav/compass/ui`, route GPX XML export should stay in `nav/export`, Android GPX content URI, persistent file writing, and chooser intent creation should stay in `android/export`, and primitive navigation text formatting should remain shared between on-screen state, GPX instruction waypoints, and notifications.
- Keep `nav/service/NavigationService` focused on Android lifecycle and orchestration, with dependency construction/attachment grouped by foreground, tracking, and routing runtime contracts, start/stop command handling, notification callbacks, location/provider event handling, location subscriptions, route execution callbacks, listener broadcasting, UI-visibility/compass gating, orientation/display-heading preparation, paused-state turn-event gating, and turn-event fan-out isolated in focused collaborators such as `NavigationServiceDependencies`, `NavigationServiceCommandHandler`, `NavigationServiceLocationHandler`, `NavigationServiceRouteCallback`, and `NavigationServiceTurnEvents`.
- Keep wake-lock ownership narrow and task-scoped in `nav/power`. Long-lived navigation reliability should come from the foreground location service, while any partial wake lock should be acquired only by the collaborator performing the short critical section that needs it.
- Keep background route computation asynchronous in `nav/routing` while all shared navigation-state mutation remains serialized on the main thread. Route executor threading/callback handoff, transient-failure retry policy, and the BRouter adapter should remain separate collaborators.
- Keep `nav/session/NavigationSession` split across focused collaborators for filtered location, route progress, blocked-road state, turn progression, route-request lifecycle handling, active-route export handoffs, display-state handoffs, and explicit handoff value types rather than collapsing that logic into one class.
- Keep active route/polyline geometry ownership in `nav/route`, route-location evaluation in `NavigationRouteEvaluator`, final-arrival checks in `NavigationArrivalDetector`, intermediate-arrival tracking in `NavigationIntermediateArrivalTracker`, blocked-road point selection in `NavigationBlockedPointSelector`, route-result application in `NavigationRouteResultApplier`, route-deviation policy, confirmation, direction-of-progress evidence, and reroute-notice selection in `nav/guidance`, route display branching in `NavigationSessionRouteDisplayState`, compass display memory in `CompassDisplayMemory`, and route display assembly in `nav/presentation`/`nav/format`/`nav/compass` so safety decisions stay easy to review independently. Display memory updates should be explicit display-advance steps rather than hidden side effects of pure state construction.
- Keep compass display state grouped by display mode, radius state, progress labels, orientation cue, blocked areas, and route points. `NavCompassState` should remain the top-level immutable compass snapshot, while rendering code consumes `CompassDisplayMode`, `CompassRadiusState`, `CompassProgressLabels`, `CompassOrientationCue`, `CompassBlockedArea`, and `CompassRoutePoint` instead of relying on duplicate scalar aliases. Construction should use named factories for projected-point snapshots and route-geometry-backed snapshots, with grouped construction inputs for display metrics, radius metrics, destination projection, blocked areas, and optional orientation cue rather than direct public constructors or long primitive constructor chains.
- Keep heuristics such as reroute thresholds, bearing trust rules, forward-look route bearing, direction-of-progress checks, polling cadence, synthetic intermediate-arrival sequencing, and turn-alert timing in small policy/planner helpers. Keep POI query/search state shared across destination and stop fields, with text-field selection state, history rename/delete actions, popup-window presentation, and query precedence/debounce/provider search kept in separate collaborators.
- Keep flavor-specific services behind a small distribution bridge. Common code may call flavor-neutral interfaces, but Google Play Services imports, Google fused-location implementation, Google POI search, Android Auto service/template code, and Google parser tests must remain under `app/src/gplay` or `app/src/testGplay`. The `fdroid` source set must provide no-op or OpenStreetMap-only behavior for the same bridge contracts.
- Keep the Android Auto entry point in `app/src/gplay/java/vibro/navigator/auto`. Auto screens should consume `nav/model/NavState` through the existing `NavigationService` listener/binder API and translate that state into Android for Cars templates without owning navigation-domain decisions. Treat phone landscape navigation UI changes as Android Auto touch points: inspect and update the Auto painters/templates whenever the landscape navigation controls, overlays, fullscreen-route behavior, text blocks, or detail affordances change.
- Keep `nav/model/NavigationRequest` as a pure domain request. Keep the Android navigation-intent extras contract owned by `android/intent/AndroidNavigationRequestIntentContract` so activities, the foreground service, and resume notifications serialize the same request shape without hand-copying extras. Keep app-wide incoming map/share URI parsing under `intent/`, separate from navigation-start extras; entry-point shells may extract Android `Intent` action/data/text directly before handing strings to that parser when no reusable adapter boundary is needed.
- Prefer extending the existing `logging/AppLogger` coverage when touching startup, permissions, routing, background execution, or network search behavior.

## Testing expectations

- Prefer JVM regression coverage, with Robolectric for Android lifecycle behavior where practical.
- The core automated suite should not require an emulator or real device, though some foreground-service and OEM notification behaviors may still need manual verification.
- Keep lifecycle decisions, heuristics, planners, and policy thresholds in small helpers when practical so they remain directly unit-testable.
- Maintain coverage for navigation-request serialization, startup/preflight flow, reroute heuristics, bearing trust, route-progress confirmation, blocked-road escalation, turn progression, route-request lifecycle handling, foreground-notification monitoring, route-execution callback handoff, turn-event dispatch, and safe listener broadcasting.
- Maintain heading-selection coverage for Route and Round Trip modes, Straight Line mode, route-start and command `16` beelines, known/unknown/poor bearing accuracy, stale moving fixes, compass fallback, beeline completion, and heading-only refreshes. Cover routed disagreement confirmation/recovery, independent fixes, poor-bearing displacement support, lifecycle resets, small roadside shifts, sharp turns, curves, U-turns, and maneuver-bounded display smoothing. Preserve startup compass tracking and the existing stationary activation, continuous tracking, and movement-resumption behavior.
- Voice-hint mapping coverage should verify the current BRouter mode-9 command set, including user-visible direction symbols.
- Maintain a zero-violation PMD maintainability gate for production and JVM test Java sources, including flavor-specific source sets, covering complexity, size, coupling, nested-flow, dead-code, duplicate-literal, and related rules.
- Distribution-sensitive changes should run explicit flavor checks, including `testFdroidDebugUnitTest`, `testGplayDebugUnitTest`, `lintFdroidDebug`, `lintGplayDebug`, `assembleFdroidRelease`, and `assembleGplayRelease`.
- Refactors that only move unchanged wiring into helpers or package-level value contracts do not require new tests by default. Behavior changes in helper-owned flows should add or update focused JVM or Robolectric coverage.

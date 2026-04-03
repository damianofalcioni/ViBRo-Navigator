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
- Check and request all required permissions on startup
- Provide a README describing VibeNavigator as a lightweight, battery-efficient, offline vibe-coded GPS navigation app that only vibrates directions
- Provide a distinctive app logo suitable for use as the app icon

## Functional specification

### 1. Main UI

The app must show a main UI implemented as an `AppCompatActivity`.

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

- Open a new navigation UI implemented as an `AppCompatActivity`
- Access the current user location
- Use the installed BRouter app intent/service integration to calculate a path from the current location to the destination
- Include any intermediate stops in the route calculation

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

- Every 2 seconds initially
- Later at a dynamic interval proportional to the distance to the next direction
- The dynamic interval must never exceed 60 seconds
- Position handling must use a Kalman filter
- Any asynchronous route calculation must apply its resulting shared navigation state in a single serialized path so stale background results cannot overwrite newer navigation state

#### 4.4.1 Off-track reroute

- The route must be recalculated whenever the user position differs by 10 meters plus the GPS error distance from the current track

#### 4.4.2 Wrong-direction reroute

- The route must also be recalculated when the user is still on the track but is moving in the wrong direction
- Wrong direction is defined as bearing difference greater than 60 degrees

#### 4.4.3 Direction distance estimation

- The app must estimate the distance left to the next direction
- The estimation must use current speed and the direction distance returned by BRouter

#### 4.4.4 Turn notifications

- The app must send notifications:
  - When navigation starts and the first route has been calculated, for the first upcoming direction even if the user is not moving yet
  - When the previous direction has just been passed
  - When 10 seconds remain to the next direction
  - When 5 seconds remain to the next direction
- Turn notifications must reuse a single notification entry in the notification list so older direction notifications do not pile up
- Replacing a direction notification in the notification list must still be compatible with smart bands or similar devices that mirror notifications as they arrive
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

#### 4.5.2 Route progress

- In the center: the distance left, time left, and arrival time
- This must be shown for:
  - The final destination
  - Every intermediate stop

#### 4.5.3 Blocked road button

- Below the progress section and centered: a `blocked road` button

##### 4.5.3.1 Blocked no-go memory

- Pressing the button must add route-based no-go points derived from the upcoming matched route geometry, not from the raw GPS position
- The first press in an area must create a single no-go point slightly ahead on the route
- The first blocked area must use a small street-scale radius of about 10 to 12 meters
- This internal no-go list must be reset when a new navigation is started

##### 4.5.3.2 Blocked reroute

- After blocking the upcoming route area, the app must recalculate the route
- The recalculation must pass the no-go point list, including per-point radii, to BRouter

##### 4.5.3.3 Repeated blocked-road escalation

- Repeated presses in the same nearby area, or repeated presses within a short time window in a nearby area, must escalate the blocked region
- Escalation must increase both:
  - the number of forward route points used as no-go points
  - the no-go radius applied to those points
- The blocked-road behavior should be tuned primarily for walking and cycling, with cars treated as a secondary use case

#### 4.5.4 Stop navigation button

- At the bottom: a button to stop navigation
- Pressing it must return to the previous UI
- Destination and intermediate stops must be kept

#### 4.5.5 Back button behavior during navigation

- Pressing the system back button while the navigation UI is open must move the whole app task to the background
- Pressing back during navigation must not reveal the main UI underneath the navigation UI
- Navigation must continue running after this backgrounding action as long as the foreground service remains active

### 4.6 Background behavior

- Navigation functionality must remain active in the background
- Navigation functionality must remain active when the screen is off

#### 4.6.1 Foreground service lifecycle

- Active navigation must run through a foreground service with an ongoing notification
- If the app task is removed from recents, navigation must stop and the foreground service must be terminated
- If the foreground notification is removed while navigation is still running, reopening the app from recents must restore the foreground notification immediately when the navigation UI reconnects to the running service
- The app should treat removal of its own ongoing navigation notification as a stop signal when the Android device delivers that removal event to the app
- Navigation request extras used by the main screen, navigation screen, foreground service, and resume notification should be serialized through one shared contract so those entry points stay behaviorally identical

### 5. About button and page

- A small button showing only the app logo must be displayed at the very top center
- Pressing it must open an about page
- The about page must contain:
  - The app version
  - The same content as the README

#### 5.1 Hidden developer mode

- While on the about page, five fast taps anywhere on the page must enable a hidden developer mode
- Enabling developer mode must show a popup confirming that developer mode is now enabled
- Developer mode state must persist across app restarts
- The app must write its log file only when developer mode is enabled
- When developer mode is enabled for the first time, the app must create a new log file named `vibe-navigator-log-yyyymmddhhmm.txt` using the current local date and time
- Repeating the five-tap developer-mode gesture while developer mode is already enabled must start a new developer log session and switch logging to a newly timestamped file
- When developer mode is enabled, the app must log the full decoded BRouter response payload in addition to the existing route summaries
- The logging implementation should keep a single shared path for log-entry formatting and file appends so single-line and multiline records cannot silently diverge in behavior

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
- Robust permission handling at startup
- Compatibility with all supported Android versions for intent parsing and deep-link handling

## Implementation guidance

- Keep `MainActivity` as a thin UI coordinator. Navigation-input validation and profile-selection state should live in dedicated helpers instead of growing back into the activity.
- Keep navigation text formatting shared between on-screen state and notifications so turn wording, distance formatting, and time formatting cannot drift across surfaces.
- Keep the foreground service focused on Android lifecycle concerns. Route/session state, reroute heuristics, blocked-road escalation, and route-result application should remain isolated in a dedicated navigation-session component.
- Keep POI search execution shared across destination and stop fields rather than allocating one executor or thread owner per input controller.

## Testing expectations

- Automated regression coverage should prefer JVM tests
- Navigation lifecycle behavior should be covered with host-side Robolectric tests where practical
- Pure lifecycle decision rules should be kept in small plain-Java helpers when practical so they can be covered by standard JUnit tests
- The project should not require an emulator or real device for its core automated test suite
- Foreground-service and task-lifecycle behaviors that depend on OEM or system UI notification handling may still require manual verification in addition to JVM coverage
- Voice-hint mapping coverage should verify the current BRouter mode-9 command set, including rendered direction symbols for user-visible cues
- Shared navigation-request serialization and live-location arbitration are part of the expected JVM regression surface and should remain covered by unit tests

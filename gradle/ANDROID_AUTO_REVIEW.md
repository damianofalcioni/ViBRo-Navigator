# Android Auto review preparation

Shared navigation simulation and the host auto-drive callback are implemented. Complete the manual DHU checks below before resubmission. Automated checks cannot establish Google Play approval or head-unit compatibility.

## Reviewer setup instructions

Use these as the basis for the Play Console App content → App access instructions; replace the test destination with one inside the downloaded BRouter region.

1. Install the Google Play variant of ViBRo Navigator and BRouter (`btools.routingapp`). Open both apps once.
2. Complete ViBRo's welcome. In BRouter, download the routing-data region containing the test origin, destination and route corridor.
3. In ViBRo, choose Route mode and a suitable available car routing profile. Profile-folder access is needed for external/custom profiles; ordinary bundled profiles do not require a custom file. Grant any requested legacy storage access.
4. Start a route on the phone to complete location/notification setup. Enable device location. The optional Google search key is not required; coordinate destinations work offline with the downloaded routing data.
5. Enable Android Auto in ViBRo Settings and connect Android Auto/DHU. Use the active route or issue a standard navigation intent to the app. Address searches require internet and result selection; coordinate intents do not.
6. Confirm that the host shows maneuvers, distance to the next turn, and ETA when available. Stop, Pause/Resume and the available Blocked road action use the same phone navigation session.

### Simulation for reviewers

1. On the phone, open Settings → Advanced → Simulation. Use its configuration button to set speed (default 50 km/h, or the equivalent in mph) and an optional starting latitude, longitude inside the downloaded BRouter region. The map button beside the coordinates selects the same position from the POI map. Save the configuration.
2. Enable the Simulation switch and start navigation to a destination in that region. Settings apply on the next navigation start. Leave the origin blank to use device location; configured coordinates avoid waiting for a physical GPS fix. Keep device location services enabled and grant Android location/notification permissions, as required by the foreground service.
3. Connect Android Auto/DHU. Guidance is marked Simulation; positions follow the route without moving the device. Pause/Resume freezes/continues movement. Stop ends it. To return to real navigation, stop, disable Simulation and start again.
4. Also test host-triggered simulation with the manual switch off: `adb shell dumpsys activity service vibro.navigator/vibro.navigator.auto.ViBRoCarAppService AUTO_DRIVE`. For the debug Google Play package, replace the component with `vibro.navigator.debug/vibro.navigator.auto.ViBRoCarAppService`. Start a route if none is active. The callback can arrive before binding and is retained by the car session. Existing real navigation restarts in simulation. This does not change the stored switch; destroying the car session stops host-triggered simulation.
5. Use an origin/destination and profile for which BRouter can calculate an actual route. Initial loading holds position, intermediate stops wait for normal arrival confirmation, and loop routes continue through their return leg. A speed of 0 intentionally holds position. Device sleep does not fast-forward the simulation.

A recording may help explain setup, but does not replace test-drive support.

## Manual checks before resubmission

- Use the Android Auto Desktop Head Unit with cluster emulation. Check next-turn metadata while ViBRo is visible and while covered by another car app.
- Drive a route with an intermediate stop and two successive maneuvers. Check that the main display shows the stop's distance and ETA and the relative distance and time to the following turn when available. Confirm that the native turn card remains available during a retained-route warning and that the map badge highlights when speed exceeds the posted limit.
- Start another navigation app and confirm ViBRo's host stop callback stops tracking, speech, car/phone turn notifications and cluster guidance; it must not restart the old session.
- Check route calculation/recalculation, pause/resume, confirmed arrival, stop, disconnect/reconnect and integration disabling. No stale turn or ETA should survive those transitions.
- Exercise `geo:0,0?q=<latitude>,<longitude>` and `google.navigation:q=<latitude>,<longitude>` through initial and new Session intents; verify address selection, cancellation, setup failure/retry and replacement by a newer destination.
- Request a coordinate route whose BRouter data is missing. When routing completes with a missing-data error, confirm the parked setup prompt appears. Download the data when parked and retry without reentering the destination.
- Check different head-unit aspect ratios, safe-area changes, night/day contrast, route/street rendering and host map gestures. Directions/ETA/controls must remain host components.
- Test the required `AUTO_DRIVE` host command, callback before/after route start, host stop and car-session destruction before claiming NF-7 coverage.
- VI-1: with no route and with missing setup/permissions, verify that every phone handoff tells the driver to use the phone only when safely parked. Check the selected app language as well as English.
- DR-1/DR-2/DR-3: measure action feedback (at most two seconds), cold launch (at most ten seconds), and destination content loading (at most ten seconds). With a slow/unreachable search provider, loading must become a localized timeout after nine seconds; Retry must retain the query/destination and Cancel must prevent any later route start. Repeat after covering/resuming the app and after a newer intent replaces the first.
- AC-1/API compatibility: exercise address selection, setup failures, repeated timeouts/retries and cancellation without exhausting the host template quota or encountering a dead end. Verify that retries keep the title and row title of the loading/status pane stable, including on Car API 1. The API-1 result list has at most four destinations and one Cancel row. Newer hosts show Cancel in the result action strip.
- EP-2: return to the Android Auto home screen and relaunch during navigation, pause and destination selection. Verify active navigation reconnects without an unintended restart; also test host/session recreation. Process-death recovery and pending selection continuity must be assessed separately from a normal covered/resumed session; do not claim they are proven by binding tests.
- VD-1/MR-1/SA-1: inspect the actual host-rendered controls and map in day/night modes, all supported aspect ratios and changing safe areas. Check text/icon contrast against Google's 4.5:1 requirement, clipping, and map transitions for distraction. Record measured results rather than treating themed resources as proof.

## Release handling

Prepare a new semantic version/code using the existing `prepareRelease` workflow after the remaining work is complete. Version `1000000` must be replaced in Closed Testing and every other affected track; a new upload cannot reuse that code. Follow [PLAY_STORE.md](PLAY_STORE.md) for release handling. This change does not publish an AAB, alter live tracks, or opt out of Android Auto.

## Official references

- [Navigation requirements NF-1 through NF-7](https://developer.android.com/docs/quality-guidelines/car-app-quality)
- [Navigation intents, metadata, notifications and simulation](https://developer.android.com/training/cars/apps/navigation)
- [Location foreground-service prerequisites](https://developer.android.com/develop/background-work/services/fgs/service-types#location)
- [Car App Library release notes](https://developer.android.com/jetpack/androidx/releases/car-app)

# Android Auto review preparation

Simulation is deferred at the maintainer's request. Do not submit this work as fully compliant until the required test-drive callback is implemented and tested. Automated checks cannot establish Google Play approval or head-unit compatibility.

## Reviewer setup instructions

Use these as the basis for the Play Console App content → App access instructions; replace the test destination with one inside the downloaded BRouter region.

1. Install the Google Play variant of ViBRo Navigator and BRouter (`btools.routingapp`). Open both apps once.
2. Complete ViBRo's welcome. In BRouter, download the routing-data region containing the test origin, destination and route corridor.
3. In ViBRo, choose Route mode and a suitable available car routing profile. Profile-folder access is needed for external/custom profiles; ordinary bundled profiles do not require a custom file. Grant any requested legacy storage access.
4. Start a route on the phone to complete location/notification setup. Enable device location. The optional Google search key is not required; coordinate destinations work offline with the downloaded routing data.
5. Enable Android Auto in ViBRo Settings and connect Android Auto/DHU. Use the active route or issue a standard navigation intent to the app. Address searches require internet and result selection; coordinate intents do not.
6. Confirm that the host shows maneuvers, distance to the next turn, and ETA when available. Stop, Pause/Resume and the available Blocked road action use the same phone navigation session.

Add the actual simulation activation instructions only after simulation exists. A recording may help explain setup, but does not replace test-drive support.

## Manual checks before resubmission

- Use the Android Auto Desktop Head Unit with cluster emulation. Check next-turn metadata while ViBRo is visible and while covered by another car app.
- Start another navigation app and confirm ViBRo's host stop callback stops tracking, speech, car/phone turn notifications and cluster guidance; it must not restart the old session.
- Check route calculation/recalculation, pause/resume, confirmed arrival, stop, disconnect/reconnect and integration disabling. No stale turn or ETA should survive those transitions.
- Exercise `geo:0,0?q=<latitude>,<longitude>` and `google.navigation:q=<latitude>,<longitude>` through initial and new Session intents; verify address selection, cancellation, setup failure/retry and replacement by a newer destination.
- Check different head-unit aspect ratios, safe-area changes, night/day contrast, route/street rendering and host map gestures. Directions/ETA/controls must remain host components.
- Complete simulation and test the required `AUTO_DRIVE` host command before claiming NF-7 coverage.

## Release handling

Prepare a new semantic version/code using the existing `prepareRelease` workflow after the remaining work is complete. Version `1000000` must be replaced in Closed Testing and every other affected track; a new upload cannot reuse that code. Follow [PLAY_STORE.md](PLAY_STORE.md) for release handling. This change does not publish an AAB, alter live tracks, or opt out of Android Auto.

## Official references

- [Navigation requirements NF-1 through NF-7](https://developer.android.com/docs/quality-guidelines/car-app-quality)
- [Navigation intents, metadata, notifications and simulation](https://developer.android.com/training/cars/apps/navigation)
- [Car App Library release notes](https://developer.android.com/jetpack/androidx/releases/car-app)

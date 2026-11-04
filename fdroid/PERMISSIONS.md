# F-Droid permission inventory

This inventory covers the `vibro.navigator.foss` release APK submitted in
[fdroiddata MR !52673](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52673).
The complete list below was checked with `aapt2 dump permissions` against
the signed `v1.0.0` FOSS APK, including permissions merged from AndroidX.
The current FOSS release manifest requests the same permissions.

| Permission | Use in ViBRo Navigator FOSS |
| --- | --- |
| `android.permission.INTERNET` | Optional OpenStreetMap/Nominatim destination and reverse-address searches, Overpass POI queries, map-tile downloads in the map picker, and resolving supported shared map links. Route calculation with BRouter's downloaded data and GPS guidance remain offline. |
| `android.permission.RECORD_AUDIO` | Microphone input when the user activates voice entry for a destination or intermediate stop. The fallback recognition-service path needs this permission; recognition is delegated to the installed speech provider. It is not used for spoken directions, which use text-to-speech. The provider determines whether recognition uses a network service. |
| `android.permission.READ_EXTERNAL_STORAGE` | Android 6–9 only (`maxSdkVersion=28`): read shared-storage BRouter profiles and downloaded routing segments for local routing/street display, and user-selected input files. Newer Android versions use document/folder grants instead of this broad permission. |
| `android.permission.WRITE_EXTERNAL_STORAGE` | Android 6–9 only (`maxSdkVersion=28`): save exported GPX tracks, diagnostic logs, and settings/database backups in the app's ViBRo download folders. Newer Android versions use Android's scoped output and document/folder APIs. |
| `android.permission.ACCESS_COARSE_LOCATION` | Approximate positioning when the user grants coarse location, including locating the user and providing the available navigation/location status. It is requested together with fine location. |
| `android.permission.ACCESS_FINE_LOCATION` | Precise GPS positions for turn guidance, route progress, off-route detection, arrival checks, and the current-location action in the map picker. |
| `android.permission.WAKE_LOCK` | A bounded partial CPU wake lock around BRouter route calculation/recalculation, so a screen-off transition does not interrupt the work. The route lock is released when the calculation exits and has a 60-second safety timeout. It does not keep the screen illuminated or hold a permanent navigation wake lock. |
| `android.permission.FOREGROUND_SERVICE` | Run the user-started navigation service while the app is backgrounded or the screen is off, with a foreground-service notification. |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | Declare the location type of that foreground service on Android versions that require this additional permission. |
| `android.permission.POST_NOTIFICATIONS` | Android 13+: show ongoing navigation status/actions and maneuver/arrival alert notifications, including notification-channel vibration patterns. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Open Android's user-approved battery-optimization exemption request to reduce interruptions to screen-off navigation. The user decides whether to grant the exemption. |
| `vibro.navigator.foss.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | An AndroidX Core compatibility permission, declared with `signature` protection. It protects dynamically registered non-exported broadcast receivers on older Android versions from broadcasts sent by unrelated apps. It does not grant access to additional user data and has no runtime prompt. |

The FOSS APK does not request `ACCESS_BACKGROUND_LOCATION`: location is used
through user-started navigation and its location foreground service. Google
Play Services and Android Auto permissions belong to the separate `gplay`
variant and are absent from this APK.

The merged manifest also references `android.permission.DUMP` as a caller
requirement on AndroidX's diagnostic receiver. That is a restriction on who
may invoke the receiver, not a permission the app requests.

References: [Android permission definitions](https://developer.android.com/reference/android/Manifest.permission),
[location foreground services](https://developer.android.com/develop/background-work/services/fgs/service-types#location),
and [AndroidX ContextCompat receiver protection](https://android.googlesource.com/platform/frameworks/support/+/b827e60cec1f802a9fa2dbfbe6a0ddd7576bafca/core/core/src/main/java/androidx/core/content/ContextCompat.java).

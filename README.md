# VibeNavigator

VibeNavigator is a lightweight, battery‑efficient, and offline‑first vibe‑coded GPS navigation app built around **BRouter**.
It keeps routing offline (via the installed BRouter app), and communicates directions through **vibration patterns** (no voice prompts) plus a minimal on‑screen view.

## What it does

- Offline routing via **BRouter** (must be installed and have routing data).
- Minimal UI with a dark/black theme.
- Destination + optional intermediate stops.
- Background navigation via a foreground service (screen off supported).
- Turn notifications with vibration (different patterns for left/right imminents).
- Pressing back during navigation sends the app to background instead of returning to the main screen.
- Removing the app from recents stops navigation.
- Reopening an active navigation session restores the foreground notification if the system hid or removed it.

## Requirements

- Android 5.0+ (minSdk 21)
- BRouter installed (`btools.routingapp`); bundled profiles are detected automatically, and custom external profiles can still be selected from a `profiles2` folder when present

## POI search

- If a Google API key is provided, searches using Google’s REST APIs.
- Otherwise uses OpenStreetMap (Nominatim).
- You can always paste coordinates directly (e.g. `45.4642, 9.1900`).

## Permissions

VibeNavigator will prompt you to enable what it needs, when it needs it:

- Location (for navigation)
- Notifications (for turn alerts on Android 13+)
- Foreground service (for background navigation)
- Battery optimization exemption (optional but recommended for reliable background behavior)

## Build

- Open the project in Android Studio and run.
- For a Google key, define `GOOGLE_MAPS_API_KEY` in `local.properties` or as an environment variable.

## Using BRouter profiles

VibeNavigator first tries to list bundled profiles directly from the installed BRouter app.
If you use custom external `*.brf` profiles, you can still select a `profiles2` folder manually.
Legacy/example path (device-dependent, may not exist on newer BRouter builds):

`/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`

## Releases / CI

GitHub Actions builds an APK on:
- every GitHub Release
- manual workflow dispatch

---

Vibe‑coded navigation: fewer distractions, more flow.

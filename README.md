# VibeNavigator

VibeNavigator is a lightweight, battery‑efficient, and offline‑first vibe‑coded GPS navigation app built around **BRouter**.
It keeps routing offline (via the installed BRouter app), and communicates directions through **vibration patterns** (no voice prompts) plus a minimal on‑screen view.

## What it does

- Offline routing via **BRouter** (must be installed and have routing data).
- Minimal UI with a dark/black theme.
- Destination + optional intermediate stops.
- Background navigation via a foreground service (screen off supported).
- Turn notifications with vibration (different patterns for left/right imminents).

## Requirements

- Android 5.0+ (minSdk 21)
- BRouter installed (`btools.routingapp`)
- BRouter profiles available under `.../brouter/profiles2` (the app guides you to grant folder access on modern Android versions)

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

On first launch, VibeNavigator asks you to select the `profiles2` folder from the BRouter app storage, so it can list `*.brf` profiles in the vehicle selector.
Example path (device-dependent):

`/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`

## Releases / CI

GitHub Actions builds an APK on:
- every GitHub Release
- manual workflow dispatch

---

Vibe‑coded navigation: fewer distractions, more flow.

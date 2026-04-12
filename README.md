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
- BRouter installed (`btools.routingapp`); bundled profiles are detected automatically, and custom external profiles can still be selected from an accessible `profiles2` folder when present
- When BRouter is not installed, the app should immediately tell the user that routing is unavailable and offer a direct link to the BRouter Play Store or F-Droid page instead of opening profile-file or profile-folder pickers

## POI search

- Typed destination and stop queries check saved history from the first typed character; only if history has no matches does the app fall back to the online provider search after 3 characters.
- If a Google API key is provided, searches using Google’s REST APIs.
- Otherwise uses OpenStreetMap (Nominatim).
- You can always paste coordinates directly (e.g. `45.4642, 9.1900`).
- Saved destination history entries can be renamed or deleted from the dropdown.
- Selecting a saved destination closes the dropdown and does not immediately reopen suggestions unless you change the text again.

## Incoming links and shares

- Accepts `geo:` and `google.navigation:` intents.
- Accepts shared `text/plain` payloads containing coordinates, addresses, or supported map links.
- Accepts `http(s)` map links from `maps.google.com`, `google.com/maps`, `www.google.com/maps`, `openstreetmap.org`, and `www.openstreetmap.org`.
- Does not register itself as a generic handler for arbitrary web pages or normal `google.com` search URLs.

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
That bundled-profile fallback keeps normal routing working even when no external profile folder is selected or accessible.
If you use custom external `*.brf` profiles, you can still select a `profiles2` folder manually.
When BRouter itself is missing, VibeNavigator must not prompt for a `profiles2` folder on app open or when starting navigation; it should only surface the missing-BRouter prompt.
For custom profile browsing, VibeNavigator probes multiple common `profiles2` locations and prefers one that actually exists on the device. Common locations include:

- `/storage/emulated/0/Android/media/btools.routingapp/brouter/profiles2`
- `/storage/emulated/0/Android/data/btools.routingapp/files/brouter/profiles2`

Some devices or BRouter builds may only expose one of those locations, while others rely mainly on bundled internal profiles.

## Releases / CI

GitHub Actions builds an APK on:
- every GitHub Release
- manual workflow dispatch

---

Vibe‑coded navigation: fewer distractions, more flow.

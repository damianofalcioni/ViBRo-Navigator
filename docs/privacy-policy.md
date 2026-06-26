---
title: Privacy Policy
permalink: /privacy-policy/
---

# Privacy Policy

Last updated: 2026-06-26

This Privacy Policy applies to ViBRo Navigator, package name
`vibro.navigator`, published by Damiano Falcioni.

ViBRo Navigator is an Android navigation app. It is designed to calculate and
show navigation guidance on the user's device, with optional network lookups for
places, map tiles, and map points of interest.

## Contact

Privacy inquiries can be submitted through the public GitHub issue tracker:

<https://github.com/damianofalcioni/ViBRo-Navigator/issues/new>

## Data handled by the app

ViBRo Navigator may access or store the following data when you use the related
features:

- Location data, including approximate and precise device location, for current
  position, navigation, route progress, map centering, and diagnostics.
- Heading and movement sensor data for compass and navigation guidance.
- Destination and stop searches, selected places, coordinates, and route names.
- Saved route data, including destination and intermediate-stop labels and
  coordinates.
- App settings, such as units, theme, POI category filters, speech settings,
  BRouter profile selections, BRouter profile parameters, and Google Play flavor
  settings.
- An optional Google Maps API key that you enter in the Google Play flavor.
- Optional app logs when the Log enabled setting is turned on. Logs can include
  navigation, route, search, location, BRouter response, and exported GPX
  details.
- Exported database backups and GPX files when you explicitly create or share
  them.

Most app-managed data is stored locally on your device in Android app storage or
in files you explicitly choose through Android's document picker. The developer
does not operate an app server that receives this data.

## Data sent to third parties

ViBRo Navigator does not send data to a developer-operated server. Some features
send requests directly from your device to third-party services or apps:

- OpenStreetMap Nominatim receives place-search text when OpenStreetMap search
  is used.
- OpenStreetMap tile servers receive tile requests for the map area displayed in
  the map picker.
- Overpass API receives map bounds and POI category selectors when map POIs are
  loaded.
- In the Google Play flavor, Google Maps Geocoding receives search text and the
  user-entered API key when Google search is enabled with a valid key.
- In the Google Play flavor, Google Play services may process location requests
  when the Use Google Fused Location setting is enabled and available.
- The installed BRouter app receives route points, routing profile information,
  and related route parameters when BRouter route calculation is used.
- Android Auto hosts may receive navigation display information when the Google
  Play flavor's Android Auto integration is enabled and connected.
- Android TextToSpeech may receive maneuver text when speech directions are
  enabled.
- Android notification listeners, connected watches, or car systems may display
  navigation notification text if you have enabled those system integrations.

Those third-party services, apps, and Android system components are controlled
by their respective providers.

## What the app does not do

ViBRo Navigator does not provide user accounts, in-app purchases, ads,
developer-operated analytics, developer-operated crash reporting, social
features, or a developer-operated tracking service.

## Permissions

The app requests Android permissions needed for navigation and related features,
including location, internet access, notifications, foreground service location,
and wake lock access. Location and notification access are used only for app
functionality such as route guidance, diagnostics, and turn notifications.

## Retention and deletion

Local app data remains on your device until you delete it, clear app storage,
uninstall the app, delete exported files, or replace it through the import
feature. Optional logs remain in app-specific files until deleted by you or by
Android app storage cleanup.

See [Data Deletion and Local Data Removal](../data-deletion/) for practical
removal steps.

## Security

App-managed data is stored using Android app storage and Android's normal app
sandbox protections. Network requests to the services listed above use HTTPS.
Database export, import, and GPX export use Android picker or sharing flows so
you choose where files go and which apps receive shared files.

## Children

ViBRo Navigator is a general navigation utility and is not designed specifically
for children.

## Changes

This policy may be updated when the app's data practices or store requirements
change. The updated version will be published on this page.

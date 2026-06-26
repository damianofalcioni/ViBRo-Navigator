---
title: Store Disclosure Notes
permalink: /store-disclosures/
---

# Store Disclosure Notes

Last updated: 2026-06-26

These notes are for maintainers preparing Google Play or F-Droid publication.
They are not a replacement for reviewing the current app code and current store
forms before submission.

## Public URLs

Use these URLs after GitHub Pages is enabled from the repository `docs/`
folder:

- Privacy Policy: `https://damianofalcioni.github.io/ViBRo-Navigator/privacy-policy/`
- Terms of Service: `https://damianofalcioni.github.io/ViBRo-Navigator/terms-of-service/`
- Data deletion and local data removal: `https://damianofalcioni.github.io/ViBRo-Navigator/data-deletion/`

## Google Play

Google Play requires a privacy policy URL in Play Console and a privacy policy
link or text inside the app. The About page links to the public privacy policy.

Google Play also requires a Data safety form. Review the form against the
current code before each release. For the current implementation, pay special
attention to:

- Approximate and precise location used for navigation, map centering, and
  diagnostics.
- Place-search text sent to OpenStreetMap Nominatim by default.
- Map tile requests sent to OpenStreetMap tile servers.
- Map bounds and POI category selectors sent to Overpass API for map POIs.
- Optional Google Maps Geocoding requests in the Google Play flavor when the
  user saves a valid API key and enables Google search.
- Optional Google Play services fused-location handling in the Google Play
  flavor.
- Route points and routing parameters handed to the installed BRouter app.
- Local POI history, saved routes, settings, BRouter profile values, optional
  logs, exported database backups, and GPX exports.
- Notification text that can be displayed by Android notification listeners,
  connected wearables, Android Auto, or similar system integrations.

Current repo behavior does not include user accounts, ads, in-app purchases,
developer-operated analytics, developer-operated crash reporting, or a
developer-operated backend service.

All developers are asked Google Play Data deletion questions. The app does not
enable account creation, so the account-deletion web-link requirement should not
apply unless account creation is added later. The public data-deletion page can
be used to explain local data removal if a non-account deletion-practice URL is
needed.

## F-Droid

F-Droid metadata needs separate public project fields. Keep these aligned:

- `WebSite`: the GitHub Pages site for app documents.
- `SourceCode`: the public source repository.
- `IssueTracker`: the public issue tracker.
- `License`: `MIT`, matching the repository license.

The F-Droid flavor must remain buildable without Google Play Services, Android
Auto dependencies, Google Geocoding code, Firebase, ads, proprietary analytics,
or bundled API keys.

No separate F-Droid privacy-policy URL is required by the current upstream
metadata file, but the public Privacy Policy is still useful for users and
maintainers.

## Official references

- Google Play User Data policy:
  <https://support.google.com/googleplay/android-developer/answer/10144311>
- Google Play Data safety form guidance:
  <https://support.google.com/googleplay/android-developer/answer/10787469>
- Google Play account deletion guidance:
  <https://support.google.com/googleplay/android-developer/answer/13327111>
- F-Droid Build Metadata Reference:
  <https://f-droid.org/en/docs/Build_Metadata_Reference/>
- F-Droid Inclusion Policy:
  <https://f-droid.org/en/docs/Inclusion_Policy/>

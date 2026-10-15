## Abstract

### Please introduce the functions and usage of your app in summary, and also attach some screenshots.

ViBRo Navigator FOSS is a small, offline-first GPS navigator for Android 6 and later. It uses BRouter for offline routes and round trips, and provides straight-line guidance without BRouter. Directions use vibration patterns, optional concise speech, and a compass-style display. Navigation can continue with the screen off. It also supports saved routes, intermediate stops, GPX import/export, OpenStreetMap destination search, and 18 interface languages.

This submission builds the `fdroid` flavor (`vibro.navigator.foss`). Google Play Services, Google POI search, and Android Auto belong to the separate `gplay` flavor and are excluded from this APK. The FOSS runtime dependency is AndroidX Core; BRouter is a separate app accessed through its service interface.

Release: [{{RELEASE_REF}}](https://github.com/damianofalcioni/ViBRo-Navigator/releases/tag/{{RELEASE_REF}}), version {{VERSION_NAME}} ({{VERSION_CODE}}).

![Main screen](https://raw.githubusercontent.com/damianofalcioni/ViBRo-Navigator/{{RELEASE_REF}}/fastlane/metadata/android/en-US/images/phoneScreenshots/01-main-ui.jpg)
![Navigation screen](https://raw.githubusercontent.com/damianofalcioni/ViBRo-Navigator/{{RELEASE_REF}}/fastlane/metadata/android/en-US/images/phoneScreenshots/03-navigation-ui.jpg)

### Please elaborate why you wrote this app. Please highlight the unique functions it provides over other similar apps in F-Droid.

As described in the upstream README, the app was created for an older Android 8 phone with a degraded battery and limited storage after a preferred navigation app dropped support. Its focus is a small APK and guidance that can be followed through vibrations and short voice instructions, with minimal need to look at a map or keep the screen on. It combines offline BRouter routing with configurable vibration guidance, round trips, and a straight-line mode. It is intended to complement BRouter rather than bundle its routing engine or map data.

## Checklist

### Policy

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy). The source is public and MIT licensed; this recipe builds the FOSS flavor.
* [x] The original app author has been notified and does not oppose inclusion. This is the upstream author's submission.
* [x] The upstream repository contains Fastlane metadata, including the required `en-US` descriptions, icon, screenshots, changelog, and translations.

### Docs

* [x] Read [CONTRIBUTING.md](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md).
* [x] Checked the metadata against the [templates](https://gitlab.com/fdroid/fdroiddata/tree/master/templates).
* [x] Read the [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/) and validated the metadata.
* [x] Read the [Quick Start Guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

### Merge Request Setup

* [x] The title follows `New app: app name`.
* [x] The fork is public and the app branch is not protected.
* [x] Read the [Git guide](https://gitlab.com/fdroid/wiki/-/wikis/Tips-for-fdroiddata-contributors/Git-Usage). No rebase is needed without conflicts.
* [x] Related fdroiddata and RFP issues have been checked; none were found for ViBRo Navigator.
* [x] Only one app is submitted in this MR.

### Metadata

* [x] Metadata is in `metadata/vibro.navigator.foss.yml`.
* [x] Metadata is valid YAML and passes schema validation.
* [x] Metadata uses LF line endings.
* [x] Descriptions, changelogs, icons, and screenshots remain in the upstream repository. This MR changes only the app's YAML recipe.
* [x] Releases are tagged. Automatic tag discovery is intentionally disabled: numeric GitHub tags can also be marked as prereleases. Upstream automation submits only stable releases, preventing prereleases from entering F-Droid through tag discovery.
* [x] The issue tracker and upstream author profile provide contact routes.
* [x] `AuthorName` is supplied.
* [x] No external source repositories or srclibs are bundled. BRouter is installed separately.
* [x] Reproducible builds are enabled using `Binaries` and `AllowedAPKSigningKeys`, retaining the upstream signing identity. F-Droid must reproduce the APK before publishing it.
* [x] ABI splitting is not applicable: the app has no native libraries and its FOSS APK is approximately 1.6 MB.
* [x] Only the latest release is included.
* [x] No disabled builds are included.
* [x] The build's `commit` is a full immutable hash.

### Pipeline

* [x] All checks in the [submission pipeline]({{PIPELINE_URL}}) pass.
* [x] Reports-tab warnings and errors have been fixed or explained. The informational listing inventory describes the upstream Fastlane content.
* [x] F-Droid CI runner guidance has been read; no paid runner configuration is required for this submission.

## Build details

The build runs Gradle from the repository root, selects only the `fdroid` flavor, and specifies the unsigned APK under `app/build/outputs/apk/fdroid/release/`. The recipe removes the development-only `docs-tools` directory from the checkout before scanning; it is website tooling and is not an Android dependency. Local signing credentials are not supplied to F-Droid.

The APK permission report reflects navigation features: coarse/fine location provides GPS guidance; Internet access supports optional destination searches; microphone access supports user-initiated voice input; legacy storage permissions are limited to Android 9 and earlier for navigation data and files. Foreground-service, notification, and wake-lock permissions support ongoing, screen-off guidance and vibration notifications. Route calculation with downloaded BRouter data remains offline.

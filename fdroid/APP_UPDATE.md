Update ViBRo Navigator FOSS to [{{RELEASE_REF}}](https://github.com/damianofalcioni/ViBRo-Navigator/releases/tag/{{RELEASE_REF}}), version {{VERSION_NAME}} ({{VERSION_CODE}}).

## Required

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy); this recipe builds the FOSS flavor.
* [x] Related fdroiddata and RFP issues have been checked; no additional issue is closed by this update.
* [x] Builds with `fdroid build`; the [submission pipeline]({{PIPELINE_URL}}) passes.

## Strongly Recommended

* [x] The upstream source contains Fastlane descriptions, images, translations, and changelogs.
* [x] The release is tagged. Automatic tag discovery remains disabled because numeric GitHub tags can also be prereleases; upstream automation submits stable releases only.

## Suggested

* [x] No external source repositories or srclibs are bundled.
* [x] ABI splitting is not applicable: the app has no native libraries and its FOSS APK is small.

The recipe retains the upstream signing identity through `Binaries` and `AllowedAPKSigningKeys`; publication requires a reproducible rebuild.

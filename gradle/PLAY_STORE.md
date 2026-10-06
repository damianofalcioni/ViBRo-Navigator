# Google Play publishing

Published stable GitHub Releases trigger `.github/workflows/build-apk.yml`.
It checks out the tag, runs lint/tests, builds signed release APKs and the `gplay`
AAB, and attaches all three to the release. Only after that build succeeds do
Google Play upload and F-Droid submission start as separate jobs. Prereleases,
pushes, pull requests, and manual build dispatches do not submit to either store.

The Play job uses the [dedicated Play upload action](https://github.com/r0adkll/upload-google-play),
pinned to a commit. It submits the already-built AAB as a **production release
with a full rollout** (`tracks: production`, `status: completed`);
CI does not enable the Gradle publisher. The release tag must be `v<versionName>`.

## GitHub Actions setup

Configure these repository secrets before publishing a release:

- `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
  `ANDROID_KEY_PASSWORD`: the existing release signing secrets.
- `PLAY_SERVICE_ACCOUNT_JSON`: the **JSON contents** of a service account key
  with access to `vibro.navigator` and permission to release to production in Play Console.
- `GITLAB_NAMESPACE`, `GITLAB_TOKEN`: required for the F-Droid submission job;
  see `fdroid/SUBMISSION.md`.

Create the Play Console app and perform the first AAB upload manually, as
described below, and ensure the app/account has production access. Publishing
a stable GitHub Release submits a full production rollout; Google's review and
policy checks still apply. If managed publishing is enabled in Play Console,
approved changes remain held until you publish them there. Turn managed
publishing off if approved releases should go live automatically. See
[Google's publishing controls](https://support.google.com/googleplay/android-developer/answer/9859654).
To change the automatic destination/status, edit the Play job's explicit
`tracks: production` and `status: completed` settings.

Automated release notes come from the Fastlane changelog matching the built
versionCode. Review them before publishing: Play requires 1–500 characters,
and the build fails on empty or oversized notes rather than truncating them.
The upload does not change the store listing or policy declarations.

To retry a failed publishing job, use GitHub's **Re-run failed jobs**. A successful
Play upload already consumed that versionCode; rerunning the upload may fail
with a duplicate version error. F-Droid submission can also be rerun through
its manual workflow for the same published tag.

## Manual Gradle fallback

The opt-in `gradle/play-publish.gradle` configuration uses Gradle Play Publisher
4.1.1. Publishing is enabled only for `gplayRelease`; normal builds and F-Droid
builds do not load the plugin. The default destination is an **internal-track
draft** using the Google Play Android App Bundle.

### One-time setup

1. Create `vibro.navigator` in Play Console and upload the first signed AAB
   manually. The publishing API cannot create the initial app.
2. Enable the Google Play Android Developer API in your Google Cloud project.
3. Create a service account, invite its email in Play Console, and grant it
   access to this app and the tracks you intend to release to.
4. Store its JSON key outside the checkout. Set the local `PLAY_SERVICE_ACCOUNT_JSON`
   to the file's absolute path. Alternatively, set
   `ANDROID_PUBLISHER_CREDENTIALS` to the JSON contents.
5. Set the existing signing environment variables:
   `ANDROID_KEYSTORE_PATH` (absolute path), `ANDROID_KEYSTORE_PASSWORD`,
   `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Use the upload key that
   Play Console expects. These are separate from the service account key.

See the [publisher setup documentation](https://github.com/Triple-T/gradle-play-publisher#prerequisites)
for service account and first-upload details.

### Publish a release manually

Prepare the version with `prepareRelease` and review its changelogs first.
Play requires a new versionCode for each new upload; the publisher deliberately
keeps the versionCode from `app/build.gradle`.

PowerShell, with the signing variables already set:

```powershell
$env:PLAY_SERVICE_ACCOUNT_JSON = 'D:\keys\play-service-account.json'
.\gradlew.bat -PplayPublishing :app:publishGplayReleaseBundle
```

This builds and uploads the signed `gplay` AAB and leaves an internal-track draft
in Play Console. Review it there and start the rollout when ready. To explicitly
publish a completed release instead:

```powershell
.\gradlew.bat -PplayPublishing :app:publishGplayReleaseBundle --track internal --release-status completed
```

Use `--track production` for a production release when the app/account is
eligible. Play Console store listing, policy declarations, testing requirements,
and review still apply.

On Linux/macOS, use `./gradlew` with the same arguments. To inspect the tasks
without uploading anything:

```powershell
.\gradlew.bat -PplayPublishing :app:tasks --group publishing
.\gradlew.bat -PplayPublishing :app:publishGplayReleaseBundle --dry-run
```

Release notes can be supplied in
`app/src/gplay/play/release-notes/en-US/default.txt` (maximum 500 characters).
The existing Fastlane changelogs remain the F-Droid metadata source; GPP does
not automatically import them. `publishGplayReleaseBundle` uploads the bundle
and release notes, while store-listing changes use the separate GPP listing
tasks.

F-Droid's signed APK must use the `fdroid` flavor. If Play App Signing uses a
different app signing certificate from your upstream APK certificate, installed
Play and F-Droid copies cannot update one another just because their upload
artifacts used the same keystore.

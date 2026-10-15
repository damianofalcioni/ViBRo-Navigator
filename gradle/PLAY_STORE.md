# Google Play publishing

Published GitHub Releases, including prereleases, trigger `.github/workflows/build-apk.yml`.
It checks out the tag, runs lint/tests, builds signed release APKs and the `gplay`
AAB, and attaches only the signed FOSS (`fdroid`) APK to the GitHub Release.
The Google Play artifacts remain CI artifacts; the AAB is passed to the Play
publishing job. Google Play upload starts only after that build and FOSS APK
attachment succeed. Stable releases additionally start F-Droid submission as
an independent job. Pushes, pull requests, and manual build dispatches do not
submit to either store.

The Play job uses the [dedicated Play upload action](https://github.com/r0adkll/upload-google-play),
pinned to a commit. It submits the already-built AAB with `status: completed`
to the track selected for the GitHub release type:

| GitHub release | Google Play destination | F-Droid metadata submission |
| --- | --- | --- |
| GitHub prerelease or a `-beta` / `-beta.N` tag | `alpha` by default, or the repository variable `PLAY_PRERELEASE_TRACK` | No |
| Stable release | `production`, full rollout | Yes |

The binary release job does not enable the Gradle publisher; the separate
listing workflow uses it. Android version names stay numeric,
for example `1.0.1` with versionCode `1000001`. The Git tag can be `v1.0.1-beta`
or `v1.0.1-beta.1` to select testing automatically, regardless of GitHub's
prerelease checkbox. A plain `v1.0.1` tag also selects testing when published
as a GitHub prerelease; otherwise it selects production.

## Initial prerequisites

Complete the relevant setup before publishing the first GitHub Release: that
event immediately starts the automatic store jobs after the signed build.
Both stores need their own credentials, in addition to the existing Android
signing secrets.

### Google Play API credentials

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select or
   create a project.
2. [Enable the Google Play Android Developer API](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com).
3. Under IAM & Admin > Service accounts, create an account such as
   `vibro-publisher`. Its publishing access is granted in Play Console.
4. In [Play Console](https://play.google.com/console/), use Users and permissions
   to invite the service account's email. Restrict access to `vibro.navigator`
   and enable **View app information (read-only)** plus **Release apps to
   testing tracks** for prereleases. For stable releases also enable
   **Release to production, exclude devices, and use Play App Signing**.
   For listing edits also enable **Manage store presence**. While the app is
   a draft, enable **Edit and delete draft apps**; without it this app accepted
   staged listing uploads but rejected the final commit with HTTP 403.
   Save the invitation. See [Google's API setup](https://developers.google.com/android-publisher/getting_started)
   and [permission reference](https://support.google.com/googleplay/android-developer/answer/9844686).
5. Open the service account in Cloud Console, select Keys > Add key > Create
   new key, choose JSON, and download the file. See
   [Google's key instructions](https://docs.cloud.google.com/iam/docs/keys-create-delete).
6. Use the complete, unencoded JSON document as the GitHub secret
   `PLAY_SERVICE_ACCOUNT_JSON`. Keep the downloaded key outside the repository.
   This secret is separate from the Android upload keystore.

### First upload and Play Console readiness

Prepare the shared listing in `fastlane/metadata/android/`: app name, short and
full descriptions, translations, 512×512 icon, 1024×500 feature graphic, and
phone screenshots. See [the shared listing guide](../fastlane/README.md) for
locale folders, limits, manual entry, local Gradle commands, and the **Google
Play Listing** GitHub workflow. The release workflow does not upload listing
text or graphics. For API listing edits, also grant **Manage store presence**
to the service account, plus **Edit and delete draft apps** while the app is
still a draft. The supplied website-hero-style feature graphic is
included as `fastlane/metadata/android/en-US/images/featureGraphic.jpg`.

If `vibro.navigator` already has an uploaded release on the internal testing
track (or another release track), the first-upload prerequisite is complete,
even if the app has never been released to production. Skip the initial
manual build/upload below. Production eligibility remains separate: complete
the store listing, policy declarations, Play App Signing setup, and any
testing/production-access requirements before the automatic production rollout.
Internal testing does not replace closed-testing requirements that may apply
to newer personal developer accounts; check the app's Production page and
[Google's testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).

An existing internal release does not necessarily remove the app's draft
status. If a completed alpha upload fails with **Only releases with status
draft may be created on draft app**, save the alpha release with
`--track alpha --release-status draft`. Finish the outstanding Dashboard/app
content tasks, configure closed-test countries and testers, then review and
roll out that existing draft in Play Console. Do not upload its versionCode
again. The initial closed-test rollout may require this Console step before
subsequent completed releases can be automated. A saved draft does not start
the required testing period. See [Google's rollout guide](https://support.google.com/googleplay/android-developer/answer/9859348).

For a new app without any uploaded release, create it in Play Console and
upload its first signed `gplay` AAB manually. The publishing API cannot create
the initial app/package. The
[upload action prerequisites](https://github.com/r0adkll/upload-google-play#inputs)
require this first upload.

With the signing environment described under [Manual Gradle fallback](#manual-gradle-fallback)
configured, build the initial bundle from the repository root:

```powershell
.\gradlew.bat :app:bundleGplayRelease
```

Upload `app/build/outputs/bundle/gplayRelease/app-gplay-release.aab` in Play
Console. If a stable GitHub Release already built successfully, the same file
is also available inside its workflow's `play-release-bundle` artifact. Use
the `gplay` bundle for `vibro.navigator`; the public FOSS APK has a different
application ID and cannot be used for this upload.

An upload consumes that bundle's versionCode, including uploads to internal
testing. If `1.0.0` (`1000000`) is already in internal testing, promote or
select that existing bundle for production in Play Console when eligible;
do not rerun the workflow's upload of the same code. See
[Google's release instructions](https://support.google.com/googleplay/android-developer/answer/9859348).
Use a new version such as `1.0.1` (`1000001`) for the next new bundle upload.
If the existing internal-test codes are lower than `1000000` and that code
has never been uploaded, the workflow can upload the current `1.0.0` bundle
once the service-account credentials and production prerequisites are ready.

Publishing a stable GitHub Release submits a full production rollout; Google's
review and policy checks still apply. If managed publishing is enabled in
Play Console, approved changes remain held until you publish them there.
Turn managed publishing off if approved releases should go live automatically.
See [Google's publishing controls](https://support.google.com/googleplay/android-developer/answer/9859654).
The stable path always selects `production`; `PLAY_PRERELEASE_TRACK` does not
change that destination. See below for the testing-track setup.

### Set up the prerelease testing track

In Play Console, configure the closed-testing track and its tester list or
Google Group before publishing a GitHub prerelease. The default `alpha`
identifier targets the standard closed-testing track. If the configured
closed track has a custom API identifier, add a repository Actions variable
named `PLAY_PRERELEASE_TRACK` with that identifier under Settings > Secrets
and variables > Actions > Variables > New repository variable. Use one track
identifier; the workflow rejects production and lists of tracks for prereleases.

Google's API identifier `beta` means **open testing**, not the closed test
required for production eligibility. Set `PLAY_PRERELEASE_TRACK=beta` only
when open testing is available and intended. For the required closed test,
use `alpha` or the actual custom closed-track identifier. See
[Google's track reference](https://developers.google.com/android-publisher/tracks).

For personal developer accounts created after November 13, 2023, keep at least
12 testers opted into the closed test continuously for 14 days, then apply for
production access. Publishing to a track does not recruit or opt in testers,
and production access still requires Google's approval. See
[Google's testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).

### F-Droid credentials

Create a GitLab `fdroiddata` fork and an access token as described in
[F-Droid initial prerequisites](../fdroid/SUBMISSION.md#initial-prerequisites).
Its namespace and token supply `GITLAB_NAMESPACE` and `GITLAB_TOKEN` below.

### Add the secrets to GitHub

Open the repository's Settings > Secrets and variables > Actions, select the
Secrets tab, and choose New repository secret for each entry. For this repo,
use [Actions secrets](https://github.com/damianofalcioni/ViBRo-Navigator/settings/secrets/actions).
Enter the exact name and value, then select Add secret. Store these as
repository secrets, not Actions variables or local file paths. See
[GitHub's instructions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets).

| Secret name | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 contents of the existing Android upload keystore. |
| `ANDROID_KEYSTORE_PASSWORD` | Password for that keystore. |
| `ANDROID_KEY_ALIAS` | Signing key alias within that keystore. |
| `ANDROID_KEY_PASSWORD` | Password for that signing key. |
| `PLAY_SERVICE_ACCOUNT_JSON` | Complete downloaded service-account JSON, including its private key; no Base64 encoding. |
| `GITLAB_NAMESPACE` | Username or group path owning the `fdroiddata` fork, without a URL or `/fdroiddata` suffix. |
| `GITLAB_TOKEN` | GitLab personal access token with `api` and `write_repository` scopes. |

Retain the existing signing secrets and signing identity when adding the store
credentials. Keep JSON keys and tokens out of Git. Replace the corresponding
GitHub secret when rotating a key or before a GitLab token expires.

Check that all seven names are listed before publishing a stable release.
Prereleases need the four signing secrets and `PLAY_SERVICE_ACCOUNT_JSON`;
the two GitLab secrets are required only for stable F-Droid submission.
With GitHub CLI:

```powershell
gh secret list --repo damianofalcioni/ViBRo-Navigator
```

This confirms the secret names, not whether the credentials or permissions
are valid. Missing store credentials can fail their jobs even when the
GitHub Release and its signed FOSS APK are already public.

## Release preparation and retries

### Publish a beta/prerelease

1. Prepare a new numeric app version, for example
   `.\gradlew.bat prepareRelease --release-version=1.0.1 --tag=v1.0.1-beta`, and review its
   generated notes. Every new uploaded bundle needs an unused, increasing
   versionCode; the example uses `1000001`.
2. Commit the changes, create and push `v1.0.1-beta`, and publish a GitHub
   Release. Selecting **Set as a pre-release** makes its testing status
   visible immediately; the workflow also recognizes `-beta` and `-beta.N`
   tags automatically and marks the release as a prerelease when attaching
   the APK. A title containing "beta" alone does not select testing.
   Alternatively, keep the plain numeric tag `v1.0.1` and select the GitHub
   prerelease checkbox. The next new beta or stable upload can use `1.0.2`
   and versionCode `1000002`; the tag suffix does not change the version code.
   F-Droid's independent tag-based update discovery is disabled; its metadata
   is submitted only by the stable-release workflow, so numeric prerelease
   tags are not selected there either.
3. The workflow runs its checks, builds signed artifacts, attaches only the
   FOSS APK while marking beta/prereleases as such on GitHub, and uploads the Play
   AAB to `alpha` or `PLAY_PRERELEASE_TRACK`. The AAB stays in the workflow's
   `play-release-bundle` artifact. No official F-Droid submission runs.
4. Have testers join through the closed-test opt-in link and collect feedback.
   When production access is granted, promote the tested bundle in Play
   Console. Do not upload that same code again by treating it as a fresh
   stable upload; changing a GitHub prerelease flag is not a Play promotion.

The existing `release: published` trigger covers both stable releases and
prereleases, including prereleases published from drafts. See
[GitHub's release event documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#release).

### Release notes and retries

Automated release notes come from the Fastlane changelog matching the built
versionCode. Review them before publishing: Play requires 1–500 characters,
and the build fails on empty or oversized notes rather than truncating them.
Release notes intentionally use **English (`en-US`) only** for both stable and
beta releases. The 18 translated store listings do not require release-note
translations; Google's [release API example](https://developers.google.com/android-publisher/tracks#specifying_release_notes)
supplies just `en-US`. For manual Console entry, keep the filled `<en-US>` block
and remove unused language blocks containing the template prompts. Do not
publish the Italian placeholder instructions as actual release notes.
The upload does not change the store listing or policy declarations.

After adding missing credentials, open the original release workflow run in
GitHub Actions and use **Re-run failed jobs**, provided the Play versionCode
has not already been uploaded manually or automatically. A successful upload
consumes that code; finish or promote the existing release in Play Console,
or prepare a new semantic version for a new upload.

To retry F-Droid alone, use **Submit F-Droid Metadata > Run workflow** with
`release_ref` set to the existing published tag, such as `v1.0.0`, and
`fdroiddata_branch` set to `vibro.navigator.foss`. This does not rerun the
Google Play upload. Do not create another GitHub Release or move its tag just
to retry store submission.

## Manual Gradle fallback

For listing text and images only, use the [shared listing guide](../fastlane/README.md#local-gradle-commands):
`preparePlayListing` validates/generates files without Google API calls;
`-PplayPublishing -PplaySubmitForReview=true :app:publishPlayListing` uploads
through GPP without building a binary or needing signing credentials. The
separate **Google Play Listing** workflow uses these same tasks and defaults to
preparation only. Publishing explicitly permits Google review submission; this
app's API rejected holding the edit back from review. No Fastlane executable,
Ruby, or Python is required.

The opt-in `gradle/play-publish.gradle` configuration uses Gradle Play Publisher
4.1.1. Publishing is enabled only for `gplayRelease`; normal builds and F-Droid
builds do not load the plugin. The default destination is an **internal-track
draft** using the Google Play Android App Bundle.

### One-time setup

1. Complete the Google Play credential and first-upload sections of the
   [initial prerequisites](#initial-prerequisites). Grant testing-track release
   permission as well if using the fallback's default internal track.
2. Store the JSON key outside the checkout. Set the local `PLAY_SERVICE_ACCOUNT_JSON`
   to the file's absolute path. Alternatively, set
   `ANDROID_PUBLISHER_CREDENTIALS` to the JSON contents. This local file-path
   setting differs from the GitHub secret, which contains the JSON itself.
3. Set the existing signing environment variables:
   `ANDROID_KEYSTORE_PATH` (absolute path), `ANDROID_KEYSTORE_PASSWORD`,
   `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Use the upload key that
   Play Console expects. These are separate from the service account key.

See the [publisher setup documentation](https://github.com/Triple-T/gradle-play-publisher#prerequisites)
for service account and first-upload details.

### Publish a release manually

Prepare the version with `prepareRelease` and review its changelogs first.
The task derives `versionCode` as `major * 1000000 + minor * 1000 + patch`
(`1.0.0` becomes `1000000`), with minor and patch each limited to `0..999`.
Use a new semantic version for a new upload; do not increment the code separately.
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
not automatically import them. Shared listing text/images are converted by
`gradle/play-listing.gradle` into build-directory GPP resources while the
canonical files remain compatible with F-Droid. `publishGplayReleaseBundle` uploads the bundle
and release notes, while store-listing changes use the separate GPP listing
tasks.

F-Droid's signed APK must use the `fdroid` flavor and application ID
`vibro.navigator.foss`. Google Play keeps `vibro.navigator`, so both versions
can be installed together and update independently. Each package must retain
its own signing identity for future updates; using the same upload keystore
does not make the packages interchangeable.

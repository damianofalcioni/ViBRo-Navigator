# F-Droid Submission Notes

This directory contains a draft `fdroiddata` metadata file for
`vibro.navigator.foss`.

The `fdroid` release launcher name is `ViBRo Navigator FOSS`; Google Play
keeps `vibro.navigator` / `ViBRo Navigator`. Both can be installed together,
with separate app data, permissions, and updates. F-Droid debug builds use
`vibro.navigator.foss.debug` / `ViBRo Navigator FOSS Debug`.

Older F-Droid builds using `vibro.navigator` do not update into the new FOSS
package. Export a database backup from the old installation and import it
into FOSS to transfer settings, history, and saved routes. Grant permissions
and folder access again for the new installation.

## Initial prerequisites

Complete the GitLab fork and credentials setup before publishing a stable
GitHub Release, because that release automatically calls the submission
workflow after attaching the signed FOSS APK.

### Create the GitLab fork

1. Sign in to [GitLab](https://gitlab.com/) and open
   [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Select Fork, choose your user or group namespace, and keep the project slug
   `fdroiddata`. Ensure the fork includes the upstream default branch,
   `master`, which the workflow uses as its starting point. See
   [GitLab's fork instructions](https://docs.gitlab.com/user/project/repository/forking_workflow/).
3. Set `GITLAB_NAMESPACE` to the namespace portion of the fork's URL. For
   `https://gitlab.com/example-user/fdroiddata`, the value is `example-user`;
   for `https://gitlab.com/example-group/mobile/fdroiddata`, it is
   `example-group/mobile`. Use the path, not a display name or the full URL.
   Although this value is not confidential, the workflow expects it as a secret.

### Create the GitLab token

1. Open your avatar > Edit profile > Access > Personal access tokens.
2. Choose Generate token > Legacy token if that choice is offered. Give it a
   name such as `vibro-fdroid-publisher` and an expiration date.
3. Select `api` and `write_repository` scopes. The workflow uses the token
   for Git pushes to your fork and API calls that create an upstream merge
   request. The account owning the token must have access to write to the fork.
4. Generate the token and copy it immediately; it cannot be viewed again.
   This value becomes `GITLAB_TOKEN`. See
   [GitLab's token instructions](https://docs.gitlab.com/user/profile/personal_access_tokens/).

### Add the repository secrets

Follow [Add the secrets to GitHub](../gradle/PLAY_STORE.md#add-the-secrets-to-github)
to add `GITLAB_NAMESPACE` and `GITLAB_TOKEN` as repository Actions secrets.
Retain the existing `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` signing secrets. Keep the
token out of Git and update its GitHub secret when rotating it or before expiry.

Google Play additionally requires `PLAY_SERVICE_ACCOUNT_JSON` and an initial
app-bundle upload. An existing internal-test release already satisfies that
upload prerequisite; production eligibility and unused version codes still
need checking. See [Google Play initial prerequisites](../gradle/PLAY_STORE.md#initial-prerequisites).
The store jobs run independently after the shared build. Missing GitLab
credentials can prevent the reusable F-Droid workflow from starting even
when the GitHub Release's build and APK attachment succeeded.

## Prepare and submit a release

Before opening the official F-Droid merge request, complete these steps:

1. Push the repository to a public GitHub URL.
2. Enable GitHub Pages from the repository `docs/` folder so the public
   store-document URLs are live.
3. Prepare local release metadata without creating a commit or tag:
   `.\gradlew.bat prepareRelease --release-version=1.0.0`.
   This derives Android `versionCode` as `major * 1000000 + minor * 1000 + patch`;
   `1.0.0` uses `1000000`. Minor and patch must each be `0..999`.
4. Review the console changelog summary, `docs/CHANGELOG/index.html`, and
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
5. Commit the current F-Droid prep changes.
6. Create and push a release tag matching `versionName`, for example `v1.0.0`.
   Publish a stable GitHub Release for that tag. The `Build and Publish Release`
   workflow checks/builds the tagged source and attaches only the signed FOSS
   (`fdroid`) APK. Google Play artifacts remain in CI for Play publishing.
   After attachment succeeds, it starts both Google Play upload and F-Droid
   submission automatically. A tag push alone does not submit to either store.
7. Confirm the release workflow's F-Droid readiness and submission jobs pass.
   They reuse the release build's APK, verify the public release APK, and then
   prepare the merge request. For retries, the manual `Submit F-Droid Metadata`
   workflow runs readiness before touching your `fdroiddata` fork.
8. Confirm `fdroid/vibro.navigator.foss.yml` points at the release tag in its
   `commit` field; `prepareRelease` writes this automatically. For manual
   submission, replace `REPLACE_WITH_APKSIGNER_SHA256_FINGERPRINT` with the
   lower-case SHA-256 certificate fingerprint from
   `apksigner verify --print-certs app-fdroid-release.apk`. The automated
   submission fills this from the verified readiness APK and pins the full commit.
9. For manual submission instead of the automatic workflow, copy the completed
   metadata into your `fdroiddata` fork as `metadata/vibro.navigator.foss.yml`.
10. Run the standard validation flow in the F-Droid build container:
   - `fdroid readmeta`
   - `fdroid rewritemeta vibro.navigator.foss`
   - `fdroid checkupdates --allow-dirty vibro.navigator.foss`
   - `fdroid lint vibro.navigator.foss`
   - `fdroid build vibro.navigator.foss`

Useful repository files already prepared upstream:

- `fastlane/metadata/android/<locale>/...` (English fallback: `en-US`;
  [shared listing guide](../fastlane/README.md) covers translations and graphics)
- `docs/`
- `.github/workflows/fdroid-ready.yml`
- `.github/workflows/fdroid-submit.yml`

Notes:

- The `Build and Publish Release` workflow is the normal per-commit CI gate and requires the
  Android signing secrets. The `F-Droid Readiness` workflow is reserved for
  manual maintainer checks and the automatic pre-submit gate in
  `Submit F-Droid Metadata`. Its separate tag-push trigger has been removed.
- GitHub prereleases attach only the signed FOSS APK and upload the Google Play
  bundle to a testing track (`alpha` by default, or `PLAY_PRERELEASE_TRACK`).
  They do not submit official F-Droid metadata. See the
  [Play prerelease guide](../gradle/PLAY_STORE.md#publish-a-betaprerelease).
  Beta tags can use `v<versionName>-beta` or `v<versionName>-beta.N`; numeric
  tags also support GitHub's prerelease checkbox. Because a numeric tag
  alone does not identify a stable release, `AutoUpdateMode` and
  `UpdateCheckMode` are `None`; stable updates come from the submission
  workflow instead of F-Droid's independent tag discovery. Keep those fields
  disabled when submitting the rendered metadata. See the
  [F-Droid metadata reference](https://f-droid.org/docs/Build_Metadata_Reference/#autoupdatemode).
- The Gradle build must run from the repository root, not `app/`, because the
  wrapper and `settings.gradle` live at the top level.
- `local.properties` is excluded in the draft recipe because it is machine-
  specific and should not be present in F-Droid builds.
- `Binaries` and `AllowedAPKSigningKeys` request exclusively your signed
  upstream APK. Publish the `fdroid` flavor under the exact filename
  `app-fdroid-release.apk` at every `v<versionName>` GitHub Release. The
  `gplay` APK contains Google dependencies and cannot substitute for it.
- The F-Droid `WebSite` field should point to the GitHub Pages app site, while
  `SourceCode` and `IssueTracker` should point to GitHub.

## GitHub Action automation

The release workflow calls the reusable `Submit F-Droid Metadata` workflow
after the signed FOSS APK has been attached. It performs these steps:

- run the `F-Droid Readiness` gate for the requested `release_ref`
- verify that the public release APK has the expected signature, package and version
- render the final `metadata/vibro.navigator.foss.yml`
- push it to your GitLab `fdroiddata` fork
- create or reuse a merge request against `fdroid/fdroiddata`

The [initial prerequisites](#initial-prerequisites) explain how to obtain and
add the GitLab and signing secrets. Play upload and F-Droid submission run
independently after the shared build succeeds, so failure of one does not
prevent the other.

Workflow inputs:

- `release_ref`: the published `v<versionName>` tag with the signed APK attached;
  rendering reads the version from that tag's source, even if the default branch is newer
- `fdroiddata_branch`: the branch name in your fork, normally
  `vibro.navigator.foss`
- `release_artifact`: reusable-workflow input supplied by the release caller
  to reuse its tested signed APKs. Leave it empty for standalone manual runs,
  which check and build the release source themselves.

### Retry after adding credentials

To retry only F-Droid, open GitHub Actions > Submit F-Droid Metadata > Run
workflow. Set `release_ref` to the existing published tag (for example,
`v1.0.0`) and keep `fdroiddata_branch` as `vibro.navigator.foss` unless using
another branch in your fork. The workflow rechecks the public release APK
and opens or reuses the merge request; a new release or tag is unnecessary.

Use **Re-run failed jobs** on the original release run only if every failed
store job should be retried. In particular, if the Play bundle was already
uploaded manually or automatically, use the F-Droid-only retry to avoid
uploading the same Play versionCode again. See
[Play retry guidance](../gradle/PLAY_STORE.md#release-preparation-and-retries).

This does not fully automate official inclusion. F-Droid maintainers still
review the merge request and perform the final reproducible rebuild/publish steps on
their own infrastructure.

## Using your signature

[F-Droid supports publishing upstream-signed APKs](https://f-droid.org/en/docs/Reproducible_Builds/#exclusively-publishing-upstream-developer-signed-apks).
It downloads the APK from `Binaries`, checks its certificate against
`AllowedAPKSigningKeys`, and independently rebuilds the `fdroid` flavor from
source. Publication requires the rebuild to match the upstream APK apart from
its signature. A mismatch skips that release; this recipe does not fall back
to an F-Droid signing key.

Before official submission, verify reproducibility in the F-Droid build
environment using the release source and published APK. Keep the Gradle, AGP,
JDK and Android build-tools versions aligned with the upstream release build.
Build the published APK from a clean checkout of the tagged commit, since AGP
embeds version-control information in release artifacts. The wrapper's daemon
JVM criteria currently select JDK 21, despite Java 17 source/target compatibility.
F-Droid's rebuild must run without `ANDROID_KEYSTORE_*` or `ANDROID_KEY_*`
signing variables. The readiness gate checks signing and release identity;
passing it does not prove binary reproducibility across build environments.

The [metadata reference](https://f-droid.org/en/docs/Build_Metadata_Reference/#binaries)
documents the versioned URL substitutions. `%v` is `versionName`; this
repository's release URL convention requires a leading `v` in the tag.
Keep the same signing key for future releases so users can install updates.

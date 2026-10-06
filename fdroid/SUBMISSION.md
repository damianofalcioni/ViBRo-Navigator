# F-Droid Submission Notes

This directory contains a draft `fdroiddata` metadata file for
`vibro.navigator`.

Before opening the official F-Droid merge request, complete these steps:

1. Push the repository to a public GitHub URL.
2. Enable GitHub Pages from the repository `docs/` folder so the public
   store-document URLs are live.
3. Prepare local release metadata without creating a commit or tag:
   `.\gradlew.bat prepareRelease --release-version=0.1.16`.
4. Review the console changelog summary, `docs/CHANGELOG/index.html`, and
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
5. Commit the current F-Droid prep changes.
6. Create and push a release tag matching `versionName`, for example `v0.1.16`.
   Publish a stable GitHub Release for that tag. The `Build and Publish Release`
   workflow checks/builds the tagged source and attaches the signed APKs and AAB.
   After attachment succeeds, it starts both Google Play upload and F-Droid
   submission automatically. A tag push alone does not submit to either store.
7. Confirm the release workflow's F-Droid readiness and submission jobs pass.
   They reuse the release build's APK, verify the public release APK, and then
   prepare the merge request. For retries, the manual `Submit F-Droid Metadata`
   workflow runs readiness before touching your `fdroiddata` fork.
8. Confirm `fdroid/vibro.navigator.yml` points at the release tag in its
   `commit` field; `prepareRelease` writes this automatically. For manual
   submission, replace `REPLACE_WITH_APKSIGNER_SHA256_FINGERPRINT` with the
   lower-case SHA-256 certificate fingerprint from
   `apksigner verify --print-certs app-fdroid-release.apk`. The automated
   submission fills this from the verified readiness APK and pins the full commit.
9. For manual submission instead of the automatic workflow, copy the completed
   metadata into your `fdroiddata` fork as `metadata/vibro.navigator.yml`.
10. Run the standard validation flow in the F-Droid build container:
   - `fdroid readmeta`
   - `fdroid rewritemeta vibro.navigator`
   - `fdroid checkupdates --allow-dirty vibro.navigator`
   - `fdroid lint vibro.navigator`
   - `fdroid build vibro.navigator`

Useful repository files already prepared upstream:

- `fastlane/metadata/android/en-US/...`
- `docs/`
- `.github/workflows/fdroid-ready.yml`
- `.github/workflows/fdroid-submit.yml`

Notes:

- The `Build and Publish Release` workflow is the normal per-commit CI gate and requires the
  Android signing secrets. The `F-Droid Readiness` workflow is reserved for
  manual maintainer checks and the automatic pre-submit gate in
  `Submit F-Droid Metadata`. Its separate tag-push trigger has been removed.
- GitHub prereleases build and attach artifacts but do not submit to either store.
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
after signed artifacts have been attached. It performs these steps:

- run the `F-Droid Readiness` gate for the requested `release_ref`
- verify that the public release APK has the expected signature, package and version
- render the final `metadata/vibro.navigator.yml`
- push it to your GitLab `fdroiddata` fork
- create or reuse a merge request against `fdroid/fdroiddata`

Required GitHub Actions secrets:

- `GITLAB_NAMESPACE`: your GitLab username or group that owns the `fdroiddata`
  fork
- `GITLAB_TOKEN`: a GitLab personal access token with `api` and
  `write_repository`
- The existing `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` signing secrets are also
  required by the release build and manual readiness builds.

Google Play additionally needs `PLAY_SERVICE_ACCOUNT_JSON`; see
`gradle/PLAY_STORE.md`. Play upload and F-Droid submission run independently
after the shared build succeeds, so failure of one does not prevent the other.

Workflow inputs:

- `release_ref`: the published `v<versionName>` tag with the signed APK attached;
  rendering reads the version from that tag's source, even if the default branch is newer
- `fdroiddata_branch`: the branch name in your fork, normally
  `vibro.navigator`
- `release_artifact`: reusable-workflow input supplied by the release caller
  to reuse its tested signed APKs. Leave it empty for standalone manual runs,
  which check and build the release source themselves.

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

# F-Droid Submission Notes

This directory contains a draft `fdroiddata` metadata file for
`vibro.navigator`.

Before opening the official F-Droid merge request, complete these steps:

1. Push the repository to a public GitHub URL.
2. Enable GitHub Pages from the repository `docs/` folder so the public
   store-document URLs are live.
3. Prepare local release metadata without creating a commit or tag:
   `.\gradlew.bat prepareRelease --release-version=0.1.0`.
4. Review the console changelog summary, `CHANGELOG.md`, and
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
5. Commit the current F-Droid prep changes.
6. Create and push a release tag matching `versionName`, for example `v0.1.0`.
7. Confirm the `F-Droid Readiness` workflow passes for that tag, or use the
   `Submit F-Droid Metadata` workflow, which runs readiness automatically
   before touching your `fdroiddata` fork.
8. Confirm `fdroid/vibro.navigator.yml` points at the release tag in its
   `commit` field; `prepareRelease` writes this automatically.
9. Copy `fdroid/vibro.navigator.yml` into your `fdroiddata` fork as
   `metadata/vibro.navigator.yml`.
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

- The `Build APK` workflow is the normal per-commit CI gate and requires the
  Android signing secrets. The `F-Droid Readiness` workflow is reserved for
  manual maintainer checks, pushed `v*` release tags, and the automatic
  pre-submit gate in `Submit F-Droid Metadata`.
- The Gradle build must run from the repository root, not `app/`, because the
  wrapper and `settings.gradle` live at the top level.
- `local.properties` is excluded in the draft recipe because it is machine-
  specific and should not be present in F-Droid builds.
- `AllowedAPKSigningKeys` is optional and should only be added if you later
  publish a signed upstream APK that you want F-Droid to verify against.
- The F-Droid `WebSite` field should point to the GitHub Pages app site, while
  `SourceCode` and `IssueTracker` should point to GitHub.

## GitHub Action automation

The `Submit F-Droid Metadata` workflow can automate the part that is safe to
automate:

- run the `F-Droid Readiness` gate for the requested `release_ref`
- render the final `metadata/vibro.navigator.yml`
- push it to your GitLab `fdroiddata` fork
- create or reuse a merge request against `fdroid/fdroiddata`

Required GitHub Actions secrets:

- `GITLAB_NAMESPACE`: your GitLab username or group that owns the `fdroiddata`
  fork
- `GITLAB_TOKEN`: a GitLab personal access token with `api` and
  `write_repository`

Workflow inputs:

- `release_ref`: the release tag or commit to place in the F-Droid metadata
- `fdroiddata_branch`: the branch name in your fork, normally
  `vibro.navigator`

This does not fully automate official inclusion. F-Droid maintainers still
review the merge request and perform the final rebuild/sign/publish steps on
their own infrastructure.

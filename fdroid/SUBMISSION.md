# F-Droid Submission Notes

This directory contains a draft `fdroiddata` metadata file for
`navigator`.

Before opening the official F-Droid merge request, complete these steps:

1. Push the repository to a public GitHub URL.
2. Commit the current F-Droid prep changes.
3. Create and push a release tag matching `versionName`, for example `v0.1.0`.
4. Replace the placeholders in `fdroid/navigator.yml`:
   - `REPLACE_WITH_GITHUB_OWNER`
   - `REPLACE_WITH_RELEASE_TAG_OR_COMMIT`
5. Copy `fdroid/navigator.yml` into your `fdroiddata` fork as
   `metadata/navigator.yml`.
6. Run the standard validation flow in the F-Droid build container:
   - `fdroid readmeta`
   - `fdroid rewritemeta navigator`
   - `fdroid checkupdates --allow-dirty navigator`
   - `fdroid lint navigator`
   - `fdroid build navigator`

Useful repository files already prepared upstream:

- `fastlane/metadata/android/en-US/...`
- `.github/workflows/fdroid-ready.yml`
- `.github/workflows/fdroid-submit.yml`

Notes:

- The Gradle build must run from the repository root, not `app/`, because the
  wrapper and `settings.gradle` live at the top level.
- `local.properties` is excluded in the draft recipe because it is machine-
  specific and should not be present in F-Droid builds.
- `AllowedAPKSigningKeys` is optional and should only be added if you later
  publish a signed upstream APK that you want F-Droid to verify against.

## GitHub Action automation

The `Submit F-Droid Metadata` workflow can automate the part that is safe to
automate:

- render the final `metadata/navigator.yml`
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
  `navigator`

This does not fully automate official inclusion. F-Droid maintainers still
review the merge request and perform the final rebuild/sign/publish steps on
their own infrastructure.

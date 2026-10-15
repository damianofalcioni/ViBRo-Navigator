# Shared store listing

`metadata/android/` is the single source for Google Play and F-Droid listing
content. Both support the Fastlane Supply layout; adding translations and a
feature graphic here does not conflict with F-Droid. No Fastlane executable is
needed for F-Droid to read these files from a published release's source tree.
Google Play uploads use Gradle Play Publisher locally and in GitHub Actions;
the Fastlane executable, Ruby, and Python are not required.

## Files and limits

```text
metadata/android/<locale>/
  title.txt
  short_description.txt
  full_description.txt
  images/
    icon.png
    featureGraphic.jpg
    phoneScreenshots/*.jpg
  changelogs/<versionCode>.txt
```

| Content | Shared requirement |
| --- | --- |
| App name | UTF-8 `title.txt`, at most 30 characters (the stricter Play limit) |
| Short description | UTF-8 `short_description.txt`, at most 80 characters |
| Full description | UTF-8 `full_description.txt`, at most 4,000 characters |
| Icon | `images/icon.png`, 512×512 PNG, at most 1,024 KB |
| Feature graphic | `images/featureGraphic.jpg`, exactly 1024×500; opaque PNG (`.png`) is also supported |
| Phone screenshots | `images/phoneScreenshots/`, 2–8 JPEG or opaque PNG images; the existing eight are 1080×1920 (9:16) |
| Release notes | `changelogs/<versionCode>.txt`, at most 500 characters |

The canonical `icon.png` uses the existing 512×512 artwork. The SVG and
`icon-512x512.png` remain available as source/reference files. Screenshot
filenames control display order alphabetically.

The feature graphic uses the maintainer-supplied website-hero design, preserved
unchanged as `metadata/android/en-US/images/featureGraphic.jpg` (1024×500 RGB
JPEG). The icon, feature graphic, and screenshots are ready for listing review.

## Translations

There are 18 listing locales, matching the app's supported languages:

| Language | Folder |
| --- | --- |
| Arabic | `ar` |
| Catalan | `ca` |
| Chinese (Simplified) | `zh-CN` |
| Czech | `cs-CZ` |
| Danish | `da-DK` |
| Dutch | `nl-NL` |
| English | `en-US` |
| French | `fr-FR` |
| German | `de-DE` |
| Greek | `el-GR` |
| Hungarian | `hu-HU` |
| Italian | `it-IT` |
| Japanese | `ja-JP` |
| Korean | `ko-KR` |
| Polish | `pl-PL` |
| Portuguese (Brazil) | `pt-BR` |
| Spanish (Spain) | `es-ES` |
| Swedish | `sv-SE` |

Each folder has all three text files. The brand stays `ViBRo Navigator` in
every locale. The new translations are AI-authored and should receive native
speaker review before publication. Portuguese uses Brazilian wording, matching
the app's existing Portuguese resources. Store locale names are not Android
`values-*` directory names.

Keep shared descriptions accurate for both flavors. Mention a flavor explicitly
when a claim differs: the F-Droid build does not require Google services. Do not
advertise Google Play-only integrations as features of the FOSS build.

`en-US` contains the shared images. Google Play uses the default language's
graphics when a text translation has no localized graphics; set the app's
default listing language to English (United States) in Play Console. F-Droid
also uses `en-US` as its fallback. If translated screenshots are added later,
put them in that locale's own `images/phoneScreenshots/` directory. The shared
banner can likewise be overridden with `images/featureGraphic.jpg` in a locale
folder. There is no need to duplicate the shared images into every language.
This fallback applies after the images have been uploaded to the corresponding
default-language listing; the folder name alone does not change Play Console's
configured default language.

The current screenshots show the English interface and the shared banner contains English
copy. Translating the listing text does not translate image text or the app's
`strings.xml` resources. Release notes intentionally remain English (`en-US`)
only for both stable and beta releases. Translating the listing into 18
languages does not require translating release notes. The release workflow
reads only `en-US/changelogs/<versionCode>.txt`; localized notes can be added
later if explicitly requested.

## Local Gradle commands

The shared files stay in Fastlane format for F-Droid. Gradle converts them to
GPP's layout under `app/build/generated/store-listing/play/`, including renamed
`short-description.txt` / `full-description.txt` and the appropriate graphics
subdirectories. Generated files are not committed. Only listing text and the
canonical icon, feature graphic, and phone screenshots are included; changelogs,
SVG/reference icons, and release binaries are excluded.

Validate text lengths, required files, image dimensions/formats, and screenshot
counts, then generate the upload layout:

```powershell
.\gradlew.bat :app:preparePlayListing
```

This makes no Google API calls and needs no publishing credentials, upload
keystore, Ruby, or opt-in GPP plugin. It checks file requirements, not Google's
policy/review eligibility. `:app:verifyPlayListingMetadata` runs just the checks.

For publication, complete [the Play prerequisites](../gradle/PLAY_STORE.md#initial-prerequisites)
and grant the service account **View app information (read-only)**,
**Release apps to testing tracks**, and **Manage store presence** for
`vibro.navigator`. While the app is a draft, also grant **Edit and delete draft
apps**; this was required to commit this app's first listing. The Android Publisher API must
be enabled in the Google Cloud project that owns the key. Use the same existing
JSON key; local `PLAY_SERVICE_ACCOUNT_JSON` is its file path:

```powershell
$env:PLAY_SERVICE_ACCOUNT_JSON = 'D:\keys\play-service-account-vibro-publisher.json'
.\gradlew.bat -PplayPublishing -PplaySubmitForReview=true :app:publishPlayListing
```

Alternatively, `ANDROID_PUBLISHER_CREDENTIALS` accepts the JSON contents. On
Linux/macOS use `./gradlew`. This invokes GPP's `publishGplayReleaseListing`,
uploading listing text and images without building/uploading APKs or AABs,
altering version codes, promoting tracks, or updating release notes. No track
or existing release version needs to be selected. Images replace the matching
Play image set; absent localized image directories use the default fallback.

**Review submission is explicit.** For this app Google rejected a commit with
`changesNotSentForReview=true`, saying changes are sent for review automatically.
The publishing task therefore refuses to run without
`-PplaySubmitForReview=true`. That option permits submission for Google review;
it cannot promise an unsent Play Console draft. Review and Play's publishing
controls determine when accepted listing changes become visible. Listing text
applies across tracks, including production where applicable.

The standard GitHub release workflow continues uploading the AAB and English
release notes separately. It does not publish the listing. Policy declarations,
content rating, privacy policy, contact details, and production eligibility
remain Play Console prerequisites outside this metadata tree. Android app-string
imports are also separate; there is no CSV/XML conversion needed for this
listing upload. For manual entry, add languages in Play Console > Main store
listing > Manage translations and paste each locale's three text files.

If Google accepts individual uploads but rejects the final commit with
`403 PERMISSION_DENIED`, the listing was not saved. Recheck the service
account's **app-level** grants for `vibro.navigator`, including **Release apps
to testing tracks** for the existing internal release, alongside read access
and **Manage store presence**. For an app that is still a draft, include
**Edit and delete draft apps**: adding that permission resolved this app's
commit failure. Production-release permission is a separate grant. The publisher's
[minimum-permissions example](https://github.com/Triple-T/gradle-play-publisher/blob/4.1.1/assets/min-perms.png)
shows read access, testing releases, and store presence for its example app.
Permission changes
may need time to propagate. Do not treat successful staging as a committed
upload or retry with broader administrative access automatically.

## GitHub Actions

After this workflow is committed to the default branch, open **Actions > Google
Play Listing > Run workflow**. It uses the same Gradle tasks as local publishing:

- Leave `publish` unchecked to validate and prepare the listing. Download the
  `google-play-listing` artifact to inspect the generated files; no secret is needed.
- Check `publish` to upload the listing and allow submission for Google review.
  Set the repository's `PLAY_SERVICE_ACCOUNT_JSON` Actions secret to the complete
  JSON key contents, not the file path. No Android signing secrets are needed.

The workflow runs only on manual dispatch. It shares the
`google-play-vibro.navigator` concurrency group with the binary upload job so CI
store edits run one at a time. Avoid running a local publish concurrently with a
CI publish. Ordinary app builds and F-Droid source builds do not load GPP.

## References

- [Fastlane Supply layout and upload behavior](https://docs.fastlane.tools/actions/supply/)
- [Gradle Play Publisher listing tasks](https://github.com/Triple-T/gradle-play-publisher/tree/4.1.1#publishing-listings)
- [F-Droid's compatible Fastlane structure](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [Play listing languages and translations](https://support.google.com/googleplay/android-developer/answer/9844778?hl=en)
- [Play text fields and limits](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)
- [Play image and screenshot requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)

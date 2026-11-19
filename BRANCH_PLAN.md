# BRouter branch regression plan

Review baseline is `codex/brouter-folder-picker` at `7db525c2` against local `main` at `1a502328`.
The branch review recorded **54 findings** with **13 P2** and **41 P3** reports. The IDs preserve report order. RG-14 and RG-48 describe the same profile-settings freeze with different setups. RG-15 and RG-38 exercise the same fallback loop at different positions. Keep both reproduction cases for each shared fix.

Status is open. The initial review passed the full F-Droid and Google Play debug unit-test suites, debug lint for both flavors, and the PMD complexity check. Focused tests reproduced the reported behavior. Physical-device picker coverage remained pending at the review baseline.

## Implementation sequence

1. Address the P2 routing-selection, provider-stall, and first-run blockers first. Keep the focused reproducers with each fix.
2. Stabilize grant verification and persistence, then update the folder prompts, Settings flows, and upgrade handling against those contracts.
3. Verify street reads and overlay recovery with the updated grant logic. Exercise the full list below before reassessing merge readiness.

## Routing profile selection and main-screen state

Separate the committed choice from the list being rendered. Track dropdown activation by input action and preserve the in-progress routing mode or Custom selection across recreation.

- [ ] **RG-01 · P2 · Prevent Start from routing with a provisional profile**
  With a saved external profile and delayed discovery, Start must use the saved profile once verified or wait for an explicit choice. [ProfileSpinnerController.java](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L139)
- [ ] **RG-13 · P3 · Open the Custom picker only for a user action**
  Refresh while the dropdown is being touched and the saved Custom row moves. A programmatic selection must leave the picker closed. [ProfileSpinnerController.java](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L178)
- [ ] **RG-16 · P3 · Preserve the saved mode when BRouter is temporarily unavailable**
  Remove BRouter while Round Trip is selected. Show Straight Line while needed and keep the saved mode available when BRouter returns. [MainActivityRouteModeController.java](app/src/main/java/vibro/navigator/main/MainActivityRouteModeController.java#L182)
- [ ] **RG-43 · P2 · Prevent canceled touches from committing a routing profile**
  Cancel a touch on the profile selector before an asynchronous refresh completes. Keep the saved profile key and restore it when its row becomes available. [Touch handler](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L70) · [Selection persistence](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L169)
- [ ] **RG-44 · P3 · Honor a keyboard or accessibility selection of the displayed profile**
  Activate the provisional bundled profile through the dropdown’s item-click handler. Commit the explicit choice so a later scan cannot switch the selection back. [Selection handler](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L169)
- [ ] **RG-45 · P3 · Keep Custom available when there are no bundled profiles**
  Delay external discovery on startup with and without a saved Custom file. Show the Custom action immediately and make an already selected, readable file available for routing. [Profile loader](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L49) · [Start behavior](app/src/main/java/vibro/navigator/main/ProfileSelectionResolver.java#L19)
- [ ] **RG-46 · P3 · Retain known external copies of formerly bundled profiles**
  Remove a bundled profile while retaining its external copy and time out the next scan. Keep the verified external entry and prevent an implicit switch to another profile. [Profile list merge](app/src/main/java/vibro/navigator/main/ProfileSpinnerOptions.java#L22)
- [ ] **RG-47 · P3 · Exclude unavailable cached profiles from route selection**
  Remove a previously listed external profile, then time out the access refresh. A stale cached row must not silently supply its name to a new route request. [Profile list cache](app/src/main/java/vibro/navigator/main/ProfileSpinnerOptions.java#L22) · [Route profile selection](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L139)

## Profile discovery and Custom picker lifecycle

Publish usable partial results with their source. Bound provider work, keep explicit requests responsive, and reconcile lifecycle events with pending lookups.

- [ ] **RG-03 · P2 · Keep profile scans available when provider calls stall**
  Occupy both external-profile workers with stalled provider queries. A scan of a healthy folder must still run, and timeouts must propagate cancellation where supported. [AndroidBRouterProfileLoader.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L52)
- [ ] **RG-05 · P2 · Retain a pending Custom picker launch across recreation**
  Rotate while the Custom picker’s starting location is being resolved. The replacement activity must open exactly one picker and preserve the committed profile. [MainActivityProfilePicker.java](app/src/main/java/vibro/navigator/main/MainActivityProfilePicker.java#L53)
- [ ] **RG-07 · P2 · Keep profiles read before a later source times out**
  Read profiles from a healthy parent, then stall an older grant. The timeout must preserve the profiles already discovered. [AndroidBRouterProfileLoader.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L52)
- [ ] **RG-17 · P3 · Coordinate the Custom picker with the automatic storage prompt**
  Complete the two lookups in either order. Keep a single foreground picker or dialog and resume the pending user action at the appropriate time. [MainActivityProfilePicker.java](app/src/main/java/vibro/navigator/main/MainActivityProfilePicker.java#L69)
- [ ] **RG-18 · P2 · Keep Custom location lookups from exhausting grant verification**
  Repeat Custom picker searches against a provider that ignores cancellation. An explicit grant for a healthy folder must still be verified. [AndroidBRouterProfilesTreeAccessPrompt.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfilesTreeAccessPrompt.java#L23)
- [ ] **RG-19 · P3 · Run a new scan after returning to the main screen**
  Resume with newly readable profiles while an older scan is in flight. Queue the resume refresh and publish the newer results. [AndroidBRouterProfileLoader.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L28)
- [ ] **RG-24 · P3 · Keep the Custom picker’s usable location on timeout**
  On Android 10, stall an early candidate with a later locally readable `profiles2`. Open the picker at the verified local location. [AndroidBRouterProfilesTreeAccessPrompt.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfilesTreeAccessPrompt.java#L26)

## Shared grants and provider verification

Track selection order per parent and data kind. Validate directory listings through the provider when needed and recheck cached children without competing with explicit selection.

- [ ] **RG-02 · P2 · Recover from a stale provider-backed street folder**
  Remove or replace `segments4` while its parent grant survives. Detect the stale child and recover access or use a readable fallback. [AndroidBRouterFolderAccess.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111)
- [ ] **RG-11 · P3 · Let explicit selection coexist with a background check**
  Select a valid provider-backed tree while it is being rechecked. Validation must complete or reuse the in-flight result without rejecting the folder solely because it is busy. [AndroidBRouterDirectoryCheck.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterDirectoryCheck.java#L80)
- [ ] **RG-12 · P3 · Preserve fallback order by selection time**
  Complete three selections out of order, then revoke the newest. Use the most recently selected remaining grant for the relevant data kind. [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L82)
- [ ] **RG-23 · P3 · Honor usable legacy child grants regardless of folder name**
  Restore a legacy street grant with readable `.rd5` files in a directory called `profiles2`. Validate the legacy grant according to the access it already provided. [AndroidBRouterFolderAccess.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L117)
- [ ] **RG-29 · P3 · Verify both children when child workers are busy**
  Fill the child worker pool and select a parent with readable `profiles2` and unreadable `segments4`. Verify the readable child and accept the parent. [AndroidBRouterDirectoryCheck.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterDirectoryCheck.java#L110)
- [ ] **RG-31 · P3 · Fall back to the document provider when direct access changes**
  Make a previously direct `profiles2` path unavailable while the same parent grant still provides access through Android. Recover profiles without requiring a new selection. [Directory check](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterGrantedDirectories.java#L28) · [Grant refresh](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111)
- [ ] **RG-33 · P3 · Treat an error listing as a failed readability check**
  Return an empty child cursor with `EXTRA_ERROR`. Do not persist it as verified access or show the folder as usable. [Folder verification](app/src/main/java/vibro/navigator/android/storage/AndroidDocumentTreeDirectories.java#L58) · [Android error flag](https://developer.android.com/reference/android/provider/DocumentsContract#EXTRA_ERROR)
- [ ] **RG-34 · P3 · Requery a directory after the provider finishes loading**
  Return an empty parent listing with `EXTRA_LOADING`, then notify the change when the children arrive. Revalidate within the request’s time budget or offer a retry that preserves context. [Parent folder listing](app/src/main/java/vibro/navigator/android/storage/AndroidDocumentTreeDirectories.java#L28) · [Android loading flag](https://developer.android.com/reference/android/provider/DocumentsContract#EXTRA_LOADING)
- [ ] **RG-35 · P3 · Refresh a replaced profile folder’s document ID**
  Replace `profiles2` with a readable directory under a new document ID while retaining the parent grant. Resolve the new ID and list its profiles. [Grant refresh](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111) · [Android document IDs](https://developer.android.com/reference/android/provider/DocumentsContract.Document#COLUMN_DOCUMENT_ID)
- [ ] **RG-36 · P3 · Check later matching child names when the first fails**
  Return two `profiles2` directories with distinct IDs and an unreadable first entry. Continue to the readable entry before rejecting the parent. [Directory lookup](app/src/main/java/vibro/navigator/android/storage/AndroidDocumentTreeDirectories.java#L43)
- [ ] **RG-40 · P3 · Discard out-of-order results for the same granted parent**
  Validate one parent before and after a child document ID changes, then deliver the older completion last. Keep the newer child mapping. [Grant ordering](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L66) · [Subfolder persistence](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L110)

## Storage prompts, navigation startup, and Settings

Recheck which data is missing when showing or dismissing a prompt. Preserve the requested action, selected parent, and earlier dismissals through picker results and activity recreation.

- [ ] **RG-06 · P3 · Guide the manual storage action toward the missing data**
  With profiles already granted and `segments4` confirmed elsewhere, the manual action must suggest the street-data parent and still allow browsing. [AndroidBRouterFolderRequests.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderRequests.java#L45)
- [ ] **RG-09 · P2 · Avoid suggesting an empty parent on first run**
  On Android 10, provide an empty `Android/media` parent and a usable `Android/data` parent. Prefer the usable location and avoid repeating the empty suggestion after rejection. [AndroidBRouterPickerLocation.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerLocation.java#L89)
- [ ] **RG-10 · P3 · Synchronize the navigation button after a late grant result**
  Rotate during validation initiated by the navigation button. The replacement screen must reflect the saved Surrounding streets setting when validation finishes. [NavigationCustomButtonController.java](app/src/main/java/vibro/navigator/nav/ui/NavigationCustomButtonController.java#L133)
- [ ] **RG-25 · P3 · Find a visible child when its parent metadata query is denied**
  Allow a document-provider query for `segments4` while denying the separate parent query. Use the visible child as evidence for offering the parent. [AndroidBRouterPickerLocation.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerLocation.java#L119)
- [ ] **RG-26 · P3 · Recheck street access before showing the Settings prompt**
  Finish background verification of an existing `segments4` grant after the user enables Surrounding streets and before the prompt appears. Apply the setting without a redundant storage request. [AboutSettingsSwitches.java](app/src/main/java/vibro/navigator/about/AboutSettingsSwitches.java#L350)
- [ ] **RG-28 · P3 · Retain navigation discovery across pause and resume**
  Pause while the first lookup is using the provider’s query slot, then resume. Consume the completed lookup or retry when the slot is available so a valid folder is still offered. [NavigationStartupSegmentsTreeAccess.java](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L98)
- [ ] **RG-30 · P3 · Preserve the Settings enable request across recreation**
  Rotate with the picker open after Settings renders the switch as off. A valid result must complete the enable action unless the user explicitly canceled it. [AboutStreetPicker.java](app/src/main/java/vibro/navigator/about/AboutStreetPicker.java#L70)
- [ ] **RG-37 · P3 · Keep Surrounding streets enabled when dismissing a stale prompt**
  Grant access while the navigation prompt is open, then dismiss it. Recheck current access before changing the saved setting. [Navigation startup prompt](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L170)
- [ ] **RG-41 · P3 · Skip the startup offer for a known profiles-only parent**
  With the only discovered parent already granted and no `segments4` available, continue navigation and keep Surrounding streets enabled for later recovery. [Folder discovery](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerLocation.java#L109) · [Startup retry](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L276)
- [ ] **RG-49 · P3 · Reuse an older valid parent as a picker hint**
  Revoke the newest grant while another usable parent remains outside the standard paths. Start manual browsing at that older parent. [Saved folder lookup](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfilesTreeAccessPrompt.java#L74)
- [ ] **RG-50 · P3 · Retain the dismissal when a different folder is selected**
  Dismiss a street-storage offer, then manually grant a different profiles-only folder. Keep the original dismissal and avoid immediately repeating the unresolved street prompt. [Picker result tracking](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterForegroundChecks.java#L132) · [Dismissal history](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderOffers.java#L34)
- [ ] **RG-51 · P3 · Apply the new trip’s folder dismissals when reusing navigation**
  Deliver a new trip request that skips the folder being checked or offered for the previous trip. Use the new skip list to discard an outdated discovery result or dismiss an outdated prompt, then continue startup with Surrounding streets still enabled. Two Robolectric probes reproduced the delayed lookup and the already visible prompt. Canceling the old prompt disabled the saved setting. [New navigation intent](app/src/main/java/vibro/navigator/nav/ui/NavigationActivity.java#L183) · [Startup state and skip list](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L103)
- [ ] **RG-52 · P3 · Bind startup folder validation to its original picker request**
  Defer validation for the first picker, resume an existing trip, then start another trip and open its picker. The first validation can clear the second picker’s token, open a retry prompt, and cause the second picker’s valid grant to be ignored. Match each completion to its original request and keep the newer picker pending until its own result arrives. Reproduced in Robolectric. [Startup reset](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L211) · [Validation callback](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L271) · [Token completion](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerRequest.java#L87)

## Profile parameter settings

Resolve the actual profile file before reading parameters and keep document queries off the UI thread.

- [ ] **RG-14 · P2 · Keep profile settings responsive during provider lookups**
  Open settings for a bundled profile with a stalled retained grant. The main thread must remain responsive and any provider-dependent result must be bounded. Also cover RG-48. [ProfileParameterSettingsController.java](app/src/main/java/vibro/navigator/main/ProfileParameterSettingsController.java#L64)
- [ ] **RG-32 · P3 · Read parameter metadata from the resolved profile file**
  Give two parents the same `.brf` name and leave the preferred copy unannotated. The parameter screen and route request must respect the preferred copy’s metadata. [Profile lookup](app/src/main/java/vibro/navigator/brouter/BRouterProfileParameterSource.java#L97) · [Route selection](app/src/main/java/vibro/navigator/main/ProfileSelectionResolver.java#L69)
- [ ] **RG-48 · P2 · Cover the profile-settings freeze with an unannotated current file**
  Open settings when the preferred `.brf` is readable and has no editable parameters, and an older provider stalls. Verify main-thread responsiveness. Use the same implementation as RG-14. [Settings click handler](app/src/main/java/vibro/navigator/main/ProfileParameterSettingsController.java#L64) · [Folder lookup](app/src/main/java/vibro/navigator/brouter/BRouterProfileParameterSource.java#L86)

## Street tile loading and overlay recovery

Keep the preferred and fallback reads bounded. Invalidate cached results on effective access changes, distinguish retryable failures from empty data, and keep per-tile reads independent of permission discovery.

- [ ] **RG-04 · P3 · Remove folder probing from the per-tile path**
  Load multiple tiles with a usable street grant and another profiles-only grant. Recheck the missing child on a bounded schedule and reuse the source snapshot for tile reads. [BRouterSegmentsRepository.java](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L129) · [AndroidBRouterFolderAccess.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111)
- [ ] **RG-08 · P2 · Refresh the live overlay after regranting the same folder**
  Restore a grant with the same tree and child URIs during a trip. Invalidate empty street chunks and reload the visible area. [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L92)
- [ ] **RG-15 · P2 · Try a healthy street source when the preferred read stalls**
  Block the preferred provider while opening a tile that exists in a fallback. Reach the working copy within the read budget. Also cover the fallback order in RG-38. [BRouterSegmentsRepository.java](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L118)
- [ ] **RG-20 · P3 · Keep successfully decoded streets when closing a file fails**
  Throw from `close()` after valid tile data has been decoded. Retain the completed geometry without publishing partial data from a failed decode. [BRouterStreetFileLoader.java](app/src/main/java/vibro/navigator/brouter/BRouterStreetFileLoader.java#L64)
- [ ] **RG-38 · P3 · Skip a stalled earlier grant on the way to a healthy fallback**
  Omit the tile from the preferred folder, stall an older grant, and keep a readable legacy copy. Reach that copy without waiting indefinitely. Use the same implementation as RG-15. [Tile fallback loop](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L113)
- [ ] **RG-39 · P3 · Retry empty overlay chunks caused by temporary read failures**
  Switch sources and fail the initial tile read, then recover while the viewport stays in the same area. Retry with a bounded policy and repopulate the overlay without another source change. [Source reload](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetOverlayController.java#L130) · [Chunk cache](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetOverlayCache.java#L57)
- [ ] **RG-53 · P3 · Keep healthy street sources reachable after successive storage changes**
  Stall three obsolete overlay loads that ignore interruption, then switch to a healthy source. The controller test confirmed that the healthy load stayed queued and the overlay stayed blank until a retired read finished. Coalesce related storage notifications and keep the newest usable source eligible for work within a bounded read budget. [Overlay worker limit](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetLoadExecutor.java#L45) · [Storage notifications](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterVolumeChanges.java#L16)
- [ ] **RG-54 · P3 · Reject truncated preferred tiles so readable fallbacks are tried**
  Keep a readable legacy copy, then select a preferred folder whose tile index points beyond the end of a truncated file. For a degree marked empty, the existing reader returns successfully, so the branch’s multi-source loader skips the fallback and draws no streets. A Robolectric repository test reproduced this and confirmed that an explicit open failure reached the fallback. Validate offsets before reporting an empty success and keep support for valid legacy tiles without a footer. [Default footer handling](app/src/main/java/vibro/navigator/brouter/BRouterRd5IndexReader.java#L100) · [Fallback loop](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L113)

## Upgrade and backup compatibility

Preserve existing picker results and device-local storage grants. Validate imported values before applying them.

- [ ] **RG-21 · P2 · Distinguish the legacy and shared first-run picker results**
  Restore an older snapshot with request `1102` pending and deliver the new shared picker’s result. Route the result to its owner and advance setup. [AndroidFirstRunPermissions.java](app/src/main/java/vibro/navigator/android/startup/AndroidFirstRunPermissions.java#L151)
- [ ] **RG-22 · P3 · Preserve device-local grants when importing older backups**
  Import a backup made before the shared grant was saved. Keep the current folder records and the Android permissions that correspond to this installation. [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L164) · [AppDataBackupPreferences.java](app/src/main/java/vibro/navigator/settings/AppDataBackupPreferences.java#L119)
- [ ] **RG-27 · P3 · Reject malformed backup values before writing preferences**
  Import a non-string value for the BRouter folder URI. Reject incompatible data and verify that the app still starts with BRouter installed. [AppDataBackupImportParser.java](app/src/main/java/vibro/navigator/settings/AppDataBackupImportParser.java#L61) · [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L159)
- [ ] **RG-42 · P3 · Honor pending navigation picker results restored from the old app**
  Restore navigation state from before picker tokens existed and deliver a valid `3005` result. Save the legacy street-tree grant and reconcile the pending startup. [Navigation picker result handler](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L183)

## Verification before merge

- Use deterministic provider doubles for stalled calls, out-of-order callbacks, error and loading cursors, and failures during file reads or close. Assert both the visible state and the persisted settings where applicable.
- Exercise rotation, pause and resume, legacy saved state, and revoked or replaced folders. When a device is available, check the system picker on Android 10 and on a newer Android version, including removable storage if available.
- Run `.\gradlew.bat :app:testFdroidDebugUnitTest :app:testGplayDebugUnitTest :app:lintFdroidDebug :app:lintGplayDebug :app:complexityCheck` after code fixes and check the branch diff.
- Record the fix and verification result for every finding, including both reproductions for shared fixes. Reassess merge readiness once the remaining findings are resolved or explicitly deferred.

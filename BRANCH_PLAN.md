# BRouter branch regression plan

Review baseline is `codex/brouter-folder-picker` at `7db525c2` against local `main` at `1a502328`.
The branch review recorded **57 findings** with **13 P2** and **44 P3** reports. The IDs preserve report order. RG-14 and RG-48 describe the same profile-settings freeze with different setups. RG-15 and RG-38 exercise the same fallback loop at different positions. Keep both reproduction cases for each shared fix.

The initial review passed the full F-Droid and Google Play debug unit-test suites, debug lint for both flavors, and the PMD complexity check. Focused tests reproduced the reported behavior. Physical-device picker coverage remained pending at the review baseline.

## Implementation status

The fix pass covers the retained backlog and RG-56. It preserves committed profile and route-mode selections, keeps in-progress picker actions across recreation, retains usable discovery results on timeout, and preserves local folder grants during backup restore. Street loading now uses bounded provider waits, retries temporary empty reads, and refreshes cached geometry after regrant or revocation.

Regression coverage includes state restoration, explicit activation of a provisional profile, a healthy discovery running alongside two abandoned calls, partial results on timeout, provider child replacement, error listings, same-folder regrant, revocation with a healthy fallback, and pre-branch backup import. The Android system picker and removable storage still need on-device coverage.

Final automated verification passed all 2,236 F-Droid and 2,308 Google Play debug unit tests, both debug lint tasks, and the PMD complexity check. `git diff --check` passed.

## Merge triage

The 57 reports include **25 findings in the fix backlog**, **13 cases to verify with those fixes**, and **19 to defer**. The original priorities and reproduction notes below preserve the review record. The triage weighs whether the branch changes observable behavior in a supported workflow, the severity of the result, and the conditions required to trigger it. A provider double can establish the failure mechanism. Device coverage is still needed to establish how often provider-specific cases occur.

### Keep in the fix backlog (25)

| Findings | User impact |
| --- | --- |
| RG-01, RG-16, RG-43 | Starting while profiles load or canceling a dropdown gesture can use or save the wrong routing profile. Temporarily losing BRouter availability can also overwrite the saved route mode. |
| RG-03, RG-07, RG-19 | A retained provider can starve profile discovery or discard profiles already found. Returning to the main screen during discovery can lose the refresh that would pick up newly available files. |
| RG-05, RG-10, RG-28, RG-30 | Rotation or a pause can lose a Custom picker action, leave the navigation button out of sync, skip a valid storage offer, or undo the requested Settings change. |
| RG-02, RG-08, RG-11, RG-33 | Replaced or regranted folders can stay unusable. Background verification can reject an explicit selection, and an error listing can be recorded as a successful grant. These affect the new shared-folder contract. |
| RG-06, RG-09, RG-26, RG-50 | Setup sends users to the wrong folder, prompts despite verified access, or forgets a dismissal after an unrelated grant. RG-09 is the strongest setup case because it can repeatedly offer the wrong Android 10 location during first run. The other three have workarounds. |
| RG-14, RG-32 | Profile settings can block the main thread. With multiple retained sources, an older same-named file can also supply parameters for the current profile. The first-nonempty-parameters lookup predates the branch, and the branch adds the multi-grant path that exposes it. |
| RG-04, RG-15, RG-39 | Tile reads can run repeat folder checks, stall before reaching a healthy source, or leave the visible overlay blank after a temporary read failure. The empty-chunk cache predates the branch, and the new live source-switch path can populate it during a transient failure. |
| RG-22 | A normal restore of a pre-branch backup can erase the new shared-folder records while Android still holds the permission. The preference replacement predates the branch. The new device-local grant data becomes vulnerable when stored in the backed-up file. |
| RG-56 | Follow-up to RG-08. The live overlay should refresh when access to a current source changes during a trip. `main` also kept cached geometry after revocation. |

Prioritize wrong-profile routing, first-run setup, valid-grant recovery, dropped lifecycle actions, the profile-settings freeze, and fallback blocked by a stalled provider. RG-04, RG-06, RG-10, RG-26, and RG-50 can follow those fixes if release scope is tight.

### Verify with retained fixes (13)

| Cases | Related fix and coverage |
| --- | --- |
| RG-13, RG-44, RG-45, RG-47 | With RG-01 and RG-43, cover callbacks from a reordered dropdown, explicit keyboard and accessibility activation, the supported Custom-only list, and stale cached external rows. |
| RG-17 | With RG-05, make sure a deferred Custom action opens a single picker when an automatic folder prompt is also pending. |
| RG-24 | With RG-03 and RG-07, retain a confirmed local picker location when an earlier provider stalls. Browsing remains possible when the hint is lost. |
| RG-31, RG-35 | With RG-02, cover the same grant recovery for profiles, including changed document IDs and the transition from direct access to a readable provider-backed child. |
| RG-37, RG-41 | With RG-26 and RG-06 or RG-09, cover the equivalent stale-access decision in navigation and skip offers for a parent already known to contain only profiles. |
| RG-38, RG-48 | Additional setups for the same fallback stall as RG-15 and the same UI freeze as RG-14. |
| RG-55 | With RG-04 and RG-15, check that one failed file is tried once when it appears in both source lists. The observed attempt still reached the healthy fallback. |

### Defer standalone work (19)

| Findings | Reason to defer |
| --- | --- |
| RG-12, RG-40 | The wrong fallback requires three out-of-order selections followed by revocation. The stale child mapping requires reselecting the same parent across a child replacement and receiving the older result last. |
| RG-18, RG-29, RG-53 | The probes saturate worker pools with eight stalled Custom lookups, four busy child workers, or three obsolete reads that ignore interruption across source changes. Cover these if straightforward when fixing provider stalls. RG-18's historical P2 rating reflects the impact after saturation. |
| RG-20, RG-54 | RG-20 is a branch regression that requires `close()` to fail after a successful decode. RG-54 requires a damaged index with an out-of-bounds offset and an empty requested degree. It exposes a pre-existing parser gap in the new fallback path. |
| RG-21, RG-42 | Both require an app upgrade while an old picker result or saved picker state is still pending. The probes cover synthetic restored state. RG-21 can block setup in that state, but the review has no device reproduction of the update-and-return sequence. |
| RG-23, RG-36 | Requires a legacy folder named for the other BRouter data type or duplicate child display names with an unreadable first match. Standard BRouter layouts avoid both conditions. |
| RG-25, RG-34 | Requires a provider that hides parent metadata while exposing the child, or that initially returns an empty loading cursor. The simulated provider behavior warrants compatibility coverage if seen on supported storage. |
| RG-27 | A malformed or edited backup can cause a persistent crash. The generic import type-checking gap predates the branch, and the reported value targets a new grant key. Track as backup validation hardening. |
| RG-46, RG-49 | Requires a BRouter update that removes a bundled profile while the matching external copy survives a timed-out scan, or revocation of the newest grant while an older nonstandard location remains usable. RG-49 loses a browsing hint. |
| RG-51, RG-52 | The simulated intent sequences reuse navigation while an older prompt or validation is pending. RG-52 also requires a second picker before the first validation completes. The review has no ordinary device workflow reproducing either sequence. |
| RG-57 | A branch regression with a brief overlay interruption when an unrelated volume is ejected during navigation. |

## Implementation sequence

1. Address the routing-selection, provider-stall, and first-run blockers in the merge triage. Keep the focused reproducers with each fix.
2. Stabilize grant verification and persistence, then update the folder prompts and Settings flows. Verify that importing an older backup preserves the local grant.
3. Verify street reads and overlay recovery with the updated grant logic, then reassess merge readiness against the retained findings and their related cases.

## Routing profile selection and main-screen state

Separate the committed choice from the list being rendered. Track dropdown activation by input action and preserve the in-progress routing mode or Custom selection across recreation.

- [x] **RG-01 · P2 · Prevent Start from routing with a provisional profile**
  With a saved external profile and delayed discovery, Start must use the saved profile once verified or wait for an explicit choice. [ProfileSpinnerController.java](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L139)
- [ ] **RG-13 · P3 · Open the Custom picker only for a user action**
  Refresh while the dropdown is being touched and the saved Custom row moves. A programmatic selection must leave the picker closed. [ProfileSpinnerController.java](app/src/main/java/vibro/navigator/main/ProfileSpinnerController.java#L178)
- [x] **RG-16 · P3 · Preserve the saved mode when BRouter is temporarily unavailable**
  Remove BRouter while Round Trip is selected. Show Straight Line while needed and keep the saved mode available when BRouter returns. [MainActivityRouteModeController.java](app/src/main/java/vibro/navigator/main/MainActivityRouteModeController.java#L182)
- [x] **RG-43 · P2 · Prevent canceled touches from committing a routing profile**
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

- [x] **RG-03 · P2 · Keep profile scans available when provider calls stall**
  Occupy both external-profile workers with stalled provider queries. A scan of a healthy folder must still run, and timeouts must propagate cancellation where supported. [AndroidBRouterProfileLoader.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L52)
- [x] **RG-05 · P2 · Retain a pending Custom picker launch across recreation**
  Rotate while the Custom picker’s starting location is being resolved. The replacement activity must open exactly one picker and preserve the committed profile. [MainActivityProfilePicker.java](app/src/main/java/vibro/navigator/main/MainActivityProfilePicker.java#L53)
- [x] **RG-07 · P2 · Keep profiles read before a later source times out**
  Read profiles from a healthy parent, then stall an older grant. The timeout must preserve the profiles already discovered. [AndroidBRouterProfileLoader.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L52)
- [ ] **RG-17 · P3 · Coordinate the Custom picker with the automatic storage prompt**
  Complete the two lookups in either order. Keep a single foreground picker or dialog and resume the pending user action at the appropriate time. [MainActivityProfilePicker.java](app/src/main/java/vibro/navigator/main/MainActivityProfilePicker.java#L69)
- [ ] **RG-18 · P2 · Keep Custom location lookups from exhausting grant verification**
  Repeat Custom picker searches against a provider that ignores cancellation. An explicit grant for a healthy folder must still be verified. [AndroidBRouterProfilesTreeAccessPrompt.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfilesTreeAccessPrompt.java#L23)
- [x] **RG-19 · P3 · Run a new scan after returning to the main screen**
  Resume with newly readable profiles while an older scan is in flight. Queue the resume refresh and publish the newer results. [AndroidBRouterProfileLoader.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfileLoader.java#L28)
- [ ] **RG-24 · P3 · Keep the Custom picker’s usable location on timeout**
  On Android 10, stall an early candidate with a later locally readable `profiles2`. Open the picker at the verified local location. [AndroidBRouterProfilesTreeAccessPrompt.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfilesTreeAccessPrompt.java#L26)

## Shared grants and provider verification

Track selection order per parent and data kind. Validate directory listings through the provider when needed and recheck cached children without competing with explicit selection.

- [x] **RG-02 · P2 · Recover from a stale provider-backed street folder**
  Remove or replace `segments4` while its parent grant survives. Detect the stale child and recover access or use a readable fallback. [AndroidBRouterFolderAccess.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111)
- [x] **RG-11 · P3 · Let explicit selection coexist with a background check**
  Select a valid provider-backed tree while it is being rechecked. Validation must complete or reuse the in-flight result without rejecting the folder solely because it is busy. [AndroidBRouterDirectoryCheck.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterDirectoryCheck.java#L80)
- [ ] **RG-12 · P3 · Preserve fallback order by selection time**
  Complete three selections out of order, then revoke the newest. Use the most recently selected remaining grant for the relevant data kind. [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L82)
- [ ] **RG-23 · P3 · Honor usable legacy child grants regardless of folder name**
  Restore a legacy street grant with readable `.rd5` files in a directory called `profiles2`. Validate the legacy grant according to the access it already provided. [AndroidBRouterFolderAccess.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L117)
- [ ] **RG-29 · P3 · Verify both children when child workers are busy**
  Fill the child worker pool and select a parent with readable `profiles2` and unreadable `segments4`. Verify the readable child and accept the parent. [AndroidBRouterDirectoryCheck.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterDirectoryCheck.java#L110)
- [ ] **RG-31 · P3 · Fall back to the document provider when direct access changes**
  Make a previously direct `profiles2` path unavailable while the same parent grant still provides access through Android. Recover profiles without requiring a new selection. [Directory check](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterGrantedDirectories.java#L28) · [Grant refresh](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111)
- [x] **RG-33 · P3 · Treat an error listing as a failed readability check**
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

- [x] **RG-06 · P3 · Guide the manual storage action toward the missing data**
  With profiles already granted and `segments4` confirmed elsewhere, the manual action must suggest the street-data parent and still allow browsing. [AndroidBRouterFolderRequests.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderRequests.java#L45)
- [x] **RG-09 · P2 · Avoid suggesting an empty parent on first run**
  On Android 10, provide an empty `Android/media` parent and a usable `Android/data` parent. Prefer the usable location and avoid repeating the empty suggestion after rejection. [AndroidBRouterPickerLocation.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerLocation.java#L89)
- [x] **RG-10 · P3 · Synchronize the navigation button after a late grant result**
  Rotate during validation initiated by the navigation button. The replacement screen must reflect the saved Surrounding streets setting when validation finishes. [NavigationCustomButtonController.java](app/src/main/java/vibro/navigator/nav/ui/NavigationCustomButtonController.java#L133)
- [ ] **RG-25 · P3 · Find a visible child when its parent metadata query is denied**
  Allow a document-provider query for `segments4` while denying the separate parent query. Use the visible child as evidence for offering the parent. [AndroidBRouterPickerLocation.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerLocation.java#L119)
- [x] **RG-26 · P3 · Recheck street access before showing the Settings prompt**
  Finish background verification of an existing `segments4` grant after the user enables Surrounding streets and before the prompt appears. Apply the setting without a redundant storage request. [AboutSettingsSwitches.java](app/src/main/java/vibro/navigator/about/AboutSettingsSwitches.java#L350)
- [x] **RG-28 · P3 · Retain navigation discovery across pause and resume**
  Pause while the first lookup is using the provider’s query slot, then resume. Consume the completed lookup or retry when the slot is available so a valid folder is still offered. [NavigationStartupSegmentsTreeAccess.java](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L98)
- [x] **RG-30 · P3 · Preserve the Settings enable request across recreation**
  Rotate with the picker open after Settings renders the switch as off. A valid result must complete the enable action unless the user explicitly canceled it. [AboutStreetPicker.java](app/src/main/java/vibro/navigator/about/AboutStreetPicker.java#L70)
- [ ] **RG-37 · P3 · Keep Surrounding streets enabled when dismissing a stale prompt**
  Grant access while the navigation prompt is open, then dismiss it. Recheck current access before changing the saved setting. [Navigation startup prompt](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L170)
- [ ] **RG-41 · P3 · Skip the startup offer for a known profiles-only parent**
  With the only discovered parent already granted and no `segments4` available, continue navigation and keep Surrounding streets enabled for later recovery. [Folder discovery](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerLocation.java#L109) · [Startup retry](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L276)
- [ ] **RG-49 · P3 · Reuse an older valid parent as a picker hint**
  Revoke the newest grant while another usable parent remains outside the standard paths. Start manual browsing at that older parent. [Saved folder lookup](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterProfilesTreeAccessPrompt.java#L74)
- [x] **RG-50 · P3 · Retain the dismissal when a different folder is selected**
  Dismiss a street-storage offer, then manually grant a different profiles-only folder. Keep the original dismissal and avoid immediately repeating the unresolved street prompt. [Picker result tracking](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterForegroundChecks.java#L132) · [Dismissal history](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderOffers.java#L34)
- [ ] **RG-51 · P3 · Apply the new trip’s folder dismissals when reusing navigation**
  Deliver a new trip request that skips the folder being checked or offered for the previous trip. Use the new skip list to discard an outdated discovery result or dismiss an outdated prompt, then continue startup with Surrounding streets still enabled. Two Robolectric probes reproduced the delayed lookup and the already visible prompt. Canceling the old prompt disabled the saved setting. [New navigation intent](app/src/main/java/vibro/navigator/nav/ui/NavigationActivity.java#L183) · [Startup state and skip list](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L103)
- [ ] **RG-52 · P3 · Bind startup folder validation to its original picker request**
  Defer validation for the first picker, resume an existing trip, then start another trip and open its picker. The first validation can clear the second picker’s token, open a retry prompt, and cause the second picker’s valid grant to be ignored. Match each completion to its original request and keep the newer picker pending until its own result arrives. Reproduced in Robolectric. [Startup reset](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L211) · [Validation callback](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L271) · [Token completion](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterPickerRequest.java#L87)

## Profile parameter settings

Resolve the actual profile file before reading parameters and keep document queries off the UI thread.

- [x] **RG-14 · P2 · Keep profile settings responsive during provider lookups**
  Open settings for a bundled profile with a stalled retained grant. The main thread must remain responsive and any provider-dependent result must be bounded. Also cover RG-48. [ProfileParameterSettingsController.java](app/src/main/java/vibro/navigator/main/ProfileParameterSettingsController.java#L64)
- [x] **RG-32 · P3 · Read parameter metadata from the resolved profile file**
  Give two parents the same `.brf` name and leave the preferred copy unannotated. The parameter screen and route request must respect the preferred copy’s metadata. [Profile lookup](app/src/main/java/vibro/navigator/brouter/BRouterProfileParameterSource.java#L97) · [Route selection](app/src/main/java/vibro/navigator/main/ProfileSelectionResolver.java#L69)
- [ ] **RG-48 · P2 · Cover the profile-settings freeze with an unannotated current file**
  Open settings when the preferred `.brf` is readable and has no editable parameters, and an older provider stalls. Verify main-thread responsiveness. Use the same implementation as RG-14. [Settings click handler](app/src/main/java/vibro/navigator/main/ProfileParameterSettingsController.java#L64) · [Folder lookup](app/src/main/java/vibro/navigator/brouter/BRouterProfileParameterSource.java#L86)

## Street tile loading and overlay recovery

Keep the preferred and fallback reads bounded. Invalidate cached results on effective access changes, distinguish retryable failures from empty data, and keep per-tile reads independent of permission discovery.

- [x] **RG-04 · P3 · Remove folder probing from the per-tile path**
  Load multiple tiles with a usable street grant and another profiles-only grant. Recheck the missing child on a bounded schedule and reuse the source snapshot for tile reads. [BRouterSegmentsRepository.java](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L129) · [AndroidBRouterFolderAccess.java](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterFolderAccess.java#L111)
- [x] **RG-08 · P2 · Refresh the live overlay after regranting the same folder**
  Restore a grant with the same tree and child URIs during a trip. Invalidate empty street chunks and reload the visible area. [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L92)
- [x] **RG-15 · P2 · Try a healthy street source when the preferred read stalls**
  Block the preferred provider while opening a tile that exists in a fallback. Reach the working copy within the read budget. Also cover the fallback order in RG-38. [BRouterSegmentsRepository.java](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L118)
- [ ] **RG-20 · P3 · Keep successfully decoded streets when closing a file fails**
  Throw from `close()` after valid tile data has been decoded. Retain the completed geometry without publishing partial data from a failed decode. [BRouterStreetFileLoader.java](app/src/main/java/vibro/navigator/brouter/BRouterStreetFileLoader.java#L64)
- [ ] **RG-38 · P3 · Skip a stalled earlier grant on the way to a healthy fallback**
  Omit the tile from the preferred folder, stall an older grant, and keep a readable legacy copy. Reach that copy without waiting indefinitely. Use the same implementation as RG-15. [Tile fallback loop](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L113)
- [x] **RG-39 · P3 · Retry empty overlay chunks caused by temporary read failures**
  Switch sources and fail the initial tile read, then recover while the viewport stays in the same area. Retry with a bounded policy and repopulate the overlay without another source change. [Source reload](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetOverlayController.java#L130) · [Chunk cache](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetOverlayCache.java#L57)
- [ ] **RG-53 · P3 · Keep healthy street sources reachable after successive storage changes**
  Stall three obsolete overlay loads that ignore interruption, then switch to a healthy source. The controller test confirmed that the healthy load stayed queued and the overlay stayed blank until a retired read finished. Coalesce related storage notifications and keep the newest usable source eligible for work within a bounded read budget. [Overlay worker limit](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetLoadExecutor.java#L45) · [Storage notifications](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterVolumeChanges.java#L16)
- [ ] **RG-54 · P3 · Reject truncated preferred tiles so readable fallbacks are tried**
  Keep a readable legacy copy, then select a preferred folder whose tile index points beyond the end of a truncated file. For a degree marked empty, the existing reader returns successfully, so the branch’s multi-source loader skips the fallback and draws no streets. A Robolectric repository test reproduced this and confirmed that an explicit open failure reached the fallback. Validate offsets before reporting an empty success and keep support for valid legacy tiles without a footer. [Default footer handling](app/src/main/java/vibro/navigator/brouter/BRouterRd5IndexReader.java#L100) · [Fallback loop](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L113)
- [ ] **RG-55 · P3 · Avoid reopening the same failed tile within one fallback pass**
  Use a shared parent grant whose `segments4` directory is also autodetected and keep a readable copy in a different folder. A Robolectric test using the real Android document lookup found two attempts to open the same failing file before the healthy copy was tried. A legacy child grant to that directory tried the failed file once. Track attempted file identities across both source lists while allowing retries on subsequent loads. [Tile attempts](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L109) · [Directory deduplication](app/src/main/java/vibro/navigator/brouter/BRouterSegmentsRepository.java#L149)
- [x] **RG-56 · P3 · Refresh cached streets when the active source loses its grant**
  Remove the preferred folder’s persisted read permission during a trip while another folder remains usable. A focused Robolectric test found that the active overlay kept the original geometry even after reopening the compass. An explicit source refresh loaded the fallback. Recheck effective access at a bounded point in the active lifecycle and invalidate the affected chunks when access changes. [Source observers](app/src/main/java/vibro/navigator/nav/service/NavigationServiceStreetOverlay.java#L26) · [Viewport reload](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetOverlayController.java#L126)
- [ ] **RG-57 · P3 · Preserve the street overlay across unrelated storage events**
  Load a visible overlay from the primary volume, then send an eject notification for a volume that supplies no BRouter data. A Robolectric service-wiring test confirmed that the notification clears the visible overlay and creates a new reader even though primary access remains valid. Keep the existing geometry and caches while the effective street sources are unchanged. [Volume receiver](app/src/main/java/vibro/navigator/android/brouter/AndroidBRouterVolumeChanges.java#L22) · [Overlay invalidation](app/src/main/java/vibro/navigator/nav/streets/SurroundingStreetOverlayController.java#L130)

## Upgrade and backup compatibility

Preserve existing picker results and device-local storage grants. Validate imported values before applying them.

- [ ] **RG-21 · P2 · Distinguish the legacy and shared first-run picker results**
  Restore an older snapshot with request `1102` pending and deliver the new shared picker’s result. Route the result to its owner and advance setup. [AndroidFirstRunPermissions.java](app/src/main/java/vibro/navigator/android/startup/AndroidFirstRunPermissions.java#L151)
- [x] **RG-22 · P3 · Preserve device-local grants when importing older backups**
  Import a backup made before the shared grant was saved. Keep the current folder records and the Android permissions that correspond to this installation. [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L164) · [AppDataBackupPreferences.java](app/src/main/java/vibro/navigator/settings/AppDataBackupPreferences.java#L119)
- [ ] **RG-27 · P3 · Reject malformed backup values before writing preferences**
  Import a non-string value for the BRouter folder URI. Reject incompatible data and verify that the app still starts with BRouter installed. [AppDataBackupImportParser.java](app/src/main/java/vibro/navigator/settings/AppDataBackupImportParser.java#L61) · [BRouterFolderGrant.java](app/src/main/java/vibro/navigator/brouter/BRouterFolderGrant.java#L159)
- [ ] **RG-42 · P3 · Honor pending navigation picker results restored from the old app**
  Restore navigation state from before picker tokens existed and deliver a valid `3005` result. Save the legacy street-tree grant and reconcile the pending startup. [Navigation picker result handler](app/src/main/java/vibro/navigator/nav/ui/NavigationStartupSegmentsTreeAccess.java#L183)

## Verification before merge

- Use deterministic provider doubles for stalled calls, out-of-order callbacks, error and loading cursors, and failures during file reads or close. Assert both the visible state and the persisted settings where applicable.
- Exercise rotation, pause and resume, legacy saved state, and revoked or replaced folders. When a device is available, check the system picker on Android 10 and on a newer Android version, including removable storage if available.
- Run `.\gradlew.bat :app:testFdroidDebugUnitTest :app:testGplayDebugUnitTest :app:lintFdroidDebug :app:lintGplayDebug :app:complexityCheck` after code fixes and check the branch diff.
- Record fixes and verification results for retained findings and their related cases. Revisit deferred reports if a fix already covers the scenario or a supported device reproduces it.

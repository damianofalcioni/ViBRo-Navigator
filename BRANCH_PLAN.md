# Branch handoff: Android Auto navigation compliance

## Current state and first actions

- The most recent audit-fixes section records the three fixes requested after syncing this branch with `main`. The earlier coverage audit and sync entries are historical.
- The Google Play appeal follow-up shared November 17, 2026 scopes the vibration restriction to the Android Auto environment. The user chose silent phone notifications while projected so compatible smartbands can still mirror them. `gplay/android/automotive/AndroidAutoConnectionMonitor` observes `CarConnection` from application startup and uses silence until the first connection result. `AndroidNavigationNotificationChannels.guidanceAlert` selects the silent channel for all shared transient navigation alerts and About tests during projection. The three Google Play regression tests cover the initial state, connect and disconnect during the same service session, alerts available for mirroring, and legacy vibration behavior.
- Verification passed with `.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest lintFdroidDebug lintGplayDebug assembleFdroidRelease assembleGplayRelease :app:complexityCheck --console=plain`. All 2,161 F-Droid and 2,259 Google Play tests passed with zero failures, errors or skips. PMD found zero violations. Both release APKs built. Both lints reported only the existing locale and Gradle version warnings. The F-Droid lint dependency model contains no Android for Cars or Google Play Services libraries. No Gradle commands remain running.
- No phone was listed by `adb devices -l`. Verify connect and disconnect mid-route on a head unit and verify actual smartband mirroring before resubmission. The companion app may filter silent notifications or apply its own haptics. Google requested deactivating bundle `1000000` across production and active test tracks and submitting a higher code. Release preparation and Play Console actions remain pending.
- Quality-audit follow-up (2026-11-06) is complete and ready for local commit: VI-1 idle phone prompts require safe parking in all 18 languages, DR-3 destination attempts have a nine-second deadline with explicit retry, and destination templates retain Car API 1 compatibility. Stale results and host actions cannot start/cancel/replace a newer request. Specification and the manual review checklist are aligned. See the final follow-up evidence below. No Gradle commands remain running.
- Full Google Play suite: 2,256 tests passed. Final-source focused Auto/automotive suite: 70 passed. Full F-Droid suite: 2,161 passed. Both flavor lints, PMD and all four APK builds passed. All test runs had zero failures/errors/skips. Only the two previously documented lint warnings remain. No device is connected, and the current SDK has no DHU installation; manual car verification and live Play submission remain outstanding and unauthorized.
- Workspace: `D:\PROJECTS\vibe-navigator`.
- Working branch: `codex/android-auto-compliance`, created from clean `main` at `1a502328` (`Document agent branch and commit workflow`). Continue this branch; do not recreate the work on main.
- Both the earlier Android Auto compliance milestone (804f9027; handoff workflow 5e5b951c) and the shared simulation milestone are complete and verified. Simulation is committed locally with this updated plan; use git log -1 for its identifier. No commands are running. Keep this branch and its existing work.
- Simulation is now authorized and in progress (latest user request). Implement it in shared code for Google Play and F-Droid, with Advanced settings row ordered label/configuration/switch/info, configurable speed using current display units, optional starting coordinates and the existing POI map picker beside them. Blank start uses device location; explicit coordinates must work without a physical fix. Android Auto onAutoDriveEnabled must activate the same simulator. Preserve pause/resume, route changes and arrival rules; clearly label simulation. Implementation and final verification are complete; see the simulation map and validation notes below.
- Nothing has been pushed, merged into `main`, published, or submitted to Play. The branch Google Play debug build is installed on the connected phone as recorded below. Both release APKs built locally are unsigned. App version remains `1.0.0` / `1000000`; this is not a new upload-ready release.
- This plan is branch-only. Keep it while work and handoffs continue; before merging this work into `main`, preserve any lasting decisions in maintained documentation, remove this file on the working branch, and commit its removal. The final merge diff and target tree must exclude `BRANCH_PLAN.md`, as now required by AGENT.md.
- Read `AGENT.md`, this plan and specification section 4.5.8, then inspect `git status` and `git log -3`. The completed milestone is recorded below; do not repeat its checks without a relevant change or new failure. Never push this working branch or merge without the required user authorization.

## Original problem and review context

Google Play flagged ViBRo Navigator (`vibro.navigator`), version code `1000000`, as **Approved with Issues / Category not permitted** for Android Auto. The appeal was rejected with essentially the same category explanation, specifically identifying the **Closed Testing** track and requiring a higher version code replacing the affected bundle.

Navigation is a supported Android Auto category. The app already used the Android for Cars App Library, declared `androidx.car.app.category.NAVIGATION`, requested `NAVIGATION_TEMPLATES` and `ACCESS_SURFACE`, and declared the template capability. The notice did not identify the exact technical reason for the category finding; do not claim that a particular code issue has been confirmed as Google's root cause.

Repository inspection nevertheless found concrete implementation gaps: text/buttons painted onto the navigation surface, missing NavigationManager ownership and cluster metadata, incoming car intents only logged, missing car-specific ongoing navigation notifications, and missing review simulation. The user supplied a navigation-requirements checklist and authorized implementation after asking to reread AGENT.md.

Initially the user explicitly said: **"leave simulation as last step, I have to think how to do it in a simple way"**. Later they requested that this branch plan provide the full context for the next agent, with AGENT.md updated if needed. That handoff requirement is now part of AGENT.md's initial workflow. They subsequently authorized shared simulation and specified the settings/configuration/map-picker UI; the earlier deferral is superseded.

## Agreed scope and important decisions

- Keep Android Auto support and repair its navigation integration. Removing Auto or merely changing a category declaration is not the chosen solution.
- Keep phone navigation, routing confidence/arrival rules, offline BRouter behavior and F-Droid compatibility intact. Use the existing service/session and binder actions rather than introducing separate car navigation logic.
- Directions, following maneuvers, ETA and navigation controls belong in host templates. The custom surface contains route/map content and the permitted speed-limit overlay. Custom setting buttons, diagnostic/detail panels, scrolling instruction columns and phone layouts are now phone-only.
- Retain Car API level 1 compatibility, with `Action.PAN` added only on hosts supporting API 2+. Deprecated header setters are intentionally and narrowly retained for this compatibility; replacing them with newer APIs requires reconsidering host support.
- Use structured app-owned maneuver/travel data, never parse localized display lines to obtain commands, distance, exit number or ETA. The following phone line is relative to the first maneuver; cluster step progress is absolute from the current position.
- Update both `androidx.car.app:app` and `app-projected` from `1.7.0` to `1.8.0-rc01`. Google's checked release notes still listed 1.7.0 as stable but explicitly recommended rc01 for its security fix. A dependency bump alone does not establish policy compliance.
- No new upload/version preparation until remaining head-unit verification is complete. Future uploads must use the repository's semantic-version/code formula and `prepareRelease`, not an independent versionCode edit.

## Implementation map

### Shared navigation snapshots and formatting

- `app/src/main/java/vibro/navigator/nav/model/NavManeuver.java`: immutable BRouter hint, localized instruction and absolute distance/time to the maneuver.
- `NavGuidanceStatus.maneuvers`: immutable structured list in the existing guidance group. Replacing display lines clears stale maneuver data; direct guidance explicitly supplies its replacement metadata.
- `NavTravelEstimate.java` and `NavProgressStatus.travelEstimate`: destination distance/duration, observation wall time and confirmed-arrival flag. Unknown duration remains NaN, not a fabricated ETA.
- `nav/format/NavStateTextFactory` builds maneuver metadata from the same upcoming-hint collector as phone text. `NavigationTextFormatter.maneuverInstruction` supplies instruction-only localized text.
- `nav/presentation/NavTravelEstimateFactory` and `NavStateResourceComposer` attach travel metadata without adding Android for Cars types to common code. Heading/overlay/pause copies preserve the grouped data.
- `nav/session/NavigationRouteStartApproachText`, `NavigationSessionRouteDisplayState`, `StraightLineNavigationGuidanceText`, `StraightLineNavigationProgress`, and `StraightLineNavigationState` supply matching direct-to-target/straight-line metadata. Beelines must not invent road-turn types.
- A null-duration unboxing bug exposed by the existing unknown-ETA test was fixed with explicit arrival branches. Keep that regression coverage.

### Host templates, map surface and ownership

All files below are under `app/src/gplay/java/vibro/navigator/auto`:

- `ViBRoCarTemplates` / `ViBRoCarGuidance`: host RoutingInfo, Step, MessageInfo and destination TravelEstimate; Stop, Pause/Resume and available Blocked road actions. Template invalidation compares actual content and skips unchanged heading-only inputs.
- `ViBRoCarManeuvers`: maps BRouter mode-9 commands, including roundabout direction/exit numbers, into car Maneuver/Step values. Generic/unknown/beeline commands remain unknown rather than guessing a left/right road turn.
- `ViBRoCarTrip`: builds cluster steps and clears obsolete steps with a loading Trip during recalculation. Unknown remaining time uses the library's unknown-time value.
- `ViBRoCarHostNavigation`: NavigationManager start/end/updateTrip and stop callback; notification lifecycle; ignores updates and stop callbacks after disposal. Handles synchronous revocation during start without publishing stale guidance. Pause/arrival/closure clear ownership, cluster output and the ongoing car notification; recalculation retains ownership.
- `ViBRoCarScreen` / `ViBRoCarSession`: process initial and new navigation intents; retain service binding and host callbacks while covered by another car screen; release them on destruction. Use the latest currentState after reentrant callbacks so a host stop cannot restore an old map frame. Cancel pending destination work when disabled/stopped. Refresh map theme from head-unit day/night configuration.
- `ViBRoCarNavigationController` / `ViBRoCarNavigationActions`: existing binder actions remain authoritative. Pending binds are released and late connection callbacks ignored; a host stop also handles the before-bind case. Removed obsolete phone-launch/settings-reconnect/detail helpers and optimistic duplicate pause-state rendering.
- `ViBRoAutoSurfaceRenderer`, `ViBRoAutoSurfacePainter`, `ViBRoAutoCompassPainter`, `ViBRoAutoCompassOverlayPainter`: safe-area route rendering, shared route/street/compass geometry, map gestures and speed-limit context. Obsolete text, button, diagnostic-panel, custom-setting and phone-launch painters/helpers were deleted with their obsolete tests. Do not restore the old surface UI.

### Incoming navigation and notifications

- `ViBRoCarIncomingNavigation`: injectable worker/access boundary, shared intent/coordinate parser, bounded address results requiring selection, generation-based latest-intent-wins, cancel/dispose protection, retained destination for setup retry. Coordinate destinations do not require online search; address search uses the existing provider selection. Pending results must never start a route after host stop/disposal.
- `gplay/android/automotive/AndroidCarNavigationStarter`: worker-side welcome/profile/BRouter inspection and saved parameter overrides; startup rechecks enabled integration, location and notification access. A missing prerequisite produces safely-parked setup instructions, not an automatic phone launch. It starts the existing NavigationService using the shared extras contract.
- `gplay/android/automotive/AndroidCarNavigationNotifications`: separate silent ongoing ID 3 notification, CATEGORY_NAVIGATION, CarAppExtender, phone resume PendingIntent and separate CarPendingIntent. Clears without removing active phone/wearable alert ID 2. Compat constants preserve minSdk 23 safety.
- Common `NavigationService.readNavRequest` stops old queued speech before replacing a request. Common `TransientNotificationTimeout.cancel` and foreground-controller cleanup immediately remove the last transient turn notification when navigation stops, invalidating its old timeout so it cannot remove a future session's alert.
- One new parked-setup message is translated across all 18 supported language files. The narrow UnusedResources annotation is intentional because Google Play-only text must remain in the shared translation catalog and F-Droid removes it during shrinking.

### Documentation

- `SPECIFICATION.md` section 4.5.8 now describes host guidance/map-only rendering, structured metadata, ownership, intents and the original simulation deferral; section 4.5.9 now documents the implemented simulator. Related phone-only text/diagnostic wording was aligned.
- `AGENT.md` now records the car adapter boundaries and requires a self-contained, updated branch handoff plan for multi-step work.
- `gradle/ANDROID_AUTO_REVIEW.md` contains reviewer phone/BRouter setup instructions, manual/DHU checks, simulation caveat and release handling; linked from `gradle/PLAY_STORE.md`.
- No published website content, store listing, live Play Console settings, credentials or release metadata has been changed.

## Validation and evidence

Run only one Gradle invocation at a time in this checkout. Logs below are ignored local build artifacts and may be absent in a new checkout; the commands are reproducible.

1. Full verification **passed** in 10m58s:

   ```powershell
   .\gradlew.bat lintFdroidDebug lintGplayDebug testFdroidDebugUnitTest testGplayDebugUnitTest assembleFdroidRelease assembleGplayRelease assembleFdroidDebug assembleGplayDebug complexityCheck --console=plain
   ```

   - F-Droid: **2,130 tests, 0 failures/errors/skips**.
   - Google Play: **2,206 tests, 0 failures/errors/skips**.
   - Both flavor lint tasks and all four APK builds succeeded; PMD had zero violations.
   - Evidence: `build/android-auto-validation.log` and `build/android-auto-full-test-summary.json`.

2. After adding the final disposed/reentrant lifecycle protections, a targeted verification **passed** in 3m44s:

   ```powershell
   .\gradlew.bat lintFdroidDebug lintGplayDebug :app:testGplayDebugUnitTest --tests 'vibro.navigator.auto.*' --tests 'vibro.navigator.android.automotive.*' assembleFdroidRelease assembleGplayRelease assembleFdroidDebug assembleGplayDebug complexityCheck --console=plain
   ```

   **52 focused tests** passed, plus both lint tasks, PMD and all APK builds. Evidence: `build/android-auto-final-validation.log`. This filtered run replaces the live GPlay XML report; do not mistake its smaller count for the earlier full suite. The full summary above preserves that evidence.

3. Lint reports were then inspected beyond their successful exit status. Newly introduced InlinedApi warnings were corrected using compat constants, and the intentionally Google Play-only shared resource was annotated narrowly. The notification adapter test now covers Android 6/API 23 and API 33. The first cleanup check caught an unbound tools namespace after the annotation was added to translations. It was corrected by keeping the annotation only on the base resource. The corrected rerun of command 2 passed in 4m27s: 53 focused tests, zero failures/errors/skips, zero PMD violations, both flavor lint tasks and all four APK builds succeeded. Both lint reports now contain zero errors and only the two pre-existing warnings listed below. Evidence: build/android-auto-cleanup-validation.log. No introduced lint warnings remain.

4. F-Droid runtime dependency verification **passed**:

   ```powershell
   .\gradlew.bat :app:dependencies --configuration fdroidReleaseRuntimeClasspath help --no-configuration-cache --warning-mode all '-Dorg.gradle.deprecation.trace=true' --console=plain
   ```

   `build/android-auto-dependencies.log` contains no Android for Cars or Play Services dependencies. Source checks found no common/F-Droid SDK imports; the existing Google package-name string in AppLogSessionInfo is diagnostic data, not SDK leakage. Quote the dotted -D argument in PowerShell; an initial unquoted diagnostic attempt was parsed as a task name and failed before running tasks, then was corrected successfully.

5. `git diff --check` passed. A technical-debt/diff review removed unused Auto helpers and found/fixed unknown-ETA, reentrant-stop, pending-bind and post-disposal issues. No phone was connected according to `adb devices`; no install or DHU run was performed.

### Known existing warnings and limits

- Lint's AppBundleLocaleChanges warning predates this change. `app/build.gradle` explicitly has `android.bundle.language.enableSplit = false`, as required by the repository's offline language picker. Do not add Play Core downloads or turn splitting on to silence this warning.
- Lint advertises Gradle 9.8.2 while the repository intentionally pins 9.8.1. A toolchain upgrade is separate work, not an Android Auto eligibility fix.
- The Configuration.setVisible deprecation was traced to AGP's `BasePlugin.createAndroidJdkImageConfiguration`, not a new project call. The pinned external toolchain still emits it. Do not add blanket warning suppressions or claim warning-free builds.
- Automated tests do not prove real head-unit behavior or Google Play acceptance. Simulation is implemented; manual DHU checks remain required.

## Remaining work and next-agent instructions

1. Verify the current branch, git status and recent commit when resuming. Both implementation milestones are complete; no Gradle invocation is active. Preserve local history and the no-push/no-merge/no-publish constraints.
2. Shared simulation is implemented, tested and locally committed. Do not revert to the old deferral or add a separate car-only engine. Its manual configuration is available in both flavors; host activation remains Google Play only.
3. Automated verification is complete and recorded below. Do not repeat completed checks without a new change, failure or unresolved concern. Preserve live tracking/arrival-confidence behavior and keep F-Droid free of car SDK dependencies.
4. Perform the manual/DHU checks in `gradle/ANDROID_AUTO_REVIEW.md`, including cluster output when covered, another navigation app taking ownership, notifications, safe-area/day-night rendering, coordinate/address intents and disconnected/reconnected lifecycle. Correct findings rather than claiming acceptance from unit tests.
5. Only after remaining requirements are verified, prepare a new semantic version/code with the existing release-prep workflow and coordinate Play submission. The flagged `1000000` bundle must be replaced in Closed Testing and any other affected tracks. No live submission is authorized by this implementation work.

Do not redo the completed template/metadata/intent integration merely because a new chat lacks the earlier conversation. Use this plan, the code, the recorded checks and the latest user instructions as the resume point.


## Shared simulation implementation (current authorized milestone)

- User explicitly authorized simulation for both builds and requested an Advanced row with a switch, configuration button and info button. Configuration accepts speed and an optional initial GPS position as coordinates, with the existing POI map picker beside it. This supersedes the initial simulation deferral.
- Product decisions: default 50 km/h; 0–300 km/h; store m/s and show current km/h/mph units; zero holds position. Manual settings apply at the next navigation start. Blank origin uses a usable cached/current device fix, waiting when unavailable; explicit coordinates avoid waiting for a physical GPS fix, but device location services and Android service permissions must remain enabled/granted. No system mock provider.
- Shared core: nav/simulation/NavigationSimulator owns scheduled one-second app-local fixes, pause cancellation, reentrant generation protection, separate wall/elapsed timestamps and a two-second cap after scheduling/sleep gaps. NavigationSimulationMotion retains the route cursor through loops, approaches new geometry without teleporting, re-anchors on replacement/direct transitions and holds at domain barriers. NavigationSimulationConfig/Input validate coordinates, locale-aware speed and unit conversion. NavigationSimulationDisplay labels phone and external next-turn guidance while preserving metadata.
- Domain snapshots: NavigationSessionSimulation reads Straight Line targets and routed direct guidance, geometry and exact required-stop barrier (coordinate and matched along-track distance). NavigationIntermediateArrivalTracker exposes only that read-only barrier. Arrival and hint progression remain owned by existing domain policies; simulation does not bypass them.
- Acquisition: NavigationLocationSimulation gates the optional simulator, physical seeding and GNSS. NavigationLocationController branches before requesting physical providers and pauses simulation with tracking. Physical fixes only seed a missing origin, and physical provider changes are ignored while simulated. NavigationServiceRuntime excludes simulated fixes from hardware stationary-sleep eligibility.
- Service: NavigationServiceSimulation samples preferences, owns host-only auto-drive activation and labels state. NavigationServiceLocationHandler owns this helper and filters real callbacks. NavigationServiceLocationRouting contains the existing unchanged route-result request decisions; NavigationServiceLocationControllers is the existing controller bundle extracted for explicit runtime attachment. The location clock is now injected by the service. These small wiring changes keep the zero-violation PMD baseline without suppressions.
- UI: AboutSimulationSettings/Dialog/Draft implement the deferred switch, deferred dialog/save/map actions, validation, scrollable form with fixed Save/Cancel, map initial/result contract, and Bundle restoration of unsaved speed/origin/units/map state. AboutSettingsControllers is allocated early to restore drafts, while expensive settings initialization remains delayed. All new labels/help/errors are translated into all 18 languages.
- Android Auto: ViBRoCarHostNavigation.onAutoDriveEnabled forwards to the controller, which latches activation before binding and invokes NavigationServiceBinder.enableSimulation once bound. Existing physical navigation restarts in simulated mode; already manual-simulated navigation continues. Host activation never persists the manual setting. Car destruction stops host-triggered navigation; ordinary non-auto-drive car disconnection retains existing behavior.
- Startup: retain the original phone/car location-service, permission, notification and BRouter checks. A configured origin avoids waiting for a real GPS fix; it does not bypass Android location foreground-service prerequisites. The initially attempted location-off bypass was removed after checking the official platform requirements: https://developer.android.com/develop/background-work/services/fgs/service-types#location.
- Maintained documentation: SPECIFICATION section 4.5.9 and the updated 4.5.8, AGENT architecture, and gradle/ANDROID_AUTO_REVIEW.md describe behavior and reviewer setup, including the AUTO_DRIVE command.

### Simulation verification and resume state

- Initial full run (build/simulation-full-validation.log): all 2,157 F-Droid and 2,236 Google Play tests passed, zero failures/errors/skips; PMD passed and all four APKs built. Overall failure was lint: API24 java.util.function callbacks were incompatible with minSdk23, plus three form accessibility/autofill warnings. Corrected these with app-owned callback ports and XML labels/autofill configuration. build/simulation-full-test-summary.json records the full-suite counts before focused reports replaced those XML files.
- build/simulation-final-validation.log passed in 6m18s: 29 F-Droid and 37 Google Play focused tests, both lints (0 errors, only two known existing warnings), PMD zero violations, and all four debug/release APKs. This includes exact between-vertex/lateral-stop regressions and SDK23 settings coverage.
- Final lifecycle review added explicit dialog dismissal on Activity destruction and a host update revision guard so synchronous AUTO_DRIVE activation during navigationStarted cannot restore an older physical-navigation snapshot. Regressions passed (5 F-Droid settings tests and 14 Google Play settings/host tests). That run failed only the host method's PMD complexity threshold; publishing was split into publishTrip and build/simulation-host-final-validation.log then passed (2m37s, host tests/lint/PMD/GPlay APKs).
- Missing-origin recovery now retries one-shot seed acquisition through the existing location recovery requests, without starting physical continuous updates. A regression verifies retry before the origin arrives and no further seeding afterward. An intermediate run failed because a CRLF-sensitive test edit missed incrementing the fake seed counter and duplicated a test literal; both test-only issues were corrected. build/simulation-verified-validation.log passed all requested checks and all four APK builds in 3m18s.
- Latest source decision: the normal location-enabled/permission startup checks are preserved in both flavors because the location foreground service requires them. Optional coordinates avoid waiting for a physical fix; do not reintroduce the location-off preflight bypass. AndroidNavigationPreflight.java and AndroidCarNavigationStarter.java are restored exactly to the existing committed behavior. Documentation now states the correct prerequisite.
- FINAL validation completed successfully in 4m47s: build/simulation-release-validation.log. F-Droid: 31 tests; Google Play: 40 tests; zero failures/errors/skips. This covers shared simulation/settings/controller/arrival/service paths, SDK23 form behavior, host lifecycle and car starter checks. Both flavor lints report 0 errors and only the two pre-existing warnings listed above. PMD has 0 violations. All four debug/release APKs built. build/simulation-final-summary.json records counts, warnings and artifact paths/sizes. No Gradle commands remain running.
- adb devices was checked again and showed no connected devices. No APK installation, physical driving test or DHU validation has been performed. The remaining manual checks are in gradle/ANDROID_AUTO_REVIEW.md. No version bump, Play submission, push or merge is authorized.

### Final outputs and next action

- APKs: app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk; app/build/outputs/apk/gplay/debug/app-gplay-debug.apk; app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk; app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk. Release artifacts remain unsigned and version 1.0.0/code 1000000 is unchanged.
- Diff whitespace validation passed. All source, translations, tests and maintained documentation are included in the local simulation commit. The working tree should be clean after that commit.
- Next action is manual phone/DHU verification using gradle/ANDROID_AUTO_REVIEW.md, then the normal release preparation workflow once authorized. No phone was connected at the last adb check. Do not claim Google Play acceptance from automated tests. Keep this branch plan for handoff and remove/commit its deletion before any explicitly approved merge.

## Quality-audit follow-up verification (2026-11-06)

- User asked to fix the remaining audit issues. ViBRoCarDestinationLoad owns elapsed-time deadlines, Future cancellation and attempt revisions; ViBRoCarDestinationTemplates owns bounded result lists and API-1/2 UI fallbacks. Incoming navigation preserves the query/selected destination for explicit Retry, prevents automatic timeout retry on foregrounding, and rejects late starts/results/actions, including result-before-timeout queue races and sleep/resume. No shared routing/simulation or phone search behavior changed. The Portuguese idle title/body split is corrected alongside translated safe-parking wording and timeout/retry messages.
- Scope: fix confirmed code gaps and host compatibility; preserve existing running-service reattachment. Ordinary relaunch/session recreation, pending selection/process-death continuity, performance, contrast, template quotas and driving distraction require the documented manual checks. No optional Tier-1 cluster-map feature or speculative route persistence was added. Do not claim full Google Play compliance or acceptance from automated tests.
- Initial focused work hit PMD class complexity and duplicate test literals; the lifecycle and UI were extracted into focused helpers and literals corrected. The first full invocation completed the entire Google Play suite (2,256 tests passed) but failed only on one duplicate coordinate URI in a new test. The test was strengthened to use distinct old/new destinations. Production sources did not change afterward. Full-suite counts were saved before the filtered final run: build/android-auto-followup-full-gplay-summary.json. Log: build/android-auto-followup-validation.log (6m41s).
- FINAL verification passed in 7m34s: build/android-auto-followup-final-validation.log; machine-readable evidence: build/android-auto-followup-final-summary.json. Exact command:

  ```powershell
  .\gradlew.bat lintFdroidDebug lintGplayDebug testFdroidDebugUnitTest :app:testGplayDebugUnitTest --tests 'vibro.navigator.auto.*' --tests 'vibro.navigator.android.automotive.*' assembleFdroidRelease assembleGplayRelease assembleFdroidDebug assembleGplayDebug complexityCheck --console=plain
  ```

- Results: full F-Droid 2,161 tests and final-source focused Google Play 70 tests, all passing with zero failures/errors/skips. Both lints have zero errors and only existing AppBundleLocaleChanges/AndroidGradlePluginVersion warnings. PMD has zero violations. No introduced compiler warnings remain; the two test methods asserting legacy API-1/2 action strips intentionally annotate deprecated accessor use. All four debug/release APKs built; release artifacts remain unsigned and version 1.0.0/code 1000000 is unchanged.
- git diff --check passed. adb devices was checked after verification and lists no devices; no installation or DHU/physical-car run was performed. No Gradle commands remain active. Next: complete gradle/ANDROID_AUTO_REVIEW.md on a phone/head unit, then prepare a new release only when authorized. Preserve the no-push/no-merge/no-publish constraints and remove this handoff plan before an explicitly approved merge.

## Branch coverage audit (2026-11-23)

- Audited implementation commit `fdda3863` against this plan and `SPECIFICATION.md` sections 4.5.8–4.5.9. Host templates and controls, map-only rendering, structured instructions and ETA, cluster and notification lifecycle, intent handling, 18-language parked guidance and timeout text, Car API 1 fallbacks, shared simulation and projected phone-alert silencing are present. Three implementation gaps remain open.
- BRouter setup: `AndroidCarNavigationStarter.prepare` checks the package and profile but omits routing-data availability. `ViBRoCarIncomingNavigation.finishStart` drops the pending destination as soon as service launch succeeds, before BRouter returns a result. Missing map data therefore cannot use the parked setup flow with the original destination retained for retry (specification 4.5.8).
- Destination replacement/retry: `ViBRoCarIncomingNavigation` uses one worker, and `ViBRoCarDestinationLoad` cancels the prior future with interruption. A probe using the compiled loader and an interruption-resistant lookup confirmed that the replacement can time out before its work runs. The result-generation guard correctly rejects obsolete work. The HTTP clients use separate blocking connect and read timeouts (specification 4.5.8).
- Simulation: `NavigationSimulationMotion.toward` uses the raw difference between longitudes. A probe against the compiled F-Droid class stepped toward `(0, -179.999)` from `(0, 179.999)` at 50 km/h. The target was 222.4 m away, but the first one-second step moved about 2,500 km. Direct-target simulation across the antimeridian breaks the movement bound (specification 4.5.9).
- Fresh verification below passed in 10m09s. F-Droid passed 2,161 tests and Google Play passed 2,259, with zero failures, errors or skips. Both debug lints passed with one existing `AppBundleLocaleChanges` warning per flavor. PMD reported zero violations. All four APKs built, and F-Droid release runtime dependencies include neither Android for Cars nor Google Play Services. `git diff --check` passed. Evidence is in the local ignored `build/android-auto-plan-audit-20261123.log` and Gradle reports under `app/build/`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease :app:complexityCheck :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain
  ```

- No ADB device or SDK Desktop Head Unit was available. The manual checks in `gradle/ANDROID_AUTO_REVIEW.md` and connect/disconnect with a real smartband remain pending. The checked-in release is still `1.0.0` / `1000000`. Release preparation and Play Console track replacement remain pending after verification.
- `git merge-tree --write-tree --name-only main HEAD` found a content conflict in `app/src/main/java/vibro/navigator/about/AboutActivity.java` against `main` at `2ba4dad3`. Resolve it and revalidate both flavors as part of any approved integration. Remove this handoff file before the approved merge.

## Sync with main (2026-11-23)

- The user requested branch alignment before implementing the three audit fixes. `git fetch origin main` confirmed that local `main` and `origin/main` both point to `2ba4dad3`. Merged `main` into `codex/android-auto-compliance` with a merge commit to preserve branch history.
- The sole conflict was `AboutActivity.java`. Retained immediate simulation-draft restoration and deferred BRouter street-picker restoration, saved both paths during recreation, and guarded street-picker teardown until settings exist. Retained `main`'s result dispatch, BRouter changes and toolchain (Gradle 9.9.2 / AGP 9.5.2).
- Post-merge verification passed in 12m36s. F-Droid passed 2,297 tests and Google Play passed 2,395, with zero failures, errors or skips. Both lints reported no issues, PMD found zero violations, and all four APKs built. The F-Droid runtime dependency report contains no Car App Library or Google Play Services. Evidence: local ignored `build/android-auto-main-sync-20261123.log`. The Gradle invocation has finished.

  ```powershell
  .\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease :app:complexityCheck :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain
  ```

- At the sync milestone the three audit fixes, manual Android Auto/head-unit checks and Play release work remained open. Keep this handoff plan on the active feature branch until it is ready to merge into `main`.

## Audit fixes (2026-11-23)

- The user authorized all three audit fixes on the branch synced to `main` at `251fa22c`. Keep the existing no-push, no-merge-into-main and no-publish constraints.
- Initial car-route recovery uses BRouter's actual missing-tile or missing-directory error. The service reports it through a one-shot request result. Android Auto retains the selected destination across GPS acquisition and route calculation, rejects late results, and opens the existing localized parked setup prompt with Retry and Cancel. BRouter can use its own primary/secondary volumes independently of ViBRo's street-overlay folder grants.
- Destination attempts use a bounded direct-handoff worker pool so an interrupted but still blocked provider cannot hold the only slot. The nine-second deadline and latest-attempt guard remain. If all slots are occupied, exit loading immediately with the existing retryable timeout.
- Simulation uses bounded great-circle direct steps. Polyline matching/interpolation and compass projection use wrapped longitude offsets for route segments crossing the antimeridian.
- Focused regression tests and PMD passed in 48s after correcting the service boundary for the route result and a test fixture API call. Evidence: `build/android-auto-audit-fixes-focused-rerun-20261123.log`.
- The first full run was interrupted during final review to guard both missing-map reporting and cleanup against an intervening phone request. The final full run passed in 11m58s. F-Droid passed 2,303 tests and Google Play passed 2,406, with zero failures, errors or skips. Both lints reported no issues, PMD found zero violations, and all four APKs built. The F-Droid release runtime report contains no Android for Cars or Google Play Services. `git diff --check` passed. Evidence: local ignored `build/android-auto-audit-fixes-final-20261123.log`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:complexityCheck testFdroidDebugUnitTest testGplayDebugUnitTest lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain
  ```

- No ADB device was connected during build verification. The manual review checklist now includes missing-map recovery and retry. Head-unit and smartband verification and Play release prep remain pending.
- On 2026-11-23, the user requested installation. Installed the verified Google Play debug APK from `cc534481` on the connected Huawei PRA-LX1 with `adb install -r app/build/outputs/apk/gplay/debug/app-gplay-debug.apk`. ADB confirmed `vibro.navigator.debug` version `1.0.0` / code `1000000`.

## Simulation start notice (2026-11-23)

- The user asked to remove the simulation text from the phone instruction and GPS status and show a message when simulated navigation starts. The service records one notice per successful simulated start, and the resumed phone screen consumes it in a localized dialog. Android Auto still labels projected guidance through its adapter.
- Full verification passed in 13m14s. F-Droid passed 2,304 tests and Google Play passed 2,408, with zero failures, errors or skips. Both lints and PMD reported zero issues, and all four APKs built. The Android Gradle plugin still emits its `Configuration.setVisible(boolean)` deprecation warning. Evidence: local ignored `build/simulation-notice-validation.log`. No Gradle command remains active.

  ```powershell
  .\gradlew.bat :app:complexityCheck testFdroidDebugUnitTest testGplayDebugUnitTest lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease --continue --console=plain
  ```

- Robolectric covered the shared phone dialog in both flavors, including deferred display until resume, regular instruction and GPS text, repeat suppression, a fresh simulated start, and ordinary navigation. The Google Play tests also cover the projected label and preserved guidance metadata. No phone was connected for a manual check.
- On 2026-11-23, installed the verified Google Play debug APK from `9a6ef5ae` on the Huawei PRA-LX1 with `adb install -r`. ADB confirmed the updated `vibro.navigator.debug` package, version `1.0.0` / code `1000000`.
- The user selected “Simulation Mode” as the startup dialog title and “You’re previewing navigation with simulated movement.” as its message. The dialog uses a dedicated title resource and both strings are localized in all 18 languages.
- Dialog copy verification passed in 3m20s. Both flavors passed the two dialog tests, both lints reported zero issues, PMD reported zero violations, and both debug APKs built. No Gradle command remains active. The local ignored log is `build/simulation-dialog-copy-validation.log`.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest --tests '*NavigationSimulationNoticeRobolectricTest' :app:testGplayDebugUnitTest --tests '*NavigationSimulationNoticeRobolectricTest' :app:lintFdroidDebug :app:lintGplayDebug :app:complexityCheck :app:assembleFdroidDebug :app:assembleGplayDebug --continue --console=plain
  ```
- Simulation now appears immediately after Google Fused Location and before logging in Advanced settings. Both flavors passed five settings tests each, lint reported zero issues for both, PMD reported zero violations, and both debug APKs built in 1m54s. The local ignored log is `build/simulation-settings-order-validation.log`. No Gradle command remains active.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest --tests '*AboutSettingsOrderRobolectricTest' --tests '*AboutSimulationSettingsTest' :app:testGplayDebugUnitTest --tests '*AboutSettingsOrderRobolectricTest' --tests '*AboutSimulationSettingsTest' :app:lintFdroidDebug :app:lintGplayDebug :app:complexityCheck :app:assembleFdroidDebug :app:assembleGplayDebug --continue --console=plain
  ```
- On 2026-11-23, installed the verified Google Play debug APK from `a2d5a782` on the Huawei PRA-LX1 with `adb install -r`. This build includes the selected startup dialog wording and Advanced settings order. ADB confirmed `vibro.navigator.debug`, version `1.0.0` / code `1000000`.

## Regression review (2026-11-23)

- Reviewed `0fd0d466` against `origin/main` at `2ba4dad3`. The user asked to record confirmed findings and check for additional regressions in ordinary workflows. The fixes are described below.
- Simulation can wait indefinitely for GPS on Android 6–10 if the optional origin is blank and no usable cached fix exists. `NavigationLocationController` bypasses continuous provider updates in simulation and retries one-shot seeding. `AndroidCurrentLocationSeeder` only supports one-shot fixes on Android 11 and newer. This affects F-Droid and Google Play when Google Fused Location is disabled or unavailable. Verify a cold start on API 26 without a cached fix and confirm that device fixes stop influencing simulation once the origin has been set.
- Simulation can start from the wrong location when a coarse network fix arrives before GPS. With the origin blank, `NavigationSimulator.seed` accepts the first non-null live fix and emits it with one-meter accuracy, bypassing the regular startup quality gate. The simulator then ignores a usable GPS fix that arrives immediately afterward. Verify both location settings with a coarse live fix followed by usable GPS and confirm that routing starts from the usable fix.
- Android Auto sends turn, destination and cluster distances as raw meters. `ViBRoCarManeuvers.distance` hardcodes the AndroidX display unit, replacing the former formatter that used feet and miles in imperial mode and kilometers for longer metric distances. Verify short and long distances in both measurement settings for the navigation template and cluster trip. See the [AndroidX Distance display contract](https://developer.android.com/reference/androidx/car/app/model/Distance#create(double,int)).
- Android Auto hides reroute failures when BRouter keeps the existing route. For example, if Blocked road cannot find an alternative, the phone and the previous Auto display show the warning from `NavProgressStatus.detailBlock`. The new `ViBRoCarGuidance.apply` reads that field only when paused or when there are no structured maneuvers, so ordinary active guidance resumes without explaining that the detour failed. Verify that a failed Blocked road reroute shows the warning in the car while the existing route remains active.
- Android Auto drops the shared low-confidence signal for nearby turns. The previous car display used `nextUncertain` and `afterNextUncertain` to distinguish maneuvers inside the GPS uncertainty radius. `ViBRoCarGuidance` and `ViBRoCarTrip` now send identical guidance when those flags change. Verify with a turn inside the fix accuracy radius and confirm the host guidance can convey the uncertainty without advancing the maneuver.
- Android Auto drops the known distance to the final destination whenever the ETA is unavailable. At a standstill on a multi-stop Straight Line route, `ViBRoCarGuidance.travelEstimate` discards the estimate containing the remaining distance. The routing card can still show the distance to the next stop. The former car status displayed both distances. Verify that a stationary multi-stop route shows its known remaining distance and leaves the ETA unavailable.
- Connecting to Android Auto can make guidance notifications reappear after the user blocked their category in Android settings. `AndroidNavigationNotificationChannels.guidanceAlert` moves every projected alert to the shared silent channel without checking whether the original left-turn, right-turn or general guidance channel was blocked. Preserve those category choices across connection changes. See [Android notification channel importance](https://developer.android.com/develop/ui/compose/notifications/channels#set-the-importance-level).
- Switching away from the ViBRo car map with the phone display off leaves compass UI work active. `ViBRoCarScreen.onStop` clears the surface and leaves the service's car-display visibility true until unbind. This keeps heading-driven UI snapshots enabled at up to ten per second as samples arrive. Verify that hiding the map suspends compass UI updates and retains cluster maneuver updates and stationary-orientation monitoring.
- The Android Auto map loses manual zoom during an automatic day/night change on an active route. `ViBRoCarSession.onCarConfigurationChanged` calls `ViBRoAutoSurfaceRenderer.refreshTheme`, which replaces the painter and its view-mode and gesture state even when the surface stays open. Verify that color changes retain the selected view and manual zoom or inclination. See [Android Auto dark map updates](https://developer.android.com/training/cars/apps/library/draw-maps).
- Both full flavor unit-test suites passed on the reviewed commit. F-Droid passed 2,304 tests and Google Play passed 2,408 with zero failures, errors or skips. `git diff --check` passed. The test log is `build/current-branch-regression-review.log`. A temporary Google Play Robolectric test passed in 27s. It applied the retained-route warning to a state with an active maneuver and verified that the shared status displayed it while the Android Auto template stayed identical. The test source was removed after verification. See `build/retained-route-warning-review-probe.log`. Head-unit verification remains pending.
- Two additional temporary Google Play Robolectric tests passed in 26s. One confirmed the navigation template and cluster trip stay identical after both uncertainty flags change. The other confirmed the template drops a known 500 m destination distance from a stationary multi-stop snapshot and still exposes the 120 m distance to its first stop. The test source was removed after verification. See `build/car-guidance-display-review-probe.log`.
- A temporary JVM test passed in both flavors in 40s. It reproduced accepting a 600 m accuracy network fix as a one-meter simulated origin and ignoring a later 5 m accuracy GPS fix. The regular startup selector rejected the network fix and accepted the GPS fix. The test source was removed after verification. See `build/simulation-origin-quality-review-probe.log`.
- A temporary Google Play Android 14 Robolectric test passed in 1m19s. With the left-turn channel set to `IMPORTANCE_NONE`, a projected left turn was posted to the enabled silent channel at `IMPORTANCE_LOW`. The test source was removed after verification. See `build/blocked-guidance-channel-review-probe.log`.
- The Google Play car lifecycle review ran 22 tests in 30s with zero failures, errors or skips. A temporary Robolectric test confirmed that after the car screen reaches `ON_STOP` with the phone display off, the navigation service still allows compass UI dispatch and publishes a snapshot on a compass refresh. The incoming-destination and host-navigation suites passed in the same run. The test source was removed after verification. See `build/car-lifecycle-regression-review.log`.
- The Google Play car map review ran 12 tests in 25s with zero failures, errors or skips. A temporary Robolectric test applied manual zoom to reach a 150 m viewport and confirmed that a theme refresh returned it to 300 m with the route and surface unchanged. The existing street viewport suite passed in the same run. The test source was removed after verification. See `build/car-map-theme-regression-review.log`.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest :app:testGplayDebugUnitTest --continue --console=plain
  ```

## Regression fixes (2026-11-23)

- The user authorized fixes for all nine findings in the regression review. The implementation covers both Android flavors and keeps Android Auto code in the Google Play flavor.
- Simulation now uses a temporary platform location subscription while it needs an origin. It accepts fixes that meet the route-start accuracy and freshness rules and removes the physical subscription immediately after acquiring an origin. A configured origin still skips physical acquisition.
- Android Auto uses the same unit thresholds as the phone for turn, destination and cluster distances. Host cues mark uncertain maneuvers with `≈`. A host guidance card carries the current instruction with a retained-route warning or a known destination distance when a timestamp cannot be supplied. Blocked Android notification categories remain blocked during projection.
- A covered car map keeps receiving route state for the cluster and suspends compass UI updates when the phone display is also hidden. Map color refreshes retain the driver's current zoom and view state.
- Focused verification passed 32 F-Droid and 102 Google Play tests, including an Android 8 cold-start test with Fused Location disabled. The initial checks found complexity violations in two classes. Those classes were simplified and PMD passed in the full run. See `build/branch-regression-fixes-focused-rerun.log` and `build/branch-regression-fixes-validation.log`.
- Full verification passed in 18m11s. F-Droid passed 2,307 tests and Google Play passed 2,417 with zero failures, errors or skips. Both lint reports found no issues, PMD passed, and all four debug and release APKs built. The log is `build/branch-regression-fixes-validation.log`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:complexityCheck :app:testFdroidDebugUnitTest :app:testGplayDebugUnitTest :app:lintFdroidDebug :app:lintGplayDebug :app:assembleFdroidDebug :app:assembleGplayDebug :app:assembleFdroidRelease :app:assembleGplayRelease --continue --console=plain
  ```

- The fixes are complete in code. Head-unit verification remains pending. The connected phone was left on its existing build.

## Additional regression review (2026-11-23)

- Reviewed `4453f080` against `origin/main` at `2ba4dad3`, focusing on ordinary usage beyond the nine findings already fixed. Three Android Auto findings remain open.
- Multi-stop routes lose the next stop's number, remaining distance, and ETA on the car display. The shared status still supplies these through `NavProgressStatus.stopProgressBlock`, which the old Auto display rendered. `ViBRoCarGuidance` reads the final destination estimate and omits the next-stop progress. An intermediate arrival cue does not restore that progress between stops.
- A failed Blocked road action causes Android Auto to replace its native turn card with a text card. `ViBRoCarGuidance` chooses `MessageInfo` whenever the retained-route warning is present, dropping the host turn icon and structured distance while the old route is still guiding. The text cue and cluster steps remain available. `NavigationSessionRouteDisplayState` carries the warning on subsequent snapshots until the recorded failure is cleared.
- A new snapshot timestamp alone invalidates the car template and republishes the cluster trip and ongoing car notification. `ViBRoCarTrip` uses the snapshot wall time when constructing step arrival timestamps, and `ViBRoCarHostNavigation` updates the notification whenever the trip changes, even with identical cue text. This occurs during routine accepted location updates with unchanged displayed distances and remaining durations. The platform recommends cluster updates based on speed and rounded distance changes. [AndroidX NavigationManager reference](https://developer.android.com/reference/androidx/car/app/navigation/NavigationManager)
- A focused Google Play run passed 16 tests in 54s with zero failures, errors, or skips. Three temporary Robolectric tests reproduced the reported behavior. The temporary source was removed after verification. The ignored log is `build/additional-branch-regression-review.log`. `git diff --check` passed. Production code and the phone install were untouched. Head-unit and smartband verification remain pending. No Gradle commands remain active.

  ```powershell
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarRegressionReviewProbeTest' --tests '*ViBRoCarGuidanceGplayTest' --tests '*ViBRoCarHostNavigationTest' --console=plain
  ```

## Follow-up regression review (2026-11-23)

- Reviewed `06f54724` against `origin/main` at `2ba4dad3` for additional issues in ordinary workflows. The three findings from the prior review remain open.
- Switching away from an idle ViBRo car screen leaves the one-second service attachment timer active until the screen is destroyed. With no navigation service running, every tick tries to bind and cancels the car notification again. The base branch removed its timer at screen stop. The probe moved the screen to `ON_STOP` and verified the timer and notification cancellation over successive ticks.
- Retrying a destination search on a poor connection can exhaust the Android Auto host's template quota. An initial timeout followed by two timed-out retries sent six consecutive loading/timeout `MessageTemplate` messages on the same screen. Their message text alternates, so these changes count as new steps under the documented `MessageTemplate` refresh rule. A host enforcing the five-step limit can close the app when the sixth step is delivered. The probe verified the emitted sequence. Actual enforcement remains to be checked on a head unit. [Android for Cars template restrictions](https://developer.android.com/training/cars/apps/library/template-restrictions) and [MessageTemplate refresh rules](https://developer.android.com/reference/androidx/car/app/model/MessageTemplate)
- The final focused Google Play run passed 18 tests in 28s with zero failures, errors, or skips. The first idle probe used Robolectric's default successful bind and was corrected to model the absent navigation service. Both temporary probes were removed after the run. The ignored final log is `build/branch-regression-followup-review-rerun.log`. Production code and the phone install were untouched. No Gradle commands remain active.

  ```powershell
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarIdleReviewProbeTest' --tests '*ViBRoCarIncomingNavigationGplayTest' --tests '*ViBRoCarDestinationTemplatesTest' --console=plain
  ```

## Follow-up guidance and notification review (2026-11-23)

- Reviewed `58d300a3` against `origin/main` at `2ba4dad3` for further ordinary driving regressions. The findings already recorded above remain open.
- The main Android Auto turn card loses the distance and estimated time between the first and second turns. The old surface displayed `afterNextLine` with both values. The normal `RoutingInfo` path now uses only the following maneuver type and instruction text. The probe compared a second turn 20 m after the first with one 500 m after it. Both produced identical main-display routing information. The cluster still received the two different distances from the current position. Host step cues support distance and duration spans. [AndroidX Step.Builder reference](https://developer.android.com/reference/androidx/car/app/navigation/model/Step.Builder)
- Android Auto puts live turn instructions in the phone notification shade even with ViBRo's Notifications setting off. The ordinary transient alert was suppressed in the projection probe, but `AndroidCarNavigationNotifications` still posted the cue as the phone notification's text. The base phone ongoing notification carried generic navigation status. The car extender supports independent car text. [AndroidX CarAppExtender.Builder reference](https://developer.android.com/reference/androidx/car/app/notification/CarAppExtender.Builder)
- The final focused Google Play run passed 12 tests in 33s with zero failures, errors, or skips. Two temporary Robolectric probes reproduced the findings and were removed after verification. The ignored final log is `build/car-guidance-phone-notification-review-final.log`. Head-unit rendering and real notification mirroring remain unverified. Production code and the phone install were untouched. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarGuidanceGplayTest' --tests '*AndroidAutoGuidanceNotificationsTest' --tests '*AndroidCarNavigationNotificationsGplayTest' --console=plain
  ```

## Follow-up simulation route review (2026-11-23)

- Reviewed `7351c68c` against `origin/main` at `2ba4dad3`. Found one further issue in the shared route simulator.
- The simulator can skip the planned road leg after reaching an intermediate stop. On an errands route where the driver passes Stop 2 on the way to Stop 1 and returns to Stop 2 via a different street, `NavigationIntermediateArrivalTracker` matches Stop 2 to its first occurrence on the entire route. Its simulation barrier is then behind the current cursor. `NavigationSimulationMotion` treats that barrier as reached along the route and heads straight to Stop 2, bypassing the planned streets. A temporary integration probe used two ordered stops and verified that after confirming Stop 1, the next simulated fix heads back down the outbound street instead of taking the planned turn onto the return leg. This is specific to simulated movement.
- The focused simulation run passed 18 tests per flavor in 18s with zero failures, errors, or skips. The temporary probe reproduced the issue in both builds and was removed. The ignored log is `build/simulation-route-order-review.log`. Production code and the phone install were untouched. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest --tests '*NavigationSimulationArrivalTest' --tests '*NavigationSimulatorTest' :app:testGplayDebugUnitTest --tests '*NavigationSimulationArrivalTest' --tests '*NavigationSimulatorTest' --console=plain
  ```

## Follow-up live navigation and car speed review (2026-11-23)

- Reviewed `9699bb33` against `origin/main` at `2ba4dad3`. Found one further user-visible change. ViBRo lost its over-limit speed cue on the car display. The old Auto GPS status made the current speed bold above the limit. The move to host templates removed that GPS status line, and the remaining map badge shows the posted limit with the same styling at any speed. The probe compared 45 km/h and 55 km/h on a 50 km/h section. The shared speed check correctly detected the change, while the rendered badge and host template remained identical. The phone's speed cue still uses the shared check.
- The live navigation checks passed 62 F-Droid and 83 Google Play tests in 41s, covering service lifecycle, route requests and replacement, location recovery, car ownership and viewport behavior. The focused car speed, viewport and guidance run passed 17 Google Play tests in 29s. Both runs had zero failures, errors, or skips. The temporary speed probe was removed after verification. Logs are `build/followup-live-navigation-review.log` and `build/android-auto-speed-cue-review.log`. Physical head-unit rendering remains unverified. Production code and the phone install were untouched. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest --tests '*NavigationLifecycleRobolectricTest' --tests '*NavigationService*Test' --tests '*NavigationRouteRequestManagerTest' :app:testGplayDebugUnitTest --tests '*NavigationLifecycleRobolectricTest' --tests '*NavigationService*Test' --tests '*NavigationRouteRequestManagerTest' --tests '*ViBRoCarHostNavigationTest' --tests '*ViBRoAutoStreetViewportGplayTest' --console=plain
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoAutoSpeedCueReviewProbeTest' --tests '*ViBRoAutoStreetViewportGplayTest' --tests '*ViBRoCarGuidanceGplayTest' --console=plain
  ```

## Follow-up simulation notification review (2026-11-23)

- Reviewed `428b9fa2` against `origin/main` at `2ba4dad3`. The user confirmed that the phone's one-time Simulation Mode dialog provides sufficient notice at startup and accepted the regular wording of simulated maneuver and arrival notifications, including those mirrored to wearables. This finding is closed. Specification section 4.5.9 records the accepted behavior and the existing Simulation label in Android Auto guidance.
- An end-to-end Robolectric probe started a simulated Straight Line route with an intermediate stop in each flavor. Normal arrival confirmation posted an unlabeled stop-arrival alert with mirroring allowed. Actual wearable delivery remains unverified. The focused run passed 7 F-Droid and 8 Google Play tests in 52s with zero failures, errors, or skips. The source for the temporary probe was removed. The ignored log is `build/simulation-external-alert-review.log`. Production code and the phone install were untouched. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest --tests '*NavigationSimulationNoticeRobolectricTest' --tests '*NavigationForegroundControllerTest' :app:testGplayDebugUnitTest --tests '*NavigationSimulationNoticeRobolectricTest' --tests '*NavigationForegroundControllerTest' --tests '*ViBRoCarSimulationDisplayTest' --console=plain
  ```

## Follow-up location and settings review (2026-11-23)

- Reviewed `07fe7f44` against `origin/main` at `2ba4dad3`. The additional pass covered regular GPS provider selection and recovery, recorded stationary and moving location traces, route matching, simulation settings and map-picker recreation, and Android Auto integration settings. No further meaningful regressions were confirmed in these workflows. Prior open findings and pending physical head-unit verification remain recorded above.
- The focused run passed 47 F-Droid and 57 Google Play tests in 57s with zero failures, errors, or skips. The ignored log is `build/followup-shared-location-and-settings-review.log`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest --tests '*NavigationLocationController*Test' --tests '*LiveLocationHysteresisReplayTest' --tests '*StationaryDeskLogReplayTest' --tests '*PolylineIndexTest' --tests '*AboutSimulation*Test' :app:testGplayDebugUnitTest --tests '*NavigationLocationController*Test' --tests '*LiveLocationHysteresisReplayTest' --tests '*StationaryDeskLogReplayTest' --tests '*PolylineIndexTest' --tests '*AboutSimulation*Test' --tests '*ViBRoCarAppServiceSettingsGplayTest' --console=plain
  ```

## Fixes for the remaining review findings (2026-11-24)

- The user authorized fixing the nine open findings above. The accepted wording of simulated phone and wearable alerts remains in effect. Started from `06333577` with the user's `1.0.1` version update in place.
- Implemented native car guidance with next-stop progress, retained-route warnings and relative following-turn metrics. Template and cluster publication ignore timestamp-only differences. Car notifications update independently with the instruction and carry that instruction in the car extension. The phone-facing text is generic. Restored the over-limit badge cue.
- Destination retry and setup transitions refresh a stable host status pane. Car service attachment polling now stops with the screen. The active binding keeps receiving cluster updates when covered, and the integration preference observer handles disabling from the phone. The simulator, arrival tracking and display guidance share stop locations matched in requested order.
- The focused run passed 30 F-Droid and 121 Google Play tests and the complexity check. The full run before the final shared stop-matching adjustment passed 2,308 F-Droid and 2,424 Google Play tests with zero failures, errors, or skips. Both debug lints reported no issues, and both flavors built debug and release APKs. The ignored logs are `build/followup-regression-fixes-focused-rerun.log` and `build/followup-regression-fixes-full.log`.
- Final verification passed 517 F-Droid and 600 Google Play tests with zero failures, errors, or skips. Complexity passed, both debug lints reported no issues, and all four debug and release APKs were rebuilt. The rerun took 4m 15s and its ignored log is `build/followup-regression-fixes-final-rerun.log`. No Gradle invocation remains active. Phone installation and physical head-unit verification remain pending because no device or Desktop Head Unit was available.

  ```powershell
  $checks = ':app:lintFdroidDebug', ':app:lintGplayDebug', ':app:assembleFdroidDebug', ':app:assembleGplayDebug', ':app:assembleFdroidRelease', ':app:assembleGplayRelease'
  .\gradlew.bat :app:complexityCheck :app:testFdroidDebugUnitTest :app:testGplayDebugUnitTest @checks --continue --console=plain
  $sharedTests = @()
  foreach ($pattern in 'vibro.navigator.nav.session.*', 'vibro.navigator.nav.guidance.*', 'vibro.navigator.nav.presentation.*', '*CompassRouteGeometryFactoryTest', '*PolylineIndexTest', '*NavigationSimulatorTest') {
      $sharedTests += '--tests', $pattern
  }
  $carTests = '--tests', 'vibro.navigator.auto.*', '--tests', '*AndroidCarNavigationNotificationsGplayTest'
  .\gradlew.bat :app:complexityCheck :app:testFdroidDebugUnitTest @sharedTests :app:testGplayDebugUnitTest @sharedTests @carTests @checks --continue --console=plain
  ```

## Follow-up branch review (2026-11-24)

- Reviewed `48c81e53` against `main` at `2ba4dad3`, focusing on common navigation workflows and findings beyond those already fixed. One additional Android Auto regression is open.
- When BRouter snaps the route start away from the current position, the car turn card drops the time to reach the road and the first turn after joining it. The previous car painter displayed the shared current and following cues. The current `NavigationSessionRouteDisplayState` keeps both lines and supplies a single structured beeline maneuver. `ViBRoCarGuidance` uses that maneuver's instruction and distance and only renders a following step when a second structured maneuver exists. The shared phone guidance still includes both cues. Preserve the beeline as the active maneuver and present the available approach time and following cue in the host template.
- The full F-Droid and Google Play unit-test suites passed on the reviewed commit in 7m58s, with 2,308 and 2,424 tests respectively and zero failures, errors or skips. The focused Google Play run passed 15 tests in 37s, including a temporary Robolectric probe that built a snapped route-start state and confirmed the omissions in the emitted navigation template. The probe was removed. `git diff --check main...HEAD` passed. Production code and the phone install were untouched. Physical head-unit verification remains pending. Local ignored logs are `build/current-head-regression-review.log` and `build/current-head-beeline-review.log`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testFdroidDebugUnitTest :app:testGplayDebugUnitTest --continue --console=plain
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarApproachReviewProbeTest' --tests '*ViBRoCarGuidanceGplayTest' --tests '*NavigationRouteStartApproachStateTest' --console=plain
  ```

## Follow-up simulation handoff review (2026-11-24)

- Reviewed the production source at `48c81e53` again after the documentation-only commit `742c7089`. Found an additional regression in the shared simulator on a routed trip with an off-road intermediate stop. BRouter's normal route augmentation adds a beeline from the road to the stop and a second leg back to the same road point. Once the service completes the return leg, the simulator uses an unrestricted polyline match. The overlapping geometry can place its cursor back on the completed outbound leg, so it retraces the detour before continuing to the destination. Live route logic already constrains the match to the completed beeline's endpoint or later. This differs from the previously fixed case of passing a later on-road stop early.
- The temporary integration probe reproduced the repeat detour in both flavors at the default 50 km/h with an approximately 89 m off-road leg and at 30 km/h with an approximately 111 m leg. The final focused run passed 26 F-Droid and 50 Google Play tests in 26s with zero failures, errors or skips. It also covered arrival, car destination handling, host ownership and screen lifecycle. The probe was removed after verification. Production code and the phone install were untouched. Head-unit verification remains pending. The local ignored log is `build/simulation-spur-regression-final-review.log`. No Gradle invocation remains active.

  ```powershell
  $sharedTests = '--tests', '*NavigationSimulationSpurReviewProbeTest', '--tests', '*NavigationSimulationArrivalTest', '--tests', '*NavigationRouteBeelineStateTest', '--tests', '*NavigationSimulatorTest'
  $carTests = '--tests', '*ViBRoCarIncomingNavigationGplayTest', '--tests', '*ViBRoCarHostNavigationTest', '--tests', '*ViBRoCarScreenLifecycleTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @sharedTests :app:testGplayDebugUnitTest @sharedTests @carTests --continue --console=plain
  ```

## Follow-up Android Auto ETA review (2026-11-24)

- Reviewed the production source at `48c81e53` after the documentation-only commit `295ba552`. Found an additional Android Auto regression during a traffic stop on a routed trip. BRouter's route-model duration remains known and steady while the vehicle is stopped. The shared destination and next-stop arrival clocks therefore move later with each new observation. The car template comparison substitutes the same wall time for both snapshots, so it discards even minute-level changes to the ETA the driver sees. The prior Auto status block displayed the actual updated shared values. Preserve routine snapshot deduplication while allowing a material change to the last published arrival clocks to refresh the template.
- A temporary Robolectric probe used 120 successive stationary route snapshots and confirmed that the screen would request no template refresh even though the next-stop text and destination arrival timestamp had moved two minutes. The focused run passed 28 F-Droid and 17 Google Play tests in 21s, with zero failures, errors or skips. The probe was removed. Production code and the phone install were untouched. Head-unit rendering remains unverified. The local ignored log is `build/traffic-eta-regression-review.log`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarTrafficEtaReviewProbeTest' --tests '*ViBRoCarGuidanceGplayTest' --tests '*ViBRoCarHostNavigationTest' :app:testFdroidDebugUnitTest --tests '*NavStateTest' --tests '*RouteTimeEstimatorTest' --continue --console=plain
  ```

## Follow-up navigation lifecycle and U-turn review (2026-11-24)

- Reviewed production source at `48c81e53` after the documentation-only commit `4539d05e`. Found one additional regression in Android Auto for a standard 180-degree reversal on the same road. BRouter emits command 15 for a U-turn with no prescribed side. The previous car display drew the shared U-turn symbol and text. The new car adapter sends `TYPE_UNKNOWN` without a maneuver icon and keeps the U-turn text. The cluster trip receives the same unknown maneuver. AndroidX documents that some instrument clusters can skip unknown maneuvers, so verify cluster rendering separately. [BRouter command generation](https://github.com/abrensch/brouter/blob/master/brouter-core/src/main/java/btools/router/VoiceHint.java#L95-L103) and [AndroidX cluster guidance](https://developer.android.com/reference/androidx/car/app/navigation/NavigationManager#updateTrip(androidx.car.app.navigation.model.Trip)).
- Lifecycle checks passed for 30 F-Droid and 72 Google Play tests in 1m23s, covering pause/resume, arrival, foreground notifications, car binding visibility, the simulation notice, and street viewport state. The focused Google Play rerun passed 21 tests in 18s. A temporary Robolectric probe confirmed that the symbol was present in shared guidance and absent from the emitted head-unit and cluster maneuver data. The probe was removed. No production code or phone install changed. Physical head-unit and cluster rendering remain unverified. Local ignored logs are `build/navigation-lifecycle-followup-review.log` and `build/car-u-turn-regression-review-rerun.log`. No Gradle invocation remains active.

  ```powershell
  $shared = '--tests', '*NavigationLifecycleRobolectricTest', '--tests', '*NavigationArrivalLifecycleTest', '--tests', '*NavigationForegroundControllerTest', '--tests', '*NavigationServiceUiVisibilityTest', '--tests', '*NavigationSimulationNoticeRobolectricTest'
  $car = '--tests', '*AndroidCarNavigationNotificationsGplayTest', '--tests', '*AndroidAutoGuidanceNotificationsTest', '--tests', '*ViBRoCarHostNavigationTest', '--tests', '*ViBRoCarScreenLifecycleTest', '--tests', '*ViBRoAutoStreetViewportGplayTest', '--tests', '*ViBRoCarIncomingNavigationGplayTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarUTurnReviewProbeTest' --tests '*ViBRoCarGuidanceGplayTest' --tests '*ViBRoCarHostNavigationTest' --tests '*VoiceHintMapperTest' --console=plain
  ```

## Follow-up car guidance and speed review (2026-11-24)

- Reviewed production source at `48c81e53` after the documentation-only commit `b1f26357`. The prior route-start finding also affects ordinary road turns. The active route card leaves out the known time to the current maneuver for every structured step. The prior car display showed it in the current instruction. A probe compared the same right turn with 12 and 47 seconds remaining. The shared instruction and cluster duration reflected both values, and the main routing card was identical. The native distance field takes a distance, and the cue supports `DurationSpan` for an optional duration. [AndroidX current step](https://developer.android.com/reference/androidx/car/app/navigation/model/RoutingInfo.Builder#setCurrentStep(androidx.car.app.navigation.model.Step,androidx.car.app.model.Distance)) and [AndroidX step cue](https://developer.android.com/reference/androidx/car/app/navigation/model/Step.Builder#setCue(java.lang.CharSequence)).
- Android Auto no longer displays numeric current speed. The previous visible status line included GPS speed, and the current overlay uses the shared value to determine the color of the posted-limit badge. The earlier fix restored the over-limit color change. A probe compared 20 and 40 km/h with a 50 km/h limit and with no reported limit. The car overlay and template were identical in both comparisons, while shared phone status showed the actual speed. Android Auto's navigation design guidance permits a current-speed map overlay. [Android Auto navigation template](https://developer.android.com/design/ui/cars/guides/templates/navigation-template).
- The focused run passed 26 F-Droid and 21 Google Play tests in 31s with zero failures, errors or skips. Both temporary probes reproduced the behavior and were removed. Production code and the phone install were untouched. Actual head-unit rendering remains unverified. The ignored log is `build/live-car-cue-regression-review.log`. No Gradle invocation remains active.

  ```powershell
  .\gradlew.bat :app:testGplayDebugUnitTest --tests '*ViBRoCarLiveCueReviewProbeTest' --tests '*ViBRoCarGuidanceGplayTest' --tests '*ViBRoAutoSpeedLimitTest' --tests '*ViBRoAutoStreetViewportGplayTest' :app:testFdroidDebugUnitTest --tests '*NavigationSessionRouteStateTest' --tests '*NavigationArrivalLifecycleTest' --continue --console=plain
  ```

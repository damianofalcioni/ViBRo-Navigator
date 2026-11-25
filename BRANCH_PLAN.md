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

## Android Auto GPS status line request (2026-11-24)

- The user chose to restore the phone's full compact GPS status line on the Android Auto navigation display. This replaces the proposed standalone current-speed readout. Show current speed, elevation, horizontal GPS accuracy, satellites used in the fix, and the live countdown to the next position evaluation. Reuse the phone's formatting, units, and unavailable-value behavior.
- Keep the posted speed-limit badge and its over-limit highlight. Check on a head unit that the full line fits the available area in day and night themes and that the countdown updates while the map is visible.
- The initial request was for a plan update. The implementation authorized later is recorded under Fixes for open regressions.

## Android Auto time to maneuver request (2026-11-24)

- Show the estimated time to the current maneuver in the main Android Auto instruction text when a duration is available. Apply this to ordinary turns and direct guidance, including the approach to a snapped route start. Keep the native distance beside the maneuver symbol.

## Follow-up route replacement and simulation review (2026-11-24)

- Reviewed `e5771295` against `main` at `2ba4dad3`. The pass covered destination replacement while routing, route result ownership and recovery, deviation rerouting, simulator activation and stop, and arrival sequencing. No additional meaningful regression was confirmed in those workflows. Prior findings and planned Android Auto work remain open.
- The focused run passed 59 F-Droid and 90 Google Play tests in 24s with zero failures, errors or skips. `git diff --check main...HEAD` passed. No ADB device or Desktop Head Unit was available for physical validation. The ignored log is `build/route-replacement-followup-review.log`. No Gradle invocation remains active.

  ```powershell
  $shared = '--tests', '*NavigationServiceInitialRouteTest', '--tests', '*NavigationServiceSimulationTest', '--tests', '*NavigationRouteRequestManagerTest', '--tests', '*NavigationSessionLocationReplacementTest', '--tests', '*NavigationSessionRouteStateDeviationTest', '--tests', '*NavigationSimulationArrivalTest', '--tests', '*NavigationSimulatorTest'
  $car = '--tests', '*ViBRoCarIncomingNavigationGplayTest', '--tests', '*ViBRoCarHostNavigationTest', '--tests', '*ViBRoCarDestinationLoadTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  ```

## Fixes for open regressions (2026-11-24)

- The user authorized implementing the open findings after `286ac68a`. Restore the full compact GPS line on Android Auto, show time to the current maneuver and the first road cue following a beeline, refresh visible arrival clocks, restore the neutral U-turn symbol, and keep the simulator beyond a completed off-road detour. Retain the previously agreed navigation behavior and keep the changes on the working branch.
- The car map uses the phone's status and countdown formatter. The navigation card sends duration as cue text and retains the host distance field. The next road cue during a beeline is a preview on the main card. A U-turn with no prescribed side supplies a neutral symbol in the host step. The main template compares arrival minutes against the last published card and cluster publication still compares relative progress. Simulated movement resumes using the live domain's completed-beeline segment boundary.
- The focused rerun passed 27 F-Droid and 59 Google Play tests plus the complexity check. It covered the off-road stop, current and following cues, arrival refresh, U-turn icon, and GPS line fit and countdown. The initial focused run found two complexity violations that were resolved for the rerun. Logs are `build/open-regression-fixes-focused.log` and `build/open-regression-fixes-focused-rerun.log`.
- The combined run passed 2,309 F-Droid tests, both lints, complexity, and all four debug and release APK builds. The F-Droid release runtime classpath contains no Android for Cars or Google Play Services dependencies. Google Play had one outdated maneuver-cue assertion and a Java heap exhaustion in the translation test. Updated the assertion for the restored duration. The isolated Google Play rerun passed all 2,428 tests in 4m17s with a temporary 3 GiB test heap. Complexity passed and both debug lint reports were empty. All final tests had zero failures, errors or skips. Logs are `build/open-regression-fixes-full.log` and `build/open-regression-fixes-gplay-rerun.log`. No Gradle invocation remains active.
- No ADB device or Desktop Head Unit was available. The head-unit checklist covers GPS layout, cue placement, arrival clocks, and the neutral U-turn icon. Some instrument clusters can omit unknown maneuver types, including a neutral U-turn with a supplied icon.

  ```powershell
  $checks = ':app:complexityCheck', ':app:testFdroidDebugUnitTest', ':app:testGplayDebugUnitTest', ':app:lintFdroidDebug', ':app:lintGplayDebug', ':app:assembleFdroidDebug', ':app:assembleGplayDebug', ':app:assembleFdroidRelease', ':app:assembleGplayRelease'
  .\gradlew.bat @checks :app:dependencies --configuration fdroidReleaseRuntimeClasspath --continue --console=plain
  $initScript = @'
  gradle.projectsEvaluated {
      allprojects {
          tasks.withType(Test).configureEach {
              if (name == 'testGplayDebugUnitTest') {
                  maxHeapSize = '3g'
              }
          }
      }
  }
  '@
  Set-Content -LiteralPath build/open-regression-fixes-test-heap.init.gradle -Encoding ascii -Value $initScript
  .\gradlew.bat -I build/open-regression-fixes-test-heap.init.gradle :app:testGplayDebugUnitTest :app:lintGplayDebug :app:complexityCheck --continue --console=plain
  ```

## Follow-up review after the open regression fixes (2026-11-24)

- Reviewed `98b83ad7` against `main` at `2ba4dad3` for additional regressions in ordinary use. One Android Auto finding remains open. When a driver starts away from a routable road or approaches an off-road stop, the main car turn card loses the beeline symbol that the previous car screen displayed. The car adapter sends BRouter command 16 as `TYPE_UNKNOWN` without an icon in both the navigation template and cluster trip. The localized cue, distance, and available duration are still present. AndroidX allows a custom icon for this type and documents that some clusters skip unknown maneuvers. [AndroidX current-step icon](https://developer.android.com/reference/androidx/car/app/navigation/model/RoutingInfo.Builder#setCurrentStep(androidx.car.app.navigation.model.Step,androidx.car.app.model.Distance)) and [AndroidX cluster guidance](https://developer.android.com/reference/androidx/car/app/navigation/NavigationManager#updateTrip(androidx.car.app.navigation.model.Trip)).
- The focused run passed 18 F-Droid and 107 Google Play tests in 1m26s with zero failures, errors, or skips. A temporary Robolectric probe confirmed the symbol in shared guidance and its absence from both emitted car models. The probe was removed. The local ignored log is `build/current-branch-additional-review.log`. `git diff --check main...HEAD` passed. Physical head-unit rendering remains unverified. No Gradle invocation remains active.

  ```powershell
  $shared = '--tests', '*NavigationSimulationArrivalTest', '--tests', '*NavigationServiceInitialRouteTest', '--tests', '*NavigationServiceUiVisibilityTest', '--tests', '*NavigationGpsDetailsDialog*Test'
  $car = '--tests', 'vibro.navigator.auto.*', '--tests', '*AndroidAutoGuidanceNotificationsTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  ```

## Follow-up arrival, destination search, and phone rendering review (2026-11-24)

- Reviewed `3b9a117b` against `main` at `2ba4dad3`. Three additional findings are open. The beeline icon finding remains open above.
- The Android Auto template still sends a destination ETA after confirmed arrival. `ViBRoCarGuidance` converts the confirmed-arrival estimate into a zero-distance, zero-time card whose arrival timestamp comes from the latest observation. Two minutes of subsequent fixes move that timestamp two minutes later and trigger another template refresh. The previous car UI showed the arrival confirmation alone. Suppress the destination estimate and its minute-based invalidation after confirmed arrival.
- Searching for a replacement destination during an active route leaves the car map marked visible while a search pane or result list occupies the same `Screen`. Even surface destruction leaves the visibility flag set. With the phone display off, this allows unnecessary compass UI snapshots at up to ten per second while the map is hidden. Track map visibility independently of the car screen lifecycle and retain car guidance updates during search. [Android for Cars map-capable templates](https://developer.android.com/training/cars/apps/library/draw-maps)
- The phone GPS line creates a localized Android configuration context on every render in both flavors, including renders driven by compass samples. The renderer performs this work before checking whether the line changed. The previous phone path reused its localized activity. A probe counted 40 configuration contexts across 40 identical reads. Reuse the localized resources across GPS status updates. Device performance and power impact were not measured.
- The first focused run passed 24 F-Droid and 114 Google Play tests in 1m15s. The second passed 4 F-Droid and 7 Google Play tests in 49s. Both completed with zero failures, errors, or skips. Temporary Robolectric probes reproduced all three findings and were removed. The local ignored logs are `build/arrival-display-visibility-regression-review.log` and `build/navigation-gps-render-regression-review.log`. `git diff --check main...HEAD` passed. Head-unit rendering remains unverified. No Gradle invocation remains active.

  ```powershell
  $shared = '--tests', '*NavigationLifecycleRobolectricTest', '--tests', '*NavigationArrivalLifecycleTest', '--tests', '*NavigationServiceUiVisibilityTest'
  $car = '--tests', 'vibro.navigator.auto.*', '--tests', '*AndroidAutoGuidanceNotificationsTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  $shared = '--tests', '*NavigationGpsStatusResourcesReviewProbeTest', '--tests', '*NavigationInstructionMetricRobolectricTest', '--tests', '*NavigationLandscapeTextLayoutRobolectricTest'
  $car = '--tests', '*ViBRoCarArrivalAndVisibilityReviewProbeTest', '--tests', '*ViBRoAutoGpsStatusPainterTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  ```

## Follow-up simulation startup and localization review (2026-11-24)

- Reviewed `c63cf10c` against `main` at `2ba4dad3` for additional ordinary-use regressions. Two shared simulation findings are open in both flavors. Previously recorded Android Auto and phone rendering findings remain open.
- Simulation with a blank origin can wait for a fresh fix despite having a usable cached GPS fix. The generic startup cache selector admits fixes up to 50 m accuracy and prefers a newer fix. The simulator rejects the selected result if its accuracy exceeds 25 m and never sees the older eligible candidate. A probe reproduced this with a three-second-old 5 m GPS fix and a one-second-old 40 m network fix. This differs from the previously fixed acceptance of an inaccurate live origin. Apply the simulator's route-start eligibility checks to each cached candidate before ranking them and preserve the live navigation cache policy.
- The Simulation speed dialog uses the device's default numeric locale with a manually selected app language. On an English device with German selected in ViBRo, a saved fractional speed appears with a period and entering a comma decimal fails validation. Use the effective app locale to format the draft and parse its speed. Keep the coordinate field's decimal-point format.
- Focused verification passed 65 F-Droid and 65 Google Play tests in 38s with zero failures, errors, or skips. Two temporary probes reproduced both findings in each flavor and were removed. The local ignored log is `build/simulation-startup-and-locale-regression-review.log`. `git diff --check main...HEAD` passed. No Gradle invocation remains active.

  ```powershell
  $shared = '--tests', '*NavigationSimulationCachedOriginReviewProbeTest', '--tests', '*AboutSimulationLocaleReviewProbeTest', '--tests', '*NavigationLocationController*Test', '--tests', '*NavigationStartupLocationSelectorTest', '--tests', '*NavigationSimulationArrivalTest', '--tests', '*NavigationSimulatorTest', '--tests', '*AboutSimulation*Test'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared --continue --console=plain
  ```

## Follow-up Android Auto destination result review (2026-11-24)

- Reviewed `1bfae197` against `main` at `2ba4dad3`. One additional finding is open in Google Play. The car address picker shows a place and its OpenStreetMap entrances with identical row titles and no distinguishing text. `OsmNominatimSearchParser` returns entrance coordinates and types alongside the parent place while deliberately reusing the parent label. The phone picker uses `PoiSuggestion` to show the localized entrance type. `ViBRoCarDestinationTemplates` uses only `Poi.displayLabel()`, so a driver can select the service entrance while intending to go to the main entrance. Carry entrance labels into the car rows and keep the chosen coordinates.
- A temporary Robolectric probe passed a station, its main entrance, and its service entrance through the production response parser, built the car list, and invoked all three row actions. The emitted rows were visually identical and selected different coordinates. The phone picker labels distinguished all three. The focused run passed 37 F-Droid and 70 Google Play tests in 49s, including destination replacement, car lifecycle, and projection notification tests, with zero failures, errors, or skips. The probe was removed. The ignored log is `build/android-auto-search-results-followup-review.log`. `git diff --check main...HEAD` passed. Physical head-unit rendering remains unverified. No Gradle invocation remains active.

  ```powershell
  $shared = '--tests', '*OsmNominatimClientTest', '--tests', '*PoiSuggestion*Test', '--tests', '*NavigationLifecycleRobolectricTest'
  $car = '--tests', '*ViBRoCarEntranceResultReviewProbeTest', '--tests', '*ViBRoCarDestinationTemplatesTest', '--tests', '*ViBRoCarIncomingNavigationGplayTest', '--tests', '*ViBRoCarHostNavigationTest', '--tests', '*ViBRoCarScreenLifecycleTest', '--tests', '*AndroidAutoGuidanceNotificationsTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  ```

## Follow-up guidance settings and notification review (2026-11-24)

- Reviewed `22572075` against `main` at `2ba4dad3`, focusing on ordinary guidance settings, maneuver speech, projection notification behavior, and shared phone/car navigation actions. No additional meaningful regression was confirmed in this pass. Previously recorded findings remain open.
- Focused verification passed 63 F-Droid and 157 Google Play tests in 1m18s with zero failures, errors, or skips. `git diff --check main...HEAD` passed. The local ignored log is `build/guidance-settings-followup-review.log`. No Gradle invocation remains active. Head-unit rendering and wearable mirroring remain unverified.

  ```powershell
  $shared = '--tests', '*NavigationForegroundControllerTest', '--tests', '*Notification*Test', '--tests', '*NavigationManeuverSpeechFormatterTest', '--tests', '*NavigationCompassModeControllerTest', '--tests', '*NavigationInstructionMetric*Test', '--tests', '*NavigationLifecycleRobolectricTest', '--tests', '*NavigationSettingsButtonRobolectricTest'
  $car = '--tests', 'vibro.navigator.auto.*', '--tests', 'vibro.navigator.android.automotive.*'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --continue --console=plain
  ```

## Fixes for the seven remaining findings (2026-11-24)

- The user authorized fixing all seven open findings. Start from `d1f68ac8`. Preserve structured car guidance, cluster ownership, live tracking, and the existing simulation and notification behavior. The implementation covers beeline symbols, confirmed-arrival ETA, map visibility during destination search, phone GPS resources, simulation origin selection and decimal input, and entrance labels in car search.
- All seven findings are fixed in code with regression coverage. The car template and cluster trip share a beeline icon. Confirmed arrival clears the destination ETA and stays stable as new fixes arrive. The car screen gates map updates on the navigation template and the surface while retaining the guidance binding. The car destination picker reuses phone entrance labels and carries the selected coordinates.
- The phone GPS line and details dialog share localized resources. Simulation ranks eligible cached fixes and uses the selected app locale for speed display and entry. The speed draft retains its numeric locale across activity recreation, and coordinates continue to use decimal points.
- The first focused run passed 89 F-Droid and 180 Google Play tests. It flagged a duplicated reflection literal in the new car lifecycle test. The first full invocation was cancelled after PMD flagged the cache time selection in the location controller. The literal was consolidated, the cache helper now owns the selection time, and the standalone complexity check passed.
- Final verification passed in 10m46s with 2,313 F-Droid and 2,435 Google Play tests and zero failures, errors, or skips. Both debug lints and the complexity check reported zero issues. All four debug and release APKs built, and the F-Droid release classpath contains no Android for Cars or Google Play Services dependencies. The command below uses the existing local Gradle init script that sets the Google Play test heap to 3 GiB. The ignored log is `build/seven-regression-fixes-full-rerun.log`. No Gradle invocation remains active. No Android device or Desktop Head Unit was available. The updated manual checklist covers actual head-unit and cluster behavior.

  ```powershell
  $checks = ':app:complexityCheck', ':app:testFdroidDebugUnitTest', ':app:testGplayDebugUnitTest', ':app:lintFdroidDebug', ':app:lintGplayDebug', ':app:assembleFdroidDebug', ':app:assembleGplayDebug', ':app:assembleFdroidRelease', ':app:assembleGplayRelease'
  .\gradlew.bat -I build/open-regression-fixes-test-heap.init.gradle @checks :app:dependencies --configuration fdroidReleaseRuntimeClasspath --max-workers=2 --continue --console=plain
  ```

## Review after the seven regression fixes (2026-11-24)

- Reviewed `652c79b6` against `main` at `2ba4dad3` for additional regressions in ordinary workflows. The pass covered Android Auto destination search and replacement, host ownership, guidance and arrival, map visibility, projected notifications, and shared phone location and simulation behavior. No additional meaningful regression was confirmed.
- Focused verification passed 41 F-Droid and 138 Google Play tests with zero failures, errors, or skips. `git diff --check main...HEAD` passed. The ignored log is `build/current-head-ordinary-regression-review.log`. No Gradle invocation remains active. No device or Desktop Head Unit was available, so head-unit rendering and wearable mirroring remain unverified.

  ```powershell
  $shared = '--tests', '*NavigationSimulationArrivalTest', '--tests', '*NavigationSimulatorTest', '--tests', '*NavigationServiceUiVisibilityTest', '--tests', '*NavigationServiceInitialRouteTest', '--tests', '*NavigationLifecycleRobolectricTest', '--tests', '*NavigationGpsStatusResourcesRobolectricTest'
  $car = '--tests', 'vibro.navigator.auto.*', '--tests', 'vibro.navigator.android.automotive.*', '--tests', '*AndroidAutoGuidanceNotificationsTest'
  .\gradlew.bat :app:testFdroidDebugUnitTest @shared :app:testGplayDebugUnitTest @shared @car --max-workers=2 --continue --console=plain
  ```

## DHU button test and street-overlay crash (2026-11-25)

- On `2de0dd5f`, the Google Play debug APK projected from the official Android 16 Google Play emulator to DHU 2.1. DHU Pause and Resume changed the shared navigation session, Stop returned both displays to idle, and the third button skipped an intermediate target in Straight Line mode.
- **Open crash for later investigation.** With **Surrounding streets** enabled, ViBRo exited during active DHU guidance in both Straight Line and BRouter Route mode. To exercise the affected workflow, start guidance on the phone, display it on the DHU, and tap the map to reveal the action strip or use the controls while the street overlay is updating. The exact action that cancels the read is unconfirmed.
- Both Android crash records show an uncaught `java.util.concurrent.CancellationException: Street loading cancelled` reaching `SurroundingStreetLoadExecutor.run` at line 62. Cancellation surfaced during segment-folder discovery in the first trace and during a provider tile read in the second. The full stacks and timestamps are in the local ignored log `build/dhu-button-crashes.log`. Reproduction on `main` remains pending.
- With the overlay disabled on the test emulator, the DHU **Blocked road** button showed “Blocked road added. Recalculating route.” BRouter 1.7.10 recalculated the Mountain View route from 3.2 km to 3.6 km and changed the first maneuver from a right turn to a left turn. Local screenshots are `build/dhu-routed-confirmed.png`, `build/dhu-routed-after-block.png`, and `build/dhu-routed-detour.png`.

## Phone landscape alignment on Android Auto (2026-11-25)

- The user requested phone-like Android Auto controls, guidance, GPS and compass layout within Google's quality guidelines. Keep the action strip and the structured maneuver and travel estimate cards on the Navigation template. The host determines their placement, visibility and styling. NF-2 requires turn directions and ETA to use template components. [Car app quality guidelines](https://developer.android.com/develop/adaptive-apps/quality-guidelines/car-app-quality#core) and [Navigation template](https://developer.android.com/design/ui/cars/guides/templates/navigation-template).
- Submit the phone button order to the host with icons only. Draw the existing GPS status at the upper left within the stable area, with the posted speed limit in the opposite corner. The GPS background is translucent in Fullscreen route mode and the map starts below the GPS line to keep the compass labels clear. Use the phone's Fullscreen route preference to select the fullscreen route or centered circular compass. The existing surrounding-street crash remains deferred.
- Verified the new Google Play debug APK on the Android 16 emulator and the 800×480 DHU during a BRouter route. The action strip showed Blocked road, a square Stop icon, and Pause. Both compass modes and the GPS placement were visible. The host showed the destination estimate and omitted the turn card in these captures. The app continues to submit structured turn information through the Navigation template. Local captures are `build/dhu-landscape-final-controls.png` and `build/dhu-landscape-compact.png`. The test emulator still has the surrounding-street overlay disabled.
- `.\gradlew.bat :app:assembleGplayDebug :app:lintGplayDebug :app:complexityCheck :app:testGplayDebugUnitTest --tests 'vibro.navigator.auto.*' --max-workers=2 --continue --console=plain` passed 87 tests with no failures, errors or skips. Lint reported no issues. The local ignored log is `build/dhu-landscape-final-checks.log`.

## DHU card investigation and map layout follow-up (2026-11-25)

- The user requested investigation of the action strip and turn-card visibility, identification of the X on the ETA card, a centered GPS line below the compass, and fullscreen map drawing behind host panels. The host's action strip appeared after map interaction and disappeared again after a 12-second idle interval on the same active route. This matches the documented host timeout. The X ended the route and returned the phone and DHU to idle. The adjacent estimate was for the final destination. [Action strip visibility](https://developer.android.com/design/ui/cars/guides/components/action-strip#visibility-of-action-strips).
- The GPS status now uses the bottom center of the map safe area. The compass remains anchored in that area while fullscreen geometry can draw beyond it under native cards. The DHU verified the new GPS placement at 800×480 during a BRouter route. The surrounding-street overlay was off on the emulator because of the recorded crash. Initial evidence is in `build/dhu-followup-zoom.png` and `build/dhu-followup-after-x.png`.
- With Android Auto `18.1.664448-release` and DHU 2.1, ViBRo's active route had no visible turn card at rest or during `AUTO_DRIVE` at 50 km/h. Google's Navigation Sample built with the same Car App Library showed routing arrows, including when its current step omitted the optional road field. Control captures are `build/dhu-sample-route-next.png` and `build/dhu-sample-no-road-routing.png`.
- Removing only the sample's maneuver icons reproduced the blank card with the ETA still present (`build/dhu-sample-no-icons-routing.png`). ViBRo now supplies explicit icons for known maneuvers in both the native routing card and cluster trip. The direction images use the phone's arrow symbols and are cached between updates. The maneuver types, exit numbers and localized cues still accompany the icons. [Google's Navigation Sample](https://github.com/android/car-samples/tree/main/car_app_library/navigation) and [Maneuver.Builder API](https://developer.android.com/reference/androidx/car/app/navigation/model/Maneuver.Builder).
- The rebuilt ViBRo APK showed the current and following turn cards on the same DHU, alongside the final destination estimate and centered GPS line. Tapping the map revealed all three actions. Local capture: `build/dhu-native-turns-controls.png`.
- A native bitmap rendering test verified that both route and street pixels extend past the host safe-area edge in Fullscreen route mode. Google Play debug assembly, lint, complexity checks, and all 88 Android Auto unit tests passed. The local ignored log is `build/dhu-fullscreen-final-checks.log`.

## DHU GPS text, simulation notice, and ETA review (2026-11-25)

- The user confirmed the speed-limit badge should stay on the right. The GPS status stays centered below the compass in the host's stable area and now uses 22 sp before display scaling, up from 16 sp. Brief changes to the host's visible area do not reposition this layout. [Stable map area](https://developer.android.com/reference/androidx/car/app/SurfaceCallback#onStableAreaChanged(android.graphics.Rect)).
- Android Auto now displays the phone's localized simulation notice as a native toast once per simulated session, when the car map first becomes visible. The service keeps independent car and phone notice flags. Turn captions, cluster metadata, and notifications use ordinary guidance text. DHU verified the toast during route loading and normal instructions during host `AUTO_DRIVE` at 50 km/h. Captures are `build/dhu-stop-restart-11.png` and `build/dhu-simulation-guidance.png`.
- Android Auto's dedicated estimate is for the final destination. On the 800×480 DHU, the routing card displayed the next stop's distance and duration and clipped its arrival clock at the end of the cue (`build/dhu-eta-while-stop-5.png`). The car guidance now puts the stop's arrival clock immediately after its label, followed by distance and duration. The focused template test verifies both payloads. [Destination travel estimate](https://developer.android.com/reference/androidx/car/app/navigation/model/NavigationTemplate.Builder#setDestinationTravelEstimate(androidx.car.app.navigation.model.TravelEstimate)).
- The rebuilt Google Play debug APK showed both arrival clocks before the intermediate stop (`build/dhu-stop-eta-first-settled.png`). The routing card showed Stop 1 at 16:02 and 1.9 km, with duration truncated. The separate final ETA card showed 4:06 PM and 3.2 km. The larger GPS line remained below the compass. Several emulator attempts logged `Foreground notification is missing, stopping navigation` during route startup. Retrying produced a working route. The cause and baseline reproduction remain open. Local device log: `build/dhu-gps-simulation-device.log`.
- `.\gradlew.bat :app:assembleGplayDebug :app:lintGplayDebug :app:complexityCheck :app:testGplayDebugUnitTest --tests 'vibro.navigator.auto.*' --tests '*NavigationServiceSimulationTest' :app:testFdroidDebugUnitTest --tests '*NavigationServiceSimulationTest' --max-workers=2 --continue --console=plain` passed 90 Google Play and 3 F-Droid tests with no failures or skips. Lint reported no issues. Local log: `build/dhu-gps-simulation-final-checks.log`.

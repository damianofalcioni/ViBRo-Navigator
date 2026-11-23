# Branch handoff: Android Auto navigation compliance

## Current state and first actions

- The coverage audit at the end of this plan supersedes earlier implementation-complete statements. It records three open gaps and pre-sync verification. The most recent section records the sync with `main`.
- The Google Play appeal follow-up shared November 17, 2026 scopes the vibration restriction to the Android Auto environment. The user chose silent phone notifications while projected so compatible smartbands can still mirror them. `gplay/android/automotive/AndroidAutoConnectionMonitor` observes `CarConnection` from application startup and uses silence until the first connection result. `AndroidNavigationNotificationChannels.guidanceAlert` selects the silent channel for all shared transient navigation alerts and About tests during projection. The three Google Play regression tests cover the initial state, connect and disconnect during the same service session, alerts available for mirroring, and legacy vibration behavior.
- Verification passed with `.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest lintFdroidDebug lintGplayDebug assembleFdroidRelease assembleGplayRelease :app:complexityCheck --console=plain`. All 2,161 F-Droid and 2,259 Google Play tests passed with zero failures, errors or skips. PMD found zero violations. Both release APKs built. Both lints reported only the existing locale and Gradle version warnings. The F-Droid lint dependency model contains no Android for Cars or Google Play Services libraries. No Gradle commands remain running.
- No phone was listed by `adb devices -l`. Verify connect and disconnect mid-route on a head unit and verify actual smartband mirroring before resubmission. The companion app may filter silent notifications or apply its own haptics. Google requested deactivating bundle `1000000` across production and active test tracks and submitting a higher code. Release preparation and Play Console actions remain pending.
- Quality-audit follow-up (2026-11-06) is complete and ready for local commit: VI-1 idle phone prompts require safe parking in all 18 languages, DR-3 destination attempts have a nine-second deadline with explicit retry, and destination templates retain Car API 1 compatibility. Stale results and host actions cannot start/cancel/replace a newer request. Specification and the manual review checklist are aligned. See the final follow-up evidence below. No Gradle commands remain running.
- Full Google Play suite: 2,256 tests passed. Final-source focused Auto/automotive suite: 70 passed. Full F-Droid suite: 2,161 passed. Both flavor lints, PMD and all four APK builds passed. All test runs had zero failures/errors/skips. Only the two previously documented lint warnings remain. No device is connected, and the current SDK has no DHU installation; manual car verification and live Play submission remain outstanding and unauthorized.
- Workspace: `D:\PROJECTS\vibe-navigator`.
- Working branch: `codex/android-auto-compliance`, created from clean `main` at `1a502328` (`Document agent branch and commit workflow`). Continue this branch; do not recreate the work on main.
- Both the earlier Android Auto compliance milestone (804f9027; handoff workflow 5e5b951c) and the shared simulation milestone are complete and verified. Simulation is committed locally with this updated plan; use git log -1 for its identifier. No commands are running. Keep this branch and its existing work.
- Simulation is now authorized and in progress (latest user request). Implement it in shared code for Google Play and F-Droid, with Advanced settings row ordered label/configuration/switch/info, configurable speed using current display units, optional starting coordinates and the existing POI map picker beside them. Blank start uses device location; explicit coordinates must work without a physical fix. Android Auto onAutoDriveEnabled must activate the same simulator. Preserve pause/resume, route changes and arrival rules; clearly label simulation. Implementation and final verification are complete; see the simulation map and validation notes below.
- Nothing has been pushed, merged into `main`, published, submitted to Play, or installed on a phone. Both release APKs built locally are unsigned. App version remains `1.0.0` / `1000000`; this is not a new upload-ready release.
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

- The three audit fixes, manual Android Auto/head-unit checks and Play release work remain open. Keep this handoff plan on the active feature branch until it is ready to merge into `main`.

# Branch handoff: Android Auto navigation compliance

## Current state and first actions

- Workspace: `D:\PROJECTS\vibe-navigator`.
- Working branch: `codex/android-auto-compliance`, created from clean `main` at `1a502328` (`Document agent branch and commit workflow`). Continue this branch; do not recreate the work on main.
- The non-simulation implementation and validation are complete. Full verification, lifecycle refinement checks, and final lint-warning cleanup verification all passed. No commands are left running. This implementation and handoff are committed together locally on this branch; use git log -1 for the commit identifier.
- Simulation is explicitly deferred by the user. No simulation implementation, simulation setting, synthetic-location provider, or functional `onAutoDriveEnabled` override has been added. Do not implement it until the user settles the simple approach they want.
- Nothing has been pushed, merged, published, submitted to Play, or installed on a phone. Both release APKs built locally are unsigned. App version remains `1.0.0` / `1000000`; this is not a new upload-ready release.
- This plan is branch-only. Keep it while work and handoffs continue; before an approved merge, preserve any lasting decisions in maintained documentation, remove this file on the working branch, and commit its removal. The final merge diff and target tree must exclude `BRANCH_PLAN.md`, as now required by AGENT.md.
- Read `AGENT.md`, this plan and specification section 4.5.8, then inspect `git status` and `git log -3`. The completed milestone is recorded below; do not repeat its checks without a relevant change or new failure. Never push this working branch or merge without the required user authorization.

## Original problem and review context

Google Play flagged ViBRo Navigator (`vibro.navigator`), version code `1000000`, as **Approved with Issues / Category not permitted** for Android Auto. The appeal was rejected with essentially the same category explanation, specifically identifying the **Closed Testing** track and requiring a higher version code replacing the affected bundle.

Navigation is a supported Android Auto category. The app already used the Android for Cars App Library, declared `androidx.car.app.category.NAVIGATION`, requested `NAVIGATION_TEMPLATES` and `ACCESS_SURFACE`, and declared the template capability. The notice did not identify the exact technical reason for the category finding; do not claim that a particular code issue has been confirmed as Google's root cause.

Repository inspection nevertheless found concrete implementation gaps: text/buttons painted onto the navigation surface, missing NavigationManager ownership and cluster metadata, incoming car intents only logged, missing car-specific ongoing navigation notifications, and missing review simulation. The user supplied a navigation-requirements checklist and authorized implementation after asking to reread AGENT.md.

The user then explicitly said: **"leave simulation as last step, I have to think how to do it in a simple way"**. Later they requested that this branch plan provide the full context for the next agent, with AGENT.md updated if needed. That handoff requirement is now part of AGENT.md's initial workflow.

## Agreed scope and important decisions

- Keep Android Auto support and repair its navigation integration. Removing Auto or merely changing a category declaration is not the chosen solution.
- Keep phone navigation, routing confidence/arrival rules, offline BRouter behavior and F-Droid compatibility intact. Use the existing service/session and binder actions rather than introducing separate car navigation logic.
- Directions, following maneuvers, ETA and navigation controls belong in host templates. The custom surface contains route/map content and the permitted speed-limit overlay. Custom setting buttons, diagnostic/detail panels, scrolling instruction columns and phone layouts are now phone-only.
- Retain Car API level 1 compatibility, with `Action.PAN` added only on hosts supporting API 2+. Deprecated header setters are intentionally and narrowly retained for this compatibility; replacing them with newer APIs requires reconsidering host support.
- Use structured app-owned maneuver/travel data, never parse localized display lines to obtain commands, distance, exit number or ETA. The following phone line is relative to the first maneuver; cluster step progress is absolute from the current position.
- Update both `androidx.car.app:app` and `app-projected` from `1.7.0` to `1.8.0-rc01`. Google's checked release notes still listed 1.7.0 as stable but explicitly recommended rc01 for its security fix. A dependency bump alone does not establish policy compliance.
- No new upload/version preparation until remaining simulation and head-unit verification are complete. Future uploads must use the repository's semantic-version/code formula and `prepareRelease`, not an independent versionCode edit.

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

- `SPECIFICATION.md` section 4.5.8 now describes host guidance/map-only rendering, structured metadata, ownership, intents and deferred simulation; related phone-only text/diagnostic wording was aligned.
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
- Automated tests do not prove real head-unit behavior or Google Play acceptance. Manual DHU checks and simulation are still required.

## Remaining work and next-agent instructions

1. Verify the current branch, git status and recent commit when resuming. The non-simulation milestone is complete and verified; no build is left running. Continue from the remaining work below, preserving a clean local history and the no-push/no-merge/no-publish constraints.
2. **Wait for the user's simulation design decision.** They want a simple approach. No specific simulation architecture has been approved. The likely host integration point is ViBRoCarHostNavigation's NavigationManagerCallback, which currently implements stop handling but inherits the no-op auto-drive callback. A settings switch alone is insufficient: the required onAutoDriveEnabled path must actually simulate navigation to the selected destination.
3. After an agreed simulation implementation, test its start/stop, pause/resume, route change, session destruction, real-location isolation and AUTO_DRIVE activation. Keep live tracking/arrival-confidence behavior intact and F-Droid free of car SDK dependencies.
4. Perform the manual/DHU checks in `gradle/ANDROID_AUTO_REVIEW.md`, including cluster output when covered, another navigation app taking ownership, notifications, safe-area/day-night rendering, coordinate/address intents and disconnected/reconnected lifecycle. Correct findings rather than claiming acceptance from unit tests.
5. Only after remaining requirements are verified, prepare a new semantic version/code with the existing release-prep workflow and coordinate Play submission. The flagged `1000000` bundle must be replaced in Closed Testing and any other affected tracks. No live submission is authorized by this implementation work.

Do not redo the completed template/metadata/intent integration merely because a new chat lacks the earlier conversation. Use this plan, the code, the recorded checks and the latest user instructions as the resume point.

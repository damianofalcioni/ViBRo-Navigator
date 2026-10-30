# Location-reliability progress

Last updated: 29 October 2026. The latest independent review prevented expired/future speed readings from permanently inflating trip maximum speed. Final verification: 4,166 passing tests, zero PMD violations, both debug lints and all four debug/release APK tasks passed. Current source label and artifact hashes are recorded in the latest verification entry below. Physical validation remains pending.

Baseline: 746aa24dd55018ca44bd82f5847fc14d21574e85. The user subsequently authorized one sustained implementation run covering all software stages. That later instruction supersedes the original one-packet-at-a-time restriction for this chat. All changes remain uncommitted. Earlier log-analysis scripts and results are local ignored artifacts.

The baseline and original integrated checks passed. Independent review corrections now also pass both-flavor tests, lint, maintainability and debug/release builds. The work includes relative-coordinate session replays, raw-evidence motion assessment, clocked motion consumers, corroborated-motion filter lag limits, sparse ETA history, uncertainty-aware acquisition, independently confirmed and ordered arrival, network-quality backoff, timing diagnostics, and shared battery plausibility checks. The earlier Huawei PRA-LX1 installation predates these corrections; no phone was attached during the correction run. Physical accuracy, notification receipt, wake latency and power measurements remain pending.
States: PLANNED, IN_PROGRESS, PARTIAL, CODE_COMPLETE, VERIFIED, NEEDS_DEVICE. CODE_COMPLETE means code and its stated local checks are finished; it does not imply physical-device validation. A recorded hardware-only gap may coexist with finished code. Never mark skipped/failed checks as passed.

| Run | Objective | State | Handoff |
|---|---|---|---|
| 01 | Minimal replay foundation | VERIFIED | Three local-offset excerpts, shared loader and synthetic motion/cadence regressions |
| 02 | Event and timing diagnostics | VERIFIED | Callback trace, elapsed/uptime, guidance/post/recovery records; explicit build label |
| 03 | Motion assessment core | VERIFIED | Independently spaced raw motion, independent stop timing, conflicting-speed rejection and 10-second read-time freshness |
| 04 | Motion-state consumers | VERIFIED | Clocked reads, expired cues and supported trip-prefix retention across UNKNOWN/reacquisition, with separate hardware-sleep credit |
| 05 | Kalman lag and uncertainty | VERIFIED | Corroborated walking lag bound with or without trustworthy speed-accuracy metadata, including precise slow speeds below the native-only threshold; conservative output uncertainty retained |
| 06 | Cadence-aware ETA history | VERIFIED | Confirmed-motion ETA history uses measurement time; separate receipt-time direction history retained |
| 07 | Uncertainty-aware acquisition | VERIFIED | Age/quality/travel caps and current trustworthy native-speed protection; independent recovery, ramp and sparse-cadence coverage |
| 08 | Confirmed arrival | VERIFIED | Arrival barriers across multi-hint updates, route application and co-located required stops |
| 09 | Independent network backoff | VERIFIED | Coarse-network probes and silence recovery, with comparative protection for a materially better network source |
| 10 | Provider selection verification | VERIFIED | Existing selection/deduplication/correction policy retained; full regression coverage passed |
| 11 | Notification/recovery diagnosis | NEEDS_DEVICE | Local scheduler/trace changes complete; OS/watch presentation and wake timing unmeasured |
| 12 | Battery telemetry validation | VERIFIED | Shared timed/percentage discontinuity checks, cached observation times and valid percentage plateaus |
| 13 | Cross-feature integration and automated checks | VERIFIED | Latest trip-speed freshness audit: 4,166 passing tests, PMD, both debug lints and four APK tasks |
| 14 | Physical-device comparison | NEEDS_DEVICE | Earlier APK installed on Huawei; review-corrected APK installation and route/watch/power trials pending |

## Handoff entry format

Append one entry per attempt; preserve previous entries. Keep it concise enough for the next agent to read.

### Run NN — attempt N — date

- State and scope completed:
- Start/end HEAD and uncommitted changes owned by this run:
- Relevant inherited changes:
- Behavior and interfaces introduced:
- Decisions and supporting evidence:
- Tests/checks actually run, exact command and outcome:
- Known failures or unfinished work:
- Hardware checks still pending:
- Specification/documentation changes:
- Next agent's exact starting point and prerequisite status:

## Execution gates

- The next run is selected by the user, not automatically dispatched.
- Prefer the next numeric run after review of the prior handoff. If the current run is PARTIAL, resume it before work that depends on it.
- Runs 04–07 require usable motion/filter interfaces from their predecessors; run 13 reconciles the full code set.
- Missing physical-device evidence in run 11 does not prevent independent battery-code work in run 12 or local integration in run 13. Carry the unresolved device work into run 14.
- If new evidence changes the diagnosis, record it and adjust the selected run; do not implement speculative fixes just to mark the roadmap complete.

## Integrated implementation — 27 October 2026

The user explicitly authorized completing all software work together. HEAD remains `746aa24dd55018ca44bd82f5847fc14d21574e85`; implementation, tests, specification/agent guidance and this planning package are uncommitted. No unrelated pre-existing source edits were present. No branch, commit, installation or publication was performed.

### Behavior and interfaces

- `NavigationLocationMotionModel.recordLocation(raw, filtered)` assesses raw evidence separately from display smoothing. A single zero-speed observation remains UNKNOWN. Subsequent ambiguous evidence clears stop confidence; coordinate evidence is provider-local. Compatibility boolean accessors remain for existing callers.
- Motion corroboration uses at least three native-speed observations without speed-accuracy metadata, a 6-second span, bounded 150-second history/65-second gaps, plausible displacement rate and 80% directness. Accuracy-aware thresholds prevent a single coarse correction from becoming travel. Linear sensor drift can still resemble motion; this is not surveyed truth. The recorded fixtures exercise component policies, rather than reconstruct every original Android session event.
- UNKNOWN suppresses confident stationary advice and contributes neither inferred moving/stationary totals nor a distance bridge. Native moving evidence is not converted into stationary accounting by suppressed display speed. Intentional hardware sleep receives its actual boundary credit. Raw stationary-sleep verification may start from UNKNOWN; its separate fixed-origin, quality, trigger and alarm prerequisites still govern suspension and short fallback checks.
- The filter bounds added displacement after corroborated walking to the smaller of raw uncertainty and max(3 m, two seconds of native travel), while output uncertainty includes the remaining displacement. Tests cover 12/15/20/60-second cadence boundaries, stops, turns, jumps and delayed corrections.
- ETA retains up to 65 seconds of provider-local progress history, independently of the 10-second direction/deviation history. Sparse estimates do not invent acceleration. Dynamic acquisition accounts for uncertainty and observation age before approach margins become too small; fixed 1-second behavior remains available.
- Arrival requires current and trusted filtered accuracy at most 25 m, bounded 10–20 m reach, and same-provider chronological observations spanning at least 3 seconds with no gap over 10 seconds. Synthetic hints cannot announce arrival or skip an unconfirmed target. Required stops precede final arrival, including co-located targets. Beeline completion shares confirmation with stop consumption; waiting inside reach must not archive an unwalked spur in GPX.
- Network sampling backs off after three independently spaced coarse observations only with live usable GNSS (or the pre-existing stable-GNSS rule). Accurate stationary repeats remain useful; improved network quality and GNSS silence restore cadence. Selection, retired callbacks, chronological gates and position-only delayed correction remain governed by existing policies.
- Callback-scoped trace IDs connect synchronous receipt, selection, motion, guidance and post completion. Elapsed/uptime screen transitions and recovery lateness use existing events. `deliveryReceipt=unobserved` explicitly separates posting from actual phone/watch receipt. No new logging timer, exact-alarm permission or permanent wake lock was added.
- Battery diagnostics preserve vendor units and label unsupported/implausible/increasing/charging counters. Consumption stays unavailable when its evidence is invalid. A percentage plateau is permitted; an increase invalidates the session discharge-drop estimate.

### Integration review

Earlier full/focused attempts exposed obsolete immediate-arrival fixtures and real interactions with beeline target order, untravelled GPX spur export, fallback sleep verification and display-speed-based accounting. These were corrected and covered by focused regressions before the final both-flavor run. Original source-selection safeguards were retained: no independent defect justifying averaging or dispatch-gate relaxation was demonstrated.

Earlier failed logs in ignored `.codex-temp` are historical attempts, superseded by the successful final command below. VERIFIED in the table means the stated local software checks passed; it does not imply physical-device validation.

### Final verification and artifacts

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=746aa24d-location-reliability-20261027 --continue --console=plain --max-workers=2
```

Outcome: **BUILD SUCCESSFUL**, exit 0, 14m 56s; 196 tasks (54 executed, 142 up to date). Exact console output remains in ignored `.codex-temp/location-verification.txt`.

- `Fdroid`: 1,899 tests, zero failures/errors/skips.
- `Gplay`: 1,971 tests, zero failures/errors/skips.
- `fdroid` debug lint: passed; 2 warnings (AppBundleLocaleChanges, AndroidGradlePluginVersion).
- `gplay` debug lint: passed; 2 warnings (AppBundleLocaleChanges, AndroidGradlePluginVersion).
- `complexityCheck`: passed with zero PMD violations; no suppressions added.
- F-Droid and GPlay debug APKs: built successfully.
- F-Droid and GPlay release APKs: built successfully, unsigned under the local signing configuration.
- All four generated BuildConfig files contain `SOURCE_REVISION=746aa24d-location-reliability-20261027`.
- `git diff --check`: passed. Replay formats/local offsets checked (23 fused-departure, 35 GNSS-departure, 25 poor-approach observations).
- `adb devices` at the initial verification: no connected device. Subsequent installation/startup is recorded below; phone/Auto visual validation, OS idle timing, watch receipt and measured energy comparison remain pending.

The two lint warnings concern the existing locale/bundle check and available Gradle version; no lint errors or new location-code warnings were reported. Gradle also reports existing deprecated-feature usage.

Usable debug artifacts and SHA-256:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `03B151898E91EEEAF6BD7AF38BD3DBD314FF3D1241090AB4D816889E15B87D2E` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `D662AF3FED9CD5035F8EE309E837DB07DEAD4EBEA02EF883E1708B156D37C857` |

### Remaining physical work

Follow DEVICE_VALIDATION.md using the labelled APK and a fresh log. Independently observe actual turn crossings and phone/watch alerts; compare requested/actual wake timing and alternating same-route provider runs with controlled settings and validated power data. Neither original log establishes measured position error, a definitive OS/watch-delay cause or a provider energy advantage. Do not change provider defaults or claim savings until field evidence supports them.

The next chat should read this status and the device protocol, retain the uncommitted changes, and start with the physical validation stage. Do not restart runs 01–13 after local verification succeeds.

### Connected-phone installation — 27 October 2026

The user connected the phone and authorized proceeding with the previously discussed installation. The Huawei PRA-LX1 runs Android API 26 and already had `vibro.navigator.debug` installed.

- Fresh rebuild: `.\gradlew.bat assembleGplayDebug -PsourceRevision=746aa24d-location-reliability-20261027 --rerun-tasks --console=plain --max-workers=2`; **BUILD SUCCESSFUL** in 1m 58s, all 36 tasks executed. Output: ignored `.codex-temp/location-phone-build.txt`.
- Updated the existing GPlay Debug package in place using `adb install -r`; result **Success**, preserving its app data. Package is `vibro.navigator.debug`, launcher **ViBRo Navigator Debug**, version 1.0.0 / 1000000. Release package remains separately installed.
- Launched `vibro.navigator.main.MainActivity`: `am start -W` returned **Status: ok**, launch time 1,435 ms; the process remained running. The app-PID-filtered AndroidRuntime error buffer contained no crash records at the smoke check. This is a startup check, not visual or navigation validation.
- The fresh APK SHA-256 is `03B151898E91EEEAF6BD7AF38BD3DBD314FF3D1241090AB4D816889E15B87D2E`; the current artifact table above reflects it. The earlier full-verification GPlay APK hash was `5FA9B0E7296032A464B27670DAD54A2960C7CE25D50E0418EECF09C514DADB26`.
- Build source label verified as `746aa24d-location-reliability-20261027`. No app source changes were made during installation. Corrected the device protocol to distinguish `.debug` test packages from release packages.
- The next step is the screen-off same-route experiment in DEVICE_VALIDATION.md, including actual phone/watch delivery and controlled power comparison. No route was started automatically.

### Independent review corrections — 27 October 2026

- Authorization: after the independent review identified four reliability gaps, the user asked to fix them. HEAD remains `746aa24dd55018ca44bd82f5847fc14d21574e85`; all original and corrective work remains uncommitted.
- Arrival: `TurnHintAdvancePolicy` stops its multi-hint consumption loop at an unconfirmed synthetic arrival. `TurnEventPlanner` suppresses imminent signals at that boundary, preserving later alert flags. Regressions cover both notification modes, passed/imminent later maneuvers and resumption after confirmed stop consumption.
- Additional arrival audit: route application must stop at an unconfirmed arrival even if its projection is behind the latest fix. Consuming one arrival must preserve a second co-located required stop. Both bypasses were reproduced with a standalone local probe, repaired in `NavigationGuidanceHintSequence`, and added to `TurnArrivalBoundaryTest`.
- Stop confidence: raw stop evidence spans at least 1 second with bounded 10-second gaps, usable raw accuracy and no conflicting native speed. Jitter can suppress displayed speed while motion remains UNKNOWN. Sparse/clustered callbacks cannot manufacture a stop.
- Freshness: timed motion reads become UNKNOWN after 10 seconds or for future-dated evidence. Orientation, full/heading-only display, route-result handoffs, departure verification and accepted-fix accounting use current monotonic time. Compass callbacks cannot refresh location evidence; expired stationary notices/cues are suppressed without adding timers. Latest-observation compatibility accessors remain for synchronous policy/tests.
- Battery: snapshots carry observation time through the existing cache. Diagnostics and trip totals share conservative time/percentage discontinuity checks, retain raw units, allow ordinary percentage plateaus, and leave trip mAh unavailable after invalid data. Fresh equal readings advance the counter time anchor. These checks do not establish vendor-unit correctness or app-only energy.
- Replays: recorded excerpts now also pass through `NavigationSession` selection, filtering, motion, Straight Line guidance and duplicate rejection using recorded receipt times/requested intervals. Their target is explicitly synthetic; this is stronger integration coverage, not a reconstruction of the original route, physical movement or watch delivery.
- Focused regression command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationMotion*Test' --tests '*NavigationStationarityTrackerTest' --tests '*TurnArrivalBoundaryTest' --tests '*NavigationTurnStateTest' --tests '*NavigationTripBattery*Test' --tests '*NavigationTripStatsTrackerTest' --tests '*NavigationChargeCounterValidationTest' --tests '*NavigationOrientationControllerTest' --tests '*RecordedLocationReliabilityTest' --console=plain --max-workers=2`; BUILD SUCCESSFUL in 1m 8s. Output: `.codex-temp/location-fix-focused.txt`.
- Follow-up recorded-session/orientation tests passed. That combined attempt initially failed PMD on two complexity limits; the planner guard was moved to its advancement boundary and an unused raw-bearing facade/helper was removed. No suppressions were added. Historical attempt: `.codex-temp/location-fix-integration.txt`.
- First full verification attempt: 1,916 F-Droid / 1,988 GPlay tests, with the same three failures in each flavor. The failing heading/reversal fixtures assumed that one stopped observation immediately confirmed stationarity; they now supply a second independent stopped observation and retain their original heading/reversal assertions. PMD passed after the complexity cleanup. Output: `.codex-temp/location-fix-verification.txt`. New arrival boundary cases and these fixture updates require the final rerun below; this attempt is not final verification.
- `adb devices` during the correction run lists no attached device. The previously installed phone APK predates these corrections. Field checks in DEVICE_VALIDATION.md remain pending.

#### Final correction verification

The corrected heading/reversal fixtures, both additional arrival barriers, original review probes and recorded session replays passed the final focused run with PMD: `.codex-temp/location-fix-final-focused.txt`, BUILD SUCCESSFUL in 1m 1s. The standalone probe results in `.codex-temp/location-fix-probes.txt` now show UNKNOWN for the three ambiguous stop cases, unavailable mAh for the counter reset, and zero later-turn events for the route-application/co-located-stop bypasses.

Final full command:

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=746aa24d-location-reliability-reviewfix-20261027 --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 11m 51s; 196 tasks (40 executed, 156 up to date). Console output: ignored `.codex-temp/location-fix-final.txt`.
- F-Droid: **1,918 tests**, zero failures/errors/skips. GPlay: **1,990 tests**, zero failures/errors/skips.
- PMD: zero violations; no suppressions added. Both debug lint tasks passed with the same two existing warnings (AppBundleLocaleChanges and AndroidGradlePluginVersion). No new lint warnings were introduced. Gradle's existing deprecation notice remains.
- Both debug APK tasks and both unsigned release APK tasks passed. All four generated BuildConfig files contain `SOURCE_REVISION=746aa24d-location-reliability-reviewfix-20261027`. Gradle reused unchanged release APK outputs after rebuilding their inputs; verification was against the final working-tree sources.
- `git diff --check` passed. HEAD is unchanged; all implementation, tests and documentation remain uncommitted.

These current debug artifacts supersede the earlier build hashes recorded above:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `8B3D6CF963BCB1BA8E59D20E1139FE8079DEFE84A9B2248D1478E7DF0D769B31` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `D76D074DDC5E2EFDB3D3B3BFF72137E69704CA624A6A2D2BA29C5DA1836F177A` |

Next: install the review-corrected build when a phone is connected and installation is authorized, then follow DEVICE_VALIDATION.md. Do not infer physical guidance accuracy, phone/watch receipt, OEM wake latency or provider energy savings from these local checks.

### Follow-up review fixes — 28 October 2026

- Authorization: the user asked to fix all three findings from the subsequent review, including the acquisition-quality recovery policy after clarification that this concerns moving navigation as well as stationary departure. HEAD remains `746aa24dd55018ca44bd82f5847fc14d21574e85`; all inherited and new work remains uncommitted.
- Network usefulness: acquisition compares the most recent valid network uncertainty with current raw GNSS using the existing selection improvement margin. A materially better network source retains ordinary cadence even if it is coarser than 50 m or GNSS stability survives its first degraded fix. Valid later observations can restore backoff. Existing selection, chronology, GNSS-silence recovery and fused exclusivity are unchanged.
- Trip expiry: `NavigationTripMotionStats` owns motion totals and the underlying observation deadline, shared with `NavTripStatus`. Live totals stop at that deadline; a later fix cannot connect distance across expiry or a provider offset. Observation age reduces the remaining accounting window. Explicit hardware-sleep credit remains separate and survives rejected wake fixes/reacquisition. The motion tracker and accounting use the same 10-second lifetime constant.
- Quality recovery: `NavigationAcquisitionRecovery` is shared by routed and Straight Line guidance. Tightening is immediate; lengthening requires three fresh, provider-local chronological observations separated by at least 2 seconds, then advances one bucket only after that bucket has elapsed. Clustered/duplicate fixes cannot advance it; provider changes and unexpected gaps restart verification. Normal sparse callback gaps remain eligible. Route/session and Straight Line target resets clear the appropriate state, and existing tighter maneuver/departure/fixed limits still apply.
- Regression coverage: better-but-coarse network, first-fix GNSS degradation, silent live/closed accounting expiry, receipt-versus-observation age, stale/future evidence, provider offsets, sleep boundaries, alternating quality, independent recovery, ramp timing, handover/reset and both routed/Straight Line integration.
- First focused attempt failed the maintainability gate on two complexity limits before completing the test task. Extracted the motion-accounting helper and simplified recovery flow; no suppressions were added. Historical output: `.codex-temp/location-review-20261028-focused.txt`.
- Focused final command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*LegacyNetworkBackoffTest' --tests '*NavigationAcquisition*Test' --tests '*NavigationUpdateSchedulerTest' --tests '*NavigationTrip*Test' --tests '*NavigationStationarySleepTripStatsTest' --tests '*StraightLineQualityRecoveryTest' complexityCheck --continue --console=plain --max-workers=2`; **BUILD SUCCESSFUL** in 46 seconds. Output: `.codex-temp/location-review-20261028-focused-3.txt`. The previous focused rerun also passed.
- Specification and agent guidance now document comparative network protection, bounded trip accounting and the separate moving-quality recovery policy. No new permissions, strings, runtime dependencies, wake-ups or permanent wake locks were introduced.
- Full both-flavor tests, lint, maintainability and debug/release builds are running with `-PsourceRevision=746aa24d-location-reliability-20261028`; final results follow below. No installation or field trial has been performed during this follow-up.
- The initial full attempt exposed the same real regression in each flavor: `NavigationSessionTest.onRawLocationChanged_keepsDynamicBucketAfterExpectedLongInterval`. A newly tightened cap was incorrectly used to judge the preceding callback gap. The existing assertion remains unchanged; recovery now checks the previous cap and continues updating its anchor on unrestricted long legs. Added independent normal-gap/long-leg regressions. The initial full run is historical evidence, not final verification: `.codex-temp/location-review-20261028-full.txt`.
- Final review also added a moving-to-stopped transition after silence: the supported prefix keeps its earlier classification rather than being relabelled by a later observation. The final rerun must include this accounting change and both sparse-cadence regressions.

#### Final follow-up verification — 28 October 2026

The final focused run included the unchanged `NavigationSessionTest` in addition to the prior focused filters, plus the new expiry-transition and normal sparse-cadence cases. **BUILD SUCCESSFUL** in 45 seconds, with zero PMD violations. Exact command/output: `.codex-temp/location-review-20261028-focused-final.txt`.

Final integrated command:

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=746aa24d-location-reliability-20261028 --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 10m 3s; 196 tasks (35 executed, 161 up to date). Exact console output: `.codex-temp/location-review-20261028-final.txt`.
- F-Droid: **1,933 tests**, zero failures/errors/skips. GPlay: **2,005 tests**, zero failures/errors/skips. Total: **3,938**.
- PMD: zero violations and no new suppressions. Both debug lint tasks passed, retaining only the existing `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings per flavor. Existing Gradle deprecation notices remain.
- Both debug APKs and both unsigned release APK tasks passed. Reused outputs were checked by Gradle against the final sources. All four generated BuildConfig variants contain `SOURCE_REVISION=746aa24d-location-reliability-20261028`.
- `git diff --check`: exit 0. HEAD remains `746aa24dd55018ca44bd82f5847fc14d21574e85`; all source, tests, documentation and the plan package remain uncommitted. No branch, commit, installation or publication was performed.
- Original probes now show 3-second network sampling for the better-but-coarse source; 10 seconds of live/closed moving credit with zero distance across the expired 60-second interval; and continued 3-second verification through alternating poor/good quality. Formal tests additionally prove stable recovery, bucket timing, provider resets, preserved intentional sleep credit and supported-prefix classification.
- `adb devices` returned no attached devices during this follow-up. The earlier Huawei installation predates these changes. Phone/Auto visual checks, physical guidance accuracy, screen-off wake timing, actual phone/watch delivery and power comparisons remain pending under DEVICE_VALIDATION.md.

Current debug artifacts supersede all earlier hashes in this document:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `17D83F23347D9611A4FDC6EBDEC1F56B4C80762DBB031D8F3E920D157068E8D2` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `D8BA6B1FB5DA328ADF7F8E23A7DF7B7D7B91F37AB135DBC45705A8DC0128C941` |

Next: when a phone is connected and installation is authorized, use this labelled build for the device protocol. Do not restart completed software work or claim field accuracy, delivery latency or energy savings from these automated checks.

### Further review corrections — 28 October 2026

- Authorization: the user asked to fix the three findings from the current independent review. Current HEAD is `0c46363134228bbbf35b3327612c4ca776d3c05b` (the prior `746aa24d` baseline plus the F-Droid distribution commit). The inherited location-reliability work and these corrections remain uncommitted; no branch, commit, installation or publication was performed.
- Approach protection: `NavigationAcquisitionQuality` uses the maximum of supported progress speed, the 1.5 m/s floor and trustworthy current native speed. Native speed must be finite and exclude zero within twice its finite, nonnegative speed uncertainty. This tightens acquisition without manufacturing ETA/turn events. The routed acceleration reproduction now requests 3 seconds instead of 12 seconds at 100 m remaining and 10 m/s measured speed.
- Trip accounting: `NavigationTripMotionStats` settles the prior supported interval before replacing its evidence, including UNKNOWN, stale fixes and reacquisition. Its previous classification and original expiry bound the credited prefix; new segments and unknown/expired/provider-offset intervals still cannot connect travelled distance. Explicit sleep credit and existing timely known-state transitions remain intact. The original UNKNOWN/reacquisition probes retain 10 seconds instead of falling to zero.
- Movement evidence: require at least three observations separated by 2 seconds, in addition to the existing 6-second span, provider, quality, directness and rate gates. Keep all valid raw samples for path coherence so ordinary one-second callbacks remain continuously MOVING after independent confirmation. The 1 s / 7 s / 7.001 s correction cluster remains UNKNOWN.
- Regressions: added eight tests across acquisition quality, routed turn scheduling, motion confidence and trip expiry. Cover invalid/unavailable speed uncertainty, a faster progress estimate, the fixed 1-second setting, delayed third observations, continuous one-second walking, UNKNOWN-to-known gaps, stale observations and reacquisition. Updated two earlier accounting tests that intentionally expected the now-corrected loss of supported time; their distance/unknown-gap safeguards remain.
- Before-fix command: `testFdroidDebugUnitTest --tests '*NavigationAcquisitionQualityTest' --tests '*NavigationUpdateSchedulerTest' --tests '*NavigationMotionConfidenceTest' --tests '*NavigationTripMotionExpiryTest' --console=plain --max-workers=2`; 35 tests, 6 expected failures reproducing all three findings. Output: `.codex-temp/review-current/regressions-before-fix.log`.
- Focused verification: `testFdroidDebugUnitTest --tests '*NavigationAcquisition*Test' --tests '*NavigationUpdateSchedulerTest' --tests '*NavigationMotion*Test' --tests '*RecordedLocation*Test' --tests '*NavigationTrip*Test' --tests '*NavigationStationarySleepTripStatsTest' --tests '*StraightLineQualityRecoveryTest' complexityCheck --continue --console=plain --max-workers=2`; **BUILD SUCCESSFUL**, exit 0, 1m 59s. No PMD suppressions. Output: `.codex-temp/review-current/fixes-focused.log`. Original standalone probes also pass their corrected expectations: `.codex-temp/review-current/probe-results-after-fix.txt`.
- Updated `SPECIFICATION.md` and the existing reliability guardrails in `AGENT.md`. No new strings, permissions, dependencies, wake-ups or permanent wake locks.
- Full verification is running with `-PsourceRevision=0c463631-location-reliability-20261028-fix3`; its result and current artifacts will follow below. `adb devices` returned no attached devices; screen-off guidance, phone/watch receipt, wake latency, actual positioning accuracy and power comparisons remain pending.

#### Final further-review verification — 28 October 2026

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261028-fix3 --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 22m 35s; 196 tasks (54 executed, 142 up to date). Exact console output: `.codex-temp/review-current/fixes-full.log`.
- F-Droid: **1,941 tests**; GPlay: **2,013 tests**. Total: **3,954**, with zero failures, errors or skips in either flavor.
- Maintainability passed with zero PMD violations and no added suppressions. Both debug lint tasks passed with the same existing `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings per flavor; no new lint warnings. Existing Gradle deprecation notices remain.
- Both debug and both unsigned release APK tasks passed. All four generated BuildConfig variants contain `SOURCE_REVISION=0c463631-location-reliability-20261028-fix3`. This label identifies the explicitly supplied build input; the checkout still contains uncommitted changes rather than a committed release snapshot.
- `git diff --check` passed. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective changes remain uncommitted. The corrections affect `NavigationAcquisitionQuality`, `NavigationMovementEvidence`, `NavigationTripMotionStats`, their focused tests, specification/agent guardrails and this plan handoff. No other feature work was introduced.
- No phone was attached. No installation, device-route trial, watch delivery check or power comparison was performed. The prior Huawei APK predates these corrections.

Current debug artifacts supersede every earlier hash in this document:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `4EFD3D89D29870BFBB414018B753DA10289A9BD98FFE1B186AFB067BE68E22BE` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `A11DFCA3E6C54A88D4469B0DA93A9FD978CAA056DDE7D81AEDECDCD78112C921` |

Next: when a phone is connected and installation is authorized, use this latest labelled build for DEVICE_VALIDATION.md. Software checks do not establish physical positioning accuracy, Android/watch receipt latency, OEM wake timing or measured provider energy savings.

### Motion-confidence renewal corrections — 28 October 2026

- Authorization: the user asked to fix both findings from the latest independent review. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; inherited work and this correction remain uncommitted.
- Raw movement: `NavigationMovementEvidence` checks progress against the latest raw observation separated by at least 2 seconds before reusing the longer coherence window. If that interval supports less than 0.2 m/s, discard the accumulated movement window and require fresh independent confirmation. Older walking cannot indefinitely renew MOVING during contradictory repeated positions. The same evidence policy continues to drive the Kalman lag safeguard.
- Native speed: `NavigationNativeSpeedEvidence` requires two trustworthy same-provider readings separated by at least 2 seconds. Invalid/missing/untrustworthy speed evidence, provider changes, lifecycle reset and gaps over 65 seconds discard confirmation. Independently confirmed one-second and normal sparse readings remain supported; the existing current-observation 10-second read-time expiry is retained. Measurement display and acquisition-quality speed checks retain their own contracts.
- Regressions: new `NavigationMovingEvidenceRenewalTest` and `NavigationMovingEvidenceIntegrationTest` cover repeated-position renewal, raw/filter divergence, walking recovery, slow/sparse motion, isolated precise speed spikes, clustered/one-second readings, provider/gap/reset boundaries, interrupted native-speed confirmation, routed/Straight Line polling and trip totals.
- Before-fix reproduction: `testFdroidDebugUnitTest --tests '*NavigationMovingEvidence*Test' --console=plain --max-workers=2`; 10 tests, 10 failures against the pre-fix code. Output: `.codex-temp/motion-renewal-fix/before.log`.
- Initial focused verification: one assertion expected UNKNOWN for a repeated position with 0.3 m/s speed, although independently confirmed stationarity is permitted there. Corrected it to reject MOVING and retain departure checks. PMD also identified one method complexity limit; extracted history pruning without suppressions. Historical output: `.codex-temp/motion-renewal-fix/focused.log`.
- Focused final command: `testFdroidDebugUnitTest --tests '*NavigationMovingEvidence*Test' --tests '*NavigationMotionConfidenceTest' --tests '*NavigationStationarityTrackerTest' --tests '*NavigationLocationMotionModelTest' --tests '*RecordedLocation*Test' --tests '*NavigationDepartureIntegrationTest' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 31 seconds, zero PMD violations. Output: `.codex-temp/motion-renewal-fix/focused-2.log`.
- Updated the motion contract in `SPECIFICATION.md` and the existing guardrail in `AGENT.md`. No new permissions, strings, dependencies, timers or wake locks.
- Full both-flavor tests, lint, maintainability and all four APK tasks are running with `-PsourceRevision=0c463631-location-reliability-20261028-motionfix`. Final results and current artifact hashes will follow below. Physical validation remains pending.
- Full unit-test results: F-Droid 1,952 and GPlay 2,024; 3,976 total, zero failures/errors/skips. The 11 new regression cases run in each flavor. Maintainability passed. Lint and APK tasks are still running; final integrated outcome follows below.
- Original standalone reproductions now report UNKNOWN with departure verification enabled after the precise single speed spike and after 15/30/45 seconds of conflicting repeated positions. The session probe retains only the 3-second supported prefix before its first contradictory fix instead of adding 45 seconds of moving time. Formal integration tests additionally verify no continued moving/stationary credit and 3-second acquisition in both navigation modes. Output: `.codex-temp/motion-renewal-fix/probes-after.log`.
- `adb devices` returned no attached devices. No installation or physical trial has been performed.

### Filter lag and same-measurement replacement corrections — 28 October 2026

- Authorization: the user asked to fix both findings from the latest review. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; inherited work and these corrections remain uncommitted.
- Prior motion-renewal verification is now complete: `.codex-temp/motion-renewal-fix/full.log` reports BUILD SUCCESSFUL in 20m 51s, including both-flavor tests, maintainability, both debug lints and all four APK tasks. Its 3,976 tests and APKs predate the corrections below.
- Filter lag: raw directional corroboration now admits trustworthy measured speed through the shared `NavigationNativeSpeedEvidence` quality gate. Independent timing, recent progress, directionality, provider locality and position uncertainty still govern corroboration. Invalid/zero-overlapping speed uncertainty is rejected, and missing/uncertain course still cannot authorize course-based prediction. The original synthetic 1 m/s probe now has approximately 3 m maximum added lag both with and without speed-accuracy metadata, rather than approximately 18 m with trustworthy speed accuracy.
- Position replacement: an accepted same-measurement accuracy improvement updates the retained filtered position and restarts motion/course histories from one corrected observation. This handles same-provider improvements and provider handovers without treating a replacement as independent evidence or connecting old coordinate-derived travel. Older/unimproved duplicates remain rejected. Session regressions verify returned and retained position/provider/accuracy agree and fresh independent stop evidence can resume confirmation.
- Production changes: `NavigationMovementEvidence`, the shared native-speed quality predicate's package visibility, and `NavigationLocationMotionModel`. Added ten JVM regression methods across `LatLonKalmanMovementQualityTest`, `NavigationMovementSpeedAccuracyTest` and `NavigationSessionLocationReplacementTest`. Updated the specification and existing agent guardrail; no new permissions, dependencies, strings, timers or wake locks.
- Before-fix reproduction: `.\gradlew.bat testFdroidDebugUnitTest --tests '*LatLonKalmanMovementQualityTest' --tests '*NavigationSessionLocationReplacementTest' --continue --console=plain --max-workers=2`; six tests, five failures. Output: `.codex-temp/reliability-review-fixes/before.log`.
- Initial focused tests passed; PMD reported one duplicated provider literal in the new session test. Replaced it with the existing provider constants, without suppressions. Output: `.codex-temp/reliability-review-fixes/focused.log`.
- Final focused command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*LatLonKalman*Test' --tests '*NavigationSessionLocation*Test' --tests '*NavigationMovement*Test' --tests '*NavigationMovingEvidence*Test' --tests '*NavigationMotionConfidenceTest' --tests '*NavigationStationarityTrackerTest' --tests '*NavigationLocationMotionModelTest' --tests '*RecordedLocation*Test' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 34 seconds, zero PMD violations. Output: `.codex-temp/reliability-review-fixes/focused-final.log`. Original standalone probes also confirm both fixes: `.codex-temp/reliability-review-fixes/probes-after.log`.
- Full verification is running with `-PsourceRevision=0c463631-location-reliability-20261028-lag-replace`; exact output is `.codex-temp/reliability-review-fixes/full.log`. Final results and current APK hashes will follow below.
- `adb devices` returned no attached devices. No installation or physical trial has been performed. Continue to carry the device checks in DEVICE_VALIDATION.md.

#### Final filter-lag and replacement verification

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261028-lag-replace --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 21m 14s; 196 tasks (52 executed, 144 up to date). Exact output: `.codex-temp/reliability-review-fixes/full.log`.
- F-Droid: **1,962 tests**; GPlay: **2,034 tests**. Total: **3,996**, with zero failures, errors or skips. Both full test tasks executed, including ten new regression methods per flavor.
- Maintainability passed with zero PMD violations; no suppressions added. Both debug lints passed with the same existing `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings per flavor. Existing Gradle plugin deprecation notices remain; no new compiler/lint/PMD warnings were introduced by these fixes.
- Both debug and both unsigned release APK tasks passed. All four generated BuildConfig variants contain `SOURCE_REVISION=0c463631-location-reliability-20261028-lag-replace`; this is an explicit build input identifying the current uncommitted working tree, rather than a new commit.
- `git diff --check` passed. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective changes remain uncommitted. No installation or physical trial was performed.

Current debug APKs supersede all earlier artifact hashes in this document:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `9864991B4E66154A4AE2E571AF54308A40617A7A76491BAC057BC4A3231400E5` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `7FF7B51D8613670ADF9C1C6D23FD169E44868CAB1618BA9F15C7047D1D842D2C` |

Next: use these builds for DEVICE_VALIDATION.md when a phone is connected and installation is authorized. Positioning accuracy, screen-off phone/watch receipt, actual wake latency and provider power comparisons remain NEEDS_DEVICE; automated checks do not establish those outcomes.

### Position-correction confidence and turn-lag corrections — 28 October 2026

- Authorization: the user asked to fix both findings from the latest review. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; inherited changes and these fixes remain uncommitted.
- Raw motion: removed the lone-displacement shortcut and obsolete stop-reference state. Movement now requires independently confirmed native speed or at least three coherent raw observations. Accurate speedless observations can corroborate movement only with uncertainty at most 25 m, progress beyond both endpoint radii, independent timing and a plausible rate at most 3 m/s. Zero/invalid/untrustworthy native speed cannot be bypassed by coordinate changes. Removed the now-unused previous-raw accessor/state.
- Turns: `NavigationMovementEvidence` searches recent coherent portions of its existing bounded history, retaining independent sample spacing/count, minimum span, recent progress, quality, directness and plausible-rate checks. Old legs cannot disable the shared Kalman lag bound after a new direction is independently supported. No course prediction was added.
- Regressions: added `NavigationPositionCorrectionMotionTest` and `LatLonKalmanTurnLagTest`, plus routed/Straight Line trip/polling coverage in `NavigationMovingEvidenceIntegrationTest`. Cover single/repeated corrections, missing speed, clustered callbacks, poor quality, handover/gaps, ordinary turns and reversals with/without speed accuracy across 1/3/5/12-second cadence.
- Before-fix run: 10 focused tests, 8 failures against the pre-fix production code; `.codex-temp/location-audit/fix-before.txt`.
- Focused verification initially passed tests but PMD found one new test's cognitive complexity. Extracted its assertion helper without suppressions. Final focused tests and `complexityCheck` passed in 24 seconds; exact command/output: `.codex-temp/location-audit/fix-focused-final.txt`.
- Original standalone reproducers now show zero moving time/distance and active departure checks for every contradictory correction. Maximum added lag on the 90-degree synthetic walk is approximately 3 m across all eight cadence/speed-quality combinations, previously 14.56–19.07 m. Outputs: `.codex-temp/location-audit/session-results-after.txt` and `turn-results-after.txt`. These are synthetic added-lag measurements, not surveyed position accuracy.
- Updated the motion/filter contracts in `SPECIFICATION.md` and the existing guardrail in `AGENT.md`. No permissions, dependencies, strings, timers or wake locks were added.
- Full verification is running with `-PsourceRevision=0c463631-location-reliability-20261028-turnfix`; exact output: `.codex-temp/location-audit/fix-full.txt`. Results and hashes follow below. Physical validation remains pending; earlier APKs are superseded only after this run completes.
- Additional consumer audit: the first recent-window implementation selected the shortest valid span, which could suppress walking display speed when filtered uncertainty was larger than raw uncertainty. Retain the longest still-coherent span among the eligible recent portions instead. Added `NavigationFilteredMovementDisplayTest` for steady walking and turns across raw accuracy levels, with/without speed accuracy. Final focused regression/PMD run passed in 37 seconds: `.codex-temp/location-audit/fix-focused-display.txt`.
- Cancelled the earlier full run before completion to incorporate that display regression fix; it is not a passed verification. Final integrated run now uses `.codex-temp/location-audit/fix-full-final.txt` with the same explicit `0c463631-location-reliability-20261028-turnfix` source label. `adb devices` returned no attached devices; no installation or physical trial was performed.

#### Final correction-confidence and turn-lag verification

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261028-turnfix --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 12m 40s; 196 tasks (51 executed, 145 up to date). Exact console output: `.codex-temp/location-audit/fix-full-final.txt`.
- F-Droid: **1,972 tests**; GPlay: **2,044 tests**; **4,016 total**, zero failures/errors/skips. Ten added regression methods run in each flavor.
- Maintainability passed with zero PMD violations and no suppressions. Both debug lints passed with zero errors and the same two existing warnings per flavor (`AppBundleLocaleChanges`, `AndroidGradlePluginVersion`). Existing Gradle deprecation notices remain; no new compiler/lint/PMD warnings.
- Both debug and both unsigned release APK tasks passed. All four generated BuildConfig variants contain `SOURCE_REVISION=0c463631-location-reliability-20261028-turnfix`. This identifies the supplied build input for the current uncommitted checkout, not a new commit.
- Final standalone probes retain approximately 3 m maximum added lag through a right-angle turn at 1/3/5/12-second cadence, with/without speed accuracy. Confirmed walking display speed remains available at 5/13/30 m raw uncertainty. Contradictory zero-speed corrections add zero moving time and zero travelled distance while retaining departure verification. Outputs: `.codex-temp/location-audit/turn-results-final.txt`, `display-results-final.txt`, `session-results-final.txt`.
- `git diff --check` passed. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective changes remain uncommitted. Updated DEVICE_VALIDATION.md to use the latest STATUS.md build/hash instead of its stale hardcoded build label.
- No phone was attached. No installation or physical trial was performed. Continue with DEVICE_VALIDATION.md; surveyed positioning accuracy, screen-off phone/watch receipt, actual wake latency and measured provider power differences remain NEEDS_DEVICE.

Current debug APKs supersede all earlier artifact hashes in this document:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `DE895DDC7E1DEDE759A5B0C806FC35156E889D9DC3C8EE4FADB382979727097C` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `B02D8B0DF99FCE44F27D6124319F58FF12FE592B9D495F70FAA0938166373C57` |

### ETA confidence and observation-time corrections — 28 October 2026

- Authorization: the user asked to fix both remaining ETA findings from the current independent review. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective changes remain uncommitted.
- Before-fix reproduction: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationEtaReliabilityIntegrationTest' --console=plain --max-workers=2`; all four new session tests failed. Exact output: `.codex-temp/review-plan-20261028/eta-before.log`.
- Added `NavigationRouteEtaHistory` for bounded provider-local live ETA observations. A confirmed-movement flag from the raw motion assessment gates history and live resolution separately from the existing stationary boolean. UNKNOWN/stop observations discard ETA anchors; unsupported departure cannot bridge prior travel. Unknown live velocity falls back to route-model timing.
- ETA anchors now use each location's monotonic measurement time. Direction/deviation history retains receipt time and can no longer implicitly seed live ETA from beeline completion, manual skips or tentative deviations. Untrusted route progress and navigation resets discard live ETA evidence.
- Display snapshots retain motion state; phone/Auto route state and direction details pass the current monotonic time. Expired display reads suppress live estimates without erasing still-valid sparse-cadence anchors. Direction details receive the service's existing clock, with no new timer.
- Regression coverage includes zero-speed corrections and turn alerts; constant/variable callback delay; speed and acceleration; 12/20/30/60-second observations; UNKNOWN/stop/provider/reset/gap boundaries; stale/future observations; duplicate callbacks; synthetic direction progress; untrusted route matches; and expired display reads. Existing policy fixtures now seed ETA history explicitly and supply confirmed movement at the full route-evaluation boundary.
- Focused verification: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationEtaReliabilityIntegrationTest' --tests '*NavigationRouteProgressTrackerTest' --tests '*NavigationRouteEtaHistoryTest' --tests '*NavigationMovingEvidenceIntegrationTest' --tests '*NavigationSessionRouteState*Test' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 41 seconds. Exact output: `.codex-temp/review-plan-20261028/eta-focused-4.log`. Subsequent final additions cover sparse buckets and direction-detail parity in the full run below. Earlier attempts are historical compile/maintainability corrections, not final verification; no suppressions were added.
- Updated the ETA contract in SPECIFICATION.md and existing motion guardrail in AGENT.md. No permissions, dependencies, strings, timers or wake locks were added.
- Full both-flavor tests, lint, maintainability and all four APK tasks are running with `-PsourceRevision=0c463631-location-reliability-20261028-etafix`; exact output: `.codex-temp/review-plan-20261028/eta-full.log`. Final outcome and APK hashes follow below. Device validation remains pending.
- Final new regressions also pass directly against the compiled JVM classes: `org.junit.runner.JUnitCore vibro.navigator.nav.guidance.NavigationRouteEtaHistoryTest vibro.navigator.nav.session.NavigationEtaReliabilityIntegrationTest`; OK (16 tests), including direction-detail parity. Output: `.codex-temp/review-plan-20261028/eta-junit-final.log`.
- Original standalone probes now show no live speed or turn event from the contradictory zero-speed corrections, and approximately 1 m/s ETA speed for the delayed 1 m/s walk. Outputs: `.codex-temp/review-plan-20261028/eta-results-after.txt` and `eta-clock-results-after.txt`.
- Full test results: F-Droid 1,988 and GPlay 2,060; 4,048 total with zero failures/errors/skips. PMD reports zero violations. Lint and APK tasks are still running; final integrated outcome follows below. `adb devices` lists no attached device; no installation or physical trial was performed.

#### Final ETA correction verification

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261028-etafix --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 17m 33s; 196 tasks (53 executed, 143 up to date). Exact console output: `.codex-temp/review-plan-20261028/eta-full.log`.
- F-Droid: **1,988 tests**; GPlay: **2,060 tests**; **4,048 total**, zero failures/errors/skips. Sixteen new regression methods execute in each flavor. No earlier assertions were weakened; existing policy fixtures were migrated to explicit ETA sampling and confirmed-movement parameters.
- PMD: zero violations, no suppressions added. Both debug lints passed with zero errors and only the existing `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings per flavor. Existing Gradle deprecation notices remain.
- Both debug and both unsigned release APK tasks passed. All four BuildConfig variants contain `SOURCE_REVISION=0c463631-location-reliability-20261028-etafix`. This labels the explicitly supplied build input for the uncommitted checkout, not a new commit.
- After whitespace-only cleanup, refreshed both debug packages and maintainability with `.\gradlew.bat assembleFdroidDebug assembleGplayDebug complexityCheck -PsourceRevision=0c463631-location-reliability-20261028-etafix --console=plain --max-workers=2`: BUILD SUCCESSFUL, exit 0, 39 seconds. Output: `.codex-temp/review-plan-20261028/eta-final-packaging.log`. The final compiled classes also pass all 16 new regressions directly via JUnit; output: `.codex-temp/review-plan-20261028/eta-junit-packaged.log`. No behavioral edits followed the full run.
- `git diff --check` passed. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective changes remain uncommitted. Current changed-file inventory: `.codex-temp/review-plan-20261028/eta-worktree-status.txt`.
- Production scope: new `NavigationRouteEtaHistory`; explicit ETA ownership in `NavigationRouteProgressTracker`; the confirmed-movement route-evaluation parameter; motion-state display snapshots and current-clock direction details through `NavigationSessionDirectionDetailsBuilder`, `NavigationSessionResourceAdapter` and `NavigationServiceBinderHost`; `NavigationMotionState.isMoving()` and unavailable live-velocity semantics. Regression classes are `NavigationRouteEtaHistoryTest` and `NavigationEtaReliabilityIntegrationTest`; existing tracker and route-policy fixture signatures were adapted. Updated SPECIFICATION.md, AGENT.md and this handoff.
- `adb devices` again lists no attached device. No installation or physical trial was performed. Phone/Auto physical presentation, screen-off phone/watch receipt, actual wake timing, surveyed position accuracy and provider power comparison remain NEEDS_DEVICE.

Current debug APKs supersede all earlier hashes in this document:

| Flavor | Local artifact | SHA-256 |
|---|---|---|
| gplay | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `1C85B5E97DC1F5DBEB60A79DB960A5681AB694F953502FCE3086F63586B1A102` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `931874846A272339E8B66DE3B6CF532B7BF864ED6476F2797D9C8A2B74462B1C` |

Next: use this labelled build and DEVICE_VALIDATION.md when a phone is connected and installation is authorized. Both ETA review findings are corrected; field outcomes remain unmeasured.

### Display/arrival freshness and guidance diagnostics — 29 October 2026

- Authorization: after the independent review found stale direct-guidance ETA, missing arrival receipt-time freshness and incomplete guidance diagnostics, the user asked to fix all three without introducing regressions. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; inherited and corrective changes remain uncommitted.
- Display: `NavigationDisplaySnapshot` suppresses expired/future speed and stationary cues using `NavigationMotionState.isFresh` and the shared 10-second evidence lifetime. Straight Line and beeline state/details suppress stale ETA; beeline text cannot replace stale speed with default walking time. Independently usable fresh speed and route-model timing retain their existing rules. Expiry does not mutate the stored position or clear sparse ETA anchors. Direct-guidance notification timing also rejects stale live speed.
- Arrival: explicit current monotonic evaluation/application time reaches `NavigationArrivalConfirmation` through routed final/intermediate checks, Straight Line, native/synthetic beeline completion, reacquisition and ordinary/confirmed-speculative route-result application. Stale/future observations reset confirmation. Stale fixes cannot establish Round Trip departure. Quality/radius limits, provider locality, independent measurement spacing and co-located stop order remain intact.
- Diagnostics: `TurnEventPlanner.Progress` carries an immutable `NavigationGuidanceDecision` with the actual target, distance, available ETA and outcome. Records distinguish not-due, already-sent, missing ETA, unreliable distance, untrusted progress, unconfirmed arrival, generated alerts and exhausted hints. Scheduler limiting causes are recorded from applied limits, including maneuver guard, quality margin/recovery and the post-maneuver ramp. Suggested cadence is labelled separately from actual provider requests. Target/reason/trust/interval changes and generated events bypass unchanged-record throttling; no instrumentation timer or wake lock was added.
- Regression coverage: added display/notification freshness, delayed Straight Line/routed/final-beeline arrival, ordered co-located stops, Round Trip departure, planner outcome/flag preservation, cadence reasons and diagnostic-throttle tests. Extended arrival freshness boundaries. Existing route-result fixtures now provide their actual elapsed application time; existing assertions were retained.
- Focused verification: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationDisplayFreshnessTest' --tests '*NavigationArrival*Test' --tests '*NavigationRoundTripArrivalFreshnessTest' --tests '*NavigationGuidanceDiagnosticsTest' --tests '*NavigationAcquisitionDiagnosticsTest' --tests '*Turn*Test' --tests '*NavigationEtaReliabilityIntegrationTest' --tests '*NavigationSessionRouteState*Test' --tests '*NavigationRouteStartApproachStateTest' --tests '*NavigationUpdateSchedulerTest' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 22 seconds, 136 tests, zero failures/errors/skips and zero PMD violations. Output: `.codex-temp/reliability-review-20261029/fix-focused-passed.log`. Earlier iterations exposed fixture/API migration and maintainability issues; they are historical attempts, not final verification.
- Original session probes now show `--` for expired Straight Line ETA/details at 21 and 120 seconds, and zero arrival events from both 14-second-delayed fixes. Before/after outputs: `.codex-temp/reliability-review-20261029/probe-results.txt` and `probe-results-after.txt`. These are synthetic policy checks, not surveyed positioning or OS delivery tests.
- Updated SPECIFICATION.md and AGENT.md with the freshness, arrival-clock and diagnostic ownership contracts. No dependency, permission, localization, provider-selection, wake-lock or alarm-policy changes were made.
- Final integrated verification is running with `-PsourceRevision=0c463631-location-reliability-20261029-freshness`; exact command and result follow below. Output: `.codex-temp/reliability-review-20261029/fix-full.log`. `adb devices` reports no attached device; no installation or physical trial was performed.

#### Integrated pass and heading-cache audit

- The first full integration command above completed successfully in 14m 42s: 4,096 passing tests (F-Droid 2,012, GPlay 2,084), zero PMD violations, both debug lints with zero errors and the two existing warnings per flavor, and both debug plus both unsigned release APKs. Output: `.codex-temp/reliability-review-20261029/fix-full.log`. This pass predates the cache correction below and is not the final artifact state.
- A subsequent display-path audit found that heading-only broadcasts could preserve old speed/ETA text by reusing a previously fresh structural snapshot. `NavigationServiceStateCache.canRefreshHeadingOnly` now checks original cached fix time against the current monotonic clock. It requests a structural refresh once when finite cached speed expires/is future-dated; the resulting unavailable-speed snapshot can be reused normally. `NavigationStateBroadcaster` uses one clock value for both eligibility and heading refresh. No timer, wake-up or per-frame rebuild loop was added.
- Added `NavigationHeadingCacheFreshnessTest` through actual Straight Line session snapshots with distinct calendar/monotonic clocks. It covers deadline boundaries, interleaved heading updates, one-time invalidation, unchanged expired-state reuse, fresh-fix recovery, future evidence and cache clearing. Existing cache tests retain their assertions and now pass explicit time.
- Focused cache/arrival/display/diagnostics checks plus PMD passed: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationHeadingCacheFreshnessTest' --tests '*NavigationServiceStateCacheTest' --tests '*NavigationStateBroadcaster*Test' --tests '*NavigationDisplayFreshnessTest' --tests '*NavigationArrival*Test' --tests '*NavigationGuidanceDiagnosticsTest' --tests '*NavigationAcquisitionDiagnosticsTest' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 1m 6s. Output: `.codex-temp/reliability-review-20261029/heading-cache-focused.log`.
- Final full verification is now running with source label `0c463631-location-reliability-20261029-freshness-cache`; output `.codex-temp/reliability-review-20261029/fix-full-cache.log`. Final counts and hashes follow below after completion; do not treat the preceding APKs as verified cache-correction builds.

#### Final freshness/cache verification

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261029-freshness-cache --continue --console=plain --max-workers=2
```

- **BUILD SUCCESSFUL**, exit 0, 13m 10s; 196 tasks (52 executed, 144 up to date). Exact output: `.codex-temp/reliability-review-20261029/fix-full-cache.log`.
- F-Droid: **2,015 tests**; GPlay: **2,087 tests**; **4,102 total**, zero failures/errors/skips. This adds 27 regression methods per flavor to the reviewed 4,048-test baseline, without removing or weakening earlier assertions.
- PMD: zero violations; no suppressions added. Both debug lints passed with zero errors and the same existing `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings per flavor. Existing Gradle deprecation notices remain; no new compiler/lint/PMD warnings were introduced.
- Both debug and both unsigned release APK tasks passed. All four generated BuildConfig files contain `SOURCE_REVISION=0c463631-location-reliability-20261029-freshness-cache`. The label identifies the explicitly supplied build input for this uncommitted checkout, not a new commit.
- Post-build checks: artifact hashes and source labels verified, `git diff --check` passed. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective work remains uncommitted. No branch, commit, installation or physical trial was performed. Current artifact metadata is also saved in `.codex-temp/reliability-review-20261029/final-artifacts.json`.
- Adapter-boundary review found no newly added Android location/GMS references. The existing `android/startup/AndroidFirstRunPermissionsTest` import of `android.location.LocationManager` is also present in HEAD and was left unchanged; production location-framework isolation is unchanged.
- `adb devices` after final verification lists no attached device. Next: install the verified debug build when a device is connected and installation is authorized, then follow DEVICE_VALIDATION.md. Positioning truth, phone/watch receipt, actual wake latency, visual phone/Auto checks and measured provider power comparisons remain NEEDS_DEVICE.

The following artifacts supersede all earlier hashes in this document:

| Flavor/build | Local artifact | SHA-256 |
|---|---|---|
| fdroid/debug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `3E3B4AE451BDF2B989A342300A8A60112447AD669C809F201C3645D9FD1C31F8` |
| fdroid/release | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | `2820539677ABDF2D3F307797CE0265609A126E4F05D11A8F857B2F21954E5D8A` |
| gplay/debug | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `286FF7BA52EF40479D4E3219D9883AD1237749380BEFC749C9F41DCA44CF4E90` |
| gplay/release | `app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk` | `A716A2CD548A9DC8527B7EB8D3AF471ABD3266101A1BECC76A800CAB724DE248` |

### Independent plan and arrival-lifecycle review — 29 October 2026

- Authorization: the user requested an independent review of the uncommitted implementation against the plan and fixes for demonstrated gaps. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; the inherited implementation and this correction remain uncommitted. Reviewed the plan, motion/filter/ETA/acquisition policies, arrival and beeline boundaries, lifecycle handoffs, recovery, battery telemetry and diagnostics. Physical trials remain separate.
- Baseline: `.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck --continue --console=plain --max-workers=2` passed in 5m 27s. Output: `.codex-temp/plan-review-20261029/baseline.log`.
- Reproduced a Round Trip gap: a fresh but poor current or smoothed fix away from the loop start could set the departure latch, allowing later good fixes at the start to announce a finish. `NavigationRouteGeometryState` now shares `NavigationArrivalConfirmation.hasUsableAccuracy` before accepting departure evidence. Existing freshness, bounded radius and end-progress checks remain in force.
- Reproduced a lifecycle gap: partial final/intermediate arrival confirmation survived a short pause. Routed, Straight Line and active beeline confirmation now resets at pause/resume without clearing completed stops or legitimate loop departure. Resume also clears evidence seeded by an asynchronous route result during pause. `NavigationSession` retains its existing departure-monitor restart.
- Before-fix proof: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationArrivalLifecycleTest' --tests '*NavigationRoundTripDepartureQualityTest' --continue --console=plain --max-workers=2` failed all six initial regressions. Output: `.codex-temp/plan-review-20261029/arrival-before.log`. A separate JUnit run of the expanded lifecycle tests then reproduced route-result reseeding during pause (six tests, one failure) against the first correction; output: `paused-route-before.log` in the same directory. Its compilation used a separate ignored probe directory and did not modify Gradle outputs.
- Expanded regression coverage comprises 11 test methods in `NavigationArrivalLifecycleTest` and `NavigationRoundTripDepartureQualityTest`: routed/Straight Line final and intermediate arrivals; active final beelines; route application while paused; completed-stop preservation; missing/invalid/boundary uncertainty; and valid loop departure/finish after reset.
- Focused verification: arrival/beeline/route-state/stationary-sleep tests plus PMD passed in 28s (`arrival-focused.log`). After the asynchronous correction, `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationArrivalLifecycleTest' --tests '*NavigationRoundTripDepartureQualityTest' complexityCheck --continue --console=plain --max-workers=2` passed all 11 new tests and maintainability in 56s (`lifecycle-focused.log`). No suppressions or weakened inherited assertions were added.
- Production changes owned by this review: `NavigationArrivalConfirmation`, `NavigationRouteGeometryState`, `NavigationIntermediateArrivalTracker`, `NavigationSessionRouteState`, `StraightLineNavigationState` and `NavigationSession`; plus the two new test classes, SPECIFICATION.md, AGENT.md and this handoff. No dependency, permission, localization, provider-selection, wake-lock or alarm API changes.
- The first integration attempt (`final-verification.log`, source label ending `arrival-review`) was deliberately stopped after the asynchronous correction was discovered. It is superseded and is not final verification evidence.
- Final verification is running with the command below; actual results and artifact hashes follow after completion. Current source-input hashes are saved in `.codex-temp/plan-review-20261029/verified-source-inputs.json`.

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261029-arrival-lifecycle --continue --console=plain --max-workers=2
```

Output: `.codex-temp/plan-review-20261029/final-lifecycle-verification.log`. No phone is attached (`adb devices`); no installation, publication or physical trial was performed. Field accuracy, phone/watch receipt, idle wake latency, visual phone/Auto checks and measured provider power differences remain NEEDS_DEVICE.

#### Final arrival/lifecycle verification

- The complete command above finished **BUILD SUCCESSFUL**, exit 0, in **9m 42s**; 196 tasks (52 executed, 144 up to date).
- F-Droid: **2,026 tests**; GPlay: **2,098 tests**; **4,124 total**, zero failures/errors/skips. Eleven new regression methods run in each flavor. Counts are saved in `.codex-temp/plan-review-20261029/final-test-counts.json`.
- PMD: zero violations, no suppressions added. Both debug lints passed with zero errors and the same two existing warnings per flavor (`AppBundleLocaleChanges`, `AndroidGradlePluginVersion`). Existing Gradle deprecation notices remain; no new compiler/lint/PMD warnings were introduced.
- Both debug APKs and both unsigned release APKs built successfully. All four generated BuildConfig variants contain `SOURCE_REVISION=0c463631-location-reliability-20261029-arrival-lifecycle`; this labels the supplied uncommitted source input, not a new commit.
- Post-build checks: `git diff --check` passed; the saved hashes of all eight source/test files owned by this review still match the files on disk. Artifact hashes are saved in `.codex-temp/plan-review-20261029/final-artifacts.json`; working-tree inventory is `final-worktree-status.txt` in the same directory. No source edits followed verification.
- Adapter/flavor review found no newly introduced Android location, GMS or Android Auto framework leakage. The existing startup-permissions test location import and package-name diagnostic string remain unchanged.
- Next: follow DEVICE_VALIDATION.md with the latest labelled APK when a phone is connected and installation is authorized. Screen-off phone/watch receipt, actual wake timing, positioning truth, visual phone/Auto checks and provider power comparison remain NEEDS_DEVICE. No phone was attached for this review.

These artifacts supersede all earlier hashes in this document:

| Flavor/build | Local artifact | SHA-256 |
|---|---|---|
| fdroid/debug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `1443E600E8EA423D5A42A24F05DCD8018FA457F7D0328B36356A839403E95F67` |
| fdroid/release | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | `2F3E9A6FD47816B174B9B38741E294970ACF19C8B063C787840451C3C53C5EE6` |
| gplay/debug | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `74FD786A70F8824A1DB13EED16382D90B4B0C4FDFD99518D9781FBFF9D92EDA4` |
| gplay/release | `app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk` | `6F56AD3C8E63B3C4DB58831D37626B8BBC4BD581E3318915DC6B9C219AFCAFEE` |

### Independent motion-window and ETA-reversal audit — 29 October 2026

- Authorization: the user asked to check the uncommitted implementation against the plan and fix demonstrated reliability gaps. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; inherited and corrective work remains uncommitted. Reviewed motion, filtering, ETA, acquisition quality/recovery, arrival/stop ordering, provider backoff, trip accounting, diagnostics and recovery against the plan's acceptance criteria. Device-only outcomes remain separate.
- Reproduced two gaps. First, renewing stationarity from only the nearest one-second pair concealed accumulated slow raw displacement, including speedless walking and contradictory zero-speed drift. Second, ETA's minimum two-second speed-sample interval could hide a newer reversal and reuse an older forward speed/acceleration anchor.
- `NavigationRawMotionHistory` now checks retained raw positions across its existing bounded ten-second window before confirming a stop. A preceding native moving observation ends the older window, so a subsequent genuine native stop still confirms promptly. Speedless stops may wait for preceding displacement to leave that window. The separate recent-position check used to suppress contradictory displayed speed is retained. No timer, threshold relaxation or sleep-policy change was added.
- `NavigationRouteEtaHistory` suppresses live forward estimates on newer backward progress and discards earlier ETA anchors before recording the new observation. This applies inside the minimum sample interval; fresh forward evidence rebuilds speed without reusing old acceleration. Older/duplicate measurements cannot trigger the reversal reset. Display resolution remains read-only with respect to this reset and preserves sparse-cadence anchors.
- Before-fix evidence: a separate `javac`/JUnit probe, using its own ignored class directory and the original compiled production classes, ran 18 tests with four failures (two motion cases and two reversal cases). Output: `.codex-temp/plan-audit-20261029/regressions-before.log`. The initial correction passed all 18 (`regressions-after.log`); final coverage adds chronological replay protection and routed/Straight Line session assertions.
- Eight added regression methods across `NavigationSlowWalkStopTest`, `NavigationRouteEtaHistoryTest` and `NavigationMovingEvidenceIntegrationTest` cover slow speedless walking, zero-speed drift, real stop confirmation/recovery, sub-two-second reversals, fresh forward recovery, older callbacks, stationary orientation eligibility and trip accounting in both navigation modes. Inherited assertions were retained.
- Broader focused motion/orientation/trip/replay tests and PMD passed in 1m 46s (`focused.log`). Final focused regressions, session integration and PMD passed in 1m 8s (`integration-focused.log`). The earlier `baseline.log` invocation passed its flavor tests but PMD observed the in-progress correction and reported two complexity violations; the helper was simplified without suppressions before the passing focused runs. That mixed-input invocation is not final verification evidence.
- Updated SPECIFICATION.md and the existing AGENT.md guardrail. Production scope owned by this audit is the two motion/ETA collaborators above; no dependency, permission, localization, provider-selection, wake-lock or alarm changes were made. Source hashes for both production and all three test files are saved in `.codex-temp/plan-audit-20261029/verified-source-inputs.json`.
- Final integrated verification is running with the following command; actual results follow after completion. Output: `.codex-temp/plan-audit-20261029/final-verification.log`. `adb devices` lists no connected phone; no installation or physical trial was performed.

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-location-reliability-20261029-motion-eta-audit --continue --console=plain --max-workers=2
```

#### Final motion-window and ETA-reversal verification

- The complete command above finished **BUILD SUCCESSFUL**, exit 0, in **12m 1s**; 196 tasks (52 executed, 144 up to date). Exact console output: `.codex-temp/plan-audit-20261029/final-verification.log`.
- F-Droid: **2,034 tests**; GPlay: **2,106 tests**; **4,140 total**, zero failures/errors/skips. Eight new regression methods run in each flavor. Counts are saved in `.codex-temp/plan-audit-20261029/final-test-counts.json`.
- PMD: zero violations, no suppressions added. Both debug lints passed with zero errors and only the same two pre-existing warnings per flavor (`AppBundleLocaleChanges`, `AndroidGradlePluginVersion`). Existing Gradle deprecation notices remain. Lint counts are saved in `final-lint-counts.json` in the same directory.
- Both debug and both unsigned release APKs built successfully. All four generated BuildConfig files contain `SOURCE_REVISION=0c463631-location-reliability-20261029-motion-eta-audit`. This identifies the explicit input label for the uncommitted checkout, not a new commit.
- All five saved source/test hashes still match the reviewed files after verification, and `git diff --check` passed. No production or test edits followed the passing focused session checks or the integrated verification. Artifact hashes and working-tree inventory are saved in `final-artifacts.json` and `final-worktree-status.txt` in the same directory. All inherited changes remain uncommitted.
- The adapter/flavor audit found no new Android location, Google Play Services or Android Auto leakage. The existing startup-permissions test's `LocationManager` import is also present in HEAD and remains unchanged.
- Updated DEVICE_VALIDATION.md with the accumulated slow-displacement and brief-reversal checks. `adb devices` still lists no phone. No installation or physical trial was performed. Next: use the latest verified APK below, with authorized installation when a phone is connected, and follow that protocol. Actual positioning accuracy, screen-off phone/watch delivery, wake latency, visual phone/Auto behavior and measured power comparisons remain NEEDS_DEVICE.

These artifacts supersede the earlier hashes in this document:

| Flavor/build | Local artifact | SHA-256 |
|---|---|---|
| fdroid/debug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `39C3AAC304C9444DD31574CEBF6F85C65EC882B8B5AE1EA8B974577D820C658F` |
| fdroid/release | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | `63662F3345A745369A4C4D359DEF60D2DAD3B940BAC8D8E904DE0B834873C2AB` |
| gplay/debug | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `C54471CCFD0EEA54EEF5DB20C280D2B0CD2B3BD723E98F2DE692119B48DC13D0` |
| gplay/release | `app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk` | `2A7D1B130EEA94FDFB2959E56B4CAF3ED5F3668F8DE725E7B1EA355FB2502A12` |

### Independent slow-walking corroboration audit — 29 October 2026

- Authorization: the user requested review of the uncommitted changes against the plan, followed by fixes for demonstrated defects. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and corrective work remains uncommitted. Reviewed motion/filter evidence, ETA, acquisition/recovery, arrival and lifecycle handoffs, provider backoff, battery validation and callback diagnostics against the plan and specification.
- Reproduced a quality-gate inconsistency: accurate native speeds below or equal to 0.35 m/s could not corroborate positional movement because `NavigationMovementEvidence` reused the native-speed-only movement threshold. Supplying precise speed uncertainty therefore made otherwise eligible sustained slow walking remain UNKNOWN and disabled its Kalman lag safeguard.
- `NavigationNativeSpeedEvidence` now separates positive-speed confidence from the native-only movement threshold. Raw position corroboration accepts slow measured speed only when its uncertainty excludes zero, retaining the existing independent observation count, spacing/span, quality, directness and displacement-rate gates. Native speed alone still requires its previous threshold and independent timing. No acquisition, arrival, provider-selection, sleep, prediction or display-speed threshold was relaxed.
- Five added regression methods cover 0.25/0.30/0.35 m/s movement, stationary positions with precise slow speed, zero-overlapping speed uncertainty, conservative filtered accuracy and lag, and Route/Straight Line motion and trip totals. Existing assertions remain unchanged.
- Before-fix evidence: isolated `javac`/JUnit execution against the originally compiled production classes failed the two motion/filter regressions (9 tests, 2 failures); the same tests passed with the corrected collaborators (9 tests, zero failures). Outputs: `.codex-temp/reliability-audit-current/slow-walking-before.log` and `slow-walking-after.log`. These probes used separate ignored class directories without modifying Gradle outputs.
- Baseline: `.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck --continue --console=plain --max-workers=2` passed in 5m 28s; output `baseline.log` in the same directory. Compilation preceded this audit's source changes.
- Focused verification: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationMovementSpeedAccuracyTest' --tests '*LatLonKalman*Test' --tests '*NavigationMovingEvidence*Test' --tests '*NavigationMotionConfidenceTest' --tests '*NavigationSlowWalkStopTest' --tests '*NavigationLocationMotionModelTest' --tests '*NavigationStationarityTrackerTest' complexityCheck --continue --console=plain --max-workers=2` passed in 19s: 93 tests, zero failures/errors/skips, zero PMD violations. Output: `focused.log`.
- Updated SPECIFICATION.md and AGENT.md with the distinction between native-only movement and slow positional corroboration. Production scope owned by this audit: `NavigationNativeSpeedEvidence` and `NavigationMovementEvidence`; test scope: `NavigationMovementSpeedAccuracyTest`, `LatLonKalmanMovementQualityTest` and `NavigationMovingEvidenceIntegrationTest`. Saved source hashes: `verified-source-inputs.json` in the same directory.
- Final integrated verification is running with the command below. Exact results and artifact hashes follow after completion; previous artifacts do not contain this correction. No phone is attached (`adb devices`); no installation, commit or physical trial was performed. Actual positioning accuracy, screen-off phone/watch receipt, wake latency and provider power comparison remain NEEDS_DEVICE.

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-reliability-slow-walking-20261029 --continue --console=plain --max-workers=2
```

Output: `.codex-temp/reliability-audit-current/final-verification.log`. Next: inspect the final results below, then follow DEVICE_VALIDATION.md with the latest verified APK when a phone is connected and installation is authorized.

#### Final slow-walking verification

- The complete command above finished **BUILD SUCCESSFUL**, exit 0, in **11m 32s**; 196 tasks (52 executed, 144 up to date). Exact console output: `.codex-temp/reliability-audit-current/final-verification.log`.
- F-Droid: **2,039 tests**; GPlay: **2,111 tests**; **4,150 total**, zero failures/errors/skips. Five added regression methods run in each flavor. Counts: `final-test-counts.json` in the same directory.
- PMD: zero violations; no suppressions added. Both current debug lint reports passed with zero errors and only the same two pre-existing warnings per flavor (`AppBundleLocaleChanges`, `AndroidGradlePluginVersion`). Existing Gradle deprecation notices remain; no new compiler/lint/PMD warnings were introduced. Current report timestamps/counts: `final-lint-counts.json`.
- Both debug and both unsigned release APKs built successfully. All four generated BuildConfig files contain `SOURCE_REVISION=0c463631-reliability-slow-walking-20261029`, the explicit build input identifying this uncommitted checkout, not a new commit. Artifact hashes: `final-artifacts.json`.
- All five saved production/test hashes still match the reviewed sources after verification. `git diff --check` passed. The adapter/flavor diff check found no new Android location, Google Play Services or direct Android monotonic-clock references outside the existing boundaries. No production or test edits followed verification.
- Updated DEVICE_VALIDATION.md with the precise slow-speed case. No phone is attached, and no install, branch, commit or physical trial was performed. Next: use the latest verified APK below with an authorized installation when a phone is connected, then follow that protocol. Positioning truth, screen-off phone/watch receipt, actual idle wake latency, visual phone/Auto behavior and provider power comparisons remain NEEDS_DEVICE.

These artifacts supersede the earlier hashes in this document:

| Flavor/build | Local artifact | SHA-256 |
|---|---|---|
| fdroid/debug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `6356683B4BB1388C4F500E0855879813C0F045BC325C9C8A8E89104D6C4E25ED` |
| fdroid/release | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | `7CC7C5475893D1B10C3A69C0A890EF761069C92EBA0F4F0EFB86A8FAA1E50CF3` |
| gplay/debug | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `8BC7D1E2ECD977B9A9D2614EE949F25DE5E6E1E527C9EAE5CA462F8C1B0D3B29` |
| gplay/release | `app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk` | `2B7FB612D2F70BE834026D9D501E67E9482FC8295FA04509F4742B788352E30E` |

### Frequent-callback ETA and Round Trip confirmation audit — 29 October 2026

- Authorization: the user requested review of all uncommitted changes against the plan and correction of demonstrated defects. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`. Inherited source/test/documentation work remains uncommitted; no branch, commit, install or publication was performed.
- Reviewed the software stages against RUNS.md, REFERENCE_PLAN.md, the original assessment, current specification and prior handoffs. No evidence justifies changing source selection, adding wake locks/alarms, or claiming measured guidance accuracy, phone/watch delivery, wake latency or provider power savings. Runs 11/14 still require physical validation.
- Reproduced ETA starvation: always selecting the latest two-second anchor rejects sustained one-second walking at 1 m/s with 12 m uncertainty, and three-second slow walking at 0.3 m/s with 5 m uncertainty. `RouteEtaMotionEstimator` now finds the latest anchor with enough displacement within the existing bounded history, while recent progress below the speed floor, stalls and reversals prevent reuse of older speed. Both acceleration intervals must independently satisfy the same quality gate; total spans over 10 seconds still cannot estimate acceleration. No quality threshold, motion-confirmation gate or fixed acquisition setting was relaxed.
- Reproduced Round Trip confirmation retention: an eligible fix near the finish, an intervening match at the loop start, and another finish match could reuse the first confirmation timestamp. `NavigationRouteGeometryState` now resets pending arrival evidence whenever end progress is ineligible, preserving already observed loop departure.
- Five regression methods cover one-second walking, slow three-second walking, the minimum speed boundary, stalled/insignificant/reversed progress, acceleration and loss/recovery of Round Trip end eligibility. Isolated javac/JUnit probes against the inherited compiled classes failed both walking cases and the Round Trip case before correction; corrected probes passed. Evidence: `.codex-temp/eta-cadence-probe/before.txt`, `arrival-before.txt` and `fixes-after.txt`. Later Gradle verification includes the final minimum-speed case.
- The first broad review command (`testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug --continue --console=plain --max-workers=2`) failed one pre-existing asynchronous street-overlay test, while GPlay, PMD and both lints passed. Output: `.codex-temp/reliability-review-checks-20261029.txt`. Its test fake ran result callbacks on the loader thread, racing settings/cache changes and the next phase's latch. The test now queues results and applies them on the test thread, matching production's serialized result delivery; assertions and production street code are unchanged. The original focused rerun passed, followed by a passing run with this deterministic scheduler.
- Final focused command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*nav.guidance.*' --tests '*NavigationRoundTrip*Test' --tests '*NavigationArrival*Test' --tests '*NavigationEtaReliabilityIntegrationTest' --tests '*SurroundingStreetOverlayControllerTest' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 11 seconds, 186 tests, zero failures/errors/skips and zero PMD violations. Output/counts: `.codex-temp/eta-cadence-probe/focused-final.txt` and `focused-counts.json`.
- Updated SPECIFICATION.md, AGENT.md and DEVICE_VALIDATION.md. Production changes owned by this audit are limited to the ETA estimator and Round Trip geometry gate; test changes are the two new regression classes and street-overlay test scheduler. No strings, dependencies, permissions, provider subscriptions or timing wake-ups were added. Saved production/test hashes: `.codex-temp/eta-cadence-probe/verified-source-inputs.json`.
- Final integrated verification is running with the following command; actual results and artifacts follow below. `adb devices` lists no connected phone, so no install or field trial was possible.

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-reliability-eta-arrival-20261029 --continue --console=plain --max-workers=2
```

Output: `.codex-temp/eta-cadence-probe/final-verification.txt`. Next: verify the completed checks and source hashes, then follow DEVICE_VALIDATION.md with the current APK when a phone is connected and installation is authorized.

#### Final frequent-callback ETA and Round Trip verification

- The complete command above finished BUILD SUCCESSFUL, exit 0, in 15 minutes; 196 tasks (52 executed, 144 up to date). Exact output: `.codex-temp/eta-cadence-probe/final-verification.txt`.
- F-Droid: 2,044 tests; GPlay: 2,116 tests; 4,160 total, zero failures/errors/skips. Counts: `final-test-counts.json` in the same directory. All five new regressions run in each flavor.
- PMD: zero violations, no new suppressions. Both debug lints passed with zero errors and the same pre-existing `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings per flavor. Existing Gradle deprecation notices remain; no new compiler/lint/PMD warnings were introduced. Current report timestamps/counts: `final-lint-counts.json`.
- Both debug and both unsigned release APKs built successfully. All four generated BuildConfig files contain `SOURCE_REVISION=0c463631-reliability-eta-arrival-20261029`, an explicit build input for this uncommitted checkout, not a new commit. Artifact sizes, hashes and timestamps: `final-artifacts.json`.
- The corrected street-overlay scheduler also passed 100 isolated JUnit suite repetitions (600 test executions), without writing to Gradle outputs. Evidence: `street-stress.txt` in the same directory. This is separate stress evidence, not part of the 4,160-test suite count.
- All five saved production/test hashes still match the verified files. `git diff --check` passed and HEAD is unchanged. No source/test edits followed the passing focused checks or full verification. Working-tree inventory: `final-worktree-status.txt`.
- No connected phone was available; no installation or physical trial was performed. Actual positioning accuracy, phone/watch receipt, idle wake latency, phone/Auto visual behavior and measured power comparisons remain NEEDS_DEVICE. Next: follow DEVICE_VALIDATION.md with the current verified APK when a phone is connected and installation is authorized.

These artifacts supersede all earlier hashes in this document:

| Flavor/build | Local artifact | SHA-256 |
|---|---|---|
| fdroid/debug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `4D989BCE05C052962028F0A9D0D8057E840C1963EC2E6615B898765A12AB5C7F` |
| fdroid/release | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | `7B60E462FEB8908B32BDC86234B098471617C8B51523C734D49391B9389642E4` |
| gplay/debug | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `3FEF445536BEA4EED484D14EF8211F404F709CE8D7972B4102A505D7E6157966` |
| gplay/release | `app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk` | `B0830B0BD98F3F34EF3C54C8363065AC9B489E0CC9ABB2E4407E882A5D2A18A2` |

### Independent trip-speed freshness audit — 29 October 2026

- Authorization: the user requested review of the uncommitted changes against the plan and correction of demonstrated defects. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; inherited work remains uncommitted. Reviewed motion/filter evidence, ETA/cadence, ordered arrival and beeline completion, provider backoff/recovery, lifecycle resets, diagnostics and battery/trip consumers against the plan and current specification.
- Reproduced one remaining consumer gap: an accepted location may be 11 seconds old (inside selection's 15-second limit), so its motion evidence and displayed live speed have expired. `NavigationTripStatsTracker` nevertheless retained its reported speed as the permanent trip maximum. The session regression reproduced a jump from 1 to 20 m/s despite expired motion evidence. Future-dated speed could also enter that statistic.
- Maximum-speed accounting now uses the existing `NavigationMotionState.isFresh` gate for monotonic observations. Earlier usable maxima remain intact, fresh readings can still raise them, and existing callers without a monotonic timestamp retain their receipt-time compatibility. No guidance, provider-selection, acquisition, sleep or battery policy changed.
- Added three regression methods across `NavigationTripMotionExpiryTest` and `NavigationMovingEvidenceIntegrationTest`: stale/future rejection and fresh recovery, the inclusive 10-second boundary, and actual accepted delayed callbacks through both Route and Straight Line sessions. Specification section 4's trip-accounting contract now records the same requirement.
- Baseline command: `.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck --console=plain --max-workers=2`; BUILD SUCCESSFUL in 7m 25s. Output: `.codex-temp/reliability-audit-baseline-20261029.txt`.
- Before-fix regression command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationTripMotionExpiryTest' --tests '*NavigationMovingEvidenceIntegrationTest' --console=plain --max-workers=2`; 15 tests, two expected failures at the maximum-speed assertions. Output: `.codex-temp/reliability-audit-speed-before-final-20261029.txt`. The earlier probe additionally exposed an overbroad test-helper assertion about the untimed compatibility accessor; the final regression explicitly checks receipt-time motion freshness instead.
- Final focused command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationTrip*Test' --tests '*NavigationMovingEvidenceIntegrationTest' --tests '*NavigationStationarySleepTripStatsTest' --tests '*NavigationTripStatsFormatterTest' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 14s, 42 tests, zero failures/errors/skips, zero PMD violations. Output: `.codex-temp/reliability-audit-speed-focused-final-20261029.txt`. The initial focused attempt passed its tests but found a duplicate provider literal in the added coverage; using the existing provider constant removed that violation without suppression.
- Production scope owned by this audit: `NavigationTripStatsTracker`; test scope: the two classes above. Source hashes: `.codex-temp/reliability-audit-inputs-20261029.json`. All other implementation changes are inherited. No branch, commit, installation or publication was performed.
- Final integrated verification is running with the command below; actual results follow after completion. No phone is attached (`adb devices`). Field guidance accuracy, Android/watch receipt, idle wake latency, phone/Auto visuals and provider energy comparison remain NEEDS_DEVICE.

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-reliability-trip-speed-20261029 --continue --console=plain --max-workers=2
```

Output: `.codex-temp/reliability-audit-final-20261029.txt`. Next: inspect the final results below, then use DEVICE_VALIDATION.md with the latest verified APK when a phone is connected and installation is authorized.

#### Final trip-speed freshness verification

- The complete command above finished BUILD SUCCESSFUL, exit 0, in 11m 6s; 196 tasks (52 executed, 144 up to date). Exact console output: `.codex-temp/reliability-audit-final-20261029.txt`.
- F-Droid: 2,047 tests; GPlay: 2,119 tests; 4,166 total, zero failures/errors/skips. All three new regression methods run in both flavors. Counts: `.codex-temp/reliability-audit-test-counts-20261029.json`.
- PMD: zero violations, no suppressions added. Both debug lints passed with zero errors and the same pre-existing AppBundleLocaleChanges and AndroidGradlePluginVersion warnings per flavor. Existing Gradle deprecation notices remain; no new compiler/lint/PMD warnings were introduced. Lint evidence: `.codex-temp/reliability-audit-lint-20261029.json`.
- Both debug and both unsigned release APKs built successfully. All four generated BuildConfig files contain `SOURCE_REVISION=0c463631-reliability-trip-speed-20261029`, the explicit input label for the uncommitted checkout, not a new commit. Sizes, timestamps and hashes: `.codex-temp/reliability-audit-artifacts-20261029.json`.
- All three saved production/test hashes still match the verified files. `git diff --check` passed, HEAD is unchanged, and no production/test edits followed the successful focused and integrated checks. All inherited implementation changes remain uncommitted.
- No connected phone was available; no installation or physical trial was performed. The remaining work is the device protocol in DEVICE_VALIDATION.md. Actual positioning accuracy, phone/watch receipt, idle wake latency, phone/Auto visuals and provider power savings remain NEEDS_DEVICE.

These artifacts supersede all earlier hashes in this document:

| Flavor/build | Local artifact | SHA-256 |
|---|---|---|
| fdroid/debug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | `9303FBD49F272AC615C2F76AB83F80EFBA97CD6EA087FE9F185C3F6E95CC9CA7` |
| fdroid/release | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | `4166FD0E5064820A9413812F8B3C1BE12F2B7AD3706A73FE307DBE703D80B344` |
| gplay/debug | `app/build/outputs/apk/gplay/debug/app-gplay-debug.apk` | `B469CC46EAE058FD28CAA789017AFCC635132344655ADAE3981CEC680637A98A` |
| gplay/release | `app/build/outputs/apk/gplay/release/app-gplay-release-unsigned.apk` | `4FBB599C9D1DFA38DBADAFE9D75CE5BF10CBB8706348641F98255D338D20F4B5` |

### Precise slow-speed stop-confidence audit — 29 October 2026

- Authorization: the user requested another review of the uncommitted implementation against the plan and correction of demonstrated defects. HEAD remains `0c46363134228bbbf35b3327612c4ca776d3c05b`; all inherited and new implementation changes remain uncommitted. Reviewed the plan's software stages against motion/filtering, ETA/cadence, arrival/order/lifecycle, provider backoff, diagnostics, battery/trip and display consumers. Physical validation remains separate.
- Reproduced a remaining contradictory-evidence gap: precise positive speed below the native-only movement threshold could still confirm STATIONARY from nearby/repeated raw positions. The same low-speed threshold could permit stationary sleep after sustained repeated positions. At 0.25–0.35 m/s with uncertainty excluding zero, this could authorize orientation advice, stationary accounting or subscription suspension before position evidence establishes movement.
- Raw stop evidence and `StationaryFixEligibility` now reuse `NavigationNativeSpeedEvidence.hasConfidentPositiveSpeed` to reject that contradiction. Motion remains UNKNOWN with active departure checks until independent raw progress corroborates MOVING. A low speed whose uncertainty includes zero may still support independently timed stop positions. No movement is inferred from slow native speed alone; ordinary native-zero and speedless stationary confirmation remain supported.
- Production scope owned by this audit: `NavigationRawMotionHistory` and `StationaryFixEligibility`. Added/strengthened regressions in `NavigationMovementSpeedAccuracyTest`, `NavigationStationarySleepTest`, `NavigationMovingEvidenceIntegrationTest`, and `NavigationStationarityTrackerTest`. The old boundary assertion expecting STATIONARY despite reliably positive 0.35 m/s now explicitly requires UNKNOWN. Route and Straight Line session tests cover orientation eligibility, departure cadence and no invented motion totals; sleep coverage also verifies fresh zero-speed recovery. SPECIFICATION.md and AGENT.md record the shared contradiction gate.
- Sequential red command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationMovementSpeedAccuracyTest' --tests '*NavigationMovingEvidenceIntegrationTest' --tests '*NavigationStationarySleepTest' --console=plain --max-workers=2`; 23 tests, four expected assertion failures reproducing the motion, sleep and session defects. Output: `.codex-temp/reliability-review-slow-stop-red-20261029.txt`.
- An earlier baseline and focused launch overlapped and collided in Gradle's shared test-result files (EOF/missing binary result file). Those attempts are invalid verification evidence. All subsequent Gradle runs are sequential. The first post-fix focused run passed the session/sleep regressions but exposed insufficient displacement in the new 20-second slow-walk fixture; extended it to 32 seconds so its final MOVING assertion meets the unchanged position-uncertainty threshold. No production threshold was relaxed.
- Final focused command: `.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationMovementSpeedAccuracyTest' --tests '*NavigationMovingEvidenceIntegrationTest' --tests '*NavigationStationar*Test' --tests '*NavigationSlowWalkStopTest' --tests '*NavigationMotion*Test' --tests '*NavigationOrientationControllerTest' --tests '*RecordedLocation*Test' complexityCheck --continue --console=plain --max-workers=2`; BUILD SUCCESSFUL in 19s; 75 tests, zero failures/errors/skips, zero PMD violations. Output: `.codex-temp/reliability-review-slow-stop-focused-final-20261029.txt`.
- Full integrated verification is running with the command below; actual completion and artifact evidence follow. Six source/test hashes are saved in `.codex-temp/reliability-review-source-hashes-20261029.json`. No branch, commit, installation or publication has been performed.

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=0c463631-reliability-slow-stop-20261029 --continue --console=plain --max-workers=2
```

Output: `.codex-temp/reliability-review-final-20261029.txt`. Real positioning accuracy, Android/watch receipt, idle wake latency, phone/Auto visuals and provider energy comparison remain NEEDS_DEVICE. Use DEVICE_VALIDATION.md after the integrated build passes and a phone is connected with installation authorized.

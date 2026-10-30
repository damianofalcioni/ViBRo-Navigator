# Location reliability and battery implementation plan

Prepared 27 October 2026 from the two route logs and the current implementation. This document plans the work; it does not change application behavior.

## Objective and priorities

Make screen-off guidance timely and conservative, then reduce unnecessary legacy GNSS/network acquisition and measure its energy cost against fused. A provider's reported accuracy is an uncertainty estimate, not an independent position reference. Success requires both replay evidence and physical-device validation.

Priority order:

1. Establish reproducible evidence and diagnostics.
2. Correct motion classification, filtering and live ETA evidence.
3. Make acquisition uncertainty-aware and arrival confirmation conservative.
4. Improve provider selection/acquisition efficiency and investigate notification/recovery timing.
5. Validate battery telemetry and perform controlled provider comparisons.

Each implementation step should be a reviewable change with focused regression coverage. Keep policy in the existing Java domain collaborators, Android APIs in adapters, and Google dependencies confined to the GPlay flavor. Update SPECIFICATION.md when the planned behavior changes an existing contract. No public documentation or release work is needed merely to create this plan.

## 1. Establish replay fixtures and attribution — P0, foundation

**Problem:** The existing logs have no build revision, no surveyed truth, unequal sampling and different provider switch points. They cannot conclusively attribute position error, battery use or watch notification latency.

**Solution:** Extract compact replay fixtures from both trips using relative monotonic time and coordinates translated into local metre offsets. Retain provider, observation/receipt times, native accuracy, speed/bearing metadata, requested cadence, provider transitions and relevant route/hint geometry. Preserve duplicate and rejected measurements needed to reproduce decisions. Use actual recorded inputs to reproduce state transitions; do not label every apparent displacement as true walking. Add independently defined synthetic walking, stopping and stationary-drift trajectories for correctness assertions.

Extend existing diagnostics with build revision, request/measurement/event identities, motion state and evidence reason, provider selection/rejection reason, raw/filtered uncertainty and displacement, route progress, remaining hint distance, timing estimate and cadence decision reason. Throttle unchanged routine records and retain buffered writes without periodic logging wake-ups.

**Acceptance:** Both supplied incidents replay deterministically without storing the user's absolute coordinates in committed resources. New diagnostics can connect a fix to a guidance decision and notification post. Synthetic fixtures provide independent expected outcomes.

**Owners:** nav/location, nav/kalman, nav/session, nav/guidance and logging; existing location/filter/desk-log replay tests.

## 2. Persistent stationary classification and inappropriate orientation notices — P0

**Evidence:** Stationary classification persists during approximately 71 m of filtered displacement on 27 October and approximately 176 m with fused on 23 October. Speed-accuracy metadata is absent, and current evidence gates can prevent recovery from a stop classification.

**Solution:** Introduce an explicit motion assessment with MOVING, STATIONARY and UNKNOWN states, confidence/evidence reason and freshness. Expire a previously confirmed stop when observations no longer support it. Confirm movement using several independently timed, directionally consistent raw observations, accounting for uncertainty and cadence. Keep evidence provider-local and discard coordinate-derived velocity/course at handover; source offsets must never count as movement. An isolated native speed, precise zero-speed observation or coordinate jump must not determine the state.

Use confirmed stationarity for orientation advice and sleep eligibility. UNKNOWN requests departure verification and suppresses confident “turn yourself” prompts. Review all boolean stationary consumers: displayed speed, compass zoom/heading, route ETA, arrival, trip statistics, departure monitoring and stationary sleep. Unobserved time must not become invented travel or confident stopped time; preserve intentional-sleep accounting and lifecycle resets.

**Acceptance:** Synthetic coherent walking with missing speed accuracy clears a prior stop after sufficient evidence; stationary drift, source switches, clustered callbacks and a single speed spike do not. Recorded cases no longer remain confidently stationary when their available evidence only supports UNKNOWN. Orientation prompts require fresh confirmed stop evidence. Both flavors and all navigation modes share the assessment.

**Owners:** NavigationStationarityTracker, NavigationMovementEvidence, NavigationLocationMotionModel, orientation and trip-stat consumers.

## 3. Excess filter displacement and its feedback into motion — P0

**Evidence:** Accepted GNSS raw-to-filtered displacement reaches 29 m on 27 October. Filter-expanded uncertainty can block the motion fallback, and behavior changes at the current 15 s sparse-interval boundary.

**Solution:** Keep provider measurement uncertainty, filter uncertainty and raw-to-filtered lag as separate quantities. Give motion assessment access to raw evidence so confidence is not determined solely by a lagging filtered output. Retain Kalman filtering, but adapt process uncertainty and bound additional lag when independent evidence supports movement. Re-anchor conservatively when stale state is no longer useful; handle sparse intervals consistently around 12, 15, 20, 30 and 60 s. Keep displayed-position uncertainty conservative.

Do not infer velocity from a position correction or relax jump rejection to obtain an attractive trace. Trusted measured course remains the prerequisite for course-based prediction. Preserve delayed-network correction chronology and apply-once rules.

**Acceptance:** Compare along-path lag against independent synthetic ground truth, stationary jitter and response to turns/stops. Establish replay-supported lag bounds rather than promising a numerical accuracy gain from the logs. Show reduced added lag on coherent moving sequences without new stationary drift, overshoot, false reroutes or provider-offset velocity.

**Owners:** LatLonKalmanFilter, LocationFilterPolicy and NavigationSessionLocationState. Integrate with item 2 without circular confidence dependencies.

## 4. Live ETA evidence disappears at normal sparse cadences — P1

**Problem:** The route-progress history expires after 10 s, although normal requests may be 12–60 s apart. A normal interval can therefore remove the anchor needed for a live route-speed estimate.

**Solution:** Separate short-window direction/deviation history from cadence-aware ETA history. Retain sufficient independently timed, same-provider route progress to estimate speed over a normal requested interval, with a bounded freshness limit and confidence. Allow acceleration only with sufficient recent evidence. Stop, reversal, provider transition, route replacement, intentional sleep and unexpected gaps invalidate the appropriate history. Use route timing when live motion is genuinely unknown; do not represent unavailable live speed as confirmed zero speed.

**Acceptance:** Normal 12/20/30/60 s moving samples retain usable ETA evidence when supported, while a stop, turn, reversal, outlier or provider offset cannot carry stale speed/acceleration forward. Existing fast deviation/direction checks retain their shorter window.

**Owners:** NavigationRouteProgressTracker, RouteEtaMotionEstimator and RouteTimeEstimator. Depends on items 2–3.

## 5. Acquisition cadence ignores position and maneuver uncertainty — P0

**Evidence:** The 08:47 incident combines 12–20 s requests with approximately 68–87 m raw uncertainty. Current cadence depends on nominal ETA without an explicit uncertainty margin.

**Solution:** Add a quality-aware cap to the existing dynamic scheduler. Determine the earliest plausible approach to the next maneuver using remaining route distance, observation age, position/filter uncertainty, route-match confidence and a conservative speed estimate. Treat this as a heuristic safety margin, not a guaranteed bound from a 68% accuracy radius. If the margin overlaps an upcoming maneuver, or motion/progress confidence deteriorates, request a short 3 s verification burst. Relax through the existing buckets only after independent stable evidence restores a sufficient margin.

Preserve the fixed 1 s user setting, existing faster startup/deviation requests, bucket ramp and 60 s upper limit. Long reliable legs can keep sparse acquisition. Beeline polling and intermediate-stop checks retain their existing limits. Do not emit a maneuver solely from a timer or extrapolated position.

**Acceptance:** Recorded poor-quality approaches trigger verification earlier. Synthetic faster approaches and sudden speed changes do not skip the configured alert opportunity because of a stale long interval. Constant poor quality far from a maneuver does not cause endless request churn. Measure any extra acquisition cost alongside guidance improvements.

**Owners:** NavigationUpdateScheduler, NavigationTurnState and NavigationLocationUpdateRequestPolicy. Depends on items 2–4.

## 6. Premature definitive arrival — P0

**Evidence:** At 08:50:01, arrival is announced with the accepted filtered position approximately 67 m from the requested destination. The existing specification explicitly reuses the uncertainty-expanded off-track radius for arrival, so this requires a deliberate product-policy change.

**Solution:** Separate the permissive near-target/approach region from confirmed arrival. Define a bounded arrival radius, usable uncertainty limit, independent-fix confirmation and approach evidence. Poor accuracy alone must not enlarge the region for definitive arrival. Keep approach guidance active when confirmation is unavailable; do not force arrival after a timeout.

Apply the policy consistently to final destinations, intermediate stops and Straight Line mode. Retain Round Trip end-progress protection, ordered stop completion, manual skip behavior and final/intermediate beelines. Verify whether each check targets the requested destination or a snapped road endpoint so arrival at the road does not silently complete the remaining beeline. Reuse existing approach/progress presentation where possible; localize any necessary new “near destination” wording.

**Acceptance:** The recorded coarse correction cannot alone announce definitive arrival. Accurate walking into a destination confirms promptly with independent evidence; nearby parallel streets, loop starts, poor fixes, provider handovers and clustered callbacks do not complete the wrong target. Update arrival overlays and phone/Auto display contracts if the radius changes.

**Owners:** NavigationArrivalDetector, NavigationIntermediateArrivalTracker, NavigationRouteGeometryState and shared direct-guidance/arrival policies.

## 7. Network acquisition is driven by GNSS health alone — P1

**Evidence:** Network provides useful corrections but also 23 coarse 100 m fixes out of 49 on 27 October. Poor GNSS currently restores faster network acquisition even if network is also unhelpful.

**Solution:** Track network quality independently: freshness, plausible uncertainty, positional novelty in the context of confirmed motion and disagreement with recent valid observations. Combine this with existing raw-GNSS health. Start with the current 30 s slow cadence as the conservative backoff candidate for repeatedly coarse network output; retain periodic probes and restore responsiveness after useful network observations or GPS loss. Keep initial/pause/resume/provider-loss behavior responsive, with hysteresis and bounded recovery. An unchanged position during a real stop is not evidence of a bad provider.

Keep raw GNSS silence deadlines independent of accepted network progress. Reconcile subscriptions in place, leave the other provider uninterrupted and keep adaptation inactive while fused is primary. Update SPECIFICATION.md because current requirements explicitly restore the normal network cadence on poor GNSS.

**Acceptance:** The coarse-repeat segment requests fewer network callbacks without losing useful recovery when network improves. Network-only/GPS-unavailable operation remains responsive. Genuine stationary fixes, delayed good network fixes and alternating quality do not cause starvation or rapid mode switching. Report subscription exposure separately from measured energy.

**Owners:** LegacyLocationAcquisitionPolicy, NavigationLocationAcquisition and existing subscription/request adapters.

## 8. Provider disagreement, selection and handover — P1 verification, conditional change

**Evidence:** Nearly contemporaneous network/GNSS positions disagree by up to 235 m. The logs do not establish which source is correct, or that an individual rejection was erroneous.

**Solution:** Replay selection and dispatch gates, including potentially useful network replacements that arrive inside the current requested interval. Preserve chronology, duplicate suppression, accuracy hysteresis, spatial plausibility and inactive-provider retirement. Correct a gate only if a replay demonstrates a useful chronological improvement is being lost. A newer timestamp on a coarse repeated position is not fresh evidence of motion.

Keep position correction separate from speed/course history. Retain the existing narrow delayed-network correction path and its uncertainty allowance. Do not add arithmetic averaging or a permanent fused + GPS + network subscription. At the old handover, fused and GPS represent essentially the same observation, which must not be counted twice.

**Acceptance:** Large source offsets create neither velocity nor turn progress, late retired callbacks are ignored, useful fresh improvements can be consumed under the intended policy, and duplicate observations do not increase confidence. Tests may confirm current behavior needs no change.

**Owners:** LiveLocationPolicy, LiveLocationDispatchPolicy, LiveLocationCoordinator and handover/filter integration tests.

## 9. Background notification delay attribution — P0 diagnostics; fix depends on evidence

**Problem:** Existing logs show rapid event-to-post calls but cannot establish Android/watch presentation latency or exact physical turn-crossing time. A minute-scale freeze at 08:47 is unconfirmed.

**Solution:** Correlate measurement receipt, acceptance, maneuver decision and NotificationManager post using a shared event identity and monotonic times. Record the next hint and explicit suppression reason when an expected alert is withheld. Log screen transitions, elapsed realtime versus uptime, and alarm requested/actual dispatch times. Keep diagnostics lightweight and event-driven.

Reproduce on the Huawei with the screen off and the same watch connected, noting phone versus watch receipt. Use a timely system bugreport/notification diagnostics to distinguish late guidance decisions, app posting, Android presentation and watch forwarding. If app decisions are late, address items 2–5. If posting is delayed, fix the demonstrated scheduling path. If the phone posts promptly but the watch is late, investigate the forwarding path with that evidence.

**Acceptance:** A reproduced delay can be attributed to a stage, or explicitly remains unobserved. Do not claim that a unit test, successful notify call or foreground service proves actual watch delivery. Add a short scoped wake lock only if a measured critical section requires it.

**Owners:** NavigationServiceLocationHandler, NavigationTurnEventDispatcher, AndroidNavigationForegroundController and Android time/screen adapters.

## 10. Recovery and stationary sleep under idle restrictions — P1 hardening

**Problem:** The existing inexact allow-while-idle alarm cannot guarantee a 30 s wake. The specification already acknowledges this. Neither log establishes this as the 08:47 cause.

**Solution:** Keep recovery as a backstop and distinguish an intended deadline from actual wake latency in diagnostics. Preserve the foreground subscription, awake timer, hardware significant-motion trigger, registration-failure fallback and independent GNSS-health checks. Verify deadline rescheduling, earlier-versus-later replacement, late-alarm handling and stale callback generations. UNKNOWN motion must retain active acquisition; only independently confirmed stationarity can suspend it.

Exercise motion wake, no-trigger devices, delayed alarms and failed scheduling on physical hardware. If testing demonstrates unacceptable departure latency, retain active acquisition for the affected run/configuration while investigating. Exact idle alarms are also subject to restrictions, so changing alarm API alone is not a demonstrated fix. No session-long wake lock or new exact-alarm permission is planned without a measured need and separate design review.

**Acceptance:** Registration failures keep acquisition active, delayed/stale events cannot restart stopped navigation, intended sleep is not treated as a provider stall, and real departure/recovery latency is recorded. Preserve sleep-time trip accounting and fixed-interval behavior.

**Owners:** NavigationServiceLocationRecovery, NavigationLocationRecoveryScheduler, AndroidNavigationLocationRecoveryAlarm and motion-trigger adapter.

## 11. Unreliable battery telemetry — P1

**Evidence:** Charge-counter readings rise while discharging and their scale is implausible for remaining µAh. Current units/support need validation. A stable percentage alone is not proof of an invalid reading.

**Solution:** Reuse and extend NavigationBatterySnapshot's existing plausibility checks rather than duplicate them. Keep raw vendor readings clearly identified, alongside validity, source, observation time, charging state and rejection reason. Validate counter direction and discontinuities across a measurement window, reset accounting at charging/reset boundaries and show unavailable consumption when the input is unsuitable. Never silently multiply values by an assumed unit correction. Compare property and broadcast percentage where supported, exposing disagreement or old readings without treating a normal percentage plateau as corruption.

Audit both logs and trip-stat consumers so rejected raw counters cannot produce believable mAh totals. Sample only on existing events and reuse existing caching.

**Acceptance:** Both supplied implausible counter sequences are excluded from energy totals; valid synthetic declining counters work; charging, unsupported values, gauge resets and stale snapshots are handled. Logs distinguish device-wide raw readings, validated counters and estimated consumption.

**Owners:** AndroidNavigationBatteryLogger, AndroidBatterySnapshotReader, NavigationBatterySnapshot/cache and trip-stat accounting.

## 12. Establish real provider accuracy and battery tradeoffs — P1 device experiment

**Solution:** After reliability fixes, run repeated screen-off trips with the same build, route, carrying position, notification/watch settings, logging level and comparable battery conditions. Alternate provider order and compare matching route sections, recording uncertainty, fix age, raw/filtered traces, missed/late/duplicate prompts, departure latency, provider request exposure and available GNSS-on/CPU-wake measures. Confirm the actual path/turn crossings independently; a planned route or fused fix is not ground truth.

Use validated energy readings if available; otherwise use suitable device power measurements and report the limitations. Avoid external USB power arrangements that change the phone's idle behavior unless explicitly controlled. A short diagnostic run may observe providers together for agreement, but must be kept separate from single-mode energy attribution. The legacy policy should already work in both flavors; no Google dependency is added to F-Droid.

**Acceptance:** Report repeated-run energy and guidance results with variability and confounders. Recommend a default/tuning only when energy improves without an unacceptable guidance or departure regression. If measurements remain unsuitable, report energy as unresolved rather than an estimated percentage saving.

## Delivery sequence and checks

| Change | Contents | Prerequisites |
|---|---|---|
| A | Relative-coordinate replay fixtures and event diagnostics; battery validation can begin here | None |
| B | Motion assessment and orientation/departure consumers | A |
| C | Filter uncertainty/lag separation and cadence-aware ETA history | A–B |
| D | Uncertainty-aware scheduler and confirmed-arrival policy | B–C |
| E | Independent network backoff and verified provider-gate improvements | A, then integration with B–D |
| F | Notification/recovery diagnosis, targeted fixes and physical sleep tests | A; retest after B–E |
| G | Telemetry completion and controlled route/power comparison | A–F |

For each change, extend the relevant existing JVM/Robolectric tests rather than create implementation-mirroring tests. Cover both fused and legacy operation, fixed/dynamic intervals, no native speed accuracy, slow walking, genuine stops, drift, reversals, sparse cadence, provider loss/handover, missing sensors, pause/resume and stop/destruction. Include Route, Round Trip and Straight Line with intermediate and final beelines.

Before delivery of application changes, run testFdroidDebugUnitTest, testGplayDebugUnitTest, complexityCheck, lintFdroidDebug, lintGplayDebug and the appropriate flavor builds, including release validation where distribution-sensitive boundaries change. Verify phone and Android Auto consumers for changed status/arrival behavior and localize new strings. Update the specification alongside deliberate changes to motion semantics, cadence, network restoration and arrival policy.

After automated checks pass, perform the physical-device trials; automated tests alone cannot validate OEM wake reliability, watch latency, actual position error or energy savings. Ask about a fresh recompile/install on a connected phone at the end of implementation, as required by the repository guide.

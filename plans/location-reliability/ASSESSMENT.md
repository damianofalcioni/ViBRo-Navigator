# Location and background-guidance review — 23 and 27 October 2026

Both logs were treated as diagnostic evidence. No application source was changed. Calculations use the attached files and the current checkout at `746aa24d`; the logs identify version 1.0.0 but do not record a source revision. Differences in instrumentation mean identical source code on both trips cannot be assumed.

## Conclusions

1. Legacy GNSS + network is viable on part of this route, but the logs do not establish a battery advantage over fused. The final section on 27 October has substantially worse reported GNSS uncertainty than fused on the same section on 23 October.
2. The 08:47 incident is consistent with fresh but inaccurate positions combined with relatively sparse evaluation. There is no evidence of a long location-callback suspension or queued turn events being released on an app wake-up. Android/watch presentation latency remains unmeasured.
3. The strongest application-side problems are persistent stationary classification during sustained position changes, substantial additional filter displacement, and a sampling policy that does not account for fix uncertainty. These can affect both fused and legacy operation.
4. Network acquisition can probably be made more selective. Increasing network frequency whenever GNSS deteriorates is wasteful when network itself repeatedly supplies coarse, unchanged positions. Savings need measurement.
5. Arrival can be declared tens of metres before the requested destination because the allowed arrival radius expands with uncertainty.

## Provider comparison

The 23 October trip changes from GNSS + network to fused at **08:33:18.924**. The 27 October trip changes from fused to GNSS + network at **08:34:51.287**. These are sequential comparisons on different dates; simultaneous independent observations and surveyed ground truth are absent.

Accuracy below means the provider's estimated horizontal 68% confidence radius, not measured position error. Duplicate callbacks with the same provider and monotonic measurement time are counted once, retaining their first delivery. Percentiles use linear interpolation. All unique incoming measurements are included, including measurements rejected by navigation.

| Trip / source | Unique measurements | Median radius | 95th-percentile radius | Median delivery age |
|---|---:|---:|---:|---:|
| 23 Oct GNSS | 107 | 19.0 m | 38.1 m | 46 ms |
| 23 Oct network | 15 | 21.2 m | 114.3 m | 3,087 ms |
| 23 Oct fused | 81 | 14.1 m | 29.2 m | 26 ms |
| 27 Oct GNSS | 218 | 22.7 m | 50.4 m | 16 ms |
| 27 Oct network | 49 | 27.2 m | 100.0 m | 89 ms |
| 27 Oct fused | 106 | 9.1 m | 16.5 m | 18 ms |

These overall figures mix different route sections and sampling frequencies. A more useful comparison projects fixes onto the initial 27 October planned route solely to assign broad spatial sections:

| Reference route section | 23 Oct source: median / p95; count | 27 Oct source: median / p95; count |
|---|---|---|
| First 700 m | GNSS: 19.0 / 38.2 m; 105 | Fused: 9.1 / 16.5 m; 106 |
| 700–1,300 m | Fused: 13.0 / 22.8 m; 27 | GNSS: 12.1 / 24.8 m; 95 |
| Beyond 1,300 m | Fused: 14.7 / 30.2 m; 54 | GNSS: 37.8 / 67.8 m; 109 |

The first sections include different starting paths/reroutes. Spatial projection is approximate, particularly for poor fixes; the planned route is not the actual travelled trace. Samples are correlated and weighted per fix, not per second or equal-distance bin. These figures support legacy viability in the middle corridor and identify a problematic final corridor, but do not establish a causal provider advantage.

At the 23 October handover, the last GPS fix and first fused fix have exactly the same monotonic measurement time (5,642,820 ms), virtually identical coordinates and 9.163 m uncertainty. Fused can return the underlying GNSS observation; it is not an independent truth source.

### What parallel network contributes

On 27 October, navigation accepted 210 GPS fixes and 9 network fixes, from 218 GPS and 49 network callbacks. Network contributed useful corrections around 08:45:09, 08:45:24 and 08:45:36; it should not simply be removed. Rejected measurements may still be retained as selection candidates, so acceptance counts alone are not a measure of provider utility or energy efficiency.

However, 23 of 49 network fixes report 100 m uncertainty. Several repeat the same coordinates with newer timestamps. Forty network/GPS pairs measured within two seconds of each other disagree by a median **35.0 m**, p95 **162.9 m**, maximum **235.0 m**. The largest disagreement is at 08:46:13 with only 176 ms between measurements. Movement during the pairing window cannot explain that example. This measures disagreement, not which provider was correct.

The current live policy primarily chooses one source with accuracy/freshness hysteresis. A separate restricted delayed-network correction exists; the 27 October dropped-fix records report `delayedPositionCorrection=false`. Running two subscriptions does not itself reproduce Google's fused estimator. Blindly averaging these positions would be inappropriate, especially given shared/correlated inputs and large disagreement.

## The background incident around 08:47 on 27 October

| Time | Logged evidence |
|---|---|
| 08:46:26 | GNSS interval increased to 20 s; raw radius approximately 53 m |
| 08:46:47 | GPS callback, approximately 76 m radius |
| 08:47:07 | GPS callback, 86.5 m radius; fix age 12 ms |
| 08:47:27 | GPS callback, 68.3 m radius; fix age 10 ms; interval shortened to 12 s |
| 08:47:40 / 08:47:52 | Further GPS fixes, approximately 70–72 m radius |
| 08:48:28–35 | Requested interval steps down through 8, 5 and 3 s |
| 08:48:35.234 → .266 | Continue event generated, then notification posted 32 ms later |
| 08:48:51.137 → .159 | Next Continue event generated, then posted 22 ms later |
| 08:49:42.150 → .173 | Next Continue event generated, then posted 23 ms later |

The device reports screen-off at battery samples 08:46:40, 08:47:44, 08:48:45 and 08:49:46. NavigationActivity resumes at 08:50:23, after those alerts. There is no logged stationary subscription suspension or location-recovery restart during the incident. Across the entire 27 October navigation, the largest gap between any location callbacks is 26.5 s, during an earlier requested 30 s interval.

Thus there is no evidence for a minute-scale app freeze at 08:47. The evidence points toward position/progress uncertainty and sampling cadence as contributors to perceived lateness. It does not establish exactly when the user passed a turn, nor exclude Android notification presentation or Bluetooth/watch delays: `Sent turn notification` confirms the app's posting call, not actual vibration/display receipt.

The older fused log also has two gaps near 35–40 s while a 20 s interval was requested, and 62–69 s gaps during requested 60 s intervals. Some delivered fixes are still fresh after those gaps. Longer requested intervals can leave guidance unevaluated for substantial periods with either provider.

## Other behaviors and code findings

### 1. Stationary classification persists through substantial position changes

On 27 October, consecutive accepted fixes remain `stationary=true` during:

- **08:44:26–08:45:46:** approximately 80 s and 71 m net filtered displacement, with GPS/network handovers.
- **08:48:35–08:49:39:** approximately 64 s and 71 m net filtered displacement, on GPS throughout.
- **08:49:45–08:50:39:** approximately 54 s and 64 m net filtered displacement.

This is also present with fused on 23 October: **08:41:42–08:43:42**, approximately two minutes and 176 m of filtered displacement. Position drift can imitate movement, so these are internal consistency problems and strong candidates for replay investigation, not proof of the user's motion at every instant.

The consequence is concrete: the motion model reports zero motion speed and orientation notices are generated, including 40° right at 08:48:40 and 90° right at 08:49:50. Zero motion also affects ETA/live-motion decisions and departure acquisition.

Code mechanisms: all GNSS and fused measurements in these logs lack speed-accuracy metadata. `NavigationStationarityTracker` cannot use its confident-speed exit; its displacement exit rejects fixes above 25 m filtered uncertainty. `NavigationMovementEvidence` provides a fallback but rejects filtered uncertainty above 50 m, resets on provider changes and on gaps over 10 s. Sustained movement can therefore fail to clear a previous stop classification. See [tracker](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/location/NavigationStationarityTracker.java:90) and [movement evidence](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/location/NavigationMovementEvidence.java:72).

Recommendation: evaluate a third motion state, unknown, and sustained directional evidence that tolerates realistic uncertainty and cadence. Preserve protections against stationary drift and source-offset jumps. Unknown motion should suppress confident “turn yourself” advice without automatically enabling stationary sleep. Do not solve this by blindly trusting every native speed.

### 2. Smoothing adds substantial displacement and can reinforce that classification

Accepted GPS fixes on 27 October have a median raw-to-filtered displacement of **7.0 m**, p95 **21.1 m**, maximum **29.0 m**. Displacement is not necessarily error, but coherent walking sequences show the filter trailing the changing raw positions. At approximately 1 m/s, 20 m of along-path lag would correspond to approximately 20 s of additional delay.

The filter's trusted course prediction requires speed and bearing accuracy, absent in both logs. It therefore uses positional smoothing without course-based prediction. Output uncertainty is conservatively increased by the raw-to-filtered displacement. That expanded uncertainty then enters motion eligibility, potentially obstructing movement confirmation. Moving intervals of 15 s or more reinitialize the filter, while 12 s intervals still smooth: the incident changes from zero displacement at the 20 s cadence to appreciable displacement at the 12 s cadence. See [filter policy](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/kalman/LocationFilterPolicy.java:18) and [filter output](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/kalman/LatLonKalmanFilter.java:105).

Recommendation: replay recorded trajectories with relative coordinates; separate uncertainty in the observation from display/filter lag when evaluating movement, and bound additional along-route lag using corroborated motion. Preserve jump rejection and stationary drift resistance.

### 3. Dynamic acquisition ignores location uncertainty

`NavigationUpdateScheduler` derives cadence from estimated time to the next hint and permits 20 s intervals within three minutes of a maneuver. Its inputs contain speed/acceleration but no accuracy or route-confidence bound. Route-progress ETA history is only 10 s long, so regular 12–20 s measurement spacing can also leave no recent anchor for a live route-speed estimate; ETA then falls back to the route model. See [scheduler](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/guidance/NavigationUpdateScheduler.java:79) and [progress history](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/guidance/NavigationRouteProgressTracker.java:15).

Recommendation: estimate earliest plausible maneuver arrival from remaining distance minus along-route uncertainty, using a conservative speed bound. Temporarily tighten acquisition when that margin becomes small or motion/route confidence deteriorates. Allow sparse acquisition on long, well-confirmed legs. This may increase consumption in difficult sections while allowing savings elsewhere; simply raising intervals everywhere would worsen guidance.

### 4. Arrival confidence expands with poor accuracy

At **08:50:01**, the accepted network observation causes “Destination reached.” Its raw position is approximately **45 m** from the requested destination and its filtered position approximately **67 m** away. The current arrival policy reuses the off-track threshold, `max(10 m, accuracy + 8 m)`, with smoothed filtered uncertainty. The route endpoint can differ slightly from the requested point. This can explain an early arrival announcement; actual user position is unknown. See [arrival radius](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/nav/route/NavigationRouteGeometryState.java:108).

Recommendation: distinguish “near destination” from confirmed arrival. Require bounded uncertainty, consistent approach/end-of-route evidence and repeated observations before a definitive arrival notification.

### 5. Recovery alarms provide no strict short-deadline guarantee

The recovery adapter uses `setAndAllowWhileIdle`, which is inexact and throttled. A requested 30 s stationary fallback or stall deadline is not a guaranteed wake time. This is an architectural limitation, not the demonstrated cause of the 08:47 event. There is no evidence that a permanent wake lock is needed for this recording. See [alarm adapter](D:/PROJECTS/vibe-navigator/app/src/main/java/vibro/navigator/android/location/AndroidNavigationLocationRecoveryAlarm.java:45).

## Battery assessment and improvement priorities

The logs cannot establish a provider energy ranking:

- The 27 October charge counter rises from 103 to 321 while unplugged/discharging, contrary to remaining-charge semantics. Current readings of approximately −180 to −895 labelled µA and a constant average of zero also require validation against the device's actual units/support.
- Battery percentage falls from 93% to 69%, then remains exactly 69% from approximately 08:40 to 08:49. It is unsuitable for segment-level energy attribution here.
- Screen state and sampling differ substantially. All 20 battery samples on 23 October report interactive=true. On 27 October, screen-off current magnitudes are much smaller than screen-on readings under both provider modes, but these are sparse device-wide snapshots in unvalidated units.
- Requested subscription duration is not measured GNSS hardware-on time. Parallel services and other apps may share the same radio/location hardware.

Recommended order:

1. Correct stationary/unknown-motion handling and limit filter-induced guidance lag; add replay coverage for both logs without saving absolute personal coordinates in tests.
2. Add uncertainty-aware maneuver acquisition. Use rapid verification bursts in the difficult final corridor and long intervals only where reliable progress leaves sufficient maneuver margin.
3. Keep the existing slower network cadence during stable GNSS, and add independent network-quality/novelty backoff. Coarse repeated network positions should trigger less frequent probes, with prompt restoration when network becomes useful or GPS is unavailable. Do not disable network solely because most callbacks were rejected.
4. Retain stationary suspension only after conservative stop confirmation, an armed hardware motion trigger and a successful fallback registration. Neither supplied log demonstrates a completed stationary suspension on 27 October; arming during confirmation is not proof of savings.
5. Validate power telemetry and run controlled screen-off A/B trips with the same build, logging, route, carrying position and acquisition constraints. Repeat in reversed order. A short simultaneous-provider diagnostic run can compare position agreement, but is unsuitable for attributing battery savings to a single provider.

Useful extra diagnostics: build revision; screen-interactivity transitions; monotonic measurement/receipt times; uptime versus elapsed time to expose deep sleep; scheduled versus actual recovery dispatch; hint identity, route progress, ETA and uncertainty at suppressed alerts; notification post sequence/time. A system bugreport captured close to a reproduced incident can help distinguish app posting from system/watch delivery.

## Reproducibility and references

Analysis scripts and machine-readable results are in this folder: `compare.py`, `details.py`, `comparison-metrics.json`, `sections.csv`, `pairs.csv`. No Android build/test was required for this read-only review. The scripts only read the supplied logs and write local analysis artifacts.

- [Android Location accuracy definition](https://developer.android.com/reference/android/location/Location#getAccuracy()): estimated 68% horizontal confidence radius.
- [Android location battery guidance](https://developer.android.com/develop/sensors-and-location/location/battery): accuracy, frequency and latency all affect consumption; fused may use multiple inputs and offers no universal measured saving for this device.
- [Screen-off navigation guidance](https://developer.android.com/develop/sensors-and-location/location/battery/scenarios): continuous user-visible navigation uses a foreground service.
- [AlarmManager.setAndAllowWhileIdle](https://developer.android.com/reference/android/app/AlarmManager#setAndAllowWhileIdle(int,long,android.app.PendingIntent)): batching and idle dispatch limits apply.

# Branch reliability review — 30 October 2026

Reviewed `codex/location-reliability` at `65de832d` against the software stages in
REFERENCE_PLAN.md and RUNS.md, with the 30 October route log supplied by the user.
The user confirmed that the recorded build matches `65de832d`, before the Round
Trip correction now committed as `a455ce9f`. Background exit
is excluded; the pre-existing untracked BACKGROUND_EXIT_20261030.md is untouched.
The follow-up changes and verification evidence are recorded below.

## Finding and correction

**Round Trip departure could remain hidden after the start segments were missed.**
Before departure was observed, `NavigationRouteGeometryState.match` always
preferred a projection onto the short initial route section, even when the user
was far beyond it. If poor fixes or sparse callbacks missed that section, later
good on-route fixes projected back onto its endpoint. The departure gate then
rejected their large apparent cross-track distance. A legitimate later finish
could never confirm; route guidance could also remain matched to the start.

A new regression reproduced the failure before the correction: six focused tests,
one failure at the expected finish assertion. Start anchoring now yields to an
ordinary non-finish route match beyond the initial section's existing corridor.
It still protects the finish until departure is observed, including when a
snapped route starts away from the user's location. The existing freshness, accuracy, route-distance,
departure and independently timed finish requirements still apply. Tests cover
missed start samples, poor early fixes, and rejection of poor/off-route departure
evidence on a densely sampled loop. No thresholds or arrival radii were loosened.
The first full-suite pass caught an initial-approach regression in the draft fix;
the final correction retains that existing protection and its regression test.
This is a synthetic geometry case discovered during review, not an incident
claimed to have occurred in the supplied Route-mode recording.

## Software assessment

| Plan area | Review result |
|---|---|
| Replays and attribution | Existing relative fixtures retained; added 30 October GPS/network callback excerpt through the session pipeline. Explicit source labels remain supported. |
| Motion and consumers | Raw evidence, independent native-speed corroboration, UNKNOWN handling, read-time expiry, departure checks and stop/sleep contradiction gates are present. No further demonstrated defect found. |
| Filter lag | Corroborated raw movement bounds additional lag without creating correction-derived velocity. Handover/jump/reacquisition protections remain covered. |
| ETA and acquisition | Separate measurement-time ETA history, sparse-cadence support, reversal/expiry handling, quality caps and independent recovery are present. Fixed one-second and faster overrides remain intact. |
| Arrival and target ordering | Bounded independent confirmation and barriers are present across routed/direct targets. Corrected the missed-departure Round Trip defect above. |
| Legacy providers | GNSS health, network quality, comparative usefulness and silence deadlines remain separate; fused-primary exclusivity and chronological dispatch are retained. |
| Notification and recovery | Trace/post/deadline diagnostics and conservative sleep prerequisites are present. The new recording supplies limited real-device evidence below. |
| Battery | Raw implausible counters are excluded; common discharge validation and trip accounting remain covered. The recording still cannot support energy attribution. |

This assessment describes the inspected code and automated coverage. It does not
establish positioning accuracy, notification receipt, worst-case OEM idle latency
or energy savings. Controlled device experiments in DEVICE_VALIDATION.md remain
necessary to complete the plan's physical validation.

## Supplied recording

Input: `vibro-navigator-log-20261030083155.txt`.
SHA-256: `F2E2576F578577B6F54978C11950740BE97DE01E150BCED88DCBD349B65A872F`.
The header records Huawei PRA-LX1 / API 26 / GPlay Debug and
`sourceRevision=unknown`; branch attribution comes from the user's confirmation.
The recording runs from 08:31:55 to 08:46:07 local time and contains no completed
arrival observation. Personal absolute positions remain outside this repository.

- 186 complete incoming-callback records: 110 fused, 57 GPS and 19 network.
  150 accepted updates: 92 fused, 51 GPS and 7 network. The final receipt record
  has no accompanying completed evaluation in the supplied text.
- Three stationary acquisition suspensions are logged. Two fallback checks resume
  acquisition around their requested 30-second deadlines; one motion-trigger
  wake at 08:35:04 resumes rapid acquisition. This single run does not establish
  worst-case idle wake latency or physical departure time.
- Screen-off callbacks continue. Legacy acquisition reaches a 60-second requested
  bucket, then tightens to 30 seconds and subsequently 3 seconds as quality and
  provider confidence deteriorate. The final poor observations retain departure
  verification instead of a confident stationary classification.
- Twelve trace-correlated imminent events reach their subsequent notification-post
  completion in 21–26 ms (median 23 ms). The reroute callback posts a warning before
  its initial turn, so pairing uses both trace identity and event order.
  Android/watch presentation is still unobserved.
- All 13 battery samples report `chargeCounterStatus=implausible_scale`; those
  counters cannot establish consumption or provider savings.
- Accepted GPS added filter displacement has median 3.0 m and maximum 20.7 m;
  fused median 2.2 m and maximum 14.4 m. These describe additional filtering
  displacement, not actual positioning error. The remaining large values occur
  without sustained corroboration; relaxing movement confidence solely to reduce
  them would risk treating source corrections or drift as travel.

The committed-format excerpt `legacy-handover-20261030.trace` retains 30 incoming
observations with relative measurement/receipt times, local metre offsets,
provider uncertainty, native speed presence and requested intervals. Its test
uses a synthetic target and checks chronological selection, duplicate rejection,
UNKNOWN verification and coarse-arrival suppression. It does not reproduce the
entire recorded route, sensor state or OS delivery path.

## Additional log findings from the follow-up review

1. **Two notification posts compete within 141 ms.** At 08:38:50.797 the
   off-route warning is posted, then at 08:38:50.938 the initial left-turn
   instruction is posted using the same notification ID, 2. The warning's
   notification entry is replaced almost immediately. This is a concrete
   notification interaction and a potential phone/watch presentation problem;
   receipt is not recorded. The shared ID and posting order predate this branch,
   so this is not established as a new regression. A notification-priority test
   should define the desired warning/ready-route handoff before changing it.

2. **Nine GPS/network handovers occur in the final minute.** Among 20 accepted
   observations from 08:45:12 through 08:46:04, the selected provider switches
   nine times. MOVING is established at 08:45:43.789, then becomes UNKNOWN at
   08:45:44.094 after a network handover. Provider-local evidence resets and rapid
   departure acquisition are intentional safeguards, but frequent changes keep
   confidence rebuilding and prevent sparse acquisition. This is a demonstrated
   efficiency/stability concern, not proof that either source is wrong. Further
   selection/hysteresis work needs a comparative replay that preserves provider
   offset and chronology protections; do not merely retain MOVING across a switch.

3. **The clocks show no measurable CPU deep sleep during the recording.** Across
   187 paired elapsed/uptime readings from 08:32:04.364 to 08:46:07.708, their
   difference is always 1,319,374–1,319,375 ms; first and last are identical.
   Because [elapsed time includes deep sleep and uptime excludes it](https://developer.android.com/reference/android/os/SystemClock),
   this indicates no added deep-sleep time at millisecond resolution during that
   interval. The acquisition suspensions and motion-trigger callback are real,
   but they do not test waking a deeply sleeping CPU. The 08:33:20 recovery has
   an explicit RECOVER_LOCATION service action; the second recovery has no such
   action and may have used the awake timer. This does not establish why the CPU
   stayed awake or attribute an energy regression to ViBRo.

4. **The seven-second reroute record includes confirmation waiting.** The
   speculative request starts at 08:38:43.820; the route is already parsed at
   08:38:44.546, then applied only after deviation confirmation at 08:38:50.888.
   Its `durationMs=7056` includes roughly 6.3 seconds holding the ready candidate.
   It is not evidence of seven seconds spent calculating in BRouter. Diagnostics
   would be clearer with separate calculation, confirmation-wait and application
   durations. The held result and single reused request support the intended
   speculative-reroute behavior.

This follow-up changed analysis documentation only. The previously verified
production code, tests, fixture and APKs are unchanged.

## Verification

Before-fix reproduction:

```powershell
.\gradlew.bat testFdroidDebugUnitTest --tests '*NavigationRoundTripDepartureQualityTest' --console=plain --max-workers=2
```

One expected regression failure. Console evidence:
`.codex-temp/reliability-roundtrip-red-20261030.txt`.

Focused post-fix verification:

```powershell
.\gradlew.bat testFdroidDebugUnitTest --tests '*RecordedLocation*Test' --tests '*NavigationRoundTrip*Test' --tests '*NavigationArrival*Test' --tests '*NavigationRouteStartApproach*Test' --tests '*NavigationSessionRouteStateTest' complexityCheck --console=plain --max-workers=2
```

BUILD SUCCESSFUL; zero PMD violations. Console evidence:
`.codex-temp/reliability-review-focused-final-pass-20261030.txt`.

Final integrated verification passed in 20m 43s: 2,054 F-Droid and 2,126 GPlay
tests (4,180 total), zero failures/errors/skips, zero PMD violations, both debug
lints and all four debug/release APK builds. Both lints retain only the existing
locale-bundle and Gradle-version advisory warnings. All four APKs carry the
explicit source label `65de832d-review-20261030`. Source/test/fixture hashes still
match; no code changed after verification. Exact command, output, artifact hashes
and remaining device work are recorded in the corresponding STATUS.md entry.

## Notification, selection and timing follow-up

Continued from `a455ce9f` on the same branch. The Round Trip correction, its tests
and the relative replay are preserved. This follow-up is uncommitted; background
exit remains excluded.

- A ready speculative reroute now applies first and posts its useful instruction
  directly. It no longer posts a competing “recalculating” warning first. When
  no instruction is available, including Single instruction mode, it retains the
  existing notice-only warning. In-flight calculations still warn immediately;
  failure does not replace that warning. Four regressions cover these paths.
- Legacy comparisons now use the accepted provider's latest active candidate
  for ties. Material improvements and the existing 8-second freshness bias and
  15-second lifetime still override preference. Chronological dispatch, cadence,
  provider retirement, filter corrections and motion resets are unchanged.
- Comparative replay through selection, dispatch, Kalman and motion reduces
  handovers from **9 to 7** and accepted updates from **22 to 16** over the whole
  30-observation excerpt (the original final-minute subset had 20 accepts).
  Maximum accepted age stays **373 ms**. Maximum rapid-acquisition dispatch gap
  increases from **5,099 to 9,790 ms**; the earlier roughly 30-second input gap is
  unchanged. The remaining switches are four material accuracy improvements and
  three freshness escapes after network silence exceeds the existing 8-second
  bias. Each actual handover still produces UNKNOWN. The old replay has
  one MOVING evaluation, the new replay none: fewer switches do **not** establish
  better motion confidence, less rapid acquisition or power savings in these
  poor observations. Three other recorded streams retain their accept counts,
  ages and handovers. Synthetic cases cover quality improvement, freshness
  boundary, source removal/reset, older timestamps and early cadence rejection.
- `NavigationRouteTiming` records calculation including retries, callback queue
  wait, ready-result confirmation wait, domain-state application and total time.
  The Android factory supplies elapsed realtime. A held result no longer logs
  its full lifetime as calculation time. Timing remains diagnostic-only; no
  timer, wake lock, subscription or route-validity rule was added for logging.
- Recovery entry points now identify `alarm`, `awake_timer` and `motion_trigger`,
  with monotonic deadline/dispatch, uptime and lateness. Regressions distinguish
  the actual timer/alarm/motion wiring without adding scheduled work.

The baseline reproduction ran eight new tests against unchanged production:
five expected failures after fixing a fixture's missing confirmation displacement.
Evidence: `.codex-temp/reliability-handoff-red-final-20261030.txt`. The first
post-change tests passed but PMD found complexity/coupling and helper issues;
these were simplified without weakening rules. Focused and expanded checks then
passed. Final full-check results and artifacts are recorded in the latest
STATUS.md entry.

Final checks passed in 10m 32s: 2,068 F-Droid and 2,140 GPlay tests (4,208 total),
zero failures/errors/skips or PMD violations, both debug lints and all four APK
builds including release shrinking. Lints retain only the two prior advisories.
All four source labels are `a455ce9f-reliability-handoff-20261030`; saved source
and preserved-file hashes still match. No source/test changes followed the check.

DEVICE_VALIDATION.md now defines a four-minute unplugged, screen-off stationary
window per provider, using retained history and the existing app events. No
phone is currently listed by ADB. No installation, settings change, history reset
or physical test was performed. CPU/GPS cessation during suspension, Android/watch
receipt and a physical provider comparison remain unresolved.

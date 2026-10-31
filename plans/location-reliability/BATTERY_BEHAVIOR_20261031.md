# Generic battery behavior detection — 31 October 2026

Update: BATTERY_WINDOW_20261031.md records the subsequent phone trial and labeled
partial-window display. The whole-trip interruption guard described here remains;
valid later epochs can now be shown with their own explicit measured coverage.

User-authorized follow-up to BATTERY_COUNTER_INVESTIGATION_20261031.md. The design
uses observed behavior rather than a Huawei/device whitelist. Application changes
are uncommitted on `codex/location-reliability`; the original investigation remains
a record of its earlier source/read-only inspection.

## Behavior

One Android battery reader supplies both trip snapshots and diagnostics. Raw
observations retain counter/current signs, percentage, power context and monotonic
time. Classifications are LEARNING, AMBIGUOUS, INFERRED or UNAVAILABLE, with a reason
and epoch. Passive inference never claims independent verification.

The pure Java detector tests micro/milli counter scales, either discharge direction,
and current scale independently. It requires four independently spaced observations
(30 seconds), a three-minute span, and three separate percentage decreases totaling
at least three points. Implied full capacity must fit broad phone/tablet bounds of
200–30,000 mAh. Available nonzero consistent-sign current must agree within a factor
of four with integrated charge change; sparse samples are deliberately imprecise.
Missing/zero current retains unknown current units. Current and counter agreeing
with each other cannot establish absolute scale without additional assumptions.

Each candidate retains its interval rate checks under that candidate's units.
Charging, unknown power, unavailable counters, chronology errors, ten-minute gaps,
counter direction reversals, rising percentage and contradictory interpretations
break the epoch. Candidate changes after inference require learning again. Raw
values remain diagnostic; the unit-contract labels do not assert vendor compliance.

Inferred trip mAh is shown with an approximation marker. The baseline and current
reading must belong to one uninterrupted discharge epoch. No partial total bridges
a reset/charging interval; once interrupted, the trip stays unavailable even if
the detector later relearns. Percentage-drop reliability remains separate. Learning
is in memory only; a fresh reader/process has no retained firmware assumptions.
The explicit normalized-snapshot API retains its existing legacy plausibility rules.

## Limits

This is inference under stated bounds, not a fuel-gauge calibration or universal
unit discovery. Exotic batteries outside those bounds remain unsupported. Relative
and remaining-charge counters with the same observed direction need not be
distinguishable, and absolute-value counters can hide a sign crossing until their
direction changes. Small gauge corrections/resets that mimic plausible discharge
cannot always be detected. Estimates remain device-wide and cannot attribute
energy to ViBRo or a location provider.

No new permissions, runtime dependencies, alarms, wake locks or periodic tasks are
introduced. Existing battery broadcasts, cache-expiry reads and the throttled
diagnostic callback provide observations. Logging-disabled navigation still learns
from battery broadcasts. Cached UI/compass reads add no evidence.

## Verification and device follow-up

Synthetic tests cover both directions/scales, signed counters, reversed current
sign, missing current, ambiguous scales, clustered/duplicate observations, charging,
unknown power, gaps, resets, and permanently interrupted trip accounting. Two
battery-only replays preserve the 27/30 October observations without coordinates
or device identifiers. Adapter tests check shared diagnostic/cache interpretation
on supported Android SDKs. Final automated results will be recorded below.

Physical checks remain necessary: normal discharge until multiple percentage
changes, charging/replugging, a long idle interval with gauge recalibration, logging
off, and a fresh process. Compare inferred normalization with raw system readings
on multiple devices. Do not infer battery savings or calibrated accuracy from the
replays. No installation or settings change is implied by this implementation.

## Final verification

The full integrated command passed in **10m 40s**:

```powershell
.\gradlew.bat testFdroidDebugUnitTest testGplayDebugUnitTest complexityCheck lintFdroidDebug lintGplayDebug assembleFdroidDebug assembleGplayDebug assembleFdroidRelease assembleGplayRelease -PsourceRevision=fbd71bb4-battery-behavior-20261031 --console=plain --max-workers=2
```

- 2,119 F-Droid and 2,191 GPlay tests: **4,310 total**, no failures, errors or skips.
  This adds 29 executed cases per flavor, including the three-SDK adapter cases.
- Zero PMD violations. Both debug lints retain only the existing
  `AppBundleLocaleChanges` and `AndroidGradlePluginVersion` warnings. The existing
  Gradle deprecation remains; the toolchain was not changed.
- All four APK builds, including both release R8/vital-lint paths, passed. Each
  BuildConfig contains the explicit pre-commit revision above; it identifies the
  uncommitted checkout rather than claiming an exact committed revision.
- All **1,382 saved verification input hashes** match. No production, test,
  resource, configuration, specification or agent-guidance changes followed the
  final run. Only this report and the status handoff were completed afterward.
- Exact console output, test/lint/PMD results, four artifact hashes and input
  evidence are local in `.codex-temp/battery-behavior-full.log`,
  `battery-behavior-verification.json`, `battery-behavior-verification-inputs.json`
  and `battery-behavior-input-check.json`.

Focused runs exposed two maintainability limits, outdated formatter fixtures and
the synthetic broadcast sender's missing compatibility permission on older test
SDKs. Those were corrected without changing receiver permissions in the app or
suppressing checks. Final focused verification passed 52 cases and PMD before the
full run. The two recorded replays resolve the expected increasing mAh counter and
mA current; their arithmetic totals remain inference examples, not calibrated
power measurements.

ADB lists no connected phone at completion. No installation, physical trial,
settings change, history reset, commit or publication occurred. Device calibration,
long idle/reset behavior and trials on additional firmware remain NEEDS_DEVICE.

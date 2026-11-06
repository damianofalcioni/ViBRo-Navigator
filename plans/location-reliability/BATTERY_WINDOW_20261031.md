# Partial battery measurement window — 31 October 2026

Follow-up to the user-reported missing mAh after an unplugged trial with a five-point
battery drop. Inspected installed build `c4a87670`; source remains on
`codex/location-reliability`. The phone's log was available directly on its selected
SD card; no additional export was necessary.

## Evidence and cause

The active-navigation log `vibro-navigator-log-20261031121958.txt` contains seven
discharging battery records between 12:20:02 and 12:28:07 Europe/Vienna. They show
raw counter values 6, 0, 11, 21, 30, 39 and 49, with negative current throughout.
Logged percentages run 87 to 83; the subsequent system read after USB reconnection
reports 82. The user's five-point drop is consistent with those observations.
The captured log SHA-256 is
`5619022a37b78108bb68fdcac31dc9dd8ecbcea5f2e782760a9c2f7d3cab3091`.

The first decline toward zero establishes a decreasing direction. The subsequent
rise breaks that epoch. This is consistent with crossing zero in the absolute-value
Huawei coulomb accumulator investigated earlier; the available unsigned readings
alone cannot prove the exact gauge event. No special Huawei interpretation is
needed for the correction below.

At 12:28:07 the detector successfully reports INFERRED: counter multiplier 1000,
increasing discharge direction, current multiplier 1000 and negative discharge
current sign. The old trip-accounting guard has already permanently invalidated
the whole-trip mAh baseline because of the earlier reversal. Consequently, the
display remains `--` despite valid later measurements. Detection worked; the
display discarded the useful later window without explaining the missing coverage.

## Correction

Preserve the unavailable whole-trip total. Track the first actually observed
snapshot in the latest uninterrupted epoch separately. Once that epoch is inferred,
publish a `NavBatteryEstimate` with partial scope and original covered start/end.
The shared phone/Auto formatter labels it partial and shows its measured duration.
The label is localized in all 18 supported languages. Earlier valid windows and
unobserved prefixes are never added to it. Charging, unavailable readings or a new
epoch hide the partial estimate until a supported window is available again.
Cached reads cannot extend its duration. Whole-trip percentage reliability stays
independent and the legacy full-trip scalar remains unavailable after interruption.

The new battery-only replay reproduces the logged direction change and successful
inference. Its first post-reversal sample is 11 and last sample 49, so its supported
window displays `≈38.0 mAh (partial, over 5 min 25 s)`. Battery broadcasts between
these log samples may place the live reader's epoch boundary earlier; the replay
does not reconstruct that missing evidence or claim an exact live partial amount.
These remain vendor-derived device-wide estimates, not calibrated consumption.

## Verification

The regression fails against unchanged production at its assertion requiring a
partial value in the formatted UI; inference and unavailable whole-trip assertions
already pass. Evidence: `.codex-temp/battery-window-red.log`.

After the correction, focused battery, trip-stat and formatter tests plus PMD pass
in `.codex-temp/battery-window-focused.log`. Coverage includes multiple resets,
charging, missing counters and cached-read coverage. The inference thresholds,
provider policies, recovery work, subscriptions and permissions are unchanged.
Full verification results and installation status will be appended below.

### Independent branch verification — 6 November 2026

The subsequent merge-readiness audit at `2ce19214` includes this partial-window
implementation and its recorded regression. After repairing a separate routed
guidance freshness gap, the full branch passed 4,330 tests, PMD, both debug lints
and all four debug/release APK builds. See AUDIT_20261106.md and the latest
STATUS.md handoff for the exact command, source label and artifact hashes. No
phone was connected for that audit; it supplies no new installation or physical
battery-validation result, and does not reconstruct the missing 31 October
handoff.

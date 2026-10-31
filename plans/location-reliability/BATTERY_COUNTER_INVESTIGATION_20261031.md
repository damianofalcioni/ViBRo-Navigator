# Huawei battery counter investigation — 31 October 2026

Inspected branch `codex/location-reliability` at `fbd71bb4`. This investigation
changes no application code or phone settings. Battery-only captures and downloaded
driver sources are local under `.codex-temp/battery-counter-investigation-20261031/`.

## Conclusion

The small counters are reproducible in Android's system services independently
of ViBRo. Public source for this Huawei platform strongly explains them: its
`charge_counter` exports the absolute value of a net coulomb accumulator in **mAh**,
where Android's app contract expects battery charge in **microamp-hours**. The
accumulator also has different semantics from remaining battery charge: discharge
adds to it, charging subtracts, and gauge maintenance can reset/rebase it. Its
absolute-value export loses the sign.

This explains the historical upward counter during discharge and the downward
counter observed during USB charging. The current path likewise exports **mA**
without conversion to Android's expected **microamps**. A reading logged as
`currentNowUa=-801` therefore strongly appears to represent about -801 mA on this
firmware, rather than -0.801 mA.

Confidence is high from the matching hardware, kernel version, live behavior and
historical numerical checks. The downloaded source is a public mirror of the
platform source, not a verified binary/source match for the installed firmware;
that remaining distinction prevents claiming exact firmware provenance or
calibrated measurement accuracy.

## Live read-only evidence

- Huawei PRA-LX1, Android 8.0/API 26, firmware `8.0.0.407(C432)`, kernel `4.4.23+`.
- The device exposes the `hi6555v100_coul` and `hisi_coul_core` drivers. The inspected
  source tree reports kernel 4.4.23 and includes that coulomb-counter implementation.
- At 11:07:27 Europe/Vienna, `dumpsys battery` reports 83%, USB charging, and charge
  counter 237. At 11:09:12, `dumpsys batteryproperties` reports the same percentage
  and charging state, counter 230, and current 239. The seven-unit decline over
  about 105 seconds corresponds to approximately 240 mA if the units are mAh,
  consistent with the observed charging current scale. The two services are read
  at different times and need not return identical snapshots.
- Subsequent reads continue the downward trend. BatteryProperties also reports
  full charge 1398 and cycle count 951; these vendor values were not independently
  calibrated and are not used here to diagnose battery health.
- Direct sysfs attribute reads are permission denied. No root, permission changes,
  battery simulation/reset, installation, or charging-setting changes were attempted.

## Source path

Source snapshot: `hisi-oss/android_kernel_huawei_hi6250-8`, commit
`65e545cbaa11d132e243287b150146bc2a51c45e`.

1. [Battery property export](https://github.com/hisi-oss/android_kernel_huawei_hi6250-8/blob/65e545cbaa11d132e243287b150146bc2a51c45e/drivers/power/hisi/hisi_bci_battery.c#L1532):
   the charge-counter property takes the absolute value of `hisi_battery_cc()`.
2. [Coulomb core](https://github.com/hisi-oss/android_kernel_huawei_hi6250-8/blob/65e545cbaa11d132e243287b150146bc2a51c45e/drivers/power/hisi/coul/hisi_coul_core.c#L2299):
   `coul_get_battery_cc()` divides the microamp-hour accumulator by 1000.
3. [Chip accumulator](https://github.com/hisi-oss/android_kernel_huawei_hi6250-8/blob/65e545cbaa11d132e243287b150146bc2a51c45e/drivers/power/hisi/coul/hi6555v100/hisi_hi6555v100_coul.c#L330):
   the underlying value is outgoing charge minus incoming charge, with offset
   correction. It is not the remaining-charge calculation.
4. [Current export](https://github.com/hisi-oss/android_kernel_huawei_hi6250-8/blob/65e545cbaa11d132e243287b150146bc2a51c45e/drivers/power/hisi/hisi_bci_battery.c#L1420)
   uses the FIFO-average current. Its
   [core implementation](https://github.com/hisi-oss/android_kernel_huawei_hi6250-8/blob/65e545cbaa11d132e243287b150146bc2a51c45e/drivers/power/hisi/coul/hisi_coul_core.c#L2751)
   divides microamps by 1000 before returning.
5. [Gauge maintenance](https://github.com/hisi-oss/android_kernel_huawei_hi6250-8/blob/65e545cbaa11d132e243287b150146bc2a51c45e/drivers/power/hisi/coul/hisi_coul_core.c#L8334)
   includes clearing/rebasing the accumulator when updating open-circuit-voltage
   evidence. A lifetime total must not assume an uninterrupted counter epoch.

The [Android BatteryManager contract](https://developer.android.com/reference/android/os/BatteryManager)
specifies microamp-hours for charge and microamps for current. ViBRo reads the
appropriate property identifiers directly, without scaling the returned integers.
Its diagnostic unit labels follow this contract; they do not establish the vendor's
actual units.

## Historical consistency check

Extracted only battery records from seven existing local recordings: 101 unplugged,
discharging samples. No travel coordinates were copied. Integrated the sparse raw
current samples with a trapezoidal approximation, interpreting raw current as mA.

| Recording | Duration | Raw counter increase | Approximate integral if current is mA |
|---|---:|---:|---:|
| 23 October, 08:23 log | 21.01 min | 252 | 250.27 mAh |
| 27 October, 08:26 log | 23.38 min | 218 | 203.25 mAh |
| 30 October, 08:31 log | 13.14 min | 94 | 91.49 mAh |

The scale and discharge direction fit the driver explanation. Sparse snapshots
miss changing loads, so these comparisons establish consistency, not calibrated
energy consumption or an app/provider ranking. Treating the raw current as
microamps instead gives integrals 1000 times smaller than the mAh interpretation.
Machine-readable results for all seven logs are saved locally as
`historical-battery-summary.json` in the investigation directory.

All 101 average-current readings are zero. The inspected battery property list
does not advertise a separate average-current property, and the live service also
reports zero for its second current entry. Unsupported/unpopulated reporting is a
plausible explanation; zero must not be interpreted as proof of zero consumption.

## Implications and follow-up

- Keep rejecting these values for the generic remaining-charge calculation.
  Multiplying by 1000 alone would fix the scale but preserve the wrong semantics.
- Preserve raw readings for diagnostics and distinguish Android-contract units
  from independently established device units in any future logging change.
- A future compatibility adapter would need an explicit firmware/device match,
  discharge-only windows, correct counter direction, and reset/rebase/sign-crossing
  handling. The absolute-value export makes generic reconstruction unsafe.
- Battery percentage remains a separate signal, with its already documented
  plateaus and gauge behavior. These findings establish no battery defect,
  provider savings, or app-specific energy attribution.
- No production or test changes were made, so Android verification was not rerun.
  Existing branch validation remains applicable to the unchanged code.

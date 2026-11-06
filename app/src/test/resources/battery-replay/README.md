# Battery behavior replays

Battery-only observations from the 27 and 30 October 2026 Huawei PRA-LX1 logs.
Rows contain relative monotonic receipt time, raw charge-counter value, raw current
and battery percentage. They contain no coordinates, device identifiers or routes.

The old log field names assert Android's expected units; the integers here remain
unscaled. Platform-source analysis and numerical consistency support an mAh
accumulator and mA current on this firmware. Replay checks dynamic interpretation,
not independently calibrated consumption, battery health or app/provider savings.

`huawei-counter-fold-20261031.csv` contains the seven discharge log samples from
the installed `c4a87670` build, with relative sample times. The raw counter descends
6 to 0, then rises to 49 while the reported level falls 87 to 83. The last sample
has an inferred mAh/mA interpretation. The session replay preserves the unavailable
whole-trip total and checks a labeled 38 mAh partial estimate over the captured
post-reversal window (11 to 49, 325,219 ms). Battery broadcasts between logged
samples are not reconstructed; this window is not asserted to be the exact live
reader's epoch boundary or an independently calibrated energy measurement.

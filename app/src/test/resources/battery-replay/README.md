# Battery behavior replays

Battery-only observations from the 27 and 30 October 2026 Huawei PRA-LX1 logs
investigated in `plans/location-reliability/BATTERY_COUNTER_INVESTIGATION_20261031.md`.
Rows contain relative monotonic receipt time, raw charge-counter value, raw current
and battery percentage. They contain no coordinates, device identifiers or routes.

The old log field names assert Android's expected units; the integers here remain
unscaled. Platform-source analysis and numerical consistency support an mAh
accumulator and mA current on this firmware. Replay checks dynamic interpretation,
not independently calibrated consumption, battery health or app/provider savings.

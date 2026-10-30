# Location replay fixtures

These excerpts come from the 23 and 27 October 2026 recordings described in
`plans/location-reliability/ASSESSMENT.md`. They contain relative monotonic times
and local metre offsets, with each excerpt's first observation as the origin.
They contain no absolute geographical coordinates. The fixture loader maps the
offsets onto a local equatorial coordinate system for the existing Java policies.

Each non-comment row contains:

`measurement_ms receipt_ms provider north_m east_m accuracy_m speed_mps speed_accuracy interval_ms`

`NaN` means absent speed or speed-accuracy metadata. Measurement spacing,
delivery age, provider transitions and duplicates are retained. The recorded
requested interval is metadata, not a promise of actual callback spacing.

- `fused-departure`: the older fused segment with prolonged stationary output.
- `gnss-departure`: the newer GPS/network segment with prolonged stationary output.
- `gnss-poor-approach`: the newer poor-quality approach around the reported incident.

Use `LocationLogReplay.load(name)` from JVM tests. These are component-policy replays of observed inputs; they do not reconstruct every original Android callback, selection decision, route or sensor event. Recorded positions and speeds
are observations, not surveyed truth. Motion/filter accuracy assertions use
separate synthetic inputs with known movement or drift. The replay regressions
check state consistency and acquisition safeguards; they cannot measure hardware
power use, actual positioning error, Android wake latency or watch delivery.

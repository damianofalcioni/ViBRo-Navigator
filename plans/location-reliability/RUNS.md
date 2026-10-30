# Bounded sequential run packets

> Execution update, 27 October 2026: the user later authorized all software stages in one sustained implementation run. The original sequential prompts below remain available for future continuation; current completion and verification evidence live in STATUS.md. Physical-device trials remain separate.

Every run inherits CONTEXT.md, the repository guidance and STATUS.md. Read the referenced item(s) in REFERENCE_PLAN.md for rationale. Each run covers only its packet. Later discoveries go into the handoff unless necessary to make the selected change correct and reviewable. If it cannot fit coherently, record PARTIAL and leave a precise continuation.

## Run 01 — Minimal replay foundation

Reference: plan item 1. Prerequisites: none.

Create a small reusable JVM replay fixture format with relative monotonic time, local position offsets and necessary source/quality metadata. Add only the key stationary/moving and poor-accuracy approach excerpts from both logs, plus independent synthetic walking and stationary-drift examples. Preserve timing, handovers and important duplicates. Add minimal replay hooks using existing location/session contracts; extend incrementally in later runs rather than build a general simulation framework.

Exclude: production policy changes, broad telemetry changes and power claims.

Done: fixtures are independent of personal Downloads paths and absolute locations; replay is deterministic; current behavior and synthetic ground truth are clearly distinguished; focused tests pass. Leave a compact fixture/helper guide for later runs.

## Run 02 — Event and timing diagnostics

Reference: items 1, 9, 10. Prerequisites: run 01 code ready.

Add build revision and correlation through measurement receipt, acceptance, maneuver decision and notification posting. Include decision/suppression reasons, requested cadence, quality/lag, screen transitions and requested-versus-actual recovery time where useful. Put elapsed-realtime/uptime reads behind appropriate adapters. Reuse the existing logger and throttle unchanged records.

Exclude: navigation policy, alarm API changes, wake locks and new logging timers.

Done: a sample event can be traced through the stages; log labels distinguish posting from actual presentation; diagnostics add no periodic wake-ups; focused logger/adapter tests pass.

## Run 03 — Motion assessment core

Reference: item 2. Prerequisites: 01–02 code ready.

Implement MOVING/STATIONARY/UNKNOWN evidence with freshness in the motion domain. Expire unsupported stop confidence; accumulate independent, provider-local directional evidence when native speed accuracy is missing. Keep compatibility entry points for consumers until run 04 so this run remains buildable. Define conservative compatibility semantics explicitly.

Exclude: broad compass/UI/trip-stat migration and Kalman retuning.

Done: independently defined motion/drift cases pass; unknown is not a confirmed stop; handover jumps and clustered samples do not create movement; recorded ambiguous segments can leave stale confident stationarity. Document interfaces and the limited consumer work deferred to 04.

## Run 04 — Motion-state consumers

Reference: item 2. Prerequisite: 03 code ready.

Wire the new motion assessment into orientation notices, departure requests, stationary-sleep eligibility, display speed/heading/zoom, ETA inputs and trip accounting. Change only motion-state handling. UNKNOWN retains active verification and suppresses confident stationary advice; preserve intentional sleep-time bookkeeping. Keep phone/Auto behavior consistent and update the product contract.

Exclude: filter tuning, ETA history length, new acquisition formulas and new arrival confirmation policy.

Done: all stationary boolean call sites are audited; recorded problematic notices have focused regression coverage; true stationary behavior and lifecycle cancellation remain correct; no invented motion/stop totals are introduced. Document any compatibility accessors retained.

## Run 05 — Kalman lag and uncertainty

Reference: item 3. Prerequisites: 03–04 code ready.

Separate observation uncertainty, output uncertainty and additional filtering displacement. Remove the confidence feedback that makes filter lag the sole determinant of raw movement evidence. Adapt responsiveness and bound added lag using corroborated motion, including the 12/15/20 s cadence boundary. Preserve conservative displayed uncertainty and trusted-course prediction requirements.

Exclude: manufactured velocity, generic estimator replacement, route ETA/cadence policy changes.

Done: synthetic ground-truth lag improves within justified bounds; stationary drift, turns, jumps, sparse fixes and delayed-network corrections retain coverage; current log excerpts replay without new motion artifacts. Report measured lag changes rather than asserted accuracy improvement.

## Run 06 — Cadence-aware ETA history

Reference: item 4. Prerequisite: 05 code ready.

Separate short direction/deviation evidence from bounded, cadence-aware live ETA history. Support trustworthy progress at normal 12–60 s intervals. Invalidate stale speed/acceleration after stops, reversals, provider/route transitions, sleep and unexpected gaps. Preserve route-model fallback and UNKNOWN semantics.

Exclude: changing maneuver alert thresholds, acquisition buckets or reroute confirmation windows.

Done: normal sparse movement retains supported ETA evidence; stale speed and source offsets cannot leak into estimates; short direction checks retain their current safeguards.

## Run 07 — Uncertainty-aware acquisition

Reference: item 5. Prerequisites: 04–06 code ready.

Cap the existing dynamic interval using remaining maneuver distance, fix age, quality, filter lag, motion/route confidence and conservative speed. Request verification before the plausible arrival margin becomes too small; restore long intervals through stable evidence and the existing ramp. Derive/tune constants with replay and synthetic faster-approach cases.

Exclude: timer-only maneuver alerts, permanent rapid acquisition, network-quality backoff and fixed 1 s behavior changes.

Done: the recorded poor approach enters verification sooner; fast synthetic approaches retain alert opportunities; long reliable legs stay sparse; quality fluctuations do not cause request churn. Update SPECIFICATION.md for the new scheduler contract.

## Run 08 — Confirmed arrival

Reference: item 6. Prerequisite: 07 code ready.

Decouple definitive arrival from the expanding off-track radius. Add bounded quality/radius and independent approach-confirmation evidence, retaining approach guidance when confidence is insufficient. Cover intermediate/final targets, Straight Line, Round Trip and final beelines. Explicitly distinguish a snapped road endpoint from the requested destination. Update radius overlays/Auto and localize text only where behavior requires it.

Exclude: unrelated reroute changes and arbitrary timeout-based arrival.

Done: a single coarse recorded correction cannot declare arrival; accurate approaches confirm promptly; nearby roads, clustered callbacks, handovers and loop starts cannot complete an incorrect target. Document the deliberate change from the old specification.

## Run 09 — Independent network backoff

Reference: item 7. Prerequisite: 08 code ready.

Track network quality separately from raw GNSS health. Back off repeated coarse/unhelpful positions conservatively, retain probes and restore useful network recovery promptly. Use motion context so real stationary repeats remain valid. Preserve independent GNSS silence recovery, in-place subscription reconciliation and fused-primary exclusivity.

Exclude: disabling network indefinitely, continuous fused/legacy overlap and changes to chronological dispatch gates.

Done: recorded coarse-repeat request exposure decreases without losing recovery cases; GPS-unavailable/network-only and pause/resume remain responsive; no request oscillation or starvation. Update the explicit network-restoration specification.

## Run 10 — Provider selection verification

Reference: item 8. Prerequisite: 09 code ready.

Replay source disagreement, handovers, duplicates, retained candidates, delayed corrections and cross-provider interval gates. Inspect useful chronological improvements potentially blocked inside the requested interval. Fix only demonstrated policy defects; a supported no-change result is a valid completion.

Exclude: blind coordinate averaging, double-counting fused/GPS observations and publishing old position corrections as new movement.

Done: offset jumps create no speed/progress; retired callbacks are ignored; deduplication and correction apply-once behavior hold; any selection change has a concrete regression case. Record which suspected problems proved to be expected behavior.

## Run 11 — Notification and recovery diagnosis

Reference: items 9–10. Prerequisites: diagnostics from 02 and location changes through 10 code ready.

Review new traces/replays and scheduler paths to distinguish late decisions, late posting and late Android/watch receipt. Verify alarm rescheduling, missed-motion/failed-registration behavior, callback generations and lifecycle cancellation. Make only evidence-supported code fixes. Retain inexact-alarm limitations explicitly and active acquisition when sleep prerequisites fail.

Exclude: asserting the 08:47 root cause without evidence, permanent wake locks, automatic exact-alarm permission changes or invented physical validation.

Done: local logic checks pass and remaining real-device questions have a precise reproduction/capture procedure. If hardware evidence is unavailable, mark it NEEDS_DEVICE and carry it to 14; do not block unrelated run 12 work.

## Run 12 — Battery telemetry validation

Reference: item 11. Prerequisite: prior code handoffs reviewed; unresolved hardware work from 11 is nonblocking.

Extend the existing battery-value validation and accounting. Separate raw vendor readings from usable consumption data; record validity/source/age. Handle implausible counter scale/direction, charging, gauge resets and stale snapshots without guessed unit conversions. A normal percentage plateau is not itself an invalid reading. Sample only on existing events.

Exclude: unsupported battery-saving claims and a new periodic telemetry service.

Done: supplied implausible counter sequences cannot yield plausible mAh totals; valid counters still work; charging/reset/unsupported cases and trip display have focused coverage.

## Run 13 — Cross-feature integration and automated verification

Reference: plan delivery checks. Prerequisites: 01–12 code ready or an explicitly supported no-change result; hardware-only questions remain listed.

Review the accumulated diff and integrate interactions among motion, filtering, ETA, scheduler, source backoff, arrival and recovery. Run cross-feature replays and appropriate explicit flavor tests, complexity, lint and builds required by AGENT.md/SPECIFICATION.md. Check phone/Auto and documentation/localization consistency. Repair regressions caused by this series; record unrelated failures with evidence.

Exclude: reopening the roadmap for unrelated improvements or claiming physical-device success from local tests.

Done: exact command/results and the buildable revision/working-tree state are recorded; intended behavior is documented; unresolved device experiments are isolated for 14. If the integrated scope reveals substantial new work, record a bounded continuation rather than declare verification complete.

## Run 14 — Physical-device comparison

Reference: item 12 and remaining hardware checks from 11/13. Prerequisite: integrated build from 13; device and user participation as needed.

Prepare/use the verified build, then compare screen-off provider modes in repeated same-route trials with alternating order and controlled settings. Capture turn crossings independently, phone/watch receipt, motion wake latency, quality, cadence and available validated power/hardware activity. Keep simultaneous-source diagnostic agreement runs separate from power attribution. Follow repository installation guidance and current user authorization.

Exclude: fabricated field results or committing personal traces to test resources.

Done: report measured tradeoffs/variability and unresolved limitations. If physical trials cannot be performed in the chat, deliver the ready-to-use protocol and mark NEEDS_DEVICE; do not claim energy or notification delivery is verified.

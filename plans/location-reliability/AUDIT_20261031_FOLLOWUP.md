# Location-reliability follow-up audit — 31 October 2026

Reviewed `codex/location-reliability` at `2c14b027` against `main` (`0c463631`),
REFERENCE_PLAN.md and RUNS.md. Background exit and its pre-existing untracked
note remain excluded. The user authorized repairs and a final simple commit.

## Findings and repairs

**Delivery delay could manufacture stationary confirmation time.** Sleep required
two seconds between callback receipts, but only increasing measurement timestamps.
Three measurements one millisecond apart, delivered two seconds apart, could
complete a fallback check. Increasing delivery age could also stretch a shorter
measurement sequence into the full twenty-second stop window. All observations
in these synthetic regressions satisfy the existing five-second freshness gate.

Sleep now requires two-second spacing in both measurement and receipt time. Both
the twenty-second initial window and four-second fallback window must elapse in
both clocks. Intentional sleep clears the new window anchor before verification;
it cannot supply missing observation time. Normal independently timed callbacks,
including the recorded sparse/speedless cases, retain their existing policy.

**A clustered relocation could leave stop evidence intact.** A fresh eligible fix
outside the fixed stop origin was ignored when received inside the sample-spacing
window. Returning to the old origin could then complete initial or abbreviated
fallback confirmation using the earlier evidence. Origin contradictions now reset
the full window, trigger and learned cadence before independent-sample admission,
as provider changes already did.

Four of five new JVM regressions fail against unchanged production; the fifth
protects the existing receipt-spacing gate. A service regression covers legacy and
fused subscription suspension, continued recovery scheduling and subsequent valid
re-suspension. The specification, agent guidance and physical protocol describe
the strengthened evidence requirements. No Android API, alarm, permission,
dependency, wake-lock or background-exit behavior changes.

## Plan assessment and limits

The software stages are substantially implemented. Reviewed raw/provider-local
motion and freshness consumers, corroborated Kalman lag, separate sparse ETA
history and reversal handling, quality-capped acquisition/recovery, ordered
arrival and Round Trip gates, source selection/retirement, network backoff,
notification/recovery diagnostics and battery plausibility/accounting. Existing
regressions protect these paths; no further demonstrated defect justified changing
their policies in this pass. The two remaining sleep defects above were repaired.

Physical accuracy, OEM idle/departure latency, actual CPU/GPS cessation, Android/
watch receipt and repeated provider-energy comparisons remain **NEEDS_DEVICE**.
These are necessary to establish the plan's real-world effectiveness. ADB lists
no connected phone; no installation or physical trial occurred.

## Verification

Before-fix evidence: `.codex-temp/sleep-review-red-20261031.txt` (five tests,
four expected failures). Focused sleep, cadence, recorded-location and recovery
tests passed after repair. The first focused PMD run identified an unused helper
parameter, which was removed; the subsequent focused tests and PMD passed.

Full integrated verification passed in **7m 43s**: **2,090 F-Droid tests and
2,162 GPlay tests (4,252 total)**, zero failures/errors/skips, zero PMD violations,
both debug lints and all four debug/release APK builds including R8. Only the
pre-existing locale/Gradle-version lint advisories and plugin-configuration
deprecation remain. Language splitting stays disabled for offline language
selection; this review does not change the toolchain.

All **1,370 saved input hashes** match, including the excluded background-exit
note. All four build configurations carry `2c14b027-sleep-review-20261031`.
Exact command and artifact hashes are in the latest STATUS.md handoff; local
machine-readable evidence is `.codex-temp/sleep-review-verification-20261031.json`.

package vibro.navigator.nav.guidance;

import vibro.navigator.nav.location.NavigationLocation;

/** Verify surprising off-route reacquisition before it can advance guidance or travel history. */
final class OffTrackPositionVerification {
    private static final long SPARSE_GAP_MS = 15_000L;
    private static final long MAX_VERIFICATION_GAP_MS = 10_000L;
    private static final long MIN_STABLE_SPAN_MS = 4_000L;
    private long lastMeasurementMs = -1L;
    private long stableSinceMs;
    private int stableSamples;
    private double verificationDistance;
    private boolean verifying;

    void reset() {
        lastMeasurementMs = -1L;
        verifying = false;
        stableSamples = 0;
    }

    boolean shouldHold(NavigationLocation location, double distance, double accuracy, long nowMs) {
        long time = location.getElapsedRealtimeOrTimeMs();
        if (time <= lastMeasurementMs) {
            return verifying;
        }
        long gap = lastMeasurementMs < 0 ? 0 : time - lastMeasurementMs;
        boolean outside = distance > RouteDeviationPolicy.resolveOffTrackThresholdMeters(accuracy);
        updateVerification(gap, outside, distance, nowMs);
        lastMeasurementMs = time;
        if (!verifying) {
            return false;
        }
        stableSamples++;
        boolean settled = stableSamples >= 3 && nowMs - stableSinceMs >= MIN_STABLE_SPAN_MS;
        if (!outside || settled) {
            verifying = false;
        }
        // The recovery observation itself never supplies arrival/departure evidence.
        return true;
    }

    private void updateVerification(long gap, boolean outside, double distance, long nowMs) {
        boolean returning = verifying && distance + 3 < verificationDistance;
        boolean start = gap >= SPARSE_GAP_MS && outside;
        if (start || returning || (verifying && gap > MAX_VERIFICATION_GAP_MS)) {
            verifying = true;
            stableSinceMs = nowMs;
            stableSamples = 0;
            verificationDistance = distance;
        }
    }
}

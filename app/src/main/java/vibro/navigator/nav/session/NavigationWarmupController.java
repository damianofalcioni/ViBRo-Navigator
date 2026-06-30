package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocationController;

public final class NavigationWarmupController {

    private static final long MAX_FAST_POLLING_MS = 60_000L;
    private static final long LONG_LOCATION_UPDATE_GAP_MS = 15_000L;
    private static final long EXPECTED_UPDATE_INTERVAL_GRACE_MS = 10_000L;
    private static final long STABLE_SAMPLE_SPACING_MS = 3_000L;
    private static final int STABLE_ON_ROUTE_UPDATES_TO_EXIT = 5;
    private static final float STABLE_ACCURACY_METERS = 25f;

    private long fastChecksUntilMs;
    private long lastEvaluationMs;
    private long lastCountedStableUpdateMs;
    private int stableOnRouteUpdateCount;

    public void reset(long nowMs) {
        fastChecksUntilMs = nowMs + MAX_FAST_POLLING_MS;
        lastEvaluationMs = 0L;
        lastCountedStableUpdateMs = 0L;
        stableOnRouteUpdateCount = 0;
    }

    public void onRouteApplied(long nowMs) {
        fastChecksUntilMs = nowMs + MAX_FAST_POLLING_MS;
        lastCountedStableUpdateMs = 0L;
        stableOnRouteUpdateCount = 0;
    }

    public long getFastChecksUntilMs() {
        return fastChecksUntilMs;
    }

    public long fastChecksUntilMsForEvaluation(long nowMs) {
        return fastChecksUntilMsForEvaluation(nowMs, NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS);
    }

    public long fastChecksUntilMsForEvaluation(long nowMs, long expectedUpdateIntervalMs) {
        if (shouldResumeFastPollingAfterGap(nowMs, expectedUpdateIntervalMs)) {
            fastChecksUntilMs = nowMs + MAX_FAST_POLLING_MS;
            stableOnRouteUpdateCount = 0;
        }
        return fastChecksUntilMs;
    }

    public void recordEvaluation(boolean stableOnRoute, float accuracyMeters, long nowMs) {
        lastEvaluationMs = nowMs;
        if (fastChecksUntilMs <= 0L || nowMs > fastChecksUntilMs) {
            return;
        }
        if (!stableOnRoute || !isStableAccuracy(accuracyMeters)) {
            stableOnRouteUpdateCount = 0;
            lastCountedStableUpdateMs = 0L;
            return;
        }
        if (isTooSoonAfterCountedStableUpdate(nowMs)) {
            return;
        }
        stableOnRouteUpdateCount++;
        lastCountedStableUpdateMs = nowMs;
        if (stableOnRouteUpdateCount >= STABLE_ON_ROUTE_UPDATES_TO_EXIT) {
            fastChecksUntilMs = nowMs - 1L;
        }
    }

    public static boolean isStableAccuracy(float accuracyMeters) {
        return Float.isFinite(accuracyMeters)
                && accuracyMeters > 0f
                && accuracyMeters <= STABLE_ACCURACY_METERS;
    }

    private boolean shouldResumeFastPollingAfterGap(long nowMs, long expectedUpdateIntervalMs) {
        if (lastEvaluationMs <= 0L || nowMs <= fastChecksUntilMs) {
            return false;
        }
        long gapMs = nowMs - lastEvaluationMs;
        long expectedGapMs = Math.max(0L, expectedUpdateIntervalMs) + EXPECTED_UPDATE_INTERVAL_GRACE_MS;
        return gapMs >= LONG_LOCATION_UPDATE_GAP_MS && gapMs > expectedGapMs;
    }

    private boolean isTooSoonAfterCountedStableUpdate(long nowMs) {
        return lastCountedStableUpdateMs > 0L
                && nowMs - lastCountedStableUpdateMs < STABLE_SAMPLE_SPACING_MS;
    }
}

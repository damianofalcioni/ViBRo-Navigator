package vibro.navigator.nav;

final class NavigationWarmupController {

    private static final long MAX_FAST_POLLING_MS = 60_000L;
    private static final int STABLE_ON_ROUTE_UPDATES_TO_EXIT = 5;
    private static final float STABLE_ACCURACY_METERS = 25f;

    private long fastChecksUntilMs;
    private int stableOnRouteUpdateCount;

    void reset(long nowMs) {
        fastChecksUntilMs = nowMs + MAX_FAST_POLLING_MS;
        stableOnRouteUpdateCount = 0;
    }

    void onRouteApplied() {
        stableOnRouteUpdateCount = 0;
    }

    long getFastChecksUntilMs() {
        return fastChecksUntilMs;
    }

    void recordEvaluation(boolean stableOnRoute, float accuracyMeters, long nowMs) {
        if (fastChecksUntilMs <= 0L || nowMs > fastChecksUntilMs) {
            return;
        }
        if (!stableOnRoute || !isStableAccuracy(accuracyMeters)) {
            stableOnRouteUpdateCount = 0;
            return;
        }
        stableOnRouteUpdateCount++;
        if (stableOnRouteUpdateCount >= STABLE_ON_ROUTE_UPDATES_TO_EXIT) {
            fastChecksUntilMs = nowMs - 1L;
        }
    }

    static boolean isStableAccuracy(float accuracyMeters) {
        return Float.isFinite(accuracyMeters)
                && accuracyMeters > 0f
                && accuracyMeters <= STABLE_ACCURACY_METERS;
    }
}

package vibro.navigator.nav.session;

final class NavigationLocationReacquisitionTracker {
    private static final long LONG_LOCATION_UPDATE_GAP_MS = 15_000L;
    private static final long EXPECTED_UPDATE_INTERVAL_GRACE_MS = 10_000L;

    private long lastAcceptedUpdateMs;

    void reset() {
        lastAcceptedUpdateMs = 0L;
    }

    boolean isReacquiring(long nowMs, long expectedUpdateIntervalMs) {
        if (lastAcceptedUpdateMs <= 0L) {
            return false;
        }
        long gapMs = nowMs - lastAcceptedUpdateMs;
        long expectedGapMs = Math.max(0L, expectedUpdateIntervalMs) + EXPECTED_UPDATE_INTERVAL_GRACE_MS;
        return gapMs >= LONG_LOCATION_UPDATE_GAP_MS && gapMs > expectedGapMs;
    }

    long gapMs(long nowMs) {
        return lastAcceptedUpdateMs <= 0L ? 0L : nowMs - lastAcceptedUpdateMs;
    }

    void recordAccepted(long nowMs) {
        lastAcceptedUpdateMs = nowMs;
    }
}

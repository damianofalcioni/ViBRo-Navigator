package vibro.navigator.nav.session;

final class NavigationLocationReacquisitionTracker {
    private static final long LONG_LOCATION_UPDATE_GAP_MS = 15_000L;

    private long lastAcceptedUpdateMs;

    void reset() {
        lastAcceptedUpdateMs = 0L;
    }

    boolean isReacquiring(long nowMs) {
        return lastAcceptedUpdateMs > 0L
                && nowMs - lastAcceptedUpdateMs >= LONG_LOCATION_UPDATE_GAP_MS;
    }

    long gapMs(long nowMs) {
        return lastAcceptedUpdateMs <= 0L ? 0L : nowMs - lastAcceptedUpdateMs;
    }

    void recordAccepted(long nowMs) {
        lastAcceptedUpdateMs = nowMs;
    }
}

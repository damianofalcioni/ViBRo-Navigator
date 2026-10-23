package vibro.navigator.nav.location;

/** Learn fresh accepted-fix spacing without counting an intentional sleep as a callback gap. */
final class StationaryFixCadence {
    private static final long MAX_GAP_MS = 15_000L;
    private long previousMs = -1L;
    private long intervalMs = 3_000L;

    long gapLimitMs() {
        return Math.min(MAX_GAP_MS, Math.max(7_000L, intervalMs + intervalMs / 2L + 1_000L));
    }

    void record(long nowMs) {
        if (previousMs >= 0L) {
            long gapMs = nowMs - previousMs;
            intervalMs = gapMs > MAX_GAP_MS ? 3_000L : gapMs;
        }
        previousMs = nowMs;
    }

    void beginBurst() {
        previousMs = -1L;
    }

    void reset() {
        beginBurst();
        intervalMs = 3_000L;
    }
}

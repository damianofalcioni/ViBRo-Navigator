package vibro.navigator.nav.location;

public final class NavigationLocationStallMonitor {
    static final long MINIMUM_STALL_TIMEOUT_MS = 30_000L;
    static final long REQUEST_INTERVAL_SLACK_MS = 15_000L;

    private long lastProgressMs = -1L;

    public void start(long nowMs) {
        lastProgressMs = nowMs;
    }

    public void reset() {
        lastProgressMs = -1L;
    }

    public void recordAcceptedFix(long nowMs) {
        lastProgressMs = nowMs;
    }

    public void recordRecoveryAttempt(long nowMs) {
        lastProgressMs = nowMs;
    }

    public boolean shouldRecover(long nowMs, long expectedUpdateIntervalMs) {
        return lastProgressMs >= 0L && nowMs >= recoveryDeadlineMs(expectedUpdateIntervalMs);
    }

    public long recoveryDeadlineMs(long expectedUpdateIntervalMs) {
        if (lastProgressMs < 0L) {
            return -1L;
        }
        return lastProgressMs + stallTimeoutMs(expectedUpdateIntervalMs);
    }

    static long stallTimeoutMs(long expectedUpdateIntervalMs) {
        long requestedWindowMs = Math.max(0L, expectedUpdateIntervalMs) + REQUEST_INTERVAL_SLACK_MS;
        return Math.max(MINIMUM_STALL_TIMEOUT_MS, requestedWindowMs);
    }
}

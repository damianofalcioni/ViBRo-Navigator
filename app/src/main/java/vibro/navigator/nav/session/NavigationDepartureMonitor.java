package vibro.navigator.nav.session;

import vibro.navigator.nav.guidance.NavigationUpdateScheduler;

/** Keep departure observations fast until independently spaced forward fixes establish travel. */
final class NavigationDepartureMonitor {
    private static final long FAST_INTERVAL_MS = 3_000L;
    private static final long MIN_SAMPLE_SPACING_MS = 2_000L;
    private static final long MAX_SAMPLE_GAP_MS = 10_000L;
    private boolean monitoring = true;
    private int stableSamples;
    private long lastSampleMs = -1L;
    private long rampIntervalMs = FAST_INTERVAL_MS;
    private long rampStartedMs = -1L;

    void reset() {
        monitoring = true;
        stableSamples = 0;
        lastSampleMs = -1L;
        rampIntervalMs = FAST_INTERVAL_MS;
        rampStartedMs = -1L;
    }

    long interval(long suggestedMs, boolean needsDepartureChecks, boolean forward, long nowMs) {
        updateState(needsDepartureChecks, forward, nowMs);
        if (suggestedMs <= 0) {
            return suggestedMs;
        }
        if (monitoring) {
            return Math.min(suggestedMs, FAST_INTERVAL_MS);
        }
        if (rampStartedMs >= 0 && nowMs - rampStartedMs >= rampIntervalMs) {
            rampIntervalMs = NavigationUpdateScheduler.nextHigherBucket(rampIntervalMs);
            rampStartedMs = nowMs;
        }
        return Math.min(suggestedMs, rampIntervalMs);
    }

    private void updateState(boolean needsDepartureChecks, boolean forward, long nowMs) {
        if (needsDepartureChecks || (monitoring && !forward)) {
            reset();
        } else if (monitoring) {
            recordForward(nowMs);
        }
    }

    private void recordForward(long nowMs) {
        if (lastSampleMs >= 0 && nowMs - lastSampleMs < MIN_SAMPLE_SPACING_MS) {
            return;
        }
        if (lastSampleMs >= 0 && nowMs - lastSampleMs > MAX_SAMPLE_GAP_MS) {
            stableSamples = 0;
        }
        lastSampleMs = nowMs;
        if (++stableSamples >= 3) {
            monitoring = false;
            rampStartedMs = nowMs;
        }
    }
}

package vibro.navigator.nav.guidance;

import java.util.Objects;
import vibro.navigator.nav.location.NavigationLocation;

/** Tighten immediately; restore quality-limited intervals only with independent fresh observations. */
public final class NavigationAcquisitionRecovery {
    private static final long MIN_INTERVAL_MS = 3_000L;
    private static final long MAX_INTERVAL_MS = 60_000L;
    private long maximumIntervalMs = MAX_INTERVAL_MS;
    private long bucketStartedMs;
    private long lastMeasurementMs = -1;
    private String provider;
    private int stableSamples;

    public void reset() {
        maximumIntervalMs = MAX_INTERVAL_MS;
        bucketStartedMs = 0;
        lastMeasurementMs = -1;
        provider = null;
        stableSamples = 0;
    }

    public long limit(long qualityCapMs, NavigationLocation location, long nowMs) {
        long previousMaximumMs = maximumIntervalMs;
        if (qualityCapMs < maximumIntervalMs) {
            tighten(qualityCapMs, nowMs);
        }
        if (location == null) {
            return Math.min(qualityCapMs, maximumIntervalMs);
        }
        long measurementMs = location.getElapsedRealtimeOrTimeMs();
        if (measurementMs > nowMs || nowMs - measurementMs > 10_000L) {
            tighten(MIN_INTERVAL_MS, nowMs);
        } else if (measurementMs > lastMeasurementMs) {
            observe(qualityCapMs, location, nowMs, previousMaximumMs);
        }
        return Math.min(qualityCapMs, maximumIntervalMs);
    }

    private void observe(long capMs, NavigationLocation location, long nowMs, long previousMaximumMs) {
        long measurementMs = location.getElapsedRealtimeOrTimeMs();
        if (isDiscontinuous(location, measurementMs, previousMaximumMs)) {
            tighten(MIN_INTERVAL_MS, nowMs);
        }
        if (lastMeasurementMs >= 0 && measurementMs - lastMeasurementMs < 2_000L) {
            return;
        }
        provider = location.getProvider();
        lastMeasurementMs = measurementMs;
        stableSamples = capMs > maximumIntervalMs ? Math.min(3, stableSamples + 1) : 0;
        if (stableSamples >= 3 && nowMs - bucketStartedMs >= maximumIntervalMs) {
            maximumIntervalMs = Math.min(capMs, NavigationUpdateScheduler.nextHigherBucket(maximumIntervalMs));
            bucketStartedMs = nowMs;
        }
    }

    private boolean isDiscontinuous(NavigationLocation location, long measurementMs, long previousMaximumMs) {
        return lastMeasurementMs >= 0 && (!Objects.equals(provider, location.getProvider())
                || measurementMs - lastMeasurementMs > Math.max(10_000L, previousMaximumMs + 5_000L));
    }

    private void tighten(long intervalMs, long nowMs) {
        maximumIntervalMs = intervalMs;
        bucketStartedMs = nowMs;
        stableSamples = 0;
    }
}

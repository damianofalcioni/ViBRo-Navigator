package vibro.navigator.nav.location;

/** Back off coarse network acquisition only while live GNSS still provides a fallback. */
final class LegacyNetworkQuality {
    private long lastTimeMs = -1;
    private int coarseFixes;
    private long lastCoarseTimeMs = -1;
    private float latestAccuracyMeters = Float.POSITIVE_INFINITY;

    void reset() {
        lastTimeMs = -1;
        coarseFixes = 0;
        lastCoarseTimeMs = -1;
        latestAccuracyMeters = Float.POSITIVE_INFINITY;
    }

    void record(NavigationLocation location, long nowMs) {
        long time = location.getElapsedRealtimeOrTimeMs();
        if (time <= lastTimeMs || time > nowMs || nowMs - time > 5_000L) {
            return;
        }
        lastTimeMs = time;
        if (!usableAccuracy(location)) {
            return;
        }
        latestAccuracyMeters = location.getAccuracy();
        if (location.getAccuracy() <= 40) {
            coarseFixes = 0;
            lastCoarseTimeMs = -1;
        } else if (isIndependentCoarse(location, time)) {
            coarseFixes = Math.min(3, coarseFixes + 1);
            lastCoarseTimeMs = time;
        }
    }

    boolean isCoarse() { return coarseFixes >= 3; }

    boolean isClearlyBetterThan(float gpsAccuracyMeters) {
        return latestAccuracyMeters + LiveLocationPolicy.accuracyImprovementMeters(gpsAccuracyMeters)
                < gpsAccuracyMeters;
    }

    private boolean isIndependentCoarse(NavigationLocation location, long timeMs) {
        return location.getAccuracy() > 50 && (lastCoarseTimeMs < 0 || timeMs - lastCoarseTimeMs >= 2_000L);
    }

    private static boolean usableAccuracy(NavigationLocation location) {
        return location.hasAccuracy() && Float.isFinite(location.getAccuracy()) && location.getAccuracy() >= 0;
    }
}

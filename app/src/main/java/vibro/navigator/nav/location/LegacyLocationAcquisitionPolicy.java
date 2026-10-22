package vibro.navigator.nav.location;

/** Raw GNSS health controls acquisition, independently of selected navigation fixes. */
final class LegacyLocationAcquisitionPolicy {
    private static final float GOOD_ACCURACY_METERS = 25f;
    private static final float POOR_ACCURACY_METERS = 40f;
    private static final long MAX_FIX_AGE_MS = 5_000L;
    private static final long MIN_GOOD_SPAN_MS = 10_000L;
    private static final long NETWORK_INTERVAL_MS = 30_000L;

    private long lastGpsTimeMs = -1L;
    private long goodSinceMs = -1L;
    private int goodFixes;
    private int poorFixes;
    private boolean stable;

    void reset() {
        lastGpsTimeMs = -1L;
        goodSinceMs = -1L;
        goodFixes = 0;
        poorFixes = 0;
        stable = false;
    }

    void record(NavigationLocation location, long nowMs, long gpsIntervalMs) {
        if (!NavigationLocationProviders.GPS_PROVIDER.equals(location.getProvider())) {
            return;
        }
        long timeMs = location.getElapsedRealtimeOrTimeMs();
        if (timeMs <= lastGpsTimeMs || timeMs > nowMs) {
            return;
        }
        expire(nowMs, gpsIntervalMs);
        lastGpsTimeMs = timeMs;
        if (!isUsable(location, nowMs)) {
            clearQuality();
            return;
        }
        recordAccuracy(location.getAccuracy(), timeMs);
    }

    private static boolean isUsable(NavigationLocation location, long nowMs) {
        long ageMs = nowMs - location.getElapsedRealtimeOrTimeMs();
        return location.hasAccuracy() && LiveLocationPolicy.isValidFix(NavigationLocationFix.from(location))
                && ageMs >= 0L && ageMs <= MAX_FIX_AGE_MS;
    }

    private void recordAccuracy(float accuracy, long timeMs) {
        if (accuracy <= GOOD_ACCURACY_METERS) {
            poorFixes = 0;
            if (goodSinceMs < 0L) {
                goodSinceMs = timeMs;
            }
            goodFixes++;
            stable = stable || (goodFixes >= 3 && timeMs - goodSinceMs >= MIN_GOOD_SPAN_MS);
            return;
        }
        goodSinceMs = -1L;
        goodFixes = 0;
        poorFixes = accuracy > POOR_ACCURACY_METERS ? poorFixes + 1 : 0;
        if (poorFixes >= 2) {
            stable = false;
        }
    }

    boolean expire(long nowMs, long gpsIntervalMs) {
        if (lastGpsTimeMs >= 0L && nowMs >= lastGpsTimeMs + silenceTimeoutMs(gpsIntervalMs)) {
            boolean changed = stable;
            clearQuality();
            return changed;
        }
        return false;
    }

    long networkIntervalMs(long nowMs, long gpsIntervalMs) {
        expire(nowMs, gpsIntervalMs);
        return stable ? Math.max(gpsIntervalMs, NETWORK_INTERVAL_MS) : gpsIntervalMs;
    }

    long recoveryDeadlineMs(long gpsIntervalMs) {
        return stable ? lastGpsTimeMs + silenceTimeoutMs(gpsIntervalMs) : -1L;
    }

    private static long silenceTimeoutMs(long gpsIntervalMs) {
        return Math.max(15_000L, gpsIntervalMs + 5_000L);
    }

    private void clearQuality() {
        goodSinceMs = -1L;
        goodFixes = 0;
        poorFixes = 0;
        stable = false;
    }
}

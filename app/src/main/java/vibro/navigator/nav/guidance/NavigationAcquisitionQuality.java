package vibro.navigator.nav.guidance;

import vibro.navigator.nav.location.NavigationLocation;

/** Heuristic approach margin, not a confidence guarantee from a provider's 68% radius. */
public final class NavigationAcquisitionQuality {
    private NavigationAcquisitionQuality() { }

    public static long limit(long suggestedMs, double distanceMeters, float speedMps,
            NavigationLocation location, long nowMs) {
        if (suggestedMs <= 3_000L || location == null) {
            return suggestedMs;
        }
        if (!hasUsableAccuracy(location)) {
            return 3_000L;
        }
        long ageMs = Math.max(0, nowMs - location.getElapsedRealtimeOrTimeMs());
        double speed = Math.max(Float.isFinite(speedMps) ? Math.max(1.5, speedMps) : 1.5,
                trustworthyNativeSpeedMps(location));
        double margin = 2 * location.getAccuracy() + speed * ageMs / 1_000.0;
        double earliestSeconds = Math.max(0, distanceMeters - margin) / speed;
        long capMs = earliestSeconds <= 30 ? 3_000L : (long) (earliestSeconds * 250);
        return Math.min(suggestedMs, floorBucket(capMs));
    }

    private static boolean hasUsableAccuracy(NavigationLocation location) {
        return location.hasAccuracy() && Float.isFinite(location.getAccuracy()) && location.getAccuracy() >= 0;
    }

    private static float trustworthyNativeSpeedMps(NavigationLocation location) {
        if (!location.hasSpeed() || !location.hasSpeedAccuracy()) {
            return 0;
        }
        float speed = location.getSpeed();
        float uncertainty = location.getSpeedAccuracyMetersPerSecond();
        return Float.isFinite(speed) && Float.isFinite(uncertainty)
                && uncertainty >= 0 && speed > 2.0 * uncertainty ? speed : 0;
    }

    private static long floorBucket(long maximumMs) {
        long result = 3_000L;
        for (long bucket : new long[] {5_000L, 8_000L, 12_000L, 20_000L, 30_000L, 60_000L}) {
            if (bucket <= maximumMs) {
                result = bucket;
            }
        }
        return result;
    }
}

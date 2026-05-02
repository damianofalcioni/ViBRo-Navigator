package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

public final class NavigationUpdateScheduler {

    private static final long MIN_UPDATE_INTERVAL_MS = 1000L;
    private static final long MAX_UPDATE_INTERVAL_MS = 60000L;
    private static final long[] UPDATE_INTERVAL_BUCKETS_MS = {
            1000L,
            2000L,
            3000L,
            5000L,
            8000L,
            12000L,
            20000L,
            30000L,
            60000L
    };
    private static final double DISTANCE_TO_INTERVAL_FACTOR = 250.0;
    private static final double VERY_IMMINENT_HINT_THRESHOLD_SECONDS = 8.0;

    public long suggestUpdateInterval(
            long nowMs,
            long fastChecksUntilMs,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            int nextHintIdx,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps
    ) {
        if (!canEstimateNextHintTime(nowMs, fastChecksUntilMs, route, polylineIndex, nextHintIdx)) {
            return MIN_UPDATE_INTERVAL_MS;
        }

        VoiceHint next = route.voiceHints.get(nextHintIdx);
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                next.indexInTrack,
                speedMps
        );
        if (timeToNextSeconds == null) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        if (timeToNextSeconds <= VERY_IMMINENT_HINT_THRESHOLD_SECONDS) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        long intervalMs = (long) Math.max(
                MIN_UPDATE_INTERVAL_MS,
                Math.min(MAX_UPDATE_INTERVAL_MS, timeToNextSeconds * DISTANCE_TO_INTERVAL_FACTOR)
        );
        return bucketInterval(intervalMs);
    }

    private static boolean canEstimateNextHintTime(
            long nowMs,
            long fastChecksUntilMs,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            int nextHintIdx
    ) {
        return nowMs > fastChecksUntilMs
                && polylineIndex != null
                && route != null
                && !route.voiceHints.isEmpty()
                && nextHintIdx >= 0
                && nextHintIdx < route.voiceHints.size();
    }

    @NonNull
    public static LongRange bounds() {
        return new LongRange(MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS);
    }

    public static long bucketInterval(long intervalMs) {
        long boundedIntervalMs = Math.max(MIN_UPDATE_INTERVAL_MS, Math.min(MAX_UPDATE_INTERVAL_MS, intervalMs));
        long bestBucketMs = UPDATE_INTERVAL_BUCKETS_MS[0];
        long bestDistanceMs = Math.abs(boundedIntervalMs - bestBucketMs);
        for (int i = 1; i < UPDATE_INTERVAL_BUCKETS_MS.length; i++) {
            long candidateBucketMs = UPDATE_INTERVAL_BUCKETS_MS[i];
            long candidateDistanceMs = Math.abs(boundedIntervalMs - candidateBucketMs);
            if (candidateDistanceMs < bestDistanceMs) {
                bestBucketMs = candidateBucketMs;
                bestDistanceMs = candidateDistanceMs;
            }
        }
        return bestBucketMs;
    }

    public static final class LongRange {
        final long min;
        final long max;

        private LongRange(long min, long max) {
            this.min = min;
            this.max = max;
        }
    }
}

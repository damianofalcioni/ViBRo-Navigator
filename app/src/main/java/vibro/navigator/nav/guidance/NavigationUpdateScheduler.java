package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

public final class NavigationUpdateScheduler {

    private static final long MIN_UPDATE_INTERVAL_MS = 3000L;
    private static final long MAX_UPDATE_INTERVAL_MS = 60000L;
    private static final long[] UPDATE_INTERVAL_BUCKETS_MS = {
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
    private static final double MANEUVER_GUARD_TIME_THRESHOLD_SECONDS = 180.0;
    private static final long MANEUVER_GUARD_MAX_INTERVAL_MS = 20_000L;

    @NonNull
    private final PostManeuverIntervalRamp postManeuverIntervalRamp = new PostManeuverIntervalRamp();

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
        return suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route,
                polylineIndex,
                resolveNextHint(route, nextHintIdx),
                null,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps
        );
    }

    public long suggestUpdateInterval(
            long nowMs,
            long fastChecksUntilMs,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            @Nullable VoiceHint next,
            @Nullable Double nextAlongTrackMeters,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps
    ) {
        if (!canEstimateRouteTime(nowMs, fastChecksUntilMs, route, polylineIndex)) {
            return MIN_UPDATE_INTERVAL_MS;
        }

        double targetAlongTrackMeters = resolveTargetAlongTrackMeters(route, polylineIndex, next, nextAlongTrackMeters);
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                targetAlongTrackMeters,
                speedMps
        );
        if (timeToNextSeconds == null) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        if (timeToNextSeconds <= VERY_IMMINENT_HINT_THRESHOLD_SECONDS) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        return intervalFromTimeToTarget(timeToNextSeconds);
    }

    long applyPostManeuverIntervalRamp(long suggestedIntervalMs, boolean passedInstruction) {
        return postManeuverIntervalRamp.apply(suggestedIntervalMs, passedInstruction);
    }

    void resetPostManeuverIntervalRamp() {
        postManeuverIntervalRamp.reset();
    }

    public long suggestDirectTargetUpdateInterval(
            long nowMs,
            long fastChecksUntilMs,
            @Nullable Double timeToTargetSeconds
    ) {
        if (nowMs <= fastChecksUntilMs
                || timeToTargetSeconds == null
                || !Double.isFinite(timeToTargetSeconds)) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        if (timeToTargetSeconds <= VERY_IMMINENT_HINT_THRESHOLD_SECONDS) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        return intervalFromTimeToTarget(timeToTargetSeconds);
    }

    private static boolean canEstimateRouteTime(
            long nowMs,
            long fastChecksUntilMs,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex
    ) {
        return nowMs > fastChecksUntilMs
                && polylineIndex != null
                && route != null
                && !route.track.isEmpty();
    }

    @Nullable
    private static VoiceHint resolveNextHint(@Nullable GeoJsonRoute route, int nextHintIdx) {
        if (route == null || nextHintIdx < 0 || nextHintIdx >= route.voiceHints.size()) {
            return null;
        }
        return route.voiceHints.get(nextHintIdx);
    }

    private static double resolveTargetAlongTrackMeters(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable VoiceHint next,
            @Nullable Double nextAlongTrackMeters
    ) {
        if (nextAlongTrackMeters != null) {
            return nextAlongTrackMeters;
        }
        if (next != null) {
            return polylineIndex.distanceAtPointIndex(next.indexInTrack);
        }
        return polylineIndex.distanceAtPointIndex(route.track.size() - 1);
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

    static long nextHigherBucket(long intervalMs) {
        long boundedIntervalMs = Math.max(MIN_UPDATE_INTERVAL_MS, Math.min(MAX_UPDATE_INTERVAL_MS, intervalMs));
        for (int i = 0; i < UPDATE_INTERVAL_BUCKETS_MS.length; i++) {
            long candidateBucketMs = UPDATE_INTERVAL_BUCKETS_MS[i];
            if (candidateBucketMs > boundedIntervalMs) {
                return candidateBucketMs;
            }
        }
        return MAX_UPDATE_INTERVAL_MS;
    }

    private static long intervalFromTimeToTarget(double timeToTargetSeconds) {
        long intervalMs = (long) Math.max(
                MIN_UPDATE_INTERVAL_MS,
                Math.min(MAX_UPDATE_INTERVAL_MS, timeToTargetSeconds * DISTANCE_TO_INTERVAL_FACTOR)
        );
        return bucketInterval(applyManeuverGuard(intervalMs, timeToTargetSeconds));
    }

    private static long applyManeuverGuard(long intervalMs, double timeToNextSeconds) {
        if (timeToNextSeconds <= MANEUVER_GUARD_TIME_THRESHOLD_SECONDS) {
            return Math.min(intervalMs, MANEUVER_GUARD_MAX_INTERVAL_MS);
        }
        return intervalMs;
    }

    public static final class LongRange {
        final long min;
        final long max;

        private LongRange(long min, long max) {
            this.min = min;
            this.max = max;
        }
    }

    private static final class PostManeuverIntervalRamp {
        private boolean active;
        private long currentMaximumIntervalMs = MIN_UPDATE_INTERVAL_MS;

        void reset() {
            active = false;
            currentMaximumIntervalMs = MIN_UPDATE_INTERVAL_MS;
        }

        long apply(long suggestedIntervalMs, boolean passedInstruction) {
            if (suggestedIntervalMs <= 0L) {
                reset();
                return suggestedIntervalMs;
            }
            if (passedInstruction) {
                return restart(suggestedIntervalMs);
            }
            if (!active) {
                return suggestedIntervalMs;
            }
            if (suggestedIntervalMs <= currentMaximumIntervalMs) {
                reset();
                return suggestedIntervalMs;
            }
            currentMaximumIntervalMs = nextHigherBucket(currentMaximumIntervalMs);
            long rampedIntervalMs = Math.min(suggestedIntervalMs, currentMaximumIntervalMs);
            if (rampedIntervalMs >= suggestedIntervalMs || currentMaximumIntervalMs >= MAX_UPDATE_INTERVAL_MS) {
                reset();
            }
            return rampedIntervalMs;
        }

        private long restart(long suggestedIntervalMs) {
            active = true;
            currentMaximumIntervalMs = MIN_UPDATE_INTERVAL_MS;
            return Math.min(suggestedIntervalMs, currentMaximumIntervalMs);
        }
    }
}

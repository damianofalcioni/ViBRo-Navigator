package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.location.NavigationLocation;

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
    private NavigationLocation qualityLocation;
    private final NavigationAcquisitionRecovery qualityRecovery = new NavigationAcquisitionRecovery();
    private String intervalReason = "not_evaluated";

    String intervalReason() {
        return intervalReason;
    }

    public void recordLocationQuality(NavigationLocation location, float accuracy) {
        qualityLocation = new NavigationLocation(location);
        qualityLocation.setAccuracy(accuracy);
    }

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
        return suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route,
                polylineIndex,
                next,
                nextAlongTrackMeters,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                Float.NaN
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
            float speedMps,
            float accelerationMps2
    ) {
        if (nowMs <= fastChecksUntilMs) {
            intervalReason = "fast_checks";
            return MIN_UPDATE_INTERVAL_MS;
        }
        if (polylineIndex == null || route == null || route.track.isEmpty()) {
            intervalReason = "route_unavailable";
            return MIN_UPDATE_INTERVAL_MS;
        }

        double targetAlongTrackMeters = resolveTargetAlongTrackMeters(route, polylineIndex, next, nextAlongTrackMeters);
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                targetAlongTrackMeters,
                RouteMotionEstimate.withAcceleration(speedMps, accelerationMps2)
        );
        if (timeToNextSeconds == null) {
            intervalReason = "eta_unavailable";
            return MIN_UPDATE_INTERVAL_MS;
        }
        if (timeToNextSeconds <= VERY_IMMINENT_HINT_THRESHOLD_SECONDS) {
            intervalReason = "imminent_hint";
            return MIN_UPDATE_INTERVAL_MS;
        }
        long qualityCapMs = NavigationAcquisitionQuality.limit(MAX_UPDATE_INTERVAL_MS,
                Math.max(0, targetAlongTrackMeters - alongTrackMeters), speedMps, qualityLocation, nowMs);
        return qualityLimitedInterval(intervalFromTimeToTarget(timeToNextSeconds), qualityCapMs, nowMs);
    }

    private long qualityLimitedInterval(long routeIntervalMs, long qualityCapMs, long nowMs) {
        long recoveredCapMs = qualityRecovery.limit(qualityCapMs, qualityLocation, nowMs);
        intervalReason = NavigationGuidanceDiagnostics.limitingIntervalReason(
                intervalReason, routeIntervalMs, qualityCapMs, recoveredCapMs);
        return Math.min(routeIntervalMs, recoveredCapMs);
    }

    long applyPostManeuverIntervalRamp(long suggestedIntervalMs, boolean passedInstruction, long nowMs) {
        long intervalMs = postManeuverIntervalRamp.apply(suggestedIntervalMs, passedInstruction, nowMs);
        if (intervalMs < suggestedIntervalMs) {
            intervalReason = "post_maneuver_ramp";
        }
        return intervalMs;
    }

    void resetPostManeuverIntervalRamp() {
        postManeuverIntervalRamp.reset();
        qualityLocation = null;
        qualityRecovery.reset();
        intervalReason = "not_evaluated";
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

    public static long nextHigherBucket(long intervalMs) {
        long boundedIntervalMs = Math.max(MIN_UPDATE_INTERVAL_MS, Math.min(MAX_UPDATE_INTERVAL_MS, intervalMs));
        for (int i = 0; i < UPDATE_INTERVAL_BUCKETS_MS.length; i++) {
            long candidateBucketMs = UPDATE_INTERVAL_BUCKETS_MS[i];
            if (candidateBucketMs > boundedIntervalMs) {
                return candidateBucketMs;
            }
        }
        return MAX_UPDATE_INTERVAL_MS;
    }

    private long intervalFromTimeToTarget(double timeToTargetSeconds) {
        long intervalMs = (long) Math.max(
                MIN_UPDATE_INTERVAL_MS,
                Math.min(MAX_UPDATE_INTERVAL_MS, timeToTargetSeconds * DISTANCE_TO_INTERVAL_FACTOR)
        );
        return bucketInterval(applyManeuverGuard(intervalMs, timeToTargetSeconds));
    }

    private long applyManeuverGuard(long intervalMs, double timeToNextSeconds) {
        intervalReason = "route_eta";
        if (timeToNextSeconds <= MANEUVER_GUARD_TIME_THRESHOLD_SECONDS) {
            long guardedIntervalMs = Math.min(intervalMs, MANEUVER_GUARD_MAX_INTERVAL_MS);
            if (guardedIntervalMs < intervalMs) {
                intervalReason = "maneuver_guard";
            }
            return guardedIntervalMs;
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
        private long currentBucketStartedAtMs;

        void reset() {
            active = false;
            currentMaximumIntervalMs = MIN_UPDATE_INTERVAL_MS;
            currentBucketStartedAtMs = 0L;
        }

        long apply(long suggestedIntervalMs, boolean passedInstruction, long nowMs) {
            if (suggestedIntervalMs <= 0L) {
                reset();
                return suggestedIntervalMs;
            }
            if (passedInstruction) {
                return restart(suggestedIntervalMs, nowMs);
            }
            if (!active) {
                return suggestedIntervalMs;
            }
            return applyActiveRamp(suggestedIntervalMs, nowMs);
        }

        private long applyActiveRamp(long suggestedIntervalMs, long nowMs) {
            if (suggestedIntervalMs < currentMaximumIntervalMs) {
                reset();
                return suggestedIntervalMs;
            }
            if (suggestedIntervalMs == currentMaximumIntervalMs) {
                if (hasCurrentBucketElapsed(nowMs)) {
                    reset();
                }
                return suggestedIntervalMs;
            }
            if (!hasCurrentBucketElapsed(nowMs)) {
                return currentMaximumIntervalMs;
            }
            currentMaximumIntervalMs = nextHigherBucket(currentMaximumIntervalMs);
            currentBucketStartedAtMs = nowMs;
            return Math.min(suggestedIntervalMs, currentMaximumIntervalMs);
        }

        private long restart(long suggestedIntervalMs, long nowMs) {
            currentMaximumIntervalMs = MIN_UPDATE_INTERVAL_MS;
            currentBucketStartedAtMs = nowMs;
            active = suggestedIntervalMs > currentMaximumIntervalMs;
            return Math.min(suggestedIntervalMs, currentMaximumIntervalMs);
        }

        private boolean hasCurrentBucketElapsed(long nowMs) {
            return nowMs - currentBucketStartedAtMs >= currentMaximumIntervalMs;
        }
    }
}

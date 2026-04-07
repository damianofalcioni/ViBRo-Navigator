package com.vibenavigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import java.util.List;

final class NavigationUpdateScheduler {

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
    private static final double SPEED_FLOOR_METERS_PER_SECOND = 1.0;
    private static final double DISTANCE_TO_INTERVAL_FACTOR = 250.0;
    private static final double VERY_IMMINENT_HINT_THRESHOLD_SECONDS = 8.0;

    long suggestUpdateInterval(
            long nowMs,
            long fastChecksUntilMs,
            @Nullable List<VoiceHint> voiceHints,
            @Nullable PolylineIndex polylineIndex,
            int nextHintIdx,
            double alongTrackMeters,
            float speedMps
    ) {
        long nextMinTimeMs = MIN_UPDATE_INTERVAL_MS;
        if (nowMs <= fastChecksUntilMs
                || polylineIndex == null
                || voiceHints == null
                || voiceHints.isEmpty()
                || nextHintIdx < 0
                || nextHintIdx >= voiceHints.size()) {
            return nextMinTimeMs;
        }

        VoiceHint next = voiceHints.get(nextHintIdx);
        double hintDistMeters = polylineIndex.distanceAtPointIndex(next.indexInTrack);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        double timeToNextSeconds = distanceToNextMeters / Math.max(SPEED_FLOOR_METERS_PER_SECOND, speedMps);
        if (timeToNextSeconds <= VERY_IMMINENT_HINT_THRESHOLD_SECONDS) {
            return MIN_UPDATE_INTERVAL_MS;
        }
        long intervalMs = (long) Math.max(
                MIN_UPDATE_INTERVAL_MS,
                Math.min(MAX_UPDATE_INTERVAL_MS, timeToNextSeconds * DISTANCE_TO_INTERVAL_FACTOR)
        );
        return bucketInterval(intervalMs);
    }

    @NonNull
    static LongRange bounds() {
        return new LongRange(MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS);
    }

    static long bucketInterval(long intervalMs) {
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

    static final class LongRange {
        final long min;
        final long max;

        private LongRange(long min, long max) {
            this.min = min;
            this.max = max;
        }
    }
}

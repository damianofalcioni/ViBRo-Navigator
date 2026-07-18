package vibro.navigator.nav.guidance;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;

public final class NavigationRouteProgressTracker {

    private static final long MAX_ACCURACY_SAMPLE_AGE_MS = 5_000L;
    private static final long DIRECTION_PROGRESS_WINDOW_MS = 3_000L;
    private static final long MAX_DIRECTION_PROGRESS_SAMPLE_AGE_MS = 10_000L;
    private static final double MIN_DIRECTION_PROGRESS_METERS = 4.0;

    private final ArrayDeque<AccuracySample> recentAccuracySamples = new ArrayDeque<>();
    private final ArrayDeque<RouteProgressSample> recentAlongTrackSamples = new ArrayDeque<>();

    public void reset() {
        recentAccuracySamples.clear();
        recentAlongTrackSamples.clear();
    }

    public double rememberAndResolveSmoothedAccuracyMeters(float accuracyMeters, long nowMs) {
        if (Float.isFinite(accuracyMeters) && accuracyMeters > 0f) {
            recentAccuracySamples.addLast(new AccuracySample(accuracyMeters, nowMs));
        }
        pruneAccuracySamples(nowMs);
        if (recentAccuracySamples.isEmpty()) {
            return accuracyMeters;
        }

        double[] samples = new double[recentAccuracySamples.size()];
        int idx = 0;
        for (AccuracySample sample : recentAccuracySamples) {
            samples[idx++] = sample.accuracyMeters;
        }
        Arrays.sort(samples);
        int middle = samples.length / 2;
        if ((samples.length & 1) == 1) {
            return samples[middle];
        }
        return (samples[middle - 1] + samples[middle]) / 2.0;
    }

    public float resolveEtaSpeedMps(
            @Nullable NavigationLocation location,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        return resolveEtaMotion(location, alongTrackMeters, accuracyMeters, likelyStationary).speedMps;
    }

    public float resolveEtaAccelerationMps2(
            @Nullable NavigationLocation location,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        return resolveEtaMotion(location, alongTrackMeters, accuracyMeters, likelyStationary).accelerationMps2;
    }

    @NonNull
    public RouteMotionEstimate resolveEtaMotion(
            @Nullable NavigationLocation location,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        if (location == null) {
            return RouteMotionEstimate.stationary();
        }
        return RouteEtaMotionEstimator.resolve(
                recentAlongTrackSamples,
                location.getElapsedRealtimeOrTimeMs(),
                alongTrackMeters,
                accuracyMeters,
                likelyStationary
        );
    }

    public float resolveEtaSpeedMps(
            long locationTimeMs,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        return resolveEtaMotion(locationTimeMs, alongTrackMeters, accuracyMeters, likelyStationary).speedMps;
    }

    @NonNull
    public RouteMotionEstimate resolveEtaMotion(
            long locationTimeMs,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        if (likelyStationary) {
            return RouteMotionEstimate.stationary();
        }
        pruneAlongTrackSamples(locationTimeMs);
        return RouteEtaMotionEstimator.resolve(
                recentAlongTrackSamples,
                locationTimeMs,
                alongTrackMeters,
                accuracyMeters,
                false
        );
    }

    @NonNull
    public DirectionAssessment assessDirection(double alongTrackMeters, long nowMs) {
        pruneAlongTrackSamples(nowMs);
        RouteProgressSample anchor = findDirectionAnchor(nowMs);
        if (anchor == null) {
            return DirectionAssessment.unknown();
        }
        double deltaMeters = alongTrackMeters - anchor.alongTrackMeters;
        if (deltaMeters >= MIN_DIRECTION_PROGRESS_METERS) {
            return DirectionAssessment.forward(deltaMeters);
        }
        if (deltaMeters <= -MIN_DIRECTION_PROGRESS_METERS) {
            return DirectionAssessment.backward(deltaMeters);
        }
        return DirectionAssessment.stalled(deltaMeters);
    }

    public void rememberAlongTrackSample(double alongTrackMeters, long nowMs) {
        recentAlongTrackSamples.addLast(new RouteProgressSample(alongTrackMeters, nowMs));
        pruneAlongTrackSamples(nowMs);
    }

    @Nullable
    private RouteProgressSample findDirectionAnchor(long nowMs) {
        return latestEligibleAlongTrackSample(nowMs, DIRECTION_PROGRESS_WINDOW_MS);
    }

    @Nullable
    private RouteProgressSample latestEligibleAlongTrackSample(long nowMs, long minimumAgeMs) {
        Iterator<RouteProgressSample> candidates = recentAlongTrackSamples.descendingIterator();
        while (candidates.hasNext()) {
            RouteProgressSample candidate = candidates.next();
            if (nowMs - candidate.timeMs >= minimumAgeMs) {
                return candidate;
            }
        }
        return null;
    }

    private void pruneAccuracySamples(long nowMs) {
        long cutoffMs = nowMs - MAX_ACCURACY_SAMPLE_AGE_MS;
        while (recentAccuracySamples.size() > 1
                && recentAccuracySamples.peekFirst() != null
                && recentAccuracySamples.peekFirst().timeMs < cutoffMs) {
            recentAccuracySamples.removeFirst();
        }
    }

    private void pruneAlongTrackSamples(long nowMs) {
        long cutoffMs = nowMs - MAX_DIRECTION_PROGRESS_SAMPLE_AGE_MS;
        while (recentAlongTrackSamples.peekFirst() != null
                && recentAlongTrackSamples.peekFirst().timeMs < cutoffMs) {
            recentAlongTrackSamples.removeFirst();
        }
    }

    public enum DirectionStatus {
        UNKNOWN,
        FORWARD,
        BACKWARD,
        STALLED
    }

    public static final class DirectionAssessment {
        @NonNull
        public final DirectionStatus status;
        public final double alongTrackDeltaMeters;

        private DirectionAssessment(@NonNull DirectionStatus status, double alongTrackDeltaMeters) {
            this.status = status;
            this.alongTrackDeltaMeters = alongTrackDeltaMeters;
        }

        @NonNull
        public static DirectionAssessment unknown() {
            return new DirectionAssessment(DirectionStatus.UNKNOWN, 0.0);
        }

        @NonNull
        public static DirectionAssessment forward(double alongTrackDeltaMeters) {
            return new DirectionAssessment(DirectionStatus.FORWARD, alongTrackDeltaMeters);
        }

        @NonNull
        public static DirectionAssessment backward(double alongTrackDeltaMeters) {
            return new DirectionAssessment(DirectionStatus.BACKWARD, alongTrackDeltaMeters);
        }

        @NonNull
        public static DirectionAssessment stalled(double alongTrackDeltaMeters) {
            return new DirectionAssessment(DirectionStatus.STALLED, alongTrackDeltaMeters);
        }
    }

    private static final class AccuracySample {
        public final double accuracyMeters;
        public final long timeMs;

        private AccuracySample(double accuracyMeters, long timeMs) {
            this.accuracyMeters = accuracyMeters;
            this.timeMs = timeMs;
        }
    }
}

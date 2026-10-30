package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import vibro.navigator.nav.location.NavigationLocation;

public final class NavigationRouteProgressTracker {

    private static final long MAX_ACCURACY_SAMPLE_AGE_MS = 5_000L;
    private static final long MAX_DIRECTION_PROGRESS_SAMPLE_AGE_MS = 10_000L;

    private final ArrayDeque<AccuracySample> recentAccuracySamples = new ArrayDeque<>();
    private final ArrayDeque<RouteProgressSample> recentAlongTrackSamples = new ArrayDeque<>();
    private final NavigationRouteEtaHistory etaHistory = new NavigationRouteEtaHistory();
    private String motionProvider;

    public void reset() {
        recentAccuracySamples.clear();
        recentAlongTrackSamples.clear();
        etaHistory.reset();
        motionProvider = null;
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

    @NonNull
    public NavigationRouteEtaHistory etaHistory() {
        return etaHistory;
    }

    public void beginLocation(NavigationLocation location, boolean confirmedMoving) {
        recordProvider(location.getProvider());
        if (!confirmedMoving) {
            etaHistory.reset();
        }
    }

    private void recordProvider(@Nullable String provider) {
        if (motionProvider != null && !Objects.equals(motionProvider, provider)) {
            // A correction between provider coordinate frames is not route progress.
            recentAlongTrackSamples.clear();
            etaHistory.reset();
        }
        motionProvider = provider;
    }

    @NonNull
    public DirectionAssessment assessDirection(double alongTrackMeters, long nowMs) {
        return assessDirection(alongTrackMeters, nowMs, Long.MIN_VALUE);
    }

    @NonNull
    // Display handoffs can exclude earlier beeline samples without resetting guidance/ETA history.
    public DirectionAssessment assessDirection(double alongTrackMeters, long nowMs, long earliestAnchorMs) {
        pruneAlongTrackSamples(nowMs);
        return RouteDirectionPolicy.assess(recentAlongTrackSamples, alongTrackMeters, nowMs, earliestAnchorMs);
    }

    public void rememberAlongTrackSample(double alongTrackMeters, long nowMs) {
        if (!recentAlongTrackSamples.isEmpty() && nowMs <= recentAlongTrackSamples.peekLast().timeMs) {
            return;
        }
        recentAlongTrackSamples.addLast(new RouteProgressSample(alongTrackMeters, nowMs));
        pruneAlongTrackSamples(nowMs);
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

package vibro.navigator.nav.guidance;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;

public final class NavigationRouteProgressTracker {

    private static final long MAX_ACCURACY_SAMPLE_AGE_MS = 5_000L;
    private static final long DIRECTION_PROGRESS_WINDOW_MS = 3_000L;
    private static final long MAX_DIRECTION_PROGRESS_SAMPLE_AGE_MS = 10_000L;
    private static final double MIN_DIRECTION_PROGRESS_METERS = 4.0;
    private static final long MIN_ETA_SPEED_SAMPLE_AGE_MS = 2_000L;
    private static final double MIN_ETA_PROGRESS_METERS = 1.0;
    private static final float MIN_TRUSTED_ETA_SPEED_MPS = 0.5f;

    private final ArrayDeque<AccuracySample> recentAccuracySamples = new ArrayDeque<>();
    private final ArrayDeque<AlongTrackSample> recentAlongTrackSamples = new ArrayDeque<>();

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
            @Nullable Location location,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        if (location == null || likelyStationary) {
            return 0f;
        }
        pruneAlongTrackSamples(location.getTime());
        AlongTrackSample anchor = findEtaAnchor(location.getTime());
        if (anchor == null) {
            return 0f;
        }
        double alongTrackDeltaMeters = alongTrackMeters - anchor.alongTrackMeters;
        if (alongTrackDeltaMeters < minimumEtaProgressMeters(accuracyMeters)) {
            return 0f;
        }
        double elapsedSeconds = (location.getTime() - anchor.timeMs) / 1000.0;
        if (elapsedSeconds <= 0.0) {
            return 0f;
        }
        float smoothedSpeedMps = (float) (alongTrackDeltaMeters / elapsedSeconds);
        return smoothedSpeedMps >= MIN_TRUSTED_ETA_SPEED_MPS ? smoothedSpeedMps : 0f;
    }

    @NonNull
    public DirectionAssessment assessDirection(double alongTrackMeters, long nowMs) {
        pruneAlongTrackSamples(nowMs);
        AlongTrackSample anchor = findDirectionAnchor(nowMs);
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
        recentAlongTrackSamples.addLast(new AlongTrackSample(alongTrackMeters, nowMs));
        pruneAlongTrackSamples(nowMs);
    }

    @Nullable
    private AlongTrackSample findEtaAnchor(long locationTimeMs) {
        for (AlongTrackSample candidate : recentAlongTrackSamples) {
            if (locationTimeMs - candidate.timeMs >= MIN_ETA_SPEED_SAMPLE_AGE_MS) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private AlongTrackSample findDirectionAnchor(long nowMs) {
        for (AlongTrackSample candidate : recentAlongTrackSamples) {
            if (nowMs - candidate.timeMs >= DIRECTION_PROGRESS_WINDOW_MS) {
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
        while (recentAlongTrackSamples.size() > 1
                && recentAlongTrackSamples.peekFirst() != null
                && recentAlongTrackSamples.peekFirst().timeMs < cutoffMs) {
            recentAlongTrackSamples.removeFirst();
        }
    }

    private static double minimumEtaProgressMeters(float accuracyMeters) {
        return Math.max(
                MIN_ETA_PROGRESS_METERS,
                Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters * 0.25 : 0.0
        );
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

    private static final class AlongTrackSample {
        public final double alongTrackMeters;
        public final long timeMs;

        private AlongTrackSample(double alongTrackMeters, long timeMs) {
            this.alongTrackMeters = alongTrackMeters;
            this.timeMs = timeMs;
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

package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class StationaryOrientationAdvisor {

    public enum Outcome {
        MOVING,
        WAITING_FOR_DWELL,
        WAITING_FOR_ROUTE,
        WAITING_FOR_SENSOR,
        WAITING_FOR_CALIBRATION,
        ALIGNED,
        NOTIFY
    }

    public static final class Decision {
        final double signedTurnDegrees;

        public Decision(double signedTurnDegrees) {
            this.signedTurnDegrees = signedTurnDegrees;
        }

        public double absoluteTurnDegrees() {
            return Math.abs(signedTurnDegrees);
        }

        public boolean turnRight() {
            return signedTurnDegrees > 0.0;
        }
    }

    public static final class Evaluation {
        @NonNull
        final Outcome outcome;
        @Nullable
        final Decision decision;

        private Evaluation(@NonNull Outcome outcome, @Nullable Decision decision) {
            this.outcome = outcome;
            this.decision = decision;
        }

        @NonNull
        public static Evaluation of(@NonNull Outcome outcome) {
            return new Evaluation(outcome, null);
        }

        @NonNull
        public static Evaluation notify(@NonNull Decision decision) {
            return new Evaluation(Outcome.NOTIFY, decision);
        }
    }

    private static final long REQUIRED_STATIONARY_DURATION_MS = 5_000L;
    private static final long MAX_SAMPLE_AGE_MS = 5_000L;
    private static final float MAX_STATIONARY_SPEED_MPS = 0.35f;
    private static final double MIN_NOTIFICATION_TURN_DEGREES = 15.0;

    public boolean isStationary(float speedMps) {
        return speedMps <= MAX_STATIONARY_SPEED_MPS;
    }

    public long requiredStationaryDurationMs() {
        return REQUIRED_STATIONARY_DURATION_MS;
    }

    @NonNull
    public Evaluation evaluate(
            float speedMps,
            long stationarySinceElapsedRealtimeMs,
            @Nullable Double routeBearingDegrees,
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs
    ) {
        if (!isStationary(speedMps)) {
            return Evaluation.of(Outcome.MOVING);
        }
        if (!hasCompletedStationaryDwell(stationarySinceElapsedRealtimeMs, nowElapsedRealtimeMs)) {
            return Evaluation.of(Outcome.WAITING_FOR_DWELL);
        }
        if (routeBearingDegrees == null) {
            return Evaluation.of(Outcome.WAITING_FOR_ROUTE);
        }
        if (!hasFreshSample(sample, nowElapsedRealtimeMs)) {
            return Evaluation.of(Outcome.WAITING_FOR_SENSOR);
        }
        double signedTurnDegrees = normalizeSignedDegrees(routeBearingDegrees - sample.headingDegrees);
        double absoluteTurnDegrees = Math.abs(signedTurnDegrees);
        if (absoluteTurnDegrees < MIN_NOTIFICATION_TURN_DEGREES) {
            return Evaluation.of(Outcome.ALIGNED);
        }
        if (!hasReliableAccuracy(sample, absoluteTurnDegrees)) {
            return Evaluation.of(Outcome.WAITING_FOR_CALIBRATION);
        }
        return Evaluation.notify(new Decision(signedTurnDegrees));
    }

    private static boolean hasCompletedStationaryDwell(
            long stationarySinceElapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        return stationarySinceElapsedRealtimeMs > 0L
                && nowElapsedRealtimeMs - stationarySinceElapsedRealtimeMs >= REQUIRED_STATIONARY_DURATION_MS;
    }

    private static boolean hasFreshSample(
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs
    ) {
        return sample != null && nowElapsedRealtimeMs - sample.elapsedRealtimeMs <= MAX_SAMPLE_AGE_MS;
    }

    private static boolean hasReliableAccuracy(
            @NonNull GeomagneticOrientationMonitor.Sample sample,
            double absoluteTurnDegrees
    ) {
        return sample.isAccuracyHighEnough()
                && sample.isHeadingAccuracyHighEnough(absoluteTurnDegrees, MIN_NOTIFICATION_TURN_DEGREES);
    }

    private static double normalizeSignedDegrees(double degrees) {
        double normalized = ((degrees + 540.0) % 360.0) - 180.0;
        if (normalized == -180.0) {
            return 180.0;
        }
        return normalized;
    }
}

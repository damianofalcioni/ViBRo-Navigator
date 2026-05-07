package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassOrientationCue;

public final class StationaryOrientationNotifier {

    public interface Sink {
        void sendStationaryOrientationNotification(@NonNull StationaryOrientationAdvisor.Decision decision);
    }

    private static final String TAG = "StationaryOrientation";
    private static final double MAX_MOVING_ALIGNMENT_DEGREES = 45.0;

    private final StationaryOrientationAdvisor advisor;
    private long stationarySinceElapsedRealtimeMs;
    private boolean handledForCurrentStop;
    @Nullable
    private CompassOrientationCue activeOrientationCue;

    public StationaryOrientationNotifier(@NonNull StationaryOrientationAdvisor advisor) {
        this.advisor = advisor;
    }

    public void reset() {
        stationarySinceElapsedRealtimeMs = 0L;
        handledForCurrentStop = false;
        activeOrientationCue = null;
    }

    @Nullable
    public CompassOrientationCue activeOrientationCue() {
        return activeOrientationCue;
    }

    public void maybeNotify(
            boolean hasActiveRoute,
            boolean routeCalculationInProgress,
            boolean likelyStationary,
            float speedMps,
            @Nullable Double routeBearingDegrees,
            @Nullable Double actualBearingDegrees,
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs,
            @NonNull Sink sink
    ) {
        if (!shouldEvaluate(hasActiveRoute, routeCalculationInProgress)) {
            reset();
            return;
        }
        if (!likelyStationary) {
            handleMovement(routeBearingDegrees, actualBearingDegrees);
            return;
        }

        rememberStationaryStart(nowElapsedRealtimeMs);
        if (handledForCurrentStop) {
            if (activeOrientationCue != null) {
                updateActiveTarget(routeBearingDegrees);
            }
            return;
        }

        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                speedMps,
                stationarySinceElapsedRealtimeMs,
                routeBearingDegrees,
                sample,
                nowElapsedRealtimeMs
        );
        handleEvaluation(evaluation, routeBearingDegrees, sink);
    }

    public static boolean shouldEvaluate(
            boolean hasActiveRoute,
            boolean routeCalculationInProgress
    ) {
        return hasActiveRoute && !routeCalculationInProgress;
    }

    private void rememberStationaryStart(long nowElapsedRealtimeMs) {
        if (stationarySinceElapsedRealtimeMs <= 0L) {
            stationarySinceElapsedRealtimeMs = nowElapsedRealtimeMs;
            handledForCurrentStop = false;
        }
    }

    private void handleEvaluation(
            @NonNull StationaryOrientationAdvisor.Evaluation evaluation,
            @Nullable Double routeBearingDegrees,
            @NonNull Sink sink
    ) {
        if (evaluation.outcome == StationaryOrientationAdvisor.Outcome.ALIGNED) {
            activeOrientationCue = null;
            handledForCurrentStop = true;
            AppLogger.i(TAG, "Stationary orientation notification skipped because the user is already aligned");
            return;
        }
        if (evaluation.outcome == StationaryOrientationAdvisor.Outcome.NOTIFY) {
            updateActiveTarget(routeBearingDegrees);
            notifyIfDecisionAvailable(evaluation, sink);
            return;
        }
        if (evaluation.outcome == StationaryOrientationAdvisor.Outcome.MOVING) {
            reset();
        }
    }

    private void notifyIfDecisionAvailable(
            @NonNull StationaryOrientationAdvisor.Evaluation evaluation,
            @NonNull Sink sink
    ) {
        if (evaluation.decision == null) {
            return;
        }
        sink.sendStationaryOrientationNotification(evaluation.decision);
        handledForCurrentStop = true;
    }

    private void handleMovement(
            @Nullable Double routeBearingDegrees,
            @Nullable Double actualBearingDegrees
    ) {
        if (activeOrientationCue == null || isMovingTowardTarget(routeBearingDegrees, actualBearingDegrees)) {
            reset();
            return;
        }
        stationarySinceElapsedRealtimeMs = 0L;
        handledForCurrentStop = false;
    }

    private static boolean isMovingTowardTarget(
            @Nullable Double routeBearingDegrees,
            @Nullable Double actualBearingDegrees
    ) {
        if (routeBearingDegrees == null || actualBearingDegrees == null) {
            return false;
        }
        return Math.abs(normalizeSignedDegrees(routeBearingDegrees - actualBearingDegrees)) <= MAX_MOVING_ALIGNMENT_DEGREES;
    }

    private void updateActiveTarget(@Nullable Double routeBearingDegrees) {
        if (routeBearingDegrees == null) {
            return;
        }
        activeOrientationCue = new CompassOrientationCue(routeBearingDegrees.floatValue());
    }

    private static double normalizeSignedDegrees(double degrees) {
        double normalized = ((degrees + 540.0) % 360.0) - 180.0;
        if (normalized == -180.0) {
            return 180.0;
        }
        return normalized;
    }
}

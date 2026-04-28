package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

final class StationaryOrientationNotifier {

    interface Sink {
        void sendStationaryOrientationNotification(@NonNull StationaryOrientationAdvisor.Decision decision);
    }

    private static final String TAG = "StationaryOrientation";

    private final StationaryOrientationAdvisor advisor;
    private long stationarySinceElapsedRealtimeMs;
    private boolean handledForCurrentStop;

    StationaryOrientationNotifier(@NonNull StationaryOrientationAdvisor advisor) {
        this.advisor = advisor;
    }

    void reset() {
        stationarySinceElapsedRealtimeMs = 0L;
        handledForCurrentStop = false;
    }

    void maybeNotify(
            boolean hasActiveRoute,
            boolean routeCalculationInProgress,
            boolean likelyStationary,
            float speedMps,
            @Nullable Double routeBearingDegrees,
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs,
            @NonNull Sink sink
    ) {
        if (!shouldEvaluate(hasActiveRoute, routeCalculationInProgress)) {
            reset();
            return;
        }
        if (!likelyStationary) {
            reset();
            return;
        }

        rememberStationaryStart(nowElapsedRealtimeMs);
        if (handledForCurrentStop) {
            return;
        }

        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                speedMps,
                stationarySinceElapsedRealtimeMs,
                routeBearingDegrees,
                sample,
                nowElapsedRealtimeMs
        );
        handleEvaluation(evaluation, sink);
    }

    static boolean shouldEvaluate(
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
            @NonNull Sink sink
    ) {
        switch (evaluation.outcome) {
            case ALIGNED:
                handledForCurrentStop = true;
                AppLogger.i(TAG, "Stationary orientation notification skipped because the user is already aligned");
                return;
            case NOTIFY:
                notifyIfDecisionAvailable(evaluation, sink);
                return;
            case MOVING:
                reset();
                return;
            case WAITING_FOR_DWELL:
            case WAITING_FOR_ROUTE:
            case WAITING_FOR_SENSOR:
            case WAITING_FOR_CALIBRATION:
            default:
                return;
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
}

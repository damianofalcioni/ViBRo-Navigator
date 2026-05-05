package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

public final class StationaryOrientationNotifier {

    public interface Sink {
        void sendStationaryOrientationNotification(@NonNull StationaryOrientationAdvisor.Decision decision);
    }

    private static final String TAG = "StationaryOrientation";

    private final StationaryOrientationAdvisor advisor;
    private long stationarySinceElapsedRealtimeMs;
    private boolean handledForCurrentStop;

    public StationaryOrientationNotifier(@NonNull StationaryOrientationAdvisor advisor) {
        this.advisor = advisor;
    }

    public void reset() {
        stationarySinceElapsedRealtimeMs = 0L;
        handledForCurrentStop = false;
    }

    public void maybeNotify(
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
            @NonNull Sink sink
    ) {
        if (evaluation.outcome == StationaryOrientationAdvisor.Outcome.ALIGNED) {
            handledForCurrentStop = true;
            AppLogger.i(TAG, "Stationary orientation notification skipped because the user is already aligned");
            return;
        }
        if (evaluation.outcome == StationaryOrientationAdvisor.Outcome.NOTIFY) {
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
}

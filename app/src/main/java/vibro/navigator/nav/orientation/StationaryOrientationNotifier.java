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

    private final StationaryOrientationAdvisor advisor;
    private long stationarySinceElapsedRealtimeMs;
    private boolean handledForCurrentStop;
    @Nullable
    private CompassOrientationCue activeOrientationCue;
    @Nullable
    private StationaryOrientationAdvisor.Outcome lastLoggedOutcome;

    public StationaryOrientationNotifier(@NonNull StationaryOrientationAdvisor advisor) {
        this.advisor = advisor;
    }

    public void reset() {
        stationarySinceElapsedRealtimeMs = 0L;
        handledForCurrentStop = false;
        activeOrientationCue = null;
        lastLoggedOutcome = null;
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
        logEvaluationIfChanged(evaluation, speedMps, routeBearingDegrees, sample, nowElapsedRealtimeMs);
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

    private void logEvaluationIfChanged(
            @NonNull StationaryOrientationAdvisor.Evaluation evaluation,
            float speedMps,
            @Nullable Double routeBearingDegrees,
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs
    ) {
        if (evaluation.outcome == lastLoggedOutcome) {
            return;
        }
        lastLoggedOutcome = evaluation.outcome;
        AppLogger.i(TAG, buildEvaluationMessage(
                evaluation,
                speedMps,
                routeBearingDegrees,
                sample,
                nowElapsedRealtimeMs
        ));
    }

    @NonNull
    private String buildEvaluationMessage(
            @NonNull StationaryOrientationAdvisor.Evaluation evaluation,
            float speedMps,
            @Nullable Double routeBearingDegrees,
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs
    ) {
        StringBuilder message = new StringBuilder("Stationary orientation evaluation outcome=");
        message.append(evaluation.outcome)
                .append(" speedMps=").append(speedMps)
                .append(" stationaryForMs=")
                .append(Math.max(0L, nowElapsedRealtimeMs - stationarySinceElapsedRealtimeMs))
                .append(" routeBearing=").append(routeBearingDegrees);
        appendSample(message, sample, nowElapsedRealtimeMs);
        if (evaluation.decision != null) {
            message.append(" signedTurnDegrees=").append(evaluation.decision.signedTurnDegrees);
        }
        return message.toString();
    }

    private static void appendSample(
            @NonNull StringBuilder message,
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            long nowElapsedRealtimeMs
    ) {
        if (sample == null) {
            message.append(" sample=null");
            return;
        }
        message.append(" sampleHeading=").append(sample.headingDegrees)
                .append(" sampleAgeMs=").append(nowElapsedRealtimeMs - sample.elapsedRealtimeMs)
                .append(" sampleAccuracy=").append(sample.accuracy)
                .append(" effectiveHeadingAccuracy=")
                .append(sample.effectiveHeadingAccuracyDegrees(nowElapsedRealtimeMs))
                .append(" legacyAccuracy=").append(sample.legacyOrientationAccuracy)
                .append(" legacyAgeMs=")
                .append(sample.legacyOrientationAccuracyElapsedRealtimeMs < 0L
                        ? null
                        : nowElapsedRealtimeMs - sample.legacyOrientationAccuracyElapsedRealtimeMs);
    }

    private void updateActiveTarget(@Nullable Double routeBearingDegrees) {
        if (routeBearingDegrees == null) {
            return;
        }
        activeOrientationCue = new CompassOrientationCue(routeBearingDegrees.floatValue());
    }

}

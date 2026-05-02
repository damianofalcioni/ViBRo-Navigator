package vibro.navigator.nav.session;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.util.AppLogger;

final class NavigationRouteEvaluator {
    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    @NonNull
    private final NavigationRouteGeometryState geometryState;
    @NonNull
    private final NavigationTurnState turnState;
    @NonNull
    private final NavigationRouteProgressTracker progressTracker;
    @NonNull
    private final NavigationRouteDeviationHandler deviationHandler;
    @NonNull
    private final NavigationSessionRouteDisplayState displayState;
    @NonNull
    private final NavigationArrivalDetector arrivalDetector;

    NavigationRouteEvaluator(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationSessionRouteDisplayState displayState,
            @NonNull NavigationArrivalDetector arrivalDetector
    ) {
        this.geometryState = geometryState;
        this.turnState = turnState;
        this.progressTracker = progressTracker;
        this.deviationHandler = deviationHandler;
        this.displayState = displayState;
        this.arrivalDetector = arrivalDetector;
    }

    @NonNull
    NavigationSessionRouteState.Evaluation evaluateLocation(
            @NonNull Location filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        if (geometryState.isRouteUnavailable()) {
            AppLogger.i(TAG, "No active route loaded, requesting route calculation");
            return NavigationSessionRouteState.Evaluation.requestRecalculation(null);
        }

        PolylineIndex.Match match = geometryState.match(filtered);
        if (match == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            return NavigationSessionRouteState.Evaluation.requestRecalculation(null);
        }
        geometryState.rememberSegment(match);
        double expectedBearingDegrees = geometryState.expectedBearingDegrees(match);
        double smoothedAccuracyMeters = progressTracker.rememberAndResolveSmoothedAccuracyMeters(accuracyMeters, nowMs);
        displayState.rememberSmoothedAccuracyMeters((float) smoothedAccuracyMeters);
        float etaSpeedMps = progressTracker.resolveEtaSpeedMps(
                filtered,
                match.alongTrackMeters,
                accuracyMeters,
                likelyStationary
        );
        NavigationRouteProgressTracker.DirectionAssessment directionOfProgress = progressTracker.assessDirection(
                match.alongTrackMeters,
                nowMs
        );
        if (arrivalDetector.isDestinationReached(filtered, accuracyMeters)) {
            deviationHandler.clearDeviationEvidence();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return NavigationSessionRouteState.Evaluation.keepRoute(
                    turnState.onDestinationReached(geometryState.route()),
                    NO_SUGGESTED_INTERVAL,
                    true
            );
        }

        NavigationRouteDeviationHandler.Decision deviationDecision = deviationHandler.evaluate(
                match,
                smoothedAccuracyMeters,
                directionOfProgress,
                speedMps,
                expectedBearingDegrees,
                actualBearingDegrees,
                nowMs
        );
        if (deviationDecision.shouldRecalculateRoute()) {
            return NavigationSessionRouteState.Evaluation.requestRecalculation(deviationDecision.getRerouteNotice());
        }
        if (deviationDecision.shouldKeepCurrentRoute()) {
            return keepCurrentRoute(
                    match,
                    etaSpeedMps,
                    accuracyMeters,
                    nowMs,
                    fastChecksUntilMs,
                    deviationDecision.isStableOnRouteSample()
            );
        }

        progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
        return keepCurrentRoute(match, etaSpeedMps, accuracyMeters, nowMs, fastChecksUntilMs, true);
    }

    @NonNull
    private NavigationSessionRouteState.Evaluation keepCurrentRoute(
            @NonNull PolylineIndex.Match match,
            float etaSpeedMps,
            float accuracyMeters,
            long nowMs,
            long fastChecksUntilMs,
            boolean stableOnRouteSample
    ) {
        NavigationTurnState.Progress progress = turnState.evaluate(
                geometryState.route(),
                geometryState.polylineIndex(),
                match.alongTrackMeters,
                match.segmentIndex,
                etaSpeedMps,
                accuracyMeters,
                nowMs,
                fastChecksUntilMs
        );
        return NavigationSessionRouteState.Evaluation.keepRoute(
                progress.turnEvents,
                progress.suggestedUpdateIntervalMs,
                stableOnRouteSample
        );
    }
}

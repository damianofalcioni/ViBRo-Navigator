package vibro.navigator.nav.session;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.startup.NavigationStartupLocationSelector;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.logging.AppLogger;

final class NavigationRouteEvaluator {
    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;
    private static final long STARTUP_LOCATION_WAIT_INTERVAL_MS = 1_000L;

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
    @NonNull
    private final NavigationIntermediateArrivalTracker intermediateArrivalTracker;

    NavigationRouteEvaluator(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationSessionRouteDisplayState displayState,
            @NonNull NavigationArrivalDetector arrivalDetector,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker
    ) {
        this.geometryState = geometryState;
        this.turnState = turnState;
        this.progressTracker = progressTracker;
        this.deviationHandler = deviationHandler;
        this.displayState = displayState;
        this.arrivalDetector = arrivalDetector;
        this.intermediateArrivalTracker = intermediateArrivalTracker;
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
            return evaluateUnavailableRoute(filtered, accuracyMeters, nowMs);
        }

        PolylineIndex.Match match = geometryState.match(filtered);
        if (match == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            return NavigationSessionRouteState.Evaluation.requestRecalculation(
                    null,
                    NavigationRouteRecalculationReason.ROUTE_MATCH_FAILED
            );
        }
        geometryState.rememberSegment(match);
        double expectedBearingDegrees = geometryState.expectedBearingDegrees(match);
        double smoothedAccuracyMeters = progressTracker.rememberAndResolveSmoothedAccuracyMeters(accuracyMeters, nowMs);
        float trustedAccuracyMeters = (float) smoothedAccuracyMeters;
        displayState.rememberSmoothedAccuracyMeters(trustedAccuracyMeters);
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
        if (arrivalDetector.isDestinationReached(filtered, trustedAccuracyMeters)) {
            deviationHandler.clearDeviationEvidence();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return NavigationSessionRouteState.Evaluation.keepRoute(
                    turnState.onDestinationReached(geometryState.route()),
                    NO_SUGGESTED_INTERVAL,
                    true
            );
        }

        Integer reachedIntermediateTrackIndex = intermediateArrivalTracker.reachedTrackIndex(
                filtered,
                trustedAccuracyMeters
        );
        if (reachedIntermediateTrackIndex != null) {
            deviationHandler.clearDeviationEvidence();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return NavigationSessionRouteState.Evaluation.keepRoute(
                    turnState.onIntermediateDestinationReached(reachedIntermediateTrackIndex),
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
            return NavigationSessionRouteState.Evaluation.requestRecalculation(
                    deviationDecision.getRerouteNotice(),
                    NavigationRouteRecalculationReason.ROUTE_DEVIATION
            );
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
    private NavigationSessionRouteState.Evaluation evaluateUnavailableRoute(
            @NonNull Location filtered,
            float accuracyMeters,
            long nowMs
    ) {
        NavigationSessionRouteState.Evaluation startupWait =
                waitForAccurateStartupLocationIfNeeded(filtered, accuracyMeters, nowMs);
        if (startupWait != null) {
            return startupWait;
        }
        AppLogger.i(TAG, "No active route loaded, requesting route calculation");
        return NavigationSessionRouteState.Evaluation.requestRecalculation(
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );
    }

    @Nullable
    private NavigationSessionRouteState.Evaluation waitForAccurateStartupLocationIfNeeded(
            @NonNull Location filtered,
            float accuracyMeters,
            long nowMs
    ) {
        if (NavigationStartupLocationSelector.isUsableForRouteStart(filtered, nowMs)) {
            return null;
        }
        AppLogger.i(TAG, "Waiting for accurate startup location before first route calculation"
                + " accuracyMeters=" + accuracyMeters);
        return NavigationSessionRouteState.Evaluation.keepRoute(
                Collections.emptyList(),
                STARTUP_LOCATION_WAIT_INTERVAL_MS,
                false
        );
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

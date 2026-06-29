package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.startup.NavigationStartupLocationSelector;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.logging.AppLogger;

final class NavigationRouteEvaluator {
    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;
    private static final long STARTUP_LOCATION_WAIT_INTERVAL_MS =
            NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS;
    private static final long REACQUISITION_FOLLOW_UP_INTERVAL_MS = 3_000L;
    private static final long ROUTE_START_APPROACH_INTERVAL_MS = 3_000L;

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
    @NonNull
    private final RouteStartApproachState routeStartApproachState;

    NavigationRouteEvaluator(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationSessionRouteDisplayState displayState,
            @NonNull NavigationArrivalDetector arrivalDetector,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker,
            @NonNull RouteStartApproachState routeStartApproachState
    ) {
        this.geometryState = geometryState;
        this.turnState = turnState;
        this.progressTracker = progressTracker;
        this.deviationHandler = deviationHandler;
        this.displayState = displayState;
        this.arrivalDetector = arrivalDetector;
        this.intermediateArrivalTracker = intermediateArrivalTracker;
        this.routeStartApproachState = routeStartApproachState;
    }

    @NonNull
    NavigationRouteEvaluation evaluateLocation(
            @NonNull NavigationLocation filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs,
            boolean reacquiringAfterLongGap
    ) {
        if (geometryState.isRouteUnavailable()) {
            return evaluateUnavailableRoute(filtered, accuracyMeters, nowMs);
        }

        PolylineIndex.Match match = geometryState.match(filtered, accuracyMeters);
        if (match == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            return NavigationRouteEvaluation.requestRecalculation(
                    null,
                    NavigationRouteRecalculationReason.ROUTE_MATCH_FAILED
            );
        }
        if (reacquiringAfterLongGap) {
            return keepRouteWhileReacquiring(
                    filtered,
                    match,
                    speedMps,
                    accuracyMeters,
                    likelyStationary,
                    nowMs,
                    fastChecksUntilMs
            );
        }
        return evaluateMatchedRoute(
                filtered,
                match,
                speedMps,
                likelyStationary,
                accuracyMeters,
                actualBearingDegrees,
                nowMs,
                fastChecksUntilMs
        );
    }

    @NonNull
    private NavigationRouteEvaluation evaluateMatchedRoute(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match match,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        double smoothedAccuracyMeters = progressTracker.rememberAndResolveSmoothedAccuracyMeters(accuracyMeters, nowMs);
        float trustedAccuracyMeters = (float) smoothedAccuracyMeters;
        displayState.rememberSmoothedAccuracyMeters(trustedAccuracyMeters);
        NavigationRouteEvaluation routeStartApproach =
                evaluateRouteStartApproachIfNeeded(filtered, match, speedMps, likelyStationary, trustedAccuracyMeters);
        if (routeStartApproach != null) {
            return routeStartApproach;
        }
        geometryState.rememberSegment(match);
        double expectedBearingDegrees = geometryState.expectedBearingDegrees(match);
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
        if (arrivalDetector.isDestinationReached(filtered, trustedAccuracyMeters, match)) {
            deviationHandler.clearDeviationEvidence();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return NavigationRouteEvaluation.keepRoute(
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
            return NavigationRouteEvaluation.keepRoute(
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
            return NavigationRouteEvaluation.requestRecalculation(
                    deviationDecision.getRerouteNotice(),
                    NavigationRouteRecalculationReason.ROUTE_DEVIATION
            );
        }
        if (deviationDecision.shouldKeepCurrentRoute()) {
            return keepCurrentRoute(
                    match,
                    etaSpeedMps,
                    nowMs,
                    fastChecksUntilMs,
                    deviationDecision.isStableOnRouteSample()
            );
        }

        progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
        return keepCurrentRoute(match, etaSpeedMps, nowMs, fastChecksUntilMs, true);
    }

    @Nullable
    private NavigationRouteEvaluation evaluateRouteStartApproachIfNeeded(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match match,
            float speedMps,
            boolean likelyStationary,
            float trustedAccuracyMeters
    ) {
        if (!routeStartApproachState.isActive()) {
            return null;
        }
        if (routeStartApproachState.isReached(match, trustedAccuracyMeters)) {
            routeStartApproachState.reset();
            displayState.clearRouteStartApproachTarget();
            geometryState.rememberSegment(match);
            return NavigationRouteEvaluation.keepRoute(
                    turnState.buildInitialTurnEventIfNeeded(
                            geometryState.route(),
                            geometryState.polylineIndex(),
                            new LatLon(filtered.getLatitude(), filtered.getLongitude()),
                            likelyStationary ? 0f : speedMps,
                            trustedAccuracyMeters
                    ),
                    ROUTE_START_APPROACH_INTERVAL_MS,
                    true
            );
        }
        if (routeStartApproachState.shouldRefreshRouteStart(filtered)) {
            AppLogger.i(TAG, "Refreshing route-start approach after improved startup NavigationLocation");
            return NavigationRouteEvaluation.requestRecalculation(
                    null,
                    NavigationRouteRecalculationReason.STARTUP_ROUTE_REFRESH
            );
        }
        deviationHandler.clearDeviationEvidence();
        return NavigationRouteEvaluation.keepRoute(
                Collections.emptyList(),
                ROUTE_START_APPROACH_INTERVAL_MS,
                false
        );
    }

    @NonNull
    private NavigationRouteEvaluation keepRouteWhileReacquiring(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match match,
            float speedMps,
            float accuracyMeters,
            boolean likelyStationary,
            long nowMs,
            long fastChecksUntilMs
    ) {
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        double smoothedAccuracyMeters = progressTracker.rememberAndResolveSmoothedAccuracyMeters(accuracyMeters, nowMs);
        float trustedAccuracyMeters = (float) smoothedAccuracyMeters;
        displayState.rememberSmoothedAccuracyMeters(trustedAccuracyMeters);
        double offTrackThresholdMeters = RouteDeviationPolicy.resolveOffTrackThresholdMeters(trustedAccuracyMeters);
        boolean trustedRouteMatch = rememberTrustedRouteMatchForReacquisition(match, offTrackThresholdMeters);
        NavigationRouteEvaluation reachedTarget =
                reachedTargetWhileReacquiring(filtered, match, trustedAccuracyMeters, trustedRouteMatch);
        if (reachedTarget != null) {
            return reachedTarget;
        }
        if (!trustedRouteMatch) {
            return NavigationRouteEvaluation.keepRoute(
                    Collections.emptyList(),
                    REACQUISITION_FOLLOW_UP_INTERVAL_MS,
                    false
            );
        }
        progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
        return keepCurrentRoute(
                match,
                likelyStationary ? 0f : speedMps,
                nowMs,
                fastChecksUntilMs,
                false
        );
    }

    private boolean rememberTrustedRouteMatchForReacquisition(
            @NonNull PolylineIndex.Match match,
            double offTrackThresholdMeters
    ) {
        if (match.distanceToTrackMeters <= offTrackThresholdMeters) {
            geometryState.rememberSegment(match);
            return true;
        }
        AppLogger.i(TAG, "Holding route segment memory during NavigationLocation reacquisition distance="
                + match.distanceToTrackMeters
                + " threshold=" + offTrackThresholdMeters);
        return false;
    }

    @Nullable
    private NavigationRouteEvaluation reachedTargetWhileReacquiring(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match match,
            float trustedAccuracyMeters,
            boolean trustedRouteMatch
    ) {
        if (!trustedRouteMatch) {
            return null;
        }
        if (arrivalDetector.isDestinationReached(filtered, trustedAccuracyMeters, match)) {
            return NavigationRouteEvaluation.keepRoute(
                    turnState.onDestinationReached(geometryState.route()),
                    NO_SUGGESTED_INTERVAL,
                    false
            );
        }
        Integer reachedIntermediateTrackIndex = intermediateArrivalTracker.reachedTrackIndex(
                filtered,
                trustedAccuracyMeters
        );
        return reachedIntermediateTrackIndex == null
                ? null
                : NavigationRouteEvaluation.keepRoute(
                        turnState.onIntermediateDestinationReached(reachedIntermediateTrackIndex),
                        NO_SUGGESTED_INTERVAL,
                        false
                );
    }

    @NonNull
    private NavigationRouteEvaluation evaluateUnavailableRoute(
            @NonNull NavigationLocation filtered,
            float accuracyMeters,
            long nowMs
    ) {
        NavigationRouteEvaluation startupWait =
                waitForAccurateStartupLocationIfNeeded(filtered, accuracyMeters, nowMs);
        if (startupWait != null) {
            return startupWait;
        }
        AppLogger.i(TAG, "No active route loaded, requesting route calculation");
        return NavigationRouteEvaluation.requestRecalculation(
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );
    }

    @Nullable
    private NavigationRouteEvaluation waitForAccurateStartupLocationIfNeeded(
            @NonNull NavigationLocation filtered,
            float accuracyMeters,
            long nowMs
    ) {
        if (NavigationStartupLocationSelector.isUsableForRouteStart(filtered, nowMs)) {
            return null;
        }
        AppLogger.i(TAG, "Waiting for accurate startup NavigationLocation before first route calculation"
                + " accuracyMeters=" + accuracyMeters);
        return NavigationRouteEvaluation.keepRoute(
                Collections.emptyList(),
                STARTUP_LOCATION_WAIT_INTERVAL_MS,
                false
        );
    }

    @NonNull
    private NavigationRouteEvaluation keepCurrentRoute(
            @NonNull PolylineIndex.Match match,
            float etaSpeedMps,
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
                nowMs,
                fastChecksUntilMs
        );
        return NavigationRouteEvaluation.keepRoute(
                progress.turnEvents,
                progress.suggestedUpdateIntervalMs,
                stableOnRouteSample
        );
    }
}

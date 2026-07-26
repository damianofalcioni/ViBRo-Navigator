package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

final class NavigationRouteDirectGuidanceEvaluator {
    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;
    private static final long DIRECT_GUIDANCE_INTERVAL_MS = 3_000L;

    @NonNull
    private final NavigationRouteGeometryState geometryState;
    @NonNull
    private final NavigationTurnState turnState;
    @NonNull
    private final NavigationRouteProgressTracker progressTracker;
    @NonNull
    private final NavigationRouteDeviationHandler deviationHandler;
    @NonNull
    private final NavigationArrivalDetector arrivalDetector;
    @NonNull
    private final NavigationIntermediateArrivalTracker intermediateArrivalTracker;
    @NonNull
    private final NavigationRouteDirectGuidanceState directGuidanceState;
    @NonNull
    private final NavigationRouteHistory routeHistory;

    NavigationRouteDirectGuidanceEvaluator(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationArrivalDetector arrivalDetector,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker,
            @NonNull NavigationRouteDirectGuidanceState directGuidanceState,
            @NonNull NavigationRouteHistory routeHistory
    ) {
        this.geometryState = geometryState;
        this.turnState = turnState;
        this.progressTracker = progressTracker;
        this.deviationHandler = deviationHandler;
        this.arrivalDetector = arrivalDetector;
        this.intermediateArrivalTracker = intermediateArrivalTracker;
        this.directGuidanceState = directGuidanceState;
        this.routeHistory = routeHistory;
    }

    @NonNull
    PolylineIndex.Match constrainRouteMatch(
            @NonNull NavigationLocation location,
            @NonNull PolylineIndex.Match fallbackMatch
    ) {
        return directGuidanceState.constrainRouteMatch(location, fallbackMatch);
    }

    boolean isRouteBeelineActive() {
        return directGuidanceState.isRouteBeelineActive();
    }

    @Nullable
    NavigationRouteEvaluation evaluateIfNeeded(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match routeMatch,
            float speedMps,
            boolean likelyStationary,
            float trustedAccuracyMeters,
            long nowMs,
            long fastChecksUntilMs,
            boolean singleInstructionMode
    ) {
        NavigationRouteEvaluation routeStartApproach = evaluateRouteStartApproachIfNeeded(
                filtered,
                routeMatch,
                speedMps,
                likelyStationary,
                trustedAccuracyMeters,
                singleInstructionMode
        );
        return routeStartApproach != null
                ? routeStartApproach
                : evaluateRouteBeelineIfNeeded(
                        filtered,
                        routeMatch,
                        speedMps,
                        likelyStationary,
                        trustedAccuracyMeters,
                        nowMs,
                        fastChecksUntilMs,
                        singleInstructionMode
                );
    }

    @Nullable
    private NavigationRouteEvaluation evaluateRouteStartApproachIfNeeded(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match match,
            float speedMps,
            boolean likelyStationary,
            float trustedAccuracyMeters,
            boolean singleInstructionMode
    ) {
        if (!directGuidanceState.isRouteStartApproachActive()) {
            return null;
        }
        if (directGuidanceState.isRouteStartApproachReached(match, trustedAccuracyMeters)) {
            directGuidanceState.clearRouteStartApproach();
            geometryState.rememberSegment(match);
            routeHistory.recordProgress(match);
            return NavigationRouteEvaluation.keepRoute(
                    NavigationInitialTurnEvents.suppressForSingleInstructionMode(
                            turnState.buildInitialTurnEventIfNeeded(
                                    geometryState.route(),
                                    geometryState.polylineIndex(),
                                    new LatLon(filtered.getLatitude(), filtered.getLongitude()),
                                    likelyStationary ? 0f : speedMps,
                                    trustedAccuracyMeters
                            ),
                            singleInstructionMode
                    ),
                    DIRECT_GUIDANCE_INTERVAL_MS,
                    true
            );
        }
        if (directGuidanceState.shouldRefreshRouteStart(filtered)) {
            AppLogger.i(TAG, "Refreshing route-start approach after improved startup NavigationLocation");
            return NavigationRouteEvaluation.requestRecalculation(
                    null,
                    NavigationRouteRecalculationReason.STARTUP_ROUTE_REFRESH
            );
        }
        deviationHandler.clearDeviationEvidence();
        return NavigationRouteEvaluation.keepRoute(
                Collections.emptyList(),
                DIRECT_GUIDANCE_INTERVAL_MS,
                false
        );
    }

    @Nullable
    private NavigationRouteEvaluation evaluateRouteBeelineIfNeeded(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match routeMatch,
            float speedMps,
            boolean likelyStationary,
            float trustedAccuracyMeters,
            long nowMs,
            long fastChecksUntilMs,
            boolean singleInstructionMode
    ) {
        double reachedRadiusMeters =
                NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(trustedAccuracyMeters);
        directGuidanceState.activateRouteBeelineIfReached(
                routeMatch,
                filtered,
                reachedRadiusMeters
        );
        if (!directGuidanceState.isRouteBeelineActive()) {
            return null;
        }
        PolylineIndex.Match completedMatch = directGuidanceState.completeRouteBeelineIfReached(
                filtered,
                reachedRadiusMeters
        );
        deviationHandler.clearDeviationEvidence();
        if (completedMatch == null) {
            return NavigationRouteEvaluation.keepRoute(
                    Collections.emptyList(),
                    DIRECT_GUIDANCE_INTERVAL_MS,
                    false
            );
        }
        rememberCompletedBeeline(completedMatch, nowMs);
        if (arrivalDetector.isDestinationReached(filtered, trustedAccuracyMeters, completedMatch)) {
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
            return NavigationRouteEvaluation.keepRoute(
                    turnState.onIntermediateDestinationReached(reachedIntermediateTrackIndex),
                    DIRECT_GUIDANCE_INTERVAL_MS,
                    true
            );
        }
        return keepCurrentRouteAfterCompletedBeeline(
                completedMatch,
                likelyStationary ? 0f : speedMps,
                nowMs,
                fastChecksUntilMs,
                singleInstructionMode
        );
    }

    private void rememberCompletedBeeline(@NonNull PolylineIndex.Match completedMatch, long nowMs) {
        geometryState.rememberSegment(completedMatch);
        progressTracker.rememberAlongTrackSample(completedMatch.alongTrackMeters, nowMs);
        routeHistory.recordProgress(completedMatch);
    }

    @NonNull
    private NavigationRouteEvaluation keepCurrentRouteAfterCompletedBeeline(
            @NonNull PolylineIndex.Match match,
            float speedMps,
            long nowMs,
            long fastChecksUntilMs,
            boolean singleInstructionMode
    ) {
        NavigationTurnState.Progress progress = turnState.evaluate(
                geometryState.route(),
                geometryState.polylineIndex(),
                match.alongTrackMeters,
                match.segmentIndex,
                speedMps,
                Float.NaN,
                nowMs,
                fastChecksUntilMs,
                singleInstructionMode
        );
        return NavigationRouteEvaluation.keepRoute(
                progress.turnEvents,
                progress.suggestedUpdateIntervalMs,
                true
        );
    }
}

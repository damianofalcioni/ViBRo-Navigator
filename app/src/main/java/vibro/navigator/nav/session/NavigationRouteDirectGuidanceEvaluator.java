package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationRouteDirectGuidanceEvaluator {
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

    boolean shouldHoldRouteDeviationWhileStationary(boolean likelyStationary) {
        return directGuidanceState.shouldHoldRouteDeviationWhileStationary(likelyStationary);
    }

    @Nullable
    NavigationRouteEvaluation evaluateIfNeeded(
            @NonNull NavigationLocation filtered,
            @NonNull PolylineIndex.Match routeMatch,
            float speedMps,
            boolean likelyStationary,
            float currentAccuracyMeters,
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
                currentAccuracyMeters,
                trustedAccuracyMeters,
                nowMs,
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
            float currentAccuracyMeters,
            float trustedAccuracyMeters,
            long nowMs,
            boolean singleInstructionMode
    ) {
        if (!directGuidanceState.isRouteStartApproachActive()) {
            return null;
        }
        if (directGuidanceState.isRouteStartApproachReached(match, currentAccuracyMeters)) {
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
        deviationHandler.clearDeviationEvidence();
        return directGuidanceState.recovery.evaluate(directGuidanceState.activeDirectTarget(),
                filtered, likelyStationary, nowMs);
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
        boolean wasActive = directGuidanceState.isRouteBeelineActive();
        directGuidanceState.activateRouteBeelineIfReached(
                routeMatch,
                filtered,
                reachedRadiusMeters
        );
        if (!directGuidanceState.isRouteBeelineActive()) {
            return null;
        }
        PolylineIndex.Match routeBoundary =
                directGuidanceState.activeRouteBeelineProgressMatch();
        if (!wasActive) {
            PolylineIndex.Match immediatelyCompleted = completeReachableBeelines(
                    filtered,
                    reachedRadiusMeters
            );
            if (immediatelyCompleted != null) {
                rememberCompletedBeelineProgress(immediatelyCompleted, nowMs);
                routeHistory.skipDirectLegs(
                        travelledMatchBeforeDirectLeg(routeMatch, routeBoundary),
                        immediatelyCompleted
                );
                startFollowingDirectLegFromCurrentFix(filtered);
                deviationHandler.clearDeviationEvidence();
                return evaluateAfterCompletedBeeline(
                        immediatelyCompleted,
                        filtered,
                        speedMps,
                        likelyStationary,
                        trustedAccuracyMeters,
                        nowMs,
                        fastChecksUntilMs,
                        singleInstructionMode
                );
            }
        }
        routeHistory.startDirectLeg(
                filtered,
                routeBoundary,
                directGuidanceState.activeRouteBeelineTargetMatch(),
                reachedRadiusMeters
        );
        PolylineIndex.Match completedMatch = completeReachableBeelines(
                filtered,
                reachedRadiusMeters
        );
        deviationHandler.clearDeviationEvidence();
        if (completedMatch == null) {
            return directGuidanceState.recovery.evaluate(directGuidanceState.activeDirectTarget(),
                    filtered, likelyStationary, nowMs);
        }
        rememberCompletedBeeline(completedMatch, nowMs);
        startFollowingDirectLeg(filtered, reachedRadiusMeters);
        return evaluateAfterCompletedBeeline(
                completedMatch,
                filtered,
                speedMps,
                likelyStationary,
                trustedAccuracyMeters,
                nowMs,
                fastChecksUntilMs,
                singleInstructionMode
        );
    }

    @Nullable
    private PolylineIndex.Match completeReachableBeelines(
            @NonNull NavigationLocation filtered,
            double reachedRadiusMeters
    ) {
        PolylineIndex.Match completedMatch = null;
        while (directGuidanceState.isRouteBeelineActive()) {
            PolylineIndex.Match nextCompleted =
                    directGuidanceState.completeRouteBeelineIfReached(
                            filtered,
                            reachedRadiusMeters
                    );
            if (nextCompleted == null) {
                return completedMatch;
            }
            completedMatch = nextCompleted;
        }
        return completedMatch;
    }

    @NonNull
    private static PolylineIndex.Match travelledMatchBeforeDirectLeg(
            @NonNull PolylineIndex.Match routeMatch,
            @NonNull PolylineIndex.Match routeBoundary
    ) {
        return routeMatch.alongTrackMeters <= routeBoundary.alongTrackMeters
                ? routeMatch
                : routeBoundary;
    }

    private void startFollowingDirectLegFromCurrentFix(
            @NonNull NavigationLocation filtered
    ) {
        PolylineIndex.Match target = directGuidanceState.activeRouteBeelineTargetMatch();
        if (target != null) {
            routeHistory.startDirectLegFromCurrentFix(filtered, target);
        }
    }

    private void startFollowingDirectLeg(
            @NonNull NavigationLocation filtered,
            double reachedRadiusMeters
    ) {
        PolylineIndex.Match progress = directGuidanceState.activeRouteBeelineProgressMatch();
        PolylineIndex.Match target = directGuidanceState.activeRouteBeelineTargetMatch();
        if (progress != null && target != null) {
            routeHistory.startDirectLeg(filtered, progress, target, reachedRadiusMeters);
        }
    }

    @NonNull
    private NavigationRouteEvaluation evaluateAfterCompletedBeeline(
            @NonNull PolylineIndex.Match completedMatch,
            @NonNull NavigationLocation filtered,
            float speedMps,
            boolean likelyStationary,
            float trustedAccuracyMeters,
            long nowMs,
            long fastChecksUntilMs,
            boolean singleInstructionMode
    ) {
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
                    !directGuidanceState.isRouteBeelineActive()
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
        rememberCompletedBeelineProgress(completedMatch, nowMs);
        routeHistory.recordProgress(completedMatch);
    }

    private void rememberCompletedBeelineProgress(
            @NonNull PolylineIndex.Match completedMatch,
            long nowMs
    ) {
        geometryState.rememberSegment(completedMatch);
        progressTracker.rememberAlongTrackSample(completedMatch.alongTrackMeters, nowMs);
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
                !directGuidanceState.isRouteBeelineActive()
        );
    }
}

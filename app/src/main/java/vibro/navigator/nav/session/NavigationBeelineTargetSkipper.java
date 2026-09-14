package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

/** Advances routed direct guidance when the user explicitly declines its current target. */
final class NavigationBeelineTargetSkipper {
    @NonNull
    private final NavigationRouteGeometryState geometryState;
    @NonNull
    private final NavigationTurnState turnState;
    @NonNull
    private final NavigationRouteProgressTracker progressTracker;
    @NonNull
    private final NavigationRouteDeviationHandler deviationHandler;
    @NonNull
    private final NavigationIntermediateArrivalTracker intermediateArrivalTracker;
    @NonNull
    private final NavigationRouteDirectGuidanceState directGuidanceState;
    @NonNull
    private final NavigationRouteHistory routeHistory;

    NavigationBeelineTargetSkipper(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker,
            @NonNull NavigationRouteDirectGuidanceState directGuidanceState,
            @NonNull NavigationRouteHistory routeHistory
    ) {
        this.geometryState = geometryState;
        this.turnState = turnState;
        this.progressTracker = progressTracker;
        this.deviationHandler = deviationHandler;
        this.intermediateArrivalTracker = intermediateArrivalTracker;
        this.directGuidanceState = directGuidanceState;
        this.routeHistory = routeHistory;
    }

    boolean skipActiveTarget(@Nullable NavigationLocation filtered, long nowMs) {
        if (directGuidanceState.skipRouteStartApproach()) {
            resetProgressEvidence();
            return true;
        }
        if (!directGuidanceState.isRouteBeelineActive()) {
            return false;
        }
        PolylineIndex.Match routeBoundary = directGuidanceState.activeRouteBeelineProgressMatch();
        if (routeBoundary == null) {
            return false;
        }
        PolylineIndex.Match skippedTarget =
                directGuidanceState.skipActiveRouteBeelineTarget(filtered);
        if (skippedTarget == null) {
            return false;
        }
        resetAfterRouteTargetSkip(skippedTarget, nowMs);
        updateTravelHistory(routeBoundary, skippedTarget);
        startFollowingTarget(filtered);
        advanceTurnGuidance(skippedTarget, nowMs);
        return true;
    }

    private void resetProgressEvidence() {
        progressTracker.reset();
        deviationHandler.clearDeviationEvidence();
    }

    private void resetAfterRouteTargetSkip(@NonNull PolylineIndex.Match skippedTarget, long nowMs) {
        directGuidanceState.notifications.reset();
        directGuidanceState.recovery.setTarget(directGuidanceState.activeDirectTarget());
        resetProgressEvidence();
        progressTracker.rememberAlongTrackSample(skippedTarget.alongTrackMeters, nowMs);
        geometryState.rememberSegment(skippedTarget);
    }

    private void updateTravelHistory(
            @NonNull PolylineIndex.Match routeBoundary,
            @NonNull PolylineIndex.Match skippedTarget
    ) {
        if (routeHistory.hasPendingRerouteFixPath()) {
            routeHistory.recordProgress(skippedTarget);
        } else {
            routeHistory.skipDirectLegs(routeBoundary, skippedTarget);
        }
    }

    private void startFollowingTarget(@Nullable NavigationLocation filtered) {
        if (filtered == null || !directGuidanceState.isRouteBeelineActive()) {
            return;
        }
        PolylineIndex.Match progress = directGuidanceState.activeRouteBeelineProgressMatch();
        PolylineIndex.Match target = directGuidanceState.activeRouteBeelineTargetMatch();
        if (progress == null || target == null) {
            return;
        }
        float accuracyMeters = filtered.hasAccuracy() ? filtered.getAccuracy() : Float.MAX_VALUE;
        routeHistory.startDirectLeg(
                filtered,
                progress,
                target,
                NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyMeters)
        );
    }

    private void advanceTurnGuidance(@NonNull PolylineIndex.Match skippedTarget, long nowMs) {
        Integer skippedIntermediate =
                intermediateArrivalTracker.skipCurrentAtTrackIndex(skippedTarget.segmentIndex);
        if (skippedIntermediate != null) {
            turnState.onIntermediateDestinationReached(skippedIntermediate);
            return;
        }
        PolylineIndex polylineIndex = geometryState.polylineIndex();
        GeoJsonRoute route = geometryState.route();
        if (polylineIndex == null || route == null) {
            return;
        }
        if (skippedTarget.alongTrackMeters >= polylineIndex.totalLengthMeters()) {
            directGuidanceState.markDestinationManuallySkipped();
            turnState.onDestinationReached(route);
            return;
        }
        turnState.evaluate(
                route,
                polylineIndex,
                skippedTarget.alongTrackMeters,
                skippedTarget.segmentIndex,
                0f,
                Float.NaN,
                nowMs,
                0L,
                false
        );
    }
}

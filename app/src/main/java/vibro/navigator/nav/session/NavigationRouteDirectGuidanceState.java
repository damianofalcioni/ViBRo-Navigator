package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.BeelineNotificationTracker;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteStartApproach;

final class NavigationRouteDirectGuidanceState {
    final NavigationBeelineRecoveryState recovery = new NavigationBeelineRecoveryState();
    final BeelineNotificationTracker notifications = new BeelineNotificationTracker();
    @NonNull
    private final RouteStartApproachState routeStartApproachState = new RouteStartApproachState();
    @NonNull
    private final NavigationRouteBeelineState routeBeelineState = new NavigationRouteBeelineState();
    private boolean routeStartApproachCompletionPending;

    void reset() {
        routeStartApproachState.reset();
        routeBeelineState.reset();
        recovery.reset();
        notifications.reset();
        routeStartApproachCompletionPending = false;
    }

    void applyRouteStartApproach(
            @NonNull RouteStartApproach.Plan plan,
            boolean allowRecovery
    ) {
        routeStartApproachState.apply(plan);
        routeStartApproachCompletionPending = false;
        recovery.onRouteApplied(allowRecovery);
        notifications.reset();
    }

    boolean isRouteStartApproachActive() {
        return routeStartApproachState.isActive();
    }

    boolean isRouteStartApproachReached(
            @NonNull PolylineIndex.Match match,
            double accuracyMeters
    ) {
        return routeStartApproachState.isReached(match, accuracyMeters);
    }

    void clearMotionEvidence() {
        recovery.clearEvidence();
        notifications.reset();
    }

    void clearRouteStartApproach() {
        if (!routeStartApproachState.isActive()) {
            return;
        }
        routeStartApproachState.reset();
        routeStartApproachCompletionPending = true;
        // Invalidate a recovery result that may already be in flight.  The
        // route-start corridor has just been accepted, so it must not be
        // allowed to replace the route after this transition.
        recovery.clearEvidence();
    }

    boolean shouldHoldRouteDeviationWhileStationary(boolean likelyStationary) {
        if (!routeStartApproachCompletionPending) {
            return false;
        }
        if (!likelyStationary) {
            routeStartApproachCompletionPending = false;
            return false;
        }
        return true;
    }

    void onRouteApplied(@NonNull GeoJsonRoute route, @NonNull PolylineIndex polylineIndex) {
        routeBeelineState.onRouteApplied(route, polylineIndex);
    }

    boolean activateRouteBeelineIfReached(
            @NonNull PolylineIndex.Match routeMatch,
            @NonNull NavigationLocation location,
            double reachedRadiusMeters
    ) {
        return routeBeelineState.activateIfReached(routeMatch, location, reachedRadiusMeters);
    }

    boolean isRouteBeelineActive() {
        return routeBeelineState.isActive();
    }

    @Nullable
    PolylineIndex.Match completeRouteBeelineIfReached(
            @NonNull NavigationLocation location,
            double reachedRadiusMeters
    ) {
        return routeBeelineState.completeIfReached(location, reachedRadiusMeters);
    }

    @NonNull
    PolylineIndex.Match constrainRouteMatch(
            @NonNull NavigationLocation location,
            @NonNull PolylineIndex.Match fallbackMatch
    ) {
        return routeBeelineState.constrainRouteMatch(location, fallbackMatch);
    }

    @Nullable
    LatLon activeDirectTarget() {
        LatLon routeStartTarget = routeStartApproachState.target();
        return routeStartTarget != null ? routeStartTarget : routeBeelineState.target();
    }

    @Nullable
    PolylineIndex.Match activeRouteBeelineProgressMatch() {
        return routeBeelineState.progressMatch();
    }

    @Nullable
    PolylineIndex.Match activeRouteBeelineTargetMatch() {
        return routeBeelineState.targetMatch();
    }

    @Nullable
    Double bearingDegreesFrom(@Nullable NavigationLocation location) {
        Double routeStartBearing = routeStartApproachState.bearingDegreesFrom(location);
        return routeStartBearing != null
                ? routeStartBearing
                : routeBeelineState.bearingDegreesFrom(location);
    }
}

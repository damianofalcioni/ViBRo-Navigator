package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteStartApproach;

final class NavigationRouteDirectGuidanceState {
    @NonNull
    private final RouteStartApproachState routeStartApproachState = new RouteStartApproachState();
    @NonNull
    private final NavigationRouteBeelineState routeBeelineState = new NavigationRouteBeelineState();

    void reset() {
        routeStartApproachState.reset();
        routeBeelineState.reset();
    }

    void applyRouteStartApproach(
            @NonNull RouteStartApproach.Plan plan,
            @Nullable NavigationLocation requestLocation,
            boolean allowStartupRefresh
    ) {
        routeStartApproachState.apply(plan, requestLocation, allowStartupRefresh);
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

    boolean shouldRefreshRouteStart(@NonNull NavigationLocation location) {
        return routeStartApproachState.shouldRefreshRouteStart(location);
    }

    void clearRouteStartApproach() {
        routeStartApproachState.reset();
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
    Double bearingDegreesFrom(@Nullable NavigationLocation location) {
        Double routeStartBearing = routeStartApproachState.bearingDegreesFrom(location);
        return routeStartBearing != null
                ? routeStartBearing
                : routeBeelineState.bearingDegreesFrom(location);
    }
}

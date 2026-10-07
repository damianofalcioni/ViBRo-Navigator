package vibro.navigator.nav.route;


import vibro.navigator.nav.orientation.NavigationExpectedBearingResolver;
import vibro.navigator.nav.orientation.NavigationRouteDisplayHeadingResolver;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;

public final class NavigationRouteGeometryState {

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    @Nullable
    private NavigationRouteDisplayHeadingResolver displayHeadingResolver;
    private int lastSegmentIndex = -1;
    private boolean roundTripRoute;
    private boolean roundTripDepartureObserved;

    public void reset() {
        route = null;
        polylineIndex = null;
        displayHeadingResolver = null;
        lastSegmentIndex = -1;
        roundTripRoute = false;
        roundTripDepartureObserved = false;
    }

    public boolean hasActiveRoute() {
        return route != null;
    }

    public boolean isRouteUnavailable() {
        return route == null || polylineIndex == null || route.track.isEmpty();
    }

    public void loadRoute(@NonNull GeoJsonRoute newRoute) {
        loadRoute(newRoute, false);
    }

    public void loadRoute(@NonNull GeoJsonRoute newRoute, boolean roundTripRoute) {
        route = newRoute;
        polylineIndex = new PolylineIndex(newRoute.track);
        displayHeadingResolver = new NavigationRouteDisplayHeadingResolver(newRoute, polylineIndex);
        lastSegmentIndex = -1;
        this.roundTripRoute = roundTripRoute;
        roundTripDepartureObserved = false;
    }

    @Nullable
    public GeoJsonRoute route() {
        return route;
    }

    @Nullable
    public PolylineIndex polylineIndex() {
        return polylineIndex;
    }

    public void rememberSegment(@NonNull PolylineIndex.Match match) {
        lastSegmentIndex = match.segmentIndex;
    }

    @Nullable
    public PolylineIndex.Match match(@NonNull NavigationLocation location, float accuracyMeters) {
        if (polylineIndex == null) {
            return null;
        }
        LatLon point = new LatLon(location.getLatitude(), location.getLongitude());
        PolylineIndex.Match match = polylineIndex.match(point, lastSegmentIndex);
        if (match == null || !shouldUseRoundTripStartMatch()) {
            return match;
        }
        PolylineIndex.Match startMatch = polylineIndex.matchBeforeDistance(
                point,
                NavigationRouteGeometryMetrics.routeStartAnchorWindowMeters(route, polylineIndex, accuracyMeters)
        );
        return startMatch != null ? startMatch : match;
    }

    @Nullable
    public PolylineIndex.Match matchInitialRoutePart(@NonNull NavigationLocation location, float accuracyMeters) {
        if (polylineIndex == null) {
            return null;
        }
        LatLon point = new LatLon(location.getLatitude(), location.getLongitude());
        PolylineIndex.Match startMatch = polylineIndex.matchBeforeDistance(
                point,
                NavigationRouteGeometryMetrics.routeStartAnchorWindowMeters(route, polylineIndex, accuracyMeters)
        );
        return startMatch != null ? startMatch : polylineIndex.match(point, lastSegmentIndex);
    }

    public double expectedBearingDegrees(@NonNull PolylineIndex.Match match) {
        return NavigationExpectedBearingResolver.resolve(polylineIndex, match);
    }

    public double displayBearingDegrees(@NonNull PolylineIndex.Match match) {
        return displayHeadingResolver == null ? match.segmentBearingDegrees : displayHeadingResolver.resolve(match);
    }

    public static double resolveDestinationReachedRadiusMeters(float accuracyMeters) {
        return RouteDeviationPolicy.resolveOffTrackThresholdMeters(accuracyMeters);
    }

    public boolean isWithinDestinationReachedRadius(@NonNull NavigationLocation location, float accuracyMeters) {
        return isWithinDestinationReachedRadius(location, accuracyMeters, null);
    }

    public boolean isWithinDestinationReachedRadius(
            @NonNull NavigationLocation location,
            float accuracyMeters,
            @Nullable PolylineIndex.Match match
    ) {
        if (route == null || route.track.isEmpty()) {
            return false;
        }
        double destinationReachedRadiusMeters = resolveDestinationReachedRadiusMeters(accuracyMeters);
        if (!NavigationRouteGeometryMetrics.isInsideDestinationReachedRadius(route, location, accuracyMeters)) {
            rememberRoundTripDeparture(match, accuracyMeters, destinationReachedRadiusMeters);
            return false;
        }
        return !roundTripRoute
                || (roundTripDepartureObserved && isNearRouteEnd(match, destinationReachedRadiusMeters));
    }

    private boolean isNearRouteEnd(@Nullable PolylineIndex.Match match, double destinationReachedRadiusMeters) {
        if (route == null || match == null) {
            return false;
        }
        double routeLengthMeters = NavigationRouteGeometryMetrics.routeLengthMeters(route, polylineIndex);
        double endThresholdMeters = Math.max(destinationReachedRadiusMeters, 25.0);
        return routeLengthMeters <= endThresholdMeters
                || match.alongTrackMeters >= routeLengthMeters - endThresholdMeters;
    }

    private boolean shouldUseRoundTripStartMatch() {
        return roundTripRoute && !roundTripDepartureObserved;
    }

    private void rememberRoundTripDeparture(
            @Nullable PolylineIndex.Match match,
            float accuracyMeters,
            double destinationReachedRadiusMeters
    ) {
        if (!roundTripRoute || roundTripDepartureObserved || match == null) {
            return;
        }
        double routeStartAnchorWindowMeters =
                NavigationRouteGeometryMetrics.routeStartAnchorWindowMeters(route, polylineIndex, accuracyMeters);
        if (match.alongTrackMeters > routeStartAnchorWindowMeters
                && match.distanceToTrackMeters <= destinationReachedRadiusMeters) {
            roundTripDepartureObserved = true;
        }
    }
}

package vibro.navigator.nav.route;


import vibro.navigator.nav.orientation.NavigationExpectedBearingResolver;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;

public final class NavigationRouteGeometryState {

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;
    private boolean roundTripRoute;

    public void reset() {
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        roundTripRoute = false;
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
        lastSegmentIndex = -1;
        this.roundTripRoute = roundTripRoute;
    }

    @Nullable
    public GeoJsonRoute route() {
        return route;
    }

    @Nullable
    public PolylineIndex polylineIndex() {
        return polylineIndex;
    }

    public int lastSegmentIndex() {
        return lastSegmentIndex;
    }

    public void rememberSegment(@NonNull PolylineIndex.Match match) {
        lastSegmentIndex = match.segmentIndex;
    }

    @Nullable
    public PolylineIndex.Match match(@NonNull NavigationLocation location) {
        if (polylineIndex == null) {
            return null;
        }
        return polylineIndex.match(
                new LatLon(location.getLatitude(), location.getLongitude()),
                lastSegmentIndex
        );
    }

    @Nullable
    public Double currentSegmentBearingDegrees(@Nullable NavigationLocation lastFiltered) {
        if (lastFiltered == null || isRouteUnavailable()) {
            return null;
        }
        PolylineIndex.Match match = match(lastFiltered);
        return match == null ? null : expectedBearingDegrees(match);
    }

    public double expectedBearingDegrees(@NonNull PolylineIndex.Match match) {
        return NavigationExpectedBearingResolver.resolve(polylineIndex, match);
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
        LatLon destination = route.track.get(route.track.size() - 1);
        double destinationDistanceMeters = GeoMath.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                destination.lat,
                destination.lon
        );
        double destinationReachedRadiusMeters = resolveDestinationReachedRadiusMeters(accuracyMeters);
        if (destinationDistanceMeters > destinationReachedRadiusMeters) {
            return false;
        }
        return !roundTripRoute || isNearRouteEnd(match, destinationReachedRadiusMeters);
    }

    private boolean isNearRouteEnd(@Nullable PolylineIndex.Match match, double destinationReachedRadiusMeters) {
        if (route == null || match == null) {
            return false;
        }
        double routeLengthMeters = Math.max(0.0, route.trackLengthMeters);
        double endThresholdMeters = Math.max(destinationReachedRadiusMeters, 25.0);
        return routeLengthMeters <= endThresholdMeters
                || match.alongTrackMeters >= routeLengthMeters - endThresholdMeters;
    }
}

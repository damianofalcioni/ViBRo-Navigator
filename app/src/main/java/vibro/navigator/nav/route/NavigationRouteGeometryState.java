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

    public void reset() {
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
    }

    public boolean hasActiveRoute() {
        return route != null;
    }

    public boolean isRouteUnavailable() {
        return route == null || polylineIndex == null || route.track.isEmpty();
    }

    public void loadRoute(@NonNull GeoJsonRoute newRoute) {
        route = newRoute;
        polylineIndex = new PolylineIndex(newRoute.track);
        lastSegmentIndex = -1;
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
        return destinationDistanceMeters <= destinationReachedRadiusMeters;
    }
}

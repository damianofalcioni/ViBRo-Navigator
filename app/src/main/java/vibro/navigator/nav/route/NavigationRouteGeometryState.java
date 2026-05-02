package vibro.navigator.nav.route;


import vibro.navigator.nav.orientation.NavigationExpectedBearingResolver;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

public final class NavigationRouteGeometryState {

    private static final double MIN_DESTINATION_REACHED_RADIUS_METERS = 5.0;

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
    public PolylineIndex.Match match(@NonNull Location location) {
        if (polylineIndex == null) {
            return null;
        }
        return polylineIndex.match(
                new LatLon(location.getLatitude(), location.getLongitude()),
                lastSegmentIndex
        );
    }

    @Nullable
    public Double currentSegmentBearingDegrees(@Nullable Location lastFiltered) {
        if (lastFiltered == null || isRouteUnavailable()) {
            return null;
        }
        PolylineIndex.Match match = match(lastFiltered);
        return match == null ? null : expectedBearingDegrees(match);
    }

    public double expectedBearingDegrees(@NonNull PolylineIndex.Match match) {
        return NavigationExpectedBearingResolver.resolve(polylineIndex, match);
    }

    public boolean isWithinDestinationReachedRadius(@NonNull Location location, float accuracyMeters) {
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
        double destinationReachedRadiusMeters = Math.max(
                MIN_DESTINATION_REACHED_RADIUS_METERS,
                Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters : 0.0
        );
        return destinationDistanceMeters <= destinationReachedRadiusMeters;
    }
}

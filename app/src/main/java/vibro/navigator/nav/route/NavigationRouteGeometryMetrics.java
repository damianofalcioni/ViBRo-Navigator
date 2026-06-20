package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;

final class NavigationRouteGeometryMetrics {
    private NavigationRouteGeometryMetrics() {
    }

    static boolean isInsideDestinationReachedRadius(
            @NonNull GeoJsonRoute route,
            @NonNull NavigationLocation location,
            float accuracyMeters
    ) {
        if (route.track.isEmpty()) {
            return false;
        }
        LatLon destination = route.track.get(route.track.size() - 1);
        double destinationDistanceMeters = GeoMath.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                destination.lat,
                destination.lon
        );
        return destinationDistanceMeters
                <= NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyMeters);
    }

    static double routeStartAnchorWindowMeters(
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            float accuracyMeters
    ) {
        double preferredWindowMeters = Math.max(
                NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyMeters),
                25.0
        );
        double routeLengthMeters = routeLengthMeters(route, polylineIndex);
        if (routeLengthMeters > 0.0) {
            preferredWindowMeters = Math.min(preferredWindowMeters, routeLengthMeters / 2.0);
        }
        return Math.max(0.0, preferredWindowMeters);
    }

    static double routeLengthMeters(@Nullable GeoJsonRoute route, @Nullable PolylineIndex polylineIndex) {
        if (route == null) {
            return 0.0;
        }
        if (Double.isFinite(route.trackLengthMeters) && route.trackLengthMeters > 0.0) {
            return route.trackLengthMeters;
        }
        return polylineIndex == null ? 0.0 : polylineIndex.totalLengthMeters();
    }
}

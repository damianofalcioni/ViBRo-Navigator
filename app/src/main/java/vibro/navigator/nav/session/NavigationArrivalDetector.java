package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.geo.LatLon;
import vibro.navigator.geo.GeoMath;

final class NavigationArrivalDetector {
    @NonNull
    private final NavigationRouteGeometryState geometryState;
    private final NavigationIntermediateArrivalTracker intermediateArrivals;

    NavigationArrivalDetector(@NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivals) {
        this.geometryState = geometryState;
        this.intermediateArrivals = intermediateArrivals;
    }

    boolean isDestinationReached(
            @NonNull NavigationLocation location,
            float accuracyMeters,
            @Nullable PolylineIndex.Match match,
            long nowMs
    ) {
        return !intermediateArrivals.hasRemainingDestinations()
                && geometryState.isWithinDestinationReachedRadius(location, accuracyMeters, match, nowMs);
    }

    boolean verifyBeelineTarget(NavigationLocation location, float accuracy, LatLon target,
            PolylineIndex.Match match, long nowMs) {
        if (geometryState.isRouteUnavailable()) {
            return false;
        }
        if (intermediateArrivals.hasRemainingDestinations()) {
            return true;
        }
        LatLon destination = geometryState.route().track.get(geometryState.route().track.size() - 1);
        return GeoMath.distanceMeters(destination.lat, destination.lon, target.lat, target.lon) > 1
                || isDestinationReached(location, accuracy, match, nowMs);
    }
}

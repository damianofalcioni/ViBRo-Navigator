package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.NavigationRouteGeometryState;

final class NavigationArrivalDetector {
    @NonNull
    private final NavigationRouteGeometryState geometryState;

    NavigationArrivalDetector(@NonNull NavigationRouteGeometryState geometryState) {
        this.geometryState = geometryState;
    }

    boolean isDestinationReached(@NonNull NavigationLocation NavigationLocation, float accuracyMeters) {
        return geometryState.isWithinDestinationReachedRadius(NavigationLocation, accuracyMeters);
    }
}

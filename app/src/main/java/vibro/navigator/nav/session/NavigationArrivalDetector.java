package vibro.navigator.nav.session;

import android.location.Location;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.NavigationRouteGeometryState;

final class NavigationArrivalDetector {
    @NonNull
    private final NavigationRouteGeometryState geometryState;

    NavigationArrivalDetector(@NonNull NavigationRouteGeometryState geometryState) {
        this.geometryState = geometryState;
    }

    boolean isDestinationReached(@NonNull Location location, float accuracyMeters) {
        return geometryState.isWithinDestinationReachedRadius(location, accuracyMeters);
    }
}

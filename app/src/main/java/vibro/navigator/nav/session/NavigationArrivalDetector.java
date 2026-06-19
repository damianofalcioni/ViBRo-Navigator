package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationArrivalDetector {
    @NonNull
    private final NavigationRouteGeometryState geometryState;

    NavigationArrivalDetector(@NonNull NavigationRouteGeometryState geometryState) {
        this.geometryState = geometryState;
    }

    boolean isDestinationReached(@NonNull NavigationLocation location, float accuracyMeters) {
        return isDestinationReached(location, accuracyMeters, null);
    }

    boolean isDestinationReached(
            @NonNull NavigationLocation location,
            float accuracyMeters,
            @Nullable PolylineIndex.Match match
    ) {
        return geometryState.isWithinDestinationReachedRadius(location, accuracyMeters, match);
    }
}

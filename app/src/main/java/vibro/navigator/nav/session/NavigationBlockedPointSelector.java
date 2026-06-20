package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.nav.guidance.NavigationBlockedRouteState;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationBlockedPointSelector {
    @NonNull
    private final NavigationRouteGeometryState geometryState;
    @NonNull
    private final NavigationBlockedRouteState blockedRouteState;

    NavigationBlockedPointSelector(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationBlockedRouteState blockedRouteState
    ) {
        this.geometryState = geometryState;
        this.blockedRouteState = blockedRouteState;
    }

    @NonNull
    List<NogoPoint> addBlockedPointsAhead(@Nullable NavigationLocation lastFiltered, long nowMs) {
        List<NogoPoint> added = new ArrayList<>();
        if (lastFiltered == null || geometryState.isRouteUnavailable()) {
            return added;
        }

        PolylineIndex.Match match = geometryState.match(lastFiltered, accuracyOf(lastFiltered));
        if (match == null) {
            return added;
        }

        return blockedRouteState.addBlockedPointsAhead(geometryState.polylineIndex(), match.alongTrackMeters, nowMs);
    }

    private static float accuracyOf(@NonNull NavigationLocation location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }
}

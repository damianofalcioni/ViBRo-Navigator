package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteStartApproach;

final class RouteStartApproachState {
    @Nullable
    private LatLon target;

    void reset() {
        target = null;
    }

    void apply(@NonNull RouteStartApproach.Plan plan) {
        if (!plan.active || plan.target == null) {
            reset();
            return;
        }
        target = new LatLon(plan.target.lat, plan.target.lon);
    }

    boolean isActive() {
        return target != null;
    }

    @Nullable
    LatLon target() {
        return target;
    }

    boolean isReached(@NonNull PolylineIndex.Match match, double smoothedAccuracyMeters) {
        return RouteStartApproach.isInsideOriginalRouteThreshold(match, smoothedAccuracyMeters);
    }
}

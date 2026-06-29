package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.routing.StartupRouteRefreshPolicy;

final class RouteStartApproachState {
    @Nullable
    private LatLon target;
    @Nullable
    private NavigationLocation routeStartRequestLocation;

    void reset() {
        target = null;
        routeStartRequestLocation = null;
    }

    void apply(@NonNull RouteStartApproach.Plan plan) {
        apply(plan, null, false);
    }

    void apply(
            @NonNull RouteStartApproach.Plan plan,
            @Nullable NavigationLocation requestLocation,
            boolean allowStartupRefresh
    ) {
        if (!plan.active || plan.target == null) {
            reset();
            return;
        }
        target = new LatLon(plan.target.lat, plan.target.lon);
        routeStartRequestLocation = allowStartupRefresh && requestLocation != null
                ? new NavigationLocation(requestLocation)
                : null;
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

    boolean shouldRefreshRouteStart(@NonNull NavigationLocation latestLocation) {
        return StartupRouteRefreshPolicy.shouldRefreshAppliedRouteStart(
                routeStartRequestLocation,
                latestLocation
        );
    }
}

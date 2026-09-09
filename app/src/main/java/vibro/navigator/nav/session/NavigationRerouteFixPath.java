package vibro.navigator.nav.session;

import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

/** A tentative departure becomes a travelled connector only after a route/approach is applied. */
final class NavigationRerouteFixPath {
    private NavigationLocation lastStableRouteFix;
    private final NavigationConnectorFixBuffer activeFixes = new NavigationConnectorFixBuffer();
    private boolean routeApplied;

    void reset() {
        lastStableRouteFix = null;
        activeFixes.clear();
        routeApplied = false;
    }

    boolean isActive() {
        return !activeFixes.isEmpty();
    }

    void onRouteApplied() {
        if (isActive()) {
            routeApplied = true;
        }
    }

    List<LatLon> recordEvaluation(NavigationLocation location, NavigationRouteEvaluation evaluation,
            boolean calculationInProgress) {
        if (!isActive()) {
            return recordWithoutActivePath(location, evaluation);
        }
        activeFixes.add(location);
        if (calculationInProgress || evaluation.shouldRecalculateRoute() || !evaluation.isStableOnRouteSample()) {
            return Collections.emptyList();
        }
        List<LatLon> completed = routeApplied ? activeFixes.points(true) : Collections.emptyList();
        activeFixes.clear();
        routeApplied = false;
        lastStableRouteFix = new NavigationLocation(location);
        return completed;
    }

    private List<LatLon> recordWithoutActivePath(NavigationLocation location, NavigationRouteEvaluation evaluation) {
        if (evaluation.recalculationReason == NavigationRouteRecalculationReason.ROUTE_DEVIATION) {
            seedStableFix();
            activeFixes.add(location);
        } else if (evaluation.isStableOnRouteSample()) {
            lastStableRouteFix = new NavigationLocation(location);
        }
        return Collections.emptyList();
    }

    void startApproach(@Nullable NavigationLocation location) {
        if (!isActive() && location != null) {
            seedStableFix();
            activeFixes.add(location);
        }
        routeApplied = true;
    }

    List<LatLon> snapshot() {
        return activeFixes.points(true);
    }

    void rememberStableFix(NavigationLocation location) {
        lastStableRouteFix = new NavigationLocation(location);
    }

    private void seedStableFix() {
        if (lastStableRouteFix != null) {
            activeFixes.add(lastStableRouteFix);
        }
    }
}

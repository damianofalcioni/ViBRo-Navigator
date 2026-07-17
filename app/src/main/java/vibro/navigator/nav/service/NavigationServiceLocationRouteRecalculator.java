package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

final class NavigationServiceLocationRouteRecalculator
        implements NavigationServiceLocationHandler.RouteRecalculator {
    @NonNull
    private final NavigationServiceRouteRecalculator routeRecalculator;

    NavigationServiceLocationRouteRecalculator(
            @NonNull NavigationServiceRouteRecalculator routeRecalculator
    ) {
        this.routeRecalculator = routeRecalculator;
    }

    @Override
    public void request(
            boolean force,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        routeRecalculator.requestForLocation(force, rerouteNotice, reason);
    }

    @Override
    public void requestSpeculative(@NonNull NavigationRouteRecalculationReason reason) {
        routeRecalculator.requestSpeculative(reason);
    }

    @Override
    public boolean confirmSpeculative(@Nullable NavigationRerouteNotice rerouteNotice) {
        return routeRecalculator.confirmSpeculative(rerouteNotice);
    }

    @Override
    public void cancelSpeculative() {
        routeRecalculator.cancelSpeculative();
    }
}

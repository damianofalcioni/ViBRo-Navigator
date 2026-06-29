package vibro.navigator.nav.routing;

public enum NavigationRouteRecalculationReason {
    EXPLICIT,
    NO_ACTIVE_ROUTE,
    STARTUP_ROUTE_REFRESH,
    ROUTE_MATCH_FAILED,
    ROUTE_DEVIATION
}

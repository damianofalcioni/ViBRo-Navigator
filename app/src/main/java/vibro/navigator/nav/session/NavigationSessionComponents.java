package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.routing.NavigationRouteRequestManager;

final class NavigationSessionComponents {
    @NonNull
    final NavigationSessionLocationState locationState = new NavigationSessionLocationState();
    @NonNull
    final NavigationSessionHeadingResolver headingResolver = new NavigationSessionHeadingResolver(locationState);
    @NonNull
    final NavigationSessionRouteState routeState = new NavigationSessionRouteState();
    @NonNull
    final NavigationWarmupController warmupController = new NavigationWarmupController();
    @NonNull
    final NavigationRouteRequestManager routeRequestManager = new NavigationRouteRequestManager();
    @NonNull
    final NavigationSessionLocationEvaluator locationEvaluator =
            new NavigationSessionLocationEvaluator(locationState, routeState, warmupController, routeRequestManager);
    @NonNull
    final NavigationSessionStateBuilder stateBuilder =
            new NavigationSessionStateBuilder(locationState, headingResolver, routeState, routeRequestManager);

    void reset(long nowMs) {
        locationEvaluator.reset();
        locationState.reset();
        routeState.reset();
        warmupController.reset(nowMs);
        routeRequestManager.reset(nowMs);
    }
}

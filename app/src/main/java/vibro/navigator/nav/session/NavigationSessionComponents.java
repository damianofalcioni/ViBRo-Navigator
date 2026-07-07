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
    final StraightLineNavigationState straightLineState = new StraightLineNavigationState();
    @NonNull
    final NavigationWarmupController warmupController = new NavigationWarmupController();
    @NonNull
    final NavigationRouteRequestManager routeRequestManager = new NavigationRouteRequestManager();
    @NonNull
    final NavigationTripStatsTracker tripStatsTracker = new NavigationTripStatsTracker();
    @NonNull
    final NavigationSessionLocationEvaluator locationEvaluator =
            new NavigationSessionLocationEvaluator(
                    locationState,
                    routeState,
                    straightLineState,
                    warmupController,
                    routeRequestManager,
                    tripStatsTracker
            );
    @NonNull
    final NavigationSessionStateBuilder stateBuilder =
            new NavigationSessionStateBuilder(
                    locationState,
                    headingResolver,
                    routeState,
                    straightLineState,
                    routeRequestManager,
                    tripStatsTracker
            );

    void reset(long nowMs) {
        locationEvaluator.reset();
        locationState.reset();
        routeState.reset();
        straightLineState.reset();
        warmupController.reset(nowMs);
        routeRequestManager.reset();
        tripStatsTracker.reset();
    }
}

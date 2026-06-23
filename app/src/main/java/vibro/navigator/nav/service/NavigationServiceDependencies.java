package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.session.NavigationSession;

final class NavigationServiceDependencies {
    @NonNull
    final NavigationForegroundRuntime foreground;
    @NonNull
    final NavigationTrackingRuntime tracking;
    @NonNull
    final NavigationRoutingRuntime routing;

    private NavigationServiceDependencies(
            @NonNull NavigationForegroundRuntime foreground,
            @NonNull NavigationTrackingRuntime tracking,
            @NonNull NavigationRoutingRuntime routing
    ) {
        this.foreground = foreground;
        this.tracking = tracking;
        this.routing = routing;
    }

    @NonNull
    static NavigationServiceDependencies create(
            @NonNull NavigationService service,
            @NonNull TaskScheduler uiScheduler,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull NavigationServiceLocationHandler locationHandler,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull Runnable stateEmitter,
            @NonNull NavigationServiceRouteCallback.RouteRecalculator routeRecalculator
    ) {
        NavigationForegroundRuntime foreground =
                NavigationForegroundRuntime.create(service, uiVisibility, locationHandler);
        NavigationTrackingRuntime tracking = NavigationTrackingRuntime.create(
                service,
                uiScheduler,
                uiVisibility,
                locationHandler,
                foreground.controller
        );
        NavigationRoutingRuntime routing = NavigationRoutingRuntime.create(
                service,
                uiScheduler,
                navigationSession,
                tracking.orientationController,
                foreground.controller,
                turnEvents,
                () -> {
                    long nowMs = AndroidElapsedRealtimeClock.INSTANCE.elapsedRealtimeMs();
                    tracking.locationController.requestLocationUpdates(
                            NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS
                    );
                    return nowMs;
                },
                stateEmitter,
                routeRecalculator
        );
        return new NavigationServiceDependencies(foreground, tracking, routing);
    }
}

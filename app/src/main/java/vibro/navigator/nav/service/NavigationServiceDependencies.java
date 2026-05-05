package vibro.navigator.nav.service;

import android.os.Handler;

import androidx.annotation.NonNull;

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
            @NonNull Handler handler,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull NavigationServiceLocationHandler locationHandler,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull Runnable stateEmitter,
            @NonNull NavigationServiceRouteCallback.RouteRecalculator routeRecalculator
    ) {
        NavigationForegroundRuntime foreground = NavigationForegroundRuntime.create(service, uiVisibility);
        NavigationTrackingRuntime tracking = NavigationTrackingRuntime.create(
                service,
                handler,
                uiVisibility,
                locationHandler,
                foreground.controller
        );
        NavigationRoutingRuntime routing = NavigationRoutingRuntime.create(
                service,
                handler,
                navigationSession,
                tracking.orientationController,
                foreground.controller,
                turnEvents,
                stateEmitter,
                routeRecalculator
        );
        return new NavigationServiceDependencies(foreground, tracking, routing);
    }
}

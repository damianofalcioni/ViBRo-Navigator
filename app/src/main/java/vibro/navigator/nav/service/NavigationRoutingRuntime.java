package vibro.navigator.nav.service;

import android.os.Handler;

import androidx.annotation.NonNull;

import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.routing.NavigationRouteExecutor;
import vibro.navigator.nav.session.NavigationSession;

final class NavigationRoutingRuntime {
    @NonNull
    final NavigationRouteExecutor executor;
    @NonNull
    final NavigationServiceRouteCallback callback;

    private NavigationRoutingRuntime(
            @NonNull NavigationRouteExecutor executor,
            @NonNull NavigationServiceRouteCallback callback
    ) {
        this.executor = executor;
        this.callback = callback;
    }

    @NonNull
    static NavigationRoutingRuntime create(
            @NonNull NavigationService service,
            @NonNull Handler handler,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationForegroundController foregroundController,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull Runnable stateEmitter,
            @NonNull NavigationServiceRouteCallback.RouteRecalculator routeRecalculator
    ) {
        NavigationRouteExecutor executor = NavigationRouteExecutor.createDefault(service, handler);
        NavigationTurnEventDispatcher turnEventDispatcher = new NavigationTurnEventDispatcher(
                new NavigationServiceTurnNotificationSink(foregroundController)
        );
        turnEvents.attachDispatcher(turnEventDispatcher);
        NavigationServiceRouteCallback callback = new NavigationServiceRouteCallback(
                service,
                navigationSession,
                orientationController,
                foregroundController,
                turnEvents,
                stateEmitter,
                routeRecalculator
        );
        return new NavigationRoutingRuntime(executor, callback);
    }
}

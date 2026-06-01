package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.android.routing.AndroidNavigationRouteExecutorFactory;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.routing.NavigationRouteExecutor;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.voice.NavigationManeuverSpeaker;

final class NavigationRoutingRuntime {
    @NonNull
    final NavigationRouteExecutor executor;
    @NonNull
    final NavigationServiceRouteCallback callback;
    @NonNull
    final NavigationManeuverSpeaker maneuverSpeaker;

    private NavigationRoutingRuntime(
            @NonNull NavigationRouteExecutor executor,
            @NonNull NavigationServiceRouteCallback callback,
            @NonNull NavigationManeuverSpeaker maneuverSpeaker
    ) {
        this.executor = executor;
        this.callback = callback;
        this.maneuverSpeaker = maneuverSpeaker;
    }

    @NonNull
    static NavigationRoutingRuntime create(
            @NonNull NavigationService service,
            @NonNull TaskScheduler uiScheduler,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationForegroundController foregroundController,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull Runnable stateEmitter,
            @NonNull NavigationServiceRouteCallback.RouteRecalculator routeRecalculator
    ) {
        NavigationRouteExecutor executor = AndroidNavigationRouteExecutorFactory.create(service, uiScheduler);
        NavigationManeuverSpeaker maneuverSpeaker = new NavigationManeuverSpeaker(service);
        NavigationTurnEventDispatcher turnEventDispatcher = new NavigationTurnEventDispatcher(
                new NavigationServiceTurnNotificationSink(foregroundController, maneuverSpeaker)
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
        return new NavigationRoutingRuntime(executor, callback, maneuverSpeaker);
    }
}

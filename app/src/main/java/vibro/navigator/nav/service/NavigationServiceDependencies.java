package vibro.navigator.nav.service;

import android.os.Handler;

import androidx.annotation.NonNull;

import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.foreground.NavigationScreenInteractivityMonitor;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.routing.NavigationRouteExecutor;
import vibro.navigator.nav.session.NavigationSession;

final class NavigationServiceDependencies {
    @NonNull
    final NavigationForegroundController foregroundController;
    @NonNull
    final NavigationLocationController locationController;
    @NonNull
    final NavigationRouteExecutor routeExecutor;
    @NonNull
    final NavigationServiceRouteCallback routeCallback;
    @NonNull
    final NavigationOrientationController orientationController;
    @NonNull
    final NavigationScreenInteractivityMonitor screenInteractivityMonitor;
    final boolean screenInteractive;

    private NavigationServiceDependencies(
            @NonNull NavigationForegroundController foregroundController,
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationRouteExecutor routeExecutor,
            @NonNull NavigationServiceRouteCallback routeCallback,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationScreenInteractivityMonitor screenInteractivityMonitor,
            boolean screenInteractive
    ) {
        this.foregroundController = foregroundController;
        this.locationController = locationController;
        this.routeExecutor = routeExecutor;
        this.routeCallback = routeCallback;
        this.orientationController = orientationController;
        this.screenInteractivityMonitor = screenInteractivityMonitor;
        this.screenInteractive = screenInteractive;
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
        NavigationForegroundController foregroundController = new NavigationForegroundController(service);
        NavigationLocationController locationController = new NavigationLocationController(service, locationHandler);
        NavigationRouteExecutor routeExecutor = NavigationRouteExecutor.createDefault(service, handler);
        NavigationTurnEventDispatcher turnEventDispatcher = new NavigationTurnEventDispatcher(
                new NavigationServiceTurnNotificationSink(foregroundController)
        );
        turnEvents.attachDispatcher(turnEventDispatcher);
        NavigationOrientationController orientationController = new NavigationOrientationController(
                service,
                handler,
                uiVisibility
        );
        NavigationServiceRouteCallback routeCallback = new NavigationServiceRouteCallback(
                service,
                navigationSession,
                orientationController,
                foregroundController,
                turnEvents,
                stateEmitter,
                routeRecalculator
        );
        locationHandler.attachControllers(locationController, orientationController, foregroundController);
        NavigationScreenInteractivityMonitor screenInteractivityMonitor =
                new NavigationScreenInteractivityMonitor(service, uiVisibility::onScreenInteractiveChanged);
        foregroundController.ensureChannels();
        return new NavigationServiceDependencies(
                foregroundController,
                locationController,
                routeExecutor,
                routeCallback,
                orientationController,
                screenInteractivityMonitor,
                screenInteractivityMonitor.start()
        );
    }
}

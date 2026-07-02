package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.session.NavigationSession;

final class NavigationServiceRuntimeFactory {
    private NavigationServiceRuntimeFactory() {
    }

    @NonNull
    static NavigationServiceRuntime create(
            @NonNull NavigationService service,
            @NonNull TaskScheduler uiScheduler,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull NavigationServiceLocationHandler locationHandler,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull Runnable stateEmitter,
            @NonNull NavigationServiceRouteCallback.RouteRecalculator routeRecalculator
    ) {
        NavigationServiceDependencies dependencies = NavigationServiceDependencies.create(
                service,
                uiScheduler,
                navigationSession,
                turnEvents,
                locationHandler,
                uiVisibility,
                stateEmitter,
                routeRecalculator
        );
        uiVisibility.setScreenInteractive(dependencies.foreground.screenInteractive);
        return new NavigationServiceRuntime(
                service,
                dependencies,
                NavigationServiceStreetOverlay.create(service, uiScheduler, stateEmitter)
        );
    }
}

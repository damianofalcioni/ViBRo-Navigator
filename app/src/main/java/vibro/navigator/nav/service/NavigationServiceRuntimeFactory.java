package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.android.sensor.AndroidNavigationMotionTrigger;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;

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
            @NonNull NavigationServiceRouteRecalculator routeRecalculator
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
        NavigationServiceRuntime runtime = new NavigationServiceRuntime(
                service,
                dependencies,
                NavigationServiceStreetOverlay.create(service, uiScheduler, stateEmitter)
        );
        runtime.locationRecovery.enableStationarySleep(
                new AndroidNavigationMotionTrigger(service, uiScheduler),
                uiScheduler,
                () -> NavigationSessionResourceAdapter.restartDepartureMonitoring(navigationSession)
        );
        return runtime;
    }
}

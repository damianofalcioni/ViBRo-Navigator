package vibro.navigator.android.routing;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.Executors;

import vibro.navigator.android.brouter.AndroidBRouterRouteClient;
import vibro.navigator.android.power.AndroidPartialWakeLock;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.power.NavigationWakeLockController;
import vibro.navigator.nav.routing.NavigationBRouterRouteCalculator;
import vibro.navigator.nav.routing.NavigationRouteExecutor;

public final class AndroidNavigationRouteExecutorFactory {

    private static final String ROUTE_WAKE_LOCK_TAG =
            "vibro.navigator.nav.NavigationRouteExecutor:route";
    private static final long ROUTE_WAKE_LOCK_TIMEOUT_MS = 60_000L;

    private AndroidNavigationRouteExecutorFactory() {
    }

    @NonNull
    public static NavigationRouteExecutor create(
            @NonNull Context context,
            @NonNull TaskScheduler mainThreadScheduler
    ) {
        Context appContext = context.getApplicationContext();
        NavigationWakeLockController wakeLockController =
                new NavigationWakeLockController(new AndroidPartialWakeLock(appContext));
        return new NavigationRouteExecutor(
                new NavigationBRouterRouteCalculator(new AndroidBRouterRouteClient(appContext)),
                Executors.newSingleThreadExecutor(),
                mainThreadScheduler,
                routeCalculation -> wakeLockController.runWithWakeLock(
                        ROUTE_WAKE_LOCK_TAG,
                        ROUTE_WAKE_LOCK_TIMEOUT_MS,
                        routeCalculation
                )
        );
    }
}

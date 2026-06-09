package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class NavigationRouteExecutor {

    public interface Sleeper {
        public void sleep(long delayMs) throws InterruptedException;
    }

    public interface Callback {
        void onRouteApplied(
                @NonNull NavigationRouteRequestSnapshot snapshot,
                @NonNull GeoJsonRoute newRoute,
                long beganAt
        );

        void onRouteFailure(
                @NonNull NavigationRouteRequestSnapshot snapshot,
                @NonNull Exception error
        );
    }

    public interface RouteCalculator {
        @NonNull
        public GeoJsonRoute routeGeoJson(
                @NonNull LatLon start,
                @NonNull List<LatLon> intermediates,
                @NonNull LatLon destination,
                @NonNull String profile,
                @NonNull List<NogoPoint> blocked
        ) throws Exception;
    }

    @VisibleForTesting
    public interface RouteCalculationGuard {
        @NonNull
        public GeoJsonRoute run(@NonNull Callable<GeoJsonRoute> routeCalculation) throws Exception;
    }

    private static final String TAG = "NavRouteExecutor";
    private static final int DEFAULT_MAX_TRANSIENT_ROUTE_RETRIES = 2;
    private static final long DEFAULT_TRANSIENT_ROUTE_RETRY_DELAY_MS = 400L;

    private final RouteCalculator routeCalculator;
    private final ExecutorService executorService;
    private final TaskScheduler mainThreadScheduler;
    private final RouteCalculationGuard routeCalculationGuard;
    private final NavigationRouteRetryPolicy routeRetryPolicy;
    private volatile boolean shutdown;

    public NavigationRouteExecutor(
            @NonNull RouteCalculator routeCalculator,
            @NonNull ExecutorService executorService,
            @NonNull TaskScheduler mainThreadScheduler
    ) {
        this(
                routeCalculator,
                executorService,
                mainThreadScheduler,
                noWakeLockGuard(),
                DEFAULT_MAX_TRANSIENT_ROUTE_RETRIES,
                DEFAULT_TRANSIENT_ROUTE_RETRY_DELAY_MS,
                Thread::sleep
        );
    }

    @VisibleForTesting
    public NavigationRouteExecutor(
            @NonNull RouteCalculator routeCalculator,
            @NonNull ExecutorService executorService,
            @NonNull TaskScheduler mainThreadScheduler,
            int maxTransientRouteRetries,
            long transientRouteRetryDelayMs,
            @NonNull Sleeper sleeper
    ) {
        this(
                routeCalculator,
                executorService,
                mainThreadScheduler,
                noWakeLockGuard(),
                maxTransientRouteRetries,
                transientRouteRetryDelayMs,
                sleeper
        );
    }

    @VisibleForTesting
    public NavigationRouteExecutor(
            @NonNull RouteCalculator routeCalculator,
            @NonNull ExecutorService executorService,
            @NonNull TaskScheduler mainThreadScheduler,
            @NonNull RouteCalculationGuard routeCalculationGuard,
            int maxTransientRouteRetries,
            long transientRouteRetryDelayMs,
            @NonNull Sleeper sleeper
    ) {
        this.routeCalculator = routeCalculator;
        this.executorService = executorService;
        this.mainThreadScheduler = mainThreadScheduler;
        this.routeCalculationGuard = routeCalculationGuard;
        this.routeRetryPolicy = new NavigationRouteRetryPolicy(
                maxTransientRouteRetries,
                transientRouteRetryDelayMs,
                sleeper
        );
    }

    public NavigationRouteExecutor(
            @NonNull RouteCalculator routeCalculator,
            @NonNull ExecutorService executorService,
            @NonNull TaskScheduler mainThreadScheduler,
            @NonNull RouteCalculationGuard routeCalculationGuard
    ) {
        this(
                routeCalculator,
                executorService,
                mainThreadScheduler,
                routeCalculationGuard,
                DEFAULT_MAX_TRANSIENT_ROUTE_RETRIES,
                DEFAULT_TRANSIENT_ROUTE_RETRY_DELAY_MS,
                Thread::sleep
        );
    }

    public void requestRoute(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Callback callback
    ) {
        if (shutdown) {
            AppLogger.w(TAG, "Ignoring route calculation request after shutdown");
            return;
        }
        try {
            executorService.submit(() -> {
                long beganAt = System.currentTimeMillis();
                try {
                    GeoJsonRoute newRoute = routeCalculationGuard.run(
                            () -> calculateRouteWithRetry(snapshot)
                    );
                    if (newRoute.track.isEmpty()) {
                        throw new IllegalStateException("BRouter returned an empty route");
                    }
                    deliverRouteApplied(snapshot, callback, newRoute, beganAt);
                } catch (Exception e) {
                    deliverRouteFailure(snapshot, callback, e);
                }
            });
        } catch (RejectedExecutionException e) {
            AppLogger.w(TAG, "Route calculation rejected because the executor is shut down", e);
            deliverRouteFailure(
                    snapshot,
                    callback,
                    new IllegalStateException("Route calculation rejected because the executor is shut down", e)
            );
        }
    }

    public void shutdown() {
        shutdown = true;
        executorService.shutdownNow();
        if (!awaitExecutorTermination()) {
            AppLogger.w(TAG, "Route executor did not terminate cleanly before cleanup");
        }
        closeRouteCalculator();
    }

    private void deliverRouteApplied(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Callback callback,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (shutdown) {
            return;
        }
        mainThreadScheduler.post(() -> {
            if (!shutdown) {
                callback.onRouteApplied(snapshot, newRoute, beganAt);
            }
        });
    }

    private void deliverRouteFailure(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Callback callback,
            @NonNull Exception error
    ) {
        if (shutdown) {
            return;
        }
        mainThreadScheduler.post(() -> {
            if (!shutdown) {
                callback.onRouteFailure(snapshot, error);
            }
        });
    }

    @NonNull
    private GeoJsonRoute calculateRouteWithRetry(@NonNull NavigationRouteRequestSnapshot snapshot) throws Exception {
        return routeRetryPolicy.calculateRoute(routeCalculator, snapshot);
    }

    private boolean awaitExecutorTermination() {
        try {
            return executorService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AppLogger.w(TAG, "Interrupted while waiting for route executor shutdown", e);
            return false;
        }
    }

    private void closeRouteCalculator() {
        if (!(routeCalculator instanceof AutoCloseable)) {
            return;
        }
        try {
            ((AutoCloseable) routeCalculator).close();
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to close route calculator cleanly", e);
        }
    }

    @NonNull
    private static RouteCalculationGuard noWakeLockGuard() {
        return routeCalculation -> routeCalculation.call();
    }
}

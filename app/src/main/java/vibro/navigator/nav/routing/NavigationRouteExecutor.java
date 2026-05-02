package vibro.navigator.nav.routing;


import vibro.navigator.nav.power.NavigationWakeLockController;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.util.AppLogger;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class NavigationRouteExecutor {

    @VisibleForTesting
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
                @NonNull Context context,
                @NonNull LatLon start,
                @NonNull List<LatLon> intermediates,
                @NonNull LatLon destination,
                @NonNull String profile,
                @NonNull List<NogoPoint> blocked
        ) throws Exception;
    }

    public interface MainThreadPoster {
        void post(@NonNull Runnable runnable);
    }

    @VisibleForTesting
    public interface RouteCalculationGuard {
        @NonNull
        public GeoJsonRoute run(@NonNull Callable<GeoJsonRoute> routeCalculation) throws Exception;
    }

    private static final String TAG = "NavRouteExecutor";
    private static final String ROUTE_WAKE_LOCK_TAG =
            "vibro.navigator.nav.NavigationRouteExecutor:route";
    private static final long ROUTE_WAKE_LOCK_TIMEOUT_MS = 60_000L;
    private static final int DEFAULT_MAX_TRANSIENT_ROUTE_RETRIES = 2;
    private static final long DEFAULT_TRANSIENT_ROUTE_RETRY_DELAY_MS = 400L;

    private final RouteCalculator routeCalculator;
    private final ExecutorService executorService;
    private final MainThreadPoster mainThreadPoster;
    private final RouteCalculationGuard routeCalculationGuard;
    private final int maxTransientRouteRetries;
    private final long transientRouteRetryDelayMs;
    private final Sleeper sleeper;

    public NavigationRouteExecutor(
            @NonNull RouteCalculator routeCalculator,
            @NonNull ExecutorService executorService,
            @NonNull MainThreadPoster mainThreadPoster
    ) {
        this(
                routeCalculator,
                executorService,
                mainThreadPoster,
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
            @NonNull MainThreadPoster mainThreadPoster,
            int maxTransientRouteRetries,
            long transientRouteRetryDelayMs,
            @NonNull Sleeper sleeper
    ) {
        this(
                routeCalculator,
                executorService,
                mainThreadPoster,
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
            @NonNull MainThreadPoster mainThreadPoster,
            @NonNull RouteCalculationGuard routeCalculationGuard,
            int maxTransientRouteRetries,
            long transientRouteRetryDelayMs,
            @NonNull Sleeper sleeper
    ) {
        this.routeCalculator = routeCalculator;
        this.executorService = executorService;
        this.mainThreadPoster = mainThreadPoster;
        this.routeCalculationGuard = routeCalculationGuard;
        this.maxTransientRouteRetries = Math.max(0, maxTransientRouteRetries);
        this.transientRouteRetryDelayMs = Math.max(0L, transientRouteRetryDelayMs);
        this.sleeper = sleeper;
    }

    @NonNull
    public static NavigationRouteExecutor createDefault(@NonNull Context context, @NonNull Handler handler) {
        NavigationWakeLockController wakeLockController = new NavigationWakeLockController(context);
        return new NavigationRouteExecutor(
                new NavigationBRouterRouteCalculator(),
                Executors.newSingleThreadExecutor(),
                new HandlerPoster(handler),
                routeCalculation -> wakeLockController.runWithWakeLock(
                        ROUTE_WAKE_LOCK_TAG,
                        ROUTE_WAKE_LOCK_TIMEOUT_MS,
                        routeCalculation
                ),
                DEFAULT_MAX_TRANSIENT_ROUTE_RETRIES,
                DEFAULT_TRANSIENT_ROUTE_RETRY_DELAY_MS,
                Thread::sleep
        );
    }

    public void requestRoute(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Callback callback
    ) {
        Context appContext = context.getApplicationContext();
        try {
            executorService.submit(() -> {
                long beganAt = System.currentTimeMillis();
                try {
                    GeoJsonRoute newRoute = routeCalculationGuard.run(
                            () -> calculateRouteWithRetry(appContext, snapshot)
                    );
                    if (newRoute.track.isEmpty()) {
                        throw new IllegalStateException("BRouter returned an empty route");
                    }
                    mainThreadPoster.post(() -> callback.onRouteApplied(snapshot, newRoute, beganAt));
                } catch (Exception e) {
                    mainThreadPoster.post(() -> callback.onRouteFailure(snapshot, e));
                }
            });
        } catch (RejectedExecutionException e) {
            AppLogger.w(TAG, "Route calculation rejected because the executor is shut down", e);
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
        if (!awaitExecutorTermination()) {
            AppLogger.w(TAG, "Route executor did not terminate cleanly before cleanup");
        }
        closeRouteCalculator();
    }

    @NonNull
    private GeoJsonRoute calculateRouteWithRetry(
            @NonNull Context appContext,
            @NonNull NavigationRouteRequestSnapshot snapshot
    ) throws Exception {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return routeCalculator.routeGeoJson(
                        appContext,
                        snapshot.start,
                        snapshot.intermediates,
                        requireDestination(snapshot),
                        requireProfile(snapshot),
                        snapshot.blocked
                );
            } catch (Exception e) {
                if (!shouldRetryTransientRouteFailure(e, attempt)) {
                    throw e;
                }
                AppLogger.w(TAG, "Transient BRouter route failure attempt="
                        + attempt + "/" + (maxTransientRouteRetries + 1)
                        + " retryDelayMs=" + transientRouteRetryDelayMs, e);
                sleepBeforeRetry();
            }
        }
    }

    @NonNull
    private static LatLon requireDestination(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.destination == null) {
            throw new IllegalStateException("Route request is missing a destination");
        }
        return snapshot.destination;
    }

    @NonNull
    private static String requireProfile(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.profile == null || snapshot.profile.trim().isEmpty()) {
            throw new IllegalStateException("Route request is missing a profile");
        }
        return snapshot.profile;
    }

    private boolean shouldRetryTransientRouteFailure(@NonNull Exception error, int attempt) {
        return attempt <= maxTransientRouteRetries && BRouterTransientFailureClassifier.isTransient(error);
    }

    private void sleepBeforeRetry() throws Exception {
        if (transientRouteRetryDelayMs <= 0L) {
            return;
        }
        try {
            sleeper.sleep(transientRouteRetryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying BRouter route calculation", e);
        }
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

    @VisibleForTesting
    public static final class HandlerPoster implements MainThreadPoster {
        private final Handler handler;

        public HandlerPoster(@NonNull Handler handler) {
            this.handler = handler;
        }

        @Override
        public void post(@NonNull Runnable runnable) {
            handler.post(runnable);
        }
    }
}

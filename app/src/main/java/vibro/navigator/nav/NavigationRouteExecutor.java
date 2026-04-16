package vibro.navigator.nav;

import android.content.Context;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import vibro.navigator.brouter.BRouterClient;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.brouter.BRouterRouter;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.util.AppLogger;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

final class NavigationRouteExecutor {

    @VisibleForTesting
    interface Sleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    interface Callback {
        void onRouteApplied(
                @NonNull NavigationSession.RouteRequestSnapshot snapshot,
                @NonNull GeoJsonRoute newRoute,
                long beganAt
        );

        void onRouteFailure(
                @NonNull NavigationSession.RouteRequestSnapshot snapshot,
                @NonNull Exception error
        );
    }

    interface RouteCalculator {
        @NonNull
        GeoJsonRoute routeGeoJson(
                @NonNull Context context,
                @NonNull LatLon start,
                @NonNull List<LatLon> intermediates,
                @NonNull LatLon destination,
                @NonNull String profile,
                @NonNull List<NogoPoint> blocked
        ) throws Exception;
    }

    interface MainThreadPoster {
        void post(@NonNull Runnable runnable);
    }

    @VisibleForTesting
    interface RouteCalculationGuard {
        @NonNull
        GeoJsonRoute run(@NonNull Callable<GeoJsonRoute> routeCalculation) throws Exception;
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

    NavigationRouteExecutor(
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
    NavigationRouteExecutor(
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
    NavigationRouteExecutor(
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
    static NavigationRouteExecutor createDefault(@NonNull Context context, @NonNull Handler handler) {
        NavigationWakeLockController wakeLockController = new NavigationWakeLockController(context);
        return new NavigationRouteExecutor(
                new BRouterRouteCalculator(),
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

    void requestRoute(
            @NonNull Context context,
            @NonNull NavigationSession.RouteRequestSnapshot snapshot,
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

    void shutdown() {
        executorService.shutdownNow();
        if (!awaitExecutorTermination()) {
            AppLogger.w(TAG, "Route executor did not terminate cleanly before cleanup");
        }
        closeRouteCalculator();
    }

    @NonNull
    private GeoJsonRoute calculateRouteWithRetry(
            @NonNull Context appContext,
            @NonNull NavigationSession.RouteRequestSnapshot snapshot
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
    private static LatLon requireDestination(@NonNull NavigationSession.RouteRequestSnapshot snapshot) {
        if (snapshot.destination == null) {
            throw new IllegalStateException("Route request is missing a destination");
        }
        return snapshot.destination;
    }

    @NonNull
    private static String requireProfile(@NonNull NavigationSession.RouteRequestSnapshot snapshot) {
        if (snapshot.profile == null || snapshot.profile.trim().isEmpty()) {
            throw new IllegalStateException("Route request is missing a profile");
        }
        return snapshot.profile;
    }

    private boolean shouldRetryTransientRouteFailure(@NonNull Exception error, int attempt) {
        return attempt <= maxTransientRouteRetries && isTransientBRouterFailure(error);
    }

    private boolean isTransientBRouterFailure(@NonNull Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof DeadObjectException || current instanceof RemoteException) {
                return true;
            }
            if (current instanceof BRouterRouteException) {
                return ((BRouterRouteException) current).reason == BRouterRouteException.Reason.SERVICE_UNAVAILABLE;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.trim().toLowerCase(Locale.ROOT);
                if (normalized.contains("brouter service not available")
                        || normalized.contains("brouter is not connected")
                        || normalized.contains("brouter binding died")
                        || normalized.contains("null binding")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
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

    private static final class BRouterRouteCalculator implements RouteCalculator, AutoCloseable {
        private final BRouterRouter router = new BRouterRouter();
        private BRouterClient client;

        @NonNull
        @Override
        public synchronized GeoJsonRoute routeGeoJson(
                @NonNull Context context,
                @NonNull LatLon start,
                @NonNull List<LatLon> intermediates,
                @NonNull LatLon destination,
                @NonNull String profile,
                @NonNull List<NogoPoint> blocked
        ) throws Exception {
            if (client == null) {
                client = new BRouterClient(context.getApplicationContext());
            }
            return router.routeGeoJson(client, start, intermediates, destination, profile, blocked);
        }

        @Override
        public synchronized void close() {
            if (client != null) {
                client.close();
                client = null;
            }
        }
    }

    @VisibleForTesting
    static final class HandlerPoster implements MainThreadPoster {
        private final Handler handler;

        HandlerPoster(@NonNull Handler handler) {
            this.handler = handler;
        }

        @Override
        public void post(@NonNull Runnable runnable) {
            handler.post(runnable);
        }
    }
}

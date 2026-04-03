package com.vibenavigator.nav;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.vibenavigator.brouter.BRouterRouter;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.util.AppLogger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

final class NavigationRouteExecutor {

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

    private static final String TAG = "NavRouteExecutor";

    private final RouteCalculator routeCalculator;
    private final ExecutorService executorService;
    private final MainThreadPoster mainThreadPoster;

    NavigationRouteExecutor(
            @NonNull RouteCalculator routeCalculator,
            @NonNull ExecutorService executorService,
            @NonNull MainThreadPoster mainThreadPoster
    ) {
        this.routeCalculator = routeCalculator;
        this.executorService = executorService;
        this.mainThreadPoster = mainThreadPoster;
    }

    @NonNull
    static NavigationRouteExecutor createDefault(@NonNull Handler handler) {
        return new NavigationRouteExecutor(
                new BRouterRouteCalculator(),
                Executors.newSingleThreadExecutor(),
                new HandlerPoster(handler)
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
                    GeoJsonRoute newRoute = routeCalculator.routeGeoJson(
                            appContext,
                            snapshot.start,
                            snapshot.intermediates,
                            requireDestination(snapshot),
                            requireProfile(snapshot),
                            snapshot.blocked
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

    private static final class BRouterRouteCalculator implements RouteCalculator {
        private final BRouterRouter router = new BRouterRouter();

        @NonNull
        @Override
        public GeoJsonRoute routeGeoJson(
                @NonNull Context context,
                @NonNull LatLon start,
                @NonNull List<LatLon> intermediates,
                @NonNull LatLon destination,
                @NonNull String profile,
                @NonNull List<NogoPoint> blocked
        ) throws Exception {
            return router.routeGeoJson(context, start, intermediates, destination, profile, blocked);
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

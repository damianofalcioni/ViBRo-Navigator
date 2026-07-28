package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.logging.AppLogger;

final class NavigationRouteRetryPolicy {
    private static final String TAG = "NavRouteExecutor";

    private final int maxTransientRouteRetries;
    private final long transientRouteRetryDelayMs;
    @NonNull
    private final NavigationRouteExecutor.Sleeper sleeper;

    NavigationRouteRetryPolicy(
            int maxTransientRouteRetries,
            long transientRouteRetryDelayMs,
            @NonNull NavigationRouteExecutor.Sleeper sleeper
    ) {
        this.maxTransientRouteRetries = Math.max(0, maxTransientRouteRetries);
        this.transientRouteRetryDelayMs = Math.max(0L, transientRouteRetryDelayMs);
        this.sleeper = sleeper;
    }

    @NonNull
    GeoJsonRoute calculateRoute(
            @NonNull NavigationRouteExecutor.RouteCalculator routeCalculator,
            @NonNull NavigationRouteRequestSnapshot snapshot
    ) throws Exception {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return calculateSnapshotRoute(routeCalculator, snapshot);
            } catch (Exception e) {
                if (!shouldRetryTransientRouteFailure(snapshot, e, attempt)) {
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
    private static GeoJsonRoute calculateSnapshotRoute(
            @NonNull NavigationRouteExecutor.RouteCalculator routeCalculator,
            @NonNull NavigationRouteRequestSnapshot snapshot
    ) throws Exception {
        if (snapshot.isRoundTrip()) {
            return routeCalculator.roundTripGeoJson(
                    snapshot.start,
                    requireProfile(snapshot),
                    snapshot.customProfile,
                    snapshot.blocked,
                    requireRoundTripDistance(snapshot),
                    snapshot.roundTripDirectionDegrees,
                    profileParameters(snapshot)
            );
        }
        return routeCalculator.routeGeoJson(
                snapshot.start,
                snapshot.intermediates,
                requireDestination(snapshot),
                requireProfile(snapshot),
                snapshot.customProfile,
                snapshot.blocked,
                profileParameters(snapshot)
        );
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

    private static int requireRoundTripDistance(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.roundTripDistanceMeters <= 0) {
            throw new IllegalStateException("Round trip route request is missing a distance");
        }
        return snapshot.roundTripDistanceMeters;
    }

    @NonNull
    private static String profileParameters(@NonNull NavigationRouteRequestSnapshot snapshot) {
        return snapshot.profileParameters == null ? "" : snapshot.profileParameters;
    }

    private boolean shouldRetryTransientRouteFailure(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error,
            int attempt
    ) {
        return !snapshot.speculative
                && attempt <= maxTransientRouteRetries
                && BRouterTransientFailureClassifier.isTransient(error);
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
}

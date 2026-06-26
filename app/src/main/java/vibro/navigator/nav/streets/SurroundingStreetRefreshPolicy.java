package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;

final class SurroundingStreetRefreshPolicy {
    static final long MIN_REFRESH_INTERVAL_MS = 45_000L;
    static final float MIN_REFRESH_DISTANCE_METERS = 120f;
    static final float FORCE_REFRESH_DISTANCE_METERS = 350f;
    static final double MIN_REFRESH_RADIUS_INCREASE_METERS = 60.0d;

    boolean shouldRefresh(
            @NonNull NavigationLocation currentLocation,
            @Nullable NavigationLocation lastRefreshLocation,
            double lastRefreshRadiusMeters,
            double requestedRadiusMeters,
            long lastRefreshElapsedMs,
            long nowElapsedMs,
            boolean inFlight
    ) {
        if (inFlight) {
            return false;
        }
        if (lastRefreshLocation == null || lastRefreshElapsedMs <= 0L) {
            return true;
        }
        if (requestedRadiusMeters - lastRefreshRadiusMeters >= MIN_REFRESH_RADIUS_INCREASE_METERS) {
            return true;
        }
        float distanceMeters = currentLocation.distanceTo(lastRefreshLocation);
        if (distanceMeters >= FORCE_REFRESH_DISTANCE_METERS) {
            return true;
        }
        return distanceMeters >= MIN_REFRESH_DISTANCE_METERS
                && nowElapsedMs - lastRefreshElapsedMs >= MIN_REFRESH_INTERVAL_MS;
    }
}

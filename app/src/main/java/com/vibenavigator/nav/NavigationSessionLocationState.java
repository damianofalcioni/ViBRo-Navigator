package com.vibenavigator.nav;

import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.nav.kalman.LatLonKalmanFilter;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayDeque;

final class NavigationSessionLocationState {

    private static final String TAG = "NavSessionLocation";
    private static final float MAX_STATIONARY_REPORTED_SPEED_MPS = 0.35f;
    private static final long RECENT_MOTION_WINDOW_MS = 3_000L;
    private static final double MAX_STATIONARY_RECENT_DISTANCE_METERS = 0.8;
    private static final float MIN_RAW_BEARING_SPEED_MPS = 1.0f;
    private static final float MIN_TRUSTED_GPS_BEARING_SPEED_MPS = 0.8f;
    private static final float MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS = 1.5f;
    private static final float MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES = 25f;
    private static final long MIN_MOVEMENT_BEARING_ELAPSED_MS = 2_000L;
    private static final double MIN_MOVEMENT_BEARING_DISTANCE_METERS = 3.0;

    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();
    private final LiveLocationCoordinator liveLocationCoordinator = new LiveLocationCoordinator();
    private final ArrayDeque<Location> recentFilteredLocations = new ArrayDeque<>();

    @Nullable
    private Location lastFiltered;
    @Nullable
    private Location previousFiltered;
    private int locationUpdateCount;

    void reset() {
        lastFiltered = null;
        previousFiltered = null;
        locationUpdateCount = 0;
        liveLocationCoordinator.reset();
        recentFilteredLocations.clear();
    }

    void onProviderDisabled(@NonNull String provider) {
        liveLocationCoordinator.clearProvider(provider);
    }

    @Nullable
    Location getLastFilteredLocation() {
        return lastFiltered;
    }

    @NonNull
    Update onRawLocationChanged(@NonNull Location location) {
        liveLocationCoordinator.remember(location);
        Location selected = liveLocationCoordinator.selectBestLiveLocation();
        if (selected == null) {
            AppLogger.d(TAG, "Dropped location because no recent candidate is available raw="
                    + formatLocation(location));
            return Update.dropped();
        }
        if (!liveLocationCoordinator.shouldDispatch(selected)) {
            AppLogger.d(TAG, "Dropped location because selected candidate is unchanged raw="
                    + formatLocation(location)
                    + " selected=" + formatLocation(selected));
            return Update.dropped();
        }
        liveLocationCoordinator.markDispatched(selected);

        Location filtered = kalman.update(selected);
        if (filtered == null) {
            AppLogger.d(TAG, "Kalman filter dropped location " + formatLocation(selected));
            return Update.dropped();
        }

        previousFiltered = lastFiltered;
        lastFiltered = filtered;
        rememberFilteredLocation(filtered);
        locationUpdateCount++;
        AppLogger.d(TAG, "Location update #" + locationUpdateCount
                + " raw=" + formatLocation(selected)
                + " filtered=" + formatLocation(filtered));
        return Update.accepted(filtered);
    }

    boolean isLikelyStationary() {
        if (lastFiltered == null) {
            return false;
        }
        if (speedMps(lastFiltered) > MAX_STATIONARY_REPORTED_SPEED_MPS) {
            return false;
        }
        pruneRecentFilteredLocations(lastFiltered.getTime());
        if (recentFilteredLocations.size() < 2) {
            return true;
        }
        Location previous = null;
        double cumulativeDistanceMeters = 0.0;
        for (Location sample : recentFilteredLocations) {
            if (previous != null) {
                cumulativeDistanceMeters += GeoMath.distanceMeters(
                        previous.getLatitude(),
                        previous.getLongitude(),
                        sample.getLatitude(),
                        sample.getLongitude()
                );
                if (cumulativeDistanceMeters > MAX_STATIONARY_RECENT_DISTANCE_METERS) {
                    return false;
                }
            }
            previous = sample;
        }
        return true;
    }

    float speedMps(@NonNull Location location) {
        if (location.hasSpeed()) {
            return Math.max(0f, location.getSpeed());
        }
        if (previousFiltered != null) {
            double distanceMeters = GeoMath.distanceMeters(
                    previousFiltered.getLatitude(),
                    previousFiltered.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            double deltaSeconds = Math.max(1.0, (location.getTime() - previousFiltered.getTime()) / 1000.0);
            return (float) (distanceMeters / deltaSeconds);
        }
        return 0f;
    }

    @Nullable
    Double actualBearingDegrees(@NonNull Location location) {
        if (location.hasBearing() && speedMps(location) > MIN_RAW_BEARING_SPEED_MPS) {
            return (double) location.getBearing();
        }
        return resolveMovementBearingDegrees(location);
    }

    @Nullable
    Double trustedActualBearingDegreesForReroute(@NonNull Location location) {
        Double gpsBearingDegrees = trustedGpsBearingDegrees(location);
        if (gpsBearingDegrees != null) {
            return gpsBearingDegrees;
        }
        return resolveMovementBearingDegrees(location);
    }

    float accuracyMeters(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    static final class Update {
        private final boolean dropped;
        @Nullable
        private final Location filteredLocation;

        private Update(boolean dropped, @Nullable Location filteredLocation) {
            this.dropped = dropped;
            this.filteredLocation = filteredLocation;
        }

        @NonNull
        static Update dropped() {
            return new Update(true, null);
        }

        @NonNull
        static Update accepted(@NonNull Location filteredLocation) {
            return new Update(false, filteredLocation);
        }

        boolean isDropped() {
            return dropped;
        }

        @NonNull
        Location getFilteredLocation() {
            if (filteredLocation == null) {
                throw new IllegalStateException("Filtered location is unavailable for a dropped update");
            }
            return filteredLocation;
        }
    }

    @NonNull
    private static String formatLocation(@Nullable Location location) {
        if (location == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(location.getProvider())
                .append("(")
                .append(location.getLatitude())
                .append(",")
                .append(location.getLongitude())
                .append(")");
        if (location.hasAccuracy()) {
            sb.append(" acc=").append(location.getAccuracy());
        }
        if (location.hasSpeed()) {
            sb.append(" speed=").append(location.getSpeed());
        }
        if (location.hasBearing()) {
            sb.append(" bearing=").append(location.getBearing());
        }
        sb.append(" time=").append(location.getTime());
        return sb.toString();
    }

    private void rememberFilteredLocation(@NonNull Location filtered) {
        recentFilteredLocations.addLast(new Location(filtered));
        pruneRecentFilteredLocations(filtered.getTime());
    }

    private void pruneRecentFilteredLocations(long newestTimeMs) {
        long cutoffTimeMs = newestTimeMs - RECENT_MOTION_WINDOW_MS;
        while (recentFilteredLocations.size() > 1
                && recentFilteredLocations.peekFirst() != null
                && recentFilteredLocations.peekFirst().getTime() < cutoffTimeMs) {
            recentFilteredLocations.removeFirst();
        }
    }

    @Nullable
    private Double trustedGpsBearingDegrees(@NonNull Location location) {
        if (!location.hasBearing()) {
            return null;
        }
        float speedMps = speedMps(location);
        if (speedMps < MIN_TRUSTED_GPS_BEARING_SPEED_MPS) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
            float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
            if (Float.isFinite(bearingAccuracyDegrees)
                    && bearingAccuracyDegrees >= 0f
                    && bearingAccuracyDegrees <= MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES) {
                return (double) location.getBearing();
            }
            return null;
        }
        return speedMps >= MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS
                ? (double) location.getBearing()
                : null;
    }

    @Nullable
    private Double resolveMovementBearingDegrees(@NonNull Location location) {
        pruneRecentFilteredLocations(location.getTime());
        for (Location sample : recentFilteredLocations) {
            long elapsedMs = location.getTime() - sample.getTime();
            if (elapsedMs < MIN_MOVEMENT_BEARING_ELAPSED_MS) {
                continue;
            }
            double distanceMeters = GeoMath.distanceMeters(
                    sample.getLatitude(),
                    sample.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            if (distanceMeters < MIN_MOVEMENT_BEARING_DISTANCE_METERS) {
                continue;
            }
            return GeoMath.bearingDegrees(
                    sample.getLatitude(),
                    sample.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
        }
        return null;
    }
}

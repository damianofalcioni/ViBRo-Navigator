package com.vibenavigator.nav;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.nav.kalman.LatLonKalmanFilter;
import com.vibenavigator.util.AppLogger;

final class NavigationSessionLocationState {

    private static final String TAG = "NavSessionLocation";

    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();
    private final LiveLocationCoordinator liveLocationCoordinator = new LiveLocationCoordinator();

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
        locationUpdateCount++;
        AppLogger.d(TAG, "Location update #" + locationUpdateCount
                + " raw=" + formatLocation(selected)
                + " filtered=" + formatLocation(filtered));
        return Update.accepted(filtered);
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
        if (location.hasBearing() && speedMps(location) > 1.0f) {
            return (double) location.getBearing();
        }
        if (previousFiltered != null) {
            double distanceMeters = GeoMath.distanceMeters(
                    previousFiltered.getLatitude(),
                    previousFiltered.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            if (distanceMeters < 3.0) {
                return null;
            }
            return GeoMath.bearingDegrees(
                    previousFiltered.getLatitude(),
                    previousFiltered.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
        }
        return null;
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
}

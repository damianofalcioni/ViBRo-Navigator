package vibro.navigator.nav;

import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import vibro.navigator.nav.kalman.LatLonKalmanFilter;
import vibro.navigator.util.AppLogger;

final class NavigationSessionLocationState {

    private static final String TAG = "NavSessionLocation";
    private static final float MIN_RAW_BEARING_SPEED_MPS = 1.0f;
    private static final float MIN_TRUSTED_GPS_BEARING_SPEED_MPS = 0.8f;
    private static final float MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS = 1.5f;
    private static final float MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES = 25f;

    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();
    private final LiveLocationCoordinator liveLocationCoordinator = new LiveLocationCoordinator();
    private final NavigationLocationMotionModel motionModel = new NavigationLocationMotionModel();

    private int locationUpdateCount;

    void reset() {
        kalman.reset();
        motionModel.reset();
        locationUpdateCount = 0;
        liveLocationCoordinator.reset();
    }

    void onProviderDisabled(@NonNull String provider) {
        liveLocationCoordinator.clearProvider(provider);
    }

    @Nullable
    Location getLastFilteredLocation() {
        return motionModel.getLastFilteredLocation();
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

        motionModel.recordFilteredLocation(filtered);
        locationUpdateCount++;
        AppLogger.d(TAG, "Location update #" + locationUpdateCount
                + " raw=" + formatLocation(selected)
                + " filtered=" + formatLocation(filtered));
        return Update.accepted(filtered);
    }

    boolean isLikelyStationary() {
        return motionModel.isLikelyStationary();
    }

    float speedMps(@NonNull Location location) {
        return motionModel.speedMps(location);
    }

    @Nullable
    Double actualBearingDegrees(@NonNull Location location) {
        if (location.hasBearing() && speedMps(location) > MIN_RAW_BEARING_SPEED_MPS) {
            return (double) location.getBearing();
        }
        return motionModel.movementBearingDegrees(location);
    }

    @Nullable
    HeadingEstimate preferredCompassHeading(@NonNull Location location, boolean likelyStationary) {
        if (likelyStationary) {
            return null;
        }
        Double gpsBearingDegrees = trustedGpsBearingDegrees(location);
        if (gpsBearingDegrees != null) {
            return new HeadingEstimate(gpsBearingDegrees, currentBearingAccuracyDegrees(location));
        }
        Double movementBearingDegrees = motionModel.movementBearingDegrees(location);
        return movementBearingDegrees == null
                ? null
                : new HeadingEstimate(movementBearingDegrees, null);
    }

    @Nullable
    Double trustedActualBearingDegreesForReroute(@NonNull Location location) {
        Double gpsBearingDegrees = trustedGpsBearingDegrees(location);
        if (gpsBearingDegrees != null) {
            return gpsBearingDegrees;
        }
        return motionModel.movementBearingDegrees(location);
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

    static final class HeadingEstimate {
        final double headingDegrees;
        @Nullable
        final Float headingAccuracyDegrees;

        private HeadingEstimate(double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
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
            if (hasTrustedBearingAccuracy(location)) {
                return (double) location.getBearing();
            }
            return null;
        }
        return speedMps >= MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS
                ? (double) location.getBearing()
                : null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static boolean hasTrustedBearingAccuracy(@NonNull Location location) {
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees)
                && bearingAccuracyDegrees >= 0f
                && bearingAccuracyDegrees <= MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES;
    }

    @Nullable
    private Float currentBearingAccuracyDegrees(@NonNull Location location) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !location.hasBearingAccuracy()) {
            return null;
        }
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees) && bearingAccuracyDegrees >= 0f
                ? bearingAccuracyDegrees
                : null;
    }

}

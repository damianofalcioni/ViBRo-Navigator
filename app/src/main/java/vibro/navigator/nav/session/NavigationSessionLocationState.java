package vibro.navigator.nav.session;


import vibro.navigator.nav.location.LiveLocationCoordinator;
import vibro.navigator.nav.location.NavigationLocationMotionModel;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.kalman.LatLonKalmanFilter;
import vibro.navigator.logging.AppLogger;

public final class NavigationSessionLocationState {

    private static final String TAG = "NavSessionLocation";
    private static final float MIN_RAW_BEARING_SPEED_MPS = 1.0f;
    private static final float MIN_COURSE_HEADING_DISPLAY_SPEED_MPS = 2.5f;

    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();
    private final LiveLocationCoordinator liveLocationCoordinator = new LiveLocationCoordinator();
    private final NavigationLocationMotionModel motionModel = new NavigationLocationMotionModel();
    private final NavigationLocationReacquisitionTracker reacquisitionTracker =
            new NavigationLocationReacquisitionTracker();
    private final NavigationGpsBearingTrustPolicy bearingTrustPolicy = new NavigationGpsBearingTrustPolicy();

    private int locationUpdateCount;

    public void reset() {
        kalman.reset();
        motionModel.reset();
        locationUpdateCount = 0;
        reacquisitionTracker.reset();
        liveLocationCoordinator.reset();
    }

    public void onProviderDisabled(@NonNull String provider) {
        liveLocationCoordinator.clearProvider(provider);
    }

    @Nullable
    public NavigationLocation getLastFilteredLocation() {
        return motionModel.getLastFilteredLocation();
    }

    @NonNull
    public Update onRawLocationChanged(@NonNull NavigationLocation rawLocation) {
        return onRawLocationChanged(rawLocation, System.currentTimeMillis());
    }

    @NonNull
    public Update onRawLocationChanged(@NonNull NavigationLocation rawLocation, long nowMs) {
        liveLocationCoordinator.remember(rawLocation);
        NavigationLocation selected = liveLocationCoordinator.selectBestLiveLocation();
        if (selected == null) {
            AppLogger.d(TAG, "Dropped NavigationLocation because no recent candidate is available raw="
                    + formatLocation(rawLocation));
            return Update.dropped();
        }
        if (!liveLocationCoordinator.shouldDispatch(selected)) {
            AppLogger.d(TAG, "Dropped NavigationLocation because selected candidate is unchanged raw="
                    + formatLocation(rawLocation)
                    + " selected=" + formatLocation(selected));
            return Update.dropped();
        }
        liveLocationCoordinator.markDispatched(selected);

        boolean reacquiringAfterLongGap = reacquisitionTracker.isReacquiring(nowMs);
        if (reacquiringAfterLongGap) {
            kalman.reset();
            motionModel.reset();
            AppLogger.i(TAG, "Reacquiring NavigationLocation after long accepted-fix gap raw="
                    + formatLocation(selected)
                    + " gapMs=" + reacquisitionTracker.gapMs(nowMs));
        }
        NavigationLocation filtered = kalman.update(selected);
        if (filtered == null) {
            AppLogger.d(TAG, "Kalman filter dropped NavigationLocation " + formatLocation(selected));
            return Update.dropped();
        }

        motionModel.recordFilteredLocation(filtered);
        locationUpdateCount++;
        reacquisitionTracker.recordAccepted(nowMs);
        AppLogger.d(TAG, "NavigationLocation update #" + locationUpdateCount
                + " raw=" + formatLocation(selected)
                + " filtered=" + formatLocation(filtered));
        return Update.accepted(filtered, reacquiringAfterLongGap);
    }

    public boolean isLikelyStationary() {
        return motionModel.isLikelyStationary();
    }

    public float speedMps(@NonNull NavigationLocation location) {
        return motionModel.speedMps(location);
    }

    @Nullable
    public Double actualBearingDegrees(@NonNull NavigationLocation location) {
        if (location.hasBearing() && speedMps(location) > MIN_RAW_BEARING_SPEED_MPS) {
            return (double) location.getBearing();
        }
        return motionModel.movementBearingDegrees(location);
    }

    @Nullable
    public HeadingEstimate preferredCompassHeading(@NonNull NavigationLocation location, boolean likelyStationary) {
        if (likelyStationary) {
            return null;
        }
        if (speedMps(location) < MIN_COURSE_HEADING_DISPLAY_SPEED_MPS) {
            return null;
        }
        Double gpsBearingDegrees = bearingTrustPolicy.trustedBearingDegrees(location, speedMps(location));
        if (gpsBearingDegrees != null) {
            return new HeadingEstimate(gpsBearingDegrees, bearingTrustPolicy.currentBearingAccuracyDegrees(location));
        }
        Double movementBearingDegrees = motionModel.movementBearingDegrees(location);
        return movementBearingDegrees == null
                ? null
                : new HeadingEstimate(movementBearingDegrees, null);
    }

    @Nullable
    public Double trustedActualBearingDegreesForReroute(@NonNull NavigationLocation location) {
        Double gpsBearingDegrees = bearingTrustPolicy.trustedBearingDegrees(location, speedMps(location));
        if (gpsBearingDegrees != null) {
            return gpsBearingDegrees;
        }
        return motionModel.movementBearingDegrees(location);
    }

    public float accuracyMeters(@NonNull NavigationLocation location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    public static final class Update {
        private final boolean dropped;
        private final boolean reacquiringAfterLongGap;
        @Nullable
        private final NavigationLocation filteredLocation;

        private Update(boolean dropped, boolean reacquiringAfterLongGap, @Nullable NavigationLocation filteredLocation) {
            this.dropped = dropped;
            this.reacquiringAfterLongGap = reacquiringAfterLongGap;
            this.filteredLocation = filteredLocation;
        }

        @NonNull
        public static Update dropped() {
            return new Update(true, false, null);
        }

        @NonNull
        public static Update accepted(@NonNull NavigationLocation filteredLocation) {
            return accepted(filteredLocation, false);
        }

        @NonNull
        public static Update accepted(@NonNull NavigationLocation filteredLocation, boolean reacquiringAfterLongGap) {
            return new Update(false, reacquiringAfterLongGap, filteredLocation);
        }

        public boolean isDropped() {
            return dropped;
        }

        public boolean isReacquiringAfterLongGap() {
            return reacquiringAfterLongGap;
        }

        @NonNull
        public NavigationLocation getFilteredLocation() {
            if (filteredLocation == null) {
                throw new IllegalStateException("Filtered NavigationLocation is unavailable for a dropped update");
            }
            return filteredLocation;
        }
    }

    public static final class HeadingEstimate {
        final double headingDegrees;
        @Nullable
        final Float headingAccuracyDegrees;

        private HeadingEstimate(double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
        }
    }

    @NonNull
    private static String formatLocation(@Nullable NavigationLocation location) {
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

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
    public Update onRawLocationChanged(@NonNull NavigationLocation NavigationLocation) {
        return onRawLocationChanged(NavigationLocation, System.currentTimeMillis());
    }

    @NonNull
    public Update onRawLocationChanged(@NonNull NavigationLocation NavigationLocation, long nowMs) {
        liveLocationCoordinator.remember(NavigationLocation);
        NavigationLocation selected = liveLocationCoordinator.selectBestLiveLocation();
        if (selected == null) {
            AppLogger.d(TAG, "Dropped NavigationLocation because no recent candidate is available raw="
                    + formatLocation(NavigationLocation));
            return Update.dropped();
        }
        if (!liveLocationCoordinator.shouldDispatch(selected)) {
            AppLogger.d(TAG, "Dropped NavigationLocation because selected candidate is unchanged raw="
                    + formatLocation(NavigationLocation)
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

    public float speedMps(@NonNull NavigationLocation NavigationLocation) {
        return motionModel.speedMps(NavigationLocation);
    }

    @Nullable
    public Double actualBearingDegrees(@NonNull NavigationLocation NavigationLocation) {
        if (NavigationLocation.hasBearing() && speedMps(NavigationLocation) > MIN_RAW_BEARING_SPEED_MPS) {
            return (double) NavigationLocation.getBearing();
        }
        return motionModel.movementBearingDegrees(NavigationLocation);
    }

    @Nullable
    public HeadingEstimate preferredCompassHeading(@NonNull NavigationLocation NavigationLocation, boolean likelyStationary) {
        if (likelyStationary) {
            return null;
        }
        if (speedMps(NavigationLocation) < MIN_COURSE_HEADING_DISPLAY_SPEED_MPS) {
            return null;
        }
        Double gpsBearingDegrees = bearingTrustPolicy.trustedBearingDegrees(NavigationLocation, speedMps(NavigationLocation));
        if (gpsBearingDegrees != null) {
            return new HeadingEstimate(gpsBearingDegrees, bearingTrustPolicy.currentBearingAccuracyDegrees(NavigationLocation));
        }
        Double movementBearingDegrees = motionModel.movementBearingDegrees(NavigationLocation);
        return movementBearingDegrees == null
                ? null
                : new HeadingEstimate(movementBearingDegrees, null);
    }

    @Nullable
    public Double trustedActualBearingDegreesForReroute(@NonNull NavigationLocation NavigationLocation) {
        Double gpsBearingDegrees = bearingTrustPolicy.trustedBearingDegrees(NavigationLocation, speedMps(NavigationLocation));
        if (gpsBearingDegrees != null) {
            return gpsBearingDegrees;
        }
        return motionModel.movementBearingDegrees(NavigationLocation);
    }

    public float accuracyMeters(@NonNull NavigationLocation NavigationLocation) {
        return NavigationLocation.hasAccuracy() ? NavigationLocation.getAccuracy() : Float.MAX_VALUE;
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
    private static String formatLocation(@Nullable NavigationLocation NavigationLocation) {
        if (NavigationLocation == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(NavigationLocation.getProvider())
                .append("(")
                .append(NavigationLocation.getLatitude())
                .append(",")
                .append(NavigationLocation.getLongitude())
                .append(")");
        if (NavigationLocation.hasAccuracy()) {
            sb.append(" acc=").append(NavigationLocation.getAccuracy());
        }
        if (NavigationLocation.hasSpeed()) {
            sb.append(" speed=").append(NavigationLocation.getSpeed());
        }
        if (NavigationLocation.hasBearing()) {
            sb.append(" bearing=").append(NavigationLocation.getBearing());
        }
        sb.append(" time=").append(NavigationLocation.getTime());
        return sb.toString();
    }

}

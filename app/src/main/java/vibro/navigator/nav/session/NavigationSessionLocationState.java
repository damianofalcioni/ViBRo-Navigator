package vibro.navigator.nav.session;


import vibro.navigator.nav.location.LiveLocationCoordinator;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationMotionModel;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.startup.NavigationStartupLocationSelector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.kalman.LatLonKalmanFilter;

public final class NavigationSessionLocationState {

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
    public Update onRawLocationChanged(@NonNull NavigationLocation rawLocation, long nowMs) {
        return onRawLocationChanged(rawLocation, nowMs, true);
    }

    @NonNull
    public Update onRawLocationChanged(
            @NonNull NavigationLocation rawLocation,
            long nowMs,
            boolean allowStartupFilterReset
    ) {
        return onRawLocationChanged(
                rawLocation,
                nowMs,
                allowStartupFilterReset,
                NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS
        );
    }

    @NonNull
    public Update onRawLocationChanged(
            @NonNull NavigationLocation rawLocation,
            long nowMs,
            boolean allowStartupFilterReset,
            long expectedUpdateIntervalMs
    ) {
        liveLocationCoordinator.remember(rawLocation);
        LiveLocationCoordinator.Selection selection = liveLocationCoordinator.selectBestLiveSelection(nowMs);
        if (selection == null) {
            NavigationLocationDebugLogger.droppedNoRecentCandidate(rawLocation);
            return Update.dropped();
        }
        NavigationLocation selected = selection.location;
        if (!liveLocationCoordinator.shouldDispatch(selection, nowMs, expectedUpdateIntervalMs)) {
            NavigationLocationDebugLogger.droppedUnchanged(rawLocation, selected);
            return Update.dropped();
        }
        liveLocationCoordinator.markDispatched(selection, nowMs);

        boolean reacquiringAfterLongGap = reacquisitionTracker.isReacquiring(nowMs, expectedUpdateIntervalMs);
        if (reacquiringAfterLongGap) {
            kalman.reset();
            motionModel.reset();
            NavigationLocationDebugLogger.reacquiringAfterLongGap(
                    selected,
                    reacquisitionTracker.gapMs(nowMs)
            );
        } else if (shouldResetStartupFilter(allowStartupFilterReset, selected, nowMs)) {
            kalman.reset();
            motionModel.reset();
            NavigationLocationDebugLogger.resettingStartupFilter(selected);
        }
        NavigationLocation filtered = kalman.update(selected);
        if (filtered == null) {
            NavigationLocationDebugLogger.kalmanDropped(selected);
            return Update.dropped();
        }

        motionModel.recordFilteredLocation(filtered);
        locationUpdateCount++;
        reacquisitionTracker.recordAccepted(nowMs);
        NavigationLocationDebugLogger.accepted(locationUpdateCount, selected, filtered);
        return Update.accepted(filtered, reacquiringAfterLongGap);
    }

    public boolean isLikelyStationary() {
        return motionModel.isLikelyStationary();
    }

    public float speedMps(@NonNull NavigationLocation location) {
        return motionModel.speedMps(location);
    }

    public float displaySpeedMps(@NonNull NavigationLocation location) {
        return motionModel.displaySpeedMps(location);
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

    private boolean shouldResetStartupFilter(
            boolean allowStartupFilterReset,
            @NonNull NavigationLocation selected,
            long nowMs
    ) {
        if (!allowStartupFilterReset) {
            return false;
        }
        NavigationLocation lastFiltered = motionModel.getLastFilteredLocation();
        return lastFiltered != null
                && !NavigationStartupLocationSelector.isUsableForRouteStart(lastFiltered, nowMs)
                && NavigationStartupLocationSelector.isUsableForRouteStart(selected, nowMs);
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

}

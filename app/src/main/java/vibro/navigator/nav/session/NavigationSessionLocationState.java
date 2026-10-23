package vibro.navigator.nav.session;


import vibro.navigator.nav.location.LiveLocationCoordinator;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationMotionModel;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;
import vibro.navigator.nav.startup.NavigationStartupLocationSelector;
import vibro.navigator.nav.startup.NavigationStartupLocationSeed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.kalman.LatLonKalmanFilter;

public final class NavigationSessionLocationState {

    private static final float MIN_RAW_BEARING_SPEED_MPS = 1.0f;

    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();
    private final LiveLocationCoordinator liveLocationCoordinator = new LiveLocationCoordinator();
    private final NavigationLocationMotionModel motionModel = new NavigationLocationMotionModel();
    private final NavigationLocationReacquisitionTracker reacquisitionTracker =
            new NavigationLocationReacquisitionTracker();
    private final NavigationGpsBearingTrustPolicy bearingTrustPolicy = new NavigationGpsBearingTrustPolicy();
    private final NavigationLocationDiagnostics diagnostics = new NavigationLocationDiagnostics();

    private int locationUpdateCount;
    private int motionRevision;
    private final NavigationLocationDebugLogger debugLogger = new NavigationLocationDebugLogger();
    private String activeProviders;

    public void reset() {
        diagnostics.reset();
        kalman.reset();
        motionModel.reset();
        motionRevision++;
        locationUpdateCount = 0;
        debugLogger.reset();
        activeProviders = null;
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
        return onSelectedLocation(rawLocation, selectRawLocation(rawLocation, nowMs), nowMs,
                allowStartupFilterReset, expectedUpdateIntervalMs);
    }

    @NonNull
    public Update onStartupLocationSeed(@NonNull NavigationLocation seed, long nowMs, long expectedUpdateIntervalMs) {
        return onSelectedLocation(seed, NavigationStartupLocationSeed.select(seed, nowMs, locationUpdateCount,
                activeProviders, liveLocationCoordinator), nowMs, true, expectedUpdateIntervalMs);
    }

    @NonNull
    private Update onSelectedLocation(NavigationLocation rawLocation, @Nullable LiveLocationCoordinator.Selection selection,
            long nowMs, boolean allowStartupFilterReset, long expectedUpdateIntervalMs) {
        debugLogger.incoming(rawLocation, nowMs, expectedUpdateIntervalMs, reacquisitionTracker.gapMs(nowMs));
        diagnostics.incoming(rawLocation.getProvider(), nowMs);
        if (selection == null) {
            diagnostics.dropped(false);
            NavigationLocationDebugLogger.droppedNoRecentCandidate(rawLocation, activeProviders);
            return Update.dropped();
        }
        NavigationLocation selected = selection.location;
        String rejectionReason = liveLocationCoordinator.dispatchRejectionReason(selection, nowMs, expectedUpdateIntervalMs);
        if (rejectionReason != null) {
            boolean corrected = kalman.correctDelayedNetworkPosition(rawLocation, nowMs);
            diagnostics.dropped(corrected);
            NavigationLocationDebugLogger.dropped(rawLocation, selected, nowMs, rejectionReason, corrected);
            return Update.dropped();
        }
        boolean reacquiringAfterLongGap = reacquisitionTracker.isReacquiring(nowMs, expectedUpdateIntervalMs);
        if (reacquiringAfterLongGap) {
            kalman.reset();
            motionModel.reset();
            motionRevision++;
            NavigationLocationDebugLogger.reacquiringAfterLongGap(
                    selected,
                    reacquisitionTracker.gapMs(nowMs)
            );
        } else if (NavigationStartupLocationSelector.shouldResetFilter(
                motionModel.getLastFilteredLocation(), selected, nowMs, allowStartupFilterReset)) {
            kalman.reset();
            motionModel.reset();
            motionRevision++;
            NavigationLocationDebugLogger.resettingStartupFilter(selected);
        }
        NavigationLocation filtered = kalman.update(selected);
        if (filtered == null) {
            diagnostics.dropped(false);
            NavigationLocationDebugLogger.kalmanDropped(selected, kalman.getLastRejectionReason());
            return Update.dropped();
        }

        liveLocationCoordinator.markDispatched(selection, nowMs);
        motionModel.recordFilteredLocation(filtered);
        locationUpdateCount++;
        diagnostics.accepted(selected.getProvider());
        reacquisitionTracker.recordAccepted(nowMs);
        NavigationLocationDebugLogger.accepted(locationUpdateCount, selected, filtered,
                motionModel.isLikelyStationary(), motionModel.speedMps(filtered), motionModel.needsDepartureChecks());
        return Update.accepted(filtered, reacquiringAfterLongGap);
    }

    private LiveLocationCoordinator.Selection selectRawLocation(NavigationLocation location, long nowMs) {
        if (!NavigationLocationProviders.isActive(activeProviders, location.getProvider())) {
            return null;
        }
        liveLocationCoordinator.remember(location);
        return liveLocationCoordinator.selectBestLiveSelection(nowMs);
    }

    public boolean isLikelyStationary() {
        return motionModel.isLikelyStationary();
    }

    boolean needsDepartureChecks() {
        return motionModel.needsDepartureChecks();
    }

    public void setActiveProviders(@NonNull String providers) {
        activeProviders = providers;
        for (String provider : new String[] {NavigationLocationProviders.GPS_PROVIDER,
                NavigationLocationProviders.NETWORK_PROVIDER, LiveLocationCoordinator.FUSED_PROVIDER}) {
            if (!NavigationLocationProviders.isActive(activeProviders, provider)) {
                liveLocationCoordinator.clearProvider(provider);
            }
        }
    }

    int motionRevision() {
        return motionRevision;
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
    public HeadingEstimate preferredLocationHeading(@NonNull NavigationLocation location, boolean likelyStationary) {
        if (likelyStationary) {
            return null;
        }
        Double gpsBearingDegrees = bearingTrustPolicy.trustedDisplayBearingDegrees(location, speedMps(location));
        if (gpsBearingDegrees != null) {
            return new HeadingEstimate(gpsBearingDegrees, bearingTrustPolicy.currentBearingAccuracyDegrees(location));
        }
        NavigationLocationMotionModel.Course course = motionModel.displayMovementCourse(location);
        return course == null || course.accuracyDegrees > 25f
                ? null
                : new HeadingEstimate(course.headingDegrees, course.accuracyDegrees);
    }

    @Nullable
    HeadingEstimate routeDisagreementHeading(@NonNull NavigationLocation location) {
        HeadingEstimate preferred = preferredLocationHeading(location, false);
        if (preferred != null && preferred.headingAccuracyDegrees != null) {
            return preferred;
        }
        Double bearing = bearingTrustPolicy.displayDisagreementBearingDegrees(location, speedMps(location));
        return bearing == null ? null
                : new HeadingEstimate(bearing, bearingTrustPolicy.currentBearingAccuracyDegrees(location));
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

}

package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

public final class NavigationLocationController {

    private static final String TAG = "NavLocation";
    public static final long DEFAULT_UPDATE_INTERVAL_MS = 3_000L;
    public static final long STARTUP_UPDATE_INTERVAL_MS = 1_000L;

    private final NavigationLocationProvider providerAccess;
    private final NavigationGnssTracker gnssStatusTracker;
    private final FusedLocationUpdateClient fusedLocationUpdateClient;
    private final NavigationLocationUpdateRequester updateRequester;
    private final FusedLocationUsePolicy fusedLocationUsePolicy;
    private final ElapsedRealtimeClock elapsedRealtimeClock;

    private long lastRequestedLocationMinTimeMs = -1L;
    @Nullable
    private String lastRequestedProvider;
    private long nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
    private boolean legacyRecoveryActive;
    private boolean stationarySuspended;
    private final NavigationLocationAcquisition acquisition;
    private final NavigationLocationSeeds seeds;

    public NavigationLocationController(
            @NonNull NavigationLocationProvider providerAccess,
            @NonNull NavigationGnssTracker gnssStatusTracker,
            @NonNull FusedLocationUpdateClient fusedLocationUpdateClient,
            @NonNull FusedLocationUsePolicy fusedLocationUsePolicy,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.providerAccess = providerAccess;
        this.gnssStatusTracker = gnssStatusTracker;
        this.fusedLocationUpdateClient = fusedLocationUpdateClient;
        this.fusedLocationUsePolicy = fusedLocationUsePolicy;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        seeds = new NavigationLocationSeeds(providerAccess, fusedLocationUpdateClient);
        this.acquisition = new NavigationLocationAcquisition(providerAccess, elapsedRealtimeClock,
                interval -> requestLocationUpdates(interval, Math.min(DEFAULT_UPDATE_INTERVAL_MS, interval)));
        this.updateRequester = new NavigationLocationUpdateRequester(
                providerAccess,
                fusedLocationUpdateClient,
                gnssStatusTracker,
                providerAccess::removeUpdates,
                this::describeAvailability
        );
        fusedLocationUpdateClient.setUpdateFailureListener(this::recoverAfterFusedFailure);
    }

    public void resetTrackingState() {
        stationarySuspended = false;
        acquisition.reset();
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        legacyRecoveryActive = false;
        providerAccess.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.removeUpdates();
        gnssStatusTracker.reset();
        providerAccess.removeUpdates();
    }

    public void stopTracking() {
        stationarySuspended = false;
        acquisition.reset();
        providerAccess.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.cancelCurrentLocationSeed();
        fusedLocationUpdateClient.removeUpdates();
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        lastRequestedProvider = null;
        gnssStatusTracker.reset();
        providerAccess.removeUpdates();
    }

    public void suspendForStationarySleep(long checkDeadlineMs) {
        stopTracking();
        stationarySuspended = true;
        nextEvaluationDeadlineElapsedMs = checkDeadlineMs;
    }

    public boolean isStationarySuspended() {
        return stationarySuspended;
    }

    public void resumeAfterStationarySleep() {
        stationarySuspended = false;
        requestFastLocationUpdates();
    }

    public void requestLocationUpdates(long minTimeMs) {
        requestLocationUpdates(minTimeMs, DEFAULT_UPDATE_INTERVAL_MS);
    }

    public void requestStartupLocationUpdates() {
        requestFastLocationUpdates();
    }

    public void requestFastLocationUpdates() {
        requestLocationUpdates(STARTUP_UPDATE_INTERVAL_MS, STARTUP_UPDATE_INTERVAL_MS);
    }

    public void restartActiveLocationUpdates(long fallbackMinTimeMs) {
        long activeMinTimeMs = getLastRequestedLocationMinTimeMsOrDefault(fallbackMinTimeMs);
        if (LiveLocationCoordinator.FUSED_PROVIDER.equals(lastRequestedProvider)) {
            legacyRecoveryActive = true;
            AppLogger.w(TAG, "Fused updates stalled; activating legacy location recovery");
        }
        acquisition.reset();
        providerAccess.removeUpdates();
        providerAccess.cancelPendingCurrentLocationRequests();
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        requestLocationUpdates(
                activeMinTimeMs,
                Math.min(DEFAULT_UPDATE_INTERVAL_MS, activeMinTimeMs)
        );
    }

    private void recoverAfterFusedFailure() {
        if (!LiveLocationCoordinator.FUSED_PROVIDER.equals(lastRequestedProvider)) {
            return;
        }
        legacyRecoveryActive = true;
        acquisition.reset();
        AppLogger.w(TAG, "Fused update request failed asynchronously; activating legacy location recovery");
        long activeMinTimeMs = getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_UPDATE_INTERVAL_MS);
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        requestLocationUpdates(activeMinTimeMs, Math.min(DEFAULT_UPDATE_INTERVAL_MS, activeMinTimeMs));
    }

    private void requestLocationUpdates(long minTimeMs, long minimumIntervalMs) {
        if (stationarySuspended) {
            return;
        }
        long requestedMinTimeMs = Math.max(minimumIntervalMs, minTimeMs);
        boolean preferFusedLocation = fusedLocationUsePolicy.shouldUseFusedLocation()
                && !legacyRecoveryActive;
        long networkMinTimeMs = acquisition.intervalForRequest(preferFusedLocation, requestedMinTimeMs);
        NavigationLocationUpdateRequester.Result result = updateRequester.request(
                requestedMinTimeMs,
                preferFusedLocation,
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider,
                networkMinTimeMs,
                acquisition.lastNetworkIntervalMs()
        );
        if (result.shouldClearActiveRequest()) {
            clearActiveLocationRequest();
            return;
        }
        refreshNextEvaluationDeadline(requestedMinTimeMs);
        lastRequestedLocationMinTimeMs = requestedMinTimeMs;
        lastRequestedProvider = result.activeProviderSummary();
        if (preferFusedLocation && !result.isFusedActive()) {
            legacyRecoveryActive = true;
            AppLogger.w(TAG, "Fused request failed; keeping legacy location recovery active");
        }
        acquisition.requested(lastRequestedProvider, requestedMinTimeMs, networkMinTimeMs);
    }

    @NonNull
    public NavigationLocationAcquisition acquisition() {
        return acquisition;
    }

    public void requestCurrentLocationSeeds() {
        seeds.request(stationarySuspended, fusedLocationUsePolicy.shouldUseFusedLocation() && !legacyRecoveryActive);
    }

    public void cancelCurrentLocationSeeds() {
        providerAccess.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.cancelCurrentLocationSeed();
    }

    public void onProviderEnabled(
            @NonNull String provider,
            long fallbackMinTimeMs,
            boolean requestCurrentLocationSeed
    ) {
        requestLocationUpdates(fallbackMinTimeMs, DEFAULT_UPDATE_INTERVAL_MS);
        seeds.requestEnabled(provider, requestCurrentLocationSeed, stationarySuspended);
    }

    public void onProviderEnabledFast(@NonNull String provider, boolean requestCurrentLocationSeed) {
        requestFastLocationUpdates();
        seeds.requestEnabled(provider, requestCurrentLocationSeed, stationarySuspended);
    }

    public long getLastRequestedLocationMinTimeMsOrDefault(long fallbackMinTimeMs) {
        return lastRequestedLocationMinTimeMs > 0L ? lastRequestedLocationMinTimeMs : fallbackMinTimeMs;
    }

    @NonNull
    public String activeProviderSummary() {
        return lastRequestedProvider == null ? "none" : lastRequestedProvider;
    }

    public long getNextEvaluationDeadlineElapsedMs() {
        return nextEvaluationDeadlineElapsedMs;
    }

    public void recordAcceptedLocationUpdate() {
        if (lastRequestedLocationMinTimeMs <= 0L || lastRequestedProvider == null) {
            return;
        }
        refreshNextEvaluationDeadline(lastRequestedLocationMinTimeMs);
    }

    public void setGnssStatusTrackingAllowed(boolean allowed) {
        gnssStatusTracker.setTrackingAllowed(allowed);
    }

    @Nullable
    public Integer getFixedSatelliteCount() {
        return gnssStatusTracker.getFixedSatelliteCount();
    }

    @Nullable
    public NavigationLocation getBestStartupLastKnownLocation(long nowMs) {
        return seeds.bestStartup(nowMs);
    }

    @NonNull
    public String describeAvailability() {
        return providerAccess.describeAvailability() + ", " + fusedLocationUpdateClient.describeAvailability()
                + ", fusedPreferred=" + fusedLocationUsePolicy.shouldUseFusedLocation()
                + ", activeProviders=" + activeProviderSummary()
                + ", networkMinTimeMs=" + acquisition.lastNetworkIntervalMs();
    }

    private void clearActiveLocationRequest() {
        acquisition.reset();
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        gnssStatusTracker.reset();
        fusedLocationUpdateClient.removeUpdates();
        providerAccess.removeUpdates();
    }

    private void refreshNextEvaluationDeadline(long minTimeMs) {
        nextEvaluationDeadlineElapsedMs = elapsedRealtimeClock.elapsedRealtimeMs() + minTimeMs;
    }

    public interface FusedLocationUsePolicy {
        boolean shouldUseFusedLocation();
    }

}

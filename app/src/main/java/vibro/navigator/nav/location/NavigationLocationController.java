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
        this.updateRequester = new NavigationLocationUpdateRequester(
                providerAccess,
                fusedLocationUpdateClient,
                gnssStatusTracker,
                providerAccess::removeUpdates,
                this::describeAvailability
        );
    }

    public void resetTrackingState() {
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        providerAccess.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.removeUpdates();
        gnssStatusTracker.reset();
        providerAccess.removeUpdates();
    }

    public void stopTracking() {
        providerAccess.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.removeUpdates();
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        lastRequestedProvider = null;
        gnssStatusTracker.reset();
        providerAccess.removeUpdates();
    }

    public void requestLocationUpdates(long minTimeMs) {
        requestLocationUpdates(minTimeMs, DEFAULT_UPDATE_INTERVAL_MS);
    }

    public void requestStartupLocationUpdates() {
        requestLocationUpdates(STARTUP_UPDATE_INTERVAL_MS, STARTUP_UPDATE_INTERVAL_MS);
    }

    private void requestLocationUpdates(long minTimeMs, long minimumIntervalMs) {
        long requestedMinTimeMs = Math.max(minimumIntervalMs, minTimeMs);
        NavigationLocationUpdateRequester.Result result = updateRequester.request(
                requestedMinTimeMs,
                fusedLocationUsePolicy.shouldUseFusedLocation(),
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider
        );
        if (result.shouldClearActiveRequest()) {
            clearActiveLocationRequest();
            return;
        }
        if (!result.hasActiveRequest()) {
            return;
        }
        refreshNextEvaluationDeadline(requestedMinTimeMs);
        lastRequestedLocationMinTimeMs = requestedMinTimeMs;
        lastRequestedProvider = result.activeProviderSummary();
    }

    public void requestCurrentLocationSeeds() {
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            return;
        }
        if (fusedLocationUsePolicy.shouldUseFusedLocation()) {
            fusedLocationUpdateClient.requestCurrentLocationSeed(fineGranted, coarseGranted);
        }
        providerAccess.requestCurrentLocationSeeds(fineGranted, coarseGranted);
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
        requestLocationUpdates(fallbackMinTimeMs);
        if (requestCurrentLocationSeed) {
            providerAccess.requestSeedForEnabledProvider(provider);
        }
    }

    public long getLastRequestedLocationMinTimeMsOrDefault(long fallbackMinTimeMs) {
        return lastRequestedLocationMinTimeMs > 0L ? lastRequestedLocationMinTimeMs : fallbackMinTimeMs;
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

    @Nullable
    public Integer getFixedSatelliteCount() {
        return gnssStatusTracker.getFixedSatelliteCount();
    }

    @Nullable
    public NavigationLocation getBestStartupLastKnownLocation(long nowMs) {
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            return null;
        }
        NavigationLocation best = LastKnownLocationSelector.findBestStartup(
                providerAccess::getLastKnownLocationQuietly,
                fineGranted,
                coarseGranted,
                nowMs
        );
        AppLogger.d(TAG, "Best last known NavigationLocation=" + NavigationLocationFormatter.format(best));
        return best;
    }

    @NonNull
    public String describeAvailability() {
        return providerAccess.describeAvailability() + ", " + fusedLocationUpdateClient.describeAvailability();
    }

    private void clearActiveLocationRequest() {
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

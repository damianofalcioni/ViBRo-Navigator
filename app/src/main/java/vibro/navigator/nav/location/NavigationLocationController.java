package vibro.navigator.nav.location;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;

public final class NavigationLocationController {

    private static final String TAG = "NavLocation";

    private final NavigationLocationProvider providerAccess;
    private final NavigationGnssTracker gnssStatusTracker;
    private final FusedLocationUpdateClient fusedLocationUpdateClient;
    private final NavigationLocationUpdateRequester updateRequester;
    private final FusedLocationUsePolicy fusedLocationUsePolicy;
    private final ElapsedRealtimeSource elapsedRealtimeSource;

    private long lastRequestedLocationMinTimeMs = -1L;
    @Nullable
    private String lastRequestedProvider;
    private long nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;

    public NavigationLocationController(
            @NonNull NavigationLocationProvider providerAccess,
            @NonNull NavigationGnssTracker gnssStatusTracker,
            @NonNull FusedLocationUpdateClient fusedLocationUpdateClient,
            @NonNull FusedLocationUsePolicy fusedLocationUsePolicy,
            @NonNull ElapsedRealtimeSource elapsedRealtimeSource
    ) {
        this.providerAccess = providerAccess;
        this.gnssStatusTracker = gnssStatusTracker;
        this.fusedLocationUpdateClient = fusedLocationUpdateClient;
        this.fusedLocationUsePolicy = fusedLocationUsePolicy;
        this.elapsedRealtimeSource = elapsedRealtimeSource;
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
        NavigationLocationUpdateRequester.Result result = updateRequester.request(
                minTimeMs,
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
        refreshNextEvaluationDeadline(minTimeMs);
        lastRequestedLocationMinTimeMs = minTimeMs;
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

    public void onProviderEnabled(@NonNull String provider, long fallbackMinTimeMs) {
        requestLocationUpdates(fallbackMinTimeMs);
        providerAccess.requestSeedForEnabledProvider(provider);
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
    public NavigationLocation getBestStartupLastKnownLocation() {
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            return null;
        }
        NavigationLocation best = LastKnownLocationSelector.findBestStartup(
                providerAccess::getLastKnownLocationQuietly,
                fineGranted,
                coarseGranted,
                System.currentTimeMillis()
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
        nextEvaluationDeadlineElapsedMs = elapsedRealtimeSource.elapsedRealtime() + minTimeMs;
    }

    public interface FusedLocationUsePolicy {
        boolean shouldUseFusedLocation();
    }

    public interface ElapsedRealtimeSource {
        long elapsedRealtime();
    }

    public static boolean canUseProvider(@NonNull String provider, boolean fineGranted, boolean coarseGranted) {
        return NavigationLocationProviders.canUseProvider(provider, fineGranted, coarseGranted);
    }

    public static boolean shouldReuseActiveLocationRequest(
            long minTimeMs,
            @Nullable String providerSummary,
            long lastRequestedLocationMinTimeMs,
            @Nullable String lastRequestedProvider
    ) {
        return NavigationLocationProviders.shouldReuseActiveLocationRequest(
                minTimeMs,
                providerSummary,
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider
        );
    }

}

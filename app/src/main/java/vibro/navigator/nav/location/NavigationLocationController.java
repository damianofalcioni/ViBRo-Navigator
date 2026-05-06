package vibro.navigator.nav.location;


import vibro.navigator.nav.model.NavState;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

import java.util.List;
import java.util.concurrent.Executor;

public final class NavigationLocationController {

    private static final String TAG = "NavLocation";

    private final LocationListener listener;
    @Nullable
    private final LocationManager locationManager;
    private final NavigationLocationProviderAccess providerAccess;
    private final NavigationGnssStatusTracker gnssStatusTracker;
    private final NavigationCurrentLocationSeeder currentLocationSeeder;
    private final FusedLocationUpdateClient fusedLocationUpdateClient;
    private final Context appContext;

    private long lastRequestedLocationMinTimeMs = -1L;
    @Nullable
    private String lastRequestedProvider;
    private long nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;

    public NavigationLocationController(@NonNull Context context, @NonNull LocationListener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.providerAccess = new NavigationLocationProviderAccess(context, locationManager, listener);
        this.gnssStatusTracker = new NavigationGnssStatusTracker(locationManager);
        this.fusedLocationUpdateClient = DistributionServices.createFusedLocationUpdateClient(context, listener);
        Executor locationCallbackExecutor = ContextCompat.getMainExecutor(context);
        this.currentLocationSeeder = new NavigationCurrentLocationSeeder(
                locationManager,
                listener,
                locationCallbackExecutor
        );
    }

    public void resetTrackingState() {
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        currentLocationSeeder.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.removeUpdates();
        gnssStatusTracker.reset();
    }

    public void stopTracking() {
        currentLocationSeeder.cancelPendingCurrentLocationRequests();
        fusedLocationUpdateClient.removeUpdates();
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        gnssStatusTracker.reset();
        removeLegacyUpdates();
    }

    public void requestLocationUpdates(long minTimeMs) {
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        if (!NavigationLocationProviderAccess.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            AppLogger.w(TAG, "Location permission unavailable, cannot request updates");
            return;
        }
        if (requestFusedLocationUpdatesIfEnabled(minTimeMs, fineGranted, coarseGranted)) {
            return;
        }
        if (locationManager == null) {
            AppLogger.w(TAG, "LocationManager unavailable, cannot request updates");
            return;
        }

        List<String> providers = providerAccess.enabledPermittedProviders(fineGranted, coarseGranted);
        String providerSummary = NavigationLocationProviderAccess.joinProviders(providers);
        if (providers.isEmpty()) {
            clearActiveLocationRequest();
            AppLogger.w(TAG, "No enabled location provider available for updates " + describeAvailability());
            return;
        }
        if (shouldReuseActiveLocationRequest(
                minTimeMs,
                providerSummary,
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider
        )) {
            return;
        }

        fusedLocationUpdateClient.removeUpdates();
        removeLegacyUpdates();

        List<String> requestedProviders = providerAccess.requestProviderUpdates(providers, minTimeMs);
        if (requestedProviders.isEmpty()) {
            clearActiveLocationRequest();
            AppLogger.w(TAG, "Failed to request location updates from permitted providers " + describeAvailability());
            return;
        }

        String requestedProviderSummary = NavigationLocationProviderAccess.joinProviders(requestedProviders);
        gnssStatusTracker.updateForRequestedProviders(requestedProviders);
        nextEvaluationDeadlineElapsedMs = SystemClock.elapsedRealtime() + minTimeMs;
        lastRequestedLocationMinTimeMs = minTimeMs;
        lastRequestedProvider = requestedProviderSummary;
        AppLogger.i(TAG, "Requested location updates provider=" + requestedProviderSummary + " minTimeMs=" + minTimeMs);
    }

    public void requestCurrentLocationSeeds() {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        if (shouldUseFusedLocation()) {
            return;
        }
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        currentLocationSeeder.requestSeeds(fineGranted, coarseGranted);
    }

    public void onProviderEnabled(@NonNull String provider, long fallbackMinTimeMs) {
        requestLocationUpdates(fallbackMinTimeMs);
        if (!shouldUseFusedLocation()) {
            currentLocationSeeder.requestSeedForEnabledProvider(provider);
        }
    }

    public long getLastRequestedLocationMinTimeMsOrDefault(long fallbackMinTimeMs) {
        return lastRequestedLocationMinTimeMs > 0L ? lastRequestedLocationMinTimeMs : fallbackMinTimeMs;
    }

    public long getNextEvaluationDeadlineElapsedMs() {
        return nextEvaluationDeadlineElapsedMs;
    }

    @Nullable
    public Integer getFixedSatelliteCount() {
        return gnssStatusTracker.getFixedSatelliteCount();
    }

    @Nullable
    public Location getBestStartupLastKnownLocation() {
        if (locationManager == null) {
            return null;
        }
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        if (!NavigationLocationProviderAccess.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            return null;
        }
        Location best = LastKnownLocationSelector.findBestStartup(
                providerAccess::getLastKnownLocationQuietly,
                fineGranted,
                coarseGranted,
                System.currentTimeMillis()
        );
        AppLogger.d(TAG, "Best last known location=" + NavigationLocationFormatter.format(best));
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
        removeLegacyUpdates();
    }

    private boolean requestFusedLocationUpdatesIfEnabled(
            long minTimeMs,
            boolean fineGranted,
            boolean coarseGranted
    ) {
        if (!shouldUseFusedLocation()) {
            return false;
        }
        String providerSummary = LiveLocationCoordinator.FUSED_PROVIDER;
        if (shouldReuseActiveLocationRequest(
                minTimeMs,
                providerSummary,
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider
        )) {
            return true;
        }
        removeLegacyUpdates();
        if (!fusedLocationUpdateClient.requestUpdates(minTimeMs, fineGranted, coarseGranted)) {
            AppLogger.w(TAG, "Fused location unavailable, falling back to legacy providers "
                    + fusedLocationUpdateClient.describeAvailability());
            return false;
        }
        currentLocationSeeder.cancelPendingCurrentLocationRequests();
        gnssStatusTracker.reset();
        nextEvaluationDeadlineElapsedMs = SystemClock.elapsedRealtime() + minTimeMs;
        lastRequestedLocationMinTimeMs = minTimeMs;
        lastRequestedProvider = providerSummary;
        AppLogger.i(TAG, "Requested fused location updates minTimeMs=" + minTimeMs);
        return true;
    }

    private boolean shouldUseFusedLocation() {
        return DistributionServices.supportsFusedLocation()
                && AppSettings.isFusedLocationEnabled(appContext)
                && fusedLocationUpdateClient.isAvailable();
    }

    private void removeLegacyUpdates() {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(listener);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while removing legacy location updates", e);
        }
    }

    public static boolean canUseProvider(@NonNull String provider, boolean fineGranted, boolean coarseGranted) {
        return NavigationLocationProviderAccess.canUseProvider(provider, fineGranted, coarseGranted);
    }

    public static boolean shouldReuseActiveLocationRequest(
            long minTimeMs,
            @Nullable String providerSummary,
            long lastRequestedLocationMinTimeMs,
            @Nullable String lastRequestedProvider
    ) {
        return NavigationLocationProviderAccess.shouldReuseActiveLocationRequest(
                minTimeMs,
                providerSummary,
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider
        );
    }

}

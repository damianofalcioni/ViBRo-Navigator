package vibro.navigator.nav;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

final class NavigationLocationController {

    private static final String TAG = "NavLocation";

    private final Context context;
    private final LocationListener listener;
    @Nullable
    private final LocationManager locationManager;
    private final Executor locationCallbackExecutor;

    private long lastRequestedLocationMinTimeMs = -1L;
    @Nullable
    private String lastRequestedProvider;
    @Nullable
    private CancellationSignal gpsCurrentLocationCancellation;
    @Nullable
    private CancellationSignal networkCurrentLocationCancellation;
    private long nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
    @Nullable
    private GnssStatus.Callback gnssStatusCallback;
    @Nullable
    private Integer fixedSatelliteCount;

    NavigationLocationController(@NonNull Context context, @NonNull LocationListener listener) {
        this.context = context;
        this.listener = listener;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.locationCallbackExecutor = ContextCompat.getMainExecutor(context);
    }

    void resetTrackingState() {
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        fixedSatelliteCount = null;
        cancelPendingCurrentLocationRequests();
        stopGnssStatusTracking();
    }

    void stopTracking() {
        cancelPendingCurrentLocationRequests();
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        fixedSatelliteCount = null;
        stopGnssStatusTracking();
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(listener);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to remove location updates", e);
        }
    }

    void requestLocationUpdates(long minTimeMs) {
        if (locationManager == null) {
            AppLogger.w(TAG, "LocationManager unavailable, cannot request updates");
            return;
        }
        boolean fineGranted = hasFineLocationPermission();
        boolean coarseGranted = hasCoarseLocationPermission();
        if (!hasAnyLocationPermission(fineGranted, coarseGranted)) {
            AppLogger.w(TAG, "Location permission unavailable, cannot request updates");
            return;
        }

        List<String> providers = new ArrayList<>(2);
        addEnabledProviderIfPermitted(providers, LocationManager.GPS_PROVIDER, fineGranted, coarseGranted);
        addEnabledProviderIfPermitted(providers, LocationManager.NETWORK_PROVIDER, fineGranted, coarseGranted);
        String providerSummary = joinProviders(providers);
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

        try {
            locationManager.removeUpdates(listener);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while resetting location updates", e);
        }

        List<String> requestedProviders = requestProviderUpdates(providers, minTimeMs);
        if (requestedProviders.isEmpty()) {
            clearActiveLocationRequest();
            AppLogger.w(TAG, "Failed to request location updates from permitted providers " + describeAvailability());
            return;
        }

        String requestedProviderSummary = joinProviders(requestedProviders);
        updateGnssStatusTracking(requestedProviders);
        nextEvaluationDeadlineElapsedMs = SystemClock.elapsedRealtime() + minTimeMs;
        lastRequestedLocationMinTimeMs = minTimeMs;
        lastRequestedProvider = requestedProviderSummary;
        AppLogger.i(TAG, "Requested location updates provider=" + requestedProviderSummary + " minTimeMs=" + minTimeMs);
    }

    @NonNull
    private List<String> requestProviderUpdates(@NonNull List<String> providers, long minTimeMs) {
        List<String> requestedProviders = new ArrayList<>(providers.size());
        for (String provider : providers) {
            if (requestProviderUpdates(provider, minTimeMs)) {
                requestedProviders.add(provider);
            }
        }
        return requestedProviders;
    }

    private void updateGnssStatusTracking(@NonNull List<String> requestedProviders) {
        if (shouldTrackGnssStatus(requestedProviders)) {
            ensureGnssStatusTracking();
        } else {
            fixedSatelliteCount = null;
            stopGnssStatusTracking();
        }
    }

    private static boolean shouldTrackGnssStatus(@NonNull List<String> requestedProviders) {
        return requestedProviders.contains(LocationManager.GPS_PROVIDER)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    void requestCurrentLocationSeeds() {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        boolean fineGranted = hasFineLocationPermission();
        boolean coarseGranted = hasCoarseLocationPermission();
        if (fineGranted && isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsCurrentLocationCancellation = requestCurrentLocationSeed(LocationManager.GPS_PROVIDER);
        }
        if (hasAnyLocationPermission(fineGranted, coarseGranted)
                && isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkCurrentLocationCancellation = requestCurrentLocationSeed(LocationManager.NETWORK_PROVIDER);
        }
    }

    void onProviderEnabled(@NonNull String provider, long fallbackMinTimeMs) {
        requestLocationUpdates(fallbackMinTimeMs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                gpsCurrentLocationCancellation = requestCurrentLocationSeed(provider);
            } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
                networkCurrentLocationCancellation = requestCurrentLocationSeed(provider);
            }
        }
    }

    long getLastRequestedLocationMinTimeMsOrDefault(long fallbackMinTimeMs) {
        return lastRequestedLocationMinTimeMs > 0L ? lastRequestedLocationMinTimeMs : fallbackMinTimeMs;
    }

    long getNextEvaluationDeadlineElapsedMs() {
        return nextEvaluationDeadlineElapsedMs;
    }

    @Nullable
    Integer getFixedSatelliteCount() {
        return fixedSatelliteCount;
    }

    @Nullable
    Location getBestStartupLastKnownLocation() {
        if (locationManager == null) {
            return null;
        }
        boolean fineGranted = hasFineLocationPermission();
        boolean coarseGranted = hasCoarseLocationPermission();
        if (!hasAnyLocationPermission(fineGranted, coarseGranted)) {
            return null;
        }
        Location gps = fineGranted ? getLastKnownLocationQuietly(LocationManager.GPS_PROVIDER) : null;
        Location network = hasAnyLocationPermission(fineGranted, coarseGranted)
                ? getLastKnownLocationQuietly(LocationManager.NETWORK_PROVIDER)
                : null;
        Location best = NavigationStartupLocationSelector.selectBest(gps, network, System.currentTimeMillis());
        AppLogger.d(TAG, "Best last known location=" + formatLocation(best));
        return best;
    }

    @NonNull
    String describeAvailability() {
        if (locationManager == null) {
            return "locationManager=null";
        }
        boolean fineGranted = hasFineLocationPermission();
        boolean coarseGranted = hasCoarseLocationPermission();
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        gpsEnabled = isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return "fineGranted=" + fineGranted
                + ", coarseGranted=" + coarseGranted
                + ", gpsEnabled=" + gpsEnabled
                + ", networkEnabled=" + networkEnabled
                + ", lastGps=" + formatLocation(fineGranted
                ? getLastKnownLocationQuietly(LocationManager.GPS_PROVIDER)
                : null)
                + ", lastNetwork=" + formatLocation(hasAnyLocationPermission(fineGranted, coarseGranted)
                ? getLastKnownLocationQuietly(LocationManager.NETWORK_PROVIDER)
                : null);
    }

    @Nullable
    private CancellationSignal requestCurrentLocationSeed(@NonNull String provider) {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }
        CancellationSignal cancellationSignal = new CancellationSignal();
        Consumer<Location> consumer = location -> {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            if (location == null) {
                AppLogger.d(TAG, "Current location seed returned null provider=" + provider);
                return;
            }
            AppLogger.i(TAG, "Received current location seed provider=" + provider
                    + " location=" + formatLocation(location));
            listener.onLocationChanged(location);
        };
        try {
            locationManager.getCurrentLocation(provider, cancellationSignal, locationCallbackExecutor, consumer);
            AppLogger.d(TAG, "Requested current location seed provider=" + provider);
            return cancellationSignal;
        } catch (SecurityException e) {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            AppLogger.w(TAG, "Permission denied while requesting current location seed provider=" + provider, e);
            return null;
        } catch (Exception e) {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            AppLogger.w(TAG, "Failed to request current location seed provider=" + provider, e);
            return null;
        }
    }

    private void clearCurrentLocationCancellation(
            @NonNull String provider,
            @NonNull CancellationSignal cancellationSignal
    ) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && gpsCurrentLocationCancellation == cancellationSignal) {
            gpsCurrentLocationCancellation = null;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)
                && networkCurrentLocationCancellation == cancellationSignal) {
            networkCurrentLocationCancellation = null;
        }
    }

    private void cancelPendingCurrentLocationRequests() {
        if (gpsCurrentLocationCancellation != null) {
            gpsCurrentLocationCancellation.cancel();
            gpsCurrentLocationCancellation = null;
        }
        if (networkCurrentLocationCancellation != null) {
            networkCurrentLocationCancellation.cancel();
            networkCurrentLocationCancellation = null;
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void ensureGnssStatusTracking() {
        if (locationManager == null
                || gnssStatusCallback != null) {
            return;
        }
        GnssStatus.Callback callback = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                fixedSatelliteCount = 0;
            }

            @Override
            public void onStopped() {
                fixedSatelliteCount = 0;
            }

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                fixedSatelliteCount = countSatellitesUsedInFix(status);
            }
        };
        try {
            locationManager.registerGnssStatusCallback(callback, new Handler(Looper.getMainLooper()));
            gnssStatusCallback = callback;
            AppLogger.d(TAG, "Registered GNSS status callback");
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while registering GNSS status callback", e);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to register GNSS status callback", e);
        }
    }

    private void stopGnssStatusTracking() {
        if (locationManager == null
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                || gnssStatusCallback == null) {
            return;
        }
        try {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            AppLogger.d(TAG, "Unregistered GNSS status callback");
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to unregister GNSS status callback", e);
        } finally {
            gnssStatusCallback = null;
        }
    }

    @Nullable
    private Location getLastKnownLocationQuietly(@NonNull String provider) {
        if (locationManager == null) {
            return null;
        }
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location provider=" + provider, e);
            return null;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read last known location provider=" + provider, e);
            return null;
        }
    }

    @Nullable
    private static String joinProviders(@NonNull List<String> providers) {
        if (providers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) {
                sb.append("+");
            }
            sb.append(providers.get(i));
        }
        return sb.toString();
    }

    static int countSatellitesUsedInFix(boolean... usedInFixFlags) {
        int fixedCount = 0;
        for (boolean usedInFix : usedInFixFlags) {
            if (usedInFix) {
                fixedCount++;
            }
        }
        return fixedCount;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private static int countSatellitesUsedInFix(@NonNull GnssStatus status) {
        boolean[] usedInFixFlags = new boolean[status.getSatelliteCount()];
        for (int i = 0; i < status.getSatelliteCount(); i++) {
            usedInFixFlags[i] = status.usedInFix(i);
        }
        return countSatellitesUsedInFix(usedInFixFlags);
    }

    private void clearActiveLocationRequest() {
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        fixedSatelliteCount = null;
        stopGnssStatusTracking();
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(listener);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while clearing location updates", e);
        }
    }

    private void addEnabledProviderIfPermitted(
            @NonNull List<String> providers,
            @NonNull String provider,
            boolean fineGranted,
            boolean coarseGranted
    ) {
        if (canUseProvider(provider, fineGranted, coarseGranted)
                && isProviderEnabled(provider)) {
            providers.add(provider);
        }
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasAnyLocationPermission(boolean fineGranted, boolean coarseGranted) {
        return fineGranted || coarseGranted;
    }

    static boolean canUseProvider(@NonNull String provider, boolean fineGranted, boolean coarseGranted) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            return fineGranted;
        }
        if (LocationManager.NETWORK_PROVIDER.equals(provider)
                || LocationManager.PASSIVE_PROVIDER.equals(provider)) {
            return hasAnyLocationPermission(fineGranted, coarseGranted);
        }
        return hasAnyLocationPermission(fineGranted, coarseGranted);
    }

    private boolean isProviderEnabled(@NonNull String provider) {
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read provider state provider=" + provider, e);
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private boolean requestProviderUpdates(@NonNull String provider, long minTimeMs) {
        if (locationManager == null) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.requestLocationUpdates(provider, minTimeMs, 0f, locationCallbackExecutor, listener);
            } else {
                locationManager.requestLocationUpdates(provider, minTimeMs, 0f, listener);
            }
            return true;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while requesting location updates provider=" + provider, e);
            return false;
        }
    }

    static boolean shouldReuseActiveLocationRequest(
            long minTimeMs,
            @Nullable String providerSummary,
            long lastRequestedLocationMinTimeMs,
            @Nullable String lastRequestedProvider
    ) {
        return providerSummary != null
                && minTimeMs == lastRequestedLocationMinTimeMs
                && providerSummary.equals(lastRequestedProvider);
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

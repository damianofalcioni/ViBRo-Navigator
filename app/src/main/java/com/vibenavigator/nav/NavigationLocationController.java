package com.vibenavigator.nav;

import android.content.Context;
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
import androidx.core.content.ContextCompat;

import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

final class NavigationLocationController {

    private static final String TAG = "NavLocation";
    private static final long STARTUP_LAST_KNOWN_MAX_AGE_MS = 15_000L;
    private static final float STARTUP_LAST_KNOWN_MAX_ACCURACY_METERS = 50f;

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
        try {
            if (locationManager == null) {
                AppLogger.w(TAG, "LocationManager unavailable, cannot request updates");
                return;
            }
            List<String> providers = new ArrayList<>(2);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                providers.add(LocationManager.GPS_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                providers.add(LocationManager.NETWORK_PROVIDER);
            }
            String providerSummary = joinProviders(providers);
            if (providers.isEmpty()) {
                nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
                fixedSatelliteCount = null;
                stopGnssStatusTracking();
                if (lastRequestedProvider != null) {
                    locationManager.removeUpdates(listener);
                    lastRequestedLocationMinTimeMs = -1L;
                    lastRequestedProvider = null;
                }
                AppLogger.w(TAG, "No enabled location provider available for updates " + describeAvailability());
                return;
            }
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                ensureGnssStatusTracking();
            } else {
                fixedSatelliteCount = null;
                stopGnssStatusTracking();
            }
            nextEvaluationDeadlineElapsedMs = SystemClock.elapsedRealtime() + minTimeMs;
            if (shouldReuseActiveLocationRequest(
                    minTimeMs,
                    providerSummary,
                    lastRequestedLocationMinTimeMs,
                    lastRequestedProvider
            )) {
                return;
            }
            locationManager.removeUpdates(listener);
            for (String provider : providers) {
                requestProviderUpdates(provider, minTimeMs);
            }
            lastRequestedLocationMinTimeMs = minTimeMs;
            lastRequestedProvider = providerSummary;
            AppLogger.i(TAG, "Requested location updates provider=" + providerSummary + " minTimeMs=" + minTimeMs);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while requesting location updates", e);
        }
    }

    void requestCurrentLocationSeeds() {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsCurrentLocationCancellation = requestCurrentLocationSeed(LocationManager.GPS_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
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
        try {
            if (locationManager == null) {
                return null;
            }
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location best = selectBestStartupLastKnownLocation(gps, network, System.currentTimeMillis());
            AppLogger.d(TAG, "Best last known location=" + formatLocation(best));
            return best;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location", e);
            return null;
        }
    }

    @Nullable
    static Location selectBestStartupLastKnownLocation(
            @Nullable Location gps,
            @Nullable Location network,
            long nowMs
    ) {
        Location best = null;
        if (isUsableStartupLastKnownLocation(gps, nowMs)) {
            best = gps;
        }
        if (isUsableStartupLastKnownLocation(network, nowMs)
                && (best == null
                || network.getTime() > best.getTime()
                || (network.getTime() == best.getTime()
                && accuracyMeters(network) < accuracyMeters(best)))) {
            best = network;
        }
        return best == null ? null : new Location(best);
    }

    static boolean isUsableStartupLastKnownLocation(@Nullable Location location, long nowMs) {
        if (location == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - location.getTime());
        if (ageMs > STARTUP_LAST_KNOWN_MAX_AGE_MS) {
            return false;
        }
        return accuracyMeters(location) <= STARTUP_LAST_KNOWN_MAX_ACCURACY_METERS;
    }

    @NonNull
    String describeAvailability() {
        if (locationManager == null) {
            return "locationManager=null";
        }
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read GPS provider state", e);
        }
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read network provider state", e);
        }
        return "gpsEnabled=" + gpsEnabled
                + ", networkEnabled=" + networkEnabled
                + ", lastGps=" + formatLocation(getLastKnownLocationQuietly(LocationManager.GPS_PROVIDER))
                + ", lastNetwork=" + formatLocation(getLastKnownLocationQuietly(LocationManager.NETWORK_PROVIDER));
    }

    private void requestProviderUpdates(@NonNull String provider, long minTimeMs) {
        if (locationManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.requestLocationUpdates(provider, minTimeMs, 0f, locationCallbackExecutor, listener);
        } else {
            locationManager.requestLocationUpdates(provider, minTimeMs, 0f, listener);
        }
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

    private void ensureGnssStatusTracking() {
        if (locationManager == null
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.N
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

    private static float accuracyMeters(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
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

    private static int countSatellitesUsedInFix(@NonNull GnssStatus status) {
        boolean[] usedInFixFlags = new boolean[status.getSatelliteCount()];
        for (int i = 0; i < status.getSatelliteCount(); i++) {
            usedInFixFlags[i] = status.usedInFix(i);
        }
        return countSatellitesUsedInFix(usedInFixFlags);
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

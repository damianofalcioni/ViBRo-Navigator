package com.vibenavigator.nav;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
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
        cancelPendingCurrentLocationRequests();
    }

    void stopTracking() {
        cancelPendingCurrentLocationRequests();
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
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
            locationManager.removeUpdates(listener);
            List<String> providers = new ArrayList<>(2);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                requestProviderUpdates(LocationManager.GPS_PROVIDER, minTimeMs);
                providers.add(LocationManager.GPS_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                requestProviderUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs);
                providers.add(LocationManager.NETWORK_PROVIDER);
            }
            if (providers.isEmpty()) {
                nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
                AppLogger.w(TAG, "No enabled location provider available for updates " + describeAvailability());
            } else {
                nextEvaluationDeadlineElapsedMs = SystemClock.elapsedRealtime() + minTimeMs;
            }
            String providerSummary = joinProviders(providers);
            if (providerSummary != null
                    && (minTimeMs != lastRequestedLocationMinTimeMs
                    || !providerSummary.equals(lastRequestedProvider))) {
                lastRequestedLocationMinTimeMs = minTimeMs;
                lastRequestedProvider = providerSummary;
                AppLogger.i(TAG, "Requested location updates provider=" + providerSummary + " minTimeMs=" + minTimeMs);
            }
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
    Location getBestLastKnownLocation() {
        try {
            if (locationManager == null) {
                return null;
            }
            Location best = null;
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gps != null) {
                best = gps;
            }
            if (network != null && (best == null || network.getTime() > best.getTime())) {
                best = network;
            }
            AppLogger.d(TAG, "Best last known location=" + formatLocation(best));
            return best;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location", e);
            return null;
        }
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

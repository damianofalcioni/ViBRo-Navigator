package vibro.navigator.android.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.Collections;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.nav.location.FusedLocationUpdateClient;

public final class GplayFusedLocationUpdateClient implements FusedLocationUpdateClient {
    private static final String TAG = "FusedLocation";
    private static final long MIN_FASTEST_INTERVAL_MS = 500L;
    private static final long CURRENT_LOCATION_SEED_DURATION_MS = 15_000L;
    private static final long CURRENT_LOCATION_SEED_MAX_UPDATE_AGE_MS = 15_000L;

    @NonNull
    private final Context context;
    @NonNull
    private final FusedLocationProviderClient client;
    @NonNull
    private final LocationCallback callback;
    private CancellationTokenSource currentLocationSeedCancellation;

    public GplayFusedLocationUpdateClient(
            @NonNull Context context,
            @NonNull NavigationLocationListener listener
    ) {
        this.context = context.getApplicationContext();
        this.client = LocationServices.getFusedLocationProviderClient(this.context);
        this.callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    NavigationLocation navigationLocation = AndroidLocationConverter.toNavigationLocation(location);
                    if (navigationLocation != null) {
                        listener.onLocationChanged(navigationLocation);
                    }
                }
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return GplayFusedLocationRuntimeAvailability.isAvailable(context);
    }

    @Override
    @SuppressLint("MissingPermission")
    public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
        if (!isAvailable() || (!fineGranted && !coarseGranted)) {
            return false;
        }
        try {
            LocationRequest request = buildRequest(minTimeMs, fineGranted);
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener(error -> AppLogger.w(TAG, "Fused location updates failed", error));
            AppLogger.i(TAG, "Requested fused location updates minTimeMs=" + minTimeMs);
            return true;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while requesting fused location updates", e);
            return false;
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to request fused location updates", e);
            return false;
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted) {
        if (!isAvailable() || (!fineGranted && !coarseGranted)) {
            return;
        }
        cancelCurrentLocationSeed();
        currentLocationSeedCancellation = new CancellationTokenSource();
        try {
            CurrentLocationRequest request = buildCurrentLocationSeedRequest(fineGranted);
            client.getCurrentLocation(request, currentLocationSeedCancellation.getToken())
                    .addOnSuccessListener(this::dispatchCurrentLocationSeed)
                    .addOnFailureListener(error -> AppLogger.w(TAG, "Fused current location seed failed", error));
            AppLogger.d(TAG, "Requested fused current location seed");
        } catch (SecurityException e) {
            cancelCurrentLocationSeed();
            AppLogger.w(TAG, "Permission denied while requesting fused current location seed", e);
        } catch (RuntimeException e) {
            cancelCurrentLocationSeed();
            AppLogger.w(TAG, "Failed to request fused current location seed", e);
        }
    }

    @Override
    public void removeUpdates() {
        cancelCurrentLocationSeed();
        try {
            client.removeLocationUpdates(callback)
                    .addOnFailureListener(error -> AppLogger.w(TAG, "Failed to remove fused updates", error));
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to remove fused updates", e);
        }
    }

    @NonNull
    @Override
    public String describeAvailability() {
        return "fusedLocation=true, " + GplayFusedLocationRuntimeAvailability.describe(context);
    }

    @NonNull
    static LocationRequest buildRequest(long minTimeMs, boolean fineGranted) {
        long intervalMs = Math.max(MIN_FASTEST_INTERVAL_MS, minTimeMs);
        long fastestMs = Math.max(MIN_FASTEST_INTERVAL_MS, intervalMs / 2L);
        int priority = fineGranted
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        return new LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis(fastestMs)
                .setMaxUpdateDelayMillis(intervalMs)
                .setMinUpdateDistanceMeters(0f)
                // Route guidance filters decide whether a delivered fix is usable; waiting here can starve
                // screen-off navigation callbacks on some devices.
                .setWaitForAccurateLocation(false)
                .build();
    }

    @NonNull
    static CurrentLocationRequest buildCurrentLocationSeedRequest(boolean fineGranted) {
        int priority = fineGranted
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        return new CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setDurationMillis(CURRENT_LOCATION_SEED_DURATION_MS)
                .setMaxUpdateAgeMillis(CURRENT_LOCATION_SEED_MAX_UPDATE_AGE_MS)
                .build();
    }

    private void dispatchCurrentLocationSeed(Location location) {
        currentLocationSeedCancellation = null;
        if (location == null) {
            AppLogger.d(TAG, "Fused current location seed returned null");
            return;
        }
        AppLogger.i(TAG, "Received fused current location seed");
        callback.onLocationResult(LocationResult.create(Collections.singletonList(location)));
    }

    private void cancelCurrentLocationSeed() {
        if (currentLocationSeedCancellation != null) {
            currentLocationSeedCancellation.cancel();
            currentLocationSeedCancellation = null;
        }
    }

}

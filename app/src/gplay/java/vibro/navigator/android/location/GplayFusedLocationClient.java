package vibro.navigator.android.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Collections;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationLocation;

public final class GplayFusedLocationClient implements FusedLocationUpdateClient, FusedLocationDiagnosticClient {
    private static final String TAG = "FusedLocation";
    private static final long MIN_INTERVAL_MS = 500L;
    private static final long CURRENT_LOCATION_SEED_DURATION_MS = 15_000L;
    private static final long CURRENT_LOCATION_SEED_MAX_UPDATE_AGE_MS = 15_000L;

    @NonNull
    private final Context context;
    @NonNull
    private final FusedLocationProviderClient client;
    @Nullable
    private final LocationCallback callback;
    @NonNull
    private final GplayCurrentLocationSeedRequest currentLocationSeedRequest =
            new GplayCurrentLocationSeedRequest();
    @NonNull
    private final GplayFusedUpdateFailureHandler updateFailureHandler =
            new GplayFusedUpdateFailureHandler();

    public GplayFusedLocationClient(
            @NonNull Context context,
            @NonNull NavigationLocationListener listener
    ) {
        this(context, createLocationCallback(listener));
    }

    @NonNull
    public static FusedLocationDiagnosticClient diagnosticClient(@NonNull Context context) {
        return new GplayFusedLocationClient(context, (LocationCallback) null);
    }

    private GplayFusedLocationClient(
            @NonNull Context context,
            @Nullable LocationCallback callback
    ) {
        this.context = context.getApplicationContext();
        this.client = LocationServices.getFusedLocationProviderClient(this.context);
        this.callback = callback;
    }

    public static boolean isRuntimeAvailable(@NonNull Context context) {
        return runtimeStatus(context) == ConnectionResult.SUCCESS;
    }

    @NonNull
    private static LocationCallback createLocationCallback(@NonNull NavigationLocationListener listener) {
        return new LocationCallback() {
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
        return isRuntimeAvailable(context);
    }

    @Override
    public void setUpdateFailureListener(@NonNull Runnable listener) {
        updateFailureHandler.setListener(listener);
    }

    @Override
    @SuppressLint("MissingPermission")
    public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
        if (callback == null || !isAvailable() || (!fineGranted && !coarseGranted)) {
            return false;
        }
        try {
            int requestGeneration = updateFailureHandler.beginRequest();
            LocationRequest request = buildRequest(minTimeMs, fineGranted);
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener(error -> updateFailureHandler.handle(requestGeneration, error));
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
        currentLocationSeedRequest.cancel();
        if (callback == null || !isAvailable() || (!fineGranted && !coarseGranted)) {
            return;
        }
        GplayCurrentLocationSeedRequest.ActiveRequest activeRequest = currentLocationSeedRequest.begin();
        try {
            CurrentLocationRequest request = buildCurrentLocationSeedRequest(fineGranted);
            client.getCurrentLocation(request, activeRequest.cancellation.getToken())
                    .addOnSuccessListener(location -> dispatchCurrentLocationSeed(activeRequest.id, location))
                    .addOnFailureListener(error -> currentLocationSeedRequest.handleFailure(activeRequest.id, error));
            AppLogger.d(TAG, "Requested fused current location seed");
        } catch (SecurityException e) {
            currentLocationSeedRequest.cancel();
            AppLogger.w(TAG, "Permission denied while requesting fused current location seed", e);
        } catch (RuntimeException e) {
            currentLocationSeedRequest.cancel();
            AppLogger.w(TAG, "Failed to request fused current location seed", e);
        }
    }

    @Override
    public void cancelCurrentLocationSeed() {
        currentLocationSeedRequest.cancel();
    }

    @Override
    public void removeUpdates() {
        cancelCurrentLocationSeed();
        updateFailureHandler.invalidate();
        if (callback == null) {
            return;
        }
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
        return "fusedLocation=true, googlePlayServicesStatus=" + runtimeStatus(context);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void requestLastKnownLocation(@NonNull FusedLocationDiagnosticClient.Callback callback) {
        if (!isAvailable()) {
            callback.onFailure("unavailable");
            return;
        }
        try {
            client.getLastLocation()
                    .addOnSuccessListener(location ->
                            callback.onLocation(AndroidLocationConverter.toNavigationLocation(location)))
                    .addOnFailureListener(error -> {
                        AppLogger.w(TAG, "Failed to read fused last known location", error);
                        callback.onFailure("unavailable");
                    });
        } catch (SecurityException e) {
            callback.onFailure("permission denied");
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to read fused last known location", e);
            callback.onFailure("unavailable");
        }
    }

    @NonNull
    static LocationRequest buildRequest(long minTimeMs, boolean fineGranted) {
        long intervalMs = Math.max(MIN_INTERVAL_MS, minTimeMs);
        int priority = fineGranted
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        return new LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs)
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

    private void dispatchCurrentLocationSeed(int requestId, Location location) {
        if (!currentLocationSeedRequest.completeIfActive(requestId)) {
            AppLogger.d(TAG, "Ignoring stale fused current location seed");
            return;
        }
        if (location == null) {
            AppLogger.d(TAG, "Fused current location seed returned null");
            return;
        }
        AppLogger.i(TAG, "Received fused current location seed");
        callback.onLocationResult(LocationResult.create(Collections.singletonList(location)));
    }

    private static int runtimeStatus(@NonNull Context context) {
        return GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context);
    }

}

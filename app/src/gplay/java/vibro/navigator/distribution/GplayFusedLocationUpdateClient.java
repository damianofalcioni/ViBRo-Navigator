package vibro.navigator.distribution;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.FusedLocationUpdateClient;

final class GplayFusedLocationUpdateClient implements FusedLocationUpdateClient {
    private static final String TAG = "FusedLocation";
    private static final long MIN_FASTEST_INTERVAL_MS = 500L;

    @NonNull
    private final Context context;
    @NonNull
    private final FusedLocationProviderClient client;
    @NonNull
    private final LocationCallback callback;

    GplayFusedLocationUpdateClient(
            @NonNull Context context,
            @NonNull LocationListener listener
    ) {
        this.context = context.getApplicationContext();
        this.client = LocationServices.getFusedLocationProviderClient(this.context);
        this.callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    listener.onLocationChanged(location);
                }
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return googlePlayServicesStatus() == ConnectionResult.SUCCESS;
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
    public void removeUpdates() {
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
        return "fusedLocation=true, googlePlayServicesStatus=" + googlePlayServicesStatus();
    }

    @NonNull
    private static LocationRequest buildRequest(long minTimeMs, boolean fineGranted) {
        long intervalMs = Math.max(MIN_FASTEST_INTERVAL_MS, minTimeMs);
        long fastestMs = Math.max(MIN_FASTEST_INTERVAL_MS, intervalMs / 2L);
        int priority = fineGranted
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        return new LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis(fastestMs)
                .setWaitForAccurateLocation(fineGranted)
                .build();
    }

    private int googlePlayServicesStatus() {
        return GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context);
    }
}

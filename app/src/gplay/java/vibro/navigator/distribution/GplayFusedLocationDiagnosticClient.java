package vibro.navigator.distribution;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;

final class GplayFusedLocationDiagnosticClient implements FusedLocationDiagnosticClient {
    private static final String TAG = "FusedLocation";

    @NonNull
    private final Context context;
    @NonNull
    private final FusedLocationProviderClient client;

    GplayFusedLocationDiagnosticClient(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.client = LocationServices.getFusedLocationProviderClient(this.context);
    }

    @Override
    public boolean isAvailable() {
        return googlePlayServicesStatus() == ConnectionResult.SUCCESS;
    }

    @Override
    @SuppressLint("MissingPermission")
    public void requestLastKnownLocation(@NonNull Callback callback) {
        if (!isAvailable()) {
            callback.onFailure("unavailable");
            return;
        }
        try {
            client.getLastLocation()
                    .addOnSuccessListener(callback::onLocation)
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

    private int googlePlayServicesStatus() {
        return GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context);
    }
}

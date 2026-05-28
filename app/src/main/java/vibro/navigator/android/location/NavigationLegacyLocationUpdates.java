package vibro.navigator.android.location;

import android.annotation.SuppressLint;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

final class NavigationLegacyLocationUpdates {
    private static final String TAG = "NavLocation";

    private NavigationLegacyLocationUpdates() {
    }

    @SuppressLint("MissingPermission")
    static void remove(
            @Nullable LocationManager locationManager,
            @NonNull LocationListener listener
    ) {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(listener);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while removing legacy location updates", e);
        }
    }
}

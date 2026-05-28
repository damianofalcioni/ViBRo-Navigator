package vibro.navigator.android.location;

import android.app.Activity;
import android.location.LocationManager;

import androidx.annotation.NonNull;

public final class AndroidLocationSettings {
    private AndroidLocationSettings() {
    }

    public static boolean isLocationEnabled(@NonNull Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
            return false;
        }
    }
}

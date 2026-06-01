package vibro.navigator.android.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class AndroidLocationPermissions {
    private AndroidLocationPermissions() {
    }

    public static boolean hasFineLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasCoarseLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAnyLocationPermission(@NonNull Context context) {
        return hasFineLocationPermission(context) || hasCoarseLocationPermission(context);
    }
}

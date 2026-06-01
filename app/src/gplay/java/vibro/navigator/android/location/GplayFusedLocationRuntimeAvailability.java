package vibro.navigator.android.location;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

public final class GplayFusedLocationRuntimeAvailability {
    private GplayFusedLocationRuntimeAvailability() {
    }

    public static boolean isAvailable(@NonNull Context context) {
        return GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
    }
}

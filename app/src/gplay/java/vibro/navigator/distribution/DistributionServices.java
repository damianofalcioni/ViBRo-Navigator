package vibro.navigator.distribution;

import android.content.Context;
import android.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

import vibro.navigator.BuildConfig;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.poi.search.GoogleGeocodeClient;
import vibro.navigator.poi.search.PoiSearchClient;

public final class DistributionServices {
    private DistributionServices() {
    }

    public static boolean supportsFusedLocation() {
        return true;
    }

    public static boolean isFusedLocationRuntimeAvailable(@NonNull Context context) {
        return GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
    }

    @NonNull
    public static FusedLocationUpdateClient createFusedLocationUpdateClient(
            @NonNull Context context,
            @NonNull LocationListener listener
    ) {
        return new GplayFusedLocationUpdateClient(context, listener);
    }

    @NonNull
    public static FusedLocationDiagnosticClient createFusedLocationDiagnosticClient(@NonNull Context context) {
        return new GplayFusedLocationDiagnosticClient(context);
    }

    @Nullable
    public static PoiSearchClient createGooglePoiSearchClient() {
        String key = BuildConfig.GOOGLE_MAPS_API_KEY;
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return new GoogleGeocodeClient(key.trim());
    }
}

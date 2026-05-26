package vibro.navigator.distribution;

import android.content.Context;
import android.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.poi.search.GoogleGeocodeClient;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.settings.AppSettings;

public final class DistributionServices {
    private DistributionServices() {
    }

    public static boolean supportsFusedLocation() {
        return true;
    }

    public static boolean supportsUserGooglePoiApiKey() {
        return true;
    }

    @NonNull
    public static GooglePoiApiKeyValidationResult validateUserGooglePoiApiKey(@NonNull String apiKey) {
        return GoogleGeocodeClient.validateApiKey(apiKey);
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
    public static PoiSearchClient createGooglePoiSearchClient(@NonNull Context context) {
        if (!AppSettings.isGooglePoiSearchEnabled(context)) {
            return null;
        }
        String key = AppSettings.getGooglePoiApiKey(context);
        if (key.trim().isEmpty()) {
            return null;
        }
        return new GoogleGeocodeClient(key);
    }
}

package vibro.navigator.distribution;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.location.NoOpFusedLocationClient;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.poi.search.PoiSearchClient;

public final class DistributionServices {
    private DistributionServices() {
    }

    public static boolean supportsFusedLocation() {
        return false;
    }

    public static boolean supportsUserGooglePoiApiKey() {
        return false;
    }

    public static boolean supportsAndroidAutoIntegration() {
        return false;
    }

    public static void configureAndroidAutoIntegration(@NonNull Context context, boolean enabled) {
    }

    @NonNull
    public static GooglePoiApiKeyValidationResult validateUserGooglePoiApiKey(@NonNull String apiKey) {
        return GooglePoiApiKeyValidationResult.ERROR;
    }

    public static boolean isFusedLocationRuntimeAvailable(@NonNull Context context) {
        return false;
    }

    @NonNull
    public static FusedLocationUpdateClient createFusedLocationUpdateClient(
            @NonNull Context context,
            @NonNull NavigationLocationListener listener
    ) {
        return new NoOpFusedLocationClient();
    }

    @NonNull
    public static FusedLocationDiagnosticClient createFusedLocationDiagnosticClient(@NonNull Context context) {
        return new NoOpFusedLocationClient();
    }

    @Nullable
    public static PoiSearchClient createGooglePoiSearchClient(@NonNull Context context) {
        return null;
    }
}

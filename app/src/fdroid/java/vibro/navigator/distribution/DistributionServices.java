package vibro.navigator.distribution;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.location.NoOpFusedLocationClient;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.poi.search.PoiReverseGeocodingClient;
import vibro.navigator.poi.search.PoiSearchClient;

public final class DistributionServices {
    private static final String NOT_SUPPORTED = "not_supported";

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

    public static boolean supportsGooglePlayServicesDiagnostics() {
        return false;
    }

    @NonNull
    public static String googlePlayServicesRuntimeStatus(@NonNull Context context) {
        return NOT_SUPPORTED;
    }

    @NonNull
    public static String androidAutoIntegrationComponentState(@NonNull Context context) {
        return NOT_SUPPORTED;
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

    @Nullable
    public static PoiReverseGeocodingClient createGooglePoiReverseGeocodingClient(@NonNull Context context) {
        return null;
    }

    @Nullable
    public static ComponentName createSpeechRecognitionServiceFallback() {
        return null;
    }
}
